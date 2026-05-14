<template>
  <div class="market-shell">
    <header class="market-header">
      <div class="header-inner">
        <button class="brand" type="button" @click="go('/')">
          <span class="brand-mark">kg</span>
          <span class="brand-name">kinda goods</span>
        </button>

        <div class="search-wrap" @focusin="openSuggestions" @focusout="scheduleCloseSuggestions">
          <form class="search-bar" @submit.prevent="submitSearch">
            <input
              v-model="keyword"
              type="search"
              placeholder="搜索商品、二手闲置"
              aria-label="搜索商品"
            />
            <button type="submit">搜索</button>
          </form>

          <div v-if="showSuggestionPanel" class="search-panel">
            <div class="search-panel-head">
              <span>搜索“{{ normalizedKeyword }}”</span>
              <span class="search-panel-actions">
                <button type="button" @mousedown.prevent="submitSearch">查看一手商品列表</button>
                <button type="button" @mousedown.prevent="submitSecondhandSearch">查看二手商品列表</button>
              </span>
            </div>

            <div v-if="suggestionLoading" class="search-loading">
              <span class="loading-dot"></span>
              正在查找相关商品...
            </div>

            <template v-else>
              <template v-if="hasSuggestions">
                <section v-if="productSuggestions.length" class="suggestion-section">
                  <div class="section-title">
                    <span>一手商品</span>
                    <em>{{ productSuggestions.length }} 件相关</em>
                  </div>
                  <button
                    v-for="item in productSuggestions"
                    :key="`product-${item.id}`"
                    class="suggestion-item"
                    type="button"
                    @mousedown.prevent="goSuggestion(`/product/${item.id}`)"
                  >
                    <span class="suggestion-cover">
                      <img
                        v-if="showSuggestionImage(item)"
                        :src="suggestionImage(item)"
                        alt=""
                        @error="markImageBroken(item)"
                      />
                      <span v-else class="suggestion-placeholder">kg</span>
                    </span>
                    <span class="suggestion-info">
                      <span class="suggestion-name">{{ item.name }}</span>
                      <span class="suggestion-meta">
                        <strong>¥{{ formatPrice(item.price) }}</strong>
                        <em>官方商城</em>
                      </span>
                    </span>
                  </button>
                </section>

                <section v-if="secondhandSuggestions.length" class="suggestion-section">
                  <div class="section-title">
                    <span>二手商品</span>
                    <em>{{ secondhandSuggestions.length }} 件相关</em>
                  </div>
                  <button
                    v-for="item in secondhandSuggestions"
                    :key="`secondhand-${item.id}`"
                    class="suggestion-item"
                    type="button"
                    @mousedown.prevent="goSuggestion(`/secondhand/${item.id}`)"
                  >
                    <span class="suggestion-cover">
                      <img
                        v-if="showSuggestionImage(item)"
                        :src="suggestionImage(item)"
                        alt=""
                        @error="markImageBroken(item)"
                      />
                      <span v-else class="suggestion-placeholder secondhand">2H</span>
                    </span>
                    <span class="suggestion-info">
                      <span class="suggestion-name">{{ item.name }}</span>
                      <span class="suggestion-meta">
                        <strong>¥{{ formatPrice(item.salePrice ?? item.price) }}</strong>
                        <em>闲置转让</em>
                      </span>
                    </span>
                  </button>
                </section>
              </template>

              <template v-else>
                <div class="empty-search">
                  <strong>暂无相关商品</strong>
                  <span>换个关键词试试，或看看下方热门商品。</span>
                </div>
                <section v-if="hotProducts.length" class="suggestion-section hot-section">
                  <div class="section-title">
                    <span>热门商品</span>
                    <em>为你推荐</em>
                  </div>
                  <button
                    v-for="item in hotProducts"
                    :key="`hot-${item.id}`"
                    class="suggestion-item"
                    type="button"
                    @mousedown.prevent="goSuggestion(`/product/${item.id}`)"
                  >
                    <span class="suggestion-cover">
                      <img
                        v-if="showSuggestionImage(item)"
                        :src="suggestionImage(item)"
                        alt=""
                        @error="markImageBroken(item)"
                      />
                      <span v-else class="suggestion-placeholder">kg</span>
                    </span>
                    <span class="suggestion-info">
                      <span class="suggestion-name">{{ item.name }}</span>
                      <span class="suggestion-meta">
                        <strong>¥{{ formatPrice(item.price) }}</strong>
                        <em>热门推荐</em>
                      </span>
                    </span>
                  </button>
                </section>
              </template>
            </template>
          </div>
        </div>

        <nav class="header-actions" aria-label="快捷入口">
          <button type="button" @click="go('/cart')">购物车</button>
          <button type="button" @click="go('/order')">我的订单</button>
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
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import { getProductListApi } from "@/api/product";
import { getSecondhandListApi } from "@/api/secondhand";
import { getFirstProductImage, toFullImageUrl } from "@/utils/productImages";
import { searchList } from "@/utils/search";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const keyword = ref("");
const searchFocused = ref(false);
const suggestionLoading = ref(false);
const productSuggestions = ref([]);
const secondhandSuggestions = ref([]);
const hotProducts = ref([]);
const brokenImageUrls = ref(new Set());
let searchTimer = null;
let closeTimer = null;
let searchSeq = 0;

const basePrimaryNav = [
  { label: "首页", path: "/" },
  { label: "商品市场", path: "/product" },
  { label: "二手市场", path: "/secondhand" },
  { label: "发布闲置", path: "/secondhand/publish" },
  { label: "领券中心", path: "/vouchers/claim" },
  { label: "消息", path: "/messages" },
  { label: "通知", path: "/notifications" },
  { label: "常见问题", path: "/faq" },
];

const rolePrimaryNav = [
  { label: "商家工作台", path: "/merchant", roles: ["OFFICIAL_SELLER", "SELLER"] },
  { label: "管理后台", path: "/admin", roles: ["ADMIN"] },
];

const baseProfileItems = [
  { label: "个人资料", path: "/profile" },
  { label: "地址管理", path: "/addresses" },
  { label: "我的订单", path: "/order" },
  { label: "购物车", path: "/cart" },
  { label: "我的评价", path: "/my-reviews" },
  { label: "浏览历史", path: "/browse-history" },
  { label: "我的优惠券", path: "/vouchers" },
  { label: "退款/售后", path: "/after-sale" },
  { label: "我的信用", path: "/credit" },
  { label: "消息", path: "/messages" },
  { label: "通知", path: "/notifications" },
  { label: "常见问题", path: "/faq" },
];

const roleProfileItems = [
  { label: "申请成为商家", path: "/merchant-apply" },
  { label: "商家工作台", path: "/merchant", roles: ["OFFICIAL_SELLER", "SELLER"] },
  { label: "管理后台", path: "/admin", roles: ["ADMIN"] },
];

const currentRole = computed(() => userStore.currentRole);
const isSellerRole = computed(() => ["OFFICIAL_SELLER", "SELLER"].includes(currentRole.value));
const isAdminRole = computed(() => currentRole.value === "ADMIN");

const primaryNav = computed(() => [
  ...basePrimaryNav,
  ...rolePrimaryNav.filter((item) => item.roles.includes(currentRole.value)),
]);

const profileItems = computed(() => [
  ...baseProfileItems,
  ...roleProfileItems.filter((item) => {
    if (item.path === "/merchant-apply") {
      return userStore.isLoggedIn && !isSellerRole.value && !isAdminRole.value;
    }
    return item.roles?.includes(currentRole.value);
  }),
]);

const displayName = computed(() => {
  if (!userStore.isLoggedIn) return "请登录";
  return userStore.userInfo?.nickname || userStore.userInfo?.username || "我的";
});

const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase());
const normalizedKeyword = computed(() => keyword.value.trim());
const hasSuggestions = computed(() => productSuggestions.value.length > 0 || secondhandSuggestions.value.length > 0);
const showSuggestionPanel = computed(() => searchFocused.value && normalizedKeyword.value.length > 0);

watch(normalizedKeyword, (value) => {
  clearTimeout(searchTimer);
  if (!value) {
    productSuggestions.value = [];
    secondhandSuggestions.value = [];
    hotProducts.value = [];
    suggestionLoading.value = false;
    return;
  }
  searchTimer = setTimeout(() => fetchSuggestions(value), 250);
});

function go(path) {
  closeSuggestions();
  router.push(path);
}

function isActive(path) {
  if (path === "/") return route.path === "/";
  return route.path.startsWith(path);
}

function submitSearch() {
  const value = normalizedKeyword.value;
  closeSuggestions();
  router.push(value ? { path: "/product", query: { keyword: value } } : "/product");
}

function submitSecondhandSearch() {
  const value = normalizedKeyword.value;
  closeSuggestions();
  router.push(value ? { path: "/secondhand", query: { keyword: value } } : "/secondhand");
}

function openSuggestions() {
  clearTimeout(closeTimer);
  searchFocused.value = true;
  if (normalizedKeyword.value) {
    fetchSuggestions(normalizedKeyword.value);
  }
}

function scheduleCloseSuggestions() {
  closeTimer = setTimeout(() => {
    searchFocused.value = false;
  }, 160);
}

function closeSuggestions() {
  clearTimeout(closeTimer);
  searchFocused.value = false;
}

async function fetchSuggestions(value) {
  const seq = ++searchSeq;
  suggestionLoading.value = true;
  try {
    const [productRes, secondhandRes] = await Promise.all([
      getProductListApi({ pageNum: 1, pageSize: 4, keyword: value }),
      getSecondhandListApi({ pageNum: 1, pageSize: 4, keyword: value }),
    ]);
    if (seq !== searchSeq) return;
    productSuggestions.value = productRes.data?.records || [];
    secondhandSuggestions.value = (secondhandRes.data?.records || []).map((item) => ({
      ...item,
      salePrice: item.salePrice ?? item.price,
    }));
    if (!hasSuggestions.value) {
      const fuzzyHit = await fetchFuzzySuggestions(seq, value);
      if (!fuzzyHit) {
        await fetchHotProducts(seq);
      }
    } else {
      hotProducts.value = [];
    }
  } catch {
    if (seq === searchSeq) {
      productSuggestions.value = [];
      secondhandSuggestions.value = [];
      await fetchHotProducts(seq);
    }
  } finally {
    if (seq === searchSeq) {
      suggestionLoading.value = false;
    }
  }
}

async function fetchFuzzySuggestions(seq, value) {
  try {
    const [productRes, secondhandRes] = await Promise.all([
      getProductListApi({ pageNum: 1, pageSize: 200 }),
      getSecondhandListApi({ pageNum: 1, pageSize: 200 }),
    ]);
    if (seq !== searchSeq) return false;

    productSuggestions.value = searchList({
      items: productRes.data?.records || [],
      keyword: value,
      keys: ["name", "description"],
      options: { threshold: 0.45 },
    }).slice(0, 4);
    secondhandSuggestions.value = searchList({
      items: (secondhandRes.data?.records || []).map((item) => ({
        ...item,
        salePrice: item.salePrice ?? item.price,
      })),
      keyword: value,
      keys: ["name", "description"],
      options: { threshold: 0.45 },
    }).slice(0, 4);

    const found = productSuggestions.value.length > 0 || secondhandSuggestions.value.length > 0;
    if (found) {
      hotProducts.value = [];
    }
    return found;
  } catch {
    return false;
  }
}

async function fetchHotProducts(seq) {
  const res = await getProductListApi({ pageNum: 1, pageSize: 4, sortBy: "sales_desc" });
  if (seq === searchSeq) {
    hotProducts.value = res.data?.records || [];
  }
}

function goSuggestion(path) {
  closeSuggestions();
  router.push(path);
}

function handleUserCommand(command) {
  if (command === "logout") {
    userStore.logout();
    router.push("/login");
    return;
  }
  router.push(command);
}

function suggestionImage(item) {
  return toFullImageUrl(getFirstProductImage(item));
}

function showSuggestionImage(item) {
  const url = suggestionImage(item);
  return Boolean(url && !brokenImageUrls.value.has(url));
}

function markImageBroken(item) {
  const url = suggestionImage(item);
  if (!url) return;
  const next = new Set(brokenImageUrls.value);
  next.add(url);
  brokenImageUrls.value = next;
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2);
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
  grid-template-columns: 180px minmax(320px, 1fr) auto;
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

.search-wrap {
  position: relative;
  min-width: 0;
}

.search-bar {
  height: 44px;
  min-width: 0;
  display: flex;
  align-items: center;
  border: 2px solid #20242d;
  border-radius: 999px;
  background: #fff;
  overflow: hidden;
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}

.search-wrap:focus-within .search-bar {
  box-shadow: 0 10px 24px rgba(31, 35, 43, 0.16);
}

.search-bar input {
  min-width: 0;
  flex: 1;
  height: 100%;
  border: 0;
  outline: 0;
  padding: 0 22px;
  background: transparent;
  color: #20242d;
  font-size: 15px;
}

.search-bar button {
  height: 36px;
  margin-right: 4px;
  border: 0;
  border-radius: 999px;
  padding: 0 20px;
  background: #20242d;
  color: #ffe100;
  cursor: pointer;
  font-weight: 900;
}

.search-panel {
  position: absolute;
  top: 54px;
  left: 0;
  right: 0;
  max-height: min(72vh, 660px);
  overflow-y: auto;
  border: 1px solid rgba(32, 36, 45, 0.12);
  border-radius: 18px;
  padding: 10px;
  background: #fff;
  box-shadow: 0 22px 48px rgba(31, 35, 43, 0.22);
}

.search-panel-head {
  min-height: 38px;
  padding: 4px 6px 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid #eef0f3;
  color: #60656f;
  font-size: 13px;
}

.search-panel-head > span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-panel-actions {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.search-panel-head button {
  flex: 0 0 auto;
  border: 0;
  border-radius: 999px;
  padding: 7px 12px;
  background: #fff2a8;
  color: #20242d;
  cursor: pointer;
  font-weight: 800;
}

.search-loading {
  padding: 20px 10px;
  display: flex;
  align-items: center;
  gap: 9px;
  color: #60656f;
  font-weight: 700;
}

.loading-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #e95800;
  box-shadow: 14px 0 0 rgba(233, 88, 0, 0.42), 28px 0 0 rgba(233, 88, 0, 0.18);
}

.empty-search {
  margin: 12px 2px;
  padding: 18px;
  border-radius: 14px;
  background: #f7f8fa;
  color: #60656f;
  display: grid;
  gap: 6px;
}

.empty-search strong {
  color: #20242d;
  font-size: 15px;
}

.empty-search span {
  font-size: 13px;
}

.suggestion-section {
  padding: 10px 2px 2px;
}

.suggestion-section + .suggestion-section {
  margin-top: 6px;
  border-top: 1px solid #eef0f3;
}

.section-title {
  height: 30px;
  padding: 0 4px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #20242d;
}

.section-title span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 900;
}

.section-title span::before {
  content: "";
  width: 4px;
  height: 15px;
  border-radius: 999px;
  background: #e95800;
}

.section-title em {
  color: #8a8f99;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.suggestion-item {
  width: 100%;
  min-height: 78px;
  border: 0;
  border-radius: 14px;
  padding: 9px;
  display: grid;
  grid-template-columns: 62px minmax(0, 1fr);
  gap: 12px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.16s ease, transform 0.16s ease;
}

.suggestion-item:hover {
  background: #fff8d0;
  transform: translateY(-1px);
}

.suggestion-cover,
.suggestion-cover img,
.suggestion-placeholder {
  width: 62px;
  height: 62px;
  border-radius: 12px;
}

.suggestion-cover {
  position: relative;
  display: block;
  overflow: hidden;
  background: #f2f4f6;
}

.suggestion-cover img {
  display: block;
  object-fit: cover;
  border: 1px solid #edf0f3;
}

.suggestion-placeholder {
  display: grid;
  place-items: center;
  background: #f2f4f6;
  color: #8a8f99;
  font-size: 13px;
  font-weight: 900;
}

.suggestion-placeholder.secondhand {
  background: #eef7f1;
  color: #238451;
}

.suggestion-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.suggestion-name {
  color: #20242d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 800;
}

.suggestion-meta {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.suggestion-meta strong {
  color: #e95800;
  font-size: 17px;
  font-weight: 950;
}

.suggestion-meta em {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 4px 8px;
  background: #f2f4f6;
  color: #60656f;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.hot-section .section-title span::before {
  background: #20242d;
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
    font-size: 18px;
  }

  .search-wrap {
    grid-column: 1 / -1;
    order: 3;
  }

  .search-panel {
    top: 50px;
  }

  .search-panel-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .search-panel-actions {
    width: 100%;
    flex-wrap: wrap;
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
