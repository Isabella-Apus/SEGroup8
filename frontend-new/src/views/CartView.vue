<template>
  <section class="page-card">
    <h2 class="title">购物车</h2>
    <el-table :data="items" border>
      <el-table-column prop="name" label="商品名" min-width="220" />
      <el-table-column prop="price" label="单价" width="110" />
      <el-table-column prop="quantity" label="数量" width="90" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button link type="danger" @click="remove(scope.row.productId)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="actions">
      <el-button @click="clear">清空购物车</el-button>
      <el-button type="primary" @click="checkout">提交订单</el-button>
    </div>
  </section>
</template>

<script setup>
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { clearCart, getCartItems, removeFromCart } from '@/utils/cart';

const items = ref(getCartItems());

const totalAmount = computed(() => {
  return items.value.reduce((acc, item) => acc + Number(item.price || 0) * Number(item.quantity || 0), 0);
});

function remove(productId) {
  items.value = removeFromCart(productId);
  ElMessage.success('已移除');
}

function clear() {
  items.value = clearCart();
  ElMessage.success('购物车已清空');
}

function checkout() {
  if (!items.value.length) {
    ElMessage.warning('购物车为空');
    return;
  }
  ElMessage.success(`提交订单成功，金额 ¥${totalAmount.value.toFixed(2)}`);
  items.value = clearCart();
}
</script>

<style scoped>
.page-card {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 14px;
}

.title {
  margin: 0 0 12px;
}

.actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
