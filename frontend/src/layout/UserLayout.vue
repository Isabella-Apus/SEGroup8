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

        <div ref="searchStackRef" class="search-stack">
          <form class="search-bar" @submit.prevent="submitSearch">
            <el-input
              v-model="keyword"
              class="search-input"
              placeholder="搜索商品、闲置、教材、数码好物"
              clearable
              @focus="openSearchPanel"
            />
            <el-button type="primary" native-type="submit">搜索</el-button>
          </form>
          <div v-if="showSearchPanel" class="search-suggest-panel" @mousedown.prevent>
            <div class="suggest-head">
              <strong>{{ keyword.trim() ? `搜索“${keyword.trim()}”` : "热门商品" }}</strong>
              <button type="button" @click="closeSearchPanel">关闭</button>
            </div>

            <div v-if="searchLoading" class="suggest-loading">正在查找相关商品...</div>

            <template v-else>
              <div v-if="keyword.trim() && hasSearchResults" class="suggest-sections">
                <section class="suggest-section">
                  <div class="section-title">
                    <span>一手商品</span>
                    <em>{{ productSuggestions.length }} 件相关</em>
                  </div>
                  <button
                    v-for="item in productSuggestions"
                    :key="`product-${item.id}`"
                    class="suggest-item"
                    type="button"
                    @click="goProduct(item, 'product')"
                  >
                    <img :src="productCover(item)" :alt="item.name" />
                    <span class="item-copy">
                      <strong>{{ item.name }}</strong>
                      <small>{{ item.description || item.categoryName || "官方商城商品" }}</small>
                    </span>
                    <span class="item-price">¥{{ formatPrice(item.price) }}</span>
                  </button>
                </section>

                <section class="suggest-section">
                  <div class="section-title">
                    <span>二手商品</span>
                    <em>{{ secondhandSuggestions.length }} 件相关</em>
                  </div>
                  <button
                    v-for="item in secondhandSuggestions"
                    :key="`secondhand-${item.id}`"
                    class="suggest-item"
                    type="button"
                    @click="goProduct(item, 'secondhand')"
                  >
                    <img :src="productCover(item)" :alt="item.name" />
                    <span class="item-copy">
                      <strong>{{ item.name }}</strong>
                      <small>{{ item.condition || item.categoryName || "校园闲置商品" }}</small>
                    </span>
                    <span class="item-price second">¥{{ formatPrice(item.price) }}</span>
                  </button>
                </section>
              </div>

              <div v-else-if="keyword.trim()" class="empty-suggest">
                <strong>暂无相关商品</strong>
                <span>换个关键词试试，或看看下面的热门推荐</span>
              </div>

              <section v-if="!hasSearchResults && hotSuggestions.length" class="suggest-section hot-section">
                <div class="section-title">
                  <span>热门商品</span>
                  <em>猜你喜欢</em>
                </div>
                <button
                  v-for="item in hotSuggestions"
                  :key="`hot-${item.type}-${item.id}`"
                  class="suggest-item"
                  type="button"
                  @click="goProduct(item, item.type)"
                >
                  <img :src="productCover(item)" :alt="item.name" />
                  <span class="item-copy">
                    <strong>{{ item.name }}</strong>
                    <small>{{ item.type === 'secondhand' ? '二手精选' : '一手热卖' }}</small>
                  </span>
                  <span class="item-price">¥{{ formatPrice(item.price) }}</span>
                </button>
              </section>
            </template>
          </div>
        </div>

        <div class="header-actions">
          <nav class="main-nav" aria-label="主导航">
            <button
              v-for="item in visibleNavItems"
              :key="item.path"
              class="nav-action"
              :class="{ active: isNavActive(item) }"
              type="button"
              @click="router.push(item.path)"
            >
              <span>{{ item.label }}</span>
            </button>
          </nav>
          
          <el-dropdown trigger="click" @command="handleCommand">
            <button class="user-pill" type="button">
              <span class="avatar">{{ avatarText }}</span>
              <span>{{ displayName }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="messages">消息中心</el-dropdown-item>
                <el-dropdown-item command="secondhandMine">我的闲置</el-dropdown-item>
                <el-dropdown-item command="secondhandSold">我卖出的</el-dropdown-item>
                <el-dropdown-item command="orders">我的订单</el-dropdown-item>
                <el-dropdown-item command="browseHistory">浏览记录</el-dropdown-item>
                <el-dropdown-item command="addresses">地址管理</el-dropdown-item>
                <el-dropdown-item command="credit">信用中心</el-dropdown-item>
                <el-dropdown-item v-if="isOfficialSeller" command="merchant">卖家工作台</el-dropdown-item>
                <el-dropdown-item v-if="isAdmin" command="admin">管理后台</el-dropdown-item>
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useUserStore } from "@/stores/user";
import { getProductListApi } from "@/api/product";
import { getSecondhandListApi } from "@/api/secondhand";
import { toAssetUrl } from "@/utils/url";
import logoUrl from "@/assets/kinda-goods-logo.svg";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const keyword = ref("");
const searchStackRef = ref(null);
const showSearchPanel = ref(false);
const searchLoading = ref(false);
const productSuggestions = ref([]);
const secondhandSuggestions = ref([]);
const hotProducts = ref([]);
const hotSecondhand = ref([]);
let searchTimer = 0;
let searchRequestSeq = 0;

const navItems = [
  { label: "首页", path: "/", match: (path) => path === "/" },
  { label: "新品商城", path: "/product", match: (path) => path.startsWith("/product") || path === "/cart" || path.startsWith("/order") },
  { label: "二手商城", path: "/secondhand", match: (path) => path.startsWith("/secondhand") },
  { label: "管理后台", path: "/admin", match: (path) => path.startsWith("/admin"), adminOnly: true },
  { label: "卖家工作台", path: "/merchant", match: (path) => path.startsWith("/merchant") },
];

const displayName = computed(() =>
  userStore.userInfo?.nickname || userStore.userInfo?.username || "普通用户",
);
const isOfficialSeller = computed(() => userStore.currentRole === "OFFICIAL_SELLER" || userStore.currentRole === "SELLER");
const isAdmin = computed(() => userStore.currentRole === "ADMIN");
const visibleNavItems = computed(() =>
  navItems.filter((item) => {
    if (item.adminOnly) {
      return isAdmin.value;
    }
    return item.path !== "/merchant" || isOfficialSeller.value;
  }),
);

const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());
const hasSearchResults = computed(() => productSuggestions.value.length > 0 || secondhandSuggestions.value.length > 0);
const hotSuggestions = computed(() => [
  ...hotProducts.value.map((item) => ({ ...item, type: "product" })),
  ...hotSecondhand.value.map((item) => ({ ...item, type: "secondhand" })),
].slice(0, 5));

watch(
  () => route.query.keyword,
  (value) => {
    keyword.value = typeof value === "string" ? value : "";
  },
  { immediate: true },
);

watch(keyword, () => {
  if (!showSearchPanel.value) {
    return;
  }
  window.clearTimeout(searchTimer);
  searchTimer = window.setTimeout(loadSearchSuggestions, 220);
});

function isNavActive(item) {
  return item.match(route.path);
}

function submitSearch() {
  const trimmed = keyword.value.trim();
  closeSearchPanel();
  router.push({
    path: "/product",
    query: trimmed ? { keyword: trimmed } : {},
  });
}

function openSearchPanel() {
  showSearchPanel.value = true;
  loadSearchSuggestions();
}

function closeSearchPanel() {
  showSearchPanel.value = false;
}

async function loadSearchSuggestions() {
  const seq = ++searchRequestSeq;
  const trimmed = keyword.value.trim();
  if (!trimmed) {
    productSuggestions.value = [];
    secondhandSuggestions.value = [];
    searchLoading.value = false;
    return;
  }
  searchLoading.value = true;
  try {
    const params = { pageNum: 1, pageSize: 4, ...(trimmed ? { keyword: trimmed } : {}) };
    const [productResult, secondhandResult] = await Promise.all([
      getProductListApi(params),
      getSecondhandListApi(params),
    ]);
    if (seq !== searchRequestSeq) {
      return;
    }
    productSuggestions.value = productResult.data?.records || [];
    secondhandSuggestions.value = secondhandResult.data?.records || [];
  } catch {
    if (seq === searchRequestSeq) {
      productSuggestions.value = [];
      secondhandSuggestions.value = [];
    }
  } finally {
    if (seq === searchRequestSeq) {
      searchLoading.value = false;
    }
  }
}

async function loadHotSuggestions() {
  try {
    const [productResult, secondhandResult] = await Promise.all([
      getProductListApi({ pageNum: 1, pageSize: 4 }),
      getSecondhandListApi({ pageNum: 1, pageSize: 4 }),
    ]);
    hotProducts.value = productResult.data?.records || [];
    hotSecondhand.value = secondhandResult.data?.records || [];
  } catch {
    hotProducts.value = [];
    hotSecondhand.value = [];
  }
}

function goProduct(item, type) {
  if (!item?.id) {
    return;
  }
  closeSearchPanel();
  router.push(type === "secondhand" ? `/secondhand/${item.id}` : `/product/${item.id}`);
}

function productCover(item) {
  const images = Array.isArray(item?.images) ? item.images : [];
  const url = item?.cover || item?.image || item?.imageUrl || images[0];
  if (!url) {
    return "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='96' height='96' viewBox='0 0 96 96'%3E%3Crect width='96' height='96' rx='12' fill='%23eef7fb'/%3E%3Cpath d='M25 63l14-18 11 12 8-9 13 15H25z' fill='%2369c7ef'/%3E%3Ccircle cx='37' cy='34' r='7' fill='%2312d89a'/%3E%3C/svg%3E";
  }
  return toAssetUrl(String(url).replace(/\\\\/g, "/"));
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2);
}

function handleDocumentPointerDown(event) {
  if (!showSearchPanel.value) {
    return;
  }
  if (!searchStackRef.value?.contains(event.target)) {
    closeSearchPanel();
  }
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
    secondhandSold: "/secondhand/sold",
    browseHistory: "/browse-history",
    addresses: "/addresses",
    credit: "/credit",
    merchant: "/merchant",
    admin: "/admin",
  };
  router.push(map[command] || "/profile");
}

onMounted(() => {
  loadHotSuggestions();
  document.addEventListener("pointerdown", handleDocumentPointerDown);
});

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", handleDocumentPointerDown);
  window.clearTimeout(searchTimer);
});
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
  grid-template-columns: 180px minmax(280px, 1fr) max-content;
  align-items: center;
  gap: 14px;
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
  position: relative;
}

.search-bar {
  height: 44px;
  display: grid;
  grid-template-columns: minmax(150px, 1fr) 82px;
  align-items: center;
  gap: 8px;
  padding: 4px;
  background: #ffffff;
  border: 2px solid #b9defb;
  border-radius: 12px;
  box-shadow: 0 12px 24px rgba(137, 199, 255, 0.14);
}

.search-input :deep(.el-input__wrapper) {
  box-shadow: none;
  border-radius: 6px;
}

.search-suggest-panel {
  position: absolute;
  top: calc(100% + 10px);
  left: 0;
  right: 0;
  z-index: 30;
  max-height: min(72vh, 620px);
  overflow: auto;
  border: 1px solid rgba(137, 199, 255, 0.42);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow:
    0 22px 60px rgba(18, 50, 65, 0.16),
    0 1px 0 rgba(255, 255, 255, 0.95) inset;
  padding: 12px;
}

.suggest-head,
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.suggest-head {
  padding: 4px 4px 10px;
  border-bottom: 1px solid var(--line-soft);
}

.suggest-head strong {
  min-width: 0;
  color: var(--text-main);
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suggest-head button {
  border: 0;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-weight: 800;
}

.suggest-head button:hover {
  color: var(--brand-primary);
}

.suggest-loading,
.empty-suggest {
  margin: 12px 0;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(233, 255, 248, 0.9), rgba(234, 244, 255, 0.9));
  color: var(--text-secondary);
  padding: 18px;
  text-align: center;
}

.empty-suggest {
  display: grid;
  gap: 5px;
}

.empty-suggest strong {
  color: var(--text-main);
  font-size: 16px;
}

.empty-suggest span {
  font-size: 13px;
}

.suggest-section {
  padding: 12px 0 2px;
}

.suggest-section + .suggest-section {
  border-top: 1px solid var(--line-soft);
}

.section-title {
  margin-bottom: 8px;
}

.section-title span {
  color: var(--text-main);
  font-size: 14px;
  font-weight: 900;
}

.section-title em {
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.suggest-item {
  width: 100%;
  min-height: 68px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  padding: 8px;
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.suggest-item:hover {
  background: rgba(234, 248, 255, 0.9);
}

.suggest-item img {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  object-fit: cover;
  background: #eef7fb;
}

.item-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.item-copy strong,
.item-copy small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-copy strong {
  color: var(--text-main);
  font-size: 14px;
  line-height: 1.2;
}

.item-copy small {
  color: var(--text-secondary);
  font-size: 12px;
}

.item-price {
  color: #1767ff;
  font-weight: 950;
  white-space: nowrap;
}

.item-price.second {
  color: #0f9f78;
}

.hot-section {
  margin-top: 6px;
  border-top: 1px solid var(--line-soft);
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 10px;
  min-width: 0;
  white-space: nowrap;
}

.main-nav {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
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
    grid-template-columns: 1fr auto;
    height: auto;
    border-radius: 10px;
    padding: 8px;
  }

  .search-suggest-panel {
    position: fixed;
    top: 118px;
    left: 10px;
    right: 10px;
    max-height: calc(100vh - 136px);
  }

  .suggest-item {
    grid-template-columns: 48px minmax(0, 1fr);
  }

  .item-price {
    grid-column: 2;
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
