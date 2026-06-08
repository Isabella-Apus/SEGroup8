package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.segroup8.platform.common.AccessControl;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.OrderStatusEnum;
import com.segroup8.platform.dto.OrderShipRequest;
import com.segroup8.platform.entity.LogisticsPathTemplate;
import com.segroup8.platform.entity.LogisticsTrace;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.OrderItem;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.mapper.LogisticsPathTemplateMapper;
import com.segroup8.platform.mapper.LogisticsTraceMapper;
import com.segroup8.platform.mapper.OrderInfoMapper;
import com.segroup8.platform.mapper.OrderItemMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.service.LogisticsService;
import com.segroup8.platform.service.logistics.LogisticsEngine;
import com.segroup8.platform.vo.LogisticsTraceVO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LogisticsServiceImpl implements LogisticsService {

    private static final String DEFAULT_ORIGIN_PROVINCE = "北京";

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ShopMapper shopMapper;
    private final SecondhandProductMapper secondhandProductMapper;
    private final LogisticsPathTemplateMapper logisticsPathTemplateMapper;
    private final LogisticsTraceMapper logisticsTraceMapper;
    private final LogisticsEngine logisticsEngine;
    private final ObjectMapper objectMapper;

    public LogisticsServiceImpl(OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper,
            ProductMapper productMapper,
            ShopMapper shopMapper,
            SecondhandProductMapper secondhandProductMapper,
            LogisticsPathTemplateMapper logisticsPathTemplateMapper,
            LogisticsTraceMapper logisticsTraceMapper,
            LogisticsEngine logisticsEngine,
            ObjectMapper objectMapper) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.shopMapper = shopMapper;
        this.secondhandProductMapper = secondhandProductMapper;
        this.logisticsPathTemplateMapper = logisticsPathTemplateMapper;
        this.logisticsTraceMapper = logisticsTraceMapper;
        this.logisticsEngine = logisticsEngine;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LogisticsTraceVO pushNextBySeller(Long orderId) {
        Long sellerUserId = AccessControl.requireUserId();
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!Integer.valueOf(OrderStatusEnum.SHIPPED.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(400, "仅待收货订单可推进物流");
        }
        ensureSellerOwnership(orderId, sellerUserId);
        ensureTemplateAndSeed(order);

        LogisticsPathTemplate template = logisticsPathTemplateMapper.selectById(order.getLogisticsTemplateId());
        if (template == null) {
            throw new BusinessException(400, "物流模板不存在");
        }
        List<String> nodes = parsePathNodes(template.getPathNodes());
        int currentIndex = order.getLogisticsCurrentIndex() == null ? 0 : order.getLogisticsCurrentIndex();
        if (currentIndex >= nodes.size() - 1) {
            throw new BusinessException(400, "物流轨迹已到达终点");
        }
        String nextNode = nodes.get(currentIndex + 1);
        LocalDateTime baseTime = latestTraceTime(orderId);
        int randomHours = ThreadLocalRandom.current().nextInt(12, 25);

        LogisticsTrace trace = new LogisticsTrace();
        trace.setOrderId(orderId);
        trace.setNodeName(nextNode);
        boolean arrived = currentIndex + 1 == nodes.size() - 1;
        LocalDateTime traceTime = baseTime.plusHours(randomHours);
        trace.setStatusDesc(arrived ? "包裹已送达，进入自动确认倒计时" : "包裹运输中");
        trace.setCreateTime(traceTime);
        logisticsTraceMapper.insert(trace);

        String logisticsStatus = arrived ? "ARRIVED" : "IN_TRANSIT";
        UpdateWrapper<OrderInfo> updateWrapper = new UpdateWrapper<OrderInfo>()
                .set("logistics_current_index", currentIndex + 1)
                .set("logistics_status", logisticsStatus)
                .eq("id", orderId);
        if (arrived) {
            updateWrapper
                    .set("arrival_time", traceTime)
                    .set("auto_confirm_deadline", traceTime.plusDays(7));
        }
        orderInfoMapper.update(null, updateWrapper);

        return toVO(trace);
    }

    @Override
    public List<LogisticsTraceVO> listByOrderId(Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        Long userId = AccessControl.requireUserId();
        boolean isBuyer = Objects.equals(userId, order.getBuyerUserId());
        boolean isSeller = hasSellerItem(orderId, userId);
        if (!isBuyer && !isSeller) {
            throw new BusinessException(403, "无权查看物流轨迹");
        }
        List<LogisticsTrace> traces = logisticsTraceMapper.selectList(new LambdaQueryWrapper<LogisticsTrace>()
                .eq(LogisticsTrace::getOrderId, orderId)
                .orderByAsc(LogisticsTrace::getCreateTime)
                .orderByAsc(LogisticsTrace::getId));
        return traces.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeWhenShipped(Long orderId, OrderShipRequest request) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        ensureTemplateAndSeed(order, request);
    }

    private void ensureTemplateAndSeed(OrderInfo order) {
        ensureTemplateAndSeed(order, null);
    }

    private void ensureTemplateAndSeed(OrderInfo order, OrderShipRequest request) {
        Long templateId = order.getLogisticsTemplateId();
        if (templateId == null) {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId())
                    .orderByAsc(OrderItem::getId));
            if (items.isEmpty()) {
                throw new BusinessException(400, "订单商品为空，无法生成物流模板");
            }
            String originProvince = resolveOriginProvince(request);
            String destProvince = (order.getReceiverProvince() == null || order.getReceiverProvince().isBlank())
                    ? "北京"
                    : order.getReceiverProvince();
            String originRegion = logisticsEngine.resolveRegion(originProvince);
            String destRegion = logisticsEngine.resolveRegion(destProvince);
            if (originRegion == null || destRegion == null) {
                throw new BusinessException(400, "省份未映射大区，无法生成物流模板");
            }
            List<String> nodes = logisticsEngine.generatePathNodes(originProvince, destProvince);
            LogisticsPathTemplate template = findOrCreateTemplate(originRegion, destRegion, nodes);
            templateId = template.getId();
            orderInfoMapper.update(null, new UpdateWrapper<OrderInfo>()
                    .set("logistics_template_id", templateId)
                    .set("logistics_status", "IN_TRANSIT")
                    .set("logistics_current_index", 0)
                    .eq("id", order.getId()));
            order.setLogisticsTemplateId(templateId);
            order.setLogisticsCurrentIndex(0);
            order.setLogisticsStatus("IN_TRANSIT");
        }
        long existing = logisticsTraceMapper.selectCount(new LambdaQueryWrapper<LogisticsTrace>()
                .eq(LogisticsTrace::getOrderId, order.getId()));
        if (existing == 0) {
            LogisticsPathTemplate template = logisticsPathTemplateMapper.selectById(templateId);
            List<String> nodes = parsePathNodes(template.getPathNodes());
            if (nodes.isEmpty()) {
                throw new BusinessException(400, "物流模板节点为空");
            }
            LogisticsTrace seed = new LogisticsTrace();
            seed.setOrderId(order.getId());
            seed.setNodeName(nodes.get(0));
            seed.setStatusDesc("包裹已揽收");
            seed.setCreateTime(LocalDateTime.now());
            try {
                logisticsTraceMapper.insert(seed);
            } catch (DataIntegrityViolationException ignored) {
                // 发货允许幂等：真实库若已有首条轨迹，不再让重复写入打断发货。
            }
        }
    }

    private LogisticsPathTemplate findOrCreateTemplate(String originRegion, String destRegion, List<String> nodes) {
        String pathNodes = writePathNodes(nodes);
        LogisticsPathTemplate template = logisticsPathTemplateMapper
                .selectOne(new LambdaQueryWrapper<LogisticsPathTemplate>()
                        .eq(LogisticsPathTemplate::getOriginRegion, originRegion)
                        .eq(LogisticsPathTemplate::getDestRegion, destRegion)
                        .last("limit 1"));
        if (template != null) {
            if (!Objects.equals(template.getPathNodes(), pathNodes)) {
                try {
                    logisticsPathTemplateMapper.update(null, new UpdateWrapper<LogisticsPathTemplate>()
                            .set("path_nodes", pathNodes)
                            .eq("id", template.getId()));
                    template.setPathNodes(pathNodes);
                } catch (DataIntegrityViolationException ignored) {
                    // 旧数据中的模板仍可复用，路径刷新失败不应阻塞卖家发货。
                }
            }
            return template;
        }
        LogisticsPathTemplate created = new LogisticsPathTemplate();
        created.setOriginRegion(originRegion);
        created.setDestRegion(destRegion);
        created.setPathNodes(pathNodes);
        try {
            logisticsPathTemplateMapper.insert(created);
            return created;
        } catch (DataIntegrityViolationException ignored) {
            LogisticsPathTemplate existing = logisticsPathTemplateMapper
                    .selectOne(new LambdaQueryWrapper<LogisticsPathTemplate>()
                            .eq(LogisticsPathTemplate::getOriginRegion, originRegion)
                            .eq(LogisticsPathTemplate::getDestRegion, destRegion)
                            .last("limit 1"));
            if (existing != null) {
                return existing;
            }
            throw ignored;
        }
    }

    private String resolveOriginProvince(OrderShipRequest request) {
        return DEFAULT_ORIGIN_PROVINCE;
    }

    private LocalDateTime latestTraceTime(Long orderId) {
        LogisticsTrace trace = logisticsTraceMapper.selectOne(new LambdaQueryWrapper<LogisticsTrace>()
                .eq(LogisticsTrace::getOrderId, orderId)
                .orderByDesc(LogisticsTrace::getCreateTime)
                .orderByDesc(LogisticsTrace::getId)
                .last("limit 1"));
        return trace == null || trace.getCreateTime() == null ? LocalDateTime.now() : trace.getCreateTime();
    }

    private void ensureSellerOwnership(Long orderId, Long sellerUserId) {
        if (!hasSellerItem(orderId, sellerUserId)) {
            throw new BusinessException(403, "无权操作该订单物流");
        }
    }

    private boolean hasSellerItem(Long orderId, Long sellerUserId) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            if ("NEW".equalsIgnoreCase(item.getProductType())) {
                Product product = productMapper.selectById(item.getProductId());
                if (product == null) {
                    continue;
                }
                Shop shop = shopMapper.selectById(product.getShopId());
                if (shop != null && Objects.equals(shop.getOwnerUserId(), sellerUserId)) {
                    return true;
                }
            } else if ("SECONDHAND".equalsIgnoreCase(item.getProductType())) {
                SecondhandProduct secondhand = secondhandProductMapper.selectById(item.getProductId());
                if (secondhand != null && Objects.equals(secondhand.getSellerUserId(), sellerUserId)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> parsePathNodes(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            List<String> nodes = objectMapper.readValue(raw, new TypeReference<List<String>>() {
            });
            return nodes == null ? List.of() : nodes;
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "物流模板节点解析失败");
        }
    }

    private String writePathNodes(List<String> nodes) {
        try {
            return objectMapper.writeValueAsString(nodes == null ? new ArrayList<>() : nodes);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "物流模板节点写入失败");
        }
    }

    private LogisticsTraceVO toVO(LogisticsTrace trace) {
        LogisticsTraceVO vo = new LogisticsTraceVO();
        vo.setId(trace.getId());
        vo.setOrderId(trace.getOrderId());
        vo.setNodeName(trace.getNodeName());
        vo.setStatusDesc(trace.getStatusDesc());
        vo.setCreateTime(trace.getCreateTime());
        return vo;
    }
}
