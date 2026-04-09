<template>
  <el-container style="min-height: 100vh">
    <el-header style="display: flex; justify-content: space-between; align-items: center">
      <strong>管理后台</strong>
      <el-space>
        <el-button link type="primary" @click="router.push('/admin')">后台首页</el-button>
        <el-button link type="primary" @click="router.push('/admin/users')">用户管理</el-button>
        <el-button link type="primary" @click="router.push('/admin/merchant-review')">入驻审核</el-button>
        <el-button link type="primary" @click="router.push('/admin/audit-logs')">审计日志</el-button>
        <el-button link type="primary" @click="router.push('/admin/orders')">订单管理</el-button>
        <el-button link type="danger" @click="logout">退出</el-button>
      </el-space>
    </el-header>
    <el-main><router-view /></el-main>
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
