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

function userPublic(user) {
    if (!user) {
        return null;
    }
    return {
        id: user.id,
        username: user.username,
        nickname: user.nickname,
        role: user.role,
        avatar: user.avatar,
    };
}

function creditLevel(score) {
    const value = Number(score || 100);
    if (value < 60) return "较差";
    if (value < 80) return "良好";
    if (value < 95) return "优秀";
    return "极好";
}

function chatConversationVO(conversation, currentUserId) {
    const otherId = (conversation.participantIds || []).find((id) => Number(id) !== Number(currentUserId));
    const other = mockStore.users.find((user) => Number(user.id) === Number(otherId));
    return {
        id: conversation.id,
        other: userPublic(other),
        participantIds: conversation.participantIds,
        sourceType: conversation.sourceType,
        sourceId: conversation.sourceId,
        sourceTitle: conversation.sourceTitle,
        lastMessageContent: conversation.lastMessageContent,
        lastMessageTime: conversation.lastMessageTime,
        unreadCount: Number(conversation.unreadByUserId) === Number(currentUserId) ? 1 : 0,
    };
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
            orderStatusName: "待发货",
            refundStatus: 0,
            refundStatusName: "",
            createTime: new Date().toISOString(),
            items: [
                {
                    id: `${orderId}-1`,
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
        const items = (data?.items || []).map((it, idx) => {
            const product = mockStore.products.find((x) => x.id === Number(it.productId));
            if (!product) {
                fail(404, `商品不存在: ${it.productId}`);
            }
            return {
                id: `${product.id}-${idx + 1}`,
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
            orderStatusName: "待发货",
            refundStatus: 0,
            refundStatusName: "",
            createTime: new Date().toISOString(),
            items,
        };
        mockStore.orders.unshift(order);
        return ok(order);
    }

    if (m === "get" && p === "/order/list") {
        const user = requireLogin(headers);
        let records = mockStore.orders.filter((o) => o.buyerUserId === user.id);
        const keyword = asText(params?.keyword);
        const rawStatus = params?.orderStatus;
        if (rawStatus !== undefined && rawStatus !== null && rawStatus !== "") {
            const st = Number(rawStatus);
            records = records.filter((o) => Number(o.orderStatus) === st);
        }
        const rawRefundStatus = params?.refundStatus;
        if (rawRefundStatus !== undefined && rawRefundStatus !== null && rawRefundStatus !== "") {
            const st = Number(rawRefundStatus);
            records = records.filter((o) => Number(o.refundStatus || 0) === st);
        }
        if (params?.afterSaleOnly === true || params?.afterSaleOnly === "true" || Number(params?.afterSaleOnly) === 1) {
            records = records.filter((o) => Number(o.refundStatus || 0) > 0);
        }
        if (keyword) {
            records = records.filter((o) => {
                const hitOrderNo = String(o.orderNo || "").includes(keyword);
                const hitProduct = (o.items || []).some((i) => String(i.productName || "").includes(keyword));
                return hitOrderNo || hitProduct;
            });
        }
        const minAmount = params?.minAmount === undefined || params?.minAmount === null || params?.minAmount === "" ? null : Number(params.minAmount);
        const maxAmount = params?.maxAmount === undefined || params?.maxAmount === null || params?.maxAmount === "" ? null : Number(params.maxAmount);
        if (minAmount !== null && Number.isFinite(minAmount)) {
            records = records.filter((o) => Number(o.totalAmount || 0) >= minAmount);
        }
        if (maxAmount !== null && Number.isFinite(maxAmount)) {
            records = records.filter((o) => Number(o.totalAmount || 0) <= maxAmount);
        }
        const startTime = params?.startTime ? Number(params.startTime) : null;
        const endTime = params?.endTime ? Number(params.endTime) : null;
        if (startTime) {
            records = records.filter((o) => new Date(o.createTime).getTime() >= startTime);
        }
        if (endTime) {
            records = records.filter((o) => new Date(o.createTime).getTime() <= endTime);
        }
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
            order.orderStatus = 9;
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
            order.refundProofUrls = Array.isArray(data?.proofUrls) ? data.proofUrls.join(",") : asText(data?.proofUrls || "");
            order.refundApplyTime = new Date().toISOString();
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
        order.orderStatus = 9;
        order.orderStatusName = "已关闭";
        order.refundDecisionSource = "SELLER";
        order.refundDecisionTime = new Date().toISOString();
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
            order.orderStatus = 9;
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
        order.orderStatus = 9;
        order.orderStatusName = "已关闭";
        order.refundDecisionSource = "ADMIN";
        order.refundDecisionRemark = asText(data?.remark);
        order.refundDecisionTime = new Date().toISOString();
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

    if (m === "get" && p === "/chat/conversations") {
        const user = requireLogin(headers);
        const records = mockStore.chatConversations
            .filter((item) => (item.participantIds || []).some((id) => Number(id) === Number(user.id)))
            .sort((a, b) => new Date(b.lastMessageTime || 0).getTime() - new Date(a.lastMessageTime || 0).getTime())
            .map((item) => chatConversationVO(item, user.id));
        return ok(records);
    }

    if (m === "post" && p === "/chat/conversations") {
        const user = requireLogin(headers);
        const targetUserId = Number(data?.targetUserId);
        if (!targetUserId || targetUserId === Number(user.id)) {
            fail(400, "会话对象不正确");
        }
        const target = mockStore.users.find((item) => Number(item.id) === targetUserId);
        if (!target) {
            fail(404, "用户不存在");
        }
        const sourceType = asText(data?.sourceType || "DIRECT").toUpperCase();
        const sourceId = data?.sourceId == null ? null : Number(data.sourceId);
        let sourceTitle = "站内私聊";
        if (sourceType === "PRODUCT") {
            sourceTitle = mockStore.products.find((item) => Number(item.id) === sourceId)?.name || "商品咨询";
        }
        if (sourceType === "SECONDHAND") {
            sourceTitle = mockStore.secondhandProducts.find((item) => Number(item.id) === sourceId)?.name || "二手商品咨询";
        }
        let conversation = mockStore.chatConversations.find((item) => {
            const members = item.participantIds || [];
            return members.includes(user.id)
                && members.includes(targetUserId)
                && item.sourceType === sourceType
                && Number(item.sourceId || 0) === Number(sourceId || 0);
        });
        if (!conversation) {
            conversation = {
                id: mockStore.next.chatConversationId++,
                participantIds: [user.id, targetUserId],
                sourceType,
                sourceId,
                sourceTitle,
                lastMessageContent: "",
                lastMessageTime: new Date().toISOString(),
                unreadByUserId: 0,
            };
            mockStore.chatConversations.unshift(conversation);
        }
        return ok(chatConversationVO(conversation, user.id));
    }

    const chatMessagesMatch = p.match(/^\/chat\/conversations\/(\d+)\/messages$/);
    if (m === "get" && chatMessagesMatch) {
        const user = requireLogin(headers);
        const conversationId = Number(chatMessagesMatch[1]);
        const conversation = mockStore.chatConversations.find((item) => Number(item.id) === conversationId);
        if (!conversation || !(conversation.participantIds || []).some((id) => Number(id) === Number(user.id))) {
            fail(404, "会话不存在");
        }
        conversation.unreadByUserId = 0;
        const records = mockStore.chatMessages
            .filter((item) => Number(item.conversationId) === conversationId)
            .sort((a, b) => new Date(a.createTime || 0).getTime() - new Date(b.createTime || 0).getTime())
            .map((item) => ({ ...item, sender: userPublic(item.sender) }));
        return ok(records);
    }

    if (m === "post" && chatMessagesMatch) {
        const user = requireLogin(headers);
        const conversationId = Number(chatMessagesMatch[1]);
        const conversation = mockStore.chatConversations.find((item) => Number(item.id) === conversationId);
        if (!conversation || !(conversation.participantIds || []).some((id) => Number(id) === Number(user.id))) {
            fail(404, "会话不存在");
        }
        const content = asText(data?.content);
        if (!content) {
            fail(400, "消息内容不能为空");
        }
        const targetId = (conversation.participantIds || []).find((id) => Number(id) !== Number(user.id));
        const message = {
            id: mockStore.next.chatMessageId++,
            conversationId,
            senderUserId: user.id,
            sender: userPublic(user),
            content,
            createTime: new Date().toISOString(),
        };
        mockStore.chatMessages.push(message);
        conversation.lastMessageContent = content;
        conversation.lastMessageTime = message.createTime;
        conversation.unreadByUserId = targetId || 0;
        return ok(message);
    }

    if (m === "get" && p === "/credit/me") {
        const user = requireLogin(headers);
        const baseScore = Number(user.creditScore || 100);
        const buyerLogs = mockStore.creditLogs.filter((item) => Number(item.userId) === Number(user.id) && item.role === "BUYER");
        const shSellerLogs = mockStore.creditLogs.filter((item) => Number(item.userId) === Number(user.id) && item.role === "SH_SELLER");
        const buyerScore = Math.max(0, Math.min(100, baseScore + buyerLogs.reduce((sum, item) => sum + Number(item.delta || 0), 0)));
        const shSellerScore = Math.max(0, Math.min(100, 96 + shSellerLogs.reduce((sum, item) => sum + Number(item.delta || 0), 0)));
        return ok({
            buyerScore,
            buyerLevel: creditLevel(buyerScore),
            buyerLogs,
            shSellerScore,
            shSellerLevel: creditLevel(shSellerScore),
            shSellerLogs,
        });
    }

    const userCreditMatch = p.match(/^\/credit\/(\d+)$/);
    if (m === "get" && userCreditMatch) {
        requireLogin(headers);
        const target = mockStore.users.find((item) => Number(item.id) === Number(userCreditMatch[1]));
        if (!target) fail(404, "用户不存在");
        const score = Number(target.creditScore || 100);
        return ok({ userId: target.id, buyerScore: score, buyerLevel: creditLevel(score), shSellerScore: score, shSellerLevel: creditLevel(score) });
    }

    if (m === "post" && p === "/report-block/report") {
        const user = requireLogin(headers);
        const reportedId = Number(data?.reportedId);
        if (!reportedId) fail(400, "被举报用户不能为空");
        const report = {
            id: mockStore.next.reportId++,
            reporterId: user.id,
            reportedId,
            tradeContext: asText(data?.tradeContext || "SH_BUYER"),
            reasonType: asText(data?.reasonType || "OTHER"),
            reasonDesc: asText(data?.reasonDesc),
            evidenceUrls: asText(data?.evidenceUrls),
            status: 0,
            createTime: new Date().toISOString(),
        };
        mockStore.reports.unshift(report);
        return ok(report);
    }

    if (m === "get" && p === "/report-block/report/my") {
        const user = requireLogin(headers);
        const records = mockStore.reports.filter((item) => Number(item.reporterId) === Number(user.id));
        return ok(paginate(records, params?.page, params?.size));
    }

    if (m === "post" && p === "/report-block/block") {
        const user = requireLogin(headers);
        const targetUserId = Number(data?.targetUserId);
        if (!targetUserId || targetUserId === Number(user.id)) fail(400, "拉黑用户不正确");
        const exists = mockStore.blocks.some((item) => Number(item.blockerId) === Number(user.id) && Number(item.blockedId) === targetUserId);
        if (!exists) {
            mockStore.blocks.unshift({
                id: mockStore.next.blockId++,
                blockerId: user.id,
                blockedId: targetUserId,
                createTime: new Date().toISOString(),
            });
        }
        return ok(null);
    }

    const unblockMatch = p.match(/^\/report-block\/block\/(\d+)$/);
    if (m === "delete" && unblockMatch) {
        const user = requireLogin(headers);
        const targetUserId = Number(unblockMatch[1]);
        mockStore.blocks = mockStore.blocks.filter((item) => !(Number(item.blockerId) === Number(user.id) && Number(item.blockedId) === targetUserId));
        return ok(null);
    }

    if (m === "get" && p === "/report-block/block/my") {
        const user = requireLogin(headers);
        return ok(mockStore.blocks.filter((item) => Number(item.blockerId) === Number(user.id)));
    }

    const blockCheckMatch = p.match(/^\/report-block\/block\/check\/(\d+)$/);
    if (m === "get" && blockCheckMatch) {
        const user = requireLogin(headers);
        const targetUserId = Number(blockCheckMatch[1]);
        return ok(mockStore.blocks.some((item) => Number(item.blockerId) === Number(user.id) && Number(item.blockedId) === targetUserId));
    }

    const blockedByMatch = p.match(/^\/report-block\/block\/blocked-by\/(\d+)$/);
    if (m === "get" && blockedByMatch) {
        const user = requireLogin(headers);
        const targetUserId = Number(blockedByMatch[1]);
        return ok(mockStore.blocks.some((item) => Number(item.blockerId) === targetUserId && Number(item.blockedId) === Number(user.id)));
    }

    if (m === "post" && p === "/upload/image") {
        const ts = Date.now();
        return ok({ url: `/uploads/mock-${ts}.png`, filename: `mock-${ts}.png` });
    }

    if (m === "get" && p === "/notifications") {
        const user = requireLogin(headers);
        const scope = asText(params?.scope).toLowerCase();
        const records = mockStore.notifications
            .filter((item) => Number(item.userId) === Number(user.id))
            .filter((item) => !scope || item.scope === scope)
            .sort((a, b) => {
                const readDiff = Number(a.isRead || 0) - Number(b.isRead || 0);
                if (readDiff !== 0) return readDiff;
                return new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime();
            })
            .map((item) => ({ ...item }));
        return ok(records);
    }

    const notificationReadMatch = p.match(/^\/notifications\/(\d+)\/read$/);
    if (m === "post" && notificationReadMatch) {
        const user = requireLogin(headers);
        const id = Number(notificationReadMatch[1]);
        const notification = mockStore.notifications.find(
            (item) => Number(item.id) === id && Number(item.userId) === Number(user.id),
        );
        if (!notification) {
            fail(404, "通知不存在");
        }
        notification.isRead = 1;
        return ok(null);
    }

    if (m === "post" && p === "/notifications/read-all") {
        const user = requireLogin(headers);
        const scope = asText(params?.scope).toLowerCase();
        mockStore.notifications
            .filter((item) => Number(item.userId) === Number(user.id))
            .filter((item) => !scope || item.scope === scope)
            .forEach((item) => {
                item.isRead = 1;
            });
        return ok(null);
    }

    fail(404, `Mock route not found: ${m.toUpperCase()} ${p}`);
}
