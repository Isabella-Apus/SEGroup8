<template>
  <div class="page-card">
    <h2 class="page-title">
      我的订单
      <span v-if="!listLoading" class="page-title-sub">（{{ tabLabel }} · 共 {{ filteredRecords.length }} 条）</span>
    </h2>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索订单号/商品名"
        clearable
        style="max-width: 360px"
        @keyup.enter="handleSearch"
      />
      <el-date-picker
        v-model="query.startTime"
        type="datetime"
        placeholder="开始时间"
        value-format="x"
        style="width: 200px"
        clearable
      />
      <el-date-picker
        v-model="query.endTime"
        type="datetime"
        placeholder="结束时间"
        value-format="x"
        style="width: 200px"
        clearable
      />
      <el-input-number v-model="query.minAmount" :min="0" :precision="2" placeholder="最小金额" style="width: 160px" />
      <el-input-number v-model="query.maxAmount" :min="0" :precision="2" placeholder="最大金额" style="width: 160px" />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
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
      <el-card v-for="order in filteredRecords" :key="order.id" shadow="hover" class="order-card" @click="goDetail(order.id)">
        <div class="order-card__header">
          <OrderStatusTag
            :status="order.orderStatus"
            :status-name="order.orderStatusName"
            :refund-status="order.refundStatus"
            :refund-status-name="order.refundStatusName"
            size="default"
          />
          <div class="meta">
            <span class="no">订单号：{{ order.orderNo }}</span>
            <span class="time">{{ formatTime(order.createTime) }}</span>
          </div>
        </div>

        <div v-for="item in order.items || []" :key="item.id" class="order-item">
          <div class="name">
            {{ item.productName }}
            <el-tag v-if="item.productType" size="small" style="margin-left: 8px" :type="item.productType === 'SECONDHAND' ? 'warning' : 'info'">
              {{ item.productType === 'SECONDHAND' ? '二手' : '新品' }}
            </el-tag>
            <el-tag v-if="item.conditionLevel" size="small" type="success" style="margin-left: 6px">
              {{ item.conditionLevel }}
            </el-tag>
          </div>
          <div class="price">￥{{ Number(item.price || 0).toFixed(2) }} × {{ item.quantity }}</div>
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
          <div class="amount">实付：<strong>￥{{ Number(order.totalAmount || 0).toFixed(2) }}</strong></div>
          <el-space>
            <el-button v-if="order.orderStatus === 0" size="small" @click="cancel(order.id)">取消订单</el-button>
            <el-button v-if="order.orderStatus === 0" size="small" type="primary" @click="pay(order.id)">立即付款</el-button>
            <el-button v-if="order.orderStatus === 1" size="small" @click="remindShip(order.id)">提醒发货</el-button>
            <el-button v-if="order.orderStatus === 2" size="small" @click="viewLogistics(order)">查看物流</el-button>
            <el-button v-if="order.orderStatus === 2" size="small" type="primary" @click="confirmReceive(order.id)">确认收货</el-button>
            <el-button v-if="order.orderStatus === 3" size="small" type="primary" @click="goReview(order.id)">去评价</el-button>
            <el-button v-if="canRefund(order.orderStatus, order.refundStatus)" size="small" type="danger" plain @click="openRefundDialog(order.id)">申请退货</el-button>
            <el-button size="small" @click="goDetail(order.id)">查看详情</el-button>
          </el-space>
        </div>
      </el-card>
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

    <el-dialog v-model="refundDialogVisible" title="申请退货/退款" width="520px">
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
          <div class="hint">最多3张（演示：上传后会保存URL）</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="refundSubmitting" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="payDialogVisible" title="确认支付" width="520px">
      <el-form label-width="90px">
        <el-form-item label="支付方式">
          <el-radio-group v-model="payForm.payMode">
            <el-radio-button label="THIRD_PARTY">微信/支付宝</el-radio-button>
            <el-radio-button label="COIN">商城币</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="payForm.payMode === 'THIRD_PARTY'" label="渠道">
          <el-radio-group v-model="payForm.payChannel">
            <el-radio-button label="WECHAT">微信</el-radio-button>
            <el-radio-button label="ALIPAY">支付宝</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <div v-if="payForm.payMode === 'THIRD_PARTY'" class="pay-qr-placeholder">第三方支付二维码占位（模拟）</div>
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
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
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
  maxAmount: null
});
const total = ref(0);
const records = ref([]);
const activeTab = ref('ALL');
const listLoading = ref(false);

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
const router = useRouter();

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
  query.orderStatus = undefined;
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

function goDetail(id) {
  router.push(`/order/${id}`);
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
  router.push({ path: `/order/${orderId}`, query: { action: 'review' } });
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
    path: `/order/${order.id}`,
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
.page-title-sub {
  margin-left: 8px;
  font-size: 14px;
  font-weight: normal;
  color: #6b7280;
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.order-list {
  display: grid;
  gap: 12px;
}

.order-card {
  cursor: pointer;
}

.order-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.meta {
  display: flex;
  gap: 12px;
  color: #6b7280;
  font-size: 12px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-top: 1px dashed #e5e7eb;
}

.order-item:first-of-type {
  border-top: 0;
  padding-top: 0;
}

.order-item .name {
  flex: 1;
}

.order-item .price {
  white-space: nowrap;
  color: #374151;
}

.order-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e5e7eb;
}

.order-extra {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f3f4f6;
  color: #4b5563;
  font-size: 12px;
}

.order-extra .line {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 6px;
}

.order-extra .label {
  color: #6b7280;
}

.order-extra .sep {
  width: 1px;
  height: 12px;
  background: #e5e7eb;
  margin: 0 6px;
}

.amount strong {
  color: #ef4444;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.hint {
  color: #6b7280;
  font-size: 12px;
  margin-top: 6px;
}

.pay-qr-placeholder {
  margin: 8px 0;
  height: 180px;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
}
</style>
