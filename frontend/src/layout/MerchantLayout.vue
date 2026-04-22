<template>
  <el-container class="merchant-layout">
    <el-header class="merchant-header">
      <strong class="brand">卖家工作台</strong>
      <el-button type="primary" plain @click="router.push('/')">返回商城</el-button>
    </el-header>
    <el-container>
      <el-aside width="220px" class="layout-aside">
        <el-menu :default-active="$route.path" router class="layout-menu" :default-openeds="openeds">
          <el-sub-menu index="order">
            <template #title>
              <span class="menu-group-title">订单</span>
            </template>
            <el-menu-item index="/merchant/orders">订单管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="product">
            <template #title>
              <span class="menu-group-title">商品</span>
            </template>
            <el-menu-item index="/merchant">我的商品</el-menu-item>
            <el-menu-item index="/merchant/seller-products/edit">发布商品</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="marketing">
            <template #title>
              <span class="menu-group-title">营销</span>
            </template>
            <el-menu-item index="/merchant/vouchers">优惠券</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="finance">
            <template #title>
              <span class="menu-group-title">财务</span>
            </template>
            <el-menu-item index="/merchant/finance">财务看板</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="data">
            <template #title>
              <span class="menu-group-title">数据</span>
            </template>
            <el-menu-item index="/merchant/seller-dashboard">数据分析</el-menu-item>
            <el-menu-item index="/merchant/account-health">账户健康</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="service">
            <template #title>
              <span class="menu-group-title">客服</span>
            </template>
            <el-menu-item index="/merchant/reviews">评价管理</el-menu-item>
            <el-menu-item index="/merchant/messages">买家消息</el-menu-item>
            <el-menu-item index="/merchant/notifications">通知</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="shop">
            <template #title>
              <span class="menu-group-title">店铺</span>
            </template>
            <el-menu-item index="/merchant/seller-shop">店铺资料</el-menu-item>
            <el-menu-item index="/merchant/shop-decoration">店铺装修</el-menu-item>
          </el-sub-menu>
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

const router = useRouter();
const openeds = ["order", "product", "marketing", "finance", "data", "service", "shop"];
</script>

<style scoped>
.merchant-layout { min-height: 100vh; }
.merchant-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(120deg, #ffffff 0%, #f4faf8 100%);
  border-bottom: 1px solid #e2eaf3;
  box-shadow: 0 3px 14px rgba(18, 43, 82, 0.06);
}
.brand { font-size: 18px; }
.layout-aside {
  border-right: 1px solid #e2eaf3;
  background: #fbfdff;
  overflow-y: auto;
}
.layout-menu {
  border-right: 0;
  padding-top: 4px;
}
.menu-group-title {
  font-size: 13px;
  font-weight: 600;
  color: #555;
}
.layout-main { padding: 18px; }
</style>
