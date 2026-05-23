<template>
  <section class="market-page">
    <div class="market-hero secondhand-hero">
      <div>
        <span class="eyebrow">个人闲置</span>
        <h1>二手市场</h1>
        <p>从学习设备到生活用品，低价淘闲置，也可以快速发布自己的物品。</p>
      </div>
      <div class="hero-deal">
        <strong>闲置瀑布流</strong>
        <span>先看价格和成色，再进详情沟通</span>
      </div>
    </div>

    <div class="market-servicebar">
      <strong>二手商城</strong>
      <div class="service-actions">
        <el-button type="primary" @click="router.push('/secondhand/publish')">发布闲置</el-button>
        <el-button @click="router.push('/secondhand/mine')">我的闲置/拍卖</el-button>
        <el-button @click="router.push('/messages')">议价消息</el-button>
        <el-button @click="router.push('/secondhand/cart')">购物车</el-button>
        <el-button @click="router.push('/secondhand/orders')">订单</el-button>
        <el-dropdown trigger="click" @command="handleServiceCommand">
          <el-button>更多</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="mine">我的闲置</el-dropdown-item>
              <el-dropdown-item command="messages">买家消息</el-dropdown-item>
              <el-dropdown-item command="coupons">领券中心</el-dropdown-item>
              <el-dropdown-item command="afterSale">售后</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="market-toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索二手商品、描述"
        clearable
        @keyup.enter="onSearch"
      />
      <el-select v-model="query.condition" placeholder="成色" @change="onSearch">
        <el-option v-for="chip in chips" :key="chip.condition" :label="chip.label" :value="chip.condition" />
      </el-select>
      <el-select v-model="query.category" placeholder="闲置分类" @change="onSearch">
        <el-option v-for="item in categories" :key="item" :label="item" :value="item" />
      </el-select>
      <el-button type="primary" @click="onSearch">搜索</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <div class="filter-row">
      <span>分类</span>
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

    <div class="filter-row">
      <span>成色</span>
      <button
        v-for="chip in chips"
        :key="chip.condition"
        class="category-chip"
        :class="{ active: query.condition === chip.condition }"
        type="button"
        @click="applyChip(chip)"
      >
        {{ chip.label }}
      </button>
    </div>

    <div class="result-strip">
      <strong>新鲜上架</strong>
      <span>{{ visibleItems.length }} 件闲置正在展示</span>
    </div>

    <div v-if="visibleItems.length" class="grid">
      <ProductCard
        v-for="item in visibleItems"
        :key="item.id"
        :product="item"
        mode="secondhand"
        route-base="/secondhand"
      />
    </div>

    <el-empty v-else-if="!loading" description="暂无二手商品" />

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
import { getSecondhandListApi } from "@/api/secondhand";
import { ALL_CATEGORY, matchSecondhandCategory, secondhandCategories } from "@/utils/categoryRules";
import { searchList } from "@/utils/search/searchService";

const route = useRoute();
const router = useRouter();
const pageSize = 100;
const loading = ref(false);
const sentinel = ref(null);
const queryPageNum = ref(1);
let observer = null;
const items = ref([]);
const total = ref(0);

const query = reactive({
  keyword: "",
  condition: "all",
  category: ALL_CATEGORY,
  sort: "",
});

const categories = secondhandCategories;

const chips = [
  { label: "全部闲置", condition: "all" },
  { label: "95 新以上", condition: "95%" },
  { label: "9 成新", condition: "90%" },
  { label: "8 成新", condition: "80%" },
];

const searchedItems = computed(() => searchList({
  items: items.value,
  keyword: query.keyword,
  keys: ["name", "description", "categoryName", "category"],
  options: { threshold: 0.42 },
}));

const visibleItems = computed(() => {
  const filtered = searchedItems.value.filter((item) => {
    const hitCondition = query.condition === "all"
      || item.condition === query.condition
      || item.conditionLevel === query.condition;
    const hitCategory = matchSecondhandCategory(item, query.category);
    return hitCondition && hitCategory;
  });
  if (query.sort === "price-asc") {
    return [...filtered].sort((a, b) => Number(a.salePrice || a.price || 0) - Number(b.salePrice || b.price || 0));
  }
  return filtered;
});

const hasMore = computed(() => items.value.length < total.value);

onMounted(async () => {
  syncKeywordFromRoute();
  await fetchPage(true);
  initObserver();
});

watch(
  () => [route.query.keyword, route.query.category, route.query.condition, route.query.sort],
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
    path: "/secondhand",
    query: {
      ...(query.keyword.trim() ? { keyword: query.keyword.trim() } : {}),
      ...(query.category !== ALL_CATEGORY ? { category: query.category } : {}),
    },
  });
  await fetchPage(true);
}

async function onReset() {
  query.keyword = "";
  query.condition = "all";
  query.category = ALL_CATEGORY;
  await router.replace("/secondhand");
  await fetchPage(true);
}

async function applyChip(chip) {
  query.condition = chip.condition;
  await fetchPage(true);
}

async function applyCategory(category) {
  query.category = category;
  await router.replace({
    path: "/secondhand",
    query: {
      ...(query.keyword.trim() ? { keyword: query.keyword.trim() } : {}),
      ...(category !== ALL_CATEGORY ? { category } : {}),
    },
  });
  await fetchPage(true);
}

async function fetchPage(reset = false) {
  if (loading.value) {
    return;
  }
  if (!reset && !hasMore.value) {
    return;
  }
  loading.value = true;
  try {
    if (reset) {
      queryPageNum.value = 1;
      items.value = [];
      total.value = 0;
    }
    const res = await getSecondhandListApi({
      pageNum: queryPageNum.value,
      pageSize,
      ...(query.category !== ALL_CATEGORY ? { category: query.category } : {}),
      ...(query.condition !== "all" ? { conditionLevel: query.condition } : {}),
    });
    const records = (res.data?.records || []).map((item) => ({
      ...item,
      condition: item.conditionLevel || item.condition,
      originPrice: item.originPrice ?? item.salePrice,
      salePrice: item.salePrice ?? item.price,
    }));
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
  const nextCondition = String(route.query.condition || "all");
  query.condition = chips.some((chip) => chip.condition === nextCondition) ? nextCondition : "all";
  query.sort = String(route.query.sort || "");
}

function handleServiceCommand(command) {
  const map = {
    mine: "/secondhand/mine",
    messages: "/messages",
    coupons: "/secondhand/coupons",
    afterSale: "/secondhand/after-sale",
  };
  router.push(map[command] || "/secondhand");
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
  border: 1px solid rgba(53, 216, 171, 0.24);
  border-radius: 8px;
  padding: 24px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  background:
    linear-gradient(90deg, rgba(233, 255, 248, 0.94), rgba(234, 244, 255, 0.82), rgba(255, 247, 251, 0.62)),
    url("https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=1400&q=80");
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
  min-height: 58px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  box-shadow: var(--shadow-soft);
}

.market-servicebar strong {
  font-size: 17px;
}

.service-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.market-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px 160px auto auto;
  gap: 10px;
  align-items: center;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 12px;
  box-shadow: var(--shadow-soft);
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
  border-color: rgba(18, 165, 148, 0.35);
  background: #e9fbf8;
  color: var(--brand-accent-strong);
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
    align-items: flex-start;
    flex-direction: column;
  }

  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}
</style>
