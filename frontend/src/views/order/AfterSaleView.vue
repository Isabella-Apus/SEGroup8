<template>
  <div class="page-card">
    <h2 class="page-title">退款/售后</h2>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索订单号/商品名"
        clearable
        style="max-width: 360px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部售后" name="ALL" />
      <el-tab-pane label="退款中" name="REFUNDING" />
      <el-tab-pane label="已退款" name="REFUNDED" />
      <el-tab-pane label="已拒绝" name="REJECTED" />
    </el-tabs>

    <el-empty v-if="records.length === 0" description="暂无售后订单" />

    <div v-else class="order-list">
      <el-card v-for="order in records" :key="order.id" shadow="hover" class="order-card" @click="goDetail(order.id)">
        <div class="order-card__header">
          <el-space>
            <el-tag type="danger" size="small">{{ order.refundStatusName || '-' }}</el-tag>
            <el-tag v-if="order.orderStatusName" size="small" type="info">{{ order.orderStatusName }}</el-tag>
          </el-space>
          <div class="meta">
            <span class="no">订单号：{{ order.orderNo }}</span>
            <span class="time">{{ formatTime(order.createTime) }}</span>
          </div>
        </div>

        <div v-for="item in order.items || []" :key="item.id" class="order-item">
          <div class="name">{{ item.productName }}</div>
          <div class="price">￥{{ Number(item.price || 0).toFixed(2) }} × {{ item.quantity }}</div>
        </div>

        <div class="order-extra" @click.stop>
          <div class="line">
            <span class="label">原因：</span>
            <span class="value">{{ order.refundReason || '-' }}</span>
          </div>
          <div class="line">
            <span class="label">凭证：</span>
            <el-space wrap>
              <el-tag v-if="proofList(order).length === 0" size="small" type="info">无</el-tag>
              <el-image
                v-for="(u, idx) in proofList(order)"
                :key="u + idx"
                :src="toFullImageUrl(u)"
                fit="cover"
                class="proof-thumb"
                :alt="`凭证${idx + 1}`"
                @click="openProofPreview(u)"
              />
            </el-space>
          </div>
        </div>

        <div v-if="order.refundStatus > 0" class="refund-timeline" @click.stop>
          <div class="refund-timeline__title">
            售后进度
            <span v-if="order.refundStatus === 2" class="refund-timeline__summary">
              {{ order.refundDecisionSource === 'SELLER' ? '卖家' : '平台' }}已同意退货：{{ order.refundDecisionRemark || '已退款' }}
            </span>
            <span v-else-if="order.refundStatus === 3" class="refund-timeline__summary reject">
              {{ order.refundDecisionSource === 'SELLER' ? '卖家' : '平台' }}已拒绝退货：{{ order.refundDecisionRemark || '退款被拒绝' }}
            </span>
          </div>
          <div class="refund-timeline__steps">
            <div v-for="step in getRefundTimeline(order)" :key="step.key" class="refund-timeline__step">
              <span class="refund-timeline__dot" :class="step.status"></span>
              <span class="refund-timeline__label">{{ step.label }}</span>
              <span class="refund-timeline__time">{{ step.time || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="order-card__footer" @click.stop>
          <div class="amount">实付：<strong>￥{{ Number(order.totalAmount || 0).toFixed(2) }}</strong></div>
          <el-space>
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

    <el-dialog v-model="proofPreviewVisible" title="退款凭证" width="720px">
      <el-image :src="toFullImageUrl(proofPreviewUrl)" fit="contain" class="proof-preview-image" />
      <template #footer>
        <el-button type="primary" @click="proofPreviewVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { getOrderListApi } from "@/api/order";
import { onRealtimeEvent } from "@/realtime/realtimeClient";

const router = useRouter();
const total = ref(0);
const records = ref([]);
const activeTab = ref("ALL");
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  refundStatus: undefined
});

const proofPreviewVisible = ref(false);
const proofPreviewUrl = ref("");

onMounted(() => {
  fetchList();
  unsubscribeRealtime = onRealtimeEvent(handleRealtimeEvent);
});
onBeforeUnmount(() => {
  if (unsubscribeRealtime) unsubscribeRealtime();
});
let unsubscribeRealtime = null;

async function fetchList() {
  const res = await getOrderListApi(query);
  records.value = res.data?.records || [];
  total.value = res.data?.total || 0;
}

function tabToRefundStatus(tab) {
  if (tab === "REFUNDING") return 1;
  if (tab === "REFUNDED") return 2;
  if (tab === "REJECTED") return 3;
  return undefined;
}

function handleTabChange() {
  query.pageNum = 1;
  query.refundStatus = tabToRefundStatus(activeTab.value);
  fetchList();
}

function handleSearch() {
  query.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.keyword = "";
  activeTab.value = "ALL";
  query.refundStatus = undefined;
  query.pageNum = 1;
  fetchList();
}

function handlePageChange(pageNum) {
  query.pageNum = pageNum;
  fetchList();
}

function handleSizeChange(pageSize) {
  query.pageSize = pageSize;
  query.pageNum = 1;
  fetchList();
}

function goDetail(id) {
  router.push(`/order/${id}`);
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

function toFullImageUrl(url) {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://')) return url;
  const normalized = url.startsWith('/') ? url : `/${url}`;
  return `http://127.0.0.1:8080${normalized}`;
}

function proofList(order) {
  if (!order?.refundProofUrls) return [];
  return String(order.refundProofUrls)
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

function openProofPreview(url) {
  proofPreviewUrl.value = url;
  proofPreviewVisible.value = true;
}

function getRefundTimeline(order) {
  const rs = order?.refundStatus ?? 0;
  if (rs <= 0) return [];

  const activeKey = rs === 1 ? "processing" : rs === 2 ? "refunded" : "rejected";
  const applyTime = formatTime(order?.refundApplyTime);
  const decisionTime = formatTime(order?.refundDecisionTime);

  const steps = [
    { key: "apply", label: "买家申请退货", done: true, time: applyTime },
    { key: "processing", label: "退款中", done: rs >= 1, time: rs === 1 ? "" : decisionTime },
    { key: "refunded", label: "已退款", done: rs >= 2, time: rs >= 2 ? decisionTime : "" },
    { key: "rejected", label: "退款被拒绝", done: rs >= 3, time: rs >= 3 ? decisionTime : "" }
  ];

  return steps.map((s) => {
    const status = s.done ? "done" : s.key === activeKey ? "active" : "todo";
    return { ...s, status };
  });
}

function handleRealtimeEvent(event) {
  const type = event?.detail?.eventType;
  if (type === "AFTER_SALE_UPDATED" || type === "ORDER_STATUS_UPDATED") {
    fetchList();
  }
}
</script>

<style scoped>
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

.order-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e5e7eb;
}

.refund-timeline {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
}

.refund-timeline__title {
  font-weight: 600;
  color: #9a3412;
  margin-bottom: 8px;
}

.refund-timeline__summary {
  margin-left: 8px;
  font-weight: 400;
  color: #b45309;
}

.refund-timeline__summary.reject {
  color: #b91c1c;
}

.refund-timeline__steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.refund-timeline__step {
  display: flex;
  gap: 8px;
  align-items: center;
}

.refund-timeline__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #e5e7eb;
  flex: 0 0 auto;
}

.refund-timeline__dot.done {
  background: #f97316;
}

.refund-timeline__dot.active {
  background: #ef4444;
}

.refund-timeline__dot.todo {
  background: #e5e7eb;
}

.refund-timeline__label {
  font-size: 13px;
  color: #9a3412;
}

.proof-thumb {
  width: 62px;
  height: 62px;
  border-radius: 8px;
  border: 1px solid #f3f4f6;
  cursor: pointer;
  overflow: hidden;
}

.proof-preview-image {
  width: 100%;
  max-height: 560px;
}

.refund-timeline__time {
  font-size: 12px;
  color: #b45309;
}

.amount strong {
  color: #ef4444;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>

