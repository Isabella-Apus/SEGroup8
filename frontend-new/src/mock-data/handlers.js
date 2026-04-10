import { mockStore } from "./store";

function ok(data) {
    return { code: 0, message: "success", data };
}

function fail(code, message) {
    const error = new Error(message);
    error.code = code;
    throw error;
}

function paginate(records, pageNum = 1, pageSize = 10) {
    const p = Number(pageNum || 1);
    const s = Number(pageSize || 10);
    const start = (p - 1) * s;
    const slice = records.slice(start, start + s);
    return {
        total: records.length,
        pageNum: p,
        pageSize: s,
        records: slice,
    };
}

function readToken(headers = {}) {
    const auth = headers.Authorization || headers.authorization || "";
    if (!auth.startsWith("Bearer ")) {
        return "";
    }
    return auth.slice(7);
}

function userByToken(token) {
    if (!token) return null;
    const userId = Number(String(token).replace("mock-token-", ""));
    return mockStore.users.find((u) => u.id === userId) || null;
}

function requireLogin(headers) {
    const token = readToken(headers);
    const user = userByToken(token);
    if (!user) {
        fail(401, "请先登录");
    }
    return user;
}

function requireAdmin(user) {
    if (user.role !== "ADMIN") {
        fail(403, "无访问权限");
    }
}

function requireSeller(user) {
    if (!["OFFICIAL_SELLER", "SELLER"].includes(user.role)) {
        fail(403, "仅卖家可操作");
    }
}

function asText(value) {
    return String(value || "").trim();
}

export async function handleMockRequest({ method, url, params, data, headers }) {
    const m = String(method || "get").toLowerCase();
    const p = String(url || "");

    if (m === "post" && p === "/auth/login") {
        const username = asText(data?.username);
        const password = asText(data?.password);
        const user = mockStore.users.find((u) => u.username === username && u.password === password);
        if (!user) {
            fail(400, "用户名或密码错误");
        }
        return ok({
            token: `mock-token-${user.id}`,
            role: user.role,
            user: { ...user, password: undefined },
        });
    }

    if (m === "post" && p === "/auth/register") {
        const username = asText(data?.username);
        if (!username) {
            fail(400, "用户名不能为空");
        }
        if (mockStore.users.some((u) => u.username === username)) {
            fail(400, "用户名已存在");
        }
        const id = mockStore.next.userId++;
        mockStore.users.push({
            id,
            username,
            password: asText(data?.password) || "123456",
            nickname: asText(data?.nickname) || username,
            role: "USER",
            status: "NORMAL",
            phone: asText(data?.phone),
            email: asText(data?.email),
            creditScore: 100,
        });
        return ok(null);
    }

    if (m === "get" && (p === "/user/me" || p === "/user/profile")) {
        const user = requireLogin(headers);
        return ok({ ...user, password: undefined });
    }

    if (m === "put" && p === "/user/profile") {
        const user = requireLogin(headers);
        Object.assign(user, {
            nickname: data?.nickname ?? user.nickname,
            avatar: data?.avatar ?? user.avatar,
            phone: data?.phone ?? user.phone,
            email: data?.email ?? user.email,
        });
        return ok({ ...user, password: undefined });
    }

    if (m === "get" && p === "/user/addresses") {
        const user = requireLogin(headers);
        return ok(mockStore.addresses.filter((a) => a.userId === user.id));
    }

    if (m === "post" && p === "/user/addresses") {
        const user = requireLogin(headers);
        const id = mockStore.next.addressId++;
        const address = { id, userId: user.id, ...data };
        mockStore.addresses.push(address);
        return ok(address);
    }

    const updateAddressMatch = p.match(/^\/user\/addresses\/(\d+)$/);
    if (m === "put" && updateAddressMatch) {
        const user = requireLogin(headers);
        const id = Number(updateAddressMatch[1]);
        const found = mockStore.addresses.find((a) => a.id === id && a.userId === user.id);
        if (!found) {
            fail(404, "地址不存在");
        }
        Object.assign(found, data || {});
        return ok(found);
    }

    if (m === "delete" && updateAddressMatch) {
        const user = requireLogin(headers);
        const id = Number(updateAddressMatch[1]);
        const before = mockStore.addresses.length;
        mockStore.addresses = mockStore.addresses.filter((a) => !(a.id === id && a.userId === user.id));
        if (mockStore.addresses.length === before) {
            fail(404, "地址不存在");
        }
        return ok(null);
    }

    if (m === "get" && p === "/product/list") {
        const keyword = asText(params?.keyword);
        const minPrice = params?.minPrice != null ? Number(params.minPrice) : null;
        const maxPrice = params?.maxPrice != null ? Number(params.maxPrice) : null;
        const records = mockStore.products
            .filter((x) => x.status === 1)
            .filter((x) => !keyword || x.name.includes(keyword))
            .filter((x) => minPrice == null || Number(x.price) >= minPrice)
            .filter((x) => maxPrice == null || Number(x.price) <= maxPrice)
            .sort((a, b) => Number(b.id) - Number(a.id));
        return ok(paginate(records, params?.pageNum, params?.pageSize));
    }

    const productDetailMatch = p.match(/^\/product\/detail\/(\d+)$/);
    if (m === "get" && productDetailMatch) {
        const id = Number(productDetailMatch[1]);
        const record = mockStore.products.find((x) => x.id === id && x.status === 1);
        if (!record) {
            fail(404, "商品不存在或已下架");
        }
        return ok(record);
    }

    if (m === "get" && p === "/product/seller/list") {
        const user = requireLogin(headers);
        requireSeller(user);
        const keyword = asText(params?.keyword);
        const status = params?.status != null && params?.status !== "" ? Number(params.status) : null;
        const records = mockStore.products
            .filter((x) => x.shopId === user.id)
            .filter((x) => !keyword || x.name.includes(keyword))
            .filter((x) => status == null || Number(x.status) === status)
            .sort((a, b) => Number(b.id) - Number(a.id));
        return ok(paginate(records, params?.pageNum, params?.pageSize));
    }

    if (m === "post" && p === "/product/seller") {
        const user = requireLogin(headers);
        requireSeller(user);
        const id = mockStore.next.productId++;
        const status = Number(data?.status ?? 1);
        const product = {
            id,
            shopId: user.id,
            name: asText(data?.name),
            cover: asText(data?.cover),
            description: asText(data?.description),
            price: Number(data?.price || 0),
            stock: Number(data?.stock || 0),
            status,
            statusName: status === 1 ? "在售" : "已下架",
            createTime: new Date().toISOString(),
        };
        mockStore.products.push(product);
        return ok(product);
    }

    const updateSellerProductMatch = p.match(/^\/product\/seller\/(\d+)$/);
    if (m === "put" && updateSellerProductMatch) {
        const user = requireLogin(headers);
        requireSeller(user);
        const id = Number(updateSellerProductMatch[1]);
        const product = mockStore.products.find((x) => x.id === id && x.shopId === user.id);
        if (!product) fail(404, "商品不存在");
        Object.assign(product, {
            name: asText(data?.name),
            cover: asText(data?.cover),
            description: asText(data?.description),
            price: Number(data?.price || 0),
            stock: Number(data?.stock || 0),
            status: Number(data?.status ?? product.status),
        });
        product.statusName = product.status === 1 ? "在售" : "已下架";
        return ok(product);
    }

    if (m === "delete" && updateSellerProductMatch) {
        const user = requireLogin(headers);
        requireSeller(user);
        const id = Number(updateSellerProductMatch[1]);
        const before = mockStore.products.length;
        mockStore.products = mockStore.products.filter((x) => !(x.id === id && x.shopId === user.id));
        if (mockStore.products.length === before) fail(404, "商品不存在");
        return ok(null);
    }

    const changeStatusMatch = p.match(/^\/product\/seller\/(\d+)\/status$/);
    if (m === "post" && changeStatusMatch) {
        const user = requireLogin(headers);
        requireSeller(user);
        const id = Number(changeStatusMatch[1]);
        const product = mockStore.products.find((x) => x.id === id && x.shopId === user.id);
        if (!product) fail(404, "商品不存在");
        product.status = Number(data?.status || 0);
        product.statusName = product.status === 1 ? "在售" : "已下架";
        return ok(product);
    }

    const changeStockMatch = p.match(/^\/product\/seller\/(\d+)\/stock\/adjust$/);
    if (m === "post" && changeStockMatch) {
        const user = requireLogin(headers);
        requireSeller(user);
        const id = Number(changeStockMatch[1]);
        const product = mockStore.products.find((x) => x.id === id && x.shopId === user.id);
        if (!product) fail(404, "商品不存在");
        const delta = Number(data?.delta || 0);
        const next = product.stock + delta;
        if (next < 0) fail(400, "库存不足");
        product.stock = next;
        return ok(product);
    }

    if (m === "get" && p === "/secondhand/list") {
        const keyword = asText(params?.keyword);
        const records = mockStore.secondhandProducts
            .filter((x) => x.status === 1)
            .filter((x) => !keyword || x.name.includes(keyword))
            .sort((a, b) => Number(b.id) - Number(a.id));
        return ok(paginate(records, params?.pageNum, params?.pageSize));
    }

    const secondhandDetailMatch = p.match(/^\/secondhand\/detail\/(\d+)$/);
    if (m === "get" && secondhandDetailMatch) {
        const id = Number(secondhandDetailMatch[1]);
        const item = mockStore.secondhandProducts.find((x) => Number(x.id) === id && x.status === 1);
        if (!item) fail(404, "二手商品不存在");
        return ok(item);
    }

    if (m === "post" && p === "/secondhand/seller") {
        const user = requireLogin(headers);
        const id = mockStore.next.secondhandId++;
        const item = {
            id,
            sellerUserId: user.id,
            name: asText(data?.name),
            cover: asText(data?.cover),
            description: asText(data?.description),
            originPrice: Number(data?.originPrice || 0),
            salePrice: Number(data?.salePrice || 0),
            conditionLevel: asText(data?.conditionLevel || data?.condition),
            status: 1,
            statusName: "在售",
            createTime: new Date().toISOString(),
        };
        mockStore.secondhandProducts.unshift(item);
        return ok(item);
    }

    if (m === "get" && p === "/secondhand/seller/list") {
        const user = requireLogin(headers);
        const records = mockStore.secondhandProducts
            .filter((x) => Number(x.sellerUserId) === Number(user.id))
            .sort((a, b) => Number(b.id) - Number(a.id));
        return ok(paginate(records, params?.pageNum, params?.pageSize));
    }

    const secondhandSellerUpdateMatch = p.match(/^\/secondhand\/seller\/(\d+)$/);
    if (m === "put" && secondhandSellerUpdateMatch) {
        const user = requireLogin(headers);
        const id = Number(secondhandSellerUpdateMatch[1]);
        const item = mockStore.secondhandProducts.find((x) => Number(x.id) === id);
        if (!item) fail(404, "二手商品不存在");
        if (Number(item.sellerUserId) !== Number(user.id)) fail(403, "无权操作该二手商品");
        Object.assign(item, {
            name: asText(data?.name || item.name),
            cover: asText(data?.cover || item.cover),
            description: asText(data?.description || item.description),
            originPrice: Number(data?.originPrice ?? item.originPrice ?? 0),
            salePrice: Number(data?.salePrice ?? item.salePrice ?? 0),
            conditionLevel: asText(data?.conditionLevel || data?.condition || item.conditionLevel),
            status: data?.status == null ? item.status : Number(data.status),
        });
        return ok(item);
    }

    const secondhandSellerDeleteMatch = p.match(/^\/secondhand\/seller\/(\d+)$/);
    if (m === "delete" && secondhandSellerDeleteMatch) {
        const user = requireLogin(headers);
        const id = Number(secondhandSellerDeleteMatch[1]);
        const index = mockStore.secondhandProducts.findIndex((x) => Number(x.id) === id);
        if (index < 0) fail(404, "二手商品不存在");
        if (Number(mockStore.secondhandProducts[index].sellerUserId) !== Number(user.id)) fail(403, "无权操作该二手商品");
        mockStore.secondhandProducts.splice(index, 1);
        return ok(null);
    }

    const secondhandSellerStatusMatch = p.match(/^\/secondhand\/seller\/(\d+)\/status$/);
    if (m === "post" && secondhandSellerStatusMatch) {
        const user = requireLogin(headers);
        const id = Number(secondhandSellerStatusMatch[1]);
        const item = mockStore.secondhandProducts.find((x) => Number(x.id) === id);
        if (!item) fail(404, "二手商品不存在");
        if (Number(item.sellerUserId) !== Number(user.id)) fail(403, "无权操作该二手商品");
        item.status = Number(data?.status ?? item.status);
        item.statusName = item.status === 1 ? "在售" : "下架";
        return ok(item);
    }

    const secondhandBuyMatch = p.match(/^\/secondhand\/(\d+)\/buy$/);
    if (m === "post" && secondhandBuyMatch) {
        const user = requireLogin(headers);
        const id = Number(secondhandBuyMatch[1]);
        const item = mockStore.secondhandProducts.find((x) => Number(x.id) === id && x.status === 1);
        if (!item) fail(404, "二手商品不存在");
        if (Number(item.sellerUserId) === Number(user.id)) fail(400, "不能购买自己发布的二手商品");
        item.status = 0;
        item.statusName = "已售出";
        const orderId = mockStore.next.orderId++;
        const order = {
            id: orderId,
            orderNo: `ORDMOCK${String(orderId).padStart(6, "0")}`,
            buyerUserId: user.id,
            totalAmount: Number(item.salePrice || 0),
            payStatus: 1,
            orderStatus: 1,
            createTime: new Date().toISOString(),
            items: [
                {
                    productId: item.id,
                    productName: item.name,
                    itemType: "SECONDHAND",
                    price: Number(item.salePrice || 0),
                    quantity: 1,
                },
            ],
        };
        mockStore.orders.unshift(order);
        return ok(order);
    }

    if (m === "post" && p === "/order/create") {
        const user = requireLogin(headers);
        const items = (data?.items || []).map((it) => {
            const product = mockStore.products.find((x) => x.id === Number(it.productId));
            if (!product) {
                fail(404, `商品不存在: ${it.productId}`);
            }
            return {
                productId: product.id,
                productName: product.name,
                itemType: "NEW",
                price: Number(product.price || 0),
                quantity: Number(it.quantity || 1),
            };
        });
        const totalAmount = items.reduce((sum, x) => sum + Number(x.price) * Number(x.quantity), 0);
        const id = mockStore.next.orderId++;
        const order = {
            id,
            orderNo: `ORDMOCK${String(id).padStart(6, "0")}`,
            buyerUserId: user.id,
            totalAmount,
            payStatus: 1,
            orderStatus: 1,
            createTime: new Date().toISOString(),
            items,
        };
        mockStore.orders.unshift(order);
        return ok(order);
    }

    if (m === "get" && p === "/order/list") {
        const user = requireLogin(headers);
        const records = mockStore.orders.filter((o) => o.buyerUserId === user.id);
        return ok(paginate(records, params?.pageNum, params?.pageSize));
    }

    const orderDetailMatch = p.match(/^\/order\/detail\/(\d+)$/);
    if (m === "get" && orderDetailMatch) {
        const user = requireLogin(headers);
        const id = Number(orderDetailMatch[1]);
        const order = mockStore.orders.find((o) => o.id === id && o.buyerUserId === user.id);
        if (!order) fail(404, "订单不存在");
        return ok(order);
    }

    const orderActionMatch = p.match(/^\/order\/(\d+)\/(pay|cancel|confirm-receive|complete|refund|ship|remind-ship)$/);
    if (m === "post" && orderActionMatch) {
        const user = requireLogin(headers);
        const id = Number(orderActionMatch[1]);
        const action = orderActionMatch[2];
        const order = mockStore.orders.find((o) => o.id === id);
        if (!order) fail(404, "订单不存在");

        if (action === "pay") {
            if (order.buyerUserId !== user.id) fail(403, "无权操作该订单");
            order.payStatus = 1;
            order.orderStatus = 1;
            order.orderStatusName = "待发货";
            return ok(order);
        }
        if (action === "cancel") {
            if (order.buyerUserId !== user.id) fail(403, "无权操作该订单");
            order.orderStatus = 5;
            order.orderStatusName = "已关闭";
            return ok(order);
        }
        if (action === "confirm-receive") {
            if (order.buyerUserId !== user.id) fail(403, "无权操作该订单");
            order.orderStatus = 3;
            order.orderStatusName = "待评价";
            return ok(order);
        }
        if (action === "complete") {
            if (order.buyerUserId !== user.id) fail(403, "无权操作该订单");
            order.orderStatus = 4;
            order.orderStatusName = "已完成";
            return ok(order);
        }
        if (action === "refund") {
            if (order.buyerUserId !== user.id) fail(403, "无权操作该订单");
            order.refundStatus = 1;
            order.refundStatusName = "待处理";
            order.refundReason = asText(data?.reason);
            return ok(order);
        }
        if (action === "ship") {
            const ownNew = (order.items || []).some((item) => {
                if (item.itemType !== "NEW") return false;
                const product = mockStore.products.find((p2) => p2.id === Number(item.productId));
                return product && Number(product.shopId) === Number(user.id);
            });
            if (!ownNew) fail(403, "无权操作该订单");
            order.orderStatus = 2;
            order.orderStatusName = "待收货";
            return ok(order);
        }
        if (action === "remind-ship") {
            if (order.buyerUserId !== user.id) fail(403, "无权操作该订单");
            return ok(null);
        }
    }

    const orderRefundActionMatch = p.match(/^\/order\/(\d+)\/refund\/(approve|reject)$/);
    if (m === "post" && orderRefundActionMatch) {
        const user = requireLogin(headers);
        const id = Number(orderRefundActionMatch[1]);
        const action = orderRefundActionMatch[2];
        const order = mockStore.orders.find((o) => o.id === id);
        if (!order) fail(404, "订单不存在");
        const ownNew = (order.items || []).some((item) => {
            if (item.itemType !== "NEW") return false;
            const product = mockStore.products.find((p2) => p2.id === Number(item.productId));
            return product && Number(product.shopId) === Number(user.id);
        });
        if (!ownNew) fail(403, "无权操作该订单");
        order.refundStatus = action === "approve" ? 2 : 3;
        order.refundStatusName = action === "approve" ? "已通过" : "已拒绝";
        order.orderStatus = 5;
        order.orderStatusName = "已关闭";
        return ok(order);
    }

    const orderReviewMatch = p.match(/^\/order\/(\d+)\/review(\/items)?$/);
    if (m === "post" && orderReviewMatch) {
        const user = requireLogin(headers);
        const id = Number(orderReviewMatch[1]);
        const order = mockStore.orders.find((o) => o.id === id && o.buyerUserId === user.id);
        if (!order) fail(404, "订单不存在");
        order.orderStatus = 4;
        order.orderStatusName = "已完成";
        return ok(order);
    }

    if (m === "get" && p === "/review/my") {
        requireLogin(headers);
        return ok(paginate([], params?.pageNum, params?.pageSize));
    }

    if (m === "get" && p === "/review/seller/list") {
        requireLogin(headers);
        return ok(paginate([], params?.pageNum, params?.pageSize));
    }

    const reviewReplyMatch = p.match(/^\/review\/(\d+)\/reply$/);
    if (m === "post" && reviewReplyMatch) {
        requireLogin(headers);
        return ok(null);
    }

    if (m === "post" && p === "/review/followup") {
        requireLogin(headers);
        return ok(null);
    }

    if (m === "get" && p === "/order/seller/list") {
        const user = requireLogin(headers);
        const records = mockStore.orders.filter((order) => {
            return (order.items || []).some((item) => {
                if (item.itemType === "SECONDHAND") {
                    const secondhand = mockStore.secondhandProducts.find((x) => x.id === Number(item.productId));
                    return secondhand && Number(secondhand.sellerUserId) === Number(user.id);
                }
                const product = mockStore.products.find((x) => x.id === Number(item.productId));
                return product && Number(product.shopId) === Number(user.id);
            });
        });
        return ok(paginate(records, params?.pageNum, params?.pageSize));
    }

    const sellerOrderDetailMatch = p.match(/^\/order\/seller\/detail\/(\d+)$/);
    if (m === "get" && sellerOrderDetailMatch) {
        const user = requireLogin(headers);
        const id = Number(sellerOrderDetailMatch[1]);
        const order = mockStore.orders.find((o) => o.id === id);
        if (!order) fail(404, "订单不存在");
        const canView = (order.items || []).some((item) => {
            if (item.itemType === "SECONDHAND") {
                const secondhand = mockStore.secondhandProducts.find((x) => x.id === Number(item.productId));
                return secondhand && Number(secondhand.sellerUserId) === Number(user.id);
            }
            const product = mockStore.products.find((x) => x.id === Number(item.productId));
            return product && Number(product.shopId) === Number(user.id);
        });
        if (!canView) fail(403, "无权查看该订单");
        return ok(order);
    }

    if (m === "get" && p === "/admin/orders/list") {
        const user = requireLogin(headers);
        requireAdmin(user);
        const keyword = asText(params?.keyword);
        const records = mockStore.orders.filter((o) => {
            if (!keyword) return true;
            const hitOrderNo = String(o.orderNo || "").includes(keyword);
            const hitProduct = (o.items || []).some((i) => String(i.productName || "").includes(keyword));
            return hitOrderNo || hitProduct;
        });
        return ok(paginate(records, params?.pageNum, params?.pageSize));
    }

    const adminOrderDetailMatch = p.match(/^\/admin\/orders\/detail\/(\d+)$/);
    if (m === "get" && adminOrderDetailMatch) {
        const user = requireLogin(headers);
        requireAdmin(user);
        const id = Number(adminOrderDetailMatch[1]);
        const order = mockStore.orders.find((o) => o.id === id);
        if (!order) fail(404, "订单不存在");
        return ok(order);
    }

    if (m === "post" && p === "/admin/orders/batch-close") {
        const user = requireLogin(headers);
        requireAdmin(user);
        const ids = Array.isArray(data?.orderIds) ? data.orderIds.map((x) => Number(x)) : [];
        const successIds = [];
        const failedItems = [];
        ids.forEach((id) => {
            const order = mockStore.orders.find((o) => o.id === id);
            if (!order) {
                failedItems.push({ orderId: id, reason: "订单不存在" });
                return;
            }
            order.orderStatus = 5;
            order.orderStatusName = "已关闭";
            successIds.push(id);
        });
        return ok({ successIds, failedItems });
    }

    const adminRefundMatch = p.match(/^\/admin\/orders\/(\d+)\/refund\/(approve|reject)$/);
    if (m === "post" && adminRefundMatch) {
        const user = requireLogin(headers);
        requireAdmin(user);
        const id = Number(adminRefundMatch[1]);
        const action = adminRefundMatch[2];
        const order = mockStore.orders.find((o) => o.id === id);
        if (!order) fail(404, "订单不存在");
        order.refundStatus = action === "approve" ? 2 : 3;
        order.refundStatusName = action === "approve" ? "已通过" : "已拒绝";
        order.orderStatus = 5;
        order.orderStatusName = "已关闭";
        order.refundDecisionRemark = asText(data?.remark);
        return ok(order);
    }

    const adminAfterSaleLogMatch = p.match(/^\/admin\/orders\/(\d+)\/after-sale-logs$/);
    if (m === "get" && adminAfterSaleLogMatch) {
        const user = requireLogin(headers);
        requireAdmin(user);
        const orderId = Number(adminAfterSaleLogMatch[1]);
        return ok([
            {
                id: 1,
                orderId,
                action: "APPLY",
                operatorRole: "BUYER",
                operatorUserId: 0,
                remark: "买家提交售后申请",
                createTime: new Date().toISOString(),
            },
        ]);
    }

    if (m === "get" && p === "/admin/users") {
        const user = requireLogin(headers);
        requireAdmin(user);
        const keyword = asText(params?.keyword);
        const status = asText(params?.status);
        const records = mockStore.users
            .filter((u) => !keyword || u.username.includes(keyword) || String(u.nickname || "").includes(keyword))
            .filter((u) => !status || u.status === status)
            .map((u) => ({ ...u, password: undefined }));
        return ok(paginate(records, params?.pageNum, params?.pageSize));
    }

    const banUserMatch = p.match(/^\/admin\/users\/(\d+)\/(ban|unban)$/);
    if (m === "put" && banUserMatch) {
        const user = requireLogin(headers);
        requireAdmin(user);
        const id = Number(banUserMatch[1]);
        const action = banUserMatch[2];
        const target = mockStore.users.find((u) => u.id === id);
        if (!target) fail(404, "用户不存在");
        target.status = action === "ban" ? "BANNED" : "NORMAL";
        mockStore.auditLogs.unshift({
            id: mockStore.next.auditLogId++,
            adminUsername: user.username,
            action: action === "ban" ? "BAN_USER" : "UNBAN_USER",
            targetType: "USER",
            targetId: id,
            detail: action === "ban" ? "管理员封禁用户" : "管理员解封用户",
            createTime: new Date().toISOString(),
        });
        return ok(null);
    }

    if (m === "get" && p === "/admin/audit-logs") {
        const user = requireLogin(headers);
        requireAdmin(user);
        const adminUsername = asText(params?.adminUsername);
        const action = asText(params?.action);
        const targetType = asText(params?.targetType);
        const records = mockStore.auditLogs
            .filter((x) => !adminUsername || x.adminUsername.includes(adminUsername))
            .filter((x) => !action || x.action === action)
            .filter((x) => !targetType || x.targetType === targetType);
        return ok(paginate(records, params?.pageNum, params?.pageSize));
    }

    if (m === "post" && p === "/user/merchant-application") {
        const user = requireLogin(headers);
        const existing = mockStore.merchantApplications.find((x) => x.userId === user.id);
        if (existing) {
            Object.assign(existing, data, {
                username: user.username,
                status: 0,
                rejectReason: "",
                applyTime: new Date().toISOString(),
            });
            return ok(existing);
        }
        const id = mockStore.next.merchantApplicationId++;
        const app = {
            id,
            userId: user.id,
            username: user.username,
            status: 0,
            rejectReason: "",
            applyTime: new Date().toISOString(),
            ...data,
        };
        mockStore.merchantApplications.unshift(app);
        return ok(app);
    }

    if (m === "get" && (p === "/user/merchant-application/me" || p === "/user/merchant-application/me.")) {
        const user = requireLogin(headers);
        const app = mockStore.merchantApplications.find((x) => x.userId === user.id) || null;
        return ok(app);
    }

    if (m === "get" && p === "/admin/merchant-applications") {
        const user = requireLogin(headers);
        requireAdmin(user);
        const status = params?.status != null && params?.status !== "" ? Number(params.status) : null;
        const records = mockStore.merchantApplications.filter((x) => status == null || Number(x.status) === status);
        return ok(paginate(records, params?.pageNum, params?.pageSize));
    }

    const approveAppMatch = p.match(/^\/admin\/merchant-applications\/(\d+)\/approve$/);
    if (m === "post" && approveAppMatch) {
        const user = requireLogin(headers);
        requireAdmin(user);
        const id = Number(approveAppMatch[1]);
        const app = mockStore.merchantApplications.find((x) => x.id === id);
        if (!app) fail(404, "申请不存在");
        app.status = 1;
        const targetUser = mockStore.users.find((x) => x.id === app.userId);
        if (targetUser) {
            targetUser.role = "OFFICIAL_SELLER";
        }
        return ok(app);
    }

    const rejectAppMatch = p.match(/^\/admin\/merchant-applications\/(\d+)\/reject$/);
    if (m === "post" && rejectAppMatch) {
        const user = requireLogin(headers);
        requireAdmin(user);
        const id = Number(rejectAppMatch[1]);
        const app = mockStore.merchantApplications.find((x) => x.id === id);
        if (!app) fail(404, "申请不存在");
        app.status = 2;
        app.rejectReason = asText(data?.rejectReason);
        return ok(app);
    }

    if (m === "post" && p === "/upload/image") {
        const ts = Date.now();
        return ok({ url: `/uploads/mock-${ts}.png`, filename: `mock-${ts}.png` });
    }

    fail(404, `Mock route not found: ${m.toUpperCase()} ${p}`);
}
