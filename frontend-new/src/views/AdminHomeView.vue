<template>
  <section class="page-card">
    <h2>管理后台首页</h2>
    <div class="actions">
      <el-button link type="primary" @click="$router.push('/admin')">后台首页</el-button>
      <el-button link type="primary" @click="$router.push('/admin/users')">用户管理</el-button>
      <el-button link type="primary" @click="$router.push('/admin/merchant-review')">入驻审核</el-button>
      <el-button link type="primary" @click="$router.push('/admin/audit-logs')">审计日志</el-button>
      <el-button link type="danger" @click="logout">退出</el-button>
    </div>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus';

function logout() {
  ElMessage.success('退出按钮已复现');
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
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
</style>
