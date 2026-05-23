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
        return base;
    }
    return {
        ...base,
        ...saved,
        next: {
            ...base.next,
            ...(saved.next || {}),
        },
    };
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
