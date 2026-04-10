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
                component: () => import("@/views/ProductFeedView.vue"),
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
                path: "reviews",
                name: "myReviews",
                component: () => import("@/views/user/MyReviewView.vue"),
            },
            {
                path: "secondhand",
                name: "secondhandList",
                component: () =>
                    import("@/views/secondhand/SecondhandListView.vue"),
            },
            {
                path: "secondhand/publish",
                name: "secondhandPublish",
                component: () =>
                    import("@/views/secondhand/SecondhandPublishView.vue"),
            },
            {
                path: "secondhand/:id",
                name: "secondhandDetail",
                component: () =>
                    import("@/views/secondhand/SecondhandDetailView.vue"),
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
                    import("@/views/merchant/MerchantWorkbenchView.vue"),
            },
            {
                path: "orders",
                name: "merchantOrders",
                component: () =>
                    import("@/views/merchant/MerchantOrdersView.vue"),
            },
            {
                path: "reviews",
                name: "merchantReviews",
                component: () =>
                    import("@/views/merchant/MerchantReviewView.vue"),
            },
            {
                path: "shop",
                name: "merchantShopSetting",
                component: () =>
                    import("@/views/merchant/MerchantShopSettingView.vue"),
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
                path: "orders",
                name: "adminOrderList",
                component: () => import("@/views/admin/AdminOrderView.vue"),
            },
            {
                path: "audit-logs",
                name: "adminAuditLogs",
                component: () => import("@/views/admin/AdminAuditLogView.vue"),
            },
        ],
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes,
});

router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore();
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
