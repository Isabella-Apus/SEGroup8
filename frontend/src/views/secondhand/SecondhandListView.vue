<template>
  <div class="page-card">
    <h2 class="page-title">二手商品</h2>
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="输入二手商品名" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="最低价">
        <el-input-number v-model="query.minPrice" :min="0" :precision="2" :step="10" style="width: 140px" />
      </el-form-item>
      <el-form-item label="最高价">
        <el-input-number v-model="query.maxPrice" :min="0" :precision="2" :step="10" style="width: 140px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="商品名" />
      <el-table-column prop="conditionLevel" label="成色" width="120" />
      <el-table-column prop="salePrice" label="售价" width="120" />
      <el-table-column prop="statusName" label="状态" width="100" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button link type="primary" @click="handleBuy(scope.row)">购买</el-button>
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
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { buySecondhandApi, getSecondhandListApi } from "@/api/secondhand";

const list = ref([]);
const total = ref(0);
const loading = ref(false);

const query = ref({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  minPrice: undefined,
  maxPrice: undefined
});

onMounted(async () => {
  await fetchList();
});

async function fetchList() {
  loading.value = true;
  try {
    const result = await getSecondhandListApi(query.value);
    list.value = result.data?.records || [];
    total.value = result.data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.value.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.value = {
    pageNum: 1,
    pageSize: 10,
    keyword: "",
    minPrice: undefined,
    maxPrice: undefined
  };
  fetchList();
}

function handlePageChange(pageNum) {
  query.value.pageNum = pageNum;
  fetchList();
}

function handleSizeChange(pageSize) {
  query.value.pageSize = pageSize;
  query.value.pageNum = 1;
  fetchList();
}

async function handleBuy(row) {
  await ElMessageBox.confirm(`确认购买二手商品「${row.name}」吗？`, "提示", {
    type: "warning"
  });
  await buySecondhandApi(row.id, {});
  ElMessage.success("购买成功，订单已创建");
  await fetchList();
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
