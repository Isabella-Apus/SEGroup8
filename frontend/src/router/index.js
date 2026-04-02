import { createRouter, createWebHistory } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useAuthStore } from '@/stores/auth';

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/user/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/UserLayout.vue'),
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/user/HomeView.vue')
      },
      {
        path: 'product',
        name: 'productList',
        component: () => import('@/views/product/ProductListView.vue')
      },
      {
        path: 'product/:id',
        name: 'productDetail',
        component: () => import('@/views/product/ProductDetailView.vue')
      },
      {
        path: 'cart',
        name: 'cart',
        component: () => import('@/views/order/CartView.vue')
      },
      {
        path: 'order',
        name: 'order',
        component: () => import('@/views/order/OrderView.vue')
      },
      {
        path: 'secondhand',
        name: 'secondhandList',
        component: () => import('@/views/secondhand/SecondhandListView.vue')
      },
      {
        path: 'secondhand/publish',
        name: 'secondhandPublish',
        component: () => import('@/views/secondhand/SecondhandPublishView.vue')
      }
    ]
  },
  {
    path: '/seller',
    component: () => import('@/layout/SellerLayout.vue'),
    meta: { roles: ['SELLER', 'ADMIN'] },
    children: [
      {
        path: '',
        name: 'sellerCenter',
        component: () => import('@/views/seller/SellerCenterView.vue')
      }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    meta: { roles: ['ADMIN'] },
    children: [
      {
        path: '',
        name: 'adminHome',
        component: () => import('@/views/admin/AdminHomeView.vue')
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  if (to.meta.public) {
    next();
    return;
  }
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录');
    next('/login');
    return;
  }
  const roles = to.meta.roles || [];
  if (roles.length && !roles.includes(authStore.role)) {
    ElMessage.error('无访问权限');
    next('/');
    return;
  }
  next();
});

export default router;
