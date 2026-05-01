<template>
  <el-container class="seller-layout">
    <el-header class="seller-header">
      <strong class="brand">卖家中心</strong>
      <el-button type="primary" plain @click="router.push('/')">返回商城</el-button>
    </el-header>
    <el-container>
      <el-aside width="220px" class="layout-aside">
        <el-menu
          :default-active="$route.path"
          router
          class="layout-menu"
          :collapse="false"
        >
          <!-- 商品 -->
          <el-menu-item index="/merchant/seller-products">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>

          <!-- 订单 -->
          <el-menu-item index="/merchant/orders">
            <el-icon><List /></el-icon>
            <span>订单管理</span>
          </el-menu-item>

          <!-- 优惠券 -->
          <el-menu-item index="/merchant/vouchers">
            <el-icon><Ticket /></el-icon>
            <span>优惠券管理</span>
          </el-menu-item>

          <!-- 数据分组 -->
          <el-sub-menu index="data">
            <template #title>
              <el-icon><DataAnalysis /></el-icon>
              <span>数据</span>
            </template>
            <el-menu-item index="/merchant/seller-dashboard">数据看板</el-menu-item>
            <el-menu-item index="/merchant/account-health">账户健康</el-menu-item>
          </el-sub-menu>

          <!-- 客服分组 -->
          <el-sub-menu index="service">
            <template #title>
              <el-icon><ChatDotRound /></el-icon>
              <span>客服</span>
            </template>
            <el-menu-item index="/merchant/reviews">评价管理</el-menu-item>
          </el-sub-menu>

          <!-- 店铺分组 -->
          <el-sub-menu index="shop">
            <template #title>
              <el-icon><Shop /></el-icon>
              <span>店铺</span>
            </template>
            <el-menu-item index="/merchant/seller-shop">店铺信息</el-menu-item>
            <el-menu-item index="/merchant/shop-decoration">店铺装修</el-menu-item>
          </el-sub-menu>
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
import {
  Goods,
  List,
  Ticket,
  DataAnalysis,
  ChatDotRound,
  Shop
} from '@element-plus/icons-vue'

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
  min-height: calc(100vh - 60px);
}
.layout-menu {
  border-right: 0;
  padding-top: 8px;
}
.layout-main {
  padding: 18px;
  background: #f7f8fa;
}
</style>