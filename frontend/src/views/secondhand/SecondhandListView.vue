<template>
  <section class="feed-page">
    <div class="hero">
      <div>
        <h1>二手交易</h1>
        <p>浏览二手商品、发布闲置、管理我的商品和拍卖。</p>
      </div>
      <div class="hero-dot"></div>
    </div>

    <el-tabs v-model="activeTab" class="mode-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="逛二手" name="browse">
        <el-form :inline="true" class="query" @submit.prevent>
          <el-form-item>
            <el-input
              v-model="query.keyword"
              placeholder="搜索二手商品"
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
              placeholder="商品分类"
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
            <el-select v-model="query.conditionLevel" placeholder="成色" clearable style="width: 160px">
              <el-option label="全新" value="全新" />
              <el-option label="99新" value="99新" />
              <el-option label="9成新" value="9成新" />
              <el-option label="8成新及以下" value="8成新及以下" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="query.isNegotiable" placeholder="是否议价" clearable style="width: 140px">
              <el-option label="可议价" :value="1" />
              <el-option label="不可议价" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSearch">搜索</el-button>
            <el-button @click="onReset">重置</el-button>
            <el-button type="success" @click="switchToPublish">发布</el-button>
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
          <el-form :model="form" label-width="110px">
            <el-form-item label="商品名称">
              <el-input v-model="form.name" placeholder="请输入商品名称" />
            </el-form-item>
            <el-form-item label="商品封面">
              <el-space class="cover-uploader" alignment="flex-start">
                <el-upload :show-file-list="false" :http-request="uploadCover" accept="image/*">
                  <el-button :loading="coverUploading">上传图片</el-button>
                </el-upload>
                <el-image v-if="form.cover" :src="toFullImageUrl(form.cover)" fit="cover" class="cover-preview" />
              </el-space>
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
                placeholder="请选择商品分类"
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
              <el-button text @click="activeTab = 'browse'">返回</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的二手商品" name="manage">
        <div class="form-shell">
          <div class="section-head">
            <h2>我的二手商品</h2>
            <el-button type="primary" @click="switchToPublish">发布二手</el-button>
          </div>

          <el-form :inline="true" class="query inner-query" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="sellerQuery.keyword"
                placeholder="搜索商品名称"
                clearable
                style="width: 220px"
                @keyup.enter="fetchSellerProducts(true)"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="sellerQuery.status" placeholder="状态筛选" clearable style="width: 140px">
                <el-option label="上架中" :value="1" />
                <el-option label="已下架/已售出" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchSellerProducts(true)">查询</el-button>
              <el-button @click="resetSellerQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="sellerLoading" :data="sellerRecords" style="width: 100%">
            <el-table-column prop="id" label="ID" width="88" />
            <el-table-column label="商品" min-width="260">
              <template #default="{ row }">
                <div class="prod">
                  <el-image v-if="row.cover" :src="toFullImageUrl(row.cover)" fit="cover" class="cover" />
                  <div v-else class="cover placeholder">暂无图片</div>
                  <div class="meta">
                    <div class="name">{{ row.name }}</div>
                    <div class="sub">
                      <span v-if="row.categoryName">{{ row.categoryName }}</span>
                      <span v-if="row.subCategoryName"> / {{ row.subCategoryName }}</span>
                      <span v-if="row.conditionLevel"> · {{ row.conditionLevel }}</span>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="售价" width="120">
              <template #default="{ row }">￥{{ Number(row.salePrice || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="议价" width="100">
              <template #default="{ row }">{{ Number(row.isNegotiable) === 1 ? '可议价' : '不可议价' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
                  {{ row.statusName || (Number(row.status) === 1 ? '上架中' : '已下架') }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="360" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="goSellerDetail(row.id)">查看详情</el-button>
                <el-button link type="warning" @click="goSellerDetail(row.id, true)">发起拍卖</el-button>
                <el-button v-if="Number(row.status) === 1" link type="danger" @click="toggleSellerStatus(row, 2)">
                  下架
                </el-button>
                <el-button v-else link type="success" @click="toggleSellerStatus(row, 1)">
                  上架
                </el-button>
                <el-button link type="danger" @click="removeSellerProduct(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!sellerLoading && sellerRecords.length === 0" description="暂无二手商品" />
          <div class="pager-wrap">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="sellerQuery.pageNum"
              :page-size="sellerQuery.pageSize"
              :total="sellerTotal"
              @current-change="onSellerPageChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我购买的二手商品" name="boughtOrders">
        <div class="form-shell">
          <el-form :inline="true" class="query inner-query" @submit.prevent>
            <el-form-item>
              <el-input v-model="boughtOrderQuery.keyword" placeholder="搜索订单号/商品名" clearable style="width: 240px" @keyup.enter="fetchBoughtOrders(true)" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="boughtOrderQuery.orderStatus" placeholder="订单状态" clearable style="width: 150px">
                <el-option label="待付款" :value="0" />
                <el-option label="待发货" :value="1" />
                <el-option label="待收货" :value="2" />
                <el-option label="待评价" :value="3" />
                <el-option label="已完成" :value="4" />
                <el-option label="已关闭" :value="9" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchBoughtOrders(true)">查询</el-button>
              <el-button @click="resetBoughtOrderQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="boughtOrderLoading" :data="boughtOrderRecords" style="width: 100%">
            <el-table-column prop="orderNo" label="订单号" min-width="190" />
            <el-table-column label="商品" min-width="240">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewProduct((row.items || [])[0]?.productId)">
                  {{ (row.items || [])[0]?.productName || '-' }}
                </el-button>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="120">
              <template #default="{ row }">￥{{ displayOrderAmount(row).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">{{ row.orderStatusName || formatOrderStatus(row.orderStatus) }}</template>
            </el-table-column>
            <el-table-column label="下单时间" min-width="170">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewBoughtOrder(row.id)">查看详情</el-button>
                <el-button v-if="Number(row.orderStatus) === 0" link type="success" @click="payBoughtOrder(row)">付款</el-button>
                <el-button v-if="Number(row.orderStatus) === 0" link type="danger" @click="cancelBoughtOrder(row)">取消</el-button>
                <el-button v-if="Number(row.orderStatus) === 1" link type="warning" @click="remindBoughtOrderShip(row)">提醒发货</el-button>
                <el-button v-if="Number(row.orderStatus) === 2" link type="success" @click="confirmBoughtOrderReceive(row)">确认收货</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!boughtOrderLoading && boughtOrderRecords.length === 0" description="暂无购买的二手商品" />
          <div class="pager-wrap">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="boughtOrderQuery.pageNum"
              :page-size="boughtOrderQuery.pageSize"
              :total="boughtOrderTotal"
              @current-change="onBoughtOrderPageChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="二手卖出订单" name="soldOrders">
        <div class="form-shell">
          <el-form :inline="true" class="query inner-query" @submit.prevent>
            <el-form-item>
              <el-input v-model="soldOrderQuery.keyword" placeholder="搜索订单号/商品名" clearable style="width: 240px" @keyup.enter="fetchSoldOrders(true)" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="soldOrderQuery.orderStatus" placeholder="订单状态" clearable style="width: 150px">
                <el-option label="待付款" :value="0" />
                <el-option label="待发货" :value="1" />
                <el-option label="待收货" :value="2" />
                <el-option label="待评价" :value="3" />
                <el-option label="已完成" :value="4" />
                <el-option label="已关闭" :value="9" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchSoldOrders(true)">查询</el-button>
              <el-button @click="resetSoldOrderQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="soldOrderLoading" :data="soldOrderRecords" style="width: 100%">
            <el-table-column prop="orderNo" label="订单号" min-width="190" />
            <el-table-column label="商品" min-width="220">
              <template #default="{ row }">{{ (row.items || [])[0]?.productName || '-' }}</template>
            </el-table-column>
            <el-table-column label="金额" width="120">
              <template #default="{ row }">￥{{ displayOrderAmount(row).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">{{ row.orderStatusName || formatOrderStatus(row.orderStatus) }}</template>
            </el-table-column>
            <el-table-column label="下单时间" min-width="170">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewSoldOrder(row.id)">查看详情</el-button>
                <el-button v-if="Number(row.orderStatus) === 1" link type="success" @click="shipSoldOrder(row)">发货</el-button>
                <el-button v-if="Number(row.orderStatus) === 2" link type="primary" @click="pushSoldOrderLogistics(row)">更新物流</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!soldOrderLoading && soldOrderRecords.length === 0" description="暂无二手卖出订单" />
          <div class="pager-wrap">
            <el-pagination
              background
              layout="prev, pager, next"
              :current-page="soldOrderQuery.pageNum"
              :page-size="soldOrderQuery.pageSize"
              :total="soldOrderTotal"
              @current-change="onSoldOrderPageChange"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的拍卖" name="auctions">
        <div class="form-shell">
          <el-form :inline="true" class="query inner-query" @submit.prevent>
            <el-form-item>
              <el-select v-model="auctionQuery.status" placeholder="状态" clearable style="width: 160px">
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
            <el-table-column prop="productId" label="商品ID" width="100" />
            <el-table-column label="起拍价" width="120">
              <template #default="{ row }">￥{{ Number(row.startPrice || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="当前价" width="130">
              <template #default="{ row }">￥{{ Number(row.currentPrice || row.startPrice || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120" />
            <el-table-column label="结束时间" min-width="180">
              <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="300" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="viewProduct(row.productId)">查看商品</el-button>
                <el-button v-if="row.settledOrderId" link type="success" @click="viewSoldOrder(row.settledOrderId)">查看订单</el-button>
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
import {
  changeSellerSecondhandStatusApi,
  closeAuctionEarlyApi,
  deleteSellerSecondhandApi,
  getMyAuctionListApi,
  getSecondhandListApi,
  getSellerSecondhandListApi,
  markAuctionFlowApi,
  publishSecondhandApi,
} from '@/api/secondhand';
import {
  cancelOrderApi,
  confirmReceiveOrderApi,
  getOrderListApi,
  getSellerOrderListApi,
  payOrderApi,
  remindShipOrderApi,
  shipOrderApi,
} from '@/api/order';
import { pushNextLogisticsApi } from '@/api/logistics';
import { uploadImageApi } from '@/api/upload';
import { SECONDHAND_CATEGORY_TREE } from '@/constants/categories';
import { getUser } from '@/utils/storage';

const TAB_NAMES = ['browse', 'publish', 'manage', 'boughtOrders', 'soldOrders', 'auctions'];
const pageSize = 16;
const loading = ref(false);
const sentinel = ref(null);
const queryPageNum = ref(1);
let observer = null;
const items = ref([]);
const total = ref(0);
const route = useRoute();
const router = useRouter();
const activeTab = ref(TAB_NAMES.includes(route.query.tab) ? route.query.tab : 'browse');

const sellerLoading = ref(false);
const sellerRecords = ref([]);
const sellerTotal = ref(0);
const sellerQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
});

const soldOrderLoading = ref(false);
const soldOrderRecords = ref([]);
const soldOrderTotal = ref(0);
const soldOrderQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  orderStatus: undefined,
});

const boughtOrderLoading = ref(false);
const boughtOrderRecords = ref([]);
const boughtOrderTotal = ref(0);
const boughtOrderQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  orderStatus: undefined,
});

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
  { label: '价格升序', value: 'price_asc' },
  { label: '价格降序', value: 'price_desc' },
];

const chips = [
  { label: '全部', condition: 'all' },
  { label: '全新', condition: '全新' },
  { label: '99新', condition: '99新' },
  { label: '9成新', condition: '9成新' },
  { label: '8成新及以下', condition: '8成新及以下' },
];

const selectedCategoryId = computed(() => {
  if (!query.categoryPath.length) return undefined;
  return query.categoryPath[1] || query.categoryPath[0];
});

const visibleItems = computed(() => items.value);
const hasMore = computed(() => items.value.length < total.value);

onMounted(async () => {
  await fetchPage(true);
  initObserver();
  loadTabData(activeTab.value);
});

onBeforeUnmount(() => {
  if (observer) observer.disconnect();
});

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = TAB_NAMES.includes(tab) ? tab : 'browse';
    loadTabData(activeTab.value);
  }
);

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
  await fetchPage(true);
}

async function fetchPage(reset = false) {
  if (loading.value) return;
  if (!reset && !hasMore.value) return;
  loading.value = true;
  try {
    if (reset) queryPageNum.value = 1;
    const res = await getSecondhandListApi({
      pageNum: queryPageNum.value,
      pageSize,
      keyword: query.keyword.trim() || undefined,
      categoryId: selectedCategoryId.value,
      minPrice: query.minPrice,
      maxPrice: query.maxPrice,
      conditionLevel: query.conditionLevel,
      isNegotiable: query.isNegotiable,
      sortBy: query.sortBy,
    });
    const records = (res.data?.records || []).map((item) => ({
      ...item,
      condition: item.conditionLevel || item.condition,
      originPrice: item.originPrice ?? item.salePrice,
      salePrice: item.salePrice ?? item.price,
    }));
    total.value = Number(res.data?.total || 0);
    items.value = reset ? records : items.value.concat(records);
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
  handleTabChange('publish');
}

function handleTabChange(name) {
  const nextQuery = { ...route.query };
  if (name && name !== 'browse') nextQuery.tab = name;
  else delete nextQuery.tab;
  router.replace({ path: '/secondhand', query: nextQuery });
  loadTabData(name);
}

function loadTabData(name) {
  if (name === 'manage') fetchSellerProducts(true);
  if (name === 'boughtOrders') fetchBoughtOrders(true);
  if (name === 'soldOrders') fetchSoldOrders(true);
  if (name === 'auctions') fetchMyAuctions(true);
}

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
  if (sentinel.value) observer.observe(sentinel.value);
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
const coverUploading = ref(false);

async function submit() {
  const [categoryId, subCategoryId] = form.categoryPath || [];
  if (!categoryId || !subCategoryId) {
    ElMessage.warning('请先选择商品分类');
    return;
  }
  submitting.value = true;
  try {
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
    await router.replace({ path: '/secondhand' });
    await fetchPage(true);
  } finally {
    submitting.value = false;
  }
}

async function uploadCover(option) {
  coverUploading.value = true;
  try {
    const result = await uploadImageApi(option.file);
    form.cover = result.data?.url || '';
    option.onSuccess?.(result);
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '图片上传失败');
    option.onError?.(error);
  } finally {
    coverUploading.value = false;
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

async function fetchSellerProducts(resetPage = false) {
  if (!getUser()?.id) {
    ElMessage.warning('请先登录');
    return;
  }
  if (sellerLoading.value) return;
  sellerLoading.value = true;
  try {
    if (resetPage) sellerQuery.pageNum = 1;
    const res = await getSellerSecondhandListApi({
      pageNum: sellerQuery.pageNum,
      pageSize: sellerQuery.pageSize,
      keyword: sellerQuery.keyword.trim() || undefined,
      status: sellerQuery.status,
    });
    sellerRecords.value = res.data?.records || [];
    sellerTotal.value = Number(res.data?.total || 0);
  } finally {
    sellerLoading.value = false;
  }
}

function resetSellerQuery() {
  sellerQuery.keyword = '';
  sellerQuery.status = undefined;
  fetchSellerProducts(true);
}

function onSellerPageChange(page) {
  sellerQuery.pageNum = page;
  fetchSellerProducts(false);
}

function goSellerDetail(id, toAuction = false) {
  router.push(toAuction ? { path: `/secondhand/${id}`, hash: '#auction' } : `/secondhand/${id}`);
}

async function toggleSellerStatus(row, status) {
  try {
    await ElMessageBox.confirm(`确认将「${row.name}」${status === 1 ? '上架' : '下架'}？`, '确认操作', { type: 'warning' });
    await changeSellerSecondhandStatusApi(row.id, status);
    ElMessage.success('操作成功');
    fetchSellerProducts(false);
    fetchPage(true);
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(e?.response?.data?.message || '操作失败');
  }
}

async function removeSellerProduct(row) {
  try {
    await ElMessageBox.confirm(`确认删除二手商品「${row.name}」？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    });
    await deleteSellerSecondhandApi(row.id);
    ElMessage.success('删除成功');
    fetchSellerProducts(true);
    fetchPage(true);
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(e?.response?.data?.message || '删除失败');
  }
}

async function fetchSoldOrders(resetPage = false) {
  if (!getUser()?.id) {
    ElMessage.warning('请先登录');
    return;
  }
  soldOrderLoading.value = true;
  try {
    if (resetPage) soldOrderQuery.pageNum = 1;
    const res = await getSellerOrderListApi({
      pageNum: soldOrderQuery.pageNum,
      pageSize: soldOrderQuery.pageSize,
      keyword: soldOrderQuery.keyword.trim() || undefined,
      orderStatus: soldOrderQuery.orderStatus,
      productType: 'SECONDHAND',
    });
    soldOrderRecords.value = res.data?.records || [];
    soldOrderTotal.value = Number(res.data?.total || 0);
  } finally {
    soldOrderLoading.value = false;
  }
}

function resetSoldOrderQuery() {
  soldOrderQuery.keyword = '';
  soldOrderQuery.orderStatus = undefined;
  fetchSoldOrders(true);
}

function onSoldOrderPageChange(page) {
  soldOrderQuery.pageNum = page;
  fetchSoldOrders(false);
}

async function fetchBoughtOrders(resetPage = false) {
  if (!getUser()?.id) {
    ElMessage.warning('请先登录');
    return;
  }
  boughtOrderLoading.value = true;
  try {
    if (resetPage) boughtOrderQuery.pageNum = 1;
    const res = await getOrderListApi({
      pageNum: boughtOrderQuery.pageNum,
      pageSize: boughtOrderQuery.pageSize,
      keyword: boughtOrderQuery.keyword.trim() || undefined,
      orderStatus: boughtOrderQuery.orderStatus,
      productType: 'SECONDHAND',
    });
    boughtOrderRecords.value = res.data?.records || [];
    boughtOrderTotal.value = Number(res.data?.total || 0);
  } finally {
    boughtOrderLoading.value = false;
  }
}

function resetBoughtOrderQuery() {
  boughtOrderQuery.keyword = '';
  boughtOrderQuery.orderStatus = undefined;
  fetchBoughtOrders(true);
}

function onBoughtOrderPageChange(page) {
  boughtOrderQuery.pageNum = page;
  fetchBoughtOrders(false);
}

async function payBoughtOrder(row) {
  try {
    await payOrderApi(row.id, { payMethod: '在线支付' });
    ElMessage.success('付款成功');
    fetchBoughtOrders(false);
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '付款失败');
  }
}

async function cancelBoughtOrder(row) {
  try {
    await ElMessageBox.confirm(`确认取消订单 ${row.orderNo || row.id}？`, '取消订单', { type: 'warning' });
    await cancelOrderApi(row.id);
    ElMessage.success('订单已取消');
    fetchBoughtOrders(false);
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(e?.response?.data?.message || '取消失败');
  }
}

async function remindBoughtOrderShip(row) {
  try {
    await remindShipOrderApi(row.id);
    ElMessage.success('已提醒卖家发货');
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提醒失败');
  }
}

async function confirmBoughtOrderReceive(row) {
  try {
    await ElMessageBox.confirm(`确认已收到订单 ${row.orderNo || row.id} 的商品？`, '确认收货', { type: 'warning' });
    await confirmReceiveOrderApi(row.id);
    ElMessage.success('收货成功');
    fetchBoughtOrders(false);
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(e?.response?.data?.message || '确认收货失败');
  }
}

async function shipSoldOrder(row) {
  try {
    await ElMessageBox.confirm(`确认订单 ${row.orderNo || row.id} 已发货？`, '确认发货', { type: 'warning' });
    await shipOrderApi(row.id);
    ElMessage.success('发货成功');
    fetchSoldOrders(false);
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(e?.response?.data?.message || '发货失败');
  }
}

async function pushSoldOrderLogistics(row) {
  try {
    const res = await pushNextLogisticsApi(row.id);
    ElMessage.success(`物流已更新：${res.data?.nodeName || '下一节点'}`);
    fetchSoldOrders(false);
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '物流更新失败');
  }
}

async function fetchMyAuctions(resetPage = false) {
  if (!getUser()?.id) {
    ElMessage.warning('请先登录');
    return;
  }
  auctionLoading.value = true;
  try {
    if (resetPage) auctionQuery.pageNum = 1;
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
  if (productId) router.push(`/secondhand/${productId}`);
}

function viewBoughtOrder(orderId) {
  if (orderId) router.push(`/secondhand/orders/${orderId}`);
}

function viewSoldOrder(orderId) {
  if (orderId) router.push(`/secondhand/sold-orders/${orderId}`);
}

function displayOrderAmount(row) {
  const payable = Number(row?.payableAmount);
  if (Number.isFinite(payable) && payable > 0) return payable;
  const total = Number(row?.totalAmount);
  return Number.isFinite(total) ? total : 0;
}

async function handleCloseAuction(row) {
  try {
    await ElMessageBox.confirm(`确认提前结束商品 ${row.productId} 的拍卖？`, '确认操作', { type: 'warning' });
    await closeAuctionEarlyApi(row.id);
    ElMessage.success('拍卖已结束');
    fetchMyAuctions(false);
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(e?.response?.data?.message || '操作失败');
  }
}

async function handleMarkAuctionFlow(row) {
  try {
    await ElMessageBox.confirm(`确认将商品 ${row.productId} 的拍卖标记为流拍？`, '确认操作', { type: 'warning' });
    await markAuctionFlowApi(row.id);
    ElMessage.success('已标记为流拍');
    fetchMyAuctions(false);
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(e?.response?.data?.message || '操作失败');
  }
}

function toFullImageUrl(url) {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  const normalized = url.startsWith('/') ? url : `/${url}`;
  return `http://localhost:8080${normalized}`;
}

function formatTime(value) {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  return d.toLocaleString('zh-CN', { hour12: false });
}

function formatOrderStatus(status) {
  const map = {
    0: '待付款',
    1: '待发货',
    2: '待收货',
    3: '待评价',
    4: '已完成',
    9: '已关闭',
  };
  return map[Number(status)] || '-';
}

function isCancel(error) {
  return error === 'cancel' || error?.toString?.().includes('cancel');
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

.hero-action {
  margin-top: 12px;
}

.hero-dot {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  background: radial-gradient(circle at 25% 25%, #fff5, #fff1 60%, transparent 70%);
}

.query,
.form-shell {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 12px;
}

.form-shell {
  padding: 16px;
}

.inner-query {
  margin-bottom: 12px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-head h2 {
  margin: 0;
  font-size: 20px;
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

.prod {
  display: flex;
  gap: 12px;
  align-items: center;
}

.cover {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.cover-preview {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  background: #f9fafb;
  font-size: 12px;
}

.meta .name {
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}

.meta .sub {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
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
