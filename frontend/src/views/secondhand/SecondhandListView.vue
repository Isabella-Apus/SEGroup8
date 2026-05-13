<template>
  <section class="feed-page">
    <div class="feed-head secondhand">
      <div>
        <p>二手闲置</p>
        <h1>淘同学手里的好物</h1>
      </div>
      <el-button type="warning" round @click="$router.push('/secondhand/publish')">去发布</el-button>
    </div>

    <el-form :inline="true" class="query" @submit.prevent>
      <el-form-item>
        <el-input
          v-model="query.keyword"
          placeholder="搜二手商品名，例如：自行车"
          clearable
          style="width: 280px"
          @keyup.enter="onSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" round @click="onSearch">搜索</el-button>
        <el-button round @click="onReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="chips">
      <button
        v-for="chip in chips"
        :key="chip.label"
        class="chip"
        :class="{ active: query.condition === chip.condition }"
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
        mode="secondhand"
        route-base="/secondhand"
      />
    </div>

    <div ref="sentinel" class="sentinel">
      <span v-if="loading">加载中...</span>
      <span v-else-if="!hasMore && visibleItems.length">已经到底了</span>
      <span v-else-if="!visibleItems.length">暂无二手商品</span>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import ProductCard from '@/components/ProductCard.vue';
import { getSecondhandListApi } from '@/api/secondhand';
import { searchList } from '@/utils/search';

const route = useRoute();
const pageSize = 16;
const loading = ref(false);
const sentinel = ref(null);
const queryPageNum = ref(1);
let observer = null;
const items = ref([]);
const total = ref(0);

const query = reactive({
  keyword: '',
  condition: 'all'
});

const chips = [
  { label: '全部', condition: 'all' },
  { label: '95 新以上', condition: '95%' },
  { label: '9 成新', condition: '90%' },
  { label: '8 成新', condition: '80%' }
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
    const hitCondition = query.condition === 'all'
      || item.condition === query.condition
      || item.conditionLevel === query.condition;
    return hitCondition;
  });
});

const visibleItems = computed(() => allItems.value);
const hasMore = computed(() => items.value.length < total.value);

onMounted(async () => {
  syncKeywordFromRoute();
  await fetchPage(true);
  if (query.keyword.trim()) {
    await ensureAllItemsLoaded();
  }
  initObserver();
});

watch(
  () => route.query.keyword,
  async () => {
    syncKeywordFromRoute();
    await fetchPage(true);
    if (query.keyword.trim()) {
      await ensureAllItemsLoaded();
    }
  }
);

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect();
  }
});

async function onSearch() {
  await fetchPage(true);
  await ensureAllItemsLoaded();
}

async function onReset() {
  query.keyword = '';
  query.condition = 'all';
  await fetchPage(true);
}

async function applyChip(chip) {
  query.condition = chip.condition;
  await fetchPage(true);
  if (query.keyword.trim()) {
    await ensureAllItemsLoaded();
  }
}

function syncKeywordFromRoute() {
  const value = route.query.keyword;
  query.keyword = Array.isArray(value) ? value[0] || '' : value || '';
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
    }
    const res = await getSecondhandListApi({
      pageNum: queryPageNum.value,
      pageSize,
      keyword: undefined,
    });
    const records = (res.data?.records || []).map((item) => ({
      ...item,
      condition: item.conditionLevel || item.condition,
      originPrice: item.originPrice ?? item.salePrice,
      salePrice: item.salePrice ?? item.price,
    }));
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
  observer = new IntersectionObserver(
    (entries) => {
      const [entry] = entries;
      if (entry.isIntersecting) {
        fetchPage(false);
      }
    },
    { root: null, rootMargin: '280px 0px', threshold: 0 }
  );
  if (sentinel.value) {
    observer.observe(sentinel.value);
  }
}
</script>

<style scoped>
.feed-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.feed-head,
.query {
  border: 1px solid #eeeeee;
  border-radius: 20px;
  background: #fff;
}

.feed-head {
  min-height: 128px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 26px;
}

.feed-head.secondhand {
  background:
    radial-gradient(circle at 88% 14%, rgba(255, 225, 0, 0.38), transparent 28%),
    #fff;
}

.feed-head p {
  margin: 0 0 4px;
  color: #8a8a8a;
}

.feed-head h1 {
  margin: 0;
  font-size: 30px;
  letter-spacing: 0;
}

.query {
  padding: 14px 14px 0;
}

.chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  border: 1px solid #eeeeee;
  border-radius: 999px;
  background: #fff;
  color: #444;
  padding: 7px 13px;
  cursor: pointer;
}

.chip:hover,
.chip.active {
  border-color: #ffe100;
  background: #fff7c2;
  font-weight: 700;
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
  .feed-head {
    min-height: auto;
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
  }

  .feed-head h1 {
    font-size: 23px;
  }

  .query :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
  }

  .query :deep(.el-input) {
    width: 100% !important;
  }

  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}
</style>
