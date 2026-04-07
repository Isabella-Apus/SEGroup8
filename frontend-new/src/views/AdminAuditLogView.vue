<template>
  <section class="page-card">
    <h2>审计日志</h2>
    <div class="actions">
      <el-input v-model="adminUsername" placeholder="管理员账号" style="width: 180px" />
      <el-button type="primary" @click="loadLogs">查询</el-button>
    </div>
    <el-table :data="rows" border>
      <el-table-column prop="adminUsername" label="管理员" width="120" />
      <el-table-column prop="action" label="操作" width="160" />
      <el-table-column prop="detail" label="详情" />
    </el-table>
  </section>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';

const adminUsername = ref('');
const rows = ref([{ adminUsername: 'admin', action: 'BAN_USER', detail: '管理员封禁用户' }]);

function loadLogs() {
  ElMessage.success('查询按钮已复现');
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
  display: flex;
  gap: 8px;
}
</style>
