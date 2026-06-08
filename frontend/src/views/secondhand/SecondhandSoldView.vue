<template>
  <section class="sold-page">
    <div class="sold-head">
      <div>
        <span class="eyebrow">My Sold Idle Goods</span>
        <h2>我卖出的闲置</h2>
        <p>管理二手成交订单、发货进度和买家沟通。</p>
      </div>
      <div class="head-actions">
        <el-button @click="router.push('/secondhand/mine')">我发布的</el-button>
        <el-button type="primary" @click="router.push('/secondhand/publish')">发布闲置</el-button>
      </div>
    </div>

    <div class="toolbar sold-toolbar">
      <el-input v-model="query.keyword" placeholder="搜索订单号/商品名" clearable @keyup.enter="fetchList(true)" />
      <el-button type="primary" @click="fetchList(true)">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <el-tabs v-model="activeTab" class="status-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="ALL" />
      <el-tab-pane label="待买家付款" name="PENDING_PAY" />
      <el-tab-pane label="待我发货" name="PENDING_SHIP" />
      <el-tab-pane label="已发货" name="SHIPPED" />
      <el-tab-pane label="待买家确认" name="WAIT_BUYER_CONFIRM" />
      <el-tab-pane label="已完成" name="COMPLETED" />
      <el-tab-pane label="售后中" name="AFTER_SALE" />
    </el-tabs>

    <div v-if="loading && records.length === 0" class="sold-card">
      <el-skeleton animated :rows="5" />
    </div>

    <el-empty v-else-if="!loading && records.length === 0" description="暂无卖出的闲置订单" />

    <div v-else v-loading="loading" class="sold-list">
      <article v-for="order in records" :key="order.id" class="sold-card">
        <div class="card-head">
          <div class="status-line">
            <span class="type-tag">二手订单</span>
            <OrderStatusTag
              :status="order.orderStatus"
              :status-name="sellerStatusName(order)"
              :refund-status="order.refundStatus"
              :refund-status-name="order.refundStatusName"
            />
            <span class="order-no">订单号：{{ order.orderNo }}</span>
          </div>
          <span class="time">{{ formatTime(order.createTime) }}</span>
        </div>

        <div class="item-row">
          <img class="cover" :src="toAssetUrl(primaryItem(order).cover) || fallbackCover" :alt="primaryItem(order).productName || '二手商品'" />
          <div class="item-main">
            <strong>{{ primaryItem(order).productName || "二手商品" }}</strong>
            <span>买家：{{ order.buyerName || order.buyerNickname || `用户 ${order.buyerUserId || '-'}` }}</span>
            <span v-if="primaryItem(order).conditionLevel">成色：{{ primaryItem(order).conditionLevel }}</span>
          </div>
          <div class="amount">
            <span>成交价</span>
            <strong>¥{{ Number(order.totalAmount || primaryItem(order).price || 0).toFixed(2) }}</strong>
          </div>
        </div>

        <div class="ship-line">
          <span class="label">发货信息：</span>
          <span>{{ deliveryCopy(order) }}</span>
        </div>

        <div class="card-footer">
          <span class="next-copy">{{ nextCopy(order) }}</span>
          <el-space wrap>
            <el-button v-if="canShip(order)" size="small" type="primary" @click="ship(order)">确认发货</el-button>
            <el-button v-if="canPushLogistics(order)" size="small" @click="pushLogistics(order)">更新物流</el-button>
            <el-button v-if="canViewLogistics(order)" size="small" @click="viewLogistics(order)">查看物流</el-button>
            <el-button size="small" type="success" plain @click="contactBuyer(order)">联系买家</el-button>
            <el-button size="small" @click="goDetail(order)">查看订单</el-button>
            <el-button v-if="Number(order.orderStatus) === 4" size="small" @click="router.push('/my-reviews')">查看评价</el-button>
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
      v-model="shipDialog.visible"
      title="确认发货"
      width="460px"
      append-to-body
      align-center
      destroy-on-close
    >
      <el-form label-width="88px">
        <el-form-item label="发货省份" required>
          <el-select v-model="shipDialog.form.originProvince" filterable placeholder="请选择省份" style="width: 100%">
            <el-option v-for="province in provinceOptions" :key="province" :label="province" :value="province" />
          </el-select>
        </el-form-item>
        <el-form-item label="发货城市">
          <el-input v-model="shipDialog.form.originCity" placeholder="请输入城市" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="shipDialog.form.originDetail" type="textarea" :rows="3" placeholder="请输入详细地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="shipDialog.submitting" @click="confirmShip">确认发货</el-button>
      </template>
    </el-dialog>

  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import { getSoldSecondhandOrderListApi, shipOrderApi } from "@/api/order";
import { pushNextLogisticsApi } from "@/api/logistics";
import OrderStatusTag from "@/components/order/OrderStatusTag.vue";
import { showOrderActionError, showOrderActionSuccess } from "@/utils/orderUi";
import { toAssetUrl } from "@/utils/url";
import { provinceOptions } from "@/utils/provinces";

const router = useRouter();
const loading = ref(false);
const records = ref([]);
const total = ref(0);
const activeTab = ref("ALL");
const fallbackCover = "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=900&q=80";

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  orderStatus: undefined,
  refundStatus: undefined,
});

const shipDialog = reactive({
  visible: false,
  submitting: false,
  order: null,
  form: {
    originProvince: "",
    originCity: "",
    originDetail: "",
  },
});

onMounted(() => fetchList(true));

function tabToQuery(tab) {
  const map = {
    PENDING_PAY: { orderStatus: 0 },
    PENDING_SHIP: { orderStatus: 1 },
    SHIPPED: { orderStatus: 2 },
    WAIT_BUYER_CONFIRM: { orderStatus: 2 },
    COMPLETED: { orderStatus: 4 },
    AFTER_SALE: { refundStatus: 1 },
  };
  return map[tab] || {};
}

async function fetchList(reset = false) {
  if (reset) query.pageNum = 1;
  const statusQuery = tabToQuery(activeTab.value);
  query.orderStatus = statusQuery.orderStatus;
  query.refundStatus = statusQuery.refundStatus;
  loading.value = true;
  try {
    const result = await getSoldSecondhandOrderListApi({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      orderStatus: query.orderStatus,
      refundStatus: query.refundStatus,
    });
    const nextRecords = result.data?.records || [];
    records.value = filterByTab(nextRecords);
    total.value = isLogisticsSplitTab(activeTab.value) ? records.value.length : Number(result.data?.total || 0);
  } finally {
    loading.value = false;
  }
}

function filterByTab(list) {
  if (activeTab.value === "SHIPPED") {
    return list.filter((order) => Number(order?.orderStatus) === 2 && !isArrived(order));
  }
  if (activeTab.value === "WAIT_BUYER_CONFIRM") {
    return list.filter((order) => Number(order?.orderStatus) === 2 && isArrived(order));
  }
  return list;
}

function isLogisticsSplitTab(tabName) {
  return tabName === "SHIPPED" || tabName === "WAIT_BUYER_CONFIRM";
}

function handleTabChange(tabName) {
  activeTab.value = tabName;
  fetchList(true);
}

function resetQuery() {
  query.keyword = "";
  activeTab.value = "ALL";
  fetchList(true);
}

function handlePageChange(pageNum) {
  query.pageNum = pageNum;
  fetchList(false);
}

function handleSizeChange(pageSize) {
  query.pageSize = pageSize;
  query.pageNum = 1;
  fetchList(false);
}

function primaryItem(order) {
  return order?.items?.[0] || {};
}

function sellerStatusName(order) {
  if (Number(order?.refundStatus || 0) === 1) return "售后中";
  if (Number(order?.orderStatus) === 2) {
    return isArrived(order) ? "待买家确认" : "已发货";
  }
  const map = {
    0: "待买家付款",
    1: "待我发货",
    3: "待买家评价",
    4: "已完成",
    9: "已关闭",
  };
  return map[Number(order?.orderStatus)] || order?.orderStatusName || "-";
}

function deliveryCopy(order) {
  if (Number(order?.orderStatus) < 2) return "暂无物流，等待发货";
  if (isArrived(order)) return order?.deliveryNo ? `已送达，物流单号：${order.deliveryNo}` : "已送达，等待买家确认";
  return order?.deliveryNo ? `物流单号：${order.deliveryNo}` : "已发货，物流单号待同步";
}

function nextCopy(order) {
  if (Number(order?.refundStatus || 0) === 1) return "买家已发起售后，请及时处理。";
  const map = {
    0: "等待买家完成付款。",
    1: "买家已付款，请确认发货。",
    2: isArrived(order) ? "商品已送达，等待买家确认收货。" : "已发货，物流运输中。",
    3: "买家已确认收货，等待评价。",
    4: "交易完成。",
    9: "订单已关闭。",
  };
  return map[Number(order?.orderStatus)] || "查看订单了解最新进度。";
}

function canShip(order) {
  return Number(order?.orderStatus) === 1;
}

function canPushLogistics(order) {
  return Number(order?.orderStatus) === 2 && !isArrived(order);
}

function canViewLogistics(order) {
  return Number(order?.orderStatus) >= 2 && Number(order?.orderStatus) !== 9;
}

function isArrived(order) {
  return String(order?.logisticsStatus || "").toUpperCase() === "ARRIVED";
}

async function ship(order) {
  shipDialog.order = order;
  shipDialog.form.originProvince = "";
  shipDialog.form.originCity = "";
  shipDialog.form.originDetail = "";
  shipDialog.visible = true;
}

async function confirmShip() {
  if (!shipDialog.form.originProvince) {
    showOrderActionError(new Error("请选择发货省份"), "发货失败");
    return;
  }
  shipDialog.submitting = true;
  try {
    await shipOrderApi(shipDialog.order.id, {
      originProvince: shipDialog.form.originProvince,
      originCity: shipDialog.form.originCity.trim(),
      originDetail: shipDialog.form.originDetail.trim(),
    });
    showOrderActionSuccess("已发货");
    shipDialog.visible = false;
    await fetchList(false);
  } catch (error) {
    showOrderActionError(error, "发货失败");
  } finally {
    shipDialog.submitting = false;
  }
}

async function pushLogistics(order) {
  try {
    await ElMessageBox.confirm("确认将物流推进到下一节点？", "更新物流", {
      type: "warning",
      confirmButtonText: "确认更新",
      cancelButtonText: "取消",
    });
    await pushNextLogisticsApi(order.id);
    showOrderActionSuccess("物流已更新");
    fetchList(false);
  } catch (error) {
    if (String(error?.message || "").includes("cancel")) return;
    showOrderActionError(error, "更新物流失败");
  }
}

function contactBuyer(order) {
  router.push({
    path: "/messages",
    query: {
      participantId: order.buyerUserId,
      sourceType: "SECONDHAND",
      sourceId: primaryItem(order).productId,
    },
  });
}

function viewLogistics(order) {
  router.push({ path: `/order/${order.id}`, query: { from: "seller", scope: "secondhand", tab: "logistics" } });
}

function goDetail(order) {
  router.push({ path: `/order/${order.id}`, query: { from: "seller", scope: "secondhand" } });
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}
</script>

<style scoped>
.sold-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sold-head,
.sold-card,
.sold-toolbar {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: var(--shadow-soft);
}

.sold-head {
  padding: 20px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.eyebrow {
  color: var(--brand-primary);
  font-size: 12px;
  font-weight: 900;
}

.sold-head h2 {
  margin: 8px 0 6px;
  font-size: 28px;
}

.sold-head p {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 700;
}

.head-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.sold-toolbar {
  padding: 12px;
  display: grid;
  grid-template-columns: minmax(240px, 1fr) auto auto;
  gap: 10px;
}

.sold-list {
  display: grid;
  gap: 12px;
}

.sold-card {
  padding: 16px;
}

.card-head,
.card-footer,
.item-row,
.status-line {
  display: flex;
  gap: 12px;
}

.card-head,
.card-footer {
  justify-content: space-between;
  align-items: center;
}

.status-line {
  flex-wrap: wrap;
  align-items: center;
}

.type-tag {
  border: 1px solid rgba(53, 216, 171, 0.28);
  border-radius: 999px;
  background: var(--brand-mint-weak);
  color: var(--brand-accent-strong);
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 900;
}

.order-no,
.time,
.ship-line,
.next-copy {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.item-row {
  margin-top: 14px;
  padding: 14px 0;
  border-top: 1px solid var(--line-soft);
  border-bottom: 1px solid var(--line-soft);
  align-items: center;
}

.cover {
  width: 84px;
  height: 84px;
  border-radius: 8px;
  object-fit: cover;
  background: var(--surface-soft);
}

.item-main {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 5px;
}

.item-main strong {
  color: var(--text-main);
  font-size: 16px;
}

.item-main span,
.amount span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.amount {
  text-align: right;
}

.amount strong {
  display: block;
  margin-top: 5px;
  color: var(--danger);
  font-size: 20px;
}

.ship-line {
  margin-top: 10px;
}

.ship-line .label {
  color: var(--text-muted);
}

.card-footer {
  margin-top: 12px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 720px) {
  .sold-head,
  .card-head,
  .card-footer,
  .item-row {
    flex-direction: column;
    align-items: stretch;
  }

  .sold-toolbar {
    grid-template-columns: 1fr;
  }

  .amount {
    text-align: left;
  }
}
</style>
