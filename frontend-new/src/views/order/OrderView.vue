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
          <el-descriptions-item label="订单状态">{{ order.orderStatus === 1 ? '已创建' : order.orderStatus }}</el-descriptions-item>
        </el-descriptions>

        <el-table :data="order.items || []" border style="margin-top: 10px">
          <el-table-column prop="productName" label="商品名" min-width="220" />
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
import { getOrderListApi } from '@/api/order';

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
</script>

<style scoped>
.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
