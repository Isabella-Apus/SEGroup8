<template>
  <div class="page-card aftersale-page">
    <section class="aftersale-head">
      <div>
        <span class="eyebrow">{{ pageCopy.eyebrow }}</span>
        <h2 class="page-title">{{ pageCopy.title }}</h2>
        <p>{{ pageCopy.desc }}</p>
      </div>
      <div class="head-stats">
        <strong>{{ total }}</strong>
        <span>{{ pageCopy.statLabel }}</span>
      </div>
    </section>

    <div class="toolbar aftersale-toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索订单号/商品名"
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-tabs v-model="activeTab" class="status-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="全部售后" name="ALL" />
      <el-tab-pane label="退款中" name="REFUNDING" />
      <el-tab-pane label="已退款" name="REFUNDED" />
      <el-tab-pane label="已拒绝" name="REJECTED" />
    </el-tabs>

    <el-empty v-if="records.length === 0" description="暂无售后订单" />

    <div v-else class="order-list">
      <article v-for="order in records" :key="order.id" class="order-card" @click="goDetail(order.id)">
        <div class="order-card__header">
          <div class="status-cluster">
            <span class="refund-badge" :class="`refund-${order.refundStatus || 0}`">
              {{ order.refundStatusName || '-' }}
            </span>
            <el-tag v-if="order.orderStatusName" size="small" type="info" effect="plain">{{ order.orderStatusName }}</el-tag>
            <span class="no">订单号：{{ order.orderNo }}</span>
          </div>
          <span class="time">{{ formatTime(order.createTime) }}</span>
        </div>

        <div class="order-items">
          <div v-for="item in order.items || []" :key="item.id" class="order-item">
            <strong class="name">{{ item.productName }}</strong>
            <div class="price">￥{{ Number(item.price || 0).toFixed(2) }} × {{ item.quantity }}</div>
          </div>
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
          <div class="amount">
            <span>实付</span>
            <strong>￥{{ Number(order.totalAmount || 0).toFixed(2) }}</strong>
          </div>
          <el-space>
            <el-button size="small" @click="goDetail(order.id)">查看详情</el-button>
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

    <el-dialog v-model="proofPreviewVisible" title="退款凭证" width="720px">
      <el-image :src="toFullImageUrl(proofPreviewUrl)" fit="contain" class="proof-preview-image" />
      <template #footer>
        <el-button type="primary" @click="proofPreviewVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { getOrderListApi } from "@/api/order";
import { onRealtimeEvent } from "@/realtime/realtimeClient";
import { toAssetUrl } from "@/utils/url";

const route = useRoute();
const router = useRouter();
const total = ref(0);
const records = ref([]);
const activeTab = ref("ALL");
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  refundStatus: undefined,
  afterSaleOnly: 1,
  orderType: "NEW"
});

const proofPreviewVisible = ref(false);
const proofPreviewUrl = ref("");
const afterSaleScope = computed(() => {
  const scope = String(route.meta?.afterSaleScope || "").toUpperCase();
  return scope === "SECONDHAND" ? "SECONDHAND" : "NEW";
});
const pageCopy = computed(() => {
  if (afterSaleScope.value === "SECONDHAND") {
    return {
      eyebrow: "Secondhand After-sale",
      title: "二手售后",
      desc: "只跟进二手商城订单的退款、凭证和处理进度。",
      statLabel: "二手售后订单",
    };
  }
  return {
    eyebrow: "New Goods After-sale",
    title: "新品售后",
    desc: "只处理新品商城订单的退款、退货和相关凭证。",
    statLabel: "新品售后订单",
  };
});

onMounted(() => {
  syncAfterSaleScope();
  fetchList();
  unsubscribeRealtime = onRealtimeEvent(handleRealtimeEvent);
});
onBeforeUnmount(() => {
  if (unsubscribeRealtime) unsubscribeRealtime();
});
let unsubscribeRealtime = null;

watch(afterSaleScope, () => {
  activeTab.value = "ALL";
  query.keyword = "";
  query.refundStatus = undefined;
  query.pageNum = 1;
  syncAfterSaleScope();
  fetchList();
});

function syncAfterSaleScope() {
  query.orderType = afterSaleScope.value;
}

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
  syncAfterSaleScope();
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
  return toAssetUrl(url);
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
.aftersale-page {
  background: transparent;
  border: 0;
  box-shadow: none;
  padding: 0;
}

.aftersale-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: stretch;
  margin-bottom: 18px;
  border: 1px solid rgba(229, 221, 210, 0.92);
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(241, 240, 251, 0.92), rgba(247, 239, 229, 0.9) 55%, rgba(220, 239, 233, 0.92)),
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

.aftersale-head p {
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

.head-stats strong {
  display: block;
  font-size: 32px;
  line-height: 1;
}

.head-stats span {
  display: block;
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.aftersale-toolbar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  border-radius: 20px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: var(--shadow-soft);
}

.search-input {
  width: 100%;
}

.status-tabs {
  margin-bottom: 12px;
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

.refund-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  border-radius: 999px;
  padding: 0 11px;
  background: #f7efe5;
  border: 1px solid #ead5c4;
  color: #8a4d2f;
  font-size: 12px;
  font-weight: 900;
}

.refund-badge.refund-1 {
  background: #fff5db;
  border-color: #f2d48a;
  color: #765206;
}

.refund-badge.refund-2 {
  background: #e4f6ef;
  border-color: #9fd8cb;
  color: #146c53;
}

.refund-badge.refund-3 {
  background: #fff0ef;
  border-color: #f3b3aa;
  color: #a63a35;
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

.order-item .name {
  color: var(--text-main);
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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
  gap: 8px;
  flex-wrap: wrap;
}

.order-extra .label {
  color: var(--text-muted);
  font-weight: 800;
}

.refund-timeline {
  margin-top: 12px;
  padding: 14px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(247, 239, 229, 0.9), rgba(220, 239, 233, 0.56));
  border: 1px solid rgba(229, 221, 210, 0.88);
}

.refund-timeline__title {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
  color: var(--text-main);
  font-weight: 900;
}

.refund-timeline__summary {
  color: #8a5a22;
  font-size: 12px;
  font-weight: 700;
}

.refund-timeline__summary.reject {
  color: #a63a35;
}

.refund-timeline__steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.refund-timeline__step {
  position: relative;
  display: grid;
  gap: 5px;
  align-content: flex-start;
  min-height: 72px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.62);
  padding: 12px;
}

.refund-timeline__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d8d2c7;
}

.refund-timeline__dot.done {
  background: var(--brand-accent-strong);
}

.refund-timeline__dot.active {
  background: var(--brand-warm);
}

.refund-timeline__dot.todo {
  background: #d8d2c7;
}

.refund-timeline__label {
  color: var(--text-main);
  font-size: 13px;
  font-weight: 900;
}

.refund-timeline__time {
  color: var(--text-secondary);
  font-size: 12px;
}

.proof-thumb {
  width: 62px;
  height: 62px;
  border-radius: 12px;
  border: 1px solid rgba(229, 221, 210, 0.92);
  cursor: pointer;
  overflow: hidden;
  background: #ffffff;
}

.proof-preview-image {
  width: 100%;
  max-height: 560px;
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

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 860px) {
  .refund-timeline__steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .aftersale-head,
  .order-card__header,
  .order-card__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .head-stats {
    text-align: left;
  }

  .aftersale-toolbar,
  .order-item,
  .refund-timeline__steps {
    grid-template-columns: 1fr;
  }

  .order-item {
    gap: 4px;
    padding: 9px 0;
  }

  .time {
    white-space: normal;
  }
}
</style>

