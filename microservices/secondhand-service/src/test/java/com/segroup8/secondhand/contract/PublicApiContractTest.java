package com.segroup8.secondhand.contract;

import static com.segroup8.secondhand.support.TestJwt.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.secondhand.support.SecondhandIntegrationSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Tag("CONTRACT")
class PublicApiContractTest extends SecondhandIntegrationSupport {
    private static final Set<String> EXPECTED_PUBLIC_OPERATIONS = Set.of(
            "GET /api/secondhand/list",
            "GET /api/secondhand/detail/{productId}",
            "GET /api/secondhand/seller-public/{sellerUserId}",
            "GET /api/secondhand/seller-public/{sellerUserId}/products",
            "GET /api/secondhand/seller/list",
            "POST /api/secondhand/seller",
            "PUT /api/secondhand/seller/{productId}",
            "DELETE /api/secondhand/seller/{productId}",
            "POST /api/secondhand/seller/{productId}/status",
            "POST /api/secondhand/{productId}/buy",
            "POST /api/secondhand/trade/bargain/apply",
            "POST /api/secondhand/trade/bargain/confirm",
            "POST /api/secondhand/trade/bargain/{negotiationId}/reject",
            "GET /api/secondhand/trade/bargain/list",
            "GET /api/secondhand/trade/bargain/effective",
            "POST /api/secondhand/trade/auction",
            "GET /api/secondhand/trade/auction/product/{productId}",
            "GET /api/secondhand/trade/auction/seller/list",
            "POST /api/secondhand/trade/auction/{auctionId}/close",
            "POST /api/secondhand/trade/auction/{auctionId}/flow",
            "POST /api/secondhand/trade/auction/{auctionId}/bid");

    @Autowired ObjectMapper json;

    @Test
    void runtimeOpenApiContainsExactlyTwentyOnePublicOperations() throws Exception {
        String body = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode paths = json.readTree(body).path("paths");
        Set<String> actual = new TreeSet<>();
        paths.properties().forEach(path -> {
            if (path.getKey().startsWith("/api/secondhand")) {
                path.getValue().properties().stream()
                        .filter(method -> Set.of("get", "post", "put", "delete", "patch").contains(method.getKey()))
                        .forEach(method -> actual.add(method.getKey().toUpperCase() + " " + path.getKey()));
            }
        });
        assertThat(actual).containsExactlyInAnyOrderElementsOf(EXPECTED_PUBLIC_OPERATIONS);
    }

    @Test
    void everyProtectedPublicOperationRejectsAnonymousRequests() throws Exception {
        byte[] empty = json.writeValueAsBytes(java.util.Map.of());
        List<MockHttpServletRequestBuilder> requests = new ArrayList<>(List.of(
                get("/api/secondhand/seller/list"),
                post("/api/secondhand/seller").contentType(MediaType.APPLICATION_JSON).content(empty),
                put("/api/secondhand/seller/1").contentType(MediaType.APPLICATION_JSON).content(empty),
                delete("/api/secondhand/seller/1"),
                post("/api/secondhand/seller/1/status").contentType(MediaType.APPLICATION_JSON).content(empty),
                post("/api/secondhand/1/buy").contentType(MediaType.APPLICATION_JSON).content(empty),
                post("/api/secondhand/trade/bargain/apply").contentType(MediaType.APPLICATION_JSON).content(empty),
                post("/api/secondhand/trade/bargain/confirm").contentType(MediaType.APPLICATION_JSON).content(empty),
                post("/api/secondhand/trade/bargain/1/reject"),
                get("/api/secondhand/trade/bargain/list"),
                get("/api/secondhand/trade/bargain/effective"),
                post("/api/secondhand/trade/auction").contentType(MediaType.APPLICATION_JSON).content(empty),
                get("/api/secondhand/trade/auction/seller/list"),
                post("/api/secondhand/trade/auction/1/close"),
                post("/api/secondhand/trade/auction/1/flow"),
                post("/api/secondhand/trade/auction/1/bid").contentType(MediaType.APPLICATION_JSON).content(empty)));
        for (MockHttpServletRequestBuilder request : requests) {
            mvc.perform(request).andExpect(jsonPath("$.code").value(401));
        }
    }

    @Test
    void flywayActuatorIsNotExposedAndRequestCorrelationHeadersAreReturned() throws Exception {
        String authorization = bearer(10, "operator");
        String links = mvc.perform(get("/actuator").header("Authorization", authorization))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(links).doesNotContain("flyway");
        mvc.perform(get("/actuator/flyway").header("Authorization", authorization))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/secondhand/seller/list")
                        .header("Authorization", bearer(10, "seller"))
                        .header("X-Request-Id", "acceptance-request")
                        .header("X-Trace-Id", "acceptance-trace"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("X-Request-Id", "acceptance-request"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("X-Trace-Id", "acceptance-trace"));
    }
}
