<template>
  <section class="page-card">
    <h2>入驻审核</h2>
    <div class="actions">
      <el-button type="primary" @click="load">查询</el-button>
    </div>
    <el-table :data="rows" border>
      <el-table-column prop="storeName" label="店名" min-width="180" />
      <el-table-column prop="statusText" label="状态" width="120" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button v-if="scope.row.status === 0" link type="success" @click="approve(scope.row.id)">通过</el-button>
          <el-button v-if="scope.row.status === 0" link type="danger" @click="openReject(scope.row.id)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="rejectVisible" title="驳回申请" width="460px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入驳回理由" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';

const rows = ref([{ id: 1, storeName: '好物小店', status: 0, statusText: '待审核' }]);
const rejectVisible = ref(false);
const rejectReason = ref('');
const pendingRejectId = ref(null);

function load() {
  ElMessage.success('查询按钮已复现');
}

function approve(id) {
  rows.value = rows.value.map((row) => (row.id === id ? { ...row, status: 1, statusText: '已通过' } : row));
  ElMessage.success('通过按钮已复现');
}

function openReject(id) {
  pendingRejectId.value = id;
  rejectReason.value = '';
  rejectVisible.value = true;
}

function confirmReject() {
  rows.value = rows.value.map((row) => (row.id === pendingRejectId.value ? { ...row, status: 2, statusText: '已驳回' } : row));
  rejectVisible.value = false;
  ElMessage.success('确认驳回按钮已复现');
}
</script>

<style scoped>
.page-card {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 16px;
}

.actions {
  margin-bottom: 10px;
}
</style>
