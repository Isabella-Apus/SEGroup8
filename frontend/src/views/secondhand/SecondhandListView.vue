<template>
  <section class="market-page">
    <div class="market-hero secondhand-hero">
      <div>
        <span class="eyebrow">个人闲置</span>
        <h1>二手市场</h1>
        <p>从学习设备到生活用品，低价淘闲置，也可以快速发布自己的物品。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="router.push('/secondhand/publish')">发布闲置</el-button>
        <el-button size="large" @click="router.push('/order')">我的订单</el-button>
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
        :key="chip.condition"
        class="category-chip"
        :class="{ active: query.condition === chip.condition }"
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
  condition: "all",
  category: "全部分类",
});

const categories = ["全部分类", "数码闲置", "服饰鞋包", "教材书籍", "宿舍生活", "运动器材"];

const chips = [
  { label: "全部闲置", condition: "all" },
  { label: "95 新以上", condition: "95%" },
  { label: "9 成新", condition: "90%" },
  { label: "8 成新", condition: "80%" },
];

const visibleItems = computed(() => {
  return items.value.filter((item) => {
    const hitCondition = query.condition === "all"
      || item.condition === query.condition
      || item.conditionLevel === query.condition;
    const hitCategory = query.category === "全部分类" || item.categoryName === query.category || item.category === query.category;
    return hitCondition && hitCategory;
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
    path: "/secondhand",
    query: {
      ...(query.keyword.trim() ? { keyword: query.keyword.trim() } : {}),
      ...(query.category !== "全部分类" ? { category: query.category } : {}),
    },
  });
  await fetchPage(true);
}

async function onReset() {
  query.keyword = "";
  query.condition = "all";
  query.category = "全部分类";
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
      ...(category !== "全部分类" ? { category } : {}),
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
      keyword: query.keyword.trim() || undefined,
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
    linear-gradient(90deg, rgba(247, 239, 229, 0.96), rgba(183, 216, 238, 0.62)),
    url("https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=1400&q=80");
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
