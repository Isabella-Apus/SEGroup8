package com.segroup8.secondhand.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.segroup8.secondhand.client.HttpAddressGateway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@Tag("DOMAIN_D")
@Tag("CONTRACT")
class HttpAddressGatewayContractTest {
    @Test
    void validatesOwnershipThroughIdentityAndParsesTheShippingSnapshot() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://identity.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAddressGateway gateway = new HttpAddressGateway(builder.build(), "internal-test-token");
        server.expect(once(), requestTo("http://identity.test/internal/users/20/addresses/100"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "internal-test-token"))
                .andExpect(header("X-Request-Id", "SECONDHAND:BARGAIN:88:address"))
                .andRespond(withSuccess("""
                        {"code":0,"data":{"addressId":100,"userId":20,"receiverName":"Receiver",
                        "receiverPhone":"13800138000","province":"Guangdong","city":"Shenzhen",
                        "detailAddress":"Nanshan Road"}}
                        """, MediaType.APPLICATION_JSON));

        var snapshot = gateway.requireOwnedAddress(20, 100, "SECONDHAND:BARGAIN:88");
        assertThat(snapshot.receiverName()).isEqualTo("Receiver");
        assertThat(snapshot.detailAddress()).isEqualTo("Nanshan Road");
        server.verify();
    }
}
