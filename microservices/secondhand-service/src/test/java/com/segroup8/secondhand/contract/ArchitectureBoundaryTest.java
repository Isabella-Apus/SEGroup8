package com.segroup8.secondhand.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("DOMAIN_D")
@Tag("ARCHITECTURE")
class ArchitectureBoundaryTest {
    private static final List<String> FORBIDDEN = List.of("OrderMapper", "BalanceMapper", "VoucherMapper",
            "NotificationMapper", "order_info", "balance_info", "voucher_user");

    @Test
    void sourceDoesNotContainCrossDomainMappersTablesOrOrderEndpoints() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        String sources;
        try (var files = Files.walk(sourceRoot)) {
            sources = files.filter(path -> path.toString().endsWith(".java"))
                    .map(this::readUnchecked).reduce("", (left, right) -> left + "\n" + right);
        }
        for (String forbidden : FORBIDDEN) {
            assertThat(sources).doesNotContain(forbidden);
        }
        assertThat(sources).doesNotContain("@RequestMapping(\"/api/order")
                .doesNotContain("@RequestMapping(\"/api/logistics");
    }

    private String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
