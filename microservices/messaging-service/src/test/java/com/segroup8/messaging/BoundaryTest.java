package com.segroup8.messaging;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class BoundaryTest {
    private static final Pattern FORBIDDEN_QUERY = Pattern.compile(
            "(?is)\\b(from|join)\\s+(`?)(user|product|shop|secondhand_product|order_info|order_item|payment|refund|merchant_application|user_block)\\2\\b");

    @Test
    void runtimeSourceNeverQueriesForeignDomainTablesOrSchemas() throws IOException {
        Path root = Path.of("src/main");
        try (var files = Files.walk(root)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                try {
                    String text = Files.readString(path).toLowerCase(Locale.ROOT);
                    assertFalse(FORBIDDEN_QUERY.matcher(text).find(), () -> "Foreign-domain query in " + path);
                    assertFalse(text.contains("segroup8_platform."), () -> "Cross-schema reference in runtime source " + path);
                    assertFalse(text.contains("x-user-id") || text.contains("x-seller-id") || text.contains("x-admin-id"),
                            () -> "Trusted identity header in " + path);
                } catch (IOException ex) { throw new RuntimeException(ex); }
            });
        }
    }
}
