<template>
  <div class="page-card">
    <div class="header">
      <el-button text @click="goBack">返回</el-button>
      <h2 class="page-title" style="margin: 0">订单详情</h2>
      <div style="width: 60px" />
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <template v-else-if="order">
      <OrderSummaryCard :stage="orderStageSummary" :next-action="orderNextActionSummary" />
      <el-descriptions :column="2" border style="margin-bottom: 12px">
        <el-descriptions-item label="订单状态">
          <OrderStatusTag
            :status="order.orderStatus"
            :status-name="order.orderStatusName"
            :refund-status="order.refundStatus"
            :refund-status-name="order.refundStatusName"
          />
        </el-descriptions-item>
        <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="退货状态">{{ order.refundStatusName || '无' }}</el-descriptions-item>
        <el-descriptions-item label="退货原因">{{ order.refundReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="退货凭证">
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
        <el-descriptions-item label="下单时间">{{ formatTime(order.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">￥{{ Number(order.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="autoConfirmTip"
        :title="autoConfirmTip"
        type="warning"
        show-icon
        :closable="false"
        class="auto-confirm-alert"
      />

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
            :key="trace.id"
            :timestamp="formatTime(trace.createTime)"
            placement="top"
          >
            <div class="trace-node">{{ trace.nodeName }}</div>
            <div class="trace-desc">{{ trace.statusDesc }}</div>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <OrderTimeline title="订单进度" :steps="orderTimeline" :expanded="timelineExpanded" :default-count="3" @toggle="timelineExpanded = !timelineExpanded" />
      <template v-if="order.refundStatus > 0">
        <OrderTimeline title="售后进度" :steps="refundTimeline" :expanded="timelineExpanded" :default-count="2" active-tag-type="danger" />
      </template>

      <el-table :data="order.items || []" border>
        <el-table-column prop="productName" label="商品" min-width="240" />
        <el-table-column label="类型" width="120">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.productType === 'SECONDHAND' ? 'warning' : 'info'">
              {{ scope.row.productType === 'SECONDHAND' ? '二手' : '新品' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成色" width="120">
          <template #default="scope">{{ scope.row.conditionLevel || '-' }}</template>
        </el-table-column>
        <el-table-column prop="price" label="单价" width="120">
          <template #default="scope">￥{{ Number(scope.row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column v-if="!isSellerView" label="鎿嶄綔" width="140">
          <template #default="scope">
            <el-button v-if="scope.row.sellerUserId" size="small" type="success" plain @click="contactSellerByItem(scope.row)">联系卖家</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!isSellerView" class="actions">
        <el-space wrap>
          <el-button v-if="order.orderStatus === 0" type="primary" @click="pay">立即付款</el-button>
          <el-button v-if="order.orderStatus === 0" @click="cancel">取消订单</el-button>
          <el-button v-if="order.orderStatus === 2" type="primary" @click="confirmReceive">确认收货</el-button>
          <el-button v-if="order.orderStatus === 3" type="primary" @click="openReviewDialog">去评价</el-button>
          <el-button v-if="canRefund(order.orderStatus, order.refundStatus)" type="danger" plain @click="openRefundDialog">申请退货</el-button>
          <!-- 付款后即可举报/拉黑卖家 -->
          <template v-if="order.orderStatus >= 1 && orderSellerUserId">
            <el-button type="warning" plain size="small" @click="openOrderReportDialog">举报卖家</el-button>
            <el-button
              v-if="!isSellerBlocked"
              type="danger"
              plain
              size="small"
              @click="handleOrderBlock"
            >拉黑卖家</el-button>
            <el-button
              v-else
              type="info"
              plain
              size="small"
              @click="handleOrderUnblock"
            >取消拉黑</el-button>
          </template>
        </el-space>
      </div>
      <div v-else class="actions">
        <el-space>
          <el-button v-if="order.orderStatus === 1" type="primary" @click="shipBySeller">去发货</el-button>
          <el-button v-if="order.refundStatus === 1" type="success" @click="approveRefundBySeller">同意退货</el-button>
          <el-button v-if="order.refundStatus === 1" type="danger" plain @click="rejectRefundBySeller">拒绝退货</el-button>
          <!-- 卖家可举报/拉黑买家 -->
          <template v-if="orderBuyerUserId">
            <el-button type="warning" plain size="small" @click="openSellerReportDialog">举报买家</el-button>
            <el-button
              v-if="!isBuyerBlocked"
              type="danger"
              plain
              size="small"
              @click="handleSellerBlock"
            >拉黑买家</el-button>
            <el-button
              v-else
              type="info"
              plain
              size="small"
              @click="handleSellerUnblock"
            >取消拉黑</el-button>
          </template>
        </el-space>
      </div>
    </template>

    <el-empty v-else description="订单不存在" />

    <el-dialog v-if="!isSellerView" v-model="reviewDialogVisible" title="提交评价（按商品逐条）" width="720px">
      <el-alert type="info" show-icon title="每个商品都可以单独评价，更像淘宝" style="margin-bottom: 10px" />
      <el-table :data="reviewItems" border>
        <el-table-column prop="productName" label="商品" min-width="220" />
        <el-table-column label="评分" width="200">
          <template #default="scope">
            <el-rate v-model="scope.row.score" />
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="260">
          <template #default="scope">
            <el-input v-model="scope.row.content" type="textarea" :rows="2" maxlength="500" show-word-limit />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-if="!isSellerView"
      v-model="refundDialogVisible"
      title="申请退货/退款"
      width="520px"
      align-center
      append-to-body
    >
      <el-form label-width="90px">
        <el-form-item label="退款方式">
          <el-radio-group v-model="refundForm.mode">
            <el-radio-button label="ONLY_REFUND" :disabled="!canOnlyRefund">仅退款</el-radio-button>
            <el-radio-button label="RETURN_REFUND">退货退款</el-radio-button>
          </el-radio-group>
        </el-form-item>
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

    <el-dialog v-model="proofPreviewVisible" title="退款凭证" width="720px">
      <el-image :src="toFullImageUrl(proofPreviewUrl)" fit="contain" class="proof-preview-image" />
      <template #footer>
        <el-button type="primary" @click="proofPreviewVisible = false">知道了</el-button>
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

    <!-- ===== 举报卖家弹窗（订单内） ===== -->
    <el-dialog v-model="orderReportDialogVisible" title="举报卖家" width="480px" append-to-body>
      <el-form :model="orderReportForm" label-width="90px" @submit.prevent>
        <el-form-item label="举报类型" required>
          <el-select v-model="orderReportForm.reasonType" style="width:100%">
            <el-option label="诈骗/虚假交易" value="FRAUD" />
            <el-option label="商品与描述不符" value="FAKE_ITEM" />
            <el-option label="态度恶劣/骚扰" value="BAD_ATTITUDE" />
            <el-option label="恶意退款" value="REFUND_ABUSE" />
            <el-option label="刷单/广告骚扰" value="SPAM" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="orderReportForm.reasonDesc" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderReportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="orderReportSubmitting" @click="handleOrderReportSubmit">提交举报</el-button>
      </template>
    </el-dialog>

    <!-- ===== 举报买家弹窗（卖家视图） ===== -->
    <el-dialog v-model="sellerReportDialogVisible" title="举报买家" width="480px" append-to-body>
      <el-form :model="sellerReportForm" label-width="90px" @submit.prevent>
        <el-form-item label="举报类型" required>
          <el-select v-model="sellerReportForm.reasonType" style="width:100%">
            <el-option label="恶意退款" value="REFUND_ABUSE" />
            <el-option label="诈骗/虚假交易" value="FRAUD" />
            <el-option label="态度恶劣/骚扰" value="BAD_ATTITUDE" />
            <el-option label="刷单/广告骚扰" value="SPAM" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="sellerReportForm.reasonDesc" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sellerReportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="sellerReportSubmitting" @click="handleSellerReportSubmit">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { Plus } from "@element-plus/icons-vue";
import { useRoute, useRouter } from "vue-router";
import {
  approveRefundOrderApi,
  cancelOrderApi,
  confirmReceiveOrderApi,
  getOrderDetailApi,
  getSellerOrderDetailApi,
  payOrderApi,
  refundOrderApi,
  rejectRefundOrderApi,
  shipOrderApi,
  submitOrderItemReviewsApi
} from "@/api/order";
import { getLogisticsTraceApi } from "@/api/logistics";
import { uploadImageApi } from "@/api/upload";
import { confirmOrderAction, showOrderActionError, showOrderActionSuccess } from "@/utils/orderUi";
import OrderStatusTag from "@/components/order/OrderStatusTag.vue";
import OrderSummaryCard from "@/components/order/OrderSummaryCard.vue";
import OrderTimeline from "@/components/order/OrderTimeline.vue";
import { onRealtimeEvent } from "@/realtime/realtimeClient";
import { submitReportApi, blockUserApi, unblockUserApi, isBlockingApi } from "@/api/credit";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const order = ref(null);
const reviewDialogVisible = ref(false);
const refundDialogVisible = ref(false);
const refundSubmitting = ref(false);
const payDialogVisible = ref(false);
const paySubmitting = ref(false);
const reviewItems = ref([]);
const logisticsTraces = ref([]);
const logisticsCardRef = ref(null);
const refundForm = reactive({
  mode: "RETURN_REFUND",
  reason: "",
  remark: "",
  proofUrls: []
});
const payForm = reactive({
  payMode: "THIRD_PARTY",
  payChannel: "WECHAT"
});

const proofList = computed(() => {
  const raw = order.value?.refundProofUrls;
  if (!raw) return [];
  return String(raw)
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
});

const proofPreviewVisible = ref(false);
const proofPreviewUrl = ref("");
const timelineExpanded = ref(false);
const isSellerView = computed(() => route.meta?.detailMode === "seller" || route.query.from === "seller");
const canOnlyRefund = computed(() => Number(order.value?.orderStatus) === 1);

// 取订单中第一个有卖家ID的商品的卖家ID
const orderSellerUserId = computed(() => {
  const items = order.value?.items || [];
  return items.find(i => i.sellerUserId)?.sellerUserId || null;
});

// 取买家ID（卖家视图使用）
const orderBuyerUserId = computed(() => order.value?.buyerUserId || null);

// 举报弹窗（订单内，买家举报卖家）
const orderReportDialogVisible = ref(false);
const orderReportSubmitting = ref(false);
const orderReportForm = ref({ reasonType: "", reasonDesc: "" });

// 拉黑状态（买家对卖家）
const isSellerBlocked = ref(false);

// 举报弹窗（卖家举报买家）
const sellerReportDialogVisible = ref(false);
const sellerReportSubmitting = ref(false);
const sellerReportForm = ref({ reasonType: "", reasonDesc: "" });

// 拉黑状态（卖家对买家）
const isBuyerBlocked = ref(false);

async function checkBlockStatus() {
  if (!orderSellerUserId.value) return;
  try {
    const res = await isBlockingApi(orderSellerUserId.value);
    isSellerBlocked.value = res.data === true;
  } catch {
    isSellerBlocked.value = false;
  }
}

async function checkBuyerBlockStatus() {
  if (!orderBuyerUserId.value) return;
  try {
    const res = await isBlockingApi(orderBuyerUserId.value);
    isBuyerBlocked.value = res.data === true;
  } catch {
    isBuyerBlocked.value = false;
  }
}

function openOrderReportDialog() {
  orderReportForm.value = { reasonType: "", reasonDesc: "" };
  orderReportDialogVisible.value = true;
}

async function handleOrderReportSubmit() {
  if (!orderReportForm.value.reasonType) {
    showOrderActionError({ message: "请选择举报类型" }, "举报失败");
    return;
  }
  orderReportSubmitting.value = true;
  try {
    await submitReportApi({
      reportedId: orderSellerUserId.value,
      reasonType: orderReportForm.value.reasonType,
      reasonDesc: orderReportForm.value.reasonDesc,
    });
    orderReportDialogVisible.value = false;
    showOrderActionSuccess("举报已提交，等待管理员审核");
  } catch (e) {
    showOrderActionError(e, "举报提交失败");
  } finally {
    orderReportSubmitting.value = false;
  }
}

async function handleOrderBlock() {
  try {
    await confirmOrderAction({
      title: "拉黑卖家",
      message: "确认拉黑该订单的卖家？拉黑后对方无法与你发起会话。",
      confirmButtonText: "确认拉黑"
    });
    await blockUserApi(orderSellerUserId.value);
    isSellerBlocked.value = true;
    showOrderActionSuccess("已拉黑该卖家");
  } catch (e) {
    if (String(e?.message || "").includes("cancel")) return;
    showOrderActionError(e, "操作失败");
  }
}

async function handleOrderUnblock() {
  try {
    await confirmOrderAction({
      title: "取消拉黑",
      message: "确认取消对该卖家的拉黑？",
      confirmButtonText: "确认取消"
    });
    await unblockUserApi(orderSellerUserId.value);
    isSellerBlocked.value = false;
    showOrderActionSuccess("已取消拉黑");
  } catch (e) {
    if (String(e?.message || "").includes("cancel")) return;
    showOrderActionError(e, "操作失败");
  }
}

// ===== 卖家举报买家 =====
function openSellerReportDialog() {
  sellerReportForm.value = { reasonType: "", reasonDesc: "" };
  sellerReportDialogVisible.value = true;
}

async function handleSellerReportSubmit() {
  if (!sellerReportForm.value.reasonType) {
    showOrderActionError({ message: "请选择举报类型" }, "举报失败");
    return;
  }
  sellerReportSubmitting.value = true;
  try {
    await submitReportApi({
      reportedId: orderBuyerUserId.value,
      reasonType: sellerReportForm.value.reasonType,
      reasonDesc: sellerReportForm.value.reasonDesc,
    });
    sellerReportDialogVisible.value = false;
    showOrderActionSuccess("举报已提交，等待管理员审核");
  } catch (e) {
    showOrderActionError(e, "举报提交失败");
  } finally {
    sellerReportSubmitting.value = false;
  }
}

// ===== 卖家拉黑买家 =====
async function handleSellerBlock() {
  try {
    await confirmOrderAction({
      title: "拉黑买家",
      message: "确认拉黑该订单的买家？拉黑后对方无法与你发起会话。",
      confirmButtonText: "确认拉黑"
    });
    await blockUserApi(orderBuyerUserId.value);
    isBuyerBlocked.value = true;
    showOrderActionSuccess("已拉黑该买家");
  } catch (e) {
    if (String(e?.message || "").includes("cancel")) return;
    showOrderActionError(e, "操作失败");
  }
}

async function handleSellerUnblock() {
  try {
    await confirmOrderAction({
      title: "取消拉黑",
      message: "确认取消对该买家的拉黑？",
      confirmButtonText: "确认取消"
    });
    await unblockUserApi(orderBuyerUserId.value);
    isBuyerBlocked.value = false;
    showOrderActionSuccess("已取消拉黑");
  } catch (e) {
    if (String(e?.message || "").includes("cancel")) return;
    showOrderActionError(e, "操作失败");
  }
}

const autoConfirmTip = computed(() => {
  const o = order.value;
  if (!o || String(o.logisticsStatus || "").toUpperCase() !== "ARRIVED" || !o.autoConfirmDeadline) {
    return "";
  }
  const now = Date.now();
  const deadline = new Date(o.autoConfirmDeadline).getTime();
  if (!deadline || deadline <= now) {
    return "订单已达到自动确认收货时间，系统将尽快处理。";
  }
  const diffMs = deadline - now;
  const dayMs = 24 * 60 * 60 * 1000;
  const hourMs = 60 * 60 * 1000;
  const days = Math.floor(diffMs / dayMs);
  const hours = Math.floor((diffMs % dayMs) / hourMs);
  return `系统将于 ${days} 天 ${hours} 小时后自动确认收货`;
});

function openProofPreview(url) {
  proofPreviewUrl.value = url;
  proofPreviewVisible.value = true;
}

onMounted(async () => {
  await fetchDetail();
  await maybeFocusLogistics();
  maybeOpenPayDialog();
  unsubscribeRealtime = onRealtimeEvent(handleRealtimeEvent);
  if (!isSellerView.value && route.query.action === "review") {
    openReviewDialog();
  }
  // 检查是否已拉黑卖家
  if (!isSellerView.value) {
    await checkBlockStatus();
  } else {
    // 卖家视图：检查是否已拉黑买家
    await checkBuyerBlockStatus();
  }
});
onBeforeUnmount(() => {
  if (unsubscribeRealtime) unsubscribeRealtime();
});
let unsubscribeRealtime = null;

async function fetchDetail() {
  loading.value = true;
  try {
    if (isSellerView.value) {
      try {
        const sellerResult = await getSellerOrderDetailApi(route.params.id);
        order.value = sellerResult.data;
        await fetchLogisticsTrace();
        return;
      } catch (_) {
        // 兜底：历史链接可能未带 seller 标记，尝试买家详情避免页面空白。
      }
    }
    const result = await getOrderDetailApi(route.params.id);
    order.value = result.data;
    await fetchLogisticsTrace();
  } finally {
    loading.value = false;
  }
}

async function maybeFocusLogistics() {
  if (route.query.tab !== "logistics") {
    return;
  }
  await nextTick();
  const cardEl = logisticsCardRef.value?.$el || logisticsCardRef.value;
  if (cardEl?.scrollIntoView) {
    cardEl.scrollIntoView({ behavior: "smooth", block: "start" });
  }
}

function goBack() {
  if (isSellerView.value) {
    router.push("/merchant/orders");
    return;
  }
  router.push("/order");
}

function contactSellerByItem(item) {
  if (!item?.sellerUserId) {
    showOrderActionError({ message: "未找到卖家信息" }, "联系卖家失败");
    return;
  }
  router.push({
    path: "/messages",
    query: {
      participantId: item.sellerUserId
    }
  });
}

function maybeOpenPayDialog() {
  if (isSellerView.value) {
    return;
  }
  if (route.query.action !== "pay") {
    return;
  }
  if (Number(order.value?.orderStatus) !== 0) {
    return;
  }
  payForm.payMode = "THIRD_PARTY";
  payForm.payChannel = "WECHAT";
  payDialogVisible.value = true;
}

async function fetchLogisticsTrace() {
  if (!route.params.id) {
    logisticsTraces.value = [];
    return;
  }
  try {
    const result = await getLogisticsTraceApi(route.params.id);
    const traces = result.data || [];
    logisticsTraces.value = [...traces].sort((a, b) => {
      const at = new Date(a.createTime || 0).getTime();
      const bt = new Date(b.createTime || 0).getTime();
      return bt - at;
    });
    if (order.value && logisticsTraces.value.length > 0) {
      const latest = logisticsTraces.value[0];
      const desc = String(latest?.statusDesc || "");
      order.value.logisticsStatus = /送达|签收/.test(desc) ? "ARRIVED" : "IN_TRANSIT";
    }
  } catch (_) {
    logisticsTraces.value = [];
  }
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

async function pay() {
  payForm.payMode = "THIRD_PARTY";
  payForm.payChannel = "WECHAT";
  payDialogVisible.value = true;
}

async function confirmPay() {
  paySubmitting.value = true;
  try {
    await payOrderApi(order.value.id, {
      payMode: payForm.payMode,
      payChannel: payForm.payChannel
    });
    payDialogVisible.value = false;
    showOrderActionSuccess("支付成功");
    await fetchDetail();
  } catch (error) {
    showOrderActionError(error, "支付失败");
  } finally {
    paySubmitting.value = false;
  }
}

async function cancel() {
  try {
    await confirmOrderAction({
      title: "确认取消订单",
      message: "取消后将无法继续付款，是否继续？",
      confirmButtonText: "确认取消"
    });
    await cancelOrderApi(order.value.id);
    showOrderActionSuccess("已取消订单");
    await fetchDetail();
  } catch (error) {
    if (String(error?.message || "").includes("cancel")) return;
    showOrderActionError(error, "取消订单失败");
  }
}

async function confirmReceive() {
  try {
    await confirmOrderAction({
      title: "确认收货",
      message: "确认已收到货物？确认后订单将进入待评价。",
      confirmButtonText: "确认收货"
    });
    await confirmReceiveOrderApi(order.value.id);
    showOrderActionSuccess("已确认收货");
    await fetchDetail();
  } catch (error) {
    if (String(error?.message || "").includes("cancel")) return;
    showOrderActionError(error, "确认收货失败");
  }
}

function openReviewDialog() {
  if (!order.value || order.value.orderStatus !== 3) {
    return;
  }
  reviewItems.value = (order.value.items || []).map((i) => ({
    productType: i.productType,
    productId: i.productId,
    productName: i.productName,
    score: 5,
    content: ""
  }));
  reviewDialogVisible.value = true;
}

async function submitReview() {
  if (!reviewItems.value.length) {
    showOrderActionError({ message: "评价商品为空" }, "提交评价失败");
    return;
  }
  for (const r of reviewItems.value) {
    if (!r.score || !String(r.content || "").trim()) {
      showOrderActionError({ message: "请为每个商品填写评分和评价内容" }, "提交评价失败");
      return;
    }
  }
  try {
    await submitOrderItemReviewsApi(order.value.id, {
      items: reviewItems.value.map((r) => ({
        productType: r.productType,
        productId: r.productId,
        score: r.score,
        content: String(r.content || "").trim()
      }))
    });
    reviewDialogVisible.value = false;
    showOrderActionSuccess("评价提交成功");
    await fetchDetail();
  } catch (error) {
    showOrderActionError(error, "提交评价失败");
  }
}

function canRefund(status, refundStatus) {
  if (refundStatus === 1 || refundStatus === 2) return false;
  return order.value?.canRefund !== 0 && (status === 1 || status === 2 || status === 3 || status === 4);
}

function openRefundDialog() {
  refundForm.mode = canOnlyRefund.value ? "ONLY_REFUND" : "RETURN_REFUND";
  refundForm.reason = "";
  refundForm.remark = "";
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
  if (!refundForm.reason) {
    showOrderActionError({ message: "请选择退货原因" }, "提交退货申请失败");
    return;
  }
  refundSubmitting.value = true;
  try {
    const reason = refundForm.remark?.trim()
      ? `${refundForm.reason}（${refundForm.remark.trim()}）`
      : refundForm.reason;
    await refundOrderApi(order.value.id, {
      refundMode: refundForm.mode,
      reason,
      proofUrls: refundForm.proofUrls
    });
    refundDialogVisible.value = false;
    showOrderActionSuccess("已提交退货申请");
    await fetchDetail();
  } catch (error) {
    showOrderActionError(error, "提交退货申请失败");
  } finally {
    refundSubmitting.value = false;
  }
}

async function shipBySeller() {
  try {
    await confirmOrderAction({
      title: "确认发货",
      message: "确认将当前订单标记为已发货？",
      confirmButtonText: "确认发货"
    });
    await shipOrderApi(order.value.id);
    showOrderActionSuccess("发货成功");
    await fetchDetail();
  } catch (error) {
    if (String(error?.message || "").includes("cancel")) return;
    showOrderActionError(error, "发货失败");
  }
}

async function approveRefundBySeller() {
  try {
    await confirmOrderAction({
      title: "同意退货",
      message: "确认同意退货并完成退款？",
      confirmButtonText: "确认同意"
    });
    await approveRefundOrderApi(order.value.id);
    showOrderActionSuccess("已同意退货");
    await fetchDetail();
  } catch (error) {
    if (String(error?.message || "").includes("cancel")) return;
    showOrderActionError(error, "同意退货失败");
  }
}

async function rejectRefundBySeller() {
  try {
    await confirmOrderAction({
      title: "拒绝退货",
      message: "确认拒绝当前退货申请？",
      confirmButtonText: "确认拒绝"
    });
    await rejectRefundOrderApi(order.value.id);
    showOrderActionSuccess("已拒绝退货");
    await fetchDetail();
  } catch (error) {
    if (String(error?.message || "").includes("cancel")) return;
    showOrderActionError(error, "拒绝退货失败");
  }
}

function handleRealtimeEvent(event) {
  const type = event?.detail?.eventType;
  const orderId = event?.detail?.payload?.orderId;
  if (!order.value || String(order.value.id) !== String(orderId)) {
    return;
  }
  if (type === "ORDER_STATUS_UPDATED" || type === "AFTER_SALE_UPDATED" || type === "LOGISTICS_UPDATED" || type === "ORDER_REMIND_SHIP") {
    fetchDetail();
  }
}

const orderTimeline = computed(() => {
  if (!order.value) return [];
  const o = order.value;
  const orderStatus = o.orderStatus ?? 0;
  const refundStatus = o.refundStatus ?? 0;
  const isClosed = orderStatus === 9;
  const hasRefund = refundStatus > 0;
  const refundTerminal = !!o.refundDecisionTime && hasRefund && (refundStatus === 2 || refundStatus === 3);

  const createdTime = formatTime(o.createTime);
  const paidTime = o.paidTime ? formatTime(o.paidTime) : '';
  const shippedTime = o.shippedTime ? formatTime(o.shippedTime) : '';
  const receivedTime = o.receivedTime ? formatTime(o.receivedTime) : '';
  const completedTime = o.completedTime ? formatTime(o.completedTime) : '';
  const closedTime = o.closedTime ? formatTime(o.closedTime) : '';
  const decisionTime = o.refundDecisionTime ? formatTime(o.refundDecisionTime) : '';

  const steps = [
    { key: "created", label: "已下单", done: true, time: createdTime },
    { key: "paid", label: "已付款", done: !!o.paidTime, time: paidTime || '-' },
    { key: "shipped", label: "已发货", done: !!o.shippedTime, time: shippedTime || '-' },
    {
      key: "received",
      label: o.receivedTime
        ? "已收货"
        : refundTerminal
          ? refundStatus === 2
            ? "已退款"
            : "退款被拒绝"
          : hasRefund && refundStatus === 1
            ? "待收货(退款中)"
            : "待收货",
      done: !!o.receivedTime || refundTerminal,
      time: o.receivedTime ? receivedTime : decisionTime || '-'
    },
    {
      key: "review",
      label: o.completedTime
        ? "已评价"
        : refundTerminal
          ? refundStatus === 2 || refundStatus === 3
            ? "退款影响评价"
            : "待评价"
          : "待评价",
      done: !!o.completedTime || refundTerminal,
      time: o.completedTime ? completedTime : decisionTime || '-'
    },
    {
      key: "completed",
      label: o.completedTime
        ? isClosed && hasRefund
          ? "已完成(退款后关闭)"
          : "已完成"
        : refundTerminal
          ? "已完成(未完成)"
          : "已完成",
      done: !!o.completedTime,
      time: completedTime || '-'
    },
    { key: "closed", label: "已关闭", done: isClosed || !!o.closedTime, time: closedTime || '-' }
  ];

  let activeKey = "created";
  if (isClosed) activeKey = "closed";
  else if (refundTerminal) {
    // 退款已做决策时，流程的“最后有效节点”应落在收货/评价阶段
    activeKey = o.completedTime ? "completed" : "review";
  } else if (orderStatus === 4) activeKey = "completed";
  else if (orderStatus === 3) activeKey = "review";
  else if (orderStatus === 2) activeKey = "received";
  else if (orderStatus === 1) activeKey = "paid";
  else activeKey = "created";

  // 售后进行中时，仍展示订单进度；售后进度在 refundTimeline 中独立展示。
  return steps.map((s) => {
    const status = s.done ? "done" : s.key === activeKey ? "active" : "todo";
    return { ...s, status };
  });
});

const refundTimeline = computed(() => {
  if (!order.value) return [];
  const o = order.value;
  const rs = o.refundStatus ?? 0;
  if (rs <= 0) return [];

  const applyTime = formatTime(o.refundApplyTime);
  const decisionTime = formatTime(o.refundDecisionTime);

  const steps = [
    { key: "apply", label: "买家申请退货", done: true, time: applyTime },
    { key: "processing", label: "退款中", done: rs >= 1, time: rs === 1 ? "" : decisionTime },
    { key: "refunded", label: "已退款", done: rs >= 2, time: rs >= 2 ? decisionTime : "" },
    { key: "rejected", label: "退款被拒绝", done: rs >= 3, time: rs >= 3 ? decisionTime : "" }
  ];

  let activeKey = "apply";
  if (rs === 1) activeKey = "processing";
  else if (rs === 2) activeKey = "refunded";
  else if (rs === 3) activeKey = "rejected";

  return steps.map((s) => {
    const status = s.done ? "done" : s.key === activeKey ? "active" : "todo";
    return { ...s, status };
  });
});

const orderStageSummary = computed(() => {
  const o = order.value;
  if (!o) return "-";
  if ((o.refundStatus ?? 0) === 1) return "售后处理中";
  if ((o.refundStatus ?? 0) === 2) return "售后已完成（已退款）";
  if ((o.refundStatus ?? 0) === 3) return "售后已拒绝";
  if (o.orderStatus === 0) return "待付款";
  if (o.orderStatus === 1) return "待发货";
  if (o.orderStatus === 2) return "待收货";
  if (o.orderStatus === 3) return "待评价";
  if (o.orderStatus === 4) return "已完成";
  if (o.orderStatus === 9) return "已关闭";
  return "状态未知";
});

const orderNextActionSummary = computed(() => {
  const o = order.value;
  if (!o) return "-";
  if ((o.refundStatus ?? 0) === 1) return "等待商家或平台审核，建议保留沟通与凭证。";
  if ((o.refundStatus ?? 0) === 2) return "退款已完成，如有问题可联系平台客服。";
  if ((o.refundStatus ?? 0) === 3) return "可查看拒绝原因，必要时补充凭证后再次申请。";
  if (o.orderStatus === 0) return "尽快完成付款，超时订单可能自动关闭。";
  if (o.orderStatus === 1) return "等待卖家发货，可使用“提醒发货”。";
  if (o.orderStatus === 2) return "收到货后请及时确认收货。";
  if (o.orderStatus === 3) return "建议完成评价，便于交易闭环。";
  if (o.orderStatus === 4) return "订单已完成，可在“我的评价”查看记录。";
  if (o.orderStatus === 9) return "订单已关闭，如有争议可联系平台。";
  return "请刷新页面后重试。";
});
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.auto-confirm-alert {
  margin-bottom: 12px;
}

.logistics-card {
  margin-bottom: 12px;
}

.logistics-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.trace-node {
  font-weight: 600;
  color: #111827;
}

.trace-desc {
  margin-top: 2px;
  font-size: 13px;
  color: #6b7280;
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


.hint {
  color: #6b7280;
  font-size: 12px;
  margin-top: 6px;
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
</style>