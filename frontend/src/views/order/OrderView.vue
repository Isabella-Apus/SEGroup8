<template>
  <div class="page-card">
    <h2 class="page-title">我的订单</h2>

    <el-empty v-if="records.length === 0" description="暂无订单" />

    <el-collapse v-else>
      <el-collapse-item v-for="order in records" :key="order.id" :title="`订单号：${order.orderNo}`" :name="order.id">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="下单时间">{{ formatTime(order.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">￥{{ Number(order.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ order.payStatus === 1 ? '已支付' : '待支付' }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ order.orderStatusName || order.orderStatus }}</el-descriptions-item>
        </el-descriptions>

        <div class="action-row">
          <el-space>
            <el-button v-if="canPay(order)" type="primary" @click="pay(order)">支付</el-button>
            <el-button v-if="canCancel(order)" @click="cancel(order)">取消订单</el-button>
            <el-button v-if="canRemind(order)" @click="remind(order)">提醒发货</el-button>
            <el-button v-if="canConfirm(order)" type="success" @click="confirmReceive(order)">确认收货</el-button>
            <el-button v-if="canComplete(order)" type="success" plain @click="complete(order)">完成订单</el-button>
            <el-button v-if="canRefund(order)" type="warning" @click="refund(order)">申请退货</el-button>
          </el-space>
        </div>

        <el-table :data="order.items || []" border style="margin-top: 10px">
          <el-table-column prop="productName" label="商品名" min-width="220" />
          <el-table-column label="类型" width="100">
            <template #default="scope">
              <el-tag :type="scope.row.itemType === 'SECONDHAND' ? 'warning' : 'info'" size="small">
                {{ scope.row.itemType === 'SECONDHAND' ? '二手' : '普通' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="price" label="单价" width="120">
            <template #default="scope">￥{{ Number(scope.row.price || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column label="小计" width="120">
            <template #default="scope">￥{{ (Number(scope.row.price || 0) * Number(scope.row.quantity || 0)).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </el-collapse-item>
    </el-collapse>

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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getOrderListApi } from '@/api/order';
import {
  cancelOrderApi,
  completeOrderApi,
  confirmReceiveOrderApi,
  payOrderApi,
  refundOrderApi,
  remindShipApi,
} from '@/api/order';

const query = reactive({
  pageNum: 1,
  pageSize: 10
});
const total = ref(0);
const records = ref([]);

onMounted(() => {
  fetchOrders();
});

async function fetchOrders() {
  const result = await getOrderListApi(query);
  records.value = result.data?.records || [];
  total.value = result.data?.total || 0;
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

function formatTime(value) {
  if (!value) {
    return '-';
  }
  return String(value).replace('T', ' ');
}

function canPay(order) {
  return Number(order?.orderStatus) === 0;
}

function canCancel(order) {
  return [0, 1, 2, 3].includes(Number(order?.orderStatus));
}

function canRemind(order) {
  return Number(order?.orderStatus) === 1;
}

function canConfirm(order) {
  return Number(order?.orderStatus) === 2;
}

function canComplete(order) {
  return Number(order?.orderStatus) === 3;
}

function canRefund(order) {
  return Number(order?.payStatus) === 1 && [1, 2, 3].includes(Number(order?.orderStatus));
}

async function pay(order) {
  await payOrderApi(order.id);
  ElMessage.success('支付成功');
  await fetchOrders();
}

async function cancel(order) {
  await cancelOrderApi(order.id);
  ElMessage.success('已取消订单');
  await fetchOrders();
}

async function remind(order) {
  await remindShipApi(order.id);
  ElMessage.success('已提醒卖家发货');
}

async function confirmReceive(order) {
  await confirmReceiveOrderApi(order.id);
  ElMessage.success('确认收货成功');
  await fetchOrders();
}

async function complete(order) {
  await completeOrderApi(order.id);
  ElMessage.success('订单已完成');
  await fetchOrders();
}

async function refund(order) {
  const { value } = await ElMessageBox.prompt('请输入退货原因（选填）', '申请退货', {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：商品破损',
    inputValue: '',
  }).catch(() => ({ value: null }));
  if (value === null) {
    return;
  }
  await refundOrderApi(order.id, { reason: value || '', proofUrls: [] });
  ElMessage.success('已提交退货申请');
  await fetchOrders();
}
</script>

<style scoped>
.action-row {
  margin-top: 12px;
}

.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
