<template>
  <div class="page-card">
    <h2 class="page-title">卖家订单管理</h2>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="订单号/商品名" clearable style="max-width: 340px" />
      <el-select v-model="query.orderStatus" placeholder="订单状态" clearable style="width: 160px">
        <el-option label="待付款" :value="0" />
        <el-option label="待发货" :value="1" />
        <el-option label="待收货" :value="2" />
        <el-option label="待评价" :value="3" />
        <el-option label="已完成" :value="4" />
        <el-option label="已关闭" :value="9" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-empty v-if="records.length === 0" description="暂无卖家相关订单" />

    <div v-else class="order-list">
      <el-card v-for="order in records" :key="order.id" shadow="hover" class="order-card">
        <div class="order-card__header">
          <el-space>
            <el-tag size="small" :type="order.orderStatus === 1 ? 'warning' : 'info'">{{ order.orderStatusName || '-' }}</el-tag>
            <el-tag v-if="order.refundStatus > 0" type="danger" size="small">{{ order.refundStatusName }}</el-tag>
          </el-space>
          <div class="meta">
            <span>订单号：{{ order.orderNo }}</span>
            <span>{{ formatTime(order.createTime) }}</span>
          </div>
        </div>
        <div v-for="item in order.items || []" :key="item.id" class="order-item">
          <div>
            {{ item.productName }}
            <el-tag size="small" style="margin-left: 8px" :type="item.productType === 'SECONDHAND' ? 'warning' : 'info'">
              {{ item.productType === 'SECONDHAND' ? '二手' : '新品' }}
            </el-tag>
            <el-tag v-if="item.conditionLevel" size="small" type="success" style="margin-left: 6px">
              {{ item.conditionLevel }}
            </el-tag>
          </div>
          <div>￥{{ Number(item.price || 0).toFixed(2) }} × {{ item.quantity }}</div>
        </div>
        <div class="order-card__footer">
          <div>总金额：<strong>￥{{ Number(order.totalAmount || 0).toFixed(2) }}</strong></div>
          <el-space>
            <el-button v-if="order.orderStatus === 1" size="small" type="primary" @click="ship(order.id)">去发货</el-button>
            <el-button v-if="order.refundStatus === 1" size="small" type="success" @click="approveRefund(order.id)">同意退货</el-button>
            <el-button v-if="order.refundStatus === 1" size="small" type="danger" plain @click="rejectRefund(order.id)">拒绝退货</el-button>
            <el-button size="small" @click="goDetail(order.id)">查看详情</el-button>
          </el-space>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { approveRefundOrderApi, getSellerOrderListApi, rejectRefundOrderApi, shipOrderApi } from "@/api/order";
import { onRealtimeEvent } from "@/realtime/realtimeClient";

const query = reactive({
  pageNum: 1,
  pageSize: 20,
  orderStatus: undefined,
  keyword: ""
});
const records = ref([]);
const router = useRouter();

onMounted(() => {
  fetchList();
  unsubscribeRealtime = onRealtimeEvent(handleRealtimeEvent);
});
onBeforeUnmount(() => {
  if (unsubscribeRealtime) unsubscribeRealtime();
});
let unsubscribeRealtime = null;

async function fetchList() {
  const result = await getSellerOrderListApi(query);
  records.value = result.data?.records || [];
}

function handleSearch() {
  query.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.pageNum = 1;
  query.keyword = "";
  query.orderStatus = undefined;
  fetchList();
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

async function ship(orderId) {
  await shipOrderApi(orderId);
  ElMessage.success("发货成功");
  fetchList();
}

async function approveRefund(orderId) {
  await approveRefundOrderApi(orderId);
  ElMessage.success("已同意退货");
  fetchList();
}

async function rejectRefund(orderId) {
  await rejectRefundOrderApi(orderId);
  ElMessage.success("已拒绝退货");
  fetchList();
}

function goDetail(orderId) {
  router.push({ path: `/order/${orderId}`, query: { from: "seller" } });
}

function handleRealtimeEvent(event) {
  const type = event?.detail?.eventType;
  if (type === "ORDER_STATUS_UPDATED" || type === "AFTER_SALE_UPDATED" || type === "LOGISTICS_UPDATED" || type === "ORDER_REMIND_SHIP") {
    fetchList();
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.order-list {
  display: grid;
  gap: 12px;
}
.order-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-top: 1px dashed #e5e7eb;
}
.order-card__header,
.order-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.meta {
  color: #6b7280;
  display: flex;
  gap: 10px;
  font-size: 12px;
}
.order-card__footer strong {
  color: #ef4444;
}
</style>
