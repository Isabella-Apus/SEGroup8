package com.segroup8.platform.service.logistics;

import com.segroup8.platform.common.BusinessException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractLogisticsEngine implements LogisticsEngine {

    @Override
    public final List<String> generatePathNodes(String originProvince, String destProvince) {
        String originRegion = resolveRegion(originProvince);
        String destRegion = resolveRegion(destProvince);
        if (originRegion == null || destRegion == null) {
            throw new BusinessException(400, "无法识别省份所属大区，无法生成物流路径");
        }
        if (originRegion.equals(destRegion)) {
            return buildSameRegionPath(originProvince, destProvince);
        }
        List<String> regionPath = buildRegionPath(originRegion, destRegion);
        if (regionPath.size() < 2) {
            throw new BusinessException(400, "物流区域路径生成失败");
        }
        return buildCrossRegionPath(originProvince, destProvince, regionPath);
    }

    protected List<String> buildSameRegionPath(String originProvince, String destProvince) {
        List<String> nodes = new ArrayList<>();
        nodes.add(normalizeProvinceName(originProvince) + "分拨中心");
        nodes.add(normalizeProvinceName(destProvince) + "分拨中心");
        return nodes;
    }

    protected List<String> buildCrossRegionPath(String originProvince, String destProvince, List<String> regionPath) {
        List<String> nodes = new ArrayList<>();
        nodes.add(normalizeProvinceName(originProvince) + "分拨中心");
        for (int i = 1; i < regionPath.size() - 1; i++) {
            String region = regionPath.get(i);
            String hub = transitHubByRegion(region);
            if (hub != null && !hub.isBlank()) {
                nodes.add(hub + "中转站");
            }
        }
        nodes.add(normalizeProvinceName(destProvince) + "分拨中心");
        return nodes;
    }

    protected List<String> buildRegionPath(String originRegion, String destRegion) {
        if (originRegion.equals(destRegion)) {
            return List.of(originRegion);
        }
        Map<String, String> prev = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(originRegion);
        visited.add(originRegion);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (destRegion.equals(current)) {
                break;
            }
            for (String next : adjacentRegions(current)) {
                if (visited.add(next)) {
                    prev.put(next, current);
                    queue.add(next);
                }
            }
        }
        if (!visited.contains(destRegion)) {
            throw new BusinessException(400, "无法匹配合理地理路径");
        }
        List<String> reversed = new ArrayList<>();
        String cursor = destRegion;
        while (cursor != null) {
            reversed.add(cursor);
            cursor = prev.get(cursor);
        }
        Collections.reverse(reversed);
        return reversed;
    }

    protected abstract List<String> adjacentRegions(String region);

    protected abstract String transitHubByRegion(String region);

    protected String normalizeProvinceName(String province) {
        if (province == null || province.isBlank()) {
            return "未知";
        }
        return province.trim();
    }
}
