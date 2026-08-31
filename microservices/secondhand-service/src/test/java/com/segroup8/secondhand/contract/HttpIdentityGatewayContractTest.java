package com.segroup8.secondhand.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.segroup8.secondhand.client.HttpIdentityGateway;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@Tag("DOMAIN_D")
@Tag("CONTRACT")
class HttpIdentityGatewayContractTest {
    @Test
    void resolvesOwnedAddressSnapshotWithInternalAuthentication() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://identity.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpIdentityGateway gateway = new HttpIdentityGateway(builder.build(), "internal-test-token");
        server.expect(once(), requestTo(
                        "http://identity.test/internal/users/20/address-snapshot?addressId=3"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "internal-test-token"))
                .andExpect(header("X-Request-Id", Matchers.startsWith("secondhand-address-")))
                .andRespond(withSuccess("{\"code\":0,\"message\":\"success\",\"data\":{"
                        + "\"id\":3,\"userId\":20,\"receiverName\":\"Buyer\","
                        + "\"receiverPhone\":\"13800008000\",\"province\":\"Zhejiang\","
                        + "\"city\":\"Hangzhou\",\"detailAddress\":\"West Lake Road 1\"}}",
                        MediaType.APPLICATION_JSON));

        var snapshot = gateway.resolveAddress(20, 3L);
        assertThat(snapshot.addressId()).isEqualTo(3L);
        assertThat(snapshot.userId()).isEqualTo(20L);
        assertThat(snapshot.detailAddress()).isEqualTo("West Lake Road 1");
        server.verify();
    }
}
