<template>
  <el-container class="admin-layout">
    <el-header class="admin-header">
      <strong class="brand">管理后台</strong>
      <el-space>
        <el-button link type="primary" @click="router.push('/admin')">后台首页</el-button>
        <el-button link type="primary" @click="router.push('/admin/users')">用户管理</el-button>
        <el-button link type="primary" @click="router.push('/admin/merchant-review')">入驻审核</el-button>
        <el-button link type="primary" @click="router.push('/admin/orders')">订单管理</el-button>
        <el-button link type="primary" @click="router.push('/admin/vouchers')">优惠券管理</el-button>
        <el-button link type="primary" @click="router.push('/admin/audit-logs')">审计日志</el-button>
        <el-button link type="danger" @click="logout">退出</el-button>
      </el-space>
    </el-header>
    <el-main class="admin-main fade-in-up"><router-view /></el-main>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const userStore = useUserStore();

function logout() {
  userStore.logout();
  router.push('/login');
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(120deg, #ffffff 0%, #f8fafc 100%);
  border-bottom: 1px solid #e2eaf3;
  box-shadow: 0 3px 14px rgba(18, 43, 82, 0.06);
}

.brand {
  font-size: 18px;
}

.admin-main {
  padding: 18px;
}
</style>
