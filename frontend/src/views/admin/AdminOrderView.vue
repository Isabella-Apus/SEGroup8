<template>
  <div class="page-card fade-in-up">
    <h2 class="page-title">订单监管</h2>

    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="订单号/商品名" clearable style="width: 220px" />
      </el-form-item>
      <el-form-item label="订单状态">
        <el-input-number v-model="query.orderStatus" :min="0" :max="10" :step="1" style="width: 120px" />
      </el-form-item>
      <el-form-item label="售后状态">
        <el-input-number v-model="query.refundStatus" :min="0" :max="10" :step="1" style="width: 120px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button type="warning" :disabled="selectedOrderIds.length === 0" @click="batchClose">批量关闭</el-button>
      </el-form-item>
    </el-form>

    <div class="table-mobile-wrap">
      <el-table
        v-loading="loading"
        :data="records"
        border
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="56" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderNo" label="订单号" min-width="200" />
        <el-table-column prop="buyerUserId" label="买家ID" width="90" />
        <el-table-column label="金额" width="120">
          <template #default="scope">￥{{ Number(scope.row.totalAmount || 0).toFixed(2) }}</template>
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
        <el-table-column prop="createTime" label="下单时间" min-width="180" />
        <el-table-column label="操作" min-width="300">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
            <el-button v-if="canDealRefund(scope.row)" link type="success" @click="approveRefund(scope.row)">同意退货</el-button>
            <el-button v-if="canDealRefund(scope.row)" link class="danger-action" @click="rejectRefund(scope.row)">拒绝退货</el-button>
            <el-button link @click="openAfterSaleLogs(scope.row)">售后日志</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">暂无订单数据</div>
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

    <el-dialog v-model="detailVisible" title="订单详情" width="880px">
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="买家ID">{{ detail.buyerUserId }}</el-descriptions-item>
        <el-descriptions-item label="订单状态">{{ detail.orderStatusName }}</el-descriptions-item>
        <el-descriptions-item label="售后状态">{{ detail.refundStatusName }}</el-descriptions-item>
        <el-descriptions-item label="审核人">{{ detail.refundDecisionUserName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审核意见">{{ detail.refundDecisionRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="table-mobile-wrap">
        <el-table v-if="detail" :data="detail.items || []" border style="margin-top: 12px">
          <el-table-column prop="productName" label="商品名" min-width="220" />
          <el-table-column prop="productType" label="类型" width="120" />
          <el-table-column prop="price" label="单价" width="120">
            <template #default="scope">￥{{ Number(scope.row.price || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="100" />
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="logVisible" title="售后日志" width="820px">
      <div class="table-mobile-wrap">
        <el-table :data="afterSaleLogs" border>
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="action" label="操作" width="140" />
          <el-table-column prop="operatorRole" label="角色" width="120" />
          <el-table-column prop="operatorUserId" label="操作人ID" width="120" />
          <el-table-column prop="remark" label="备注" min-width="180" />
          <el-table-column prop="createTime" label="时间" min-width="180" />
          <template #empty>
            <div class="empty-state">暂无售后日志</div>
          </template>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  approveAdminRefundApi,
  batchCloseAdminOrdersApi,
  getAdminAfterSaleLogsApi,
  getAdminOrderDetailApi,
  pageAdminOrdersApi,
  rejectAdminRefundApi,
} from '@/api/adminOrder';

const loading = ref(false);
const total = ref(0);
const records = ref([]);
const selectedOrderIds = ref([]);
const detailVisible = ref(false);
const detail = ref(null);
const logVisible = ref(false);
const afterSaleLogs = ref([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  orderStatus: undefined,
  refundStatus: undefined,
});

onMounted(() => {
  fetchOrders();
});

async function fetchOrders() {
  loading.value = true;
  try {
    const result = await pageAdminOrdersApi(query);
    records.value = result.data?.records || [];
    total.value = result.data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleSelectionChange(selection) {
  selectedOrderIds.value = selection.map((item) => item.id);
}

function search() {
  query.pageNum = 1;
  fetchOrders();
}

function reset() {
  query.pageNum = 1;
  query.pageSize = 10;
  query.keyword = '';
  query.orderStatus = undefined;
  query.refundStatus = undefined;
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

function canDealRefund(order) {
  return Number(order?.refundStatus) === 1;
}

function orderStatusClass(order) {
  const status = Number(order?.orderStatus);
  if ([4, 5].includes(status)) {
    return 'status-success';
  }
  if (status === 6) {
    return 'status-danger';
  }
  if ([1, 2, 3].includes(status)) {
    return 'status-progress';
  }
  return 'status-pending';
}

function refundStatusClass(order) {
  const status = Number(order?.refundStatus);
  if (status === 2) {
    return 'status-success';
  }
  if (status === 3) {
    return 'status-danger';
  }
  if (status === 1) {
    return 'status-progress';
  }
  return 'status-pending';
}

async function batchClose() {
  await ElMessageBox.confirm(`确认批量关闭 ${selectedOrderIds.value.length} 个订单吗？`, '提示', {
    type: 'warning',
  });
  await batchCloseAdminOrdersApi(selectedOrderIds.value);
  ElMessage.success('批量关闭操作已提交');
  selectedOrderIds.value = [];
  await fetchOrders();
}

async function openDetail(order) {
  const result = await getAdminOrderDetailApi(order.id);
  detail.value = result.data || null;
  detailVisible.value = true;
}

async function approveRefund(order) {
  const { value } = await ElMessageBox.prompt('可填写审核意见（选填）', '同意退货', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：同意退货并退款',
    inputValue: '',
  }).catch(() => ({ value: null }));
  if (value === null) {
    return;
  }
  await approveAdminRefundApi(order.id, { remark: value || '' });
  ElMessage.success('已同意退货');
  await fetchOrders();
}

async function rejectRefund(order) {
  const { value } = await ElMessageBox.prompt('可填写审核意见（选填）', '拒绝退货', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：证据不足，拒绝退货',
    inputValue: '',
  }).catch(() => ({ value: null }));
  if (value === null) {
    return;
  }
  await rejectAdminRefundApi(order.id, { remark: value || '' });
  ElMessage.success('已拒绝退货');
  await fetchOrders();
}

async function openAfterSaleLogs(order) {
  const result = await getAdminAfterSaleLogsApi(order.id);
  afterSaleLogs.value = result.data || [];
  logVisible.value = true;
}
</script>

<style scoped>
.query-form {
  margin-bottom: 12px;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
