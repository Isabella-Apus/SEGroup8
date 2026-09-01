package com.segroup8.messaging.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documents the WebSocket handshake, which Spring MVC does not discover as a REST mapping. */
@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI messagingOpenApi() {
        Operation handshake = new Operation()
                .operationId("realtimeHandshake")
                .description("WebSocket handshake at /ws/realtime; supply JWT token query parameter and an allowed Origin.")
                .responses(new ApiResponses()
                        .addApiResponse("101", new ApiResponse().description("Switching Protocols"))
                        .addApiResponse("401", new ApiResponse().description("Missing or invalid JWT"))
                        .addApiResponse("403", new ApiResponse().description("Banned user or disallowed Origin"))
                        .addApiResponse("503", new ApiResponse().description("Access projection unavailable")));
        return new OpenAPI().path("/ws/realtime", new PathItem().get(handshake));
    }
}
