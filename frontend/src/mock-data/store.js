import {
    generateAddresses,
    generateAuditLogs,
    generateBlocks,
    generateChatData,
    generateCreditLogs,
    generateMerchantApplications,
    generateNotifications,
    generateOrders,
    generateProducts,
    generateReports,
    generateSecondhandProducts,
    generateUsers,
} from "./generators";

function nextIdOf(list) {
    if (!list.length) {
        return 1;
    }
    return Math.max(...list.map((x) => Number(x.id || 0))) + 1;
}

const users = generateUsers();
const products = generateProducts();
const secondhandProducts = generateSecondhandProducts();
const addresses = generateAddresses();
const merchantApplications = generateMerchantApplications();
const auditLogs = generateAuditLogs();
const orders = generateOrders(products);
const notifications = generateNotifications();
const creditLogs = generateCreditLogs();
const reports = generateReports();
const blocks = generateBlocks();
const chat = generateChatData(users, products, secondhandProducts);

export const mockStore = {
    users,
    products,
    secondhandProducts,
    addresses,
    merchantApplications,
    auditLogs,
    orders,
    notifications,
    creditLogs,
    reports,
    blocks,
    chatConversations: chat.conversations,
    chatMessages: chat.messages,
    next: {
        userId: nextIdOf(users),
        productId: nextIdOf(products),
        secondhandId: nextIdOf(secondhandProducts),
        addressId: nextIdOf(addresses),
        merchantApplicationId: nextIdOf(merchantApplications),
        auditLogId: nextIdOf(auditLogs),
        orderId: nextIdOf(orders),
        notificationId: nextIdOf(notifications),
        creditLogId: nextIdOf(creditLogs),
        reportId: nextIdOf(reports),
        blockId: nextIdOf(blocks),
        chatConversationId: nextIdOf(chat.conversations),
        chatMessageId: nextIdOf(chat.messages),
    },
};
