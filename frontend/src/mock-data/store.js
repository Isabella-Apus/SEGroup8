import {
    generateAddresses,
    generateAuditLogs,
    generateMerchantApplications,
    generateOrders,
    generateProducts,
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

export const mockStore = {
    users,
    products,
    secondhandProducts,
    addresses,
    merchantApplications,
    auditLogs,
    orders,
    next: {
        userId: nextIdOf(users),
        productId: nextIdOf(products),
        secondhandId: nextIdOf(secondhandProducts),
        addressId: nextIdOf(addresses),
        merchantApplicationId: nextIdOf(merchantApplications),
        auditLogId: nextIdOf(auditLogs),
        orderId: nextIdOf(orders),
    },
};
