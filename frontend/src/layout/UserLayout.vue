<template>
  <div class="market-shell">
    <header class="market-header">
      <div class="header-inner">
        <button class="brand" type="button" @click="go('/')">
          <span class="brand-mark">kg</span>
          <span class="brand-name">kinda goods</span>
        </button>

        <form class="search-bar" @submit.prevent="submitSearch">
          <input
            v-model="keyword"
            type="search"
            placeholder="搜索商品、闲置、教材、数码好物"
            aria-label="搜索商品"
          />
          <button type="submit">搜索</button>
        </form>

        <nav class="header-actions" aria-label="顶部快捷入口">
          <button type="button" @click="go('/cart')">购物车</button>
          <button type="button" @click="go('/order')">订单</button>
          <button v-if="!userStore.isLoggedIn" class="user-entry" type="button" @click="go('/login')">
            <span class="avatar">{{ avatarText }}</span>
            <span>{{ displayName }}</span>
          </button>
          <el-dropdown v-else trigger="click" placement="bottom-end" @command="handleUserCommand">
            <button class="user-entry" type="button">
              <span class="avatar">{{ avatarText }}</span>
              <span>{{ displayName }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="item in profileItems"
                  :key="item.path"
                  :command="item.path"
                >
                  {{ item.label }}
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </nav>
      </div>

      <div class="nav-strip">
        <button
          v-for="item in primaryNav"
          :key="item.path"
          class="nav-chip"
          :class="{ active: isActive(item.path) }"
          type="button"
          @click="go(item.path)"
        >
          {{ item.label }}
        </button>
      </div>
    </header>

    <main class="page-stage fade-in-up">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const keyword = ref("");

const primaryNav = [
  { label: "首页", path: "/" },
  { label: "商品市场", path: "/product" },
  { label: "二手闲置", path: "/secondhand" },
  { label: "发布闲置", path: "/secondhand/publish" },
  { label: "领券中心", path: "/vouchers/claim" },
  { label: "消息", path: "/messages" },
  { label: "通知", path: "/notifications" },
  { label: "卖家工作台", path: "/merchant" },
  { label: "管理后台", path: "/admin" },
];

const profileItems = [
  { label: "个人资料", path: "/profile" },
  { label: "地址管理", path: "/addresses" },
  { label: "我的订单", path: "/order" },
  { label: "购物车", path: "/cart" },
  { label: "我的评价", path: "/my-reviews" },
  { label: "浏览记录", path: "/browse-history" },
  { label: "我的优惠券", path: "/vouchers" },
  { label: "售后 / 退款", path: "/after-sale" },
  { label: "我的信用", path: "/credit" },
  { label: "站内消息", path: "/messages" },
  { label: "通知", path: "/notifications" },
  { label: "申请成为卖家", path: "/merchant-apply" },
];

const displayName = computed(() => {
  if (!userStore.isLoggedIn) return "登录";
  return userStore.userInfo?.nickname || userStore.userInfo?.username || "我的";
});

const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());

function go(path) {
  router.push(path);
}

function isActive(path) {
  if (path === "/") return route.path === "/";
  return route.path.startsWith(path);
}

function submitSearch() {
  const value = keyword.value.trim();
  router.push(value ? { path: "/product", query: { keyword: value } } : "/product");
}

function handleUserCommand(command) {
  if (command === "logout") {
    userStore.logout();
    router.push("/login");
    return;
  }
  router.push(command);
}
</script>

<style scoped>
.market-shell {
  min-height: 100vh;
  background: #f2f4f6;
}

.market-header {
  position: sticky;
  top: 0;
  z-index: 40;
  background: #ffe100;
  box-shadow: 0 2px 12px rgba(31, 35, 43, 0.08);
}

.header-inner {
  width: min(1440px, calc(100% - 44px));
  height: 76px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 180px minmax(280px, 1fr) auto;
  align-items: center;
  gap: 22px;
}

.brand,
.header-actions button,
.nav-chip {
  font: inherit;
}

.brand {
  border: 0;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  background: transparent;
  color: #20242d;
  cursor: pointer;
  font-weight: 900;
}

.brand-mark {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border: 3px solid #20242d;
  border-radius: 14px;
  font-size: 18px;
  letter-spacing: 0;
  text-transform: lowercase;
  line-height: 1;
}

.brand-name {
  font-size: 24px;
  white-space: nowrap;
}

.search-bar {
  height: 42px;
  min-width: 0;
  display: flex;
  align-items: center;
  border: 2px solid #20242d;
  border-radius: 999px;
  background: #fff;
  overflow: hidden;
}

.search-bar input {
  min-width: 0;
  flex: 1;
  height: 100%;
  border: 0;
  outline: 0;
  padding: 0 20px;
  background: transparent;
  color: #20242d;
  font-size: 15px;
}

.search-bar button {
  height: 34px;
  margin-right: 4px;
  border: 0;
  border-radius: 999px;
  padding: 0 18px;
  background: #20242d;
  color: #ffe100;
  cursor: pointer;
  font-weight: 800;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions > button,
.user-entry {
  height: 34px;
  border: 0;
  border-radius: 999px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background: rgba(255, 255, 255, 0.58);
  color: #20242d;
  cursor: pointer;
  font-weight: 700;
}

.header-actions > button:hover,
.user-entry:hover {
  background: #fff;
}

.avatar {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #20242d;
  color: #ffe100;
  font-size: 13px;
  font-weight: 900;
}

.nav-strip {
  width: min(1440px, calc(100% - 44px));
  margin: 0 auto;
  padding: 0 0 12px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.nav-chip {
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

.nav-chip:hover,
.nav-chip.active {
  background: #fff;
}

.page-stage {
  width: min(1440px, calc(100% - 44px));
  margin: 18px auto 32px;
  min-width: 0;
}

@media (max-width: 1180px) {
  .header-inner {
    grid-template-columns: 150px minmax(220px, 1fr) auto;
    gap: 14px;
  }

  .brand-name {
    display: none;
  }

  .header-actions > button:first-child {
    display: none;
  }
}

@media (max-width: 760px) {
  .header-inner {
    width: calc(100% - 20px);
    height: auto;
    padding: 10px 0;
    grid-template-columns: auto 1fr;
  }

  .brand-mark {
    width: 40px;
    height: 40px;
    font-size: 24px;
  }

  .search-bar {
    grid-column: 1 / -1;
    order: 3;
  }

  .header-actions {
    justify-content: flex-end;
  }

  .header-actions > button:nth-child(n + 2) {
    display: none;
  }

  .user-entry span:last-child {
    display: none;
  }

  .nav-strip,
  .page-stage {
    width: calc(100% - 16px);
  }

  .page-stage {
    margin-top: 10px;
  }
}
</style>
