<template>
  <el-container class="user-layout">
    <el-header class="layout-header">
      <strong class="brand">购物与二手交易平台</strong>
      <div class="header-actions">
        <el-space>
          <span class="nickname">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || "游客" }}</span>
          <el-button size="small" @click="goProfile">个人中心</el-button>
          <el-button size="small" type="danger" @click="handleLogout">退出登录</el-button>
        </el-space>
      </div>
    </el-header>
    <el-container>
      <el-aside width="230px" class="layout-aside">
        <el-menu :default-active="$route.path" router class="layout-menu">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/product">商品</el-menu-item>
          <el-menu-item index="/cart">购物车</el-menu-item>
          <el-menu-item index="/order">我的订单</el-menu-item>
          <el-menu-item index="/secondhand">二手商品</el-menu-item>
          <el-menu-item index="/secondhand/publish">发布二手</el-menu-item>
          <el-menu-item index="/messages">站内消息</el-menu-item>
          <el-menu-item index="/notifications">通知</el-menu-item>
          <el-menu-item index="/profile">个人资料</el-menu-item>
          <el-menu-item index="/addresses">地址管理</el-menu-item>
          <el-menu-item index="/my-reviews">我的评价</el-menu-item>
          <el-menu-item index="/browse-history">浏览记录</el-menu-item>
          <el-menu-item index="/vouchers">优惠券中心</el-menu-item>
          <el-menu-item index="/after-sale">退款/售后</el-menu-item>
          <el-menu-item v-if="userStore.currentRole === 'OFFICIAL_SELLER'" index="/merchant">进入卖家工作台</el-menu-item>
          <el-menu-item v-else index="/merchant-apply">申请成为卖家</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main fade-in-up">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const userStore = useUserStore();

function handleLogout() {
  userStore.logout();
  router.push("/login");
}

function goProfile() {
  router.push("/profile");
}
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
}

.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 62px;
  background: linear-gradient(120deg, #ffffff 0%, #f4faf8 100%);
  border-bottom: 1px solid #e2eaf3;
  box-shadow: 0 3px 14px rgba(18, 43, 82, 0.06);
}

.brand {
  font-size: 18px;
  letter-spacing: 0.3px;
}

.nickname {
  color: #4a5a72;
}

.layout-aside {
  border-right: 1px solid #e2eaf3;
  background: #fbfdff;
}

.layout-menu {
  border-right: 0;
  padding-top: 8px;
}

.layout-main {
  padding: 18px;
}

@media (max-width: 900px) {
  .layout-aside {
    width: 86px !important;
  }

  .brand {
    font-size: 15px;
  }
}
</style>
