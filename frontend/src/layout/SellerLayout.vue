<template>
  <el-container class="seller-layout">
    <el-header class="seller-header">
      <strong class="brand">卖家中心</strong>
      <el-button type="primary" plain @click="router.push('/')">返回商城</el-button>
    </el-header>
    <el-container>
      <el-aside width="230px" class="layout-aside">
        <el-menu :default-active="$route.path" router class="layout-menu">
          <el-menu-item index="/merchant/seller-products">商品管理</el-menu-item>
          <el-menu-item index="/seller/orders">订单管理</el-menu-item>
          <el-menu-item index="/seller/reviews">评价管理</el-menu-item>
          <el-menu-item index="/merchant/seller-dashboard">数据看板</el-menu-item>
          <el-menu-item index="/merchant/seller-shop">店铺设置</el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.seller-layout {
  min-height: 100vh;
}
.seller-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(120deg, #ffffff 0%, #f4faf8 100%);
  border-bottom: 1px solid #e2eaf3;
  box-shadow: 0 3px 14px rgba(18, 43, 82, 0.06);
}
.brand {
  font-size: 18px;
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
</style>