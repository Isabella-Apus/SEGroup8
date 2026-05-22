package com.segroup8.platform.service.impl;

import com.segroup8.platform.common.AccessControl;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.entity.ChatConversation;
import com.segroup8.platform.entity.ChatMessage;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.ChatConversationMapper;
import com.segroup8.platform.mapper.ChatMessageMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.SecondhandTradeService;
import com.segroup8.platform.vo.PageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class SecondhandTradeServiceImpl implements SecondhandTradeService {

    private static final int ON_SHELF = 1;
    private static final String PENDING = "PENDING";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String REJECTED = "REJECTED";
    private static final String USED = "USED";
    private static final String ORDER_CREATED = "ORDER_CREATED";
    private static final String ONGOING = "ONGOING";
    private static final String FINISHED = "FINISHED";
    private static final String FLOW = "FLOW";
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final SecondhandProductMapper secondhandProductMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final ChatConversationMapper chatConversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AtomicLong negotiationId = new AtomicLong(1);
    private final AtomicLong auctionId = new AtomicLong(1);
    private final Map<Long, Map<String, Object>> negotiations = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Object>> auctions = new ConcurrentHashMap<>();

    public SecondhandTradeServiceImpl(SecondhandProductMapper secondhandProductMapper,
                                      OrderInfoMapper orderInfoMapper,
                                      OrderItemMapper orderItemMapper,
                                      UserMapper userMapper,
                                      ChatConversationMapper chatConversationMapper,
                                      ChatMessageMapper chatMessageMapper) {
        this.secondhandProductMapper = secondhandProductMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.chatConversationMapper = chatConversationMapper;
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyBargain(Map<String, Object> request) {
        Long buyerUserId = AccessControl.requireUserId();
        Long productId = asLong(request.get("productId"));
        BigDecimal proposedPrice = asMoney(request.get("proposedPrice"));
        if (productId == null || proposedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "议价参数不完整");
        }
        SecondhandProduct product = requireTradableProduct(productId);
        if (Objects.equals(product.getSellerUserId(), buyerUserId)) {
            throw new BusinessException(400, "不能向自己的二手商品议价");
        }
        if (isAuctionOngoing(productId)) {
            throw new BusinessException(400, "该商品正在拍卖中，暂不能议价");
        }
        Map<String, Object> existing = negotiations.values().stream()
                .filter(item -> Objects.equals(asLong(item.get("productId")), productId))
                .filter(item -> Objects.equals(asLong(item.get("buyerUserId")), buyerUserId))
                .filter(item -> List.of(PENDING, CONFIRMED).contains(asText(item.get("status"))))
                .findFirst()
                .orElse(null);
        Map<String, Object> row = existing == null ? new LinkedHashMap<>() : existing;
        if (existing == null) {
            row.put("id", negotiationId.getAndIncrement());
            row.put("productId", productId);
            row.put("sellerUserId", product.getSellerUserId());
            row.put("buyerUserId", buyerUserId);
            row.put("createTime", LocalDateTime.now());
        }
        row.put("proposedPrice", proposedPrice);
        row.put("confirmedPrice", null);
        row.put("status", PENDING);
        row.put("statusName", "待卖家确认");
        row.put("updateTime", LocalDateTime.now());
        negotiations.put(asLong(row.get("id")), row);
        appendBargainChatMessage(product, buyerUserId, proposedPrice);
        return withProductInfo(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmBargain(Map<String, Object> request) {
        Long sellerUserId = AccessControl.requireUserId();
        Long id = asLong(request.get("negotiationId"));
        Map<String, Object> row = requireNegotiation(id);
        if (!Objects.equals(asLong(row.get("sellerUserId")), sellerUserId)) {
            throw new BusinessException(403, "无权确认该议价");
        }
        if (!PENDING.equals(asText(row.get("status")))) {
            throw new BusinessException(400, "当前议价不可确认");
        }
        BigDecimal confirmedPrice = asMoney(request.getOrDefault("confirmedPrice", row.get("proposedPrice")));
        Long orderId = createNegotiatedOrder(row, confirmedPrice);
        row.put("confirmedPrice", confirmedPrice);
        row.put("status", ORDER_CREATED);
        row.put("statusName", "卖家已同意，订单已创建");
        row.put("orderId", orderId);
        row.put("updateTime", LocalDateTime.now());
        appendNegotiationDecisionChatMessage(
                row,
                "我已同意 ¥" + confirmedPrice.setScale(2, RoundingMode.HALF_UP).toPlainString()
                        + " 的议价，系统已生成二手订单，请到二手订单里查看。");
        return withProductInfo(row);
    }

    @Override
    public PageVO<Map<String, Object>> pageBargains(Long pageNum, Long pageSize, Long productId, Long counterpartUserId, String status) {
        Long currentUserId = AccessControl.requireUserId();
        List<String> statuses = status == null || status.isBlank()
                ? List.of()
                : Arrays.stream(status.split(",")).map(String::trim).filter(item -> !item.isBlank()).collect(Collectors.toList());
        List<Map<String, Object>> records = negotiations.values().stream()
                .filter(item -> Objects.equals(asLong(item.get("sellerUserId")), currentUserId)
                        || Objects.equals(asLong(item.get("buyerUserId")), currentUserId))
                .filter(item -> productId == null || Objects.equals(asLong(item.get("productId")), productId))
                .filter(item -> counterpartUserId == null
                        || Objects.equals(asLong(item.get("sellerUserId")), counterpartUserId)
                        || Objects.equals(asLong(item.get("buyerUserId")), counterpartUserId))
                .filter(item -> statuses.isEmpty() || statuses.contains(asText(item.get("status"))))
                .sorted(Comparator.comparing(item -> asLocalDateTime(((Map<String, Object>) item).get("updateTime"))).reversed())
                .map(this::withProductInfo)
                .collect(Collectors.toList());
        return paginate(records, pageNum, pageSize);
    }

    @Override
    public Map<String, Object> rejectBargain(Long negotiationId) {
        Long sellerUserId = AccessControl.requireUserId();
        Map<String, Object> row = requireNegotiation(negotiationId);
        if (!Objects.equals(asLong(row.get("sellerUserId")), sellerUserId)) {
            throw new BusinessException(403, "无权拒绝该议价");
        }
        row.put("status", REJECTED);
        row.put("statusName", "卖家已拒绝");
        row.put("updateTime", LocalDateTime.now());
        appendNegotiationDecisionChatMessage(
                row,
                "这次 ¥" + asMoney(row.get("proposedPrice")).setScale(2, RoundingMode.HALF_UP).toPlainString()
                        + " 的议价我先不接受，后续有需要我们再沟通。");
        return withProductInfo(row);
    }

    @Override
    public Map<String, Object> getMyEffectiveBargain(Long productId) {
        Long buyerUserId = AccessControl.requireUserId();
        return negotiations.values().stream()
                .filter(item -> Objects.equals(asLong(item.get("productId")), productId))
                .filter(item -> Objects.equals(asLong(item.get("buyerUserId")), buyerUserId))
                .filter(item -> CONFIRMED.equals(asText(item.get("status"))))
                .filter(item -> item.get("orderId") == null)
                .findFirst()
                .map(this::withProductInfo)
                .orElse(null);
    }

    @Override
    public Map<String, Object> createAuction(Map<String, Object> request) {
        Long sellerUserId = AccessControl.requireUserId();
        Long productId = asLong(request.get("productId"));
        BigDecimal startPrice = asMoney(request.get("startPrice"));
        BigDecimal incrementAmount = asMoney(request.getOrDefault("incrementAmount", BigDecimal.ONE));
        long durationMinutes = Math.max(10L, asLong(request.getOrDefault("durationMinutes", 60L)));
        SecondhandProduct product = requireTradableProduct(productId);
        if (!Objects.equals(product.getSellerUserId(), sellerUserId)) {
            throw new BusinessException(403, "只能为自己发布的二手商品发起拍卖");
        }
        if (isAuctionOngoing(productId)) {
            throw new BusinessException(400, "该商品已有进行中的拍卖");
        }
        if (startPrice.compareTo(BigDecimal.ZERO) <= 0 || incrementAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "拍卖金额必须大于0");
        }
        Long id = auctionId.getAndIncrement();
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("productId", productId);
        row.put("sellerUserId", sellerUserId);
        row.put("startPrice", startPrice);
        row.put("incrementAmount", incrementAmount);
        row.put("currentPrice", startPrice);
        row.put("currentBidderUserId", null);
        row.put("bidCount", 0);
        row.put("status", ONGOING);
        row.put("statusName", "进行中");
        row.put("startTime", LocalDateTime.now());
        row.put("endTime", LocalDateTime.now().plusMinutes(durationMinutes));
        row.put("createTime", LocalDateTime.now());
        auctions.put(id, row);
        return withAuctionInfo(row);
    }

    @Override
    public Map<String, Object> getAuctionByProductId(Long productId) {
        return auctions.values().stream()
                .filter(item -> Objects.equals(asLong(item.get("productId")), productId))
                .peek(this::settleAuctionIfNeeded)
                .sorted(Comparator.comparing(item -> asLong(((Map<String, Object>) item).get("id"))).reversed())
                .findFirst()
                .map(this::withAuctionInfo)
                .orElse(null);
    }

    @Override
    public PageVO<Map<String, Object>> pageMyAuctions(Long pageNum, Long pageSize, String status) {
        Long sellerUserId = AccessControl.requireUserId();
        List<Map<String, Object>> records = auctions.values().stream()
                .peek(this::settleAuctionIfNeeded)
                .filter(item -> Objects.equals(asLong(item.get("sellerUserId")), sellerUserId))
                .filter(item -> status == null || status.isBlank() || status.equals(asText(item.get("status"))))
                .sorted(Comparator.comparing(item -> asLong(((Map<String, Object>) item).get("id"))).reversed())
                .map(this::withAuctionInfo)
                .collect(Collectors.toList());
        return paginate(records, pageNum, pageSize);
    }

    @Override
    public Map<String, Object> closeAuctionEarly(Long auctionId) {
        Long sellerUserId = AccessControl.requireUserId();
        Map<String, Object> row = requireAuction(auctionId);
        if (!Objects.equals(asLong(row.get("sellerUserId")), sellerUserId)) {
            throw new BusinessException(403, "无权操作该拍卖");
        }
        row.put("endTime", LocalDateTime.now());
        settleAuctionIfNeeded(row);
        return withAuctionInfo(row);
    }

    @Override
    public Map<String, Object> markAuctionFlow(Long auctionId) {
        Long sellerUserId = AccessControl.requireUserId();
        Map<String, Object> row = requireAuction(auctionId);
        if (!Objects.equals(asLong(row.get("sellerUserId")), sellerUserId)) {
            throw new BusinessException(403, "无权操作该拍卖");
        }
        row.put("status", FLOW);
        row.put("statusName", "已流拍");
        row.put("endTime", LocalDateTime.now());
        return withAuctionInfo(row);
    }

    @Override
    public Map<String, Object> placeBid(Long auctionId, Map<String, Object> request) {
        Long bidderUserId = AccessControl.requireUserId();
        Map<String, Object> row = requireAuction(auctionId);
        settleAuctionIfNeeded(row);
        if (!ONGOING.equals(asText(row.get("status")))) {
            throw new BusinessException(400, "该拍卖已结束");
        }
        if (Objects.equals(asLong(row.get("sellerUserId")), bidderUserId)) {
            throw new BusinessException(400, "卖家不能参与自己的拍卖");
        }
        BigDecimal bidAmount = asMoney(request.get("bidAmount"));
        BigDecimal minBid = minBid(row);
        if (bidAmount.compareTo(minBid) < 0) {
            throw new BusinessException(400, "出价不能低于 " + minBid);
        }
        row.put("currentPrice", bidAmount);
        row.put("currentBidderUserId", bidderUserId);
        row.put("bidCount", asLong(row.getOrDefault("bidCount", 0L)) + 1L);
        row.put("updateTime", LocalDateTime.now());
        return withAuctionInfo(row);
    }

    private SecondhandProduct requireTradableProduct(Long productId) {
        SecondhandProduct product = productId == null ? null : secondhandProductMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(404, "二手商品不存在");
        }
        if (!Objects.equals(product.getStatus(), ON_SHELF)) {
            throw new BusinessException(400, "二手商品已下架或已售出");
        }
        return product;
    }

    private Map<String, Object> requireNegotiation(Long id) {
        Map<String, Object> row = id == null ? null : negotiations.get(id);
        if (row == null) {
            throw new BusinessException(404, "议价记录不存在");
        }
        return row;
    }

    private Map<String, Object> requireAuction(Long id) {
        Map<String, Object> row = id == null ? null : auctions.get(id);
        if (row == null) {
            throw new BusinessException(404, "拍卖不存在");
        }
        return row;
    }

    private void appendBargainChatMessage(SecondhandProduct product, Long buyerUserId, BigDecimal proposedPrice) {
        if (product == null || product.getSellerUserId() == null || buyerUserId == null) {
            return;
        }
        String content = "你好，我对「" + product.getName() + "」出价 ¥"
                + proposedPrice.setScale(2, RoundingMode.HALF_UP).toPlainString()
                + "，可以考虑一下吗？";
        appendSecondhandChatMessage(product, buyerUserId, buyerUserId, product.getSellerUserId(), content);
    }

    private void appendNegotiationDecisionChatMessage(Map<String, Object> negotiation, String content) {
        Long productId = asLong(negotiation.get("productId"));
        SecondhandProduct product = productId == null ? null : secondhandProductMapper.selectById(productId);
        Long buyerUserId = asLong(negotiation.get("buyerUserId"));
        Long sellerUserId = asLong(negotiation.get("sellerUserId"));
        appendSecondhandChatMessage(product, buyerUserId, sellerUserId, buyerUserId, content);
    }

    private void appendSecondhandChatMessage(SecondhandProduct product, Long buyerUserId, Long senderUserId,
            Long receiverUserId, String content) {
        if (product == null || buyerUserId == null || senderUserId == null || receiverUserId == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        ChatConversation conversation = chatConversationMapper.selectOne(new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getBuyerUserId, buyerUserId)
                .eq(ChatConversation::getSellerUserId, product.getSellerUserId())
                .eq(ChatConversation::getSourceType, "SECONDHAND")
                .eq(ChatConversation::getSourceId, product.getId())
                .last("limit 1"));
        if (conversation == null) {
            conversation = new ChatConversation();
            conversation.setBuyerUserId(buyerUserId);
            conversation.setSellerUserId(product.getSellerUserId());
            conversation.setSourceType("SECONDHAND");
            conversation.setSourceId(product.getId());
            conversation.setSourceTitle(product.getName());
            conversation.setCreateTime(now);
            conversation.setUpdateTime(now);
            chatConversationMapper.insert(conversation);
        }

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderUserId(senderUserId);
        message.setReceiverUserId(receiverUserId);
        message.setContent(content);
        message.setIsRead(0);
        message.setCreateTime(now);
        chatMessageMapper.insert(message);

        conversation.setSourceTitle(product.getName());
        conversation.setLastMessageContent(content);
        conversation.setLastMessageTime(now);
        conversation.setUpdateTime(now);
        chatConversationMapper.updateById(conversation);
    }

    private Long createNegotiatedOrder(Map<String, Object> negotiation, BigDecimal confirmedPrice) {
        Long productId = asLong(negotiation.get("productId"));
        Long buyerUserId = asLong(negotiation.get("buyerUserId"));
        return createSecondhandPendingOrder(productId, buyerUserId, confirmedPrice, "议价成交");
    }

    private Long createAuctionOrder(Map<String, Object> auction) {
        Long productId = asLong(auction.get("productId"));
        Long buyerUserId = asLong(auction.get("currentBidderUserId"));
        if (productId == null || buyerUserId == null) {
            return null;
        }
        BigDecimal finalPrice = asMoney(auction.getOrDefault("currentPrice", auction.get("startPrice")));
        return createSecondhandPendingOrder(productId, buyerUserId, finalPrice, "拍卖成交");
    }

    private Long createSecondhandPendingOrder(Long productId, Long buyerUserId, BigDecimal price, String remarkPrefix) {
        SecondhandProduct product = requireTradableProduct(productId);
        int updated = secondhandProductMapper.update(null, new UpdateWrapper<SecondhandProduct>()
                .set("status", 0)
                .eq("id", productId)
                .eq("status", ON_SHELF));
        if (updated == 0) {
            throw new BusinessException(400, "二手商品已售出");
        }

        OrderInfo order = new OrderInfo();
        order.setOrderNo(generateOrderNo(buyerUserId));
        order.setBuyerUserId(buyerUserId);
        order.setTotalAmount(price);
        order.setPayStatus(0);
        order.setOrderStatus(OrderStatusEnum.PENDING_PAY.getCode());
        order.setCanRefund(1);
        order.setPayMethod("待付款");
        order.setLogisticsStatus("PENDING");
        order.setLogisticsCurrentIndex(0);
        order.setRemark(remarkPrefix + "：" + product.getName());
        order.setCreateTime(LocalDateTime.now());
        orderInfoMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductType("SECONDHAND");
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setPrice(price);
        item.setQuantity(1);
        item.setStatus(1);
        item.setCreateTime(LocalDateTime.now());
        orderItemMapper.insert(item);
        return order.getId();
    }

    private boolean isAuctionOngoing(Long productId) {
        return auctions.values().stream()
                .filter(item -> Objects.equals(asLong(item.get("productId")), productId))
                .peek(this::settleAuctionIfNeeded)
                .anyMatch(item -> ONGOING.equals(asText(item.get("status"))));
    }

    private void settleAuctionIfNeeded(Map<String, Object> auction) {
        if (!ONGOING.equals(asText(auction.get("status")))) {
            return;
        }
        Object endTimeRaw = auction.get("endTime");
        if (!(endTimeRaw instanceof LocalDateTime endTime) || endTime.isAfter(LocalDateTime.now())) {
            return;
        }
        if (auction.get("currentBidderUserId") == null) {
            auction.put("status", FLOW);
            auction.put("statusName", "已流拍");
            return;
        }
        if (auction.get("settledOrderId") == null) {
            Long orderId = createAuctionOrder(auction);
            auction.put("settledOrderId", orderId);
            auction.put("orderId", orderId);
        }
        auction.put("status", FINISHED);
        auction.put("statusName", "已结束");
    }

    private Map<String, Object> withProductInfo(Map<String, Object> row) {
        Map<String, Object> copy = new LinkedHashMap<>(row);
        SecondhandProduct product = secondhandProductMapper.selectById(asLong(row.get("productId")));
        if (product != null) {
            copy.put("productName", product.getName());
            copy.put("productStatus", product.getStatus());
            copy.put("sellerName", userName(product.getSellerUserId()));
        }
        copy.put("buyerName", userName(asLong(row.get("buyerUserId"))));
        Long orderId = asLong(row.get("orderId"));
        if (orderId != null) {
            OrderInfo order = orderInfoMapper.selectById(orderId);
            if (order != null) {
                copy.put("orderNo", order.getOrderNo());
            }
        }
        return copy;
    }

    private Map<String, Object> withAuctionInfo(Map<String, Object> row) {
        Map<String, Object> copy = withProductInfo(row);
        copy.put("currentBidderName", userName(asLong(row.get("currentBidderUserId"))));
        Long settledOrderId = asLong(row.get("settledOrderId"));
        if (settledOrderId != null) {
            OrderInfo order = orderInfoMapper.selectById(settledOrderId);
            if (order != null) {
                copy.put("settledOrderNo", order.getOrderNo());
            }
        }
        return copy;
    }

    private String generateOrderNo(Long userId) {
        String timePart = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        long userPart = userId == null ? 0L : Math.floorMod(userId, 10000L);
        return "SND" + timePart + String.format("%04d", userPart);
    }

    private String userName(Long userId) {
        if (userId == null) {
            return "";
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return "";
        }
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }

    private BigDecimal minBid(Map<String, Object> auction) {
        BigDecimal current = asMoney(auction.get("currentPrice"));
        if (auction.get("currentBidderUserId") == null) {
            return current;
        }
        return current.add(asMoney(auction.getOrDefault("incrementAmount", BigDecimal.ONE)));
    }

    private PageVO<Map<String, Object>> paginate(List<Map<String, Object>> records, Long pageNum, Long pageSize) {
        long page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int from = (int) Math.min(records.size(), (page - 1) * size);
        int to = (int) Math.min(records.size(), from + size);
        PageVO<Map<String, Object>> vo = new PageVO<>();
        vo.setTotal((long) records.size());
        vo.setPageNum(page);
        vo.setPageSize(size);
        vo.setRecords(new ArrayList<>(records.subList(from, to)));
        return vo;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : Long.valueOf(text);
    }

    private BigDecimal asMoney(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text);
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private LocalDateTime asLocalDateTime(Object value) {
        if (value instanceof LocalDateTime time) {
            return time;
        }
        return LocalDateTime.MIN;
    }
}
