<template>
  <section class="page-card">
    <h2>用户管理</h2>
    <div class="actions">
      <el-button type="primary" @click="loadUsers">查询</el-button>
    </div>
    <el-table :data="rows" border>
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button v-if="scope.row.status !== 'BANNED'" link type="danger" @click="changeStatus(scope.row, 'ban')">封禁</el-button>
          <el-button v-else link type="success" @click="changeStatus(scope.row, 'unban')">解封</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { ref } from 'vue';
import { ElMessage } from 'element-plus';

const rows = ref([{ username: 'demo_user', status: 'NORMAL' }]);

function loadUsers() {
  ElMessage.success('查询按钮已复现');
}

function changeStatus(row, action) {
  row.status = action === 'ban' ? 'BANNED' : 'NORMAL';
  ElMessage.success(action === 'ban' ? '封禁按钮已复现' : '解封按钮已复现');
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
