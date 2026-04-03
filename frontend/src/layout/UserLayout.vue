<template>
  <el-container style="min-height: 100vh">
    <el-header style="display: flex; justify-content: space-between; align-items: center">
      <strong>购物与二手交易平台</strong>
      <div>
        <el-space>
          <span>{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '游客' }}</span>
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
          <el-menu-item index="/profile">个人资料</el-menu-item>
          <el-menu-item index="/addresses">地址管理</el-menu-item>
          <el-menu-item v-if="userStore.currentRole === 'OFFICIAL_SELLER'" index="/merchant">进入卖家工作台</el-menu-item>
          <el-menu-item v-else index="/merchant-apply">申请成为卖家</el-menu-item>
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
import { useUserStore } from '@/stores/user';

const router = useRouter();
const userStore = useUserStore();

function handleLogout() {
  userStore.logout();
  router.push('/login');
}

function goProfile() {
  router.push('/profile');
}
</script>
