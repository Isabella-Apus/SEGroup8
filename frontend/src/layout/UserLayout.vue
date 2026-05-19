<template>
  <div class="user-layout">
    <header class="market-header">
      <div class="header-inner">
        <button class="brand" type="button" @click="router.push('/')">
          <span class="brand-mark">kg</span>
          <span class="brand-copy">
            <strong>Kinda Goods</strong>
            <small>一手好物和二手闲置</small>
          </span>
        </button>

        <form class="search-bar" @submit.prevent="submitSearch">
          <el-input
            v-model="keyword"
            class="search-input"
            placeholder="搜索商品、二手闲置"
            clearable
          />
          <el-segmented v-model="searchMode" :options="searchModes" size="small" />
          <el-button type="primary" native-type="submit">搜索</el-button>
        </form>

        <div class="header-actions">
          <button class="quick-action" type="button" @click="router.push('/order')">我的订单</button>
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-pill" type="button">
              <span class="avatar">{{ avatarText }}</span>
              <span>{{ displayName }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="cart">购物车</el-dropdown-item>
                <el-dropdown-item command="addresses">地址管理</el-dropdown-item>
                <el-dropdown-item command="credit">我的信用</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <nav class="nav-row">
        <button
          v-for="item in primaryNav"
          :key="item.path"
          type="button"
          class="nav-item"
          :class="{ active: isActive(item) }"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </nav>
    </header>

    <main class="layout-main fade-in-up">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const keyword = ref("");
const searchMode = ref("product");
const searchModes = [
  { label: "商品", value: "product" },
  { label: "二手", value: "secondhand" },
];

const primaryNav = computed(() => [
  { label: "首页", path: "/" },
  { label: "商品市场", path: "/product" },
  { label: "二手市场", path: "/secondhand" },
  { label: "发布闲置", path: "/secondhand/publish" },
  { label: "消息", path: "/messages" },
  { label: "通知", path: "/notifications" },
  { label: "售后", path: "/after-sale" },
  userStore.currentRole === "OFFICIAL_SELLER"
    ? { label: "卖家工作台", path: "/merchant" }
    : { label: "申请开店", path: "/merchant-apply" },
]);

const displayName = computed(() =>
  userStore.userInfo?.nickname || userStore.userInfo?.username || "普通用户",
);

const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());

function submitSearch() {
  const target = searchMode.value === "secondhand" ? "/secondhand" : "/product";
  const trimmed = keyword.value.trim();
  router.push({
    path: target,
    query: trimmed ? { keyword: trimmed } : {},
  });
}

function isActive(item) {
  if (item.path === "/") {
    return route.path === "/";
  }
  return route.path === item.path || route.path.startsWith(`${item.path}/`);
}

function handleCommand(command) {
  if (command === "logout") {
    userStore.logout();
    ElMessage.success("已退出登录");
    router.push("/login");
    return;
  }
  const map = {
    profile: "/profile",
    cart: "/cart",
    addresses: "/addresses",
    credit: "/credit",
  };
  router.push(map[command] || "/profile");
}
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
}

.market-header {
  position: sticky;
  top: 0;
  z-index: 20;
  background: linear-gradient(115deg, #dcefe9 0%, #f7efe5 48%, #f1f0fb 100%);
  border-bottom: 1px solid rgba(39, 50, 58, 0.12);
  box-shadow: 0 10px 24px rgba(32, 36, 45, 0.08);
}

.header-inner {
  width: min(var(--container), calc(100% - 32px));
  min-height: 72px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: auto minmax(280px, 1fr) auto;
  align-items: center;
  gap: 18px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
  color: var(--brand-primary);
}

.brand-mark {
  width: 52px;
  height: 52px;
  border: 3px solid var(--brand-primary);
  border-radius: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 23px;
  font-weight: 900;
  line-height: 1;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.15;
}

.brand-copy strong {
  font-size: 18px;
  font-weight: 900;
}

.brand-copy small {
  margin-top: 4px;
  color: #4a473d;
  font-weight: 700;
}

.search-bar {
  height: 48px;
  display: grid;
  grid-template-columns: minmax(160px, 1fr) auto auto;
  align-items: center;
  gap: 8px;
  padding: 5px;
  background: #ffffff;
  border: 2px solid var(--brand-primary);
  border-radius: 999px;
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: none;
  border-radius: 999px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.quick-action,
.user-pill {
  height: 42px;
  border: 0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--brand-primary);
  padding: 0 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 800;
  cursor: pointer;
  white-space: nowrap;
}

.quick-action:hover,
.user-pill:hover {
  background: #ffffff;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--brand-primary);
  color: var(--brand-accent);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}

.nav-row {
  width: min(var(--container), calc(100% - 32px));
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 0 14px;
  overflow-x: auto;
}

.nav-item {
  min-height: 38px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--brand-primary);
  padding: 0 16px;
  font-size: 15px;
  font-weight: 900;
  white-space: nowrap;
  cursor: pointer;
}

.nav-item.active,
.nav-item:hover {
  background: #ffffff;
}

.layout-main {
  width: min(var(--container), calc(100% - 32px));
  margin: 0 auto;
  padding: 24px 0 36px;
}

@media (max-width: 1050px) {
  .header-inner {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 14px 0 10px;
  }

  .brand-copy small {
    display: none;
  }

  .header-actions {
    justify-content: space-between;
  }
}

@media (max-width: 680px) {
  .header-inner,
  .nav-row,
  .layout-main {
    width: min(100% - 20px, var(--container));
  }

  .search-bar {
    grid-template-columns: 1fr;
    height: auto;
    border-radius: 20px;
    padding: 8px;
  }

  .header-actions {
    overflow-x: auto;
  }

  .quick-action,
  .user-pill {
    height: 38px;
    padding: 0 12px;
  }

  .layout-main {
    padding-top: 16px;
  }
}
</style>
