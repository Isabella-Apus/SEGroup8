<template>
  <section class="feed-page">
    <div class="hero">
      <div>
        <h1>二手捡漏</h1>
      </div>
      <div class="hero-dot"></div>
    </div>

    <el-form :inline="true" class="query" @submit.prevent>
      <el-form-item>
        <el-input
          v-model="query.keyword"
          placeholder="搜二手商品名，例如：自行车"
          clearable
          style="width: 240px"
          @keyup.enter="onSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="onSearch">搜索</el-button>
        <el-button @click="onReset">重置</el-button>
        <el-button type="success" @click="$router.push('/secondhand/publish')">去发布</el-button>
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
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import ProductCard from '@/components/ProductCard.vue';
import { getSecondhandListApi } from '@/api/secondhand';
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
  condition: 'all'
});

const chips = [
  { label: '全部', condition: 'all' },
  { label: '95新以上', condition: '95%' },
  { label: '9成新', condition: '90%' },
  { label: '8成新', condition: '80%' }
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
  await fetchPage(true);
  initObserver();
});

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
  background: linear-gradient(120deg, #ff6f2f, #ff9822);
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
  border: 1px solid #ffd6b9;
  border-radius: 999px;
  background: #fff8f2;
  color: #7f4f2f;
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
