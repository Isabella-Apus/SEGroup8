package com.segroup8.order;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.order.DownstreamGateway.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("CONTRACT")
class PaymentFailureContractTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @MockBean DownstreamGateway downstream;
    @Test void unknownDebitReturnsControlledErrorWhileReadRemainsAvailable() throws Exception {
        when(downstream.reserve(anyString(),anyLong(),anyList())).thenReturn(new Reservation("r",
                List.of(new ProductSnapshot(1,"p",BigDecimal.TEN,1,2,null))));
        when(downstream.quote(anyString(),anyLong(),any(),any())).thenReturn(new Quote(BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO));
        when(downstream.debit(anyString(),anyLong(),any(),any(),any())).thenReturn(RemoteResult.UNKNOWN);
        when(downstream.paymentResult(anyString())).thenReturn(RemoteResult.UNKNOWN,RemoteResult.SUCCEEDED);
        String body="{\"items\":[{\"productId\":1,\"quantity\":1}],\"receiverName\":\"b\",\"receiverPhone\":\"13800008000\",\"receiverProvince\":\"p\",\"receiverCity\":\"c\",\"receiverDetailAddress\":\"d\"}";
        var user=new org.springframework.http.HttpHeaders();user.set("X-User-Id","41");user.set("X-User-Role","USER");
        String created=mvc.perform(post("/api/order/create").headers(user).header("Idempotency-Key","outage-create").contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long id=json.readTree(created).path("data").path("id").asLong();
        mvc.perform(post("/api/order/{id}/pay",id).headers(user).header("Idempotency-Key","outage-pay").contentType("application/json").content("{}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("PAYMENT_TEMPORARILY_UNAVAILABLE"));
        mvc.perform(get("/api/order/detail/{id}",id).headers(user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value(0)).andExpect(jsonPath("$.data.orderStatusKey").value("PAYMENT_PENDING"));
        mvc.perform(post("/api/order/{id}/pay",id).headers(user).header("Idempotency-Key","outage-pay").contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value(1));
        verify(downstream,times(1)).debit(anyString(),anyLong(),any(),any(),any());
    }
}
