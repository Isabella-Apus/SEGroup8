import { mockStore } from "./store";
import {
    ALL_CATEGORY,
    matchProductCategory,
    matchSecondhandCategory,
    productCategories,
    secondhandCategories,
} from "../utils/categoryRules";
import { searchList } from "../utils/search/searchService";

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

const categoryAliases = {
    electronics: "电子数码",
    digital: "电子数码",
    clothing: "服装鞋包",
    clothes: "服装鞋包",
    food: "生活百货",
    home: "生活百货",
    sports: "运动户外",
    books: "学习办公",
    office: "学习办公",
    other: "生活百货",
};

const mockCategoryTree = [
    {
        id: 1,
        name: "电子数码",
        parentId: null,
        children: [
            { id: 101, name: "手机", parentId: 1, children: [] },
            { id: 102, name: "电脑/平板", parentId: 1, children: [] },
            { id: 103, name: "摄影摄像", parentId: 1, children: [] },
            { id: 104, name: "影音娱乐", parentId: 1, children: [] },
            { id: 105, name: "智能穿戴", parentId: 1, children: [] },
        ],
    },
    {
        id: 2,
        name: "服饰鞋包",
        parentId: null,
        children: [
            { id: 201, name: "女装", parentId: 2, children: [] },
            { id: 202, name: "男装", parentId: 2, children: [] },
            { id: 203, name: "运动服饰", parentId: 2, children: [] },
            { id: 204, name: "鞋包", parentId: 2, children: [] },
            { id: 205, name: "配饰", parentId: 2, children: [] },
        ],
    },
    {
        id: 3,
        name: "家居生活",
        parentId: null,
        children: [
            { id: 301, name: "家具家装", parentId: 3, children: [] },
            { id: 302, name: "厨房用具", parentId: 3, children: [] },
            { id: 303, name: "居家日用", parentId: 3, children: [] },
            { id: 304, name: "家用电器", parentId: 3, children: [] },
            { id: 305, name: "收纳整理", parentId: 3, children: [] },
        ],
    },
    {
        id: 4,
        name: "美妆个护",
        parentId: null,
        children: [
            { id: 401, name: "面部护肤", parentId: 4, children: [] },
            { id: 402, name: "彩妆", parentId: 4, children: [] },
            { id: 403, name: "个人护理", parentId: 4, children: [] },
            { id: 404, name: "香水香氛", parentId: 4, children: [] },
            { id: 405, name: "美容仪器", parentId: 4, children: [] },
        ],
    },
    {
        id: 5,
        name: "运动户外",
        parentId: null,
        children: [
            { id: 501, name: "健身器材", parentId: 5, children: [] },
            { id: 502, name: "户外装备", parentId: 5, children: [] },
            { id: 503, name: "体育用品", parentId: 5, children: [] },
            { id: 504, name: "骑行运动", parentId: 5, children: [] },
        ],
    },
    {
        id: 6,
        name: "图书音像",
        parentId: null,
        children: [
            { id: 601, name: "教材教辅", parentId: 6, children: [] },
            { id: 602, name: "小说文学", parentId: 6, children: [] },
            { id: 603, name: "艺术收藏", parentId: 6, children: [] },
            { id: 604, name: "办公文具", parentId: 6, children: [] },
        ],
    },
    {
        id: 7,
        name: "美食",
        parentId: null,
        children: [
            { id: 701, name: "休闲零食", parentId: 7, children: [] },
            { id: 702, name: "粮油调味", parentId: 7, children: [] },
            { id: 703, name: "生鲜果蔬", parentId: 7, children: [] },
            { id: 704, name: "冲调饮品", parentId: 7, children: [] },
            { id: 705, name: "地方特产", parentId: 7, children: [] },
        ],
    },
    {
        id: 8,
        name: "其他",
        parentId: null,
        children: [
            { id: 801, name: "未分类", parentId: 8, children: [] },
        ],
    },
];

function cloneCategoryTree(scene) {
    const excludeFood = String(scene || "NEW").toUpperCase() === "SECONDHAND";
    return mockCategoryTree
        .filter((node) => !(excludeFood && Number(node.id) === 7))
        .map((node) => ({
            ...node,
            children: (node.children || []).map((child) => ({ ...child })),
        }));
}

function findRootCategoryName(id) {
    const target = Number(id);
    if (!target) {
        return "";
    }
    for (const root of mockCategoryTree) {
        if (Number(root.id) === target || (root.children || []).some((child) => Number(child.id) === target)) {
            return root.name;
        }
    }
    return "";
}

function normalizeCategory(value) {
    const raw = asText(value);
    return categoryAliases[raw] || raw;
}

function filterByKeyword(records, keyword) {
    if (!keyword) {
        return records;
    }
    return searchList({
        items: records,
        keyword,
        keys: ["name", "description", "categoryName", "category"],
        options: { threshold: 0.42 },
    });
}

function resolveProductCategory(source = {}) {
    const direct = normalizeCategory(source.categoryName || source.category);
    if (productCategories.includes(direct)) {
        return direct;
    }
    const byId = findRootCategoryName(source.subCategoryId || source.categoryId);
    if (byId) {
        return byId;
    }
    return productCategories.find((category) => category !== ALL_CATEGORY && matchProductCategory(source, category)) || "生活百货";
}

function resolveSecondhandCategory(source = {}) {
    const direct = normalizeCategory(source.categoryName || source.category);
    if (secondhandCategories.includes(direct)) {
        return direct;
    }
    const byId = findRootCategoryName(source.subCategoryId || source.categoryId);
    if (byId) {
        return byId;
    }
    return secondhandCategories.find((category) => category !== ALL_CATEGORY && matchSecondhandCategory(source, category)) || "宿舍生活";
}

function normalizeBrowseProductType(value) {
    const type = String(value || "NEW").toUpperCase();
    return ["SECONDHAND", "SH", "IDLE"].includes(type) ? "SECONDHAND" : "NEW";
}

function upsertBrowseHistory(user, payload = {}) {
    const productId = Number(payload.productId || payload.id || payload.sourceId);
    if (!productId) {
        fail(400, "商品 ID 不能为空");
    }

    const productType = normalizeBrowseProductType(payload.productType || payload.type || payload.sourceType);
    const source =
        productType === "SECONDHAND"
            ? mockStore.secondhandProducts.find((item) => Number(item.id) === productId && Number(item.status) === 1)
            : mockStore.products.find((item) => Number(item.id) === productId && Number(item.status) === 1);

    if (!source) {
        fail(404, "商品不存在或已下架");
    }

    const existing = mockStore.browseHistory.find((item) => (
        Number(item.userId) === Number(user.id) &&
        String(item.recordType || "").toLowerCase() === "product" &&
        normalizeBrowseProductType(item.productType) === productType &&
        Number(item.product?.id) === productId
    ));

    const record = {
        userId: user.id,
        recordType: "product",
        productType,
        product: {
            id: source.id,
            name: source.name,
            cover: source.cover,
            price: productType === "SECONDHAND" ? source.salePrice : source.price,
        },
        browseTime: new Date().toISOString(),
    };

    if (existing) {
        Object.assign(existing, record);
        return existing;
    }

    const created = {
        id: mockStore.next.browseHistoryId++,
        ...record,
    };
    mockStore.browseHistory.unshift(created);
    return created;
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

function shopOwnerById(shopId) {
    return mockStore.users.find(
        (user) => Number(user.id) === Number(shopId) && ["OFFICIAL_SELLER", "SELLER"].includes(user.role),
    );
}

function sellerShopVO(owner) {
    if (!owner) {
        return null;
    }
    const products = mockStore.products.filter((item) => Number(item.shopId) === Number(owner.id));
    const reviews = mockStore.reviews.filter((item) => Number(item.sellerUserId) === Number(owner.id));
    const goodCount = reviews.filter((item) => Number(item.score || item.rating || 0) >= 4).length;
    const goodRate = reviews.length ? (goodCount / reviews.length) * 100 : 100;
    const shopScore = Number(owner.creditScore || 100);
    return {
        id: owner.id,
        ownerUserId: owner.id,
        name: owner.shopName || owner.nickname || "Kinda 商家",
        description: owner.shopDesc || "销售学习设备、数码配件和宿舍生活用品。",
        logo: owner.avatar || "",
        bannerUrl: owner.bannerUrl || "",
        region: owner.region || "",
        category: owner.category || "",
        businessHours: owner.businessHours || "",
        returnPolicy: owner.returnPolicy || "",
        shippingPolicy: owner.shippingPolicy || "",
        announcement: owner.announcement || "",
        decorationJson: owner.shopDecorationJson || "",
        rating: {
            overallScore: shopScore,
            overallLevel: creditLevel(shopScore),
            shopScore,
            shopLevel: creditLevel(shopScore),
            shopSoldCount: products.filter((item) => Number(item.status) === 1).length,
            shopGoodRate: goodRate,
        },
    };
}

function sellerOwnsOrder(order, sellerId) {
    return (order?.items || []).some((item) => {
        if (item.itemType === "SECONDHAND" || item.productType === "SECONDHAND") {
            const secondhand = mockStore.secondhandProducts.find((x) => Number(x.id) === Number(item.productId));
            return secondhand && Number(secondhand.sellerUserId) === Number(sellerId);
        }
        const product = mockStore.products.find((x) => Number(x.id) === Number(item.productId));
        return product && Number(product.shopId) === Number(sellerId);
    });
}

function reviewSellerId(item) {
    if (item.productType === "SECONDHAND") {
        return mockStore.secondhandProducts.find((x) => Number(x.id) === Number(item.productId))?.sellerUserId || 0;
    }
    return mockStore.products.find((x) => Number(x.id) === Number(item.productId))?.shopId || 0;
}

function orderItemType(item = {}) {
    return String(item.productType || item.itemType || "NEW").toUpperCase() === "SECONDHAND" ? "SECONDHAND" : "NEW";
}

function orderType(order = {}) {
    const explicit = String(order.orderType || "").toUpperCase();
    if (explicit === "NEW" || explicit === "SECONDHAND") {
        return explicit;
    }
    return (order.items || []).some((item) => orderItemType(item) === "SECONDHAND") ? "SECONDHAND" : "NEW";
}

function filterOrders(records, params = {}) {
    let result = [...records];
    const keyword = asText(params.keyword);
    const rawStatus = params.orderStatus;
    const rawRefundStatus = params.refundStatus;
    const rawOrderType = asText(params.productType || params.orderType).toUpperCase();
    if (rawOrderType === "NEW" || rawOrderType === "SECONDHAND") {
        result = result.filter((o) => orderType(o) === rawOrderType);
    }
    if (rawStatus !== undefined && rawStatus !== null && rawStatus !== "") {
        const st = Number(rawStatus);
        result = result.filter((o) => Number(o.orderStatus) === st);
    }
    if (rawRefundStatus !== undefined && rawRefundStatus !== null && rawRefundStatus !== "") {
        const st = Number(rawRefundStatus);
        result = result.filter((o) => Number(o.refundStatus || 0) === st);
    }
    if (params.afterSaleOnly === true || params.afterSaleOnly === "true" || Number(params.afterSaleOnly) === 1) {
        result = result.filter((o) => Number(o.refundStatus || 0) > 0);
    }
    if (keyword) {
        result = result.filter((o) => {
            const hitOrderNo = String(o.orderNo || "").includes(keyword);
            const hitProduct = (o.items || []).some((i) => String(i.productName || "").includes(keyword));
            return hitOrderNo || hitProduct;
        });
    }
    const minAmount = params.minAmount === undefined || params.minAmount === null || params.minAmount === "" ? null : Number(params.minAmount);
    const maxAmount = params.maxAmount === undefined || params.maxAmount === null || params.maxAmount === "" ? null : Number(params.maxAmount);
    if (minAmount !== null && Number.isFinite(minAmount)) {
        result = result.filter((o) => Number(o.totalAmount || 0) >= minAmount);
    }
    if (maxAmount !== null && Number.isFinite(maxAmount)) {
        result = result.filter((o) => Number(o.totalAmount || 0) <= maxAmount);
    }
    const startTime = params.startTime ? Number(params.startTime) : null;
    const endTime = params.endTime ? Number(params.endTime) : null;
    if (startTime) {
        result = result.filter((o) => new Date(o.createTime).getTime() >= startTime);
    }
    if (endTime) {
        result = result.filter((o) => new Date(o.createTime).getTime() <= endTime);
    }
    return result.sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());
}

function paginateParams(records, params = {}) {
    return paginate(records, params.pageNum || params.page || 1, params.pageSize || params.size || 10);
}

function addLogisticsTrace(order, nodeName, statusDesc) {
    const trace = {
        id: mockStore.next.logisticsTraceId++,
        orderId: order.id,
        nodeName,
        statusDesc,
        createTime: new Date().toISOString(),
    };
    mockStore.logisticsTraces.unshift(trace);
    return trace;
}

function ensureInitialLogistics(order) {
    if (!order || Number(order.orderStatus) < 2) return;
    const hasTrace = mockStore.logisticsTraces.some((trace) => Number(trace.orderId) === Number(order.id));
    if (!hasTrace) {
        addLogisticsTrace(order, "卖家已发货", "包裹已由卖家交付物流");
    }
}

function createSecondhandOrder(user, item, options = {}) {
    const orderId = mockStore.next.orderId++;
    const price = Number(options.price ?? item.salePrice ?? 0);
    const address = mockStore.addresses.find((addr) =>
        Number(addr.userId) === Number(user.id) &&
        (options.addressId ? Number(addr.id) === Number(options.addressId) : Number(addr.isDefault) === 1)
    ) || mockStore.addresses.find((addr) => Number(addr.userId) === Number(user.id));
    const order = {
        id: orderId,
        orderNo: `ORDMOCK${String(orderId).padStart(6, "0")}`,
        buyerUserId: user.id,
        orderType: "SECONDHAND",
        totalAmount: price,
        payableAmount: price,
        payStatus: 0,
        payMethod: "",
        orderStatus: 0,
        orderStatusName: "待付款",
        refundStatus: 0,
        refundStatusName: "",
        deliveryNo: "",
        logisticsStatus: "PENDING",
        receiverName: address?.receiverName || "",
        receiverPhone: address?.receiverPhone || "",
        receiverProvince: address?.province || "",
        receiverCity: address?.city || "",
        receiverDetailAddress: address?.detailAddress || "",
        createTime: new Date().toISOString(),
        items: [
            {
                id: `${orderId}-1`,
                productId: item.id,
                productName: item.name,
                itemType: "SECONDHAND",
                productType: "SECONDHAND",
                sellerUserId: item.sellerUserId,
                sellerName: item.sellerName,
                conditionLevel: item.conditionLevel,
                price,
                quantity: 1,
            },
        ],
        secondhandSource: options.source || "DIRECT",
    };
    mockStore.orders.unshift(order);
    item.status = 0;
    item.statusName = "已售出";
    return order;
}

function negotiationVO(negotiation) {
    if (!negotiation) return null;
    const product = mockStore.secondhandProducts.find((item) => Number(item.id) === Number(negotiation.productId));
    const buyer = mockStore.users.find((item) => Number(item.id) === Number(negotiation.buyerUserId));
    const seller = mockStore.users.find((item) => Number(item.id) === Number(negotiation.sellerUserId));
    const order = mockStore.orders.find((item) => Number(item.id) === Number(negotiation.orderId));
    return {
        ...negotiation,
        productName: product?.name || "",
        productCover: product?.cover || "",
        productStatus: product?.status,
        buyerName: buyer?.nickname || buyer?.username || "",
        sellerName: product?.sellerName || seller?.nickname || seller?.username || "",
        orderNo: order?.orderNo || "",
    };
}

function auctionStatusName(status) {
    return ({ ONGOING: "进行中", FINISHED: "已结束", FLOW: "已流拍", CLOSED: "已关闭" })[status] || status || "";
}

function auctionVO(auction) {
    if (!auction) return null;
    const product = mockStore.secondhandProducts.find((item) => Number(item.id) === Number(auction.productId));
    const bidder = mockStore.users.find((item) => Number(item.id) === Number(auction.currentBidderUserId));
    return {
        ...auction,
        productName: product?.name || "",
        sellerName: product?.sellerName || "",
        statusName: auctionStatusName(auction.status),
        currentBidderName: bidder?.nickname || bidder?.username || "",
    };
}

function settleAuctionIfNeeded(auction) {
    if (!auction || auction.status !== "ONGOING") return auction;
    if (new Date(auction.endTime || 0).getTime() > Date.now()) return auction;
    if (!auction.currentBidderUserId) {
        auction.status = "FLOW";
        auction.statusName = "已流拍";
        return auction;
    }
    const item = mockStore.secondhandProducts.find((x) => Number(x.id) === Number(auction.productId));
    const bidder = mockStore.users.find((x) => Number(x.id) === Number(auction.currentBidderUserId));
    if (item && bidder && Number(item.status) === 1) {
        const order = createSecondhandOrder(bidder, item, {
            price: Number(auction.currentPrice || auction.startPrice || 0),
            source: "AUCTION",
        });
        auction.settledOrderId = order.id;
    }
    auction.status = "FINISHED";
    auction.statusName = "已结束";
    return auction;
}

function findAuctionByProduct(productId) {
    const auctions = mockStore.productAuctions
        .filter((item) => Number(item.productId) === Number(productId))
        .sort((a, b) => Number(b.id) - Number(a.id));
    auctions.forEach(settleAuctionIfNeeded);
    return auctions.find((item) => item.status === "ONGOING") || auctions[0] || null;
}

function voucherStatusName(status) {
    return ({ 0: "已关闭", 1: "进行中", 2: "未开始", 3: "已结束" })[Number(status)] || "进行中";
}

function voucherPublicVO(voucher, userId) {
    const seller = mockStore.users.find((item) => Number(item.id) === Number(voucher.sellerUserId));
    const claimedUserIds = voucher.claimedUserIds || [];
    return {
        ...voucher,
        mallType: String(voucher.mallType || "NEW").toUpperCase(),
        mallTypeName: voucher.mallTypeName || (String(voucher.mallType || "NEW").toUpperCase() === "SECONDHAND" ? "二手商城" : "新品商城"),
        sellerName: seller?.shopName || seller?.nickname || "Kinda 商家",
        claimed: claimedUserIds.some((id) => Number(id) === Number(userId)),
        remainCount: Math.max(0, Number(voucher.totalCount || 0) - Number(voucher.usedCount || 0)),
    };
}

function isVoucherActive(voucher) {
    const now = Date.now();
    const start = new Date(voucher.startTime || 0).getTime();
    const end = new Date(voucher.endTime || 0).getTime();
    return Number(voucher.status) === 1 && (!start || start <= now) && (!end || end >= now);
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

function upsertChatConversation({ buyerUserId, sellerUserId, sourceType = "SECONDHAND", sourceId, sourceTitle }) {
    const normalizedType = String(sourceType || "DIRECT").toUpperCase();
    let conversation = mockStore.chatConversations.find((item) => {
        const members = item.participantIds || [];
        return members.some((id) => Number(id) === Number(buyerUserId))
            && members.some((id) => Number(id) === Number(sellerUserId))
            && item.sourceType === normalizedType
            && Number(item.sourceId || 0) === Number(sourceId || 0);
    });
    if (!conversation) {
        conversation = {
            id: mockStore.next.chatConversationId++,
            participantIds: [buyerUserId, sellerUserId],
            sourceType: normalizedType,
            sourceId,
            sourceTitle,
            lastMessageContent: "",
            lastMessageTime: new Date().toISOString(),
            unreadByUserId: 0,
        };
        mockStore.chatConversations.unshift(conversation);
    } else {
        conversation.sourceTitle = sourceTitle || conversation.sourceTitle;
    }
    return conversation;
}

function appendChatMessage({ conversation, senderUserId, receiverUserId, content }) {
    if (!conversation?.id || !content) {
        return null;
    }
    const sender = mockStore.users.find((item) => Number(item.id) === Number(senderUserId));
    const message = {
        id: mockStore.next.chatMessageId++,
        conversationId: conversation.id,
        senderUserId,
        sender: userPublic(sender),
        content,
        createTime: new Date().toISOString(),
    };
    mockStore.chatMessages.push(message);
    conversation.lastMessageContent = content;
    conversation.lastMessageTime = message.createTime;
    conversation.unreadByUserId = receiverUserId || 0;
    const index = mockStore.chatConversations.findIndex((item) => Number(item.id) === Number(conversation.id));
    if (index > 0) {
        mockStore.chatConversations.splice(index, 1);
        mockStore.chatConversations.unshift(conversation);
    }
    return message;
}

function appendNegotiationDecisionMessage(negotiation, content) {
    if (!negotiation || !content) {
        return;
    }
    const product = mockStore.secondhandProducts.find((item) => Number(item.id) === Number(negotiation.productId));
    const conversation = upsertChatConversation({
        buyerUserId: negotiation.buyerUserId,
        sellerUserId: negotiation.sellerUserId,
        sourceType: "SECONDHAND",
        sourceId: negotiation.productId,
        sourceTitle: product?.name || negotiation.productName || "二手商品",
    });
    appendChatMessage({
        conversation,
        senderUserId: negotiation.sellerUserId,
        receiverUserId: negotiation.buyerUserId,
        content,
    });
}

export async function handleMockRequest({ method, url, params, data, headers }) {
    const m = String(method || "get").toLowerCase();
    const p = String(url || "");

    if (m === "get" && p === "/category/tree") {
        return ok(cloneCategoryTree(params?.scene));
    }

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
            shopDesc: data?.shopDesc ?? user.shopDesc,
            bannerUrl: data?.bannerUrl ?? user.bannerUrl,
            businessHours: data?.businessHours ?? user.businessHours,
            returnPolicy: data?.returnPolicy ?? user.returnPolicy,
            shippingPolicy: data?.shippingPolicy ?? user.shippingPolicy,
            announcement: data?.announcement ?? user.announcement,
        });
        return ok({ ...user, password: undefined });
    }

    if (m === "get" && p === "/user/search") {
        const user = requireLogin(headers);
        const keyword = asText(params?.keyword).trim().toLowerCase();
        const records = mockStore.users
            .filter((item) => Number(item.id) !== Number(user.id))
            .filter((item) => String(item.role || "").toUpperCase() !== "ADMIN")
            .filter((item) => {
                if (!keyword) return true;
                return String(item.id).includes(keyword)
                    || String(item.username || "").toLowerCase().includes(keyword)
                    || String(item.nickname || "").toLowerCase().includes(keyword);
            })
            .slice(0, 10)
            .map((item) => ({ ...item, password: undefined }));
        return ok(records);
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

    if (m === "get" && p === "/user/browse-history") {
        const user = requireLogin(headers);
        const records = mockStore.browseHistory
            .filter((item) => Number(item.userId) === Number(user.id))
            .sort((a, b) => new Date(b.browseTime || 0).getTime() - new Date(a.browseTime || 0).getTime());
        return ok(records);
    }

    if (m === "post" && p === "/user/browse-history") {
        const user = requireLogin(headers);
        return ok(upsertBrowseHistory(user, data));
    }

    const browseHistoryMatch = p.match(/^\/user\/browse-history\/(\d+)$/);
    if (m === "delete" && browseHistoryMatch) {
        const user = requireLogin(headers);
        const id = Number(browseHistoryMatch[1]);
        const before = mockStore.browseHistory.length;
        mockStore.browseHistory = mockStore.browseHistory.filter((item) => !(Number(item.id) === id && Number(item.userId) === Number(user.id)));
        if (mockStore.browseHistory.length === before) {
            fail(404, "浏览记录不存在");
        }
        return ok(null);
    }

    if (m === "post" && p === "/user/browse-history/delete-batch") {
        const user = requireLogin(headers);
        const ids = Array.isArray(data) ? data.map(Number) : (data?.historyIds || data?.ids || []).map(Number);
        const drop = new Set(ids);
        mockStore.browseHistory = mockStore.browseHistory.filter((item) => Number(item.userId) !== Number(user.id) || !drop.has(Number(item.id)));
        return ok(null);
    }

    if (m === "delete" && p === "/user/browse-history/all") {
        const user = requireLogin(headers);
        mockStore.browseHistory = mockStore.browseHistory.filter((item) => Number(item.userId) !== Number(user.id));
        return ok(null);
    }

    if (m === "get" && p === "/product/list") {
        const keyword = asText(params?.keyword);
        const category = normalizeCategory(params?.category);
        const minPrice = params?.minPrice != null ? Number(params.minPrice) : null;
        const maxPrice = params?.maxPrice != null ? Number(params.maxPrice) : null;
        const baseRecords = mockStore.products
            .filter((x) => x.status === 1)
            .filter((x) => !category || category === ALL_CATEGORY || matchProductCategory(x, category))
            .filter((x) => minPrice == null || Number(x.price) >= minPrice)
            .filter((x) => maxPrice == null || Number(x.price) <= maxPrice)
            .sort((a, b) => Number(b.id) - Number(a.id));
        const records = filterByKeyword(baseRecords, keyword);
        return ok(paginate(records, params?.pageNum, params?.pageSize));
    }

    const productDetailMatch = p.match(/^\/product\/detail\/(\d+)$/);
    if (m === "get" && productDetailMatch) {
        const id = Number(productDetailMatch[1]);
        const record = mockStore.products.find((x) => x.id === id);
        if (!record) {
            fail(404, "商品不存在或已下架");
        }
        return ok(record);
    }

    const publicShopMatch = p.match(/^\/shop\/public\/(\d+)$/);
    if (m === "get" && publicShopMatch) {
        const shop = sellerShopVO(shopOwnerById(publicShopMatch[1]));
        if (!shop) {
            fail(404, "店铺不存在或已关闭");
        }
        return ok(shop);
    }

    const publicShopProductsMatch = p.match(/^\/shop\/public\/(\d+)\/products$/);
    if (m === "get" && publicShopProductsMatch) {
        const shopId = Number(publicShopProductsMatch[1]);
        const owner = shopOwnerById(shopId);
        if (!owner) {
            fail(404, "店铺不存在或已关闭");
        }
        const keyword = asText(params?.keyword);
        const sortBy = asText(params?.sortBy || "time_desc");
        const records = filterByKeyword(
            mockStore.products.filter((item) => Number(item.shopId) === shopId && Number(item.status) === 1),
            keyword,
        ).sort((a, b) => {
            if (sortBy === "price_asc") return Number(a.price || 0) - Number(b.price || 0);
            if (sortBy === "price_desc") return Number(b.price || 0) - Number(a.price || 0);
            if (sortBy === "sales_desc") return Number(b.sales || b.id || 0) - Number(a.sales || a.id || 0);
            return new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime();
        });
        return ok(paginateParams(records, params));
    }

    if (m === "get" && p === "/shop/seller/current") {
        const user = requireLogin(headers);
        requireSeller(user);
        return ok(sellerShopVO(user));
    }

    if (m === "put" && p === "/shop/seller/decoration") {
        const user = requireLogin(headers);
        requireSeller(user);
        const decorationJson = asText(data?.decorationJson);
        if (!decorationJson) {
            fail(400, "装修内容不能为空");
        }
        user.shopDecorationJson = decorationJson;
        return ok(sellerShopVO(user));
    }

    if (m === "get" && p === "/product/seller/list") {
        const user = requireLogin(headers);
        requireSeller(user);
        const keyword = asText(params?.keyword);
        const status = params?.status != null && params?.status !== "" ? Number(params.status) : null;
        const baseRecords = mockStore.products
            .filter((x) => x.shopId === user.id)
            .filter((x) => status == null || Number(x.status) === status)
            .sort((a, b) => Number(b.id) - Number(a.id));
        const records = filterByKeyword(baseRecords, keyword);
        return ok(paginateParams(records, params));
    }

    if (m === "post" && p === "/product/seller") {
        const user = requireLogin(headers);
        requireSeller(user);
        const id = mockStore.next.productId++;
        const status = Number(data?.status ?? 1);
        const categoryName = resolveProductCategory(data);
        const product = {
            id,
            shopId: user.id,
            categoryId: data?.categoryId == null ? productCategories.indexOf(categoryName) : Number(data.categoryId),
            categoryName,
            category: categoryName,
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
        const categoryName = resolveProductCategory({ ...product, ...data });
        Object.assign(product, {
            name: asText(data?.name),
            cover: asText(data?.cover),
            description: asText(data?.description),
            price: Number(data?.price || 0),
            stock: Number(data?.stock || 0),
            status: Number(data?.status ?? product.status),
            categoryId: data?.categoryId == null ? product.categoryId : Number(data.categoryId),
            categoryName,
            category: categoryName,
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
        const category = normalizeCategory(params?.category);
        const conditionLevel = asText(params?.conditionLevel || params?.condition);
        const baseRecords = mockStore.secondhandProducts
            .filter((x) => x.status === 1)
            .filter((x) => !category || category === ALL_CATEGORY || matchSecondhandCategory(x, category))
            .filter((x) => !conditionLevel || x.conditionLevel === conditionLevel || x.condition === conditionLevel)
            .sort((a, b) => Number(b.id) - Number(a.id));
        const records = filterByKeyword(baseRecords, keyword);
        return ok(paginateParams(records, params));
    }

    const secondhandDetailMatch = p.match(/^\/secondhand\/detail\/(\d+)$/);
    if (m === "get" && secondhandDetailMatch) {
        const id = Number(secondhandDetailMatch[1]);
        const item = mockStore.secondhandProducts.find((x) => Number(x.id) === id);
        if (!item) fail(404, "二手商品不存在");
        return ok(item);
    }

    if (m === "post" && p === "/secondhand/seller") {
        const user = requireLogin(headers);
        const id = mockStore.next.secondhandId++;
        const categoryName = resolveSecondhandCategory(data);
        const item = {
            id,
            sellerUserId: user.id,
            categoryId: data?.categoryId == null ? secondhandCategories.indexOf(categoryName) : Number(data.categoryId),
            categoryName,
            category: categoryName,
            name: asText(data?.name),
            cover: asText(data?.cover),
            description: asText(data?.description),
            originPrice: Number(data?.originPrice || 0),
            salePrice: Number(data?.salePrice || 0),
            conditionLevel: asText(data?.conditionLevel || data?.condition),
            isNegotiable: 1,
            status: 1,
            statusName: "在售",
            createTime: new Date().toISOString(),
        };
        mockStore.secondhandProducts.unshift(item);
        return ok(item);
    }

    if (m === "get" && p === "/secondhand/seller/list") {
        const user = requireLogin(headers);
        const keyword = asText(params?.keyword);
        const status = params?.status;
        let records = mockStore.secondhandProducts
            .filter((x) => Number(x.sellerUserId) === Number(user.id));
        if (status !== undefined && status !== null && status !== "") {
            const target = Number(status);
            records = records.filter((x) => target === 2 ? Number(x.status) !== 1 : Number(x.status) === target);
        }
        records = filterByKeyword(records, keyword)
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
        const categoryName = resolveSecondhandCategory({ ...item, ...data });
        Object.assign(item, {
            name: asText(data?.name || item.name),
            cover: asText(data?.cover || item.cover),
            description: asText(data?.description || item.description),
            originPrice: Number(data?.originPrice ?? item.originPrice ?? 0),
            salePrice: Number(data?.salePrice ?? item.salePrice ?? 0),
            conditionLevel: asText(data?.conditionLevel || data?.condition || item.conditionLevel),
            isNegotiable: 1,
            status: data?.status == null ? item.status : Number(data.status),
            categoryId: data?.categoryId == null ? item.categoryId : Number(data.categoryId),
            categoryName,
            category: categoryName,
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

    if (m === "post" && p === "/secondhand/trade/bargain/apply") {
        const user = requireLogin(headers);
        const productId = Number(data?.productId);
        const item = mockStore.secondhandProducts.find((x) => Number(x.id) === productId && Number(x.status) === 1);
        if (!item) fail(404, "二手商品不存在");
        if (Number(item.sellerUserId) === Number(user.id)) fail(400, "不能向自己的二手商品议价");
        const auction = findAuctionByProduct(productId);
        if (auction?.status === "ONGOING") fail(400, "该商品正在拍卖中，暂不能议价");
        const price = Number(data?.proposedPrice || 0);
        if (price <= 0) fail(400, "议价金额必须大于0");
        const existing = mockStore.productNegotiations.find((negotiation) =>
            Number(negotiation.productId) === productId &&
            Number(negotiation.buyerUserId) === Number(user.id) &&
            ["PENDING", "CONFIRMED"].includes(negotiation.status)
        );
        if (existing) {
            existing.proposedPrice = price;
            existing.status = "PENDING";
            existing.statusName = "待卖家确认";
            existing.updateTime = new Date().toISOString();
            const conversation = upsertChatConversation({
                buyerUserId: user.id,
                sellerUserId: item.sellerUserId,
                sourceType: "SECONDHAND",
                sourceId: productId,
                sourceTitle: item.name,
            });
            appendChatMessage({
                conversation,
                senderUserId: user.id,
                receiverUserId: item.sellerUserId,
                content: `你好，我对「${item.name || "这件闲置"}」出价 ¥${price.toFixed(2)}，可以考虑一下吗？`,
            });
            return ok(negotiationVO(existing));
        }
        const negotiation = {
            id: mockStore.next.negotiationId++,
            productId,
            sellerUserId: item.sellerUserId,
            buyerUserId: user.id,
            proposedPrice: price,
            confirmedPrice: null,
            status: "PENDING",
            statusName: "待卖家确认",
            createTime: new Date().toISOString(),
        };
        mockStore.productNegotiations.unshift(negotiation);
        const conversation = upsertChatConversation({
            buyerUserId: user.id,
            sellerUserId: item.sellerUserId,
            sourceType: "SECONDHAND",
            sourceId: productId,
            sourceTitle: item.name,
        });
        appendChatMessage({
            conversation,
            senderUserId: user.id,
            receiverUserId: item.sellerUserId,
            content: `你好，我对「${item.name || "这件闲置"}」出价 ¥${price.toFixed(2)}，可以考虑一下吗？`,
        });
        return ok(negotiationVO(negotiation));
    }

    if (m === "post" && p === "/secondhand/trade/bargain/confirm") {
        const user = requireLogin(headers);
        const negotiation = mockStore.productNegotiations.find((item) => Number(item.id) === Number(data?.negotiationId));
        if (!negotiation) fail(404, "议价记录不存在");
        if (Number(negotiation.sellerUserId) !== Number(user.id)) fail(403, "无权确认该议价");
        if (negotiation.status !== "PENDING") fail(400, "当前议价不可确认");
        const item = mockStore.secondhandProducts.find((x) => Number(x.id) === Number(negotiation.productId));
        if (!item || Number(item.status) !== 1) fail(400, "二手商品已下架或已售出");
        const buyer = mockStore.users.find((x) => Number(x.id) === Number(negotiation.buyerUserId));
        if (!buyer) fail(404, "买家不存在");
        const confirmedPrice = Number(data?.confirmedPrice || negotiation.proposedPrice || 0);
        const order = createSecondhandOrder(buyer, item, {
            price: confirmedPrice,
            source: "BARGAIN",
        });
        negotiation.confirmedPrice = confirmedPrice;
        negotiation.status = "ORDER_CREATED";
        negotiation.statusName = "卖家已同意，订单已创建";
        negotiation.orderId = order.id;
        negotiation.orderNo = order.orderNo;
        negotiation.updateTime = new Date().toISOString();
        appendNegotiationDecisionMessage(
            negotiation,
            `我已同意 ¥${confirmedPrice.toFixed(2)} 的议价，系统已生成二手订单，请到我的订单里查看。`,
        );
        return ok(negotiationVO(negotiation));
    }

    const bargainRejectMatch = p.match(/^\/secondhand\/trade\/bargain\/(\d+)\/reject$/);
    if (m === "post" && bargainRejectMatch) {
        const user = requireLogin(headers);
        const negotiation = mockStore.productNegotiations.find((item) => Number(item.id) === Number(bargainRejectMatch[1]));
        if (!negotiation) fail(404, "议价记录不存在");
        if (Number(negotiation.sellerUserId) !== Number(user.id)) fail(403, "无权拒绝该议价");
        negotiation.status = "REJECTED";
        negotiation.statusName = "卖家已拒绝";
        negotiation.updateTime = new Date().toISOString();
        appendNegotiationDecisionMessage(
            negotiation,
            `这次 ¥${Number(negotiation.proposedPrice || 0).toFixed(2)} 的议价我先不接受，后续有需要我们再沟通。`,
        );
        return ok(negotiationVO(negotiation));
    }

    if (m === "get" && p === "/secondhand/trade/bargain/list") {
        const user = requireLogin(headers);
        const productId = params?.productId == null || params.productId === "" ? null : Number(params.productId);
        const counterpartUserId = params?.counterpartUserId == null || params.counterpartUserId === "" ? null : Number(params.counterpartUserId);
        const status = asText(params?.status);
        let records = mockStore.productNegotiations.filter((item) =>
            Number(item.sellerUserId) === Number(user.id) || Number(item.buyerUserId) === Number(user.id)
        );
        if (productId) {
            records = records.filter((item) => Number(item.productId) === productId);
        }
        if (counterpartUserId) {
            records = records.filter((item) =>
                Number(item.sellerUserId) === counterpartUserId || Number(item.buyerUserId) === counterpartUserId
            );
        }
        if (status) {
            const allowed = status.split(",").map((item) => item.trim()).filter(Boolean);
            records = records.filter((item) => allowed.includes(item.status));
        }
        records = records
            .sort((a, b) => new Date(b.updateTime || b.createTime || 0).getTime() - new Date(a.updateTime || a.createTime || 0).getTime())
            .map(negotiationVO);
        return ok(paginateParams(records, params));
    }

    if (m === "get" && p === "/secondhand/trade/bargain/effective") {
        const user = requireLogin(headers);
        const productId = Number(params?.productId);
        const negotiation = mockStore.productNegotiations.find((item) =>
            Number(item.productId) === productId &&
            Number(item.buyerUserId) === Number(user.id) &&
            item.status === "CONFIRMED" &&
            !item.orderId
        );
        return ok(negotiationVO(negotiation));
    }

    if (m === "post" && p === "/secondhand/trade/auction") {
        const user = requireLogin(headers);
        const productId = Number(data?.productId);
        const item = mockStore.secondhandProducts.find((x) => Number(x.id) === productId && Number(x.status) === 1);
        if (!item) fail(404, "二手商品不存在");
        if (Number(item.sellerUserId) !== Number(user.id)) fail(403, "只能为自己发布的二手商品发起拍卖");
        const existing = findAuctionByProduct(productId);
        if (existing?.status === "ONGOING") fail(400, "该商品已有进行中的拍卖");
        const startPrice = Number(data?.startPrice || 0);
        const incrementAmount = Number(data?.incrementAmount || 1);
        const durationMinutes = Number(data?.durationMinutes || 60);
        if (startPrice <= 0 || incrementAmount <= 0 || durationMinutes < 10) fail(400, "拍卖参数不完整");
        const now = Date.now();
        const auction = {
            id: mockStore.next.auctionId++,
            productId,
            sellerUserId: user.id,
            startPrice,
            incrementAmount,
            currentPrice: startPrice,
            currentBidderUserId: null,
            bidCount: 0,
            status: "ONGOING",
            statusName: "进行中",
            startTime: new Date(now).toISOString(),
            endTime: new Date(now + durationMinutes * 60 * 1000).toISOString(),
            createTime: new Date(now).toISOString(),
        };
        mockStore.productAuctions.unshift(auction);
        return ok(auctionVO(auction));
    }

    const auctionByProductMatch = p.match(/^\/secondhand\/trade\/auction\/product\/(\d+)$/);
    if (m === "get" && auctionByProductMatch) {
        const auction = findAuctionByProduct(Number(auctionByProductMatch[1]));
        return ok(auctionVO(auction));
    }

    if (m === "get" && p === "/secondhand/trade/auction/seller/list") {
        const user = requireLogin(headers);
        const status = asText(params?.status);
        let records = mockStore.productAuctions
            .map(settleAuctionIfNeeded)
            .filter((item) => Number(item.sellerUserId) === Number(user.id));
        if (status) {
            records = records.filter((item) => item.status === status);
        }
        records = records.sort((a, b) => Number(b.id) - Number(a.id)).map(auctionVO);
        return ok(paginateParams(records, params));
    }

    const auctionCloseMatch = p.match(/^\/secondhand\/trade\/auction\/(\d+)\/close$/);
    if (m === "post" && auctionCloseMatch) {
        const user = requireLogin(headers);
        const auction = mockStore.productAuctions.find((item) => Number(item.id) === Number(auctionCloseMatch[1]));
        if (!auction) fail(404, "拍卖不存在");
        if (Number(auction.sellerUserId) !== Number(user.id)) fail(403, "无权操作该拍卖");
        if (auction.status !== "ONGOING") fail(400, "该拍卖已结束");
        auction.endTime = new Date().toISOString();
        settleAuctionIfNeeded(auction);
        return ok(auctionVO(auction));
    }

    const auctionFlowMatch = p.match(/^\/secondhand\/trade\/auction\/(\d+)\/flow$/);
    if (m === "post" && auctionFlowMatch) {
        const user = requireLogin(headers);
        const auction = mockStore.productAuctions.find((item) => Number(item.id) === Number(auctionFlowMatch[1]));
        if (!auction) fail(404, "拍卖不存在");
        if (Number(auction.sellerUserId) !== Number(user.id)) fail(403, "无权操作该拍卖");
        if (auction.status !== "ONGOING") fail(400, "该拍卖已结束");
        auction.status = "FLOW";
        auction.statusName = "已流拍";
        auction.endTime = new Date().toISOString();
        return ok(auctionVO(auction));
    }

    const auctionBidMatch = p.match(/^\/secondhand\/trade\/auction\/(\d+)\/bid$/);
    if (m === "post" && auctionBidMatch) {
        const user = requireLogin(headers);
        const auction = mockStore.productAuctions.find((item) => Number(item.id) === Number(auctionBidMatch[1]));
        if (!auction) fail(404, "拍卖不存在");
        settleAuctionIfNeeded(auction);
        if (auction.status !== "ONGOING") fail(400, "该拍卖已结束");
        if (Number(auction.sellerUserId) === Number(user.id)) fail(400, "卖家不能参与自己的拍卖");
        const bidAmount = Number(data?.bidAmount || 0);
        const current = Number(auction.currentPrice || auction.startPrice || 0);
        const minBid = Number(auction.currentBidderUserId ? current + Number(auction.incrementAmount || 1) : current);
        if (bidAmount < minBid) fail(400, `出价不能低于 ¥${minBid.toFixed(2)}`);
        auction.currentPrice = bidAmount;
        auction.currentBidderUserId = user.id;
        auction.bidCount = Number(auction.bidCount || 0) + 1;
        auction.updateTime = new Date().toISOString();
        return ok(auctionVO(auction));
    }

    const secondhandBuyMatch = p.match(/^\/secondhand\/(\d+)\/buy$/);
    if (m === "post" && secondhandBuyMatch) {
        const user = requireLogin(headers);
        const id = Number(secondhandBuyMatch[1]);
        const item = mockStore.secondhandProducts.find((x) => Number(x.id) === id && x.status === 1);
        if (!item) fail(404, "二手商品不存在");
        if (Number(item.sellerUserId) === Number(user.id)) fail(400, "不能购买自己发布的二手商品");
        const auction = findAuctionByProduct(id);
        if (auction?.status === "ONGOING") fail(400, "该商品正在拍卖中，请参与竞拍");
        const effective = mockStore.productNegotiations.find((negotiation) =>
            Number(negotiation.productId) === id &&
            Number(negotiation.buyerUserId) === Number(user.id) &&
            negotiation.status === "CONFIRMED" &&
            !negotiation.orderId
        );
        const order = createSecondhandOrder(user, item, {
            price: effective ? Number(effective.confirmedPrice || effective.proposedPrice || item.salePrice || 0) : Number(item.salePrice || 0),
            addressId: data?.addressId,
            source: effective ? "BARGAIN" : "DIRECT",
        });
        if (effective) {
            effective.status = "USED";
            effective.orderId = order.id;
            effective.usedTime = new Date().toISOString();
        }
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
                productType: "NEW",
                sellerUserId: product.sellerUserId || product.shopId,
                sellerName: product.sellerName || product.shopName,
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
            orderType: "NEW",
            totalAmount,
            payStatus: 0,
            payMethod: "",
            orderStatus: 0,
            orderStatusName: "待付款",
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
        const records = filterOrders(mockStore.orders.filter((o) => o.buyerUserId === user.id), params || {});
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
            if (Number(order.orderStatus) !== 0) fail(400, "当前订单不可支付");
            order.payStatus = 1;
            order.payMethod = data?.payMode === "COIN"
                ? "商城币支付"
                : (data?.payChannel === "ALIPAY" ? "支付宝支付" : "微信支付");
            order.paidTime = new Date().toISOString();
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
            if (!sellerOwnsOrder(order, user.id)) fail(403, "无权操作该订单");
            order.orderStatus = 2;
            order.orderStatusName = "待收货";
            order.shippedTime = new Date().toISOString();
            order.deliveryNo = order.deliveryNo || `KG${String(order.id).padStart(8, "0")}`;
            order.logisticsStatus = "IN_TRANSIT";
            addLogisticsTrace(order, "卖家已发货", "包裹已由卖家交付物流");
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
        if (!sellerOwnsOrder(order, user.id)) fail(403, "无权操作该订单");
        order.refundStatus = action === "approve" ? 2 : 3;
        order.refundStatusName = action === "approve" ? "已通过" : "已拒绝";
        order.orderStatus = 9;
        order.orderStatusName = "已关闭";
        order.refundDecisionSource = "SELLER";
        order.refundDecisionRemark = asText(data?.remark);
        order.refundDecisionTime = new Date().toISOString();
        return ok(order);
    }

    const orderReviewMatch = p.match(/^\/order\/(\d+)\/review(\/items)?$/);
    if (m === "post" && orderReviewMatch) {
        const user = requireLogin(headers);
        const id = Number(orderReviewMatch[1]);
        const order = mockStore.orders.find((o) => o.id === id && o.buyerUserId === user.id);
        if (!order) fail(404, "订单不存在");
        const reviewItems = Array.isArray(data?.items)
            ? data.items
            : (order.items || []).map((item) => ({
                productType: item.itemType || item.productType || "NEW",
                productId: item.productId,
                score: data?.score || 5,
                content: data?.content || "默认好评",
            }));
        reviewItems.forEach((item) => {
            const orderItem = (order.items || []).find((it) => Number(it.productId) === Number(item.productId));
            mockStore.reviews.unshift({
                id: mockStore.next.reviewId++,
                orderId: order.id,
                orderNo: order.orderNo,
                userId: user.id,
                sellerUserId: reviewSellerId(item),
                productId: Number(item.productId),
                productType: item.productType || orderItem?.itemType || "NEW",
                productName: orderItem?.productName || `商品 ${item.productId}`,
                score: Number(item.score || 5),
                rating: Number(item.score || 5),
                content: asText(item.content || "默认好评"),
                reviewType: "ORIGINAL",
                sellerReply: "",
                sellerReplyTime: "",
                createTime: new Date().toISOString(),
            });
        });
        order.orderStatus = 4;
        order.orderStatusName = "已完成";
        order.completedTime = new Date().toISOString();
        return ok(order);
    }

    if (m === "get" && p === "/review/my") {
        const user = requireLogin(headers);
        let records = mockStore.reviews.filter((item) => Number(item.userId) === Number(user.id));
        const keyword = asText(params?.keyword);
        const score = params?.score === undefined || params?.score === null || params?.score === "" ? null : Number(params.score);
        if (keyword) {
            records = records.filter((item) => String(item.content || "").includes(keyword) || String(item.productName || "").includes(keyword));
        }
        if (score != null) {
            records = records.filter((item) => Number(item.score || item.rating) === score);
        }
        records = records.sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());
        return ok(paginateParams(records, params));
    }

    if (m === "get" && p === "/review/seller/list") {
        const user = requireLogin(headers);
        let records = mockStore.reviews.filter((item) => Number(item.sellerUserId) === Number(user.id));
        const keyword = asText(params?.keyword);
        if (keyword) {
            records = records.filter((item) => String(item.content || "").includes(keyword) || String(item.productName || "").includes(keyword));
        }
        records = records.sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());
        return ok(paginateParams(records, params));
    }

    const reviewReplyMatch = p.match(/^\/review\/(\d+)\/reply$/);
    if (m === "post" && reviewReplyMatch) {
        const user = requireLogin(headers);
        const review = mockStore.reviews.find((item) => Number(item.id) === Number(reviewReplyMatch[1]));
        if (!review) fail(404, "评价不存在");
        if (Number(review.sellerUserId) !== Number(user.id)) fail(403, "无权回复该评价");
        review.sellerReply = asText(data?.reply || data?.content);
        review.sellerReplyTime = new Date().toISOString();
        return ok(review);
    }

    const sellerReviewReplyMatch = p.match(/^\/review\/seller\/(\d+)\/reply$/);
    if (m === "post" && sellerReviewReplyMatch) {
        const user = requireLogin(headers);
        const review = mockStore.reviews.find((item) => Number(item.id) === Number(sellerReviewReplyMatch[1]));
        if (!review) fail(404, "评价不存在");
        if (Number(review.sellerUserId) !== Number(user.id)) fail(403, "无权回复该评价");
        review.sellerReply = asText(data?.reply || data?.content);
        review.sellerReplyTime = new Date().toISOString();
        return ok(review);
    }

    if (m === "post" && p === "/review/followup") {
        const user = requireLogin(headers);
        const original = mockStore.reviews.find((item) =>
            Number(item.userId) === Number(user.id)
            && Number(item.orderId) === Number(data?.orderId)
            && Number(item.productId) === Number(data?.productId)
            && item.reviewType === "ORIGINAL"
        );
        if (!original) fail(404, "原评价不存在");
        const review = {
            id: mockStore.next.reviewId++,
            ...original,
            score: Number(data?.score || 5),
            rating: Number(data?.score || 5),
            content: asText(data?.content),
            reviewType: "FOLLOWUP",
            sellerReply: "",
            sellerReplyTime: "",
            createTime: new Date().toISOString(),
        };
        mockStore.reviews.unshift(review);
        return ok(review);
    }

    if (m === "get" && p === "/order/seller/list") {
        const user = requireLogin(headers);
        const records = filterOrders(mockStore.orders.filter((order) => sellerOwnsOrder(order, user.id)), params);
        return ok(paginateParams(records, params));
    }

    const sellerOrderDetailMatch = p.match(/^\/order\/seller\/detail\/(\d+)$/);
    if (m === "get" && sellerOrderDetailMatch) {
        const user = requireLogin(headers);
        const id = Number(sellerOrderDetailMatch[1]);
        const order = mockStore.orders.find((o) => o.id === id);
        if (!order) fail(404, "订单不存在");
        if (!sellerOwnsOrder(order, user.id)) fail(403, "无权查看该订单");
        return ok(order);
    }

    if (m === "get" && p === "/admin/orders/list") {
        const user = requireLogin(headers);
        requireAdmin(user);
        const records = filterOrders(mockStore.orders, params);
        return ok(paginateParams(records, params));
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
            targetUser.shopName = app.storeName || targetUser.nickname;
            targetUser.category = resolveProductCategory({ categoryId: app.categoryId, categoryName: app.categoryName });
            targetUser.shopContactName = app.contactName;
            targetUser.shopContactPhone = app.contactPhone;
            targetUser.region = [app.warehouseProvince, app.warehouseCity].filter(Boolean).join(" ");
            targetUser.warehouseAddr = [app.warehouseProvince, app.warehouseCity, app.warehouseDetail].filter(Boolean).join(" ");
            targetUser.idCardNoMasked = app.idCardNo ? `${String(app.idCardNo).slice(0, 4)}**********${String(app.idCardNo).slice(-4)}` : "";
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
        const shopLogs = mockStore.creditLogs.filter((item) => Number(item.userId) === Number(user.id) && item.role === "SELLER");
        const buyerScore = Math.max(0, Math.min(100, baseScore + buyerLogs.reduce((sum, item) => sum + Number(item.delta || 0), 0)));
        const shSellerScore = Math.max(0, Math.min(100, 96 + shSellerLogs.reduce((sum, item) => sum + Number(item.delta || 0), 0)));
        const shopScore = Math.max(0, Math.min(100, baseScore + shopLogs.reduce((sum, item) => sum + Number(item.delta || 0), 0)));
        return ok({
            buyerScore,
            buyerLevel: creditLevel(buyerScore),
            buyerLogs,
            shSellerScore,
            shSellerLevel: creditLevel(shSellerScore),
            shSellerLogs,
            shopScore,
            shopLevel: creditLevel(shopScore),
            shopLogs,
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

    if (m === "get" && p === "/admin/reports") {
        const user = requireLogin(headers);
        requireAdmin(user);
        const status = params?.status === undefined || params?.status === null || params?.status === "" ? null : Number(params.status);
        const reportedId = params?.reportedId === undefined || params?.reportedId === null || params?.reportedId === "" ? null : Number(params.reportedId);
        const records = mockStore.reports
            .filter((item) => status == null || Number(item.status) === status)
            .filter((item) => reportedId == null || Number(item.reportedId) === reportedId)
            .map((item) => {
                const reporter = mockStore.users.find((u) => Number(u.id) === Number(item.reporterId));
                return {
                    ...item,
                    reporterRole: /SELLER/.test(String(item.tradeContext || "")) || reporter?.role === "OFFICIAL_SELLER" ? "SELLER" : "BUYER",
                    reporterName: reporter?.nickname || reporter?.username || "",
                };
            })
            .sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());
        return ok(paginateParams(records, params));
    }

    if (m === "post" && p === "/admin/reports/audit") {
        const user = requireLogin(headers);
        requireAdmin(user);
        const report = mockStore.reports.find((item) => Number(item.id) === Number(data?.reportId));
        if (!report) fail(404, "举报不存在");
        if (Number(report.status) !== 0) fail(400, "该举报已处理");
        const decision = Number(data?.decision);
        report.status = decision === 1 ? 1 : 2;
        report.adminRemark = asText(data?.adminRemark);
        report.auditAdminId = user.id;
        report.auditTime = new Date().toISOString();
        if (decision === 1) {
            const delta = -Math.abs(Number(data?.customDelta || 5));
            const target = mockStore.users.find((item) => Number(item.id) === Number(report.reportedId));
            if (target) {
                target.creditScore = Math.max(0, Math.min(100, Number(target.creditScore || 100) + delta));
            }
            mockStore.creditLogs.unshift({
                id: mockStore.next.creditLogId++,
                userId: Number(report.reportedId),
                role: /SELLER/.test(String(report.tradeContext || "")) ? "SELLER" : "BUYER",
                delta,
                reasonDesc: `举报成立：${report.reasonType}`,
                createTime: new Date().toISOString(),
            });
        }
        return ok(report);
    }

    if (m === "post" && p === "/admin/reports/credit-adjust") {
        const user = requireLogin(headers);
        requireAdmin(user);
        const userId = Number(params?.userId || data?.userId);
        const target = mockStore.users.find((item) => Number(item.id) === userId);
        if (!target) fail(404, "用户不存在");
        const delta = Number(params?.delta ?? data?.delta ?? 0);
        if (!delta) fail(400, "调整值不能为0");
        target.creditScore = Math.max(0, Math.min(100, Number(target.creditScore || 100) + delta));
        mockStore.creditLogs.unshift({
            id: mockStore.next.creditLogId++,
            userId,
            role: asText(params?.role || data?.role || "BUYER"),
            delta,
            reasonDesc: asText(params?.remark || data?.remark || "管理员手动调整"),
            createTime: new Date().toISOString(),
        });
        mockStore.auditLogs.unshift({
            id: mockStore.next.auditLogId++,
            adminUsername: user.username,
            action: "CREDIT_ADJUST",
            targetType: "USER",
            targetId: userId,
            detail: `信用分调整 ${delta}`,
            createTime: new Date().toISOString(),
        });
        return ok(null);
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

    if (m === "get" && p === "/finance/dashboard") {
        const user = requireLogin(headers);
        const sellerOrders = mockStore.orders.filter((order) => sellerOwnsOrder(order, user.id));
        const completedIncome = sellerOrders
            .filter((order) => Number(order.payStatus) === 1 && Number(order.refundStatus || 0) !== 2)
            .reduce((sum, order) => sum + Number(order.totalAmount || 0), 0);
        return ok({
            businessBalance: Number(completedIncome.toFixed(2)),
            personalBalance: 288.88,
        });
    }

    if (m === "get" && p === "/finance/business/records") {
        const user = requireLogin(headers);
        const records = mockStore.orders
            .filter((order) => sellerOwnsOrder(order, user.id))
            .flatMap((order) => {
                const rows = [{
                    id: `income-${order.id}`,
                    orderId: order.id,
                    tradeType: "ORDER_INCOME",
                    tradeTypeName: "订单收入",
                    amount: Number(order.totalAmount || 0),
                    remark: `订单 ${order.orderNo} 收入`,
                    createTime: order.paidTime || order.createTime,
                }];
                if (Number(order.refundStatus) === 2) {
                    rows.push({
                        id: `refund-${order.id}`,
                        orderId: order.id,
                        tradeType: "ORDER_REFUND",
                        tradeTypeName: "订单退款",
                        amount: -Number(order.totalAmount || 0),
                        remark: `订单 ${order.orderNo} 退款`,
                        createTime: order.refundDecisionTime || new Date().toISOString(),
                    });
                }
                return rows;
            })
            .sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());
        return ok(records);
    }

    if (m === "get" && p === "/finance/my-wallet/records") {
        requireLogin(headers);
        return ok([
            { id: 1, tradeType: "RECHARGE", tradeTypeName: "钱包充值", amount: 100, remark: "商城币充值", createTime: new Date().toISOString() },
        ]);
    }

    if (m === "post" && p === "/finance/recharge") {
        requireLogin(headers);
        return ok({ amount: Number(data?.amount || 0), createTime: new Date().toISOString() });
    }

    if (m === "get" && p === "/voucher/list") {
        const user = requireLogin(headers);
        const mallType = asText(params.mallType || params.scope || "").toUpperCase();
        const records = mockStore.vouchers
            .filter((item) => isVoucherActive(item))
            .filter((item) => !mallType || String(item.mallType || "NEW").toUpperCase() === mallType)
            .sort((a, b) => Number(a.claimedUserIds?.includes(user.id) || 0) - Number(b.claimedUserIds?.includes(user.id) || 0))
            .map((item) => voucherPublicVO(item, user.id));
        return ok(paginateParams(records, params));
    }

    if (m === "get" && p === "/voucher/my") {
        const user = requireLogin(headers);
        const mallType = asText(params.mallType || params.scope || "").toUpperCase();
        const records = mockStore.vouchers
            .filter((item) => (item.claimedUserIds || []).some((id) => Number(id) === Number(user.id)))
            .filter((item) => !mallType || String(item.mallType || "NEW").toUpperCase() === mallType)
            .sort((a, b) => new Date(b.claimedAtByUser?.[user.id] || b.startTime || 0).getTime() - new Date(a.claimedAtByUser?.[user.id] || a.startTime || 0).getTime())
            .map((item) => voucherPublicVO(item, user.id));
        return ok(paginateParams(records, params));
    }

    const claimVoucherMatch = p.match(/^\/voucher\/(\d+)\/claim$/);
    if (m === "post" && claimVoucherMatch) {
        const user = requireLogin(headers);
        const voucher = mockStore.vouchers.find((item) => Number(item.id) === Number(claimVoucherMatch[1]));
        if (!voucher || !isVoucherActive(voucher)) fail(404, "优惠券不可领取");
        if (Number(voucher.usedCount || 0) >= Number(voucher.totalCount || 0)) fail(400, "优惠券已领完");
        voucher.claimedUserIds = voucher.claimedUserIds || [];
        voucher.claimedAtByUser = voucher.claimedAtByUser || {};
        if (voucher.claimedUserIds.some((id) => Number(id) === Number(user.id))) {
            return ok(voucherPublicVO(voucher, user.id));
        }
        voucher.claimedUserIds.push(user.id);
        voucher.claimedAtByUser[user.id] = new Date().toISOString();
        voucher.usedCount = Number(voucher.usedCount || 0) + 1;
        return ok(voucherPublicVO(voucher, user.id));
    }

    if (m === "get" && p === "/voucher/seller/list") {
        const user = requireLogin(headers);
        requireSeller(user);
        const records = mockStore.vouchers
            .filter((item) => Number(item.sellerUserId) === Number(user.id))
            .sort((a, b) => Number(b.id) - Number(a.id));
        return ok(paginateParams(records, params));
    }

    if (m === "post" && p === "/voucher/seller") {
        const user = requireLogin(headers);
        requireSeller(user);
        const status = 1;
        const voucher = {
            id: mockStore.next.voucherId++,
            sellerUserId: user.id,
            mallType: "NEW",
            mallTypeName: "新品商城",
            name: asText(data?.name),
            type: Number(data?.type || 1),
            typeName: Number(data?.type || 1) === 1 ? "满减" : "折扣",
            minAmount: Number(data?.minAmount || 0),
            discountAmount: data?.discountAmount == null ? null : Number(data.discountAmount),
            discountRate: data?.discountRate == null ? null : Number(data.discountRate),
            totalCount: Number(data?.totalCount || 1),
            usedCount: 0,
            startTime: data?.startTime || new Date().toISOString(),
            endTime: data?.endTime || new Date(Date.now() + 7 * 86400000).toISOString(),
            status,
            statusName: voucherStatusName(status),
        };
        mockStore.vouchers.unshift(voucher);
        return ok(voucher);
    }

    const voucherMatch = p.match(/^\/voucher\/seller\/(\d+)$/);
    if (m === "put" && voucherMatch) {
        const user = requireLogin(headers);
        requireSeller(user);
        const voucher = mockStore.vouchers.find((item) => Number(item.id) === Number(voucherMatch[1]) && Number(item.sellerUserId) === Number(user.id));
        if (!voucher) fail(404, "优惠券不存在");
        Object.assign(voucher, {
            name: asText(data?.name || voucher.name),
            type: Number(data?.type ?? voucher.type),
            minAmount: Number(data?.minAmount ?? voucher.minAmount),
            discountAmount: data?.discountAmount == null ? null : Number(data.discountAmount),
            discountRate: data?.discountRate == null ? null : Number(data.discountRate),
            totalCount: Number(data?.totalCount ?? voucher.totalCount),
            startTime: data?.startTime || voucher.startTime,
            endTime: data?.endTime || voucher.endTime,
        });
        voucher.typeName = Number(voucher.type) === 1 ? "满减" : "折扣";
        return ok(voucher);
    }

    if (m === "delete" && voucherMatch) {
        const user = requireLogin(headers);
        requireSeller(user);
        const before = mockStore.vouchers.length;
        mockStore.vouchers = mockStore.vouchers.filter((item) => !(Number(item.id) === Number(voucherMatch[1]) && Number(item.sellerUserId) === Number(user.id)));
        if (mockStore.vouchers.length === before) fail(404, "优惠券不存在");
        return ok(null);
    }

    const voucherCloseMatch = p.match(/^\/voucher\/seller\/(\d+)\/close$/);
    if (m === "post" && voucherCloseMatch) {
        const user = requireLogin(headers);
        requireSeller(user);
        const voucher = mockStore.vouchers.find((item) => Number(item.id) === Number(voucherCloseMatch[1]) && Number(item.sellerUserId) === Number(user.id));
        if (!voucher) fail(404, "优惠券不存在");
        voucher.status = 0;
        voucher.statusName = voucherStatusName(0);
        return ok(voucher);
    }

    if (m === "post" && p === "/logistics/push-next") {
        const user = requireLogin(headers);
        const orderId = Number(data?.orderId);
        const order = mockStore.orders.find((item) => Number(item.id) === orderId);
        if (!order) fail(404, "订单不存在");
        if (!sellerOwnsOrder(order, user.id) && user.role !== "ADMIN") fail(403, "无权操作该订单物流");
        const count = mockStore.logisticsTraces.filter((trace) => Number(trace.orderId) === orderId).length;
        const nodes = [
            ["揽收完成", "包裹已进入校区物流站"],
            ["运输中", "包裹正在前往收货地址"],
            ["派送中", "配送员正在派送，请保持电话畅通"],
            ["已签收", "包裹已送达并签收"],
        ];
        const [nodeName, statusDesc] = nodes[Math.min(count, nodes.length - 1)];
        const trace = addLogisticsTrace(order, nodeName, statusDesc);
        order.logisticsStatus = nodeName === "已签收" ? "ARRIVED" : "IN_TRANSIT";
        if (nodeName === "已签收") {
            order.orderStatus = Math.max(Number(order.orderStatus || 0), 3);
            order.orderStatusName = "待评价";
            order.receivedTime = new Date().toISOString();
            order.autoConfirmDeadline = new Date(Date.now() + 3 * 86400000).toISOString();
        }
        return ok(trace);
    }

    const logisticsTraceMatch = p.match(/^\/logistics\/order\/(\d+)\/trace$/);
    if (m === "get" && logisticsTraceMatch) {
        const user = requireLogin(headers);
        const orderId = Number(logisticsTraceMatch[1]);
        const order = mockStore.orders.find((item) => Number(item.id) === orderId);
        if (!order) fail(404, "订单不存在");
        if (Number(order.buyerUserId) !== Number(user.id) && !sellerOwnsOrder(order, user.id) && user.role !== "ADMIN") {
            fail(403, "无权查看物流");
        }
        ensureInitialLogistics(order);
        const records = mockStore.logisticsTraces
            .filter((trace) => Number(trace.orderId) === orderId)
            .sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime());
        return ok(records);
    }

    if (m === "post" && p === "/upload/image") {
        const ts = Date.now();
        return ok({ url: `/uploads/mock-${ts}.png`, filename: `mock-${ts}.png` });
    }

    if (m === "post" && p === "/upload/media") {
        const ts = Date.now();
        return ok({ url: `/uploads/mock-${ts}.png`, filename: `mock-${ts}.png`, contentType: "image/png" });
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
