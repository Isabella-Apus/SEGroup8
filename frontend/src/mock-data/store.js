import {
    generateAddresses,
    generateAuditLogs,
    generateBlocks,
    generateBrowseHistory,
    generateChatData,
    generateCreditLogs,
    generateMerchantApplications,
    generateNotifications,
    generateOrders,
    generateProductAuctions,
    generateProductNegotiations,
    generateProducts,
    generateReviews,
    generateReports,
    generateSecondhandProducts,
    generateUsers,
    generateVouchers,
} from "./generators";

function nextIdOf(list) {
    if (!list.length) {
        return 1;
    }
    return Math.max(...list.map((x) => Number(x.id || 0))) + 1;
}

const STORAGE_KEY = "segroup8_mock_store_v2";

const LEGACY_COPY_REPLACEMENTS = {
    "Kinda 官方好物店": "Kinda 校园数码店",
    "主营校园学习、数码和生活好物。": "销售学习设备、数码配件和宿舍生活用品。",
    "数码装备限时上新": "本店商品与服务说明",
    "精选键盘、鼠标和学习效率装备，今天下单尽快发货。": "查看主营商品、发货安排和售后规则。",
    "新店上新中，欢迎收藏店铺并浏览更多商品。": "下单前可先查看商品详情、库存和店铺说明。",
    "精选好物": "主营清晰",
    "围绕学习与宿舍场景精选": "展示店铺主要品类",
    "及时处理": "响应及时",
    "订单与消息会尽快响应": "订单与消息按时处理",
    "稳定售后": "规则明确",
    "购物问题按平台规则处理": "售后说明按平台规则执行",
};

function normalizeCopy(value) {
    if (typeof value !== "string") {
        return value;
    }
    const replaced = LEGACY_COPY_REPLACEMENTS[value] || value;
    return replaced.replace("（演示售后数据）", "");
}

function normalizeCopyDeep(value) {
    if (typeof value === "string") {
        return normalizeCopy(value);
    }
    if (Array.isArray(value)) {
        return value.map((item) => normalizeCopyDeep(item));
    }
    if (value && typeof value === "object") {
        return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, normalizeCopyDeep(item)]));
    }
    return value;
}

function normalizeDecorationJson(value) {
    if (!value) {
        return value;
    }
    try {
        const parsed = typeof value === "string" ? JSON.parse(value) : value;
        const normalized = normalizeCopyDeep(parsed);
        return typeof value === "string" ? JSON.stringify(normalized) : normalized;
    } catch {
        return normalizeCopy(value);
    }
}

function normalizeStoreCopy(store) {
    const users = Array.isArray(store.users) ? store.users : [];
    users.forEach((user) => {
        user.nickname = normalizeCopy(user.nickname);
        user.shopName = normalizeCopy(user.shopName);
        user.shopDesc = normalizeCopy(user.shopDesc);
        user.shopDecorationJson = normalizeDecorationJson(user.shopDecorationJson);
    });

    const products = Array.isArray(store.products) ? store.products : [];
    products.forEach((product) => {
        product.sellerName = normalizeCopy(product.sellerName);
        product.shopName = normalizeCopy(product.shopName);
        product.description = normalizeCopy(product.description);
    });

    const vouchers = Array.isArray(store.vouchers) ? store.vouchers : [];
    vouchers.forEach((voucher) => {
        voucher.sellerName = normalizeCopy(voucher.sellerName);
    });

    const orders = Array.isArray(store.orders) ? store.orders : [];
    orders.forEach((order) => {
        order.refundReason = normalizeCopy(order.refundReason);
        (order.items || []).forEach((item) => {
            item.productName = normalizeCopy(item.productName);
            item.sellerName = normalizeCopy(item.sellerName);
        });
    });

    return store;
}

function createInitialStore() {
    const users = generateUsers();
    const products = generateProducts();
    const secondhandProducts = generateSecondhandProducts();
    const addresses = generateAddresses();
    const browseHistory = generateBrowseHistory(products, secondhandProducts);
    const merchantApplications = generateMerchantApplications();
    const auditLogs = generateAuditLogs();
    const orders = generateOrders(products, secondhandProducts);
    const notifications = generateNotifications();
    const creditLogs = generateCreditLogs();
    const reports = generateReports();
    const blocks = generateBlocks();
    const reviews = generateReviews(orders, products, secondhandProducts);
    const vouchers = generateVouchers();
    const productNegotiations = generateProductNegotiations();
    const productAuctions = generateProductAuctions(secondhandProducts);
    const chat = generateChatData(users, products, secondhandProducts);

    return {
        users,
        products,
        secondhandProducts,
        addresses,
        browseHistory,
        merchantApplications,
        auditLogs,
        orders,
        notifications,
        creditLogs,
        reports,
        blocks,
        reviews,
        vouchers,
        productNegotiations,
        productAuctions,
        logisticsTraces: [],
        chatConversations: chat.conversations,
        chatMessages: chat.messages,
        next: {
            userId: nextIdOf(users),
            productId: nextIdOf(products),
            secondhandId: nextIdOf(secondhandProducts),
            addressId: nextIdOf(addresses),
            browseHistoryId: nextIdOf(browseHistory),
            merchantApplicationId: nextIdOf(merchantApplications),
            auditLogId: nextIdOf(auditLogs),
            orderId: nextIdOf(orders),
            notificationId: nextIdOf(notifications),
            creditLogId: nextIdOf(creditLogs),
            reportId: nextIdOf(reports),
            blockId: nextIdOf(blocks),
            reviewId: nextIdOf(reviews),
            voucherId: nextIdOf(vouchers),
            negotiationId: nextIdOf(productNegotiations),
            auctionId: nextIdOf(productAuctions),
            logisticsTraceId: 1,
            chatConversationId: nextIdOf(chat.conversations),
            chatMessageId: nextIdOf(chat.messages),
        },
    };
}

function storage() {
    if (typeof window === "undefined" || !window.localStorage) {
        return null;
    }
    return window.localStorage;
}

function readStoredSnapshot() {
    try {
        const raw = storage()?.getItem(STORAGE_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
}

function mergeStore(base, saved) {
    if (!saved || typeof saved !== "object") {
        return normalizeStoreCopy(base);
    }
    return normalizeStoreCopy({
        ...base,
        ...saved,
        next: {
            ...base.next,
            ...(saved.next || {}),
        },
    });
}

function replaceStore(target, next) {
    Object.keys(target).forEach((key) => {
        delete target[key];
    });
    Object.assign(target, next);
}

export const mockStore = mergeStore(createInitialStore(), readStoredSnapshot());

export function syncMockStoreFromStorage() {
    const saved = readStoredSnapshot();
    if (!saved) {
        return;
    }
    replaceStore(mockStore, mergeStore(createInitialStore(), saved));
}

export function persistMockStore() {
    try {
        storage()?.setItem(STORAGE_KEY, JSON.stringify(mockStore));
    } catch {
        // Mock persistence is best-effort; the app can still run with in-memory data.
    }
}
