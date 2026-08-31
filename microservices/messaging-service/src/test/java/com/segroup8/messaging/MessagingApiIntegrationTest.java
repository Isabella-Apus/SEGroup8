package com.segroup8.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.segroup8.messaging.access.GovernanceBlockPort;
import com.segroup8.messaging.realtime.RealtimePublisher;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MessagingApiIntegrationTest {
    private static final String SECRET = "TEST_ONLY_MESSAGING_SECRET_12345678901234567890";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockBean RealtimePublisher realtime;
    @MockBean GovernanceBlockPort governanceBlocks;

    @BeforeEach
    void seed() {
        jdbc.update("delete from chat_message");
        jdbc.update("delete from chat_conversation");
        jdbc.update("delete from notification");
        jdbc.update("delete from user_block_projection");
        jdbc.update("delete from user_access_projection");
        jdbc.update("insert into user_access_projection(user_id,access_status,role,display_name,source_version) values " +
                "(1,'ACTIVE','USER','Alice',1),(2,'ACTIVE','SELLER','Bob',1),(3,'ACTIVE','USER','Mallory',1),(4,'BANNED','USER','Banned',1)");
        jdbc.update("insert into user_block_projection(blocker_user_id,blocked_user_id,active,source_version) values " +
                "(1,2,0,1),(2,1,0,1),(1,3,0,1),(3,1,0,1),(1,4,0,1),(4,1,0,1)");
        jdbc.update("insert into chat_conversation(id,buyer_user_id,seller_user_id,buyer_display_name,buyer_role," +
                "seller_display_name,seller_role,source_type,source_id,source_title) values " +
                "(10,1,2,'Alice','USER','Bob','SELLER','DIRECT',0,'Direct chat')");
        jdbc.update("insert into chat_message(id,conversation_id,sender_user_id,receiver_user_id,content,is_read) values " +
                "(100,10,2,1,'hello',0)");
        jdbc.update("insert into notification(id,user_id,title,content,scope,is_read) values " +
                "(20,1,'Mine','hello','buyer',0),(21,2,'Other','secret','buyer',0),(22,1,'Seller notice','ship','seller',0)");
    }

    @Test
    void securityRejectsMissingInvalidExpiredAndAcceptsValidJwt() throws Exception {
        mvc.perform(get("/api/chat/conversations")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat/conversations").header("Authorization", "Bearer broken"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat/conversations").header("Authorization", bearer(tokenWithoutUid())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat/conversations").header("Authorization", bearer(token(1, -60))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/chat/conversations").header("Authorization", bearer(token(1, 3600))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    void participantCanReadAndSendButNonParticipantCannot() throws Exception {
        mvc.perform(get("/api/chat/conversations/10/messages").header("Authorization", bearer(token(1, 3600))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].content").value("hello"));
        mvc.perform(get("/api/chat/conversations/10/messages").header("Authorization", bearer(token(3, 3600))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/chat/conversations/10/messages").header("Authorization", bearer(token(1, 3600)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"new message\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.receiverUserId").value(2));
        mvc.perform(post("/api/chat/conversations/10/messages").header("Authorization", bearer(token(3, 3600)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"intrusion\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/chat/conversations/10/messages").header("Authorization", bearer(token(1, 3600)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void conversationCreationRejectsSelfBlockBanAndUnknownBlockState() throws Exception {
        mvc.perform(post("/api/chat/conversations").header("Authorization", bearer(token(1, 3600)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetUserId\":1}"))
                .andExpect(status().isBadRequest());
        jdbc.update("update user_block_projection set active=1 where blocker_user_id=2 and blocked_user_id=1");
        mvc.perform(post("/api/chat/conversations").header("Authorization", bearer(token(1, 3600)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetUserId\":2}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/chat/conversations").header("Authorization", bearer(token(4, 3600)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetUserId\":1}"))
                .andExpect(status().isForbidden());
        jdbc.update("delete from user_block_projection where blocker_user_id=3 and blocked_user_id=1");
        mvc.perform(post("/api/chat/conversations").header("Authorization", bearer(token(1, 3600)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetUserId\":3}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void conversationCreationUsesResolvedTargetAndLocalSnapshots() throws Exception {
        mvc.perform(post("/api/chat/conversations").header("Authorization", bearer(token(1, 3600)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":2,\"sourceType\":\"PRODUCT\",\"sourceId\":55,\"sourceTitle\":\"Book\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.sourceTitle").value("Book"))
                .andExpect(jsonPath("$.data.other.nickname").value("Bob"));
        org.junit.jupiter.api.Assertions.assertEquals("Book", jdbc.queryForObject(
                "select source_title from chat_conversation where source_type='PRODUCT' and source_id=55", String.class));
    }

    @Test
    void conversationCreationIsIdempotentWhenSellerInitiatesTheSameSource() throws Exception {
        mvc.perform(post("/api/chat/conversations").header("Authorization", bearer(token(1, 3600)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":2,\"sourceType\":\"PRODUCT\",\"sourceId\":55}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").isNumber());
        mvc.perform(post("/api/chat/conversations").header("Authorization", bearer(token(2, 3600)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetUserId\":1,\"sourceType\":\"PRODUCT\",\"sourceId\":55}"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(1, jdbc.queryForObject(
                "select count(*) from chat_conversation where source_type='PRODUCT' and source_id=55", Integer.class));
    }

    @Test
    void sendingFailsWhenEitherDirectionIsBlocked() throws Exception {
        jdbc.update("update user_block_projection set active=1 where blocker_user_id=1 and blocked_user_id=2");
        mvc.perform(post("/api/chat/conversations/10/messages").header("Authorization", bearer(token(1, 3600)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"blocked\"}"))
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals(0,
                jdbc.queryForObject("select count(*) from chat_message where content='blocked'", Integer.class));
    }

    @Test
    void migratedInactiveProjectionUsesGovernanceFallbackAndFailsClosedOnBlock() throws Exception {
        jdbc.update("update user_block_projection set source_version=0 where "
                + "(blocker_user_id=1 and blocked_user_id=2) or (blocker_user_id=2 and blocked_user_id=1)");
        when(governanceBlocks.isCommunicationBlocked(anyLong(), anyLong()))
                .thenReturn(Optional.of(true));
        mvc.perform(post("/api/chat/conversations/10/messages").header("Authorization", bearer(token(1, 3600)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"stale allowed state\"}"))
                .andExpect(status().isForbidden());
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbc.queryForObject(
                "select count(*) from chat_message where content='stale allowed state'", Integer.class));
    }

    @Test
    void notificationOwnershipAndScopedReadAllAreEnforced() throws Exception {
        mvc.perform(get("/api/notifications").header("Authorization", bearer(token(1, 3600))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(post("/api/notifications/21/read").header("Authorization", bearer(token(1, 3600))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/notifications/20/read").header("Authorization", bearer(token(1, 3600))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/notifications/read-all?scope=seller").header("Authorization", bearer(token(1, 3600))))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(0,
                jdbc.queryForObject("select is_read from notification where id=21", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbc.queryForObject("select is_read from notification where id=22", Integer.class));
    }

    @Test
    void pushFailureCannotRollbackChatOrNotificationPersistence() throws Exception {
        doThrow(new RuntimeException("simulated websocket failure")).when(realtime)
                .pushToUser(anyLong(), anyString(), any());
        doThrow(new RuntimeException("simulated websocket failure")).when(realtime)
                .pushToUsers(any(), anyString(), any());
        mvc.perform(post("/api/chat/conversations/10/messages").header("Authorization", bearer(token(1, 3600)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"durable\"}"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbc.queryForObject("select count(*) from chat_message where content='durable'", Integer.class));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                jdbc.queryForObject("select count(*) from notification where user_id=2 and title='新消息'", Integer.class));
    }

    private String bearer(String token) { return "Bearer " + token; }
    private String token(long uid, long validForSeconds) {
        Instant now = Instant.now();
        return Jwts.builder().claim("uid", uid).claim("username", "u" + uid).claim("role", "USER")
                .issuedAt(Date.from(now.minusSeconds(120))).expiration(Date.from(now.plusSeconds(validForSeconds)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
    private String tokenWithoutUid() {
        Instant now = Instant.now();
        return Jwts.builder().claim("username", "missing").claim("role", "USER")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
