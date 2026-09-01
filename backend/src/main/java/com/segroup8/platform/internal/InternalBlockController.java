package com.segroup8.platform.internal;

import com.segroup8.platform.common.Result;
import com.segroup8.platform.mapper.UserBlockMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Identity-governance internal block query used by messaging-service. */
@RestController
@RequestMapping("/internal/blocks")
public class InternalBlockController {
    private final UserBlockMapper blocks;

    public InternalBlockController(UserBlockMapper blocks) { this.blocks = blocks; }

    @PostMapping("/check")
    public Result<?> check(@RequestBody Map<String, Object> request) {
        Object rawPairs = request == null ? null : request.get("pairs");
        if (!(rawPairs instanceof List<?> pairs) || pairs.isEmpty() || pairs.size() > 2) {
            return Result.fail(400, "pairs must contain one or two block checks");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object rawPair : pairs) {
            if (!(rawPair instanceof Map<?, ?> pair)) return Result.fail(400, "invalid block pair");
            Long blockerId = number(pair.get("blockerId"));
            Long blockedId = number(pair.get("blockedId"));
            if (blockerId == null || blockedId == null) return Result.fail(400, "blockerId and blockedId are required");
            result.add(Map.of("blockerId", blockerId, "blockedId", blockedId,
                    "blocked", blocks.isBlocked(blockerId, blockedId) > 0));
        }
        return Result.success(result);
    }

    private Long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        try { return Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException ex) { return null; }
    }
}
