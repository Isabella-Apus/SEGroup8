package com.segroup8.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

@Tag("CONTRACT")
class OpenApiContractTest {
    private static final Pattern TEMPLATE = Pattern.compile("\\{([^}]+)}");

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
