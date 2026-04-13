package com.segroup8.platform.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.entity.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fuse.js 适配器：通过 Node 进程调用 fuse 搜索能力，
 * 将 Java 服务层与具体搜索实现解耦。
 */
@Component
public class FuseAdapter implements FuzzySearchAdapter {

    private static final double DEFAULT_THRESHOLD = 0.3D;

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Integer>> INTEGER_LIST_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String nodeCommand;
    private final String fuseCjsPath;

    public FuseAdapter(ObjectMapper objectMapper,
            @Value("${search.fuse.node-command:node}") String nodeCommand,
            @Value("${search.fuse.cjs-path:../../fuse/dist/fuse.cjs}") String fuseCjsPath) {
        this.objectMapper = objectMapper;
        this.nodeCommand = nodeCommand;
        this.fuseCjsPath = fuseCjsPath;
    }

    @Override
    public List<String> fuzzySearch(List<String> data, String keyword) {
        return fuzzySearch(data, keyword, DEFAULT_THRESHOLD);
    }

    public List<String> fuzzySearch(List<String> data, String keyword, Double threshold) {
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        if (!StringUtils.hasText(keyword)) {
            return List.copyOf(data);
        }

        try {
            String output = runFuseScript(
                    nodeScriptForStrings(),
                    objectMapper.writeValueAsString(new FuseInput(data, keyword, normalizeThreshold(threshold))));
            if (!StringUtils.hasText(output)) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(output, STRING_LIST_TYPE);
        } catch (IOException e) {
            throw new IllegalStateException("Fuse 搜索调用异常", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fuse 搜索线程被中断", e);
        }
    }

    public List<Product> fuzzySearchProducts(List<Product> products, String keyword) {
        return fuzzySearchProducts(products, keyword, DEFAULT_THRESHOLD);
    }

    public List<Product> fuzzySearchProducts(List<Product> products, String keyword, Double threshold) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        if (!StringUtils.hasText(keyword)) {
            return List.copyOf(products);
        }

        List<ProductFuseDoc> docs = products.stream()
                .map(product -> new ProductFuseDoc(
                        safeText(product.getName()),
                        safeText(product.getDescription())))
                .toList();

        try {
            String payload = objectMapper.writeValueAsString(
                    new ProductFuseInput(docs, keyword, normalizeThreshold(threshold)));
            String output = runFuseScript(nodeScriptForProductIndices(), payload);
            if (!StringUtils.hasText(output)) {
                return Collections.emptyList();
            }

            List<Integer> matchedIndexes = objectMapper.readValue(output, INTEGER_LIST_TYPE);
            List<Product> matchedProducts = new ArrayList<>(matchedIndexes.size());
            for (Integer index : matchedIndexes) {
                if (index != null && index >= 0 && index < products.size()) {
                    matchedProducts.add(products.get(index));
                }
            }
            return matchedProducts;
        } catch (IOException e) {
            throw new IllegalStateException("Fuse 商品搜索调用异常", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fuse 商品搜索线程被中断", e);
        }
    }

    private String runFuseScript(String script, String payload) throws IOException, InterruptedException {
        Path fusePath = Path.of(fuseCjsPath).toAbsolutePath().normalize();
        ProcessBuilder processBuilder = new ProcessBuilder(
                nodeCommand,
                "-e",
                script,
                fusePath.toString(),
                payload);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        byte[] outputBytes = process.getInputStream().readAllBytes();
        int exitCode = process.waitFor();
        String output = new String(outputBytes, StandardCharsets.UTF_8).trim();
        if (exitCode != 0) {
            throw new IllegalStateException("Fuse 搜索执行失败: " + output);
        }
        return output;
    }

    private String nodeScriptForStrings() {
        return "const Fuse = require(process.argv[1]);"
                + "const input = JSON.parse(process.argv[2]);"
                + "const fuse = new Fuse(input.data, { threshold: input.threshold });"
                + "const result = fuse.search(input.keyword).map(item => item.item);"
                + "process.stdout.write(JSON.stringify(result));";
    }

    private String nodeScriptForProductIndices() {
        return "const Fuse = require(process.argv[1]);"
                + "const input = JSON.parse(process.argv[2]);"
                + "const fuse = new Fuse(input.data, {"
                + " threshold: input.threshold,"
                + " ignoreLocation: true,"
                + " keys: [{ name: 'name', weight: 0.7 }, { name: 'description', weight: 0.3 }]"
                + "});"
                + "const result = fuse.search(input.keyword).map(item => item.refIndex);"
                + "process.stdout.write(JSON.stringify(result));";
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private double normalizeThreshold(Double threshold) {
        if (threshold == null || threshold.isNaN() || threshold.isInfinite()) {
            return DEFAULT_THRESHOLD;
        }
        if (threshold < 0D) {
            return 0D;
        }
        if (threshold > 1D) {
            return 1D;
        }
        return threshold;
    }

    private record FuseInput(List<String> data, String keyword, double threshold) {
    }

    private record ProductFuseInput(List<ProductFuseDoc> data, String keyword, double threshold) {
    }

    private record ProductFuseDoc(String name, String description) {
    }
}
