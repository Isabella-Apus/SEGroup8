package com.segroup8.finance.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.segroup8.finance.BenefitsFinanceApplication;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.yaml.snakeyaml.Yaml;

@SpringBootTest(classes = BenefitsFinanceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RuntimeContractTest {
    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "put", "post", "delete", "options", "head", "patch", "trace");

    @Autowired TestRestTemplate http;

    @Test
    void actuatorExposesOnlyApprovedEndpointsAndBuildIdentity() {
        JsonNode root = getJson("/actuator");
        assertThat(root.at("/_links/health/href").asText()).contains("/actuator/health");
        assertThat(root.at("/_links/info/href").asText()).contains("/actuator/info");
        assertThat(root.at("/_links/flyway").isMissingNode()).isTrue();

        assertThat(getJson("/actuator/health/liveness").path("status").asText()).isEqualTo("UP");
        assertThat(getJson("/actuator/health/readiness").path("status").asText()).isEqualTo("UP");

        JsonNode info = getJson("/actuator/info");
        assertThat(info.at("/app/name").asText()).isEqualTo("benefits-finance-service");
        assertThat(info.at("/app/version").asText()).isEqualTo("test");
        assertThat(info.at("/app/commit").asText()).isEqualTo("test-commit");
        assertThat(info.at("/app/buildTime").asText()).isEqualTo("2026-08-30T00:00:00Z");

        ResponseEntity<String> flyway = http.getForEntity("/actuator/flyway", String.class);
        assertThat(flyway.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void runtimeOpenApiRoutesAndMethodsExactlyMatchVersionedContract() throws Exception {
        JsonNode runtime = getJson("/v3/api-docs");
        Set<String> runtimeRoutes = new LinkedHashSet<>();
        runtime.path("paths").properties().forEach(path -> path.getValue().properties().forEach(operation -> {
            String normalizedMethod = operation.getKey().toLowerCase(Locale.ROOT);
            if (HTTP_METHODS.contains(normalizedMethod)) {
                runtimeRoutes.add(normalizedMethod.toUpperCase(Locale.ROOT) + " " + path.getKey());
            }
        }));

        Path contract = repositoryRoot().resolve(
                "02_docs/microservices/benefits-finance-service/openapi.yaml");
        Map<?, ?> versioned;
        try (InputStream input = Files.newInputStream(contract)) {
            versioned = new Yaml().load(input);
        }
        Set<String> expectedRoutes = new LinkedHashSet<>();
        Map<?, ?> paths = (Map<?, ?>) versioned.get("paths");
        paths.forEach((path, definition) -> ((Map<?, ?>) definition).forEach((method, operation) -> {
            String normalizedMethod = method.toString().toLowerCase(Locale.ROOT);
            if (HTTP_METHODS.contains(normalizedMethod)) {
                expectedRoutes.add(normalizedMethod.toUpperCase(Locale.ROOT) + " " + path);
            }
        }));

        assertThat(runtimeRoutes)
                .as("runtime /v3/api-docs route+method set must equal the committed OpenAPI contract")
                .containsExactlyInAnyOrderElementsOf(expectedRoutes);
    }

    private JsonNode getJson(String path) {
        ResponseEntity<JsonNode> response = http.getForEntity(path, JsonNode.class);
        assertThat(response.getStatusCode()).as(path).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).as(path + " response body").isNotNull();
        return response.getBody();
    }

    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("microservices/pom.xml"))) {
            current = current.getParent();
        }
        if (current == null) throw new IllegalStateException("repository root not found");
        return current;
    }
}
