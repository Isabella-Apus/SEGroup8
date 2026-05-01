<template>
  <section class="feed-page">
    <div class="hero">
      <div>
        <h1>二手交易</h1>
      </div>
      <div class="hero-dot"></div>
    </div>

    <el-tabs v-model="activeTab" class="mode-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="逛二手" name="browse">
        <el-form :inline="true" class="query" @submit.prevent>
          <el-form-item>
            <el-input
              v-model="query.keyword"
              placeholder="搜二手商品名，例如：自行车"
              clearable
              style="width: 260px"
              @keyup.enter="onSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-cascader
              v-model="query.categoryPath"
              :options="SECONDHAND_CATEGORY_TREE"
              :props="cascaderProps"
              clearable
              filterable
              collapse-tags
              collapse-tags-tooltip
              placeholder="分类筛选"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item>
            <el-input-number v-model="query.minPrice" :min="0" :precision="2" :step="10" placeholder="最低价" />
          </el-form-item>
          <el-form-item>
            <el-input-number v-model="query.maxPrice" :min="0" :precision="2" :step="10" placeholder="最高价" />
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.conditionLevel" placeholder="成色筛选" clearable style="width: 160px">
              <el-option label="全新" value="全新" />
              <el-option label="99新" value="99新" />
              <el-option label="9成新" value="9成新" />
              <el-option label="8成新及以下" value="8成新及以下" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.isNegotiable" placeholder="议价筛选" clearable style="width: 140px">
              <el-option label="可议价" :value="1" />
              <el-option label="不可议价" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSearch">搜索</el-button>
            <el-button @click="onReset">重置</el-button>
            <el-button type="success" @click="switchToPublish">去发布</el-button>
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

        <div class="grid">
          <ProductCard
            v-for="item in visibleItems"
            :key="item.id"
            :product="item"
            mode="secondhand"
            route-base="/secondhand"
            :highlight-keyword="query.keyword.trim()"
          />
        </div>

        <div ref="sentinel" class="sentinel">
          <span v-if="loading">加载中...</span>
          <span v-else-if="!hasMore && visibleItems.length">已经到底了</span>
          <span v-else-if="!visibleItems.length">暂无二手商品</span>
        </div>
      </el-tab-pane>

      <el-tab-pane label="发布二手" name="publish">
        <div class="form-shell">
          <el-form :model="form" label-width="96px">
            <el-form-item label="商品名称">
              <el-input v-model="form.name" placeholder="例如：九成新办公椅" />
            </el-form-item>
            <el-form-item label="封面链接">
              <el-input v-model="form.cover" placeholder="请输入图片 URL" />
            </el-form-item>
            <el-form-item label="原价">
              <el-input-number v-model="form.originPrice" :min="1" :precision="2" :step="10" />
            </el-form-item>
            <el-form-item label="售价">
              <el-input-number v-model="form.salePrice" :min="1" :precision="2" :step="10" />
            </el-form-item>
            <el-form-item label="商品分类">
              <el-cascader
                v-model="form.categoryPath"
                :options="SECONDHAND_CATEGORY_TREE"
                :props="cascaderProps"
                clearable
                filterable
                placeholder="先选一级，再选二级"
                style="width: 320px"
              />
            </el-form-item>
            <el-form-item label="成色">
              <el-select v-model="form.condition" style="width: 180px">
                <el-option label="全新" value="全新" />
                <el-option label="99新" value="99新" />
                <el-option label="9成新" value="9成新" />
                <el-option label="8成新及以下" value="8成新及以下" />
              </el-select>
            </el-form-item>
            <el-form-item label="议价">
              <el-switch
                v-model="form.isNegotiable"
                :active-value="1"
                :inactive-value="0"
                active-text="可议价"
                inactive-text="不可议价"
              />
            </el-form-item>
            <el-form-item label="商品描述">
              <el-input v-model="form.description" type="textarea" :rows="4" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="submit">发布二手</el-button>
              <el-button @click="reset">重置</el-button>
              <el-button text @click="activeTab = 'browse'">返回逛二手</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的拍卖" name="auctions">
        <div class="form-shell">
          <el-form :inline="true" class="query" @submit.prevent>
            <el-form-item>
              <el-select v-model="auctionQuery.status" placeholder="状态筛选" clearable style="width: 160px">
                <el-option label="进行中" value="ONGOING" />
                <el-option label="已结束" value="FINISHED" />
                <el-option label="流拍" value="FLOW" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchMyAuctions(true)">查询</el-button>
              <el-button @click="resetAuctionQuery">重置</el-button>
            </el-form-item>
          </el-form>
          <el-table v-loading="auctionLoading" :data="auctionRecords" style="width: 100%">
            <el-table-column prop="productId" label="商品ID" width="88" />
            <el-table-column label="起拍价" width="110">
              <template #default="{ row }">￥{{ Number(row.startPrice || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="当前价" width="110">
              <template #default="{ row }">￥{{ Number(row.currentPrice || row.startPrice || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column label="结束时间" min-width="180">
              <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="320" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewProduct(row.productId)">查看商品</el-button>
                <el-button v-if="row.settledOrderId" link type="success" @click="viewOrder(row.settledOrderId)">查看订单</el-button>
                <el-button v-if="row.status === 'ONGOING'" link type="warning" @click="handleCloseAuction(row)">提前结束</el-button>
                <el-button v-if="row.status === 'ONGOING'" link type="danger" @click="handleMarkAuctionFlow(row)">标记流拍</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!auctionLoading && auctionRecords.length === 0" description="暂无拍卖记录" />
          <div class="pager-wrap">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="auctionQuery.pageNum"
              :page-size="auctionQuery.pageSize"
              :total="auctionTotal"
              @current-change="onAuctionPageChange"
            />
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import ProductCard from '@/components/ProductCard.vue';
import { closeAuctionEarlyApi, getMyAuctionListApi, getSecondhandListApi, markAuctionFlowApi, publishSecondhandApi } from '@/api/secondhand';
import { SECONDHAND_CATEGORY_TREE } from '@/constants/categories';
import { getUser } from '@/utils/storage';
const pageSize = 16;
const loading = ref(false);
const sentinel = ref(null);
const queryPageNum = ref(1);
let observer = null;
const items = ref([]);
const total = ref(0);
const route = useRoute();
const router = useRouter();
const activeTab = ref(route.query.tab === 'publish' || route.query.tab === 'auctions' ? route.query.tab : 'browse');
const auctionLoading = ref(false);
const auctionRecords = ref([]);
const auctionTotal = ref(0);
const auctionQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
});

const query = reactive({
  keyword: '',
  categoryPath: [],
  minPrice: undefined,
  maxPrice: undefined,
  conditionLevel: undefined,
  isNegotiable: undefined,
  sortBy: 'time_desc',
  condition: 'all',
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
  { label: '全部', condition: 'all' },
  { label: '全新', condition: '全新' },
  { label: '99新', condition: '99新' },
  { label: '9成新', condition: '9成新' },
  { label: '8成新及以下', condition: '8成新及以下' }
];

const selectedCategoryId = computed(() => {
  if (!query.categoryPath.length) {
    return undefined;
  }
  return query.categoryPath[1] || query.categoryPath[0];});

const visibleItems = computed(() => items.value);
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
}

async function onReset() {
  query.keyword = '';
  query.categoryPath = [];
  query.minPrice = undefined;
  query.maxPrice = undefined;
  query.conditionLevel = undefined;
  query.isNegotiable = undefined;
  query.sortBy = 'time_desc';
  query.condition = 'all';
  await fetchPage(true);
}

async function applyChip(chip) {
  query.condition = chip.condition;
  query.conditionLevel = chip.condition === 'all' ? undefined : chip.condition;
  await fetchPage(true);}

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
      keyword: query.keyword.trim() || undefined,
      categoryId: selectedCategoryId.value,
      minPrice: query.minPrice,
      maxPrice: query.maxPrice,
      conditionLevel: query.conditionLevel,
      isNegotiable: query.isNegotiable,
      sortBy: query.sortBy,    });
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

function changeSort(value) {
  query.sortBy = value;
  fetchPage(true);
}

function switchToPublish() {
  activeTab.value = 'publish';
}

function handleTabChange(name) {
  const nextQuery = { ...route.query };
  if (name === 'publish' || name === 'auctions') nextQuery.tab = name;
  else delete nextQuery.tab;
  router.replace({ path: '/secondhand', query: nextQuery });
  if (name === 'auctions') {
    fetchMyAuctions(true);
  }
}

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = tab === 'publish' || tab === 'auctions' ? tab : 'browse';
  }
);
function initObserver() {
  observer = new IntersectionObserver(
    (entries) => {
      const [entry] = entries;
      if (entry.isIntersecting && activeTab.value === 'browse') {
        fetchPage(false);
      }
    },
    { root: null, rootMargin: '280px 0px', threshold: 0 }
  );
  if (sentinel.value) {
    observer.observe(sentinel.value);
  }
}

const form = reactive({
  name: '',
  cover: '',
  originPrice: 100,
  salePrice: 80,
  categoryPath: [],
  condition: '9成新',
  isNegotiable: 1,
  description: '',
});

const submitting = ref(false);

async function submit() {
  submitting.value = true;
  try {
    const [categoryId, subCategoryId] = form.categoryPath || [];
    if (!categoryId || !subCategoryId) {
      ElMessage.warning('请先选择一级与二级分类');
      return;
    }
    await publishSecondhandApi({
      name: form.name,
      cover: form.cover,
      description: form.description,
      originPrice: form.originPrice,
      salePrice: form.salePrice,
      categoryId,
      subCategoryId,
      conditionLevel: form.condition,
      isNegotiable: form.isNegotiable,
    });
    ElMessage.success('二手商品发布成功');
    reset();
    activeTab.value = 'browse';
    await fetchPage(true);
  } finally {
    submitting.value = false;
  }
}

function reset() {
  form.name = '';
  form.cover = '';
  form.originPrice = 100;
  form.salePrice = 80;
  form.categoryPath = [];
  form.condition = '9成新';
  form.isNegotiable = 1;
  form.description = '';
}

async function fetchMyAuctions(reset = false) {
  if (!getUser()?.id) {
    ElMessage.warning('请先登录');
    return;
  }
  auctionLoading.value = true;
  try {
    if (reset) {
      auctionQuery.pageNum = 1;
    }
    const res = await getMyAuctionListApi({
      pageNum: auctionQuery.pageNum,
      pageSize: auctionQuery.pageSize,
      status: auctionQuery.status || undefined,
    });
    auctionRecords.value = res.data?.records || [];
    auctionTotal.value = Number(res.data?.total || 0);
  } finally {
    auctionLoading.value = false;
  }
}

function resetAuctionQuery() {
  auctionQuery.status = undefined;
  fetchMyAuctions(true);
}

function onAuctionPageChange(page) {
  auctionQuery.pageNum = page;
  fetchMyAuctions(false);
}

function viewProduct(productId) {
  router.push(`/secondhand/${productId}`);
}

function viewOrder(orderId) {
  if (!orderId) return;
  router.push(`/order/${orderId}`);
}

async function handleCloseAuction(row) {
  try {
    await ElMessageBox.confirm(
      `确认提前结束拍卖（商品ID: ${row.productId}）？有出价时会立即结算。`,
      '提前结束确认',
      { type: 'warning' }
    );
    await closeAuctionEarlyApi(row.id);
    ElMessage.success('拍卖已提前结束');
    fetchMyAuctions(false);
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return;
    ElMessage.error(e?.response?.data?.message || '操作失败');
  }
}

async function handleMarkAuctionFlow(row) {
  try {
    await ElMessageBox.confirm(
      `确认将拍卖标记为流拍（商品ID: ${row.productId}）？此操作仅适用于无人出价场景。`,
      '流拍确认',
      { type: 'warning' }
    );
    await markAuctionFlowApi(row.id);
    ElMessage.success('已标记为流拍');
    fetchMyAuctions(false);
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return;
    ElMessage.error(e?.response?.data?.message || '操作失败');
  }
}

function formatTime(value) {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  return d.toLocaleString('zh-CN', { hour12: false });
}

</script>

<style scoped>
.feed-page {
  padding: 8px 10px 20px;
}

.mode-tabs {
  margin-top: 8px;
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

.form-shell {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 16px;
}

.pager-wrap {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
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
