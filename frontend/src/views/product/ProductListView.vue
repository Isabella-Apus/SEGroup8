<template>
  <section class="feed-page">
    <div class="hero">
      <div>
        <h1>新品优选</h1>
      </div>
      <div class="hero-dot"></div>
    </div>

    <el-form :inline="true" class="query" @submit.prevent>
      <el-form-item>
        <el-input
          v-model="query.keyword"
          placeholder="搜商品名，例如：键盘"
          clearable
          style="width: 240px"
          @keyup.enter="onSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="chips">
      <button
        v-for="chip in chips"
        :key="chip.label"
        class="chip"
        type="button"
        @click="applyChip(chip)"
      >
        {{ chip.label }}
      </button>
    </div>

    <div class="grid">
      <ProductCard
        v-for="item in visibleItems"
        :key="item.id"
        :product="item"
        mode="product"
        route-base="/product"
      />
    </div>

    <div ref="sentinel" class="sentinel">
      <span v-if="loading">加载中...</span>
      <span v-else-if="!hasMore && visibleItems.length">已经到底了</span>
      <span v-else-if="!visibleItems.length">暂无商品</span>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import ProductCard from '@/components/ProductCard.vue';
import { getProductListApi } from '@/api/product';
import { searchList } from '@/utils/search';

const pageSize = 16;
const loading = ref(false);
const sentinel = ref(null);
const queryPageNum = ref(1);
let observer = null;
const items = ref([]);
const total = ref(0);

const query = reactive({
  keyword: '',
  priceRange: 'all'
});

const chips = [
  { label: '全部', range: 'all' },
  { label: '100 元以下', range: 'low' },
  { label: '100-500 元', range: 'mid' },
  { label: '500 元以上', range: 'high' }
];

const allItems = computed(() => {
  const keywordTrimmed = query.keyword.trim();
  const source = keywordTrimmed
    ? searchList({
      items: items.value,
      keyword: keywordTrimmed,
      keys: ['name', 'description'],
    })
    : items.value;
  return source.filter((item) => {
    const price = Number(item.price || 0);
    let hitPrice = true;
    if (query.priceRange === 'low') hitPrice = price < 100;
    if (query.priceRange === 'mid') hitPrice = price >= 100 && price <= 500;
    if (query.priceRange === 'high') hitPrice = price > 500;
    return hitPrice;
  });
});

const visibleItems = computed(() => allItems.value);
const hasMore = computed(() => items.value.length < total.value);

onMounted(async () => {
  await fetchPage(true);
  initObserver();
});

onBeforeUnmount(() => {
  if (observer) observer.disconnect();
});

async function onSearch() {
  await fetchPage(true);
  await ensureAllItemsLoaded();
}

async function onReset() {
  query.keyword = '';
  query.priceRange = 'all';
  await fetchPage(true);
}

async function applyChip(chip) {
  query.priceRange = chip.range;
  await fetchPage(true);
  if (query.keyword.trim()) {
    await ensureAllItemsLoaded();
  }
}

async function fetchPage(reset = false) {
  if (loading.value) return;
  if (!reset && !hasMore.value) return;
  loading.value = true;
  try {
    if (reset) queryPageNum.value = 1;
    const params = {
      pageNum: queryPageNum.value,
      pageSize,
      keyword: undefined
    };
    const res = await getProductListApi(params);
    const records = res.data?.records || [];
    total.value = Number(res.data?.total || 0);
    if (reset) {
      items.value = records;
    } else {
      items.value = items.value.concat(records);
    }
    queryPageNum.value += 1;
  } finally {
    loading.value = false;
  }
}

async function ensureAllItemsLoaded() {
  while (hasMore.value) {
    await fetchPage(false);
  }
}

function initObserver() {
  observer = new IntersectionObserver((entries) => {
    const [entry] = entries;
    if (entry.isIntersecting) fetchPage(false);
  }, { root: null, rootMargin: '280px 0px', threshold: 0 });
  if (sentinel.value) observer.observe(sentinel.value);
}
</script>

<style scoped>
.feed-page {
  padding: 8px 10px 20px;
}

.hero {
  border-radius: 22px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  margin-bottom: 14px;
  background: linear-gradient(120deg, #3b82f6, #6366f1);
}

.hero h1 {
  margin: 0;
  font-size: 30px;
}

.hero p {
  margin: 8px 0 0;
  opacity: .92;
}

.hero-dot {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  background: radial-gradient(circle at 25% 25%, #fff5, #fff1 60%, transparent 70%);
}

.query {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 12px;
}

.chips {
  margin: 10px 0 14px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  border: 1px solid #c7d2fe;
  border-radius: 999px;
  background: #eef2ff;
  color: #374151;
  padding: 6px 12px;
  cursor: pointer;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.sentinel {
  text-align: center;
  padding: 24px 8px;
  color: #80848d;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .feed-page {
    padding: 6px;
  }

  .hero {
    padding: 14px;
    border-radius: 16px;
  }

  .hero h1 {
    font-size: 24px;
  }

  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}
</style>
