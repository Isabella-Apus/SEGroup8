<template>
  <div class="page-card order-page">
    <section class="order-head">
      <div>
        <span class="eyebrow">{{ pageCopy.eyebrow }}</span>
        <h2 class="page-title">{{ pageCopy.title }}</h2>
        <p>{{ pageCopy.desc }}</p>
      </div>
      <div v-if="!listLoading" class="head-stats">
        <span>{{ orderTypeLabel }} · {{ tabLabel }}</span>
        <strong>{{ filteredRecords.length }}</strong>
        <small>当前结果</small>
      </div>
    </section>

    <div class="toolbar order-toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索订单号/商品名"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
      />
      <el-date-picker
        v-model="query.startTime"
        type="datetime"
        placeholder="开始时间"
        value-format="x"
        class="time-input"
        clearable
      />
      <el-date-picker
        v-model="query.endTime"
        type="datetime"
        placeholder="结束时间"
        value-format="x"
        class="time-input"
        clearable
      />
      <el-input-number v-model="query.minAmount" :min="0" :precision="2" placeholder="最小金额" class="amount-input" />
      <el-input-number v-model="query.maxAmount" :min="0" :precision="2" placeholder="最大金额" class="amount-input" />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div v-if="!isScopedOrderPage" class="order-type-row" aria-label="订单类型筛选">
      <button
        v-for="item in orderTypeOptions"
        :key="item.value"
        type="button"
        class="order-type-card"
        :class="{ active: query.productType === item.value }"
        @click="changeOrderType(item.value)"
      >
        <strong>{{ item.label }}</strong>
        <span>{{ item.desc }}</span>
      </button>
    </div>

    <el-tabs v-model="activeTab" class="status-tabs" @tab-click="handleTabClick">
      <el-tab-pane label="全部" name="ALL" />
      <el-tab-pane label="待付款" name="PENDING_PAY" />
      <el-tab-pane label="待发货" name="PENDING_SHIP" />
      <el-tab-pane label="待收货" name="SHIPPED" />
      <el-tab-pane label="待评价" name="RECEIVED" />
      <el-tab-pane label="已完成" name="COMPLETED" />
      <el-tab-pane label="已关闭" name="CLOSED" />
    </el-tabs>

    <div v-if="listLoading && records.length === 0" class="skeleton-panel">
      <el-skeleton animated :rows="6" />
    </div>

    <el-empty v-else-if="!listLoading && filteredRecords.length === 0" description="暂无订单" />

    <div v-else v-loading="listLoading" class="order-list">
      <article v-for="order in filteredRecords" :key="order.id" class="order-card" @click="goDetail(order.id)">
        <div class="order-card__header">
          <div class="status-cluster">
            <span class="order-kind" :class="orderKindClass(order)">{{ orderKindLabel(order) }}</span>
            <OrderStatusTag
              :status="order.orderStatus"
              :status-name="order.orderStatusName"
              :refund-status="order.refundStatus"
              :refund-status-name="order.refundStatusName"
              size="default"
            />
            <span class="no">订单号：{{ order.orderNo }}</span>
          </div>
          <span class="time">{{ formatTime(order.createTime) }}</span>
        </div>

        <div class="order-items">
          <div v-for="item in order.items || []" :key="item.id" class="order-item">
            <div class="item-main">
              <strong class="name">{{ item.productName }}</strong>
              <span class="item-tags">
                <el-tag size="small" :type="getItemType(item) === 'SECONDHAND' ? 'warning' : 'info'">
                  {{ getItemType(item) === 'SECONDHAND' ? '二手' : '新品' }}
                </el-tag>
                <el-tag v-if="item.conditionLevel" size="small" type="success">
                  {{ item.conditionLevel }}
                </el-tag>
              </span>
            </div>
            <div class="price">￥{{ Number(item.price || 0).toFixed(2) }} × {{ item.quantity }}</div>
          </div>
        </div>

        <div class="order-extra">
          <div class="line">
            <span class="label">收货信息：</span>
            <span class="value">
              {{ order.receiverName || '-' }} {{ order.receiverPhone || '' }}
              {{ order.receiverProvince || '' }}{{ order.receiverCity || '' }}{{ order.receiverDetailAddress || '' }}
            </span>
          </div>
          <div class="line">
            <span class="label">支付方式：</span>
            <span class="value">{{ order.payMethod || '-' }}</span>
            <span class="sep" />
            <span class="label">物流单号：</span>
            <span class="value">{{ order.deliveryNo || '暂无' }}</span>
          </div>
        </div>

        <div class="order-card__footer" @click.stop>
          <div class="amount">
            <span>实付</span>
            <strong>￥{{ Number(order.totalAmount || 0).toFixed(2) }}</strong>
          </div>
          <el-space wrap class="order-actions">
            <el-button v-if="order.orderStatus === 0" size="small" @click="cancel(order.id)">取消订单</el-button>
            <el-button v-if="order.orderStatus === 0" size="small" type="primary" @click="pay(order.id)">立即付款</el-button>
            <el-button v-if="order.orderStatus === 1" size="small" @click="remindShip(order.id)">提醒发货</el-button>
            <el-button v-if="order.orderStatus === 2" size="small" @click="viewLogistics(order)">查看物流</el-button>
            <el-button v-if="order.orderStatus === 2" size="small" type="primary" @click="confirmReceive(order.id)">确认收货</el-button>
            <el-button v-if="order.orderStatus === 3" size="small" type="primary" @click="goReview(order.id)">去评价</el-button>
            <el-button v-if="canRefund(order.orderStatus, order.refundStatus)" size="small" type="danger" plain @click="openRefundDialog(order.id)">申请退货</el-button>
            <el-button size="small" @click="goDetail(order.id)">查看详情</el-button>
            <el-button v-if="getOrderPrimarySeller(order)?.sellerUserId" size="small" type="success" plain @click="contactSeller(order)">联系卖家</el-button>
          </el-space>
        </div>
      </article>
    </div>

    <div class="pager-wrap">
      <el-pagination
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNum"
        :page-sizes="[10, 20, 50]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog
      v-model="refundDialogVisible"
      title="申请退货/退款"
      width="520px"
      align-center
      append-to-body
    >
      <el-form label-width="90px">
        <el-form-item label="退货原因">
          <el-select v-model="refundForm.reason" placeholder="请选择" style="width: 100%">
            <el-option label="不想要了/拍错了" value="不想要了/拍错了" />
            <el-option label="质量问题/损坏" value="质量问题/损坏" />
            <el-option label="发错货/漏发" value="发错货/漏发" />
            <el-option label="描述不符" value="描述不符" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="refundForm.remark" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="可选，最多255字" />
        </el-form-item>
        <el-form-item label="上传凭证">
          <el-upload
            :auto-upload="false"
            :limit="3"
            :on-change="handleProofChange"
            :on-remove="handleProofRemove"
            accept="image/*"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="hint">最多3张，上传后会作为售后凭证保存。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="refundSubmitting" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="payDialogVisible"
      title="确认支付"
      width="520px"
      align-center
      append-to-body
      class="order-pay-dialog"
    >
      <el-form label-width="90px">
        <el-form-item label="支付方式">
          <el-radio-group v-model="payForm.payMode">
            <el-radio-button value="THIRD_PARTY">微信/支付宝</el-radio-button>
            <el-radio-button value="COIN">商城币</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="payForm.payMode === 'THIRD_PARTY'" label="渠道">
          <el-radio-group v-model="payForm.payChannel">
            <el-radio-button value="WECHAT">微信</el-radio-button>
            <el-radio-button value="ALIPAY">支付宝</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div v-if="payForm.payMode === 'THIRD_PARTY'" class="pay-qr-placeholder">选择渠道并完成付款后，点击“我已支付”。</div>
        <el-alert
          v-else
          type="info"
          show-icon
          :closable="false"
          title="确认后将直接扣减商城币余额。"
        />
      </el-form>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="paySubmitting" @click="confirmPay">{{ payForm.payMode === 'THIRD_PARTY' ? '我已支付' : '确认支付' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { useRoute, useRouter } from 'vue-router';
import { cancelOrderApi, confirmReceiveOrderApi, getOrderListApi, payOrderApi, refundOrderApi, remindShipOrderApi } from '@/api/order';
import { uploadImageApi } from '@/api/upload';
import { confirmOrderAction, showOrderActionError, showOrderActionSuccess } from '@/utils/orderUi';
import OrderStatusTag from '@/components/order/OrderStatusTag.vue';
import { fuzzySearchItems } from '@/utils/search';
import { onRealtimeEvent } from '@/realtime/realtimeClient';

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderStatus: undefined,
  keyword: '',
  startTime: null,
  endTime: null,
  minAmount: null,
  maxAmount: null,
  productType: 'ALL'
});
const total = ref(0);
const records = ref([]);
const activeTab = ref('ALL');
const listLoading = ref(false);
const route = useRoute();
const router = useRouter();

const filteredRecords = computed(() => {
  const keyword = String(query.keyword || '').trim();
  if (!keyword) {
    return records.value;
  }
  return fuzzySearchItems(records.value, keyword, {
    keys: [
      'orderNo',
      {
        name: 'itemNames',
        getFn: (order) => (order.items || []).map((item) => item.productName || '').join(' '),
      },
    ],
    threshold: 0.4,
  });
});

const tabLabelMap = {
  ALL: '全部',
  PENDING_PAY: '待付款',
  PENDING_SHIP: '待发货',
  SHIPPED: '待收货',
  RECEIVED: '待评价',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
};
const tabLabel = computed(() => tabLabelMap[activeTab.value] || '全部');
const orderTypeOptions = [
  { label: '全部订单', value: 'ALL', desc: '新品和二手一起看' },
  { label: '新品订单', value: 'NEW', desc: '官方商品、商城下单' },
  { label: '二手订单', value: 'SECONDHAND', desc: '闲置交易、议价沟通' }
];
const orderTypeLabel = computed(() => orderTypeOptions.find((item) => item.value === query.productType)?.label || '全部订单');
const scopedOrderType = computed(() => {
  const scope = String(route.meta?.orderScope || '').toUpperCase();
  return scope === 'NEW' || scope === 'SECONDHAND' ? scope : '';
});
const isScopedOrderPage = computed(() => !!scopedOrderType.value);
const pageCopy = computed(() => {
  if (scopedOrderType.value === 'SECONDHAND') {
    return {
      eyebrow: 'Secondhand Orders',
      title: '二手订单',
      desc: '只展示二手商城产生的订单，便于查看闲置交易进度。'
    };
  }
  if (scopedOrderType.value === 'NEW') {
    return {
      eyebrow: 'New Goods Orders',
      title: '新品订单',
      desc: '只展示新品商城产生的订单，购物车结算和售后都在这里。'
    };
  }
  return {
    eyebrow: 'Orders',
    title: '我的订单',
    desc: '查看交易进度、物流状态和售后入口。'
  };
});

const refundDialogVisible = ref(false);
const refundSubmitting = ref(false);
const refundTargetId = ref(null);
const payDialogVisible = ref(false);
const paySubmitting = ref(false);
const payTargetId = ref(null);
const refundForm = reactive({
  reason: '',
  remark: '',
  proofUrls: []
});
const payForm = reactive({
  payMode: 'THIRD_PARTY',
  payChannel: 'WECHAT'
});

let unsubscribeRealtime = null;

onMounted(() => {
  syncOrderScopeFromRoute();
  syncQueryFromTab();
  fetchOrders();
  unsubscribeRealtime = onRealtimeEvent(handleRealtimeEvent);
});

onBeforeUnmount(() => {
  if (unsubscribeRealtime) unsubscribeRealtime();
});

async function fetchOrders() {
  listLoading.value = true;
  try {
    const result = await getOrderListApi(query);
    records.value = result.data?.records || [];
    total.value = result.data?.total || 0;
  } finally {
    listLoading.value = false;
  }
}

function tabToStatus(tab) {
  switch (tab) {
    case 'PENDING_PAY':
      return 0;
    case 'PENDING_SHIP':
      return 1;
    case 'SHIPPED':
      return 2;
    case 'RECEIVED':
      return 3;
    case 'COMPLETED':
      return 4;
    case 'CLOSED':
      return 9;
    default:
      return undefined;
  }
}

function syncQueryFromTab() {
  query.orderStatus = tabToStatus(activeTab.value);
}

function syncOrderScopeFromRoute() {
  query.productType = scopedOrderType.value || 'ALL';
}

watch(
  () => route.meta?.orderScope,
  () => {
    activeTab.value = 'ALL';
    query.pageNum = 1;
    syncOrderScopeFromRoute();
    syncQueryFromTab();
    fetchOrders();
  },
);

async function handleTabClick() {
  await nextTick();
  query.pageNum = 1;
  syncQueryFromTab();
  fetchOrders();
}

function handleSearch() {
  query.pageNum = 1;
  fetchOrders();
}

function handleReset() {
  query.keyword = '';
  query.startTime = null;
  query.endTime = null;
  query.minAmount = null;
  query.maxAmount = null;
  activeTab.value = 'ALL';
  syncOrderScopeFromRoute();
  query.orderStatus = undefined;
  query.pageNum = 1;
  fetchOrders();
}

function changeOrderType(type) {
  if (isScopedOrderPage.value) {
    return;
  }
  query.productType = type;
  query.pageNum = 1;
  fetchOrders();
}

function handlePageChange(pageNum) {
  query.pageNum = pageNum;
  fetchOrders();
}

function handleSizeChange(pageSize) {
  query.pageSize = pageSize;
  query.pageNum = 1;
  fetchOrders();
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ');
}

function orderDetailPath(orderOrId) {
  const id = typeof orderOrId === 'object' ? orderOrId.id : orderOrId;
  if (scopedOrderType.value === 'SECONDHAND' || (typeof orderOrId === 'object' && getOrderKind(orderOrId) === 'SECONDHAND')) {
    return `/secondhand/orders/${id}`;
  }
  return `/order/${id}`;
}

function goDetail(orderOrId) {
  router.push(orderDetailPath(orderOrId));
}

function getOrderPrimarySeller(order) {
  const items = order?.items || [];
  return items.find((item) => item?.sellerUserId) || null;
}

function getItemType(item) {
  return String(item?.productType || item?.itemType || 'NEW').toUpperCase() === 'SECONDHAND' ? 'SECONDHAND' : 'NEW';
}

function getOrderKind(order) {
  const explicit = String(order?.orderType || '').toUpperCase();
  if (explicit === 'NEW' || explicit === 'SECONDHAND') {
    return explicit;
  }
  return (order?.items || []).some((item) => getItemType(item) === 'SECONDHAND') ? 'SECONDHAND' : 'NEW';
}

function orderKindLabel(order) {
  return getOrderKind(order) === 'SECONDHAND' ? '二手订单' : '新品订单';
}

function orderKindClass(order) {
  return getOrderKind(order) === 'SECONDHAND' ? 'secondhand' : 'new';
}

function contactSeller(order) {
  const seller = getOrderPrimarySeller(order);
  if (!seller?.sellerUserId) {
    showOrderActionError({ message: '未找到卖家信息' }, '联系卖家失败');
    return;
  }
  router.push({
    path: '/messages',
    query: {
      participantId: seller.sellerUserId,
      sourceType: getItemType(seller) === 'SECONDHAND' ? 'SECONDHAND' : 'PRODUCT',
      sourceId: seller.productId
    }
  });
}

async function pay(orderId) {
  payTargetId.value = orderId;
  payForm.payMode = 'THIRD_PARTY';
  payForm.payChannel = 'WECHAT';
  payDialogVisible.value = true;
}

async function confirmPay() {
  if (!payTargetId.value) return;
  paySubmitting.value = true;
  try {
    await payOrderApi(payTargetId.value, {
      payMode: payForm.payMode,
      payChannel: payForm.payChannel
    });
    payDialogVisible.value = false;
    showOrderActionSuccess('支付成功');
    fetchOrders();
  } catch (error) {
    showOrderActionError(error, '支付失败');
  } finally {
    paySubmitting.value = false;
  }
}

async function cancel(orderId) {
  try {
    await confirmOrderAction({
      title: '确认取消订单',
      message: '取消后将无法继续付款，是否继续？',
      confirmButtonText: '确认取消'
    });
    await cancelOrderApi(orderId);
    showOrderActionSuccess('已取消订单');
    fetchOrders();
  } catch (error) {
    if (String(error?.message || '').includes('cancel')) return;
    showOrderActionError(error, '取消订单失败');
  }
}

async function confirmReceive(orderId) {
  try {
    await confirmOrderAction({
      title: '确认收货',
      message: '确认已收到货物？确认后订单将进入待评价。',
      confirmButtonText: '确认收货'
    });
    await confirmReceiveOrderApi(orderId);
    showOrderActionSuccess('已确认收货');
    fetchOrders();
  } catch (error) {
    if (String(error?.message || '').includes('cancel')) return;
    showOrderActionError(error, '确认收货失败');
  }
}

function goReview(orderId) {
  router.push({ path: orderDetailPath(orderId), query: { action: 'review' } });
}

function canRefund(status, refundStatus) {
  if (refundStatus === 1 || refundStatus === 2) return false;
  return status === 1 || status === 2 || status === 3 || status === 4;
}

function openRefundDialog(orderId) {
  refundTargetId.value = orderId;
  refundForm.reason = '';
  refundForm.remark = '';
  refundForm.proofUrls = [];
  refundDialogVisible.value = true;
}

async function handleProofChange(file) {
  if (!file?.raw) return;
  const res = await uploadImageApi(file.raw);
  const url = res.data?.url;
  if (url) {
    refundForm.proofUrls.push(url);
  }
}

function handleProofRemove(file) {
  const url = file?.response?.data?.url || file?.url;
  if (!url) return;
  refundForm.proofUrls = refundForm.proofUrls.filter((u) => u !== url);
}

async function submitRefund() {
  if (!refundTargetId.value) return;
  if (!refundForm.reason) {
    showOrderActionError({ message: '请选择退货原因' }, '提交退货申请失败');
    return;
  }
  refundSubmitting.value = true;
  try {
    const remark = refundForm.remark?.trim();
    const reason = remark ? `${refundForm.reason}（${remark}）` : refundForm.reason;
    await refundOrderApi(refundTargetId.value, {
      reason,
      proofUrls: refundForm.proofUrls
    });
    refundDialogVisible.value = false;
    showOrderActionSuccess('已提交退货申请');
    await fetchOrders();
  } catch (error) {
    showOrderActionError(error, '提交退货申请失败');
  } finally {
    refundSubmitting.value = false;
  }
}

async function remindShip(orderId) {
  try {
    await remindShipOrderApi(orderId);
    showOrderActionSuccess('已提醒卖家发货');
  } catch (error) {
    showOrderActionError(error, '提醒发货失败');
  }
}

function viewLogistics(order) {
  router.push({
    path: orderDetailPath(order),
    query: { tab: 'logistics' }
  });
}

function handleRealtimeEvent(event) {
  const type = event?.detail?.eventType;
  if (type === 'ORDER_STATUS_UPDATED' || type === 'AFTER_SALE_UPDATED' || type === 'LOGISTICS_UPDATED' || type === 'ORDER_REMIND_SHIP') {
    fetchOrders();
  }
}
</script>

<style scoped>
.order-page {
  background: transparent;
  border: 0;
  box-shadow: none;
  padding: 0;
}

.order-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: stretch;
  margin-bottom: 18px;
  border: 1px solid rgba(229, 221, 210, 0.92);
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(220, 239, 233, 0.92), rgba(247, 239, 229, 0.9) 54%, rgba(241, 240, 251, 0.92)),
    #ffffff;
  padding: 22px;
  box-shadow: var(--shadow-soft);
}

.eyebrow {
  display: inline-flex;
  width: fit-content;
  border-radius: 999px;
  padding: 5px 11px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--brand-primary);
  font-size: 12px;
  font-weight: 900;
}

.page-title {
  margin: 10px 0 6px;
  font-size: 30px;
}

.order-head p {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 700;
}

.head-stats {
  min-width: 132px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.72);
  padding: 14px 16px;
  text-align: right;
  box-shadow: 0 10px 24px rgba(39, 50, 58, 0.08);
}

.head-stats span,
.head-stats small {
  display: block;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.head-stats strong {
  display: block;
  margin: 2px 0;
  font-size: 32px;
  line-height: 1;
}

.order-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1.35fr) repeat(2, minmax(180px, 0.75fr)) repeat(2, 150px) auto auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  border-radius: 20px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--shadow-soft);
}

.search-input,
.time-input,
.amount-input {
  width: 100%;
}

.status-tabs {
  margin-bottom: 12px;
}

.order-type-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.order-type-card {
  min-height: 68px;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.82);
  color: var(--text-main);
  padding: 12px 14px;
  text-align: left;
  cursor: pointer;
  box-shadow: var(--shadow-soft);
}

.order-type-card strong,
.order-type-card span {
  display: block;
}

.order-type-card strong {
  font-size: 16px;
  font-weight: 900;
}

.order-type-card span {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.order-type-card.active,
.order-type-card:hover {
  border-color: rgba(60, 146, 255, 0.42);
  background: linear-gradient(135deg, #eaf4ff 0%, #e9fff8 100%);
}

.status-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: rgba(101, 112, 108, 0.16);
}

.status-tabs :deep(.el-tabs__item) {
  font-weight: 800;
  color: var(--text-secondary);
}

.status-tabs :deep(.el-tabs__item.is-active) {
  color: var(--brand-primary);
}

.status-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 999px;
  background: var(--brand-accent-strong);
}

.order-list {
  display: grid;
  gap: 14px;
}

.order-card {
  cursor: pointer;
  border: 1px solid rgba(229, 221, 210, 0.92);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.86);
  padding: 18px;
  box-shadow: var(--shadow-soft);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.order-card:hover {
  transform: translateY(-2px);
  border-color: rgba(120, 196, 182, 0.55);
  box-shadow: var(--shadow-float);
}

.order-card__header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(229, 221, 210, 0.72);
}

.status-cluster {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.order-kind {
  min-height: 28px;
  border-radius: 999px;
  padding: 0 11px;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 900;
}

.order-kind.new {
  color: var(--brand-primary);
  background: var(--brand-primary-weak);
  border: 1px solid rgba(60, 146, 255, 0.22);
}

.order-kind.secondhand {
  color: var(--brand-accent-strong);
  background: var(--brand-mint-weak);
  border: 1px solid rgba(53, 216, 171, 0.28);
}

.no,
.time {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.time {
  white-space: nowrap;
}

.order-items {
  display: grid;
  gap: 0;
  padding: 6px 0;
}

.order-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  min-height: 46px;
  border-bottom: 1px dashed rgba(101, 112, 108, 0.14);
}

.order-item:last-child {
  border-bottom: 0;
}

.item-main {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.order-item .name {
  color: var(--text-main);
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-tags {
  display: inline-flex;
  gap: 6px;
}

.order-item .price {
  white-space: nowrap;
  color: var(--text-main);
  font-weight: 800;
}

.order-extra {
  display: grid;
  gap: 8px;
  margin-top: 8px;
  border-radius: 16px;
  background: rgba(246, 244, 239, 0.72);
  padding: 12px;
  color: var(--text-secondary);
  font-size: 12px;
}

.order-extra .line {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.order-extra .label {
  color: var(--text-muted);
  font-weight: 800;
}

.order-extra .sep {
  width: 1px;
  height: 12px;
  background: rgba(101, 112, 108, 0.2);
  margin: 0 6px;
}

.order-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(229, 221, 210, 0.72);
}

.amount span {
  display: block;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.amount strong {
  color: var(--danger);
  font-size: 20px;
}

.order-actions {
  justify-content: flex-end;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.hint {
  color: var(--text-secondary);
  font-size: 12px;
  margin-top: 6px;
}

.pay-qr-placeholder {
  margin: 8px 0;
  height: 180px;
  border: 2px dashed rgba(120, 196, 182, 0.45);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  background: rgba(220, 239, 233, 0.28);
}

:global(.order-pay-dialog.el-dialog) {
  border-radius: 8px;
  overflow: hidden;
}

:global(.order-pay-dialog .el-dialog__body) {
  padding-top: 10px;
}

@media (max-width: 1080px) {
  .order-toolbar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .order-type-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .order-head,
  .order-card__header,
  .order-card__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .head-stats {
    text-align: left;
  }

  .order-toolbar {
    grid-template-columns: 1fr;
  }

  .order-item {
    grid-template-columns: 1fr;
    gap: 4px;
    padding: 9px 0;
  }

  .time {
    white-space: normal;
  }
}
</style>
