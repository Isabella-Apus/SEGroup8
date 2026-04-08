<template>
  <div class="page-card">
    <h2 class="page-title">购物车</h2>

    <el-empty v-if="items.length === 0" description="购物车暂无商品" />

    <template v-else>
      <el-table
        ref="tableRef"
        :data="items"
        row-key="productId"
        border
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
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
            <el-button link type="primary" @click="checkoutSingle(scope.row)">下单</el-button>
            <el-button link type="danger" @click="remove(scope.row.productId)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="cart-footer">
        <div class="total">
          已选 {{ selectedItems.length }} 件，合计：<strong>￥{{ selectedTotalAmount }}</strong>
        </div>
        <el-space>
          <el-button @click="toggleAllSelection(true)">全选</el-button>
          <el-button @click="toggleAllSelection(false)">取消全选</el-button>
          <el-button type="danger" plain @click="removeSelected">批量删除</el-button>
          <el-button @click="clear">清空购物车</el-button>
          <el-button type="primary" @click="checkoutSelected">批量下单</el-button>
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
const tableRef = ref(null);

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
  saveCartItems(validItems);
}

function handleQtyChange() {
  saveCartItems(items.value);
}

function remove(productId) {
  items.value = removeFromCart(productId);
  selectedItems.value = selectedItems.value.filter((item) => item.productId !== productId);
}

function clear() {
  clearCart();
  items.value = [];
  selectedItems.value = [];
}

function handleSelectionChange(selection) {
  selectedItems.value = selection;
}

function toggleAllSelection(selected) {
  if (!tableRef.value) {
    return;
  }
  if (selected) {
    items.value.forEach((item) => {
      tableRef.value.toggleRowSelection(item, true);
    });
    return;
  }
  tableRef.value.clearSelection();
}

async function submitOrder(targetItems) {
  if (!targetItems.length) {
    ElMessage.warning('请先选择要下单的商品');
    return;
  }

  await createOrderApi({
    items: targetItems.map((item) => ({
      productId: item.productId,
      quantity: Number(item.quantity || 0)
    }))
  });

  const orderedIds = new Set(targetItems.map((item) => item.productId));
  const remain = items.value.filter((item) => !orderedIds.has(item.productId));
  saveCartItems(remain);
  items.value = remain;
  selectedItems.value = [];
  ElMessage.success('下单成功');
  router.push('/order');
}

async function checkoutSelected() {
  await refreshCartItems();
  if (!items.value.length) {
    ElMessage.warning('购物车为空或商品已下架');
    return;
  }
  const selectedIds = new Set(selectedItems.value.map((item) => item.productId));
  const targets = items.value.filter((item) => selectedIds.has(item.productId));
  await submitOrder(targets);
}

async function checkoutSingle(row) {
  await refreshCartItems();
  const target = items.value.find((item) => item.productId === row.productId);
  if (!target) {
    ElMessage.warning('商品不存在或已下架');
    return;
  }
  await submitOrder([target]);
}

function removeSelected() {
  if (!selectedItems.value.length) {
    ElMessage.warning('请先选择要删除的商品');
    return;
  }
  const selectedIds = new Set(selectedItems.value.map((item) => item.productId));
  const remain = items.value.filter((item) => !selectedIds.has(item.productId));
  saveCartItems(remain);
  items.value = remain;
  selectedItems.value = [];
  ElMessage.success('已删除选中商品');
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
