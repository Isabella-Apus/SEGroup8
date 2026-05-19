<template>
  <section class="market-page">
    <div class="market-hero product-hero">
      <div>
        <span class="eyebrow">官方商品</span>
        <h1>商品市场</h1>
        <p>按分类和预算挑选一手商品，适合直接下单购买。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="router.push('/order')">我的订单</el-button>
      </div>
    </div>

    <div class="market-toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索商品名、描述"
        clearable
        @keyup.enter="onSearch"
      />
      <el-select v-model="query.priceRange" placeholder="价格区间" @change="onSearch">
        <el-option v-for="chip in chips" :key="chip.range" :label="chip.label" :value="chip.range" />
      </el-select>
      <el-select v-model="query.category" placeholder="商品分类" @change="onSearch">
        <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
      </el-select>
      <el-button type="primary" @click="onSearch">搜索</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <div class="category-row">
      <button
        v-for="item in categories"
        :key="item"
        class="category-chip"
        :class="{ active: query.category === item }"
        type="button"
        @click="applyCategory(item)"
      >
        {{ item }}
      </button>
    </div>

    <div class="category-row">
      <button
        v-for="chip in chips"
        :key="chip.range"
        class="category-chip"
        :class="{ active: query.priceRange === chip.range }"
        type="button"
        @click="applyChip(chip)"
      >
        {{ chip.label }}
      </button>
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

const route = useRoute();
const router = useRouter();
const pageSize = 16;
const loading = ref(false);
const sentinel = ref(null);
const queryPageNum = ref(1);
let observer = null;
const items = ref([]);
const total = ref(0);

const query = reactive({
  keyword: "",
  priceRange: "all",
  category: "全部分类",
});

const categories = ["全部分类", "电子数码", "服装鞋包", "学习办公", "生活百货", "运动户外"];

const chips = [
  { label: "全部商品", range: "all" },
  { label: "100 元以下", range: "low" },
  { label: "100-500 元", range: "mid" },
  { label: "500 元以上", range: "high" },
];

const visibleItems = computed(() => {
  return items.value.filter((item) => {
    const price = Number(item.price || 0);
    let hitPrice = true;
    if (query.priceRange === "low") hitPrice = price < 100;
    if (query.priceRange === "mid") hitPrice = price >= 100 && price <= 500;
    if (query.priceRange === "high") hitPrice = price > 500;
    const hitCategory = query.category === "全部分类" || item.categoryName === query.category || item.category === query.category;
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
  () => [route.query.keyword, route.query.category],
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
      ...(query.category !== "全部分类" ? { category: query.category } : {}),
    },
  });
  await fetchPage(true);
}

async function onReset() {
  query.keyword = "";
  query.priceRange = "all";
  query.category = "全部分类";
  await router.replace("/product");
  await fetchPage(true);
}

async function applyChip(chip) {
  query.priceRange = chip.range;
  await fetchPage(true);
}

async function applyCategory(category) {
  query.category = category;
  await router.replace({
    path: "/product",
    query: {
      ...(query.keyword.trim() ? { keyword: query.keyword.trim() } : {}),
      ...(category !== "全部分类" ? { category } : {}),
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
      keyword: query.keyword.trim() || undefined,
    });
    const records = res.data?.records || [];
    total.value = Number(res.data?.total || records.length || 0);
    items.value = reset ? records : items.value.concat(records);
    queryPageNum.value += 1;
  } finally {
    loading.value = false;
  }
}

function syncKeywordFromRoute() {
  query.keyword = String(route.query.keyword || "");
  query.category = String(route.query.category || "全部分类");
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
  gap: 16px;
}

.market-hero {
  min-height: 210px;
  border: 2px solid var(--brand-primary);
  border-radius: 26px;
  padding: 28px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  background:
    linear-gradient(90deg, rgba(220, 239, 233, 0.96), rgba(241, 240, 251, 0.72), rgba(255, 255, 255, 0.38)),
    url("https://images.unsplash.com/photo-1472851294608-062f824d29cc?auto=format&fit=crop&w=1400&q=80");
  background-position: center;
  background-size: cover;
}

.eyebrow {
  display: inline-flex;
  background: #ffffff;
  border-radius: 999px;
  padding: 5px 12px;
  font-weight: 900;
}

.market-hero h1 {
  margin: 12px 0 8px;
  font-size: clamp(34px, 5vw, 52px);
  line-height: 1;
}

.market-hero p {
  max-width: 520px;
  margin: 0;
  color: #3d382b;
  font-weight: 700;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.market-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px 160px auto auto;
  gap: 10px;
  align-items: center;
  border: 1px solid var(--line-soft);
  border-radius: 20px;
  background: #ffffff;
  padding: 12px;
  box-shadow: var(--shadow-soft);
}

.category-row {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.category-chip {
  min-height: 38px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: #ffffff;
  padding: 0 16px;
  color: var(--text-main);
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
}

.category-chip.active,
.category-chip:hover {
  border-color: var(--brand-primary);
  background: var(--brand-accent);
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.sentinel {
  text-align: center;
  padding: 24px 8px;
  color: var(--text-secondary);
}

@media (max-width: 980px) {
  .market-toolbar {
    grid-template-columns: 1fr 160px;
  }

  .grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .market-hero {
    align-items: flex-start;
    flex-direction: column;
    border-radius: 20px;
    padding: 18px;
  }

  .market-toolbar {
    grid-template-columns: 1fr;
  }

  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}
</style>
