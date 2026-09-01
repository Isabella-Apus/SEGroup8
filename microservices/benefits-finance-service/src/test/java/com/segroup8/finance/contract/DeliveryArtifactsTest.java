package com.segroup8.finance.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class DeliveryArtifactsTest {
    @Test
    void requiredArchitectureOperationsEvidenceAndDeploymentFilesExistAndParse() throws Exception {
        Path root = repositoryRoot();
        Path architecture = root.resolve("02_docs/microservices/benefits-finance-service");
        for (String file : List.of("README.md", "service-boundary.md", "service-diagram.mmd",
                "service-diagram.svg", "openapi.yaml", "database-ownership.md", "cross-service-calls.md",
                "migration-version-report.md", "before-after-code-diff.md", "traceability.md", "delivery-manifest.md")) {
            assertThat(architecture.resolve(file)).exists().isRegularFile();
        }
        assertThat(root.resolve("02_docs/microservices/service-acceptance-checklist.md")).exists().isRegularFile();
        for (String file : List.of("README.md", "operations-runbook.md", "deployment-failure-drill.md",
                "reconciliation-runbook.md")) {
            assertThat(root.resolve("03_devops/microservices/benefits-finance-service").resolve(file)).exists();
        }
        for (String file : List.of("test-plan.md", "api-test-report.md", "e2e-test-report.md",
                "deployment-test-report.md", "fault-injection-report.md", "reconciliation-report.md",
                "result-summary.json")) {
            assertThat(root.resolve("04_tests/microservices/benefits-finance-service").resolve(file)).exists();
        }

        Map<?, ?> openApi = yaml(architecture.resolve("openapi.yaml"));
        assertThat(openApi.get("openapi")).isEqualTo("3.0.3");
        Map<?, ?> paths = (Map<?, ?>) openApi.get("paths");
        assertThat(paths.containsKey("/internal/payments/debit")).isTrue();
        assertThat(paths.containsKey("/internal/settlements")).isTrue();
        yaml(root.resolve("deploy/helm/segroup8/Chart.yaml"));
        yaml(root.resolve("deploy/helm/segroup8/values.yaml"));
        yaml(root.resolve(".github/workflows/benefits-finance-service-ci-cd.yml"));
        try (InputStream input = Files.newInputStream(architecture.resolve("service-diagram.svg"))) {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
        }
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
