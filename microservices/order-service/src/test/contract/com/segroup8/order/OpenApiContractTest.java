package com.segroup8.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

@Tag("CONTRACT")
@SpringBootTest
class OpenApiContractTest {
    private static final Pattern TEMPLATE = Pattern.compile("\\{([^}]+)}");
    private static final Set<String> RUNTIME_OPERATIONS = Set.of(
            "POST /api/order/create", "GET /api/order/list", "GET /api/order/detail/{id}",
            "POST /api/order/{id}/pay", "POST /api/order/{id}/cancel", "POST /api/order/{id}/ship",
            "POST /api/order/{id}/remind-ship", "POST /api/order/{id}/confirm-receive",
            "POST /api/order/{id}/complete", "POST /api/order/{id}/refund",
            "POST /api/order/{id}/refund/approve", "POST /api/order/{id}/refund/reject",
            "GET /api/order/seller/list", "GET /api/order/seller/detail/{id}",
            "POST /api/order/{id}/review", "POST /api/order/{id}/review/items",
            "GET /api/review/my", "GET /api/review/seller/list", "POST /api/review/followup",
            "POST /api/review/{id}/reply", "POST /api/review/seller/{id}/reply",
            "GET /api/logistics/{orderId}", "GET /api/logistics/order/{orderId}/trace",
            "POST /api/logistics/trace", "POST /api/logistics/push-next",
            "GET /api/admin/orders", "GET /api/admin/orders/list", "GET /api/admin/orders/{id}",
            "GET /api/admin/orders/detail/{id}", "GET /api/admin/orders/{id}/after-sale-logs",
            "POST /api/admin/orders/batch-close", "POST /api/admin/orders/{id}/refund/approve",
            "POST /api/admin/orders/{id}/refund/reject", "POST /internal/orders/secondhand",
            "GET /internal/orders/by-business-key/{key}", "GET /internal/orders/{id}/snapshot");

    @Autowired @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping mappings;

    @Test
    void runtimeControllerSurfaceIsAnExactReviewedSet() {
        Set<String> actual = new TreeSet<>();
        mappings.getHandlerMethods().forEach((mapping, handler) -> {
            Class<?> controller = handler.getBeanType();
            if (!controller.getPackageName().equals("com.segroup8.order")
                    || !controller.isAnnotationPresent(RestController.class)) return;
            var paths = mapping.getPathPatternsCondition();
            assertThat(paths).as("path patterns for %s", handler).isNotNull();
            paths.getPatternValues().forEach(path -> mapping.getMethodsCondition().getMethods()
                    .forEach(method -> actual.add(method.name() + " " + path)));
        });
        assertThat(actual).containsExactlyInAnyOrderElementsOf(RUNTIME_OPERATIONS);
        assertThat(actual).hasSize(36);
    }

    @Test
    @SuppressWarnings("unchecked")
    void openApiParsesAndDeclaresEveryPathParameter() throws Exception {
        Path repositoryRoot = findRepositoryRoot(Path.of("").toAbsolutePath());
        Path specification = repositoryRoot.resolve("02_docs/microservices/order-service/openapi.yaml");
        Map<String,Object> document;
        try (InputStream input = Files.newInputStream(specification)) {
            document = new Yaml().load(input);
        }
        assertThat(document.get("openapi")).isEqualTo("3.0.3");
        Map<String,Object> paths = (Map<String,Object>) document.get("paths");
        assertThat(paths).containsKeys("/api/order/create", "/api/review/followup",
                "/api/logistics/order/{orderId}/trace", "/internal/orders/secondhand");

        for (var pathEntry : paths.entrySet()) {
            var matcher = TEMPLATE.matcher(pathEntry.getKey());
            while (matcher.find()) {
                String expected = matcher.group(1);
                Map<String,Object> pathItem = (Map<String,Object>) pathEntry.getValue();
                for (var operationEntry : pathItem.entrySet()) {
                    if (!List.of("get", "post", "put", "delete", "patch").contains(operationEntry.getKey())) continue;
                    Map<String,Object> operation = (Map<String,Object>) operationEntry.getValue();
                    assertThat((List<Object>) operation.get("parameters"))
                            .as("%s %s declares {%s}", operationEntry.getKey(), pathEntry.getKey(), expected)
                            .isNotNull()
                            .anySatisfy(parameter -> assertThat(String.valueOf(parameter).toLowerCase())
                                    .contains(expected.toLowerCase()));
                }
            }
        }
    }

    private Path findRepositoryRoot(Path start) {
        Path current = start;
        while (current != null && !Files.exists(current.resolve(".git"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("Repository root not found from " + start);
        return current;
    }
}
