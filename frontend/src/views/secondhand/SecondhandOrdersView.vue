<template>
  <div class="page-card">
    <h2 class="page-title">我购买的二手商品</h2>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索订单号/商品名" clearable style="max-width: 320px" @keyup.enter="fetchOrders(true)" />
      <el-select v-model="query.orderStatus" placeholder="订单状态" clearable style="width: 150px">
        <el-option label="待付款" :value="0" />
        <el-option label="待发货" :value="1" />
        <el-option label="待收货" :value="2" />
        <el-option label="待评价" :value="3" />
        <el-option label="已完成" :value="4" />
        <el-option label="已关闭" :value="9" />
      </el-select>
      <el-button type="primary" @click="fetchOrders(true)">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <el-empty v-if="!loading && records.length === 0" description="暂无二手订单" />

    <div v-else v-loading="loading" class="order-list">
      <el-card v-for="order in records" :key="order.id" shadow="hover" class="order-card">
        <div class="order-head">
          <strong>{{ formatOrderStatus(order.orderStatus, order.orderStatusName) }}</strong>
          <span>{{ order.orderNo }}</span>
          <span>{{ formatTime(order.createTime) }}</span>
        </div>

        <div v-for="item in order.items || []" :key="item.id" class="order-item">
          <div>
            <div class="item-name">{{ item.productName }}</div>
            <div class="item-meta">￥{{ Number(item.price || 0).toFixed(2) }} × {{ item.quantity }}</div>
          </div>
          <el-button text type="primary" @click="router.push(`/secondhand/${item.productId}`)">商品详情</el-button>
        </div>

        <div class="order-extra">
          <span>实付：￥{{ Number(order.payableAmount ?? order.totalAmount ?? 0).toFixed(2) }}</span>
          <span>物流单号：{{ order.deliveryNo || '暂无' }}</span>
        </div>

        <div class="actions">
          <el-button v-if="Number(order.orderStatus) === 0" size="small" @click="cancel(order.id)">取消订单</el-button>
          <el-button v-if="Number(order.orderStatus) === 0" size="small" type="primary" @click="pay(order.id)">立即付款</el-button>
          <el-button v-if="Number(order.orderStatus) === 1" size="small" @click="remindShip(order.id)">提醒发货</el-button>
          <el-button v-if="Number(order.orderStatus) === 2" size="small" @click="router.push({ path: `/secondhand/orders/${order.id}`, query: { tab: 'logistics' } })">查看物流</el-button>
          <el-button v-if="Number(order.orderStatus) === 2" size="small" type="primary" @click="confirmReceive(order.id)">确认收货</el-button>
          <el-button size="small" @click="router.push(`/secondhand/orders/${order.id}`)">订单详情</el-button>
        </div>
      </el-card>
    </div>

    <div class="pager-wrap">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNum"
        @current-change="onPageChange"
      />
    </div>

    <el-dialog v-model="payDialogVisible" title="确认支付" width="420px">
      <p>确认支付该二手订单？</p>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="paySubmitting" @click="confirmPay">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import {
  cancelOrderApi,
  confirmReceiveOrderApi,
  getOrderListApi,
  payOrderApi,
  remindShipOrderApi,
} from '@/api/order';

const router = useRouter();
const loading = ref(false);
const records = ref([]);
const total = ref(0);
const payDialogVisible = ref(false);
const paySubmitting = ref(false);
const payTargetId = ref(null);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  orderStatus: undefined,
  productType: 'SECONDHAND',
});

onMounted(() => fetchOrders(true));

async function fetchOrders(reset = false) {
  loading.value = true;
  try {
    if (reset) query.pageNum = 1;
    const res = await getOrderListApi(query);
    records.value = res.data?.records || [];
    total.value = Number(res.data?.total || 0);
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.keyword = '';
  query.orderStatus = undefined;
  fetchOrders(true);
}

function onPageChange(page) {
  query.pageNum = page;
  fetchOrders(false);
}

async function cancel(orderId) {
  try {
    await ElMessageBox.confirm('确认取消该订单？', '取消订单', { type: 'warning' });
    await cancelOrderApi(orderId);
    ElMessage.success('已取消订单');
    fetchOrders(false);
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return;
    ElMessage.error(e?.response?.data?.message || '取消失败');
  }
}

function pay(orderId) {
  payTargetId.value = orderId;
  payDialogVisible.value = true;
}

async function confirmPay() {
  if (!payTargetId.value) return;
  paySubmitting.value = true;
  try {
    await payOrderApi(payTargetId.value, { payMode: 'THIRD_PARTY', payChannel: 'WECHAT' });
    payDialogVisible.value = false;
    ElMessage.success('支付成功');
    fetchOrders(false);
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '支付失败');
  } finally {
    paySubmitting.value = false;
  }
}

async function remindShip(orderId) {
  try {
    await remindShipOrderApi(orderId);
    ElMessage.success('已提醒卖家发货');
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '提醒失败');
  }
}

async function confirmReceive(orderId) {
  try {
    await ElMessageBox.confirm('确认已收到该二手商品？', '确认收货', { type: 'warning' });
    await confirmReceiveOrderApi(orderId);
    ElMessage.success('已确认收货');
    fetchOrders(false);
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return;
    ElMessage.error(e?.response?.data?.message || '确认收货失败');
  }
}

function formatTime(value) {
  if (!value) return '-';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value).replace('T', ' ');
  return d.toLocaleString('zh-CN', { hour12: false });
}

function formatOrderStatus(status, statusName) {
  if (statusName) return statusName;
  const map = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '待评价', 4: '已完成', 9: '已关闭' };
  return map[Number(status)] || '-';
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-head,
.order-extra,
.actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.order-head {
  justify-content: space-between;
  color: #4b5563;
}

.order-item {
  margin-top: 10px;
  padding: 12px 0;
  border-top: 1px solid #eef2f7;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.item-name {
  font-weight: 700;
  color: #111827;
}

.item-meta,
.order-extra {
  color: #6b7280;
  font-size: 13px;
}

.actions {
  justify-content: flex-end;
  margin-top: 12px;
}

.pager-wrap {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
