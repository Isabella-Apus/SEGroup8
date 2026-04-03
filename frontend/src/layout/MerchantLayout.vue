<template>
    <el-container style="min-height: 100vh">
        <el-header class="merchant-header">
            <strong>卖家工作台</strong>
            <el-button type="primary" plain @click="router.push('/')">返回商城</el-button>
        </el-header>
        <el-container>
            <el-aside width="220px">
                <el-menu :default-active="$route.path" router>
                    <el-menu-item index="/merchant">商品管理</el-menu-item>
                    <el-menu-item index="/merchant/orders">订单管理</el-menu-item>
                    <el-menu-item index="/merchant/shop">店铺设置</el-menu-item>
                </el-menu>
            </el-aside>
            <el-main>
                <router-view />
            </el-main>
        </el-container>
    </el-container>
</template>

<script setup>
import { useRouter } from "vue-router";

const router = useRouter();
</script>

<style scoped>
.merchant-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}
</style>
