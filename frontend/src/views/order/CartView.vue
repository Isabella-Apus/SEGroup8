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

    <el-dialog v-model="checkoutVisible" title="确认结算" width="860px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon title="请先确认收货地址，再选择可用优惠券后提交订单。" style="margin-bottom: 12px" />
      <el-form label-width="100px">
        <el-form-item label="收货地址">
          <div v-if="selectedAddress" class="address-box">
            <div>{{ selectedAddress.receiverName }} {{ selectedAddress.receiverPhone }}</div>
            <div>{{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.detailAddress }}</div>
          </div>
        </el-form-item>
        <el-form-item label="我的优惠券">
          <el-select v-model="selectedVoucherId" placeholder="不使用优惠券" clearable filterable style="width: 100%">
            <el-option :value="null" label="不使用优惠券" />
            <el-option
              v-for="voucher in availableVouchers"
              :key="voucher.id"
              :value="voucher.id"
              :label="voucherOptionLabel(voucher)"
            />
          </el-select>
          <div v-if="availableVouchers.length === 0 && unavailableVoucherReasons.length" class="voucher-reasons">
            <div class="voucher-reasons__title">当前暂无可用优惠券，可能原因：</div>
            <ul>
              <li v-for="(reason, idx) in unavailableVoucherReasons" :key="idx">{{ reason }}</li>
            </ul>
          </div>
        </el-form-item>
      </el-form>

      <el-table :data="checkoutPreviewItems" border stripe style="margin-top: 12px">
        <el-table-column prop="name" label="商品" min-width="220" />
        <el-table-column prop="price" label="单价" width="120">
          <template #default="scope">￥{{ Number(scope.row.price || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column label="小计" width="120">
          <template #default="scope">￥{{ (Number(scope.row.price || 0) * Number(scope.row.quantity || 0)).toFixed(2) }}</template>
        </el-table-column>
      </el-table>

      <div class="pay-summary">
        <div>商品金额：￥{{ previewTotal.toFixed(2) }}</div>
        <div>优惠金额：-￥{{ previewDiscount.toFixed(2) }}</div>
        <div class="payable">应付金额：￥{{ previewPayable.toFixed(2) }}</div>
      </div>

      <template #footer>
        <el-button @click="checkoutVisible = false">取消</el-button>
        <el-button type="primary" :loading="placingOrder" @click="submitCheckout">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { createOrderApi } from '@/api/order';
import { getProductDetailApi } from '@/api/product';
import { listAddressesApi } from '@/api/user';
import { myAvailableVoucherApi, myUnavailableVoucherReasonsApi } from '@/api/voucher';
import { clearCart, getCartItems, removeFromCart, saveCartItems } from '@/utils/cart';

const router = useRouter();
const items = ref([]);
const selectedItems = ref([]);
const checkoutVisible = ref(false);
const placingOrder = ref(false);
const selectedAddress = ref(null);
const availableVouchers = ref([]);
const selectedVoucherId = ref(null);
const checkoutPreviewItems = ref([]);
const unavailableVoucherReasons = ref([]);

const selectedTotalAmount = computed(() => {
  return selectedItems.value
    .reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0)
    .toFixed(2);
});
const previewTotal = computed(() => checkoutPreviewItems.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0));
const selectedVoucher = computed(() => availableVouchers.value.find((v) => v.id === selectedVoucherId.value) || null);
const previewDiscount = computed(() => calcDiscount(selectedVoucher.value, previewTotal.value));
const previewPayable = computed(() => Math.max(0.01, previewTotal.value - previewDiscount.value));

onMounted(async () => {
  await refreshCartItems();
});

watch(selectedVoucherId, () => {
  // 触发 computed 重新计算即可
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

  const addr = await confirmAddressAndPick();
  if (!addr) return;
  selectedAddress.value = addr;
  checkoutPreviewItems.value = validCheckoutItems;
  selectedVoucherId.value = null;
  await loadMyVouchers();
  checkoutVisible.value = true;
}

async function handleBuyNow(product, qty) {
  const addr = await confirmAddressAndPick();
  if (!addr) return;
  selectedAddress.value = addr;
  checkoutPreviewItems.value = [{ ...product, quantity: qty }];
  selectedVoucherId.value = null;
  await loadMyVouchers();
  checkoutVisible.value = true;
}

async function loadMyVouchers() {
  const totalAmount = previewTotal.value || checkoutPreviewItems.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0);
  const detailResults = await Promise.allSettled(
    checkoutPreviewItems.value.map((item) => getProductDetailApi(item.productId))
  );
  const shopIds = Array.from(new Set(
    detailResults
      .filter((r) => r.status === 'fulfilled')
      .map((r) => r.value?.data?.sellerUserId ?? r.value?.data?.shopId)
      .filter((id) => id !== null && id !== undefined)
  ));
  const params = {
    page: 1,
    pageSize: 100,
    shopIds: shopIds.join(','),
    totalAmount: Number(totalAmount.toFixed(2))
  };
  const [availableResult, reasonResult] = await Promise.all([
    myAvailableVoucherApi(params),
    myUnavailableVoucherReasonsApi({ shopIds: params.shopIds, totalAmount: params.totalAmount })
  ]);
  availableVouchers.value = availableResult?.data?.records || [];
  unavailableVoucherReasons.value = reasonResult?.data || [];
}

function calcDiscount(voucher, totalAmount) {
  if (!voucher) return 0;
  if (voucher.type === 1) {
    const d = Number(voucher.discountAmount || 0);
    return Math.min(d, Math.max(0, totalAmount - 0.01));
  }
  if (voucher.type === 2) {
    const rate = Number(voucher.discountRate || 0);
    return totalAmount * (1 - rate);
  }
  return 0;
}

function voucherOptionLabel(voucher) {
  const rule = voucher.type === 1
    ? (voucher.minAmount > 0 ? `满${Number(voucher.minAmount).toFixed(2)}减${Number(voucher.discountAmount).toFixed(2)}` : `无门槛减${Number(voucher.discountAmount).toFixed(2)}`)
    : (voucher.minAmount > 0 ? `满${Number(voucher.minAmount).toFixed(2)}打${(Number(voucher.discountRate) * 10).toFixed(1)}折` : `无门槛打${(Number(voucher.discountRate) * 10).toFixed(1)}折`);
  return `${voucher.name}｜${rule}`;
}

async function confirmAddressAndPick() {
  const result = await listAddressesApi();
  const addresses = result?.data || [];
  if (!addresses.length) {
    ElMessage.warning('请先新增收货地址后再下单');
    router.push({ name: 'addressManager' });
    return null;
  }
  const preferred = addresses.find((a) => Number(a.isDefault) === 1) || addresses[0];
  const summary = `${preferred.receiverName} ${preferred.receiverPhone}\n${preferred.province}${preferred.city}${preferred.detailAddress}`;
  const confirmed = await ElMessageBox.confirm(`请确认本次收货地址：\n${summary}`, '确认收货地址', {
    confirmButtonText: '继续',
    cancelButtonText: '去改地址',
    type: 'warning'
  }).then(() => true).catch(() => {
    router.push({ name: 'addressManager' });
    return false;
  });
  if (!confirmed) return null;
  return preferred;
}

async function submitCheckout() {
  if (!selectedAddress.value) return;
  placingOrder.value = true;
  try {
    const result = await createOrderApi({
      addressId: selectedAddress.value.id,
      voucherId: selectedVoucherId.value,
      items: checkoutPreviewItems.value.map((item) => ({
        productId: item.productId,
        quantity: Number(item.quantity || 0)
      }))
    });
    const checkoutIds = new Set(checkoutPreviewItems.value.map((item) => item.productId));
    const remain = items.value.filter((item) => !checkoutIds.has(item.productId));
    saveCartItems(remain);
    items.value = remain;
    selectedItems.value = [];
    checkoutVisible.value = false;
    ElMessage.success('下单成功');
    const orderId = result?.data?.id;
    if (orderId) {
      router.push({ path: `/order/${orderId}`, query: { action: 'pay' } });
      return;
    }
    router.push('/order');
  } finally {
    placingOrder.value = false;
  }
}
</script>

<style scoped>
.cart-footer {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.total { font-size: 18px; }
.total strong { color: #ef4444; }
.address-box {
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
  width: 100%;
}
.pay-summary {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
  display: grid;
  gap: 6px;
  font-size: 14px;
}
.payable { font-size: 18px; color: #ef4444; font-weight: 700; }
.voucher-reasons {
  margin-top: 8px;
  padding: 8px 10px;
  border: 1px dashed #f59e0b;
  border-radius: 6px;
  background: #fffbeb;
  color: #92400e;
  font-size: 12px;
}
.voucher-reasons__title {
  font-weight: 600;
  margin-bottom: 4px;
}
.voucher-reasons ul {
  margin: 0;
  padding-left: 18px;
}
</style>
