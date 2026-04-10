<template>
  <div class="page-card">
    <h2 class="page-title">评价管理</h2>

    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="评价内容" clearable style="width: 220px" />
      </el-form-item>
      <el-form-item label="评分">
        <el-select v-model="query.score" clearable placeholder="全部" style="width: 120px">
          <el-option v-for="s in [1,2,3,4,5]" :key="s" :label="`${s} 星`" :value="s" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" border>
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column prop="productName" label="商品" min-width="180" />
      <el-table-column prop="score" label="评分" width="90" />
      <el-table-column prop="content" label="评价内容" min-width="220" />
      <el-table-column prop="sellerReply" label="卖家回复" min-width="220" />
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="reply(scope.row)">回复评价</el-button>
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
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getSellerReviewsApi, replyReviewApi } from '@/api/review';

const loading = ref(false);
const records = ref([]);
const total = ref(0);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  score: undefined,
});

onMounted(() => {
  fetchReviews();
});

async function fetchReviews() {
  loading.value = true;
  try {
    const result = await getSellerReviewsApi(query);
    records.value = result.data?.records || [];
    total.value = result.data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  fetchReviews();
}

function reset() {
  query.pageNum = 1;
  query.pageSize = 10;
  query.keyword = '';
  query.score = undefined;
  fetchReviews();
}

function handlePageChange(pageNum) {
  query.pageNum = pageNum;
  fetchReviews();
}

function handleSizeChange(pageSize) {
  query.pageSize = pageSize;
  query.pageNum = 1;
  fetchReviews();
}

async function reply(row) {
  const { value } = await ElMessageBox.prompt('请输入回复内容', '回复评价', {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
    inputPlaceholder: '最多 500 字',
    inputValue: row.sellerReply || '',
  }).catch(() => ({ value: null }));
  if (value == null) {
    return;
  }
  await replyReviewApi(row.id, { reply: value });
  ElMessage.success('回复成功');
  await fetchReviews();
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
