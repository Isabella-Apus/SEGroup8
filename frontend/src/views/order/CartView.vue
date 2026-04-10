<template>
  <div class="page-card">
    <h2 class="page-title">购物车</h2>

    <el-empty v-if="items.length === 0" description="购物车暂无商品" />

    <template v-else>
      <el-table
        :data="items"
        border
        row-key="productId"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="56" reserve-selection />
        <el-table-column prop="name" label="商品名" min-width="220" />
        <el-table-column prop="price" label="单价" width="120">
          <template #default="scope">￥{{ Number(scope.row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="160">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.quantity"
              :min="1"
              :max="999"
              @change="handleQtyChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="140">
          <template #default="scope">￥{{ (Number(scope.row.price || 0) * Number(scope.row.quantity || 0)).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button link type="danger" @click="remove(scope.row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="cart-footer">
        <div class="total">已选合计：<strong>￥{{ selectedTotalAmount }}</strong></div>
        <el-space>
          <el-button @click="clear">清空购物车</el-button>
          <el-button type="primary" :disabled="selectedItems.length === 0" @click="checkout">结算选中商品</el-button>
        </el-space>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { createOrderApi } from '@/api/order';
import { getProductDetailApi } from '@/api/product';
import { clearCart, getCartItems, removeFromCart, saveCartItems } from '@/utils/cart';

const router = useRouter();
const items = ref([]);
const selectedItems = ref([]);

const selectedTotalAmount = computed(() => {
  return selectedItems.value
    .reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0)
    .toFixed(2);
});

onMounted(async () => {
  await refreshCartItems();
});

async function refreshCartItems() {
  const rawItems = getCartItems();
  const checks = await Promise.allSettled(
    rawItems.map((item) => getProductDetailApi(item.productId))
  );
  const validItems = rawItems.filter((_, index) => checks[index].status === 'fulfilled');
  items.value = validItems;
  selectedItems.value = [];
  saveCartItems(validItems);
}

function handleQtyChange() {
  saveCartItems(items.value);
}

function handleSelectionChange(selection) {
  selectedItems.value = selection;
}

function remove(row) {
  items.value = removeFromCart(row.productId);
  selectedItems.value = selectedItems.value.filter((item) => item.productId !== row.productId);
}

function clear() {
  clearCart();
  items.value = [];
  selectedItems.value = [];
}

async function checkout() {
  const toCheckout = [...selectedItems.value];
  if (!toCheckout.length) {
    ElMessage.warning('请先选择要结算的商品');
    return;
  }
  const checks = await Promise.allSettled(
    toCheckout.map((item) => getProductDetailApi(item.productId))
  );
  const validCheckoutItems = toCheckout.filter((_, index) => checks[index].status === 'fulfilled');
  if (!validCheckoutItems.length) {
    ElMessage.warning('选中商品已失效，请重新选择');
    await refreshCartItems();
    return;
  }
  const result = await createOrderApi({
    items: validCheckoutItems.map((item) => ({
      productId: item.productId,
      quantity: Number(item.quantity || 0)
    }))
  });
  const checkoutIds = new Set(validCheckoutItems.map((item) => item.productId));
  const remain = items.value.filter((item) => !checkoutIds.has(item.productId));
  saveCartItems(remain);
  items.value = remain;
  selectedItems.value = [];
  ElMessage.success('下单成功');
  const orderId = result?.data?.id;
  if (orderId) {
    router.push({ path: `/order/${orderId}`, query: { action: 'pay' } });
    return;
  }
  router.push('/order');
}
</script>

<style scoped>
.cart-footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total {
  font-size: 18px;
}

.total strong {
  color: #ef4444;
}
</style>
