package com.segroup8.platform.common;

import com.segroup8.platform.context.UserContext;
import com.segroup8.platform.entity.OrderInfo;
import com.segroup8.platform.entity.User;
import com.segroup8.platform.mapper.UserMapper;

import java.util.function.BooleanSupplier;

/**
 * 统一的权限校验入口（尽量减少散落的 role/登录判断）
 */
public final class AccessControl {

    private AccessControl() {
    }

    public static Long requireUserId() {
        Long uid = UserContext.getUserId();
        if (uid == null) {
            throw new BusinessException(401, "未登录");
        }
        return uid;
    }

    public static User requireAdmin(UserMapper userMapper) {
        Long uid = requireUserId();
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        if (!RoleEnum.ADMIN.name().equals(user.getRole())) {
            throw new BusinessException(403, "无访问权限");
        }
        return user;
    }

    public static Long requireOrderOwnedByBuyer(OrderInfo order, String messageWhenDenied) {
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        Long uid = requireUserId();
        if (order.getBuyerUserId() == null || !uid.equals(order.getBuyerUserId())) {
            throw new BusinessException(403, messageWhenDenied);
        }
        return uid;
    }

    public static Long requireSellerOwnership(BooleanSupplier ownershipCheck, String messageWhenDenied) {
        Long uid = requireUserId();
        if (ownershipCheck == null || !ownershipCheck.getAsBoolean()) {
            throw new BusinessException(403, messageWhenDenied);
        }
        return uid;
    }
}

