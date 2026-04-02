<template>
  <div class="page-card">
    <h2 class="page-title">商品列表</h2>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="商品名" />
      <el-table-column prop="price" label="价格" width="120" />
      <el-table-column prop="stock" label="库存" width="120" />
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link type="primary" @click="goDetail(scope.row.id)">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getProductListApi } from '@/api/product';

const router = useRouter();
const list = ref([]);

onMounted(async () => {
  const result = await getProductListApi();
  list.value = result.data || [];
});

function goDetail(id) {
  router.push(`/product/${id}`);
}
</script>
