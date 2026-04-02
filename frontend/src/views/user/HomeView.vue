<template>
  <div class="page-card">
    <h2 class="page-title">首页</h2>
    <el-descriptions title="当前登录用户" :column="1" border>
      <el-descriptions-item label="账号">{{ authStore.user?.username || '-' }}</el-descriptions-item>
      <el-descriptions-item label="昵称">{{ authStore.user?.nickname || '-' }}</el-descriptions-item>
      <el-descriptions-item label="角色">{{ authStore.user?.role || '-' }}</el-descriptions-item>
    </el-descriptions>

    <h3 style="margin-top: 24px">推荐商品</h3>
    <el-table :data="products" border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="price" label="价格" width="120" />
      <el-table-column prop="stock" label="库存" width="120" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { getProductListApi } from '@/api/product';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const products = ref([]);

onMounted(async () => {
  await authStore.fetchMe();
  const result = await getProductListApi();
  products.value = result.data || [];
});
</script>
