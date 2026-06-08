package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.BrowseHistory;
import com.segroup8.platform.entity.Product;
import com.segroup8.platform.entity.SecondhandProduct;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.mapper.BrowseHistoryMapper;
import com.segroup8.platform.mapper.ProductMapper;
import com.segroup8.platform.mapper.SecondhandProductMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.service.BrowseHistoryService;
import com.segroup8.platform.vo.BrowseHistoryVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrowseHistoryServiceImpl implements BrowseHistoryService {

    private final BrowseHistoryMapper browseHistoryMapper;
    private final ProductMapper productMapper;
    private final SecondhandProductMapper secondhandProductMapper;
    private final ShopMapper shopMapper;

    public BrowseHistoryServiceImpl(BrowseHistoryMapper browseHistoryMapper,
            ProductMapper productMapper,
            SecondhandProductMapper secondhandProductMapper,
            ShopMapper shopMapper) {
        this.browseHistoryMapper = browseHistoryMapper;
        this.productMapper = productMapper;
        this.secondhandProductMapper = secondhandProductMapper;
        this.shopMapper = shopMapper;
    }

    @Override
    public void saveBrowseHistory(Long productId, String productType) {
        Long userId = UserContext.getUserId();
        if (userId == null || productId == null) {
            return;
        }

        String normalizedType = normalizeProductType(productType);

        // Check if history exists, update time if yes, insert if no
        LambdaQueryWrapper<BrowseHistory> query = new LambdaQueryWrapper<>();
        query.eq(BrowseHistory::getUserId, userId)
                .eq(BrowseHistory::getProductId, productId)
                .eq(BrowseHistory::getProductType, normalizedType);

        BrowseHistory existing = browseHistoryMapper.selectOne(query);
        if (existing != null) {
            existing.setProductType(normalizedType);
            existing.setBrowseTime(LocalDateTime.now());
            existing.setUpdateTime(LocalDateTime.now());
            browseHistoryMapper.updateById(existing);
        } else {
            BrowseHistory history = new BrowseHistory();
            history.setUserId(userId);
            history.setProductId(productId);
            history.setProductType(normalizedType);
            history.setBrowseTime(LocalDateTime.now());
            history.setCreateTime(LocalDateTime.now());
            history.setUpdateTime(LocalDateTime.now());
            browseHistoryMapper.insert(history);
        }
    }

    @Override
    public List<BrowseHistoryVO> getBrowseHistory() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return List.of();
        }

        LambdaQueryWrapper<BrowseHistory> query = new LambdaQueryWrapper<>();
        query.eq(BrowseHistory::getUserId, userId)
                .orderByDesc(BrowseHistory::getBrowseTime);

        List<BrowseHistory> historyList = browseHistoryMapper.selectList(query);

        return historyList.stream().map(history -> {
            if ("SHOP".equalsIgnoreCase(history.getProductType())) {
                Shop shop = shopMapper.selectById(history.getProductId());
                if (shop == null) {
                    return null;
                }
                BrowseHistoryVO.ProductVO shopVO = new BrowseHistoryVO.ProductVO(
                        shop.getId(),
                        shop.getName(),
                        null,
                        shop.getLogo());
                return new BrowseHistoryVO(
                        history.getId(),
                        shopVO,
                        "SHOP",
                        history.getBrowseTime());
            }

            if ("SECONDHAND".equalsIgnoreCase(history.getProductType())) {
                SecondhandProduct secondhand = secondhandProductMapper.selectById(history.getProductId());
                if (secondhand == null) {
                    return null;
                }
                BrowseHistoryVO.ProductVO productVO = new BrowseHistoryVO.ProductVO(
                        secondhand.getId(),
                        secondhand.getName(),
                        secondhand.getSalePrice(),
                        secondhand.getCover());
                return new BrowseHistoryVO(
                        history.getId(),
                        productVO,
                        "SECONDHAND",
                        history.getBrowseTime());
            }

            Product product = productMapper.selectById(history.getProductId());
            if (product == null) {
                return null;
            }

            BrowseHistoryVO.ProductVO productVO = new BrowseHistoryVO.ProductVO(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getCover());

            return new BrowseHistoryVO(
                    history.getId(),
                    productVO,
                    "NEW",
                    history.getBrowseTime());
        }).filter(v -> v != null).collect(Collectors.toList());
    }

    @Override
    public void deleteBrowseHistory(Long historyId) {
        Long userId = requireUserId();
        browseHistoryMapper.delete(new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getId, historyId)
                .eq(BrowseHistory::getUserId, userId));
    }

    @Override
    public void deleteBrowseHistoryBatch(List<Long> historyIds) {
        if (historyIds == null || historyIds.isEmpty()) {
            return;
        }
        Long userId = requireUserId();
        browseHistoryMapper.delete(new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId)
                .in(BrowseHistory::getId, historyIds));
    }

    @Override
    public void clearBrowseHistory() {
        Long userId = requireUserId();
        browseHistoryMapper.delete(new LambdaQueryWrapper<BrowseHistory>()
                .eq(BrowseHistory::getUserId, userId));
    }

    private String normalizeProductType(String productType) {
        if ("SHOP".equalsIgnoreCase(productType) || "STORE".equalsIgnoreCase(productType)) {
            return "SHOP";
        }
        if ("SECONDHAND".equalsIgnoreCase(productType)) {
            return "SECONDHAND";
        }
        return "NEW";
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }
}
