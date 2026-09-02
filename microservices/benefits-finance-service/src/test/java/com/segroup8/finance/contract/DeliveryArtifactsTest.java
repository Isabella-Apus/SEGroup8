package com.segroup8.finance.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class DeliveryArtifactsTest {
    @Test
    void deploymentConfigurationAndApiContractParse() throws Exception {
        Path root = repositoryRoot();
        Path architecture = root.resolve("02_docs/microservices/benefits-finance-service");
        Map<?, ?> openApi = yaml(architecture.resolve("openapi.yaml"));
        assertThat(openApi.get("openapi")).isEqualTo("3.0.3");
        Map<?, ?> paths = (Map<?, ?>) openApi.get("paths");
        assertThat(paths.containsKey("/internal/payments/debit")).isTrue();
        assertThat(paths.containsKey("/internal/settlements")).isTrue();
        yaml(root.resolve("deploy/helm/segroup8/Chart.yaml"));
        yaml(root.resolve("deploy/helm/segroup8/values.yaml"));
        yaml(root.resolve(".github/workflows/benefits-finance-service-ci-cd.yml"));
    }

    private Map<?, ?> yaml(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path)) {
            return new Yaml().load(input);
        }
    }

    private Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("microservices/pom.xml"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("repository root not found");
        return current;
    }
}
