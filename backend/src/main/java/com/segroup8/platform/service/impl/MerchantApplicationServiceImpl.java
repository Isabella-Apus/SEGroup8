package com.segroup8.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.segroup8.platform.common.BusinessException;
import com.segroup8.platform.common.RoleEnum;
import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.dto.AdminMerchantApplicationQueryRequest;
import com.segroup8.platform.dto.MerchantApplicationRejectRequest;
import com.segroup8.platform.dto.MerchantApplicationSubmitRequest;
import com.segroup8.platform.entity.MerchantApplication;
import com.segroup8.platform.entity.Shop;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.MerchantApplicationMapper;
import com.segroup8.platform.mapper.ShopMapper;
import com.segroup8.platform.mapper.UserMapper;
import com.segroup8.platform.service.MerchantApplicationService;
import com.segroup8.platform.service.NotificationService;
import com.segroup8.platform.vo.MerchantApplicationVO;
import com.segroup8.platform.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class MerchantApplicationServiceImpl implements MerchantApplicationService {

    private final MerchantApplicationMapper merchantApplicationMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final ShopMapper shopMapper;

    public MerchantApplicationServiceImpl(MerchantApplicationMapper merchantApplicationMapper,
            UserMapper userMapper,
            NotificationService notificationService,
            ShopMapper shopMapper) {
        this.merchantApplicationMapper = merchantApplicationMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.shopMapper = shopMapper;
    }

    @Override
    public void submit(MerchantApplicationSubmitRequest request) {
        Long userId = requireUserId();
        MerchantApplication existing = merchantApplicationMapper.selectOne(new LambdaQueryWrapper<MerchantApplication>()
                .eq(MerchantApplication::getUserId, userId)
                .orderByDesc(MerchantApplication::getId)
                .last("limit 1"));
        if (existing != null && (existing.getStatus() == 0 || existing.getStatus() == 1)) {
            throw new BusinessException(400, existing.getStatus() == 0 ? "已提交申请，请等待审核" : "您已是认证卖家");
        }

        MerchantApplication application = new MerchantApplication();
        application.setUserId(userId);
        application.setStoreName(request.getStoreName());
        application.setCategoryId(request.getCategoryId());
        application.setIdCardNo(request.getIdCardNo());
        application.setBankCardNo(request.getBankCardNo());
        application.setLicenseImg(request.getLicenseImg());
        application.setWarehouseProvince(request.getWarehouseProvince());
        application.setWarehouseCity(request.getWarehouseCity());
        application.setWarehouseDetail(request.getWarehouseDetail());
        application.setWarehouseAddr(buildWarehouseAddr(
                request.getWarehouseProvince(),
                request.getWarehouseCity(),
                request.getWarehouseDetail(),
                request.getWarehouseAddr()));
        application.setContactName(request.getContactName());
        application.setContactPhone(request.getContactPhone());
        application.setStatus(0);
        application.setRejectReason(null);
        application.setApplyTime(LocalDateTime.now());
        merchantApplicationMapper.insert(application);
    }

    @Override
    public MerchantApplicationVO getMyApplication() {
        Long userId = requireUserId();
        MerchantApplication application = merchantApplicationMapper.selectOne(new LambdaQueryWrapper<MerchantApplication>()
                .eq(MerchantApplication::getUserId, userId)
                .orderByDesc(MerchantApplication::getId)
                .last("limit 1"));
        if (application == null) {
            return null;
        }
        User user = userMapper.selectById(application.getUserId());
        return toVO(application, user, false);
    }

    @Override
    public PageVO<MerchantApplicationVO> pageForAdmin(AdminMerchantApplicationQueryRequest request) {
        assertAdmin();
        LambdaQueryWrapper<MerchantApplication> wrapper = new LambdaQueryWrapper<MerchantApplication>()
                .orderByDesc(MerchantApplication::getApplyTime);
        if (request.getStatus() != null) {
            wrapper.eq(MerchantApplication::getStatus, request.getStatus());
        }
        Page<MerchantApplication> page = merchantApplicationMapper
                .selectPage(Page.of(request.getPageNum(), request.getPageSize()), wrapper);
        List<MerchantApplicationVO> records = page.getRecords().stream()
                .map(app -> toVO(app, userMapper.selectById(app.getUserId()), true))
                .toList();

        PageVO<MerchantApplicationVO> result = new PageVO<>();
        result.setTotal(page.getTotal());
        result.setPageNum(request.getPageNum());
        result.setPageSize(request.getPageSize());
        result.setRecords(records);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void approve(Long applicationId) {
        assertAdmin();
        MerchantApplication app = merchantApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BusinessException(404, "申请记录不存在");
        }
        if (app.getStatus() != null && app.getStatus() == 1) {
            return;
        }
        app.setStatus(1);
        app.setRejectReason(null);
        merchantApplicationMapper.updateById(app);

        User user = userMapper.selectById(app.getUserId());
        if (user == null) {
            throw new BusinessException(404, "申请用户不存在");
        }
        user.setRole(RoleEnum.OFFICIAL_SELLER.name());
        user.setShopName(app.getStoreName());
        user.setCategory(app.getCategoryId() == null ? null : String.valueOf(app.getCategoryId()));
        user.setRegion(buildRegion(app.getWarehouseProvince(), app.getWarehouseCity()));
        userMapper.updateById(user);

        upsertShopByApplication(app);
        notificationService.createNotification(
                user.getId(),
                "入驻审核结果",
                "恭喜，您的入驻申请已通过，现可进入卖家工作台。");
    }

    @Override
    public void reject(Long applicationId, MerchantApplicationRejectRequest request) {
        assertAdmin();
        MerchantApplication app = merchantApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new BusinessException(404, "申请记录不存在");
        }
        app.setStatus(2);
        app.setRejectReason(request.getRejectReason());
        merchantApplicationMapper.updateById(app);

        notificationService.createNotification(
                app.getUserId(),
                "入驻审核结果",
                "您的入驻申请被驳回，原因：" + request.getRejectReason());
    }

    private MerchantApplicationVO toVO(MerchantApplication app, User user, boolean includeSensitive) {
        MerchantApplicationVO vo = new MerchantApplicationVO();
        vo.setId(app.getId());
        vo.setUserId(app.getUserId());
        vo.setUsername(user != null ? user.getUsername() : "");
        vo.setStoreName(app.getStoreName());
        vo.setCategoryId(app.getCategoryId());
        vo.setIdCardNo(includeSensitive ? maskMiddle(app.getIdCardNo()) : null);
        vo.setBankCardNo(includeSensitive ? maskMiddle(app.getBankCardNo()) : null);
        vo.setLicenseImg(app.getLicenseImg());
        vo.setWarehouseAddr(app.getWarehouseAddr());
        String[] warehouseParts = parseWarehouseParts(app);
        vo.setWarehouseProvince(warehouseParts[0]);
        vo.setWarehouseCity(warehouseParts[1]);
        vo.setWarehouseDetail(warehouseParts[2]);
        vo.setContactName(app.getContactName());
        vo.setContactPhone(app.getContactPhone());
        vo.setStatus(app.getStatus());
        vo.setRejectReason(app.getRejectReason());
        vo.setApplyTime(app.getApplyTime());
        return vo;
    }

    private void upsertShopByApplication(MerchantApplication app) {
        Shop shop = shopMapper.selectOne(new LambdaQueryWrapper<Shop>()
                .eq(Shop::getOwnerUserId, app.getUserId())
                .orderByDesc(Shop::getId)
                .last("limit 1"));
        if (shop == null) {
            shop = new Shop();
            shop.setOwnerUserId(app.getUserId());
            shop.setStatus(1);
            applyShopBackfillFromApplication(shop, app);
            shopMapper.insert(shop);
            return;
        }
        shop.setStatus(1);
        applyShopBackfillFromApplication(shop, app);
        shopMapper.updateById(shop);
    }

    private void applyShopBackfillFromApplication(Shop shop, MerchantApplication app) {
        shop.setName(app.getStoreName());
        shop.setRegion(buildRegion(app.getWarehouseProvince(), app.getWarehouseCity()));
        shop.setContactName(app.getContactName());
        shop.setContactPhone(app.getContactPhone());
        shop.setIdCardNoMasked(maskMiddle(app.getIdCardNo()));
        shop.setWarehouseAddr(buildWarehouseAddr(
                app.getWarehouseProvince(),
                app.getWarehouseCity(),
                app.getWarehouseDetail(),
                app.getWarehouseAddr()));
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录");
        }
        return userId;
    }

    private void assertAdmin() {
        Long userId = requireUserId();
        User user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getRole(), RoleEnum.ADMIN.name())) {
            throw new BusinessException(403, "无管理员权限");
        }
    }

    private String maskMiddle(String val) {
        if (!StringUtils.hasText(val) || val.length() <= 6) {
            return val;
        }
        return val.substring(0, 3) + "****" + val.substring(val.length() - 3);
    }

    private String buildWarehouseAddr(String province, String city, String detail, String fallback) {
        if (StringUtils.hasText(province) && StringUtils.hasText(city) && StringUtils.hasText(detail)) {
            return province + " " + city + " " + detail;
        }
        return fallback;
    }

    private String[] parseWarehouseParts(MerchantApplication app) {
        String province = app.getWarehouseProvince();
        String city = app.getWarehouseCity();
        String detail = app.getWarehouseDetail();
        if (StringUtils.hasText(province) || StringUtils.hasText(city) || StringUtils.hasText(detail)) {
            return new String[] {
                    StringUtils.hasText(province) ? province : "",
                    StringUtils.hasText(city) ? city : "",
                    StringUtils.hasText(detail) ? detail : ""
            };
        }
        if (!StringUtils.hasText(app.getWarehouseAddr())) {
            return new String[] { "", "", "" };
        }
        String[] tokens = app.getWarehouseAddr().trim().split("\\s+", 3);
        if (tokens.length == 1) {
            return new String[] { "", "", tokens[0] };
        }
        if (tokens.length == 2) {
            return new String[] { tokens[0], tokens[1], "" };
        }
        return new String[] { tokens[0], tokens[1], tokens[2] };
    }

    private String buildRegion(String province, String city) {
        if (StringUtils.hasText(province) && StringUtils.hasText(city)) {
            return province + " " + city;
        }
        return StringUtils.hasText(province) ? province : city;
    }
}
