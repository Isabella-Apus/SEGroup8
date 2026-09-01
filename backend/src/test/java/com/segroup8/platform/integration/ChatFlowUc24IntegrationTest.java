package com.segroup8.platform.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.realtime.RealtimePushService;
import com.segroup8.platform.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "classpath:integration/uc24-chat-setup.sql")
@Tag("DOMAIN_E")
@Tag("UC24")
class ChatFlowUc24IntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate db;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private RealtimePushService realtimePushService;

    private String buyerToken;
    private String sellerToken;
    private String outsiderToken;

    @BeforeEach
    void createTokens() {
        buyerToken = jwtUtils.createToken(2401L, "uc24_buyer", "USER");
        sellerToken = jwtUtils.createToken(2402L, "uc24_seller", "OFFICIAL_SELLER");
        outsiderToken = jwtUtils.createToken(2403L, "uc24_outsider", "USER");
    }

    @Test
    void duplicateCreationReturnsOneConversationToBothParticipantsOnly() throws Exception {
        long firstId = createProductConversation(buyerToken, 2402L);
        long repeatedId = createProductConversation(sellerToken, 2401L);

        assertEquals(firstId, repeatedId);
        assertEquals(1, count("select count(*) from chat_conversation where id = ?", firstId));

        mvc.perform(get("/api/chat/conversations").header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(firstId))
                .andExpect(jsonPath("$.data[0].other.userId").value(2402));

        mvc.perform(get("/api/chat/conversations").header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(firstId))
                .andExpect(jsonPath("$.data[0].other.userId").value(2401));

        mvc.perform(get("/api/chat/conversations").header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void bidirectionalHistoryReadStateNotificationsAndIsolationPersist() throws Exception {
        long conversationId = createProductConversation(buyerToken, 2402L);
        send(conversationId, buyerToken, "buyer message", 2402L);

        mvc.perform(get("/api/chat/conversations").header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].unreadCount").value(1));

        mvc.perform(get("/api/chat/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(sellerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("buyer message"))
                .andExpect(jsonPath("$.data[0].isRead").value(1));

        send(conversationId, sellerToken, "seller reply", 2401L);

        mvc.perform(get("/api/chat/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(buyerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].content").value("buyer message"))
                .andExpect(jsonPath("$.data[1].content").value("seller reply"))
                .andExpect(jsonPath("$.data[1].isRead").value(1));

        assertEquals(2, count("select count(*) from chat_message where conversation_id = ?", conversationId));
        assertEquals(1, count("select count(*) from notification where user_id = 2401"));
        assertEquals(1, count("select count(*) from notification where user_id = 2402"));

        mvc.perform(get("/api/chat/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        mvc.perform(post("/api/chat/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"outsider leak\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
        assertEquals(2, count("select count(*) from chat_message where conversation_id = ?", conversationId));

        verify(realtimePushService, atLeastOnce())
                .pushToUser(eq(2402L), eq("NOTIFICATION_CREATED"), any());
        verify(realtimePushService, atLeastOnce())
                .pushToUsers(eq(List.of(2401L, 2402L)), eq("CHAT_MESSAGE"), any());
    }

    @Test
    void invalidContentAndEitherDirectionBlockLeaveNoMessage() throws Exception {
        long conversationId = createProductConversation(buyerToken, 2402L);

        rejectValidationMessage(conversationId, buyerToken, "   ");
        rejectValidationMessage(conversationId, buyerToken, "x".repeat(1001));

        db.update("insert into user_block(blocker_id, blocked_id, create_time) values(2401, 2402, current_timestamp)");
        rejectMessage(conversationId, sellerToken, "blocked by buyer", 403);
        db.update("delete from user_block where blocker_id = 2401 and blocked_id = 2402");
        db.update("insert into user_block(blocker_id, blocked_id, create_time) values(2402, 2401, current_timestamp)");
        rejectMessage(conversationId, buyerToken, "blocked by seller", 403);

        assertEquals(0, count("select count(*) from chat_message where conversation_id = ?", conversationId));
        assertEquals(0, count("select count(*) from notification where user_id in (2401, 2402)"));
    }

    @Test
    void realtimeFailureKeepsTheMessageAndNotificationCommitted() throws Exception {
        long conversationId = createProductConversation(buyerToken, 2402L);
        doThrow(new IllegalStateException("realtime unavailable"))
                .when(realtimePushService)
                .pushToUsers(any(), eq("CHAT_MESSAGE"), any());

        mvc.perform(post("/api/chat/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(buyerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"persist despite push failure\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("persist despite push failure"));

        assertEquals(1, count("select count(*) from chat_message where conversation_id = ?", conversationId));
        assertEquals(1, count("select count(*) from notification where user_id = 2402"));
        assertTrue(count("select count(*) from chat_message where content = ?", "persist despite push failure") == 1);
    }

    private long createProductConversation(String token, long targetUserId) throws Exception {
        MvcResult result = mvc.perform(post("/api/chat/conversations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":" + targetUserId
                                + ",\"sourceType\":\"PRODUCT\",\"sourceId\":2401}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sourceType").value("PRODUCT"))
                .andReturn();
        return responseData(result).path("id").asLong();
    }

    private void send(long conversationId, String token, String content, long receiverId) throws Exception {
        mvc.perform(post("/api/chat/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value(content))
                .andExpect(jsonPath("$.data.receiverUserId").value(receiverId));
    }

    private void rejectMessage(long conversationId, String token, String content, int code) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("content", content));
        mvc.perform(post("/api/chat/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code));
    }

    private void rejectValidationMessage(long conversationId, String token, String content) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("content", content));
        mvc.perform(post("/api/chat/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private int count(String sql, Object... args) {
        return db.queryForObject(sql, Integer.class, args);
    }

    private JsonNode responseData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
