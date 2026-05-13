function randInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pick(list) {
    return list[randInt(0, list.length - 1)];
}

const productNames = [
    "机械键盘", "无线鼠标", "27寸显示器", "运动耳机", "学习平板", "扩展坞", "电竞椅", "路由器"
];

const secondhandNames = [
    "二手山地车", "二手显示器", "二手键盘", "二手耳机", "二手平板", "二手书桌", "二手台灯"
];

const conditions = ["95%", "90%", "80%"];

export function generateUsers() {
    const fixed = [
        { id: 1, username: "admin", password: "admin123", nickname: "平台管理员", role: "ADMIN", status: "NORMAL", phone: "13800000000", email: "admin@demo.com", creditScore: 100 },
        { id: 2, username: "seller", password: "seller123", nickname: "官方卖家", role: "OFFICIAL_SELLER", status: "NORMAL", phone: "13800000001", email: "seller@demo.com", creditScore: 100 },
        { id: 3, username: "user", password: "user123", nickname: "普通用户", role: "USER", status: "NORMAL", phone: "13800000002", email: "user@demo.com", creditScore: 98 }
    ];
    const generated = Array.from({ length: 18 }).map((_, idx) => {
        const id = idx + 4;
        return {
            id,
            username: `user_${id}`,
            password: "123456",
            nickname: `用户${id}`,
            role: "USER",
            status: idx % 7 === 0 ? "BANNED" : "NORMAL",
            phone: `1390000${String(id).padStart(4, "0")}`,
            email: `user_${id}@demo.com`,
            creditScore: randInt(70, 100)
        };
    });
    return fixed.concat(generated);
}

export function generateProducts() {
    return Array.from({ length: 60 }).map((_, idx) => {
        const id = idx + 1;
        const status = idx % 9 === 0 ? 0 : 1;
        const shopId = idx % 2 === 0 ? 2 : 20;
        return {
            id,
            shopId,
            name: `${pick(productNames)} ${id}`,
            cover: `https://picsum.photos/seed/product-${id}/720/540`,
            images: [
                `https://picsum.photos/seed/product-${id}/720/540`,
                `https://picsum.photos/seed/product-${id}-b/720/540`,
                `https://picsum.photos/seed/product-${id}-c/720/540`,
            ],
            description: "自动生成测试商品数据",
            price: randInt(39, 2999),
            stock: randInt(0, 120),
            status,
            statusName: status === 1 ? "在售" : "已下架",
            createTime: new Date(Date.now() - id * 3600 * 1000).toISOString()
        };
    });
}

export function generateSecondhandProducts() {
    return Array.from({ length: 48 }).map((_, idx) => {
        const id = idx + 1;
        const originPrice = randInt(120, 2600);
        const salePrice = Math.max(20, Math.floor(originPrice * (0.35 + Math.random() * 0.45)));
        return {
            id,
            sellerUserId: idx % 2 === 0 ? 3 : 2,
            name: `${pick(secondhandNames)} ${id}`,
            cover: `https://picsum.photos/seed/second-${id}/720/540`,
            images: [
                `https://picsum.photos/seed/second-${id}/720/540`,
                `https://picsum.photos/seed/second-${id}-b/720/540`,
                `https://picsum.photos/seed/second-${id}-c/720/540`,
            ],
            description: "自动生成二手商品测试数据",
            originPrice,
            salePrice,
            conditionLevel: pick(conditions),
            status: 1,
            statusName: "在售",
            createTime: new Date(Date.now() - id * 1800 * 1000).toISOString()
        };
    });
}

export function generateAddresses() {
    return [
        { id: 1, userId: 3, receiverName: "张三", receiverPhone: "13800000002", province: "北京市", city: "北京市", detailAddress: "海淀区中关村软件园", isDefault: 1 }
    ];
}

export function generateMerchantApplications() {
    return [
        { id: 1, userId: 3, username: "user", storeName: "好物小店", categoryId: 2, contactName: "张三", contactPhone: "13800000002", status: 0, rejectReason: "", applyTime: new Date().toISOString(), licenseImg: "", warehouseProvince: "北京市", warehouseCity: "北京市", warehouseDetail: "软件园" }
    ];
}

export function generateAuditLogs() {
    return Array.from({ length: 30 }).map((_, idx) => ({
        id: idx + 1,
        adminUsername: "admin",
        action: idx % 2 === 0 ? "BAN_USER" : "UNBAN_USER",
        targetType: "USER",
        targetId: randInt(3, 20),
        detail: "自动生成审计日志",
        createTime: new Date(Date.now() - idx * 2400 * 1000).toISOString()
    }));
}

export function generateOrders(products) {
    const itemA = products[0];
    const itemB = products[1];
    return [
        {
            id: 1,
            orderNo: "ORDMOCK0001",
            buyerUserId: 3,
            totalAmount: Number(itemA.price) + Number(itemB.price),
            payStatus: 1,
            orderStatus: 1,
            createTime: new Date().toISOString(),
            items: [
                { productId: itemA.id, productName: itemA.name, price: itemA.price, quantity: 1 },
                { productId: itemB.id, productName: itemB.name, price: itemB.price, quantity: 1 }
            ]
        }
    ];
}
