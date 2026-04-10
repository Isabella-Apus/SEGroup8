<template>
  <section class="feed-page">
    <div class="hero">
      <div>
        <h1>猜你喜欢</h1>
        <p>发现每日上新，沉浸式浏览无限滚动商品流</p>
      </div>
      <div class="hero-dot"></div>
    </div>

    <el-form :inline="true" class="query" @submit.prevent>
      <el-form-item>
        <el-input
          v-model="query.keyword"
          placeholder="搜商品名，例如：无线耳机"
          clearable
          style="width: 240px"
          @keyup.enter="onSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-input-number v-model="query.minPrice" :min="0" :precision="2" :step="10" placeholder="最低价" />
      </el-form-item>
      <el-form-item>
        <el-input-number v-model="query.maxPrice" :min="0" :precision="2" :step="10" placeholder="最高价" />
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
      <ProductCard v-for="item in items" :key="item.id" :product="item" />
    </div>

    <div ref="sentinel" class="sentinel">
      <span v-if="loading">加载中...</span>
      <span v-else-if="!hasMore && items.length">已经到底了</span>
      <span v-else-if="!items.length">暂无商品</span>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import ProductCard from '@/components/ProductCard.vue';
import { getProductListApi } from '@/api/product';

const pageSize = 20;
const loading = ref(false);
const items = ref([]);
const total = ref(0);
const sentinel = ref(null);
let observer = null;

const query = reactive({
  pageNum: 1,
  pageSize,
  keyword: '',
  minPrice: undefined,
  maxPrice: undefined
});

const chips = [
  { label: '全部', minPrice: undefined, maxPrice: undefined },
  { label: '百元好物', minPrice: 0, maxPrice: 100 },
  { label: '200-500', minPrice: 200, maxPrice: 500 },
  { label: '千元以上', minPrice: 1000, maxPrice: undefined }
];

const hasMore = computed(() => items.value.length < total.value || total.value === 0);

onMounted(async () => {
  await fetchPage(true);
  initObserver();
});

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect();
  }
});

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
      query.pageNum = 1;
    }
    const result = await getProductListApi(query);
    const records = result.data?.records || [];
    total.value = Number(result.data?.total || 0);
    if (reset) {
      items.value = records;
    } else {
      items.value = items.value.concat(records);
    }
    query.pageNum += 1;
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  fetchPage(true);
}

function onReset() {
  query.keyword = '';
  query.minPrice = undefined;
  query.maxPrice = undefined;
  fetchPage(true);
}

function applyChip(chip) {
  query.minPrice = chip.minPrice;
  query.maxPrice = chip.maxPrice;
  fetchPage(true);
}

function initObserver() {
  observer = new IntersectionObserver(
    (entries) => {
      const [entry] = entries;
      if (entry.isIntersecting) {
        fetchPage(false);
      }
    },
    {
      root: null,
      rootMargin: '280px 0px',
      threshold: 0
    }
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
  animation: fadeSlide .6s ease;
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

@keyframes fadeSlide {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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
