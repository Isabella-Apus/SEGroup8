<template>
  <div class="page-card">
    <div class="head">
      <el-button text @click="goBack">返回</el-button>
      <h2 class="page-title">二手订单详情</h2>
      <span />
    </div>

    <el-skeleton v-if="loading" :rows="7" animated />
    <el-empty v-else-if="!order" description="订单不存在" />

    <template v-else>
      <el-alert
        v-if="autoConfirmTip"
        :title="autoConfirmTip"
        type="warning"
        show-icon
        :closable="false"
        class="auto-confirm-alert"
      />

      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ order.orderStatusName || formatOrderStatus(order.orderStatus) }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">￥{{ displayAmount(order).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="物流单号">{{ order.deliveryNo || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ order.receiverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ order.receiverPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ fullAddress(order) }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(order.createTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-card ref="logisticsCardRef" class="logistics-card" shadow="never">
        <template #header>
          <div class="logistics-head">
            <span>物流轨迹</span>
            <el-tag size="small" type="info">{{ order.logisticsStatus || 'PENDING' }}</el-tag>
          </div>
        </template>
        <el-empty v-if="logisticsTraces.length === 0" description="暂无物流轨迹" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="trace in logisticsTraces"
            :key="trace.id || `${trace.nodeName}-${trace.createTime}`"
            :timestamp="formatTime(trace.createTime)"
            placement="top"
          >
            <div class="trace-node">{{ trace.nodeName }}</div>
            <div class="trace-desc">{{ trace.statusDesc }}</div>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <div class="timeline-block">
        <div class="section-head">
          <div class="section-title">订单进度</div>
          <el-button text type="primary" @click="timelineExpanded = !timelineExpanded">
            {{ timelineExpanded ? '收起时间线' : '展开时间线' }}
          </el-button>
        </div>
        <div class="timeline">
          <div v-for="step in displayedOrderTimeline" :key="step.key" class="timeline-step">
            <div class="timeline-dot">
              <span :class="['timeline-dot__inner', step.status]"></span>
            </div>
            <div class="timeline-content">
              <div class="timeline-label">
                {{ step.label }}
                <el-tag v-if="step.status === 'active'" size="small" type="primary">进行中</el-tag>
              </div>
              <div class="timeline-time">{{ step.time || '-' }}</div>
            </div>
          </div>
        </div>
      </div>

      <el-table :data="order.items || []" border style="margin-top: 14px">
        <el-table-column prop="productName" label="二手商品" min-width="220" />
        <el-table-column label="成交价" width="130">
          <template #default="{ row }">￥{{ Number(row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/secondhand/${row.productId}`)">商品详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="actions">
        <template v-if="mode === 'seller'">
          <el-button v-if="Number(order.orderStatus) === 1" type="success" @click="ship">发货</el-button>
          <el-button v-if="Number(order.orderStatus) === 2" type="primary" plain @click="pushLogistics">更新物流</el-button>
        </template>
        <template v-else>
          <el-button v-if="Number(order.orderStatus) === 0" @click="cancel">取消订单</el-button>
          <el-button v-if="Number(order.orderStatus) === 0" type="primary" @click="pay">立即付款</el-button>
          <el-button v-if="Number(order.orderStatus) === 1" @click="remindShip">提醒发货</el-button>
          <el-button v-if="Number(order.orderStatus) === 2" plain @click="focusLogistics">查看物流</el-button>
          <el-button v-if="Number(order.orderStatus) === 2" type="primary" @click="confirmReceive">确认收货</el-button>
        </template>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import {
  cancelOrderApi,
  confirmReceiveOrderApi,
  getOrderDetailApi,
  getSellerOrderDetailApi,
  payOrderApi,
  remindShipOrderApi,
  shipOrderApi,
} from '@/api/order';
import { getLogisticsTraceApi, pushNextLogisticsApi } from '@/api/logistics';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const order = ref(null);
const mode = computed(() => route.meta?.detailMode || 'buyer');
const logisticsTraces = ref([]);
const logisticsCardRef = ref(null);
const timelineExpanded = ref(false);

onMounted(async () => {
  await fetchDetail();
  await maybeFocusLogistics();
});

async function fetchDetail() {
  loading.value = true;
  try {
    const id = route.params.id;
    const res = mode.value === 'seller' ? await getSellerOrderDetailApi(id) : await getOrderDetailApi(id);
    order.value = res.data || null;
    await fetchLogisticsTrace();
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '订单不存在');
    order.value = null;
    logisticsTraces.value = [];
  } finally {
    loading.value = false;
  }
}

function goBack() {
  router.push(mode.value === 'seller' ? '/secondhand?tab=soldOrders' : '/secondhand?tab=boughtOrders');
}

async function ship() {
  try {
    await ElMessageBox.confirm('确认该二手订单已发货？', '确认发货', { type: 'warning' });
    await shipOrderApi(order.value.id);
    ElMessage.success('发货成功');
    fetchDetail();
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(e?.response?.data?.message || '发货失败');
  }
}

async function pushLogistics() {
  try {
    const res = await pushNextLogisticsApi(order.value.id);
    ElMessage.success(`物流已更新：${res.data?.nodeName || '下一节点'}`);
    await fetchDetail();
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '物流更新失败');
  }
}

async function cancel() {
  try {
    await ElMessageBox.confirm('确认取消该订单？', '取消订单', { type: 'warning' });
    await cancelOrderApi(order.value.id);
    ElMessage.success('已取消订单');
    fetchDetail();
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(e?.response?.data?.message || '取消失败');
  }
}

async function pay() {
  try {
    await payOrderApi(order.value.id, { payMode: 'THIRD_PARTY', payChannel: 'WECHAT' });
    ElMessage.success('支付成功');
    fetchDetail();
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '支付失败');
  }
}

async function remindShip() {
  try {
    await remindShipOrderApi(order.value.id);
    ElMessage.success('已提醒卖家发货');
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提醒失败');
  }
}

async function confirmReceive() {
  try {
    await ElMessageBox.confirm('确认已收到该二手商品？', '确认收货', { type: 'warning' });
    await confirmReceiveOrderApi(order.value.id);
    ElMessage.success('已确认收货');
    fetchDetail();
  } catch (e) {
    if (isCancel(e)) return;
    ElMessage.error(e?.response?.data?.message || '确认收货失败');
  }
}

function displayAmount(row) {
  const payable = Number(row?.payableAmount);
  if (Number.isFinite(payable) && payable > 0) return payable;
  const total = Number(row?.totalAmount);
  return Number.isFinite(total) ? total : 0;
}

async function fetchLogisticsTrace() {
  if (!route.params.id) {
    logisticsTraces.value = [];
    return;
  }
  try {
    const res = await getLogisticsTraceApi(route.params.id);
    const traces = res.data || [];
    logisticsTraces.value = [...traces].sort((a, b) => {
      const at = new Date(a.createTime || 0).getTime();
      const bt = new Date(b.createTime || 0).getTime();
      return bt - at;
    });
    if (order.value && logisticsTraces.value.length > 0) {
      const latest = logisticsTraces.value[0];
      const desc = String(latest?.statusDesc || '');
      order.value.logisticsStatus = /送达|签收/.test(desc) ? 'ARRIVED' : 'IN_TRANSIT';
    }
  } catch (_) {
    logisticsTraces.value = [];
  }
}

async function focusLogistics() {
  await nextTick();
  const el = logisticsCardRef.value?.$el || logisticsCardRef.value;
  if (el?.scrollIntoView) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

async function maybeFocusLogistics() {
  if (route.query.tab === 'logistics') {
    await focusLogistics();
  }
}

const displayedOrderTimeline = computed(() => {
  const steps = orderTimeline.value;
  return timelineExpanded.value ? steps : steps.slice(0, 3);
});

const orderTimeline = computed(() => {
  if (!order.value) return [];
  const o = order.value;
  const orderStatus = Number(o.orderStatus ?? 0);
  const isClosed = orderStatus === 9;
  const steps = [
    { key: 'created', label: '已下单', done: true, time: formatTime(o.createTime) },
    { key: 'paid', label: '已付款', done: !!o.paidTime || orderStatus >= 1, time: o.paidTime ? formatTime(o.paidTime) : '-' },
    { key: 'shipped', label: '已发货', done: !!o.shippedTime || orderStatus >= 2, time: o.shippedTime ? formatTime(o.shippedTime) : '-' },
    { key: 'received', label: '已收货', done: !!o.receivedTime || orderStatus >= 3, time: o.receivedTime ? formatTime(o.receivedTime) : '-' },
    { key: 'completed', label: '已完成', done: !!o.completedTime || orderStatus === 4, time: o.completedTime ? formatTime(o.completedTime) : '-' },
    { key: 'closed', label: '已关闭', done: isClosed || !!o.closedTime, time: o.closedTime ? formatTime(o.closedTime) : '-' },
  ];

  let activeKey = 'created';
  if (isClosed) activeKey = 'closed';
  else if (orderStatus === 4) activeKey = 'completed';
  else if (orderStatus === 3) activeKey = 'received';
  else if (orderStatus === 2) activeKey = 'received';
  else if (orderStatus === 1) activeKey = 'shipped';
  else activeKey = 'paid';

  return steps.map((step) => ({
    ...step,
    status: step.done ? 'done' : step.key === activeKey ? 'active' : 'todo',
  }));
});

const autoConfirmTip = computed(() => {
  const o = order.value;
  if (!o || String(o.logisticsStatus || '').toUpperCase() !== 'ARRIVED' || !o.autoConfirmDeadline) {
    return '';
  }
  const deadline = new Date(o.autoConfirmDeadline).getTime();
  if (!deadline || deadline <= Date.now()) {
    return '订单已达到自动确认收货时间，系统将尽快处理。';
  }
  const diffMs = deadline - Date.now();
  const dayMs = 24 * 60 * 60 * 1000;
  const hourMs = 60 * 60 * 1000;
  const days = Math.floor(diffMs / dayMs);
  const hours = Math.floor((diffMs % dayMs) / hourMs);
  return `系统将于 ${days} 天 ${hours} 小时后自动确认收货`;
});

function fullAddress(row) {
  const parts = [row?.receiverProvince, row?.receiverCity, row?.receiverDetailAddress].filter(Boolean);
  return parts.length ? parts.join(' ') : '-';
}

function formatTime(value) {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value).replace('T', ' ');
  return d.toLocaleString('zh-CN', { hour12: false });
}

function formatOrderStatus(status) {
  const map = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '待评价', 4: '已完成', 9: '已关闭' };
  return map[Number(status)] || '-';
}

function isCancel(e) {
  return e === 'cancel' || e?.toString?.().includes('cancel');
}
</script>

<style scoped>
.head {
  display: grid;
  grid-template-columns: 100px 1fr 100px;
  align-items: center;
  margin-bottom: 18px;
}

.head .page-title {
  text-align: center;
  margin: 0;
}

.actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.auto-confirm-alert,
.logistics-card,
.timeline-block {
  margin-bottom: 14px;
}

.logistics-card {
  margin-top: 14px;
}

.logistics-head,
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.trace-node {
  font-weight: 600;
  color: #111827;
}

.trace-desc {
  margin-top: 4px;
  color: #6b7280;
}

.timeline-block {
  padding: 10px 12px;
  border-radius: 10px;
  background: #fbfbfc;
  border: 1px solid #f3f4f6;
}

.section-title {
  font-weight: 600;
  color: #111827;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
}

.timeline-step {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.timeline-dot {
  width: 18px;
  display: flex;
  justify-content: center;
  padding-top: 2px;
}

.timeline-dot__inner {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  background: #e5e7eb;
}

.timeline-dot__inner.done {
  background: #22c55e;
}

.timeline-dot__inner.active {
  background: #3b82f6;
}

.timeline-content {
  flex: 1;
}

.timeline-label {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  font-weight: 500;
  color: #111827;
}

.timeline-time {
  color: #6b7280;
  font-size: 12px;
  margin-top: 2px;
}
</style>
