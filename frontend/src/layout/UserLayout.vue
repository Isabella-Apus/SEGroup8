<template>
  <el-container style="min-height: 100vh">
    <el-header style="display: flex; justify-content: space-between; align-items: center">
      <strong>购物与二手交易平台</strong>
      <div>
        <el-space>
          <span>{{ authStore.user?.nickname || authStore.user?.username || '游客' }}</span>
          <el-button size="small" @click="goProfile">个人中心</el-button>
          <el-button size="small" type="danger" @click="handleLogout">退出</el-button>
        </el-space>
      </div>
    </el-header>
    <el-container>
      <el-aside width="220px">
        <el-menu :default-active="$route.path" router>
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/product">商品</el-menu-item>
          <el-menu-item index="/cart">购物车</el-menu-item>
          <el-menu-item index="/order">我的订单</el-menu-item>
          <el-menu-item index="/secondhand">二手</el-menu-item>
          <el-menu-item index="/secondhand/publish">发布二手</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();

function handleLogout() {
  authStore.logout();
  router.push('/login');
}

function goProfile() {
  router.push('/');
}
</script>
