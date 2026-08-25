<template>
  <div class="page-card fade-in-up">
    <h2 class="page-title">卖家订单管理</h2>

    <div class="toolbar table-toolbar">
      <el-input v-model="query.keyword" placeholder="订单号/商品名" clearable style="max-width: 320px" @keyup.enter="handleSearch" />
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

    <div class="table-mobile-wrap">
      <el-table v-loading="loading" :data="records" border class="kg-table">
        <el-table-column prop="orderNo" label="订单号" min-width="200" show-overflow-tooltip />
        <el-table-column prop="buyerUserId" label="买家ID" width="90" />
        <el-table-column label="预计到账" width="120">
          <template #default="scope"><span class="amount-text">￥{{ sellerReceivable(scope.row).toFixed(2) }}</span></template>
        </el-table-column>
        <el-table-column label="订单状态" width="130">
          <template #default="scope">
            <el-tag class="status-tag" :class="orderStatusClass(scope.row)" size="small" effect="plain">
              {{ scope.row.orderStatusName || scope.row.orderStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="售后状态" width="130">
          <template #default="scope">
            <el-tag class="status-tag" :class="refundStatusClass(scope.row)" size="small" effect="plain">
              {{ scope.row.refundStatusName || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" min-width="180">
          <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="340" fixed="right">
          <template #default="scope">
            <div class="table-actions">
              <el-button link type="primary" @click="goDetail(scope.row.id)">详情页</el-button>
              <el-button link type="primary" @click="openDetail(scope.row)">弹窗详情</el-button>
              <el-button v-if="canShip(scope.row)" link type="success" @click="ship(scope.row)">发货</el-button>
              <el-button v-if="canPushLogistics(scope.row)" link type="primary" @click="pushLogistics(scope.row)">更新进度</el-button>
              <el-button v-if="canApproveRefund(scope.row)" link type="success" @click="approveRefund(scope.row)">同意退货</el-button>
              <el-button v-if="canRejectRefund(scope.row)" link class="danger-action" @click="rejectRefund(scope.row)">拒绝退货</el-button>
              <el-button link type="warning" @click="openReportBuyerDialog(scope.row)">举报买家</el-button>
              <el-button
                v-if="!blockedBuyerIds.has(scope.row.buyerUserId)"
                link
                class="danger-action"
                @click="handleBlockBuyer(scope.row)"
              >拉黑买家</el-button>
              <el-button
                v-else
                link
                type="info"
                @click="handleUnblockBuyer(scope.row)"
              >取消拉黑</el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">暂无符合条件的订单</div>
        </template>
      </el-table>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="860px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="买家ID">{{ detail.buyerUserId }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ detail.orderStatusName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="售后状态">{{ detail.refundStatusName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品总额">￥{{ Number(detail.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="买家实付">￥{{ Number(detail.payableAmount ?? detail.totalAmount ?? 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="商家承担优惠">-￥{{ Number(detail.sellerBearAmount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="预计到账">￥{{ sellerReceivable(detail).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="收货人">{{ detail.receiverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.receiverPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货地址">{{ fullAddress(detail) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="detail" :data="detail.items || []" border style="margin-top: 12px">
        <el-table-column prop="productName" label="商品名" min-width="220" />
        <el-table-column prop="productType" label="类型" width="120" />
        <el-table-column prop="price" label="单价" width="120">
          <template #default="scope">￥{{ Number(scope.row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" />
      </el-table>
    </el-dialog>

    <!-- ===== 举报买家弹窗 ===== -->
    <el-dialog v-model="reportBuyerDialogVisible" title="举报买家" width="480px">
      <el-form :model="reportBuyerForm" label-width="90px">
        <el-form-item label="举报类型" required>
          <el-select v-model="reportBuyerForm.reasonType" style="width:100%">
            <el-option label="恶意退款" value="REFUND_ABUSE" />
            <el-option label="诈骗/虚假交易" value="FRAUD" />
            <el-option label="态度恶劣/骚扰" value="BAD_ATTITUDE" />
            <el-option label="刷单/广告骚扰" value="SPAM" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="reportBuyerForm.reasonDesc" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportBuyerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportBuyerSubmitting" @click="handleReportBuyerSubmit">提交举报</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import {
  approveRefundOrderApi,
  getSellerOrderDetailApi,
  getSellerOrderListApi,
  rejectRefundOrderApi,
  shipOrderApi
} from '@/api/order';
import { pushNextLogisticsApi } from '@/api/logistics';
import { onRealtimeEvent } from '@/realtime/realtimeClient';
import { submitReportApi, blockUserApi, unblockUserApi, isBlockingApi } from '@/api/credit';

const loading = ref(false);
const total = ref(0);
const records = ref([]);
const detailVisible = ref(false);
const detail = ref(null);
const router = useRouter();
// 记录已被拉黑的买家ID集合
const blockedBuyerIds = ref(new Set());

// 商家券从商品总额中扣除；平台券由平台补贴，不减少商家到账。
function sellerReceivable(order) {
  const gross = Number(order?.totalAmount || 0);
  const sellerDiscount = Number(order?.sellerBearAmount || 0);
  return Math.max(0, gross - sellerDiscount);
}

const query = reactive({
  pageNum: 1,
  pageSize: 20,
  orderStatus: undefined,
  keyword: ''
});

let unsubscribeRealtime = null;

onMounted(() => {
  fetchList();
  unsubscribeRealtime = onRealtimeEvent(handleRealtimeEvent);
});

onBeforeUnmount(() => {
  if (unsubscribeRealtime) unsubscribeRealtime();
});

async function fetchList() {
  loading.value = true;
  try {
    const result = await getSellerOrderListApi({ ...query, productType: 'NEW' });
    records.value = result.data?.records || [];
    total.value = result.data?.total || 0;
    // 检查每个买家的拉黑状态
    await refreshBlockedStatus();
  } finally {
    loading.value = false;
  }
}

async function refreshBlockedStatus() {
  const uniqueBuyerIds = [...new Set(records.value.map(r => r.buyerUserId).filter(Boolean))];
  const newBlockedSet = new Set();
  await Promise.allSettled(
    uniqueBuyerIds.map(async (id) => {
      try {
        const res = await isBlockingApi(id);
        if (res.data === true) newBlockedSet.add(id);
      } catch {}
    })
  );
  blockedBuyerIds.value = newBlockedSet;
}

function handleSearch() {
  query.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.pageNum = 1;
  query.keyword = '';
  query.orderStatus = undefined;
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
  if (!value) return '-';
  return String(value).replace('T', ' ');
}

function canShip(order) {
  return Number(order?.orderStatus) === 1;
}

function canApproveRefund(order) {
  return Number(order?.refundStatus) === 1;
}

function canPushLogistics(order) {
  return Number(order?.orderStatus) === 2 && String(order?.logisticsStatus || "").toUpperCase() !== "ARRIVED";
}

function canRejectRefund(order) {
  return Number(order?.refundStatus) === 1;
}

function orderStatusClass(order) {
  const status = Number(order?.orderStatus);
  if (status === 4) return 'status-success';
  if (status === 9) return 'status-danger';
  if ([1, 2, 3].includes(status)) return 'status-progress';
  return 'status-pending';
}

function refundStatusClass(order) {
  const status = Number(order?.refundStatus);
  if (status === 2) return 'status-success';
  if (status === 3) return 'status-danger';
  if (status === 1) return 'status-progress';
  return 'status-pending';
}

async function ship(order) {
  try {
    await ElMessageBox.confirm('确认该订单已发货吗？', '提示', { type: 'warning' });
    await shipOrderApi(order.id);
    ElMessage.success('发货成功');
    await fetchList();
  } catch (error) {
    if (String(error?.message || '').includes('cancel')) return;
    ElMessage.error(error?.response?.data?.message || error?.message || '发货失败');
  }
}

async function approveRefund(order) {
  await ElMessageBox.confirm('确认同意该订单退货吗？', '提示', { type: 'warning' });
  await approveRefundOrderApi(order.id);
  ElMessage.success('已同意退货');
  await fetchList();
}

async function rejectRefund(order) {
  await ElMessageBox.confirm('确认拒绝该订单退货吗？', '提示', { type: 'warning' });
  await rejectRefundOrderApi(order.id);
  ElMessage.success('已拒绝退货');
  await fetchList();
}

async function pushLogistics(order) {
  const result = await pushNextLogisticsApi(order.id);
  const detailRes = await getSellerOrderDetailApi(order.id);
  const targetIndex = records.value.findIndex((it) => String(it.id) === String(order.id));
  if (targetIndex >= 0) {
    records.value[targetIndex] = detailRes.data;
  }
  ElMessage.success(`物流已更新：${result.data?.nodeName || '下一节点'}`);
}

function goDetail(orderId) {
  router.push(`/merchant/orders/${orderId}`);
}

async function openDetail(order) {
  const result = await getSellerOrderDetailApi(order.id);
  detail.value = result.data || null;
  detailVisible.value = true;
}

function fullAddress(order) {
  const parts = [order?.receiverProvince, order?.receiverCity, order?.receiverDetailAddress].filter(Boolean);
  return parts.length ? parts.join(' ') : '-';
}

function handleRealtimeEvent(event) {
  const type = event?.detail?.eventType;
  if (type === 'ORDER_STATUS_UPDATED' || type === 'AFTER_SALE_UPDATED' || type === 'LOGISTICS_UPDATED' || type === 'ORDER_REMIND_SHIP') {
    fetchList();
  }
}

// ===== 举报买家 =====
const reportBuyerDialogVisible = ref(false);
const reportBuyerSubmitting = ref(false);
const reportBuyerForm = ref({ reasonType: '', reasonDesc: '' });
let reportTargetBuyerId = null;

function openReportBuyerDialog(order) {
  reportTargetBuyerId = order.buyerUserId;
  reportBuyerForm.value = { reasonType: '', reasonDesc: '' };
  reportBuyerDialogVisible.value = true;
}

async function handleReportBuyerSubmit() {
  if (!reportBuyerForm.value.reasonType) {
    ElMessage.warning('请选择举报类型');
    return;
  }
  reportBuyerSubmitting.value = true;
  try {
    await submitReportApi({
      reportedId: reportTargetBuyerId,
      tradeContext: "SH_SELLER",
      reasonType: reportBuyerForm.value.reasonType,
      reasonDesc: reportBuyerForm.value.reasonDesc,
    });
    ElMessage.success('举报已提交，等待管理员审核');
    reportBuyerDialogVisible.value = false;
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '举报提交失败');
  } finally {
    reportBuyerSubmitting.value = false;
  }
}

// ===== 拉黑买家 =====
async function handleBlockBuyer(order) {
  try {
    await ElMessageBox.confirm(
      `确认拉黑买家（ID: ${order.buyerUserId}）？拉黑后对方的二手商品不会出现在你的列表中。`,
      '拉黑确认',
      { type: 'warning', confirmButtonText: '确认拉黑', cancelButtonText: '取消' }
    );
    await blockUserApi(order.buyerUserId);
    blockedBuyerIds.value = new Set([...blockedBuyerIds.value, order.buyerUserId]);
    ElMessage.success('已拉黑该买家');
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return;
    ElMessage.error(e?.response?.data?.message || '操作失败');
  }
}

async function handleUnblockBuyer(order) {
  try {
    await ElMessageBox.confirm(
      `确认取消拉黑买家（ID: ${order.buyerUserId}）？`,
      '取消拉黑',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '取消' }
    );
    await unblockUserApi(order.buyerUserId);
    const newSet = new Set(blockedBuyerIds.value);
    newSet.delete(order.buyerUserId);
    blockedBuyerIds.value = newSet;
    ElMessage.success('已取消拉黑');
  } catch (e) {
    if (e === 'cancel' || e?.toString?.().includes('cancel')) return;
    ElMessage.error(e?.response?.data?.message || '操作失败');
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

.danger-action {
  color: #ef4444;
}
</style>
