<template>
  <div class="user-layout">
    <header class="market-header">
      <div class="top-strip">
        <div class="top-inner">
          <span>欢迎来到 Kinda Goods</span>
          <div class="top-links">
            <button type="button" @click="router.push('/faq')">常见问题</button>
            <button type="button" @click="router.push('/credit')">信用中心</button>
            <button type="button" @click="router.push('/messages')">消息</button>
            <button type="button" @click="router.push('/notifications')">通知</button>
          </div>
        </div>
      </div>

      <div class="header-inner">
        <button class="brand" type="button" @click="router.push('/')">
          <img class="brand-logo" :src="logoUrl" alt="Kinda Goods" />
        </button>

        <div class="search-stack">
          <form class="search-bar" @submit.prevent="submitSearch">
            <el-segmented v-model="searchMode" :options="searchModes" size="small" @change="handleSearchModeChange" />
            <el-input
              v-model="keyword"
              class="search-input"
              :placeholder="searchPlaceholder"
              clearable
            />
            <el-button type="primary" native-type="submit">搜索</el-button>
          </form>
        </div>

        <div class="header-actions">
          <nav class="main-nav" aria-label="主导航">
            <button
              v-for="item in navItems"
              :key="item.path"
              class="nav-action"
              :class="{ active: isNavActive(item) }"
              type="button"
              @click="router.push(item.path)"
            >
              {{ item.label }}
            </button>
          </nav>

          <button
            v-if="isOfficialSeller"
            class="nav-action seller-entry"
            type="button"
            @click="router.push('/merchant')"
          >
            卖家工作台
          </button>

          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-pill" type="button">
              <span class="avatar">{{ avatarText }}</span>
              <span>{{ displayName }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="messages">消息中心</el-dropdown-item>
                <el-dropdown-item command="secondhandMine">我的闲置/拍卖</el-dropdown-item>
                <el-dropdown-item command="orders">新品订单</el-dropdown-item>
                <el-dropdown-item command="secondhandOrders">二手订单</el-dropdown-item>
                <el-dropdown-item command="browseHistory">浏览记录</el-dropdown-item>
                <el-dropdown-item command="addresses">地址管理</el-dropdown-item>
                <el-dropdown-item command="credit">信用中心</el-dropdown-item>
                <el-dropdown-item v-if="isOfficialSeller" command="merchant">卖家工作台</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="layout-main fade-in-up">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import logoUrl from "@/assets/kinda-goods-logo.svg";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const keyword = ref("");
const searchMode = ref("product");
const searchModes = [
  { label: "新品", value: "product" },
  { label: "二手", value: "secondhand" },
];

const navItems = [
  { label: "首页", path: "/", match: (path) => path === "/" },
  { label: "新品商城", path: "/product", match: (path) => path.startsWith("/product") || path === "/cart" || path.startsWith("/order") },
  { label: "二手商城", path: "/secondhand", match: (path) => path.startsWith("/secondhand") },
];

const displayName = computed(() =>
  userStore.userInfo?.nickname || userStore.userInfo?.username || "普通用户",
);
const isOfficialSeller = computed(() => userStore.currentRole === "OFFICIAL_SELLER" || userStore.currentRole === "SELLER");

const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());
const searchPlaceholder = computed(() =>
  searchMode.value === "secondhand"
    ? "搜索二手教材、闲置耳机、宿舍收纳"
    : "搜索新品键盘、耳机、教材、宿舍好物",
);

watch(
  () => route.path,
  () => {
    searchMode.value = route.path.startsWith("/secondhand") ? "secondhand" : "product";
  },
  { immediate: true },
);

watch(
  () => route.query.keyword,
  (value) => {
    keyword.value = typeof value === "string" ? value : "";
  },
  { immediate: true },
);

function isNavActive(item) {
  return item.match(route.path);
}

function submitSearch() {
  const target = searchMode.value === "secondhand" ? "/secondhand" : "/product";
  const trimmed = keyword.value.trim();
  router.push({
    path: target,
    query: trimmed ? { keyword: trimmed } : {},
  });
}

function handleSearchModeChange() {
  const target = searchMode.value === "secondhand" ? "/secondhand" : "/product";
  const trimmed = keyword.value.trim();
  router.push({
    path: target,
    query: trimmed ? { keyword: trimmed } : {},
  });
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
    messages: "/messages",
    secondhandMine: "/secondhand/mine",
    orders: "/order",
    secondhandOrders: "/secondhand/orders",
    browseHistory: "/browse-history",
    addresses: "/addresses",
    credit: "/credit",
    merchant: "/merchant",
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
  background: rgba(255, 255, 255, 0.94);
  border-bottom: 1px solid rgba(137, 199, 255, 0.28);
  box-shadow: 0 8px 24px rgba(137, 199, 255, 0.12);
  backdrop-filter: blur(14px);
}

.top-strip {
  background: linear-gradient(90deg, #e9fff8 0%, #eaf4ff 54%, #fff7fb 100%);
  color: var(--text-secondary);
  font-size: 12px;
}

.top-inner {
  width: min(var(--container), calc(100% - 32px));
  height: 32px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.top-links {
  display: flex;
  align-items: center;
  gap: 14px;
}

.top-links button {
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  padding: 0;
  font-size: 12px;
  cursor: pointer;
}

.top-links button:hover {
  color: var(--brand-primary);
}

.header-inner {
  width: min(var(--container), calc(100% - 32px));
  min-height: 74px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 190px minmax(320px, 560px) minmax(310px, 1fr);
  align-items: center;
  gap: 18px;
}

.brand {
  display: inline-flex;
  align-items: center;
  border: 0;
  background: transparent;
  padding: 0;
  cursor: pointer;
  color: var(--text-main);
}

.brand-logo {
  width: 168px;
  height: 58px;
  object-fit: contain;
  object-position: left center;
  display: block;
}

.search-stack {
  min-width: 0;
}

.search-bar {
  height: 44px;
  display: grid;
  grid-template-columns: auto minmax(150px, 1fr) 82px;
  align-items: center;
  gap: 6px;
  padding: 4px;
  background: #ffffff;
  border: 2px solid #b9defb;
  border-radius: 14px;
  box-shadow: 0 12px 24px rgba(137, 199, 255, 0.14);
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: none;
  border-radius: 6px;
}

.search-bar :deep(.el-segmented) {
  --el-segmented-item-selected-color: #ffffff;
  --el-segmented-item-selected-bg-color: #69c7ef;
  --el-border-radius-base: 6px;
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
  white-space: normal;
}

.main-nav {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.nav-action,
.user-pill {
  height: 38px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  color: var(--text-main);
  padding: 0 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 800;
  cursor: pointer;
  white-space: nowrap;
  flex: 0 0 auto;
}

.nav-action:hover,
.user-pill:hover {
  border-color: #9bd8f9;
  color: var(--brand-primary);
  background: var(--brand-primary-weak);
}

.nav-action.active {
  border-color: rgba(105, 199, 239, 0.55);
  background: linear-gradient(135deg, rgba(233, 255, 248, 0.9), rgba(234, 244, 255, 0.95));
  color: var(--brand-primary);
}

.seller-entry {
  border-color: rgba(95, 230, 189, 0.45);
  background: linear-gradient(135deg, #e9fff8 0%, #eaf4ff 100%);
  color: #0f8f70;
}

.user-pill {
  flex: 0 0 auto;
  padding-right: 14px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--brand-gradient-strong);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}

.layout-main {
  width: min(var(--container), calc(100% - 32px));
  margin: 0 auto;
  padding: 18px 0 36px;
}

@media (max-width: 1320px) {
  .header-inner {
    grid-template-columns: 170px minmax(260px, 1fr);
    gap: 12px;
    padding: 10px 0;
  }

  .header-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }

  .main-nav {
    overflow: visible;
  }
}

@media (max-width: 680px) {
  .top-inner {
    width: min(100% - 20px, var(--container));
  }

  .top-links {
    display: none;
  }

  .header-inner,
  .layout-main {
    width: min(100% - 20px, var(--container));
  }

  .header-inner {
    grid-template-columns: 1fr;
  }

  .brand-logo {
    width: 154px;
    height: 52px;
  }

  .search-bar {
    grid-template-columns: 1fr;
    height: auto;
    border-radius: 10px;
    padding: 8px;
  }

  .header-actions {
    align-items: flex-start;
    overflow-x: auto;
  }

  .nav-action,
  .user-pill {
    height: 36px;
    padding: 0 12px;
  }

  .layout-main {
    padding-top: 16px;
  }
}
</style>
