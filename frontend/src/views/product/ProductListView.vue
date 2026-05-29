<template>
  <section class="market-page">
    <div class="market-hero product-hero">
      <div>
        <span class="eyebrow">官方商品</span>
        <h1>商品市场</h1>
        <p>浏览正在出售的新品商品，按分类、价格快速筛选。</p>
      </div>
    </div>

    <div class="market-servicebar">
      <button type="button" class="service-tile" @click="router.push('/cart')">
        <span class="tile-icon"><el-icon><ShoppingCart /></el-icon></span>
        <span class="tile-label">购物车</span>
      </button>
      <button type="button" class="service-tile" @click="router.push('/order')">
        <span class="tile-icon"><el-icon><Document /></el-icon></span>
        <span class="tile-label">我的订单</span>
      </button>
      <button type="button" class="service-tile" @click="router.push('/product/coupons')">
        <span class="tile-icon"><el-icon><Ticket /></el-icon></span>
        <span class="tile-label">领券中心</span>
      </button>
      <button type="button" class="service-tile" @click="router.push('/product/after-sale')">
        <span class="tile-icon"><el-icon><Service /></el-icon></span>
        <span class="tile-label">售后服务</span>
      </button>
      <button type="button" class="service-tile" @click="router.push(sellerEntryPath)">
        <span class="tile-icon"><el-icon><Shop /></el-icon></span>
        <span class="tile-label">{{ sellerEntryLabel }}</span>
      </button>
    </div>

    <div class="market-toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索商品名、描述"
        clearable
        @keyup.enter="onSearch"
      />
      <el-select v-model="query.priceRange" placeholder="全部商品" @change="onSearch">
        <el-option v-for="chip in chips" :key="chip.range" :label="chip.label" :value="chip.range" />
      </el-select>
      <el-select v-model="query.category" placeholder="全部分类" @change="onSearch">
        <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
      </el-select>
      <el-button type="primary" @click="onSearch">搜索</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <div class="result-strip">
      <strong>猜你喜欢</strong>
      <span>{{ visibleItems.length }} 件商品正在展示</span>
    </div>

    <div v-if="visibleItems.length" class="grid">
      <ProductCard
        v-for="item in visibleItems"
        :key="item.id"
        :product="item"
        mode="product"
        route-base="/product"
      />
    </div>

    <el-empty v-else-if="!loading" description="暂无商品" />

    <div ref="sentinel" class="sentinel">
      <span v-if="loading">加载中...</span>
      <span v-else-if="!hasMore && visibleItems.length">已经到底了</span>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import ProductCard from "@/components/ProductCard.vue";
import { getProductListApi } from "@/api/product";
import { useUserStore } from "@/stores/user";
import { ALL_CATEGORY, matchProductCategory, productCategories } from "@/utils/categoryRules";
import { searchList } from "@/utils/search/searchService";
import {
  Document,
  Service,
  Shop,
  ShoppingCart,
  Ticket,
} from "@element-plus/icons-vue";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const pageSize = 100;
const loading = ref(false);
const sentinel = ref(null);
const queryPageNum = ref(1);
let observer = null;
const items = ref([]);
const total = ref(0);

const query = reactive({
  keyword: "",
  priceRange: "all",
  category: ALL_CATEGORY,
});

const categories = productCategories;

const chips = [
  { label: "全部商品", range: "all" },
  { label: "100 元以下", range: "low" },
  { label: "100-500 元", range: "mid" },
  { label: "500 元以上", range: "high" },
];
const sellerEntryLabel = computed(() => (userStore.currentRole === "OFFICIAL_SELLER" ? "卖家工作台" : "申请开店"));
const sellerEntryPath = computed(() => (userStore.currentRole === "OFFICIAL_SELLER" ? "/merchant" : "/product/merchant-apply"));

const searchedItems = computed(() => searchList({
  items: items.value,
  keyword: query.keyword,
  keys: ["name", "description", "categoryName", "category"],
  options: { threshold: 0.42 },
}));

const visibleItems = computed(() => {
  return searchedItems.value.filter((item) => {
    const price = Number(item.price || 0);
    let hitPrice = true;
    if (query.priceRange === "low") hitPrice = price < 100;
    if (query.priceRange === "mid") hitPrice = price >= 100 && price <= 500;
    if (query.priceRange === "high") hitPrice = price > 500;
    const hitCategory = matchProductCategory(item, query.category);
    return hitPrice && hitCategory;
  });
});

const hasMore = computed(() => items.value.length < total.value);

onMounted(async () => {
  syncKeywordFromRoute();
  await fetchPage(true);
  initObserver();
});

watch(
  () => [route.query.keyword, route.query.category, route.query.price],
  async () => {
    syncKeywordFromRoute();
    await fetchPage(true);
  },
);

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect();
  }
});

async function onSearch() {
  await router.replace({
    path: "/product",
    query: {
      ...(query.keyword.trim() ? { keyword: query.keyword.trim() } : {}),
      ...(query.category !== ALL_CATEGORY ? { category: query.category } : {}),
      ...(query.priceRange !== "all" ? { price: query.priceRange } : {}),
    },
  });
  await fetchPage(true);
}

async function onReset() {
  query.keyword = "";
  query.priceRange = "all";
  query.category = ALL_CATEGORY;
  await router.replace("/product");
  await fetchPage(true);
}

async function applyChip(chip) {
  query.priceRange = chip.range;
  await router.replace({
    path: "/product",
    query: {
      ...(query.keyword.trim() ? { keyword: query.keyword.trim() } : {}),
      ...(query.category !== ALL_CATEGORY ? { category: query.category } : {}),
      ...(chip.range !== "all" ? { price: chip.range } : {}),
    },
  });
  await fetchPage(true);
}

async function applyCategory(category) {
  query.category = category;
  await router.replace({
    path: "/product",
    query: {
      ...(query.keyword.trim() ? { keyword: query.keyword.trim() } : {}),
      ...(category !== ALL_CATEGORY ? { category } : {}),
      ...(query.priceRange !== "all" ? { price: query.priceRange } : {}),
    },
  });
  await fetchPage(true);
}

async function fetchPage(reset = false) {
  if (loading.value) return;
  if (!reset && !hasMore.value) return;
  loading.value = true;
  try {
    if (reset) {
      queryPageNum.value = 1;
      items.value = [];
      total.value = 0;
    }
    const res = await getProductListApi({
      pageNum: queryPageNum.value,
      pageSize,
      ...(query.category !== ALL_CATEGORY ? { category: query.category } : {}),
      ...priceParams(),
    });
    const records = res.data?.records || [];
    total.value = Number(res.data?.total || records.length || 0);
    items.value = reset ? records : items.value.concat(records);
    queryPageNum.value += 1;
  } catch {
    if (reset) {
      items.value = [];
      total.value = 0;
    }
  } finally {
    loading.value = false;
  }
}

function syncKeywordFromRoute() {
  query.keyword = String(route.query.keyword || "");
  const nextCategory = String(route.query.category || ALL_CATEGORY);
  query.category = categories.includes(nextCategory) ? nextCategory : ALL_CATEGORY;
  const nextPrice = String(route.query.price || "all");
  query.priceRange = chips.some((item) => item.range === nextPrice) ? nextPrice : "all";
}

function priceParams() {
  if (query.priceRange === "low") {
    return { maxPrice: 99.99 };
  }
  if (query.priceRange === "mid") {
    return { minPrice: 100, maxPrice: 500 };
  }
  if (query.priceRange === "high") {
    return { minPrice: 500.01 };
  }
  return {};
}

function initObserver() {
  observer = new IntersectionObserver(
    (entries) => {
      const [entry] = entries;
      if (entry.isIntersecting) {
        fetchPage(false);
      }
    },
    { root: null, rootMargin: "280px 0px", threshold: 0 },
  );
  if (sentinel.value) {
    observer.observe(sentinel.value);
  }
}
</script>

<style scoped>
.market-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.market-hero {
  min-height: 188px;
  border: 1px solid rgba(60, 146, 255, 0.24);
  border-radius: 8px;
  padding: 24px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  background:
    linear-gradient(90deg, rgba(234, 244, 255, 0.94), rgba(233, 255, 248, 0.82), rgba(255, 247, 251, 0.62)),
    url("https://images.unsplash.com/photo-1472851294608-062f824d29cc?auto=format&fit=crop&w=1400&q=80");
  background-position: center;
  background-size: cover;
  color: var(--text-main);
  overflow: hidden;
}

.eyebrow {
  display: inline-flex;
  background: rgba(255, 255, 255, 0.92);
  border-radius: 999px;
  padding: 5px 12px;
  font-weight: 900;
  color: var(--text-main);
}

.market-hero h1 {
  margin: 12px 0 8px;
  font-size: clamp(34px, 5vw, 52px);
  line-height: 1;
}

.market-hero p {
  max-width: 520px;
  margin: 0;
  color: var(--text-secondary);
  font-weight: 700;
  line-height: 1.7;
}

.hero-deal {
  margin-left: auto;
  width: 190px;
  border: 1px solid rgba(137, 199, 255, 0.42);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  padding: 14px;
  backdrop-filter: blur(8px);
}

.hero-deal strong,
.hero-deal span {
  display: block;
}

.hero-deal strong {
  font-size: 18px;
}

.hero-deal span {
  margin-top: 7px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.market-servicebar {
  min-height: 112px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 12px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  box-shadow: var(--shadow-soft);
}

.service-tile {
  position: relative;
  min-width: 0;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
  color: var(--text-main);
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;
}

.service-tile {
  cursor: pointer;
}

.service-tile:hover {
  border-color: var(--brand-primary);
  background: var(--brand-primary-weak);
}

.tile-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #ffffff;
  color: var(--brand-primary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 16px rgba(137, 199, 255, 0.16);
}

.tile-icon :deep(.el-icon) {
  font-size: 24px;
}

.tile-label {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 800;
  line-height: 1.25;
}

.market-toolbar {
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(180px, 0.26fr) minmax(180px, 0.26fr) 88px 88px;
  gap: 16px;
  align-items: center;
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  background: #ffffff;
  padding: 16px 18px;
  box-shadow: var(--shadow-soft);
}

.market-toolbar > .el-input,
.market-toolbar > .el-select {
  width: 100%;
}

.market-toolbar :deep(.el-input__wrapper),
.market-toolbar :deep(.el-select__wrapper) {
  min-height: 46px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px var(--line-soft) inset;
}

.market-toolbar :deep(.el-input__wrapper.is-focus),
.market-toolbar :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--brand-primary) inset;
}

.market-toolbar :deep(.el-input__inner),
.market-toolbar :deep(.el-select__placeholder) {
  font-size: 16px;
  font-weight: 700;
}

.market-toolbar > .el-button {
  height: 46px;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 900;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow-x: auto;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 10px 12px;
}

.filter-row > span {
  flex: 0 0 auto;
  color: var(--text-secondary);
  font-weight: 900;
}

.category-chip {
  min-height: 32px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: #ffffff;
  padding: 0 13px;
  color: var(--text-main);
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
}

.category-chip.active,
.category-chip:hover {
  border-color: rgba(60, 146, 255, 0.35);
  background: var(--brand-primary-weak);
  color: var(--brand-primary);
}

.result-strip {
  min-height: 46px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 0 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.result-strip strong {
  font-size: 18px;
}

.result-strip span {
  color: var(--text-muted);
  font-size: 13px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.sentinel {
  text-align: center;
  padding: 24px 8px;
  color: var(--text-secondary);
}

@media (max-width: 980px) {
  .market-toolbar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .market-toolbar > .el-input {
    grid-column: 1 / -1;
  }

  .grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .market-hero {
    align-items: flex-start;
    flex-direction: column;
    padding: 18px;
  }

  .hero-deal {
    margin-left: 0;
    width: 100%;
  }

  .market-toolbar {
    grid-template-columns: 1fr;
  }

  .market-servicebar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}
</style>
