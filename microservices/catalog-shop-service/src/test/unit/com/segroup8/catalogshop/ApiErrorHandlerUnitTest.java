package com.segroup8.catalogshop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@Tag("DOMAIN_B")
class ApiErrorHandlerUnitTest {
    @Test
    void preservesDomainErrorContract() {
        var response = new ApiErrorHandler().domain(new ApiException("PRODUCT_NOT_FOUND", "商品不存在", HttpStatus.NOT_FOUND));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("PRODUCT_NOT_FOUND", response.getBody().get("code"));
    }
}
