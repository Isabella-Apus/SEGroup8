function randInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pick(list) {
    return list[randInt(0, list.length - 1)];
}

const productCatalog = [
    { name: "电子数码", items: ["机械键盘", "无线鼠标", "27寸显示器", "学习平板", "扩展坞", "路由器"] },
    { name: "服装鞋包", items: ["牛津衬衫", "通勤托特包", "复古帆布鞋", "轻薄外套", "棒球帽", "针织开衫"] },
    { name: "学习办公", items: ["错题整理本", "桌面文件架", "护眼台灯", "便携笔袋", "课程资料夹", "人体工学椅"] },
    { name: "生活百货", items: ["保温杯", "桌面收纳盒", "香薰套装", "迷你风扇", "床头小夜灯", "洗衣凝珠"] },
    { name: "运动户外", items: ["运动耳机", "瑜伽垫", "运动水壶", "速干毛巾", "羽毛球拍", "健身弹力带"] },
];

const secondhandCatalog = [
    { name: "数码闲置", items: ["二手显示器", "二手键盘", "二手耳机", "二手平板", "闲置相机", "备用充电宝"] },
    { name: "服饰鞋包", items: ["闲置双肩包", "九成新卫衣", "闲置运动鞋", "通勤斜挎包", "羊毛围巾", "牛仔外套"] },
    { name: "教材书籍", items: ["二手教材", "考研资料", "英语词汇书", "课程笔记", "专业参考书", "小说套装"] },
    { name: "宿舍生活", items: ["二手书桌", "二手台灯", "折叠收纳箱", "床上小桌板", "宿舍置物架", "迷你电饭煲"] },
    { name: "运动器材", items: ["二手山地车", "闲置滑板", "哑铃套装", "羽毛球拍", "篮球", "露营折叠椅"] },
];

const productCategories = productCatalog.map((item) => item.name);
const secondhandCategories = secondhandCatalog.map((item) => item.name);
const conditions = ["95%", "90%", "80%"];

export function generateUsers() {
    const fixed = [
        { id: 1, username: "admin", password: "admin123", nickname: "平台管理员", role: "ADMIN", status: "NORMAL", phone: "13800000000", email: "admin@demo.com", creditScore: 100 },
        {
            id: 2,
            username: "seller",
            password: "seller123",
            nickname: "官方卖家",
            role: "OFFICIAL_SELLER",
            status: "NORMAL",
            phone: "13800000001",
            email: "seller@demo.com",
            creditScore: 100,
            shopName: "Kinda 官方好物店",
            shopDesc: "主营校园学习、数码和生活好物。",
            category: "电子数码",
            shopContactName: "李四",
            shopContactPhone: "13800000001",
            region: "北京市 北京市",
            warehouseAddr: "北京市 北京市 海淀区中关村软件园",
            businessHours: "周一至周日 9:00-21:00",
        },
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
        const shopId = 2;
        const category = productCatalog[idx % productCatalog.length];
        const categoryName = category.name;
        const itemName = category.items[Math.floor(idx / productCatalog.length) % category.items.length];
        return {
            id,
            shopId,
            sellerUserId: 2,
            sellerName: "Kinda 官方好物店",
            shopName: "Kinda 官方好物店",
            categoryId: (idx % productCatalog.length) + 1,
            categoryName,
            name: `${itemName} ${id}`,
            cover: `https://picsum.photos/seed/product-${id}/720/540`,
            images: [
                `https://picsum.photos/seed/product-${id}/720/540`,
                `https://picsum.photos/seed/product-${id}-b/720/540`,
                `https://picsum.photos/seed/product-${id}-c/720/540`,
            ],
            description: `${categoryName}精选好物，适合日常学习、通勤和宿舍生活。`,
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
        const sellerUserId = [4, 5, 6, 2][idx % 4];
        const originPrice = randInt(120, 2600);
        const salePrice = Math.max(20, Math.floor(originPrice * (0.35 + Math.random() * 0.45)));
        const category = secondhandCatalog[idx % secondhandCatalog.length];
        const categoryName = category.name;
        const itemName = category.items[Math.floor(idx / secondhandCatalog.length) % category.items.length];
        return {
            id,
            sellerUserId,
            sellerName: sellerUserId === 2 ? "官方卖家" : `用户${sellerUserId}`,
            categoryId: (idx % secondhandCatalog.length) + 1,
            categoryName,
            name: `${itemName} ${id}`,
            cover: `https://picsum.photos/seed/second-${id}/720/540`,
            images: [
                `https://picsum.photos/seed/second-${id}/720/540`,
                `https://picsum.photos/seed/second-${id}-b/720/540`,
                `https://picsum.photos/seed/second-${id}-c/720/540`,
            ],
            description: `${itemName}闲置转让，成色已标注，可先沟通再下单。`,
            originPrice,
            salePrice,
            conditionLevel: pick(conditions),
            isNegotiable: 1,
            status: 1,
            statusName: "在售",
            createTime: new Date(Date.now() - id * 1800 * 1000).toISOString()
        };
    });
}

export function generateProductNegotiations() {
    return [];
}

export function generateProductAuctions(secondhandProducts = []) {
    const first = secondhandProducts.find((item) => Number(item.status) === 1);
    if (!first) {
        return [];
    }
    const now = Date.now();
    const startPrice = Math.max(10, Math.floor(Number(first.salePrice || 0) * 0.82));
    return [
        {
            id: 1,
            productId: first.id,
            sellerUserId: first.sellerUserId,
            startPrice,
            incrementAmount: 5,
            currentPrice: startPrice,
            currentBidderUserId: null,
            bidCount: 0,
            status: "ONGOING",
            statusName: "进行中",
            startTime: new Date(now - 20 * 60 * 1000).toISOString(),
            endTime: new Date(now + 6 * 60 * 60 * 1000).toISOString(),
            createTime: new Date(now - 20 * 60 * 1000).toISOString(),
        },
    ];
}

export function generateAddresses() {
    return [
        { id: 1, userId: 3, receiverName: "张三", receiverPhone: "13800000002", province: "北京市", city: "北京市", detailAddress: "海淀区中关村软件园", isDefault: 1 }
    ];
}

export function generateBrowseHistory(products, secondhandProducts) {
    const now = Date.now();
    const newGoods = products
        .filter((item) => Number(item.status) === 1)
        .slice(0, 8)
        .map((item, idx) => ({
            id: idx + 1,
            userId: 3,
            recordType: "product",
            productType: "NEW",
            product: {
                id: item.id,
                name: item.name,
                cover: item.cover,
                price: item.price,
            },
            browseTime: new Date(now - idx * 35 * 60 * 1000).toISOString(),
        }));
    const secondhandGoods = secondhandProducts
        .filter((item) => Number(item.status) === 1)
        .slice(0, 6)
        .map((item, idx) => ({
            id: newGoods.length + idx + 1,
            userId: 3,
            recordType: "product",
            productType: "SECONDHAND",
            product: {
                id: item.id,
                name: item.name,
                cover: item.cover,
                price: item.salePrice,
            },
            browseTime: new Date(now - (idx + 3) * 55 * 60 * 1000).toISOString(),
        }));
    return newGoods.concat(secondhandGoods);
}

export function generateCreditLogs() {
    const now = Date.now();
    return [
        { id: 1, userId: 3, role: "BUYER", delta: 2, reasonDesc: "按时确认收货", createTime: new Date(now - 2 * 86400000).toISOString() },
        { id: 2, userId: 3, role: "BUYER", delta: -1, reasonDesc: "取消订单提醒", createTime: new Date(now - 5 * 86400000).toISOString() },
        { id: 3, userId: 3, role: "SH_SELLER", delta: 3, reasonDesc: "二手交易评价良好", createTime: new Date(now - 3 * 86400000).toISOString() },
        { id: 4, userId: 2, role: "SH_SELLER", delta: 2, reasonDesc: "卖家及时发货", createTime: new Date(now - 4 * 86400000).toISOString() },
    ];
}

export function generateReports() {
    return [
        {
            id: 1,
            reporterId: 3,
            reportedId: 2,
            tradeContext: "SH_BUYER",
            reasonType: "FAKE_ITEM",
            reasonDesc: "商品描述与实际有差异，等待平台审核。",
            evidenceUrls: "",
            status: 0,
            createTime: new Date(Date.now() - 6 * 3600 * 1000).toISOString(),
        },
    ];
}

export function generateBlocks() {
    return [];
}

export function generateChatData(users, products, secondhandProducts) {
    const now = Date.now();
    const product = products[1];
    const secondhand = secondhandProducts[1];
    const conversations = [
        {
            id: 1,
            participantIds: [3, 2],
            sourceType: "PRODUCT",
            sourceId: product.id,
            sourceTitle: product.name,
            lastMessageContent: "这件商品今天还能发货吗？",
            lastMessageTime: new Date(now - 35 * 60 * 1000).toISOString(),
            unreadByUserId: 0,
        },
        {
            id: 2,
            participantIds: [3, 2],
            sourceType: "SECONDHAND",
            sourceId: secondhand.id,
            sourceTitle: secondhand.name,
            lastMessageContent: "可以，支持当面验货。",
            lastMessageTime: new Date(now - 80 * 60 * 1000).toISOString(),
            unreadByUserId: 3,
        },
    ];
    const findUser = (id) => users.find((user) => Number(user.id) === Number(id));
    const messages = [
        {
            id: 1,
            conversationId: 1,
            senderUserId: 3,
            sender: findUser(3),
            content: "你好，这个键盘还有现货吗？",
            createTime: new Date(now - 48 * 60 * 1000).toISOString(),
        },
        {
            id: 2,
            conversationId: 1,
            senderUserId: 2,
            sender: findUser(2),
            content: "有的，今天下单可以尽快安排发货。",
            createTime: new Date(now - 42 * 60 * 1000).toISOString(),
        },
        {
            id: 3,
            conversationId: 1,
            senderUserId: 3,
            sender: findUser(3),
            content: "这件商品今天还能发货吗？",
            createTime: new Date(now - 35 * 60 * 1000).toISOString(),
        },
        {
            id: 4,
            conversationId: 2,
            senderUserId: 3,
            sender: findUser(3),
            content: "这个二手商品可以线下看一下吗？",
            createTime: new Date(now - 95 * 60 * 1000).toISOString(),
        },
        {
            id: 5,
            conversationId: 2,
            senderUserId: 2,
            sender: findUser(2),
            content: "可以，支持当面验货。",
            createTime: new Date(now - 80 * 60 * 1000).toISOString(),
        },
    ];
    return { conversations, messages };
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

export function generateOrders(products, secondhandProducts = []) {
    const itemA = products[0];
    const itemB = products[1];
    const secondhand = secondhandProducts[0];
    const orders = [
        {
            id: 1,
            orderNo: "ORDMOCK0001",
            buyerUserId: 3,
            orderType: "NEW",
            totalAmount: Number(itemA.price) + Number(itemB.price),
            payStatus: 1,
            orderStatus: 1,
            createTime: new Date().toISOString(),
            orderStatusName: "待发货",
            refundStatus: 1,
            refundStatusName: "待处理",
            refundReason: "不想要了/拍错了（演示售后数据）",
            refundProofUrls: "",
            refundApplyTime: new Date(Date.now() - 18 * 60 * 1000).toISOString(),
            items: [
                { id: 1, productId: itemA.id, productName: itemA.name, itemType: "NEW", productType: "NEW", sellerUserId: itemA.sellerUserId || itemA.shopId, sellerName: itemA.sellerName || itemA.shopName, price: itemA.price, quantity: 1 },
                { id: 2, productId: itemB.id, productName: itemB.name, itemType: "NEW", productType: "NEW", sellerUserId: itemB.sellerUserId || itemB.shopId, sellerName: itemB.sellerName || itemB.shopName, price: itemB.price, quantity: 1 }
            ]
        }
    ];
    if (secondhand) {
        orders.push({
            id: 2,
            orderNo: "ORDMOCK0002",
            buyerUserId: 3,
            orderType: "SECONDHAND",
            totalAmount: Number(secondhand.salePrice || 0),
            payStatus: 1,
            orderStatus: 2,
            createTime: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
            orderStatusName: "待收货",
            refundStatus: 0,
            refundStatusName: "",
            deliveryNo: "KGSH202605220001",
            items: [
                {
                    id: "2-1",
                    productId: secondhand.id,
                    productName: secondhand.name,
                    itemType: "SECONDHAND",
                    productType: "SECONDHAND",
                    sellerUserId: secondhand.sellerUserId,
                    sellerName: secondhand.sellerName,
                    conditionLevel: secondhand.conditionLevel,
                    price: Number(secondhand.salePrice || 0),
                    quantity: 1,
                },
            ],
        });
    }
    return orders;
}

export function generateReviews(orders, products, secondhandProducts) {
    let nextId = 1;
    const sellerOf = (item) => {
        if (item.itemType === "SECONDHAND") {
            return secondhandProducts.find((x) => Number(x.id) === Number(item.productId))?.sellerUserId || 0;
        }
        return products.find((x) => Number(x.id) === Number(item.productId))?.shopId || 0;
    };
    return orders.flatMap((order) => (order.items || []).map((item, idx) => ({
        id: nextId++,
        orderId: order.id,
        orderNo: order.orderNo,
        userId: order.buyerUserId,
        sellerUserId: sellerOf(item),
        productId: item.productId,
        productType: item.itemType || item.productType || "NEW",
        productName: item.productName,
        score: idx === 0 ? 5 : 4,
        rating: idx === 0 ? 5 : 4,
        content: idx === 0 ? "发货很快，商品和描述一致。" : "整体不错，包装也比较完整。",
        reviewType: "ORIGINAL",
        sellerReply: idx === 0 ? "感谢支持，我们会继续保持。" : "",
        sellerReplyTime: idx === 0 ? new Date(Date.now() - 8 * 60 * 60 * 1000).toISOString() : "",
        createTime: new Date(Date.now() - (idx + 1) * 12 * 60 * 60 * 1000).toISOString(),
    })));
}

export function generateVouchers() {
    const now = Date.now();
    return [
        {
            id: 1,
            sellerUserId: 2,
            mallType: "NEW",
            mallTypeName: "新品商城",
            name: "新人满减券",
            type: 1,
            typeName: "满减",
            minAmount: 99,
            discountAmount: 12,
            discountRate: null,
            totalCount: 100,
            usedCount: 18,
            status: 1,
            statusName: "进行中",
            startTime: new Date(now - 2 * 86400000).toISOString(),
            endTime: new Date(now + 14 * 86400000).toISOString(),
        },
        {
            id: 2,
            sellerUserId: 2,
            mallType: "NEW",
            mallTypeName: "新品商城",
            name: "开学季折扣券",
            type: 2,
            typeName: "折扣",
            minAmount: 199,
            discountAmount: null,
            discountRate: 0.88,
            totalCount: 80,
            usedCount: 9,
            status: 1,
            statusName: "进行中",
            startTime: new Date(now - 86400000).toISOString(),
            endTime: new Date(now + 10 * 86400000).toISOString(),
        },
        {
            id: 3,
            sellerUserId: 4,
            mallType: "SECONDHAND",
            mallTypeName: "二手商城",
            name: "闲置沟通券",
            type: 1,
            typeName: "满减",
            minAmount: 60,
            discountAmount: 8,
            discountRate: null,
            totalCount: 60,
            usedCount: 7,
            status: 1,
            statusName: "进行中",
            startTime: new Date(now - 3 * 86400000).toISOString(),
            endTime: new Date(now + 9 * 86400000).toISOString(),
        },
        {
            id: 4,
            sellerUserId: 5,
            mallType: "SECONDHAND",
            mallTypeName: "二手商城",
            name: "教材闲置券",
            type: 2,
            typeName: "折扣",
            minAmount: 80,
            discountAmount: null,
            discountRate: 0.92,
            totalCount: 50,
            usedCount: 5,
            status: 1,
            statusName: "进行中",
            startTime: new Date(now - 86400000).toISOString(),
            endTime: new Date(now + 12 * 86400000).toISOString(),
        },
    ];
}

export function generateNotifications() {
    const now = Date.now();
    return [
        {
            id: 1,
            userId: 3,
            title: "订单已发货",
            content: "您的订单 ORDMOCK0001 已由卖家发货，请留意物流进度。",
            scope: "buyer",
            isRead: 0,
            createTime: new Date(now - 18 * 60 * 1000).toISOString()
        },
        {
            id: 2,
            userId: 3,
            title: "优惠券到账",
            content: "一张新人专享优惠券已放入您的账户，可在领券中心查看。",
            scope: "buyer",
            isRead: 1,
            createTime: new Date(now - 4 * 60 * 60 * 1000).toISOString()
        },
        {
            id: 3,
            userId: 2,
            title: "买家提醒发货",
            content: "买家已提醒您尽快处理订单 ORDMOCK0001，请进入卖家工作台发货。",
            scope: "seller",
            isRead: 0,
            createTime: new Date(now - 35 * 60 * 1000).toISOString()
        },
        {
            id: 4,
            userId: 2,
            title: "店铺数据提醒",
            content: "今日店铺有新的访问记录，建议检查商品库存和订单状态。",
            scope: "seller",
            isRead: 1,
            createTime: new Date(now - 8 * 60 * 60 * 1000).toISOString()
        }
    ];
}
