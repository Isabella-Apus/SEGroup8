package com.segroup8.platform.interceptor;

import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.IdempotencyRecord;
import com.segroup8.platform.mapper.IdempotencyRecordMapper;
import com.segroup8.platform.testsupport.DomainCTestTags;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@Tag(DomainCTestTags.DOMAIN_C)
@Tag(DomainCTestTags.PLATFORM)
class IdempotencyInterceptorTest {

    private final IdempotencyRecordMapper mapper = Mockito.mock(IdempotencyRecordMapper.class);
    private final IdempotencyInterceptor interceptor = new IdempotencyInterceptor(mapper);

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void shouldAllowFirstRequestAndReplayDuplicateResult() throws Exception {
        UserContext.setUserId(1001L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/order/1/pay");
        request.addHeader("X-Idempotency-Key", "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Mockito.when(mapper.selectOne(Mockito.any())).thenReturn(null);

        boolean first = interceptor.preHandle(request, response, new Object());
        Assertions.assertTrue(first);

        IdempotencyRecord recorded = new IdempotencyRecord();
        recorded.setStatus(1);
        recorded.setHttpStatus(200);
        recorded.setResponseBody("{\"code\":0,\"message\":\"success\",\"data\":{\"id\":1}}");
        recorded.setExpireTime(java.time.LocalDateTime.now().plusSeconds(10));
        Mockito.when(mapper.selectOne(Mockito.any())).thenReturn(recorded);
        Mockito.when(mapper.insert(Mockito.any(IdempotencyRecord.class))).thenThrow(new DuplicateKeyException("dup"));

        boolean second = interceptor.preHandle(request, response, new Object());
        Assertions.assertFalse(second);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(response.getContentAsString().contains("\"code\":0"));
        Assertions.assertEquals("SUCCESS", response.getHeader("X-Idempotency-Replay"));
    }

    @Test
    void shouldReturnSuccessEnvelopeWhenDuplicateStillProcessing() throws Exception {
        UserContext.setUserId(1002L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/order/2/refund");
        request.addHeader("X-Idempotency-Key", "xyz-456");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Mockito.when(mapper.selectOne(Mockito.any())).thenReturn(null);
        Assertions.assertTrue(interceptor.preHandle(request, response, new Object()));

        IdempotencyRecord processing = new IdempotencyRecord();
        processing.setStatus(0);
        processing.setExpireTime(java.time.LocalDateTime.now().plusSeconds(10));
        Mockito.when(mapper.selectOne(Mockito.any())).thenReturn(processing);
        Mockito.when(mapper.insert(Mockito.any(IdempotencyRecord.class))).thenThrow(new DuplicateKeyException("dup"));

        boolean second = interceptor.preHandle(request, response, new Object());
        Assertions.assertFalse(second);
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertTrue(response.getContentAsString().contains("\"code\":0"));
        Assertions.assertEquals("PROCESSING", response.getHeader("X-Idempotency-Replay"));
    }

    @Test
    void shouldIgnoreWhenHeaderMissing() {
        UserContext.setUserId(1001L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/order/1/pay");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Assertions.assertTrue(interceptor.preHandle(request, response, new Object()));
        Assertions.assertTrue(interceptor.preHandle(request, response, new Object()));
    }
}
