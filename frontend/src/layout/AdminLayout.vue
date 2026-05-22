<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <button class="brand-block" type="button" @click="router.push('/admin')">
        <img :src="logoUrl" alt="Kinda Goods" />
        <span>管理后台</span>
      </button>

      <nav class="side-nav">
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          class="side-nav-item"
          :class="{ active: isActive(item.path) }"
          @click="router.push(item.path)"
        >
          <span class="nav-mark" :style="{ background: item.color }"></span>
          {{ item.label }}
        </button>
      </nav>
    </aside>

    <section class="admin-shell">
      <header class="admin-topbar">
        <div>
          <span class="top-kicker">Kinda Goods Admin</span>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="top-actions">
          <button type="button" @click="router.push('/')">返回商城</button>
          <button type="button" class="danger" @click="logout">退出登录</button>
        </div>
      </header>

      <main class="admin-main fade-in-up">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import logoUrl from "@/assets/kinda-goods-logo.svg";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const navItems = [
  { label: "总览", path: "/admin", color: "#35d8ab" },
  { label: "用户管理", path: "/admin/users", color: "#69b9ff" },
  { label: "入驻审核", path: "/admin/merchant-review", color: "#ffc6dc" },
  { label: "订单管理", path: "/admin/orders", color: "#b7a6ff" },
  { label: "举报审核", path: "/admin/reports", color: "#ffd36e" },
  { label: "审计日志", path: "/admin/audit-logs", color: "#7ee8cb" },
];

const currentTitle = computed(() => navItems.find((item) => isActive(item.path))?.label || "管理后台");

function isActive(path) {
  if (path === "/admin") {
    return route.path === "/admin";
  }
  return route.path === path || route.path.startsWith(`${path}/`);
}

function logout() {
  userStore.logout();
  router.push("/login");
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  background:
    radial-gradient(circle at 14% 8%, rgba(126, 232, 203, 0.42), transparent 28%),
    radial-gradient(circle at 92% 0%, rgba(155, 216, 255, 0.46), transparent 30%),
    linear-gradient(135deg, #f8fffc 0%, #f3fbff 48%, #fff7fb 100%);
}

.admin-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  border-right: 1px solid rgba(137, 199, 255, 0.28);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(16px);
  padding: 18px 14px;
}

.brand-block {
  width: 100%;
  border: 0;
  background: transparent;
  display: grid;
  gap: 8px;
  justify-items: start;
  cursor: pointer;
  color: var(--text-main);
  font-weight: 900;
}

.brand-block img {
  width: 168px;
  height: 58px;
  object-fit: contain;
  object-position: left center;
}

.brand-block span {
  border-radius: 999px;
  background: #e9fff8;
  color: #159d7d;
  padding: 5px 11px;
  font-size: 12px;
}

.side-nav {
  margin-top: 18px;
  display: grid;
  gap: 8px;
}

.side-nav-item {
  width: 100%;
  min-height: 42px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 900;
  cursor: pointer;
  text-align: left;
}

.side-nav-item:hover,
.side-nav-item.active {
  border-color: rgba(137, 199, 255, 0.42);
  background: rgba(255, 255, 255, 0.88);
  color: var(--brand-primary);
  box-shadow: var(--shadow-soft);
}

.nav-mark {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: 0 0 auto;
}

.admin-shell {
  min-width: 0;
}

.admin-topbar {
  position: sticky;
  top: 0;
  z-index: 12;
  min-height: 82px;
  border-bottom: 1px solid rgba(137, 199, 255, 0.24);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
  padding: 14px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.top-kicker {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 900;
}

.admin-topbar h1 {
  margin: 4px 0 0;
  font-size: 24px;
  line-height: 1.1;
}

.top-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.top-actions button {
  height: 36px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  color: var(--text-main);
  padding: 0 13px;
  font-weight: 900;
  cursor: pointer;
}

.top-actions button:hover {
  border-color: #9bd8ff;
  background: #eef8ff;
  color: var(--brand-primary);
}

.top-actions .danger {
  color: #f0647d;
}

.admin-main {
  padding: 18px 22px 36px;
}

.admin-main :deep(.page-card),
.admin-main :deep(.el-card) {
  border: 1px solid rgba(137, 199, 255, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-soft);
}

.admin-main :deep(.toolbar),
.admin-main :deep(.query-form) {
  border: 1px solid rgba(137, 199, 255, 0.24);
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(233, 255, 248, 0.72), rgba(234, 244, 255, 0.68));
}

.admin-main :deep(.el-table th.el-table__cell) {
  background: #f4fbff;
}

@media (max-width: 860px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    position: static;
    height: auto;
  }

  .side-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
