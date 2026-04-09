<template>
  <div class="page-card">
    <h2 class="page-title">订单管理</h2>

    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="订单号/商品名" clearable style="max-width: 320px" />
      <el-select v-model="query.orderStatus" placeholder="订单状态" clearable style="width: 160px">
        <el-option label="待付款" :value="0" />
        <el-option label="待发货" :value="1" />
        <el-option label="待收货" :value="2" />
        <el-option label="待评价" :value="3" />
        <el-option label="已完成" :value="4" />
        <el-option label="已关闭" :value="9" />
      </el-select>
      <el-select v-model="query.refundStatus" placeholder="退款状态" clearable style="width: 170px">
        <el-option label="退款中" :value="1" />
        <el-option label="已退款" :value="2" />
        <el-option label="退款被拒绝" :value="3" />
      </el-select>
      <el-date-picker
        v-model="query.startTime"
        type="datetime"
        placeholder="开始时间"
        value-format="x"
        clearable
        style="width: 200px"
      />
      <el-date-picker
        v-model="query.endTime"
        type="datetime"
        placeholder="结束时间"
        value-format="x"
        clearable
        style="width: 200px"
      />
      <el-input-number v-model="query.minAmount" :min="0" :precision="2" placeholder="最小金额" style="width: 160px" />
      <el-input-number v-model="query.maxAmount" :min="0" :precision="2" placeholder="最大金额" style="width: 160px" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
      <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleBatchClose">
        批量关闭
      </el-button>
    </div>

    <el-table :data="records" border @selection-change="handleSelectionChange" @row-click="openDetail">
      <el-table-column type="selection" width="48" />
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="orderNo" label="订单号" min-width="220" />
      <el-table-column prop="buyerUserId" label="买家ID" width="110" />
      <el-table-column prop="orderStatusName" label="状态" width="120" />
      <el-table-column prop="totalAmount" label="金额" width="120">
        <template #default="scope">￥{{ Number(scope.row.totalAmount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="180">
        <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button link type="primary" @click.stop="openDetail(scope.row)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

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

    <el-drawer v-model="drawerVisible" title="订单详情" size="50%">
      <el-skeleton v-if="drawerLoading" :rows="6" animated />
      <template v-else-if="drawerOrder">
        <OrderSummaryCard :stage="orderStageSummary" :next-action="orderNextActionSummary" />
        <el-descriptions :column="2" border style="margin-bottom: 12px">
          <el-descriptions-item label="订单状态">
            <OrderStatusTag
              :status="drawerOrder.orderStatus"
              :status-name="drawerOrder.orderStatusName"
              :refund-status="drawerOrder.refundStatus"
              :refund-status-name="drawerOrder.refundStatusName"
            />
          </el-descriptions-item>
          <el-descriptions-item label="订单号">{{ drawerOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="买家ID">{{ drawerOrder.buyerUserId ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="交易方式">{{ drawerOrder.tradeMode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatTime(drawerOrder.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ formatTime(drawerOrder.paidTime) }}</el-descriptions-item>
          <el-descriptions-item label="发货时间">{{ formatTime(drawerOrder.shippedTime) }}</el-descriptions-item>
          <el-descriptions-item label="收货时间">{{ formatTime(drawerOrder.receivedTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatTime(drawerOrder.completedTime) }}</el-descriptions-item>
          <el-descriptions-item label="关闭时间">{{ formatTime(drawerOrder.closedTime) }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">￥{{ Number(drawerOrder.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ drawerOrder.payMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物流单号">{{ drawerOrder.deliveryNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货地址">
            {{ drawerOrder.receiverName || '-' }} {{ drawerOrder.receiverPhone || '' }}
            {{ drawerOrder.receiverProvince || '' }}{{ drawerOrder.receiverCity || '' }}{{ drawerOrder.receiverDetailAddress || '' }}
          </el-descriptions-item>
          <el-descriptions-item label="退款状态">{{ drawerOrder.refundStatusName || '无' }}</el-descriptions-item>
          <el-descriptions-item label="退款原因">{{ drawerOrder.refundReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="退款凭证">
            <el-space wrap>
              <el-tag v-if="proofList.length === 0" size="small" type="info">无</el-tag>
              <el-image
                v-for="(u, idx) in proofList"
                :key="u + idx"
                :src="toFullImageUrl(u)"
                fit="cover"
                class="proof-thumb"
                :alt="`凭证${idx + 1}`"
                @click="openProofPreview(u)"
              />
            </el-space>
          </el-descriptions-item>
          <el-descriptions-item v-if="drawerOrder.refundStatus > 0" label="申请时间">{{ formatTime(drawerOrder.refundApplyTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="drawerOrder.refundStatus > 0" label="决策时间">{{ formatTime(drawerOrder.refundDecisionTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="drawerOrder.refundStatus > 0" label="审核人">
            <span v-if="drawerOrder.refundDecisionUserId">
              {{ drawerOrder.refundDecisionUserName || '管理员' }} (ID: {{ drawerOrder.refundDecisionUserId }})
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="drawerOrder.refundStatus > 0" label="审核意见">{{ drawerOrder.refundDecisionRemark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <OrderTimeline title="订单进度" :steps="orderTimeline" :expanded="timelineExpanded" :default-count="3" @toggle="timelineExpanded = !timelineExpanded" />
        <template v-if="drawerOrder.refundStatus > 0">
          <OrderTimeline title="售后进度" :steps="refundTimeline" :expanded="timelineExpanded" :default-count="2" active-tag-type="danger" />
        </template>
        <el-table :data="drawerOrder.items || []" border>
          <el-table-column prop="productName" label="商品" min-width="220" />
          <el-table-column prop="productType" label="类型" width="120" />
          <el-table-column prop="conditionLevel" label="成色" width="120" />
          <el-table-column prop="price" label="单价" width="120">
            <template #default="scope">￥{{ Number(scope.row.price || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="100" />
        </el-table>

        <div class="timeline-block" style="margin-top: 12px">
          <div class="section-title section-title--row">
            <span>售后操作记录</span>
            <el-button
              text
              type="primary"
              @click="afterSaleLogsVisible = !afterSaleLogsVisible; if (afterSaleLogsVisible) ensureAfterSaleLogs();"
            >
              {{ afterSaleLogsVisible ? "收起记录" : "展开记录" }}
            </el-button>
          </div>
          <el-table v-if="afterSaleLogsVisible" :data="afterSaleLogs" border size="small">
            <el-table-column prop="createTime" label="时间" width="180">
              <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
            </el-table-column>
            <el-table-column prop="operatorRole" label="角色" width="120" />
            <el-table-column prop="operatorUserId" label="操作者ID" width="120" />
            <el-table-column prop="action" label="动作" width="120" />
            <el-table-column prop="remark" label="说明/意见" min-width="220" />
          </el-table>
        </div>

        <div class="refund-actions" v-if="drawerOrder.refundStatus === 1">
          <div class="refund-actions__title">售后处理</div>
          <el-space>
            <el-button type="primary" :loading="approveRefundLoading" @click="approveRefund" size="small">
              同意退货
            </el-button>
            <el-button type="danger" :loading="rejectRefundLoading" @click="rejectRefund" size="small">
              拒绝退货
            </el-button>
          </el-space>
        </div>
      </template>
      <el-empty v-else description="暂无数据" />
    </el-drawer>

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
import {
  approveAdminRefundOrderApi,
  batchCloseAdminOrderApi,
  getAdminOrderAfterSaleLogsApi,
  getAdminOrderDetailApi,
  getAdminOrderListApi,
  rejectAdminRefundOrderApi
} from "@/api/adminOrder";
import { confirmOrderAction, showOrderActionError, showOrderActionSuccess } from "@/utils/orderUi";
import { uiDialog } from "@/utils/uiFeedback";
import OrderStatusTag from "@/components/order/OrderStatusTag.vue";
import OrderSummaryCard from "@/components/order/OrderSummaryCard.vue";
import OrderTimeline from "@/components/order/OrderTimeline.vue";
import { useAdminOrderProgress } from "@/composables/useAdminOrderProgress";
import { onRealtimeEvent } from "@/realtime/realtimeClient";

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderStatus: undefined,
  refundStatus: undefined,
  startTime: null,
  endTime: null,
  minAmount: null,
  maxAmount: null,
  keyword: ""
});

const total = ref(0);
const records = ref([]);
const selectedIds = ref([]);

const drawerVisible = ref(false);
const drawerLoading = ref(false);
const drawerOrder = ref(null);
const afterSaleLogs = ref([]);
const afterSaleLogsLoaded = ref(false);
const afterSaleLogsVisible = ref(false);

const proofPreviewVisible = ref(false);
const proofPreviewUrl = ref("");
const approveRefundLoading = ref(false);
const rejectRefundLoading = ref(false);
const timelineExpanded = ref(false);
const {
  proofList,
  toFullImageUrl,
  orderTimeline,
  refundTimeline,
  orderStageSummary,
  orderNextActionSummary
} = useAdminOrderProgress(drawerOrder);

onMounted(() => {
  fetchList();
  unsubscribeRealtime = onRealtimeEvent(handleRealtimeEvent);
});
onBeforeUnmount(() => {
  if (unsubscribeRealtime) unsubscribeRealtime();
});
let unsubscribeRealtime = null;

async function fetchList() {
  const result = await getAdminOrderListApi(query);
  records.value = result.data?.records || [];
  total.value = result.data?.total || 0;
}

function handleSearch() {
  query.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.pageNum = 1;
  query.keyword = "";
  query.orderStatus = undefined;
  query.refundStatus = undefined;
  query.startTime = null;
  query.endTime = null;
  query.minAmount = null;
  query.maxAmount = null;
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

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ");
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map((row) => row.id);
}

async function openDetail(row) {
  drawerVisible.value = true;
  drawerLoading.value = true;
  drawerOrder.value = null;
  afterSaleLogs.value = [];
  afterSaleLogsLoaded.value = false;
  afterSaleLogsVisible.value = false;
  try {
    const res = await getAdminOrderDetailApi(row.id);
    drawerOrder.value = res.data;
  } finally {
    drawerLoading.value = false;
  }
}

async function ensureAfterSaleLogs() {
  if (afterSaleLogsLoaded.value || !drawerOrder.value?.id) return;
  const logsRes = await getAdminOrderAfterSaleLogsApi(drawerOrder.value.id);
  afterSaleLogs.value = logsRes.data || [];
  afterSaleLogsLoaded.value = true;
}

async function handleBatchClose() {
  if (selectedIds.value.length === 0) return;
  try {
    await confirmOrderAction({
      title: "确认批量关闭",
      message: `确认关闭已选 ${selectedIds.value.length} 个订单？关闭后订单不可恢复。`,
      confirmButtonText: "确认关闭"
    });
    const res = await batchCloseAdminOrderApi(selectedIds.value);
    const successIds = res.data?.successIds || [];
    const failedItems = res.data?.failedItems || [];
    const count = successIds.length;
    if (failedItems.length === 0) {
      showOrderActionSuccess(`批量关闭完成，成功 ${count} 单`);
    } else {
      const detail = failedItems
        .slice(0, 5)
        .map((item) => `#${item.orderId} ${item.reason}${item.currentStatusName && item.currentStatusName !== "-" ? `（当前：${item.currentStatusName}）` : ""}`)
        .join("\n");
      await uiDialog.alert(
        `成功 ${count} 单，失败 ${failedItems.length} 单。\n\n失败明细（最多展示5条）：\n${detail}`,
        "批量关闭结果",
        { type: "warning", confirmButtonText: "知道了" }
      );
    }
    selectedIds.value = [];
    fetchList();
  } catch (error) {
    if (String(error?.message || "").includes("cancel")) return;
    showOrderActionError(error, "批量关闭失败");
  }
}

async function approveRefund() {
  if (!drawerOrder.value?.id) return;
  try {
    const { value } = await uiDialog.prompt(
      `请输入审核意见（可选）\n订单号：${drawerOrder.value.orderNo || '-'}\n退款原因：${drawerOrder.value.refundReason || '-'}`,
      "确认同意退货并完成退款",
      {
        type: "warning",
        confirmButtonText: "同意退货并退款",
        cancelButtonText: "取消",
        inputPlaceholder: "不填则自动记录为同意退货",
        inputPattern: /^.{0,255}$/,
        inputErrorMessage: "审核意见最多255字"
      }
    );
    approveRefundLoading.value = true;
    const res = await approveAdminRefundOrderApi(drawerOrder.value.id, { remark: value?.trim() || "" });
    drawerOrder.value = res.data;
    showOrderActionSuccess("已同意退货并完成退款");
    fetchList();
  } catch (e) {
    // 用户取消确认时也会进入 catch，这里只提示真正的失败
    const msg = e?.message || e?.response?.data?.message;
    if (msg && String(msg).includes("cancel")) return;
    if (!msg) {
      // Element Plus 取消时通常没有 response.message，直接忽略即可
      return;
    }
    showOrderActionError(e, "同意退货失败");
  } finally {
    approveRefundLoading.value = false;
  }
}

async function rejectRefund() {
  if (!drawerOrder.value?.id) return;
  try {
    const { value } = await uiDialog.prompt(
      `请输入审核意见（可选）\n订单号：${drawerOrder.value.orderNo || '-'}\n退款原因：${drawerOrder.value.refundReason || '-'}`,
      "确认拒绝退货申请",
      {
        type: "warning",
        confirmButtonText: "拒绝退货",
        cancelButtonText: "取消",
        inputPlaceholder: "不填则自动记录为拒绝退货",
        inputPattern: /^.{0,255}$/,
        inputErrorMessage: "审核意见最多255字"
      }
    );
    rejectRefundLoading.value = true;
    const res = await rejectAdminRefundOrderApi(drawerOrder.value.id, { remark: value?.trim() || "" });
    drawerOrder.value = res.data;
    showOrderActionSuccess("已拒绝退货申请");
    fetchList();
  } catch (e) {
    const msg = e?.message || e?.response?.data?.message;
    if (msg && String(msg).includes("cancel")) return;
    if (!msg) return;
    showOrderActionError(e, "拒绝退货失败");
  } finally {
    rejectRefundLoading.value = false;
  }
}

function openProofPreview(url) {
  proofPreviewUrl.value = url;
  proofPreviewVisible.value = true;
}

function handleRealtimeEvent(event) {
  const type = event?.detail?.eventType;
  if (type === "ORDER_STATUS_UPDATED" || type === "AFTER_SALE_UPDATED" || type === "LOGISTICS_UPDATED" || type === "ORDER_REMIND_SHIP") {
    fetchList();
    if (drawerOrder.value?.id) {
      getAdminOrderDetailApi(drawerOrder.value.id).then((res) => {
        drawerOrder.value = res.data;
      });
    }
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

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
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

.timeline-block {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fbfbfc;
  border: 1px solid #f3f4f6;
}

.section-title--row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

</style>

