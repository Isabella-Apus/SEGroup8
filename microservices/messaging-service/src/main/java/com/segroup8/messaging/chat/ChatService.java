package com.segroup8.messaging.chat;

import com.segroup8.messaging.access.AccessPolicy;
import com.segroup8.messaging.access.AccessPolicy.AccessSnapshot;
import com.segroup8.messaging.access.BlockPolicy;
import com.segroup8.messaging.chat.ChatModels.Conversation;
import com.segroup8.messaging.chat.ChatModels.CreateConversationRequest;
import com.segroup8.messaging.chat.ChatModels.Message;
import com.segroup8.messaging.chat.ChatModels.Participant;
import com.segroup8.messaging.common.ApiException;
import com.segroup8.messaging.delivery.DeliveryOutboxService;
import com.segroup8.messaging.notification.NotificationService;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final Set<String> SOURCE_TYPES = Set.of("DIRECT", "PRODUCT", "SECONDHAND");
    private final JdbcTemplate jdbc;
    private final AccessPolicy access;
    private final BlockPolicy blocks;
    private final NotificationService notifications;
    private final DeliveryOutboxService delivery;

    public ChatService(JdbcTemplate jdbc, AccessPolicy access, BlockPolicy blocks,
            NotificationService notifications, DeliveryOutboxService delivery) {
        this.jdbc = jdbc; this.access = access; this.blocks = blocks; this.notifications = notifications;
        this.delivery = delivery;
    }

    @Transactional
    public Conversation createOrGet(long actorId, CreateConversationRequest request, String token) {
        if (actorId == request.targetUserId()) throw new ApiException(400, "Cannot create a conversation with yourself");
        AccessSnapshot actor = access.requireActive(actorId);
        AccessSnapshot target = access.requireActive(request.targetUserId());
        blocks.requireCommunicationAllowed(actorId, request.targetUserId(), token);
        String type = normalizeType(request.sourceType());
        long sourceId = normalizeSourceId(type, request.sourceId());
        String title = normalizeTitle(type, request.sourceTitle());
        Participants pair = participants(actor, target, type);
        Long existing = findConversation(pair.buyer().userId(), pair.seller().userId(), type, sourceId);
        long id;
        if (existing != null) {
            id = existing;
            jdbc.update("update chat_conversation set source_title=?, update_time=current_timestamp where id=?", title, id);
        } else {
            try { id = insertConversation(pair, type, sourceId, title); }
            catch (DuplicateKeyException ex) {
                Long concurrent = findConversation(pair.buyer().userId(), pair.seller().userId(), type, sourceId);
                if (concurrent == null) throw ex;
                id = concurrent;
            }
        }
        return requireConversation(actorId, id);
    }

    public List<Conversation> list(long userId) {
        access.requireActive(userId);
        return jdbc.query(CONVERSATION_SELECT + " where (c.buyer_user_id=? or c.seller_user_id=?) "
                        + "order by c.last_message_time desc, c.id desc",
                (rs, row) -> mapConversation(rs, userId), userId, userId, userId);
    }

    @Transactional
    public List<Message> messages(long userId, long conversationId) {
        ConversationRow conversation = requireConversationRow(userId, conversationId);
        jdbc.update("update chat_message set is_read=1 where conversation_id=? and receiver_user_id=? and is_read=0",
                conversationId, userId);
        return jdbc.query("select id,conversation_id,sender_user_id,receiver_user_id,content,is_read,create_time "
                        + "from chat_message where conversation_id=? order by create_time,id",
                (rs, row) -> new Message(rs.getLong("id"), rs.getLong("conversation_id"),
                        rs.getLong("sender_user_id"), rs.getLong("receiver_user_id"), rs.getString("content"),
                        rs.getInt("is_read"), rs.getTimestamp("create_time").toLocalDateTime(),
                        participant(conversation, rs.getLong("sender_user_id"))), conversationId);
    }

    @Transactional
    public Message send(long senderId, long conversationId, String content, String token) {
        ConversationRow conversation = requireConversationRow(senderId, conversationId);
        long receiverId = conversation.buyerId() == senderId ? conversation.sellerId() : conversation.buyerId();
        access.requireActive(senderId);
        access.requireActive(receiverId);
        blocks.requireCommunicationAllowed(senderId, receiverId, token);
        String normalized = normalizeContent(content);
        LocalDateTime now = LocalDateTime.now();
        long messageId = insertMessage(conversationId, senderId, receiverId, normalized, now);
        jdbc.update("update chat_conversation set last_message_content=?,last_message_time=?,update_time=? where id=?",
                normalized, Timestamp.valueOf(now), Timestamp.valueOf(now), conversationId);
        Message message = new Message(messageId, conversationId, senderId, receiverId, normalized, 0, now,
                participant(conversation, senderId));
        String senderName = message.sender().nickname();
        String targetPath = "OFFICIAL_SELLER".equalsIgnoreCase(participant(conversation, receiverId).role())
                ? "/merchant/messages?conversationId=" + conversationId : "/messages?conversationId=" + conversationId;
        notifications.create(receiverId, "新消息", senderName + " 给你发送了新消息", targetPath, null);
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) traceId = UUID.randomUUID().toString();
        delivery.enqueueWebSocket(null, "delivery:chat:" + messageId + ":" + senderId,
                senderId, "CHAT_MESSAGE", message, traceId);
        delivery.enqueueWebSocket(null, "delivery:chat:" + messageId + ":" + receiverId,
                receiverId, "CHAT_MESSAGE", message, traceId);
        log.info("messaging chat persisted conversationId={} messageId={} traceId={}",
                conversationId, messageId, traceId);
        return message;
    }

    private long insertConversation(Participants pair, String type, long sourceId, String title) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("insert into chat_conversation(" +
                    "buyer_user_id,seller_user_id,buyer_display_name,buyer_avatar_url,buyer_role," +
                    "seller_display_name,seller_avatar_url,seller_role,source_type,source_id,source_title,create_time,update_time) " +
                    "values(?,?,?,?,?,?,?,?,?,?,?,current_timestamp,current_timestamp)", new String[]{"id"});
            int i = 1;
            for (Object value : List.of(pair.buyer().userId(), pair.seller().userId(),
                    text(pair.buyer().displayName()), text(pair.buyer().avatarUrl()), text(pair.buyer().role()),
                    text(pair.seller().displayName()), text(pair.seller().avatarUrl()), text(pair.seller().role()),
                    type, sourceId, title)) ps.setObject(i++, value);
            return ps;
        }, key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }

    private long insertMessage(long conversationId, long senderId, long receiverId, String content, LocalDateTime now) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("insert into chat_message(" +
                    "conversation_id,sender_user_id,receiver_user_id,content,is_read,create_time) values(?,?,?,?,0,?)",
                    new String[]{"id"});
            ps.setLong(1, conversationId); ps.setLong(2, senderId); ps.setLong(3, receiverId);
            ps.setString(4, content); ps.setTimestamp(5, Timestamp.valueOf(now)); return ps;
        }, key);
        return Objects.requireNonNull(key.getKey()).longValue();
    }

    private Long findConversation(long buyer, long seller, String type, long sourceId) {
        return jdbc.query("select id from chat_conversation where buyer_user_id=? and seller_user_id=? and source_type=? and source_id=?",
                rs -> rs.next() ? rs.getLong(1) : null, buyer, seller, type, sourceId);
    }

    private Conversation requireConversation(long userId, long id) {
        List<Conversation> values = jdbc.query(CONVERSATION_SELECT + " where c.id=? and (c.buyer_user_id=? or c.seller_user_id=?)",
                (rs, row) -> mapConversation(rs, userId), userId, id, userId, userId);
        if (values.isEmpty()) throw new ApiException(403, "Conversation not found or access denied");
        return values.get(0);
    }

    private ConversationRow requireConversationRow(long userId, long id) {
        List<ConversationRow> values = jdbc.query("select * from chat_conversation where id=? and (buyer_user_id=? or seller_user_id=?)",
                (rs, row) -> new ConversationRow(rs.getLong("id"), rs.getLong("buyer_user_id"), rs.getLong("seller_user_id"),
                        rs.getString("buyer_display_name"), rs.getString("buyer_avatar_url"), rs.getString("buyer_role"),
                        rs.getString("seller_display_name"), rs.getString("seller_avatar_url"), rs.getString("seller_role")),
                id, userId, userId);
        if (values.isEmpty()) throw new ApiException(403, "Conversation not found or access denied");
        return values.get(0);
    }

    private Conversation mapConversation(java.sql.ResultSet rs, long userId) throws java.sql.SQLException {
        long buyer = rs.getLong("buyer_user_id"), seller = rs.getLong("seller_user_id");
        Participant buyerView = new Participant(buyer, fallback(rs.getString("buyer_display_name"), buyer),
                rs.getString("buyer_avatar_url"), rs.getString("buyer_role"));
        Participant sellerView = new Participant(seller, fallback(rs.getString("seller_display_name"), seller),
                rs.getString("seller_avatar_url"), rs.getString("seller_role"));
        Timestamp last = rs.getTimestamp("last_message_time");
        return new Conversation(rs.getLong("id"), rs.getString("source_type"), rs.getLong("source_id"),
                rs.getString("source_title"), rs.getString("last_message_content"),
                last == null ? null : last.toLocalDateTime(), rs.getInt("unread_count"),
                userId == buyer ? buyerView : sellerView, userId == buyer ? sellerView : buyerView);
    }

    private Participant participant(ConversationRow row, long userId) {
        if (row.buyerId() == userId) return new Participant(userId, fallback(row.buyerName(), userId), row.buyerAvatar(), row.buyerRole());
        return new Participant(userId, fallback(row.sellerName(), userId), row.sellerAvatar(), row.sellerRole());
    }

    private Participants participants(AccessSnapshot actor, AccessSnapshot target, String type) {
        if (isSeller(actor.role()) && !isSeller(target.role())) return new Participants(target, actor);
        if (!isSeller(actor.role()) && isSeller(target.role())) return new Participants(actor, target);
        if (actor.userId() > target.userId()) return new Participants(target, actor);
        return new Participants(actor, target);
    }
    private boolean isSeller(String role) { return "SELLER".equalsIgnoreCase(role) || "OFFICIAL_SELLER".equalsIgnoreCase(role); }
    private String normalizeType(String value) {
        String type = value == null || value.isBlank() ? "DIRECT" : value.trim().toUpperCase(Locale.ROOT);
        if (!SOURCE_TYPES.contains(type)) throw new ApiException(400, "Unsupported conversation source type");
        return type;
    }
    private long normalizeSourceId(String type, Long id) {
        if ("DIRECT".equals(type)) return id == null ? 0 : id;
        if (id == null || id <= 0) throw new ApiException(400, "sourceId is required");
        return id;
    }
    private String normalizeTitle(String type, String value) {
        if (value != null && !value.isBlank()) return value.trim();
        return switch (type) { case "PRODUCT" -> "Product inquiry"; case "SECONDHAND" -> "Secondhand inquiry"; default -> "Direct chat"; };
    }
    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) throw new ApiException(400, "Message content must not be empty");
        String value = content.trim();
        if (value.length() > 1000) throw new ApiException(400, "Message content must not exceed 1000 characters");
        return value;
    }
    private String fallback(String value, long id) { return value == null || value.isBlank() ? "User " + id : value; }
    private String text(String value) { return value == null ? "" : value; }

    private static final String CONVERSATION_SELECT = "select c.*, (select count(*) from chat_message m " +
            "where m.conversation_id=c.id and m.receiver_user_id=? and m.is_read=0) unread_count from chat_conversation c";
    private record Participants(AccessSnapshot buyer, AccessSnapshot seller) {}
    private record ConversationRow(long id, long buyerId, long sellerId, String buyerName, String buyerAvatar,
            String buyerRole, String sellerName, String sellerAvatar, String sellerRole) {}
}
