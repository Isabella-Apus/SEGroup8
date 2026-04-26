<template>
  <div class="page-card">
    <h2 class="page-title">商品详情</h2>
    <el-skeleton v-if="loading" :rows="6" animated />

    <div v-else-if="product" class="detail-wrap">
      <div class="cover-box">
        <el-image v-if="product.cover" :src="toFullImageUrl(product.cover)" fit="cover" class="cover-image" />
        <div v-else class="cover-placeholder">暂无图片</div>
      </div>

      <div class="info-box">
        <h3>{{ product.name }}</h3>
        <p class="price">￥{{ Number(product.price || 0).toFixed(2) }}</p>
        <p>库存：{{ product.stock }}</p>
        <p>状态：{{ product.statusName }}</p>
        <p v-if="product.sellerName">卖家：{{ product.sellerName }}</p>
        <p class="desc">{{ product.description || "暂无商品描述" }}</p>

        <div class="actions">
          <span>购买数量：</span>
          <el-input-number v-model="quantity" :min="1" :max="maxQuantity" @change="recalcPreview" />
        </div>

        <el-space>
          <el-button type="primary" :disabled="maxQuantity <= 0" @click="handleAddToCart">加入购物车</el-button>
          <el-button :disabled="maxQuantity <= 0" @click="openBuyNow">立即购买</el-button>
          <el-button v-if="canChatWithSeller" type="success" plain @click="handleContactSeller">联系卖家</el-button>
          <el-button text @click="goBack">返回</el-button>
        </el-space>
      </div>
    </div>

    <p v-else class="empty-tip">商品不存在</p>

    <el-dialog v-model="buyDialogVisible" title="立即购买" width="860px" destroy-on-close>
      <el-alert type="info" :closable="false" show-icon title="请选择收货地址并可直接使用我的优惠券，选中后应付金额会实时更新。" style="margin-bottom: 12px" />
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
            <el-option v-for="voucher in availableVouchers" :key="voucher.id" :value="voucher.id" :label="voucherOptionLabel(voucher)" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="previewItems" border stripe style="margin-top: 12px">
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
        <el-button @click="buyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="buySubmitting" @click="confirmBuyNow">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { getProductDetailApi } from "@/api/product";
import { createOrderApi } from "@/api/order";
import { listAddressesApi } from "@/api/user";
import { myAvailableVoucherApi } from "@/api/voucher";
import { addToCart, removeFromCart } from "@/utils/cart";
import { getUser } from "@/utils/storage";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const product = ref(null);
const quantity = ref(1);
const buyDialogVisible = ref(false);
const buySubmitting = ref(false);
const selectedAddress = ref(null);
const availableVouchers = ref([]);
const selectedVoucherId = ref(null);
const previewItems = ref([]);

const maxQuantity = computed(() => Number(product.value?.stock || 0));
const canChatWithSeller = computed(() => {
  if (!product.value?.sellerUserId) return false;
  return Number(product.value.sellerUserId) !== Number(getUser()?.id);
});
const previewTotal = computed(() => previewItems.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0));
const selectedVoucher = computed(() => availableVouchers.value.find((v) => v.id === selectedVoucherId.value) || null);
const previewDiscount = computed(() => calcDiscount(selectedVoucher.value, previewTotal.value));
const previewPayable = computed(() => Math.max(0.01, previewTotal.value - previewDiscount.value));

onMounted(async () => {
  await fetchDetail();
});

watch(selectedVoucherId, () => {});

async function fetchDetail() {
  loading.value = true;
  try {
    const result = await getProductDetailApi(route.params.id);
    product.value = result.data;
    if (maxQuantity.value > 0) quantity.value = 1;
  } finally {
    loading.value = false;
  }
}

function handleAddToCart() {
  if (!product.value) return;
  if (quantity.value > maxQuantity.value) {
    ElMessage.warning("购买数量超过库存");
    return;
  }
  addToCart(product.value, Number(quantity.value));
  ElMessage.success("已加入购物车");
}

async function openBuyNow() {
  if (!product.value) return;
  if (quantity.value > maxQuantity.value) {
    ElMessage.warning("购买数量超过库存");
    return;
  }
  const addr = await confirmAddressAndPick();
  if (!addr) return;
  selectedAddress.value = addr;
  previewItems.value = [{ ...product.value, quantity: Number(quantity.value) }];
  selectedVoucherId.value = null;
  await loadMyVouchers();
  buyDialogVisible.value = true;
}

async function loadMyVouchers() {
  const totalAmount = previewTotal.value;
  const shopIds = product.value?.sellerUserId
    ? [product.value.sellerUserId]
    : (product.value?.shopId ? [product.value.shopId] : []);
  const result = await myAvailableVoucherApi({
    page: 1,
    pageSize: 100,
    shopIds: shopIds.join(','),
    totalAmount: Number(totalAmount.toFixed(2))
  });
  availableVouchers.value = result?.data?.records || [];
}

function calcDiscount(voucher, totalAmount) {
  if (!voucher) return 0;
  if (voucher.type === 1) return Math.min(Number(voucher.discountAmount || 0), Math.max(0, totalAmount - 0.01));
  if (voucher.type === 2) return totalAmount * (1 - Number(voucher.discountRate || 0));
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
    ElMessage.warning("请先新增收货地址");
    router.push({ name: "addressManager" });
    return null;
  }
  const preferred = addresses.find((item) => Number(item.isDefault) === 1) || addresses[0];
  const summary = `${preferred.receiverName} ${preferred.receiverPhone}\n${preferred.province}${preferred.city}${preferred.detailAddress}`;
  const confirmed = await ElMessageBox.confirm(`请确认收货地址：\n${summary}`, "收货地址确认", {
    confirmButtonText: "提交订单",
    cancelButtonText: "修改地址",
    type: "warning",
  }).then(() => true).catch(() => {
    router.push({ name: "addressManager" });
    return false;
  });
  if (!confirmed) return null;
  return preferred;
}

async function confirmBuyNow() {
  if (!selectedAddress.value) return;
  buySubmitting.value = true;
  try {
    const result = await createOrderApi({
      addressId: selectedAddress.value.id,
      voucherId: selectedVoucherId.value,
      items: previewItems.value.map((item) => ({
        productId: item.id,
        quantity: Number(item.quantity || 0),
      })),
    });
    removeFromCart(product.value.id);
    ElMessage.success("下单成功");
    buyDialogVisible.value = false;
    const orderId = result?.data?.id;
    if (orderId) {
      router.push({ path: `/order/${orderId}`, query: { action: "pay" } });
      return;
    }
    router.push("/order");
  } finally {
    buySubmitting.value = false;
  }
}

function handleContactSeller() {
  if (!product.value?.sellerUserId) {
    ElMessage.warning("当前无法联系卖家");
    return;
  }
  router.push({ path: "/messages", query: { participantId: product.value.sellerUserId, sourceType: "PRODUCT", sourceId: product.value.id } });
}

function toFullImageUrl(url) {
  if (!url) return "";
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  const normalized = url.startsWith("/") ? url : `/${url}`;
  return `http://localhost:8080${normalized}`;
}

function goBack() {
  if (route.query.from === "browse-history") {
    router.push("/browse-history");
    return;
  }
  router.push("/product");
}
</script>

<style scoped>
.detail-wrap { display: grid; grid-template-columns: 280px 1fr; gap: 24px; }
.cover-box { width: 280px; height: 280px; border: 1px solid #e5e7eb; border-radius: 10px; overflow: hidden; }
.cover-image { width: 100%; height: 100%; }
.cover-placeholder { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; color: #6b7280; background: #f9fafb; }
.price { color: #ef4444; font-size: 22px; font-weight: 700; margin: 8px 0; }
.desc { color: #4b5563; line-height: 1.8; }
.actions { margin: 14px 0; }
.address-box { padding: 10px 12px; border: 1px solid #e5e7eb; border-radius: 8px; background: #fafafa; width: 100%; }
.pay-summary { margin-top: 12px; padding: 12px; border: 1px solid #e5e7eb; border-radius: 8px; background: #fafafa; display: grid; gap: 6px; font-size: 14px; }
.payable { font-size: 18px; color: #ef4444; font-weight: 700; }
@media (max-width: 900px) { .detail-wrap { grid-template-columns: 1fr; } .cover-box { width: 100%; max-width: 360px; margin: 0 auto; } }
</style>
