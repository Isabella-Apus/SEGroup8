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

    if (m === "post" && p === "/secondhand/publish") {
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
                price: product.price,
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
