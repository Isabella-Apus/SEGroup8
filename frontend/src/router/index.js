import { createRouter, createWebHistory } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";

const routes = [
    {
        path: "/login",
        name: "login",
        component: () => import("@/views/user/Login.vue"),
        meta: { public: true },
    },
    {
        path: "/register",
        name: "register",
        component: () => import("@/views/user/Register.vue"),
        meta: { public: true },
    },
    {
        path: "/",
        component: () => import("@/layout/UserLayout.vue"),
        children: [
            {
                path: "",
                name: "home",
                component: () => import("@/views/user/HomeView.vue"),
            },
            {
                path: "product",
                name: "productList",
                component: () => import("@/views/product/ProductListView.vue"),
            },
            {
                path: "product/:id",
                name: "productDetail",
                component: () =>
                    import("@/views/product/ProductDetailView.vue"),
            },
            {
                path: "cart",
                name: "cart",
                component: () => import("@/views/order/CartView.vue"),
            },
            {
                path: "order",
                name: "order",
                component: () => import("@/views/order/OrderView.vue"),
            },
            {
                path: "order/:id",
                name: "orderDetail",
                component: () => import("@/views/order/OrderDetailView.vue"),
                meta: { detailMode: "buyer" },
            },
            {
                path: "secondhand",
                name: "secondhandList",
                component: () =>
                    import("@/views/secondhand/SecondhandListView.vue"),
            },
            {
                path: "secondhand/:id",
                name: "secondhandDetail",
                component: () =>
                    import("@/views/secondhand/SecondhandDetailView.vue"),
            },
            {
                path: "secondhand/publish",
                name: "secondhandPublish",
                component: () =>
                    import("@/views/secondhand/SecondhandPublishView.vue"),
            },
            {
                path: "profile",
                name: "profile",
                component: () => import("@/views/user/Profile.vue"),
            },
            {
                path: "addresses",
                name: "addressManager",
                component: () => import("@/views/user/AddressManager.vue"),
            },
            {
                path: "my-reviews",
                name: "myReviews",
                component: () => import("@/views/user/MyReviewsView.vue"),
            },
            {
                path: "browse-history",
                name: "browseHistory",
                component: () => import("@/views/user/BrowseHistoryView.vue"),
            },
            {
                path: "after-sale",
                name: "afterSale",
                component: () => import("@/views/order/AfterSaleView.vue"),
            },
            {
                path: "messages",
                name: "messages",
                component: () => import("@/views/chat/ChatView.vue"),
            },
            {
                path: "notifications",
                name: "notifications",
                component: () => import("@/views/notification/NotificationView.vue"),
            },
            {
                path: "merchant-apply",
                name: "merchantApply",
                component: () => import("@/views/user/MerchantApplyView.vue"),
            },
        ],
    },
    {
        path: "/merchant",
        component: () => import("@/layout/MerchantLayout.vue"),
        meta: { roles: ["OFFICIAL_SELLER"] },
        children: [
            {
                path: "",
                name: "merchantWorkbench",
                component: () =>
                    import("@/views/seller/SellerProductList.vue"),
            },
            {
                path: "orders",
                name: "merchantOrders",
                component: () =>
                    import("@/views/merchant/MerchantOrdersView.vue"),
            },
            {
                path: "orders/:id",
                name: "merchantOrderDetail",
                component: () => import("@/views/order/OrderDetailView.vue"),
                meta: { detailMode: "seller" },
            },
            {
                path: "finance",
                name: "merchantFinance",
                component: () =>
                    import("@/views/merchant/MerchantFinanceView.vue"),
            },
            {
                path: "reviews",
                name: "merchantReviews",
                component: () =>
                    import("@/views/merchant/MerchantReviewsView.vue"),
            },
            {
                path: "messages",
                name: "merchantMessages",
                component: () => import("@/views/chat/ChatView.vue"),
            },
            {
                path: "notifications",
                name: "merchantNotifications",
                component: () => import("@/views/notification/NotificationView.vue"),
            },
            {
                path: "shop",
                name: "merchantShopSetting",
                component: () =>
                    import("@/views/merchant/MerchantShopSettingView.vue"),
            },
            {
                path: "seller-products",
                name: "sellerProducts",
                component: () => import("@/views/seller/SellerProductList.vue"),
            },
            {
                path: "seller-products/edit/:id?",
                name: "sellerProductEdit",
                component: () => import("@/views/seller/SellerProductEdit.vue"),
            },
            {
                path: "seller-dashboard",
                name: "sellerDashboard",
                component: () => import("@/views/seller/SellerDashboard.vue"),
            },
            {
                path: "seller-shop",
                name: "sellerShop",
                component: () => import("@/views/seller/SellerShopSetting.vue"),
            },
            {
                path: "vouchers",
                name: "sellerVouchers",
                component: () => import("@/views/seller/SellerVoucher.vue"),
            },
            {
                path: "account-health",
                name: "sellerAccountHealth",
                component: () => import("@/views/seller/SellerAccountHealth.vue"),
            },
            {
                path: "shop-decoration",
                name: "sellerShopDecoration",
                component: () => import("@/views/seller/SellerShopDecoration.vue"),
            },
            
        ],
    },
    {
        path: "/admin",
        component: () => import("@/layout/AdminLayout.vue"),
        meta: { roles: ["ADMIN"] },
        children: [
            {
                path: "",
                name: "adminHome",
                component: () => import("@/views/admin/AdminHomeView.vue"),
            },
            {
                path: "users",
                name: "adminUserList",
                component: () => import("@/views/admin/AdminUserList.vue"),
            },
            {
                path: "merchant-review",
                name: "adminMerchantReview",
                component: () =>
                    import("@/views/admin/AdminMerchantReviewView.vue"),
            },
            {
                path: "audit-logs",
                name: "adminAuditLogs",
                component: () => import("@/views/admin/AdminAuditLogView.vue"),
            },
            {
                path: "orders",
                name: "adminOrders",
                component: () =>
                    import("@/views/admin/AdminOrderManageView.vue"),
            },
        ],
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

const FIRST_VISIT_GUARD_KEY = "segroup8_force_login_checked";

router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore();
    // Force the app to start from login once per browser session.
    if (!sessionStorage.getItem(FIRST_VISIT_GUARD_KEY)) {
        sessionStorage.setItem(FIRST_VISIT_GUARD_KEY, "1");
        if (to.path !== "/login") {
            userStore.logout();
            next("/login");
            return;
        }
    }
    if (to.meta.public) {
        next();
        return;
    }
    if (!userStore.isLoggedIn) {
        ElMessage.warning("请先登录");
        next("/login");
        return;
    }
    if (!userStore.userInfo) {
        try {
            await userStore.fetchProfile();
        } catch (error) {
            next("/login");
            return;
        }
    }
    const roles = to.meta.roles || [];
    if (roles.length && !roles.includes(userStore.currentRole)) {
        ElMessage.error("无访问权限");
        next("/");
        return;
    }
    next();
});

export default router;
