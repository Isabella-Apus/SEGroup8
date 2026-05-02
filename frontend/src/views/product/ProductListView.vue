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
        <div class="search-box" @focusin="openSuggest" @focusout="onSearchBlur">
          <el-input
            v-model="query.keyword"
            placeholder="搜商品名，例如：键盘"
            clearable
            style="width: 260px"
            @keyup.enter="onSearch"
          />
          <div v-if="suggestVisible" class="suggest-layer">
            <div class="suggest-col">
              <div class="suggest-title">搜索历史</div>
              <div class="tag-wrap">
                <el-tag
                  v-for="item in searchHistory"
                  :key="`h-${item}`"
                  class="suggest-tag"
                  @click="applyKeyword(item)"
                >
                  {{ item }}
                </el-tag>
                <span v-if="!searchHistory.length" class="empty-tips">暂无历史</span>
              </div>
            </div>
            <div class="suggest-col">
              <div class="suggest-title">热门搜索</div>
              <div class="hot-wrap">
                <button
                  v-for="item in hotKeywords"
                  :key="`hot-${item.keyword}`"
                  type="button"
                  class="hot-item"
                  @click="applyKeyword(item.keyword)"
                >
                  <span class="rank">{{ item.rank }}</span>
                  <span class="word">{{ item.keyword }}</span>
                  <span class="score">{{ item.score }}</span>
                </button>
                <span v-if="!hotKeywords.length" class="empty-tips">暂无热搜</span>
              </div>
            </div>
          </div>
        </div>
      </el-form-item>
      <el-form-item>
        <el-cascader
          v-model="query.categoryPath"
          :options="CATEGORY_TREE"
          :props="cascaderProps"
          clearable
          filterable
          collapse-tags
          collapse-tags-tooltip
          placeholder="选择分类"
          style="width: 260px"
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

    <div class="sort-row">
      <el-button
        v-for="item in sortOptions"
        :key="item.value"
        :type="query.sortBy === item.value ? 'warning' : 'default'"
        plain
        @click="changeSort(item.value)"
      >
        {{ item.label }}
      </el-button>
    </div>

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

    <div v-if="searchedKeyword" class="result-head">
      找到关于 “{{ searchedKeyword }}” 的相关商品共 {{ total }} 件
    </div>

    <div class="grid">
      <ProductCard
        v-for="item in visibleItems"
        :key="item.id"
        :product="item"
        mode="product"
        route-base="/product"
        :highlight-keyword="searchedKeyword"
      />
    </div>

    <div v-if="showEmptyState" class="empty-box">
      <div class="empty-illust">
        <span class="magnifier"></span>
      </div>
      <div class="empty-main">没有找到符合条件的商品</div>
      <div class="empty-sub">试试更短关键词或放宽筛选条件，已为你推荐热门商品</div>
      <div class="grid recommend-grid" v-if="recommendItems.length">
        <ProductCard
          v-for="item in recommendItems"
          :key="`r-${item.id}`"
          :product="item"
          mode="product"
          route-base="/product"
        />
      </div>
    </div>

    <div ref="sentinel" class="sentinel">
      <span v-if="loading">加载中...</span>
      <span v-else-if="!hasMore && visibleItems.length">已经到底了</span>
      <span v-else-if="!visibleItems.length && !showEmptyState">暂无商品</span>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import ProductCard from '@/components/ProductCard.vue';
import { getProductListApi } from '@/api/product';
import { getHotSearchApi, getSearchHistoryApi } from '@/api/search';
import { CATEGORY_TREE } from '@/constants/categories';

const pageSize = 16;
const route = useRoute();
const loading = ref(false);
const sentinel = ref(null);
const queryPageNum = ref(1);
let observer = null;
const items = ref([]);
const total = ref(0);
const searchedKeyword = ref('');
const recommendItems = ref([]);
const searchHistory = ref([]);
const hotKeywords = ref([]);
const suggestVisible = ref(false);
let blurTimer = null;

const query = reactive({
  keyword: '',
  categoryPath: [],
  sortBy: 'time_desc',
  minPrice: undefined,
  maxPrice: undefined,
  priceRange: 'all',
});

const cascaderProps = {
  emitPath: true,
  checkStrictly: false,
  value: 'value',
  label: 'label',
  children: 'children',
};

const sortOptions = [
  { label: '最新发布', value: 'time_desc' },
  { label: '销量优先', value: 'sales_desc' },
  { label: '价格升序', value: 'price_asc' },
  { label: '价格降序', value: 'price_desc' },
];

const chips = [
  { label: '全部', range: 'all' },
  { label: '100 元以下', range: 'low' },
  { label: '100-500 元', range: 'mid' },
  { label: '500 元以上', range: 'high' }
];

const visibleItems = computed(() => items.value);
const hasMore = computed(() => items.value.length < total.value);
const showEmptyState = computed(() => !loading.value && !items.value.length && isFilterActive.value);
const isFilterActive = computed(() => {
  return !!searchedKeyword.value
    || query.categoryPath.length > 0
    || query.minPrice != null
    || query.maxPrice != null
    || query.priceRange !== 'all';
});
const selectedCategoryId = computed(() => {
  if (!query.categoryPath.length) {
    return undefined;
  }
  return query.categoryPath[1] || query.categoryPath[0];
});

onMounted(async () => {
  const presetCategoryId = Number(route.query.categoryId || 0);
  if (presetCategoryId > 0) {
    query.categoryPath = [presetCategoryId];
  }
  await fetchPage(true);
  initObserver();
});

onBeforeUnmount(() => {
  if (observer) observer.disconnect();
  if (blurTimer) {
    clearTimeout(blurTimer);
  }
});

async function onSearch() {
  searchedKeyword.value = query.keyword.trim();
  closeSuggest();
  await fetchPage(true);
}

async function onReset() {
  query.keyword = '';
  query.categoryPath = [];
  query.sortBy = 'time_desc';
  query.minPrice = undefined;
  query.maxPrice = undefined;
  query.priceRange = 'all';
  searchedKeyword.value = '';
  recommendItems.value = [];
  await fetchPage(true);
}

async function applyChip(chip) {
  query.priceRange = chip.range;
  if (chip.range === 'all') {
    query.minPrice = undefined;
    query.maxPrice = undefined;
  }
  if (chip.range === 'low') {
    query.minPrice = 0;
    query.maxPrice = 100;
  }
  if (chip.range === 'mid') {
    query.minPrice = 100;
    query.maxPrice = 500;
  }
  if (chip.range === 'high') {
    query.minPrice = 500;
    query.maxPrice = undefined;
  }
  await fetchPage(true);
}

async function fetchPage(reset = false) {
  if (loading.value) return;
  if (!reset && !hasMore.value) return;
  loading.value = true;
  try {
    if (reset) queryPageNum.value = 1;
    const keyword = query.keyword.trim();
    const params = {
      pageNum: queryPageNum.value,
      pageSize,
      keyword: keyword || undefined,
      categoryId: selectedCategoryId.value,
      sortBy: query.sortBy,
      minPrice: query.minPrice,
      maxPrice: query.maxPrice,
    };
    const res = await getProductListApi(params);
    const records = res.data?.records || [];
    total.value = Number(res.data?.total || 0);
    if (reset) {
      items.value = records;
    } else {
      items.value = items.value.concat(records);
    }
    if (reset && !records.length && isFilterActive.value) {
      await fetchRecommend();
    } else if (reset) {
      recommendItems.value = [];
    }
    queryPageNum.value += 1;
  } finally {
    loading.value = false;
  }
}

async function fetchRecommend() {
  const res = await getProductListApi({ pageNum: 1, pageSize: 8, sortBy: 'sales_desc' });
  recommendItems.value = res.data?.records || [];
}

function changeSort(value) {
  query.sortBy = value;
  fetchPage(true);
}

async function openSuggest() {
  if (blurTimer) {
    clearTimeout(blurTimer);
  }
  suggestVisible.value = true;
  const [historyRes, hotRes] = await Promise.all([
    getSearchHistoryApi().catch(() => ({ data: [] })),
    getHotSearchApi().catch(() => ({ data: [] })),
  ]);
  searchHistory.value = historyRes.data || [];
  hotKeywords.value = hotRes.data || [];
}

function onSearchBlur() {
  blurTimer = setTimeout(() => {
    closeSuggest();
  }, 120);
}

function closeSuggest() {
  suggestVisible.value = false;
}

function applyKeyword(keyword) {
  query.keyword = keyword;
  onSearch();
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

.search-box {
  position: relative;
}

.suggest-layer {
  position: absolute;
  top: 44px;
  left: 0;
  width: 560px;
  z-index: 20;
  background: #fff;
  border: 1px solid #ece7da;
  border-radius: 12px;
  box-shadow: 0 12px 34px rgba(41, 30, 17, 0.12);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 12px;
}

.suggest-col {
  border-radius: 10px;
  background: #fffaf3;
  padding: 10px;
}

.suggest-title {
  font-weight: 700;
  color: #6b4d2f;
  margin-bottom: 8px;
}

.tag-wrap {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.suggest-tag {
  cursor: pointer;
}

.hot-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hot-item {
  border: 0;
  background: #fff;
  border-radius: 8px;
  padding: 6px 8px;
  display: grid;
  grid-template-columns: 24px 1fr auto;
  gap: 8px;
  text-align: left;
  cursor: pointer;
}

.hot-item:hover {
  background: #fff0da;
}

.rank {
  color: #e2781f;
  font-weight: 700;
}

.word {
  color: #2f2f2f;
}

.score {
  color: #8a8a8a;
  font-size: 12px;
}

.empty-tips {
  color: #8e8a80;
  font-size: 13px;
}

.sort-row {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.result-head {
  color: #5f5f5f;
  margin-bottom: 10px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.empty-box {
  margin: 16px 0;
  border: 1px dashed #dfd4be;
  border-radius: 18px;
  padding: 28px 20px;
  text-align: center;
  background: linear-gradient(180deg, #fffdf9 0%, #fff6ea 100%);
}

.empty-illust {
  width: 86px;
  height: 86px;
  margin: 0 auto 10px;
  border-radius: 50%;
  background: #fff;
  display: grid;
  place-items: center;
  box-shadow: 0 6px 20px rgba(100, 80, 40, 0.12);
}

.magnifier {
  width: 36px;
  height: 36px;
  border: 5px solid #e6a23c;
  border-radius: 50%;
  position: relative;
  display: inline-block;
}

.magnifier::after {
  content: '';
  position: absolute;
  width: 16px;
  height: 5px;
  background: #e6a23c;
  border-radius: 4px;
  transform: rotate(35deg);
  right: -12px;
  bottom: -6px;
}

.empty-main {
  font-size: 19px;
  font-weight: 700;
  color: #4f412f;
}

.empty-sub {
  margin-top: 6px;
  color: #847462;
}

.recommend-grid {
  margin-top: 18px;
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

  .suggest-layer {
    width: 92vw;
    grid-template-columns: 1fr;
  }
}
</style>
