<template>
  <div class="admin-shell">
    <header class="admin-topbar">
      <div class="admin-topbar-inner">
        <button class="admin-brand" type="button" @click="router.push('/admin')">
          <span class="brand-mark">kg</span>
          <span>
            <strong>管理后台</strong>
            <em>kinda goods 控制台</em>
          </span>
        </button>

        <nav class="admin-actions" aria-label="管理员快捷入口">
          <button type="button" @click="router.push('/')">返回商城</button>
          <button type="button" @click="router.push('/merchant')">卖家工作台</button>
          <button class="danger" type="button" @click="logout">退出</button>
        </nav>
      </div>

      <nav class="admin-nav" aria-label="管理员导航">
        <button
          v-for="item in navItems"
          :key="item.path"
          class="admin-nav-item"
          :class="{ active: isActive(item.path) }"
          type="button"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>
    </header>

    <main class="admin-main fade-in-up">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const navItems = [
  { label: '后台首页', path: '/admin' },
  { label: '用户管理', path: '/admin/users' },
  { label: '入驻审核', path: '/admin/merchant-review' },
  { label: '审计日志', path: '/admin/audit-logs' },
  { label: '订单管理', path: '/admin/orders' },
  { label: '优惠券管理', path: '/admin/vouchers' },
  { label: '举报审核', path: '/admin/reports' },
];

function isActive(path) {
  if (path === '/admin') return route.path === '/admin';
  return route.path.startsWith(path);
}

function logout() {
  userStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  background: #f2f4f6;
}

.admin-topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  background: #ffe100;
  box-shadow: 0 2px 12px rgba(31, 35, 43, 0.08);
}

.admin-topbar-inner {
  width: min(1440px, calc(100% - 44px));
  height: 76px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.admin-brand,
.admin-actions button,
.admin-nav-item {
  font: inherit;
}

.admin-brand {
  min-width: 0;
  border: 0;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: transparent;
  color: #20242d;
  cursor: pointer;
  text-align: left;
}

.brand-mark {
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border: 3px solid #20242d;
  border-radius: 14px;
  font-size: 18px;
  letter-spacing: 0;
  text-transform: lowercase;
  line-height: 1;
  font-weight: 900;
}

.admin-brand strong,
.admin-brand em {
  display: block;
}

.admin-brand strong {
  font-size: 24px;
  line-height: 1.1;
  font-weight: 900;
}

.admin-brand em {
  margin-top: 4px;
  color: #4f5662;
  font-size: 13px;
  font-style: normal;
  font-weight: 700;
}

.admin-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-actions button {
  height: 36px;
  border: 0;
  border-radius: 999px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.62);
  color: #20242d;
  cursor: pointer;
  font-weight: 800;
}

.admin-actions button:hover {
  background: #fff;
}

.admin-actions button.danger {
  background: #20242d;
  color: #ffe100;
}

.admin-nav {
  width: min(1440px, calc(100% - 44px));
  margin: 0 auto;
  padding: 0 0 12px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.admin-nav-item {
  height: 34px;
  border: 0;
  border-radius: 999px;
  padding: 0 14px;
  background: transparent;
  color: #2b2f38;
  cursor: pointer;
  font-weight: 700;
  white-space: nowrap;
}

.admin-nav-item:hover,
.admin-nav-item.active {
  background: #fff;
}

.admin-main {
  width: min(1440px, calc(100% - 44px));
  margin: 18px auto 32px;
  padding: 0;
  min-width: 0;
}

.admin-main :deep(.page-card),
.admin-main :deep(.el-card) {
  border: 1px solid #eeeeee;
  border-radius: 20px;
  box-shadow: 0 8px 20px rgba(30, 34, 40, 0.04);
}

.admin-main :deep(.page-title) {
  color: #20242d;
  font-size: 24px;
  font-weight: 900;
  letter-spacing: 0;
}

.admin-main :deep(.toolbar) {
  border-radius: 16px;
}

.admin-main :deep(.el-button--primary) {
  --el-button-bg-color: #20242d;
  --el-button-border-color: #20242d;
  --el-button-hover-bg-color: #333842;
  --el-button-hover-border-color: #333842;
}

.admin-main :deep(.el-button--warning) {
  --el-button-bg-color: #ffe100;
  --el-button-border-color: #ffe100;
  --el-button-text-color: #20242d;
  --el-button-hover-bg-color: #ffe83f;
  --el-button-hover-border-color: #ffe83f;
  --el-button-hover-text-color: #20242d;
}

.admin-main :deep(.el-table) {
  border-radius: 14px;
  overflow: hidden;
}

.admin-main :deep(.el-table th.el-table__cell) {
  background: #fafafa;
  color: #333842;
  font-weight: 800;
}

.admin-main :deep(.el-input__wrapper),
.admin-main :deep(.el-select__wrapper) {
  border-radius: 12px;
}

@media (max-width: 760px) {
  .admin-topbar-inner {
    width: calc(100% - 20px);
    height: auto;
    padding: 10px 0;
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-brand strong {
    font-size: 21px;
  }

  .admin-actions {
    width: 100%;
    overflow-x: auto;
  }

  .admin-nav,
  .admin-main {
    width: calc(100% - 16px);
  }

  .admin-main {
    margin-top: 10px;
  }
}
</style>
