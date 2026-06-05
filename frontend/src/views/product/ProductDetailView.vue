<template>
  <section class="detail-page">
    <el-skeleton v-if="loading" :rows="8" animated class="page-card" />

    <div v-else-if="product" class="detail-shell">
      <div class="gallery">
        <el-image :src="selectedImage" fit="cover" class="main-image" />
        <div class="thumbs" v-if="imageList.length > 1">
          <button
            v-for="image in imageList"
            :key="image"
            class="thumb"
            :class="{ active: selectedImage === image }"
            type="button"
            @click="selectedImage = image"
          >
            <img :src="image" :alt="product.name" />
          </button>
        </div>
      </div>

      <div class="buy-panel">
        <div class="crumb">商品市场 / 官方商品</div>
        <h1>{{ product.name }}</h1>
        <p class="desc">{{ product.description || "暂无商品描述" }}</p>

        <div class="price-box">
          <span>到手价</span>
          <strong>¥{{ Number(product.price || 0).toFixed(2) }}</strong>
        </div>

        <div class="promo-line">
          <span>价格清晰</span>
          <span>支持售后</span>
          <span>库存同步</span>
        </div>

        <dl class="info-grid">
          <div>
            <dt>库存</dt>
            <dd>{{ product.stock ?? 0 }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ productDisplayStatus }}</dd>
          </div>
          <div>
            <dt>卖家</dt>
            <dd>{{ product.sellerName || product.shopName || "官方商家" }}</dd>
          </div>
        </dl>

        <div class="quantity-row">
          <span>购买数量</span>
          <el-input-number v-model="quantity" :min="1" :max="maxQuantity || 1" />
        </div>

        <div class="coupon-select-row">
          <span>优惠券</span>
          <el-select
            v-model="selectedVoucherId"
            clearable
            :loading="couponsLoading"
            placeholder="不使用优惠券"
          >
            <el-option
              v-for="coupon in availableCoupons"
              :key="coupon.id"
              :label="formatCouponOption(coupon)"
              :value="coupon.id"
            />
          </el-select>
          <small v-if="getUser()?.id && !couponsLoading && !availableCoupons.length">当前商品暂无可用优惠券</small>
          <small v-else-if="!getUser()?.id">登录并领券后可在此选择</small>
        </div>

        <div class="order-preview">
          <span>本单应付</span>
          <div>
            <small v-if="couponDiscountAmount > 0">已优惠 ¥{{ couponDiscountAmount.toFixed(2) }}</small>
            <strong>¥{{ payableAmount }}</strong>
          </div>
        </div>

        <div class="actions">
          <el-button type="warning" size="large" :disabled="maxQuantity <= 0" @click="handleBuyNow">
            立即购买
          </el-button>
          <el-button type="primary" size="large" :disabled="maxQuantity <= 0" @click="handleAddToCart">
            加入购物车
          </el-button>
          <el-button size="large" @click="handleContactSeller">
            联系卖家
          </el-button>
          <el-button size="large" :disabled="!product.shopId" @click="handleEnterShop">
            <el-icon><Shop /></el-icon>
            进入店家
          </el-button>
        </div>

        <div class="service-row">
          <span>支持售后</span>
          <span>地址确认后下单</span>
          <span>订单页可追踪状态</span>
        </div>
      </div>
    </div>

    <p v-else class="empty-tip page-card">商品不存在</p>
  </section>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Shop } from "@element-plus/icons-vue";
import { useRoute, useRouter } from "vue-router";
import { getProductDetailApi } from "@/api/product";
import { createOrderApi } from "@/api/order";
import { listCheckoutCouponsApi } from "@/api/coupon";
import { listAddressesApi, recordBrowseHistoryApi } from "@/api/user";
import { addToCart, removeFromCart } from "@/utils/cart";
import { getUser } from "@/utils/storage";
import { toAssetUrl } from "@/utils/url";
import { formatCouponOption, getCouponDiscount } from "@/utils/coupon";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const product = ref(null);
const quantity = ref(1);
const selectedImage = ref("");
const availableCoupons = ref([]);
const selectedVoucherId = ref(null);
const couponsLoading = ref(false);
let couponRequestId = 0;

const maxQuantity = computed(() => Number(product.value?.stock || 0));
const productDisplayStatus = computed(() => maxQuantity.value <= 0 ? "已售罄" : (product.value?.statusName || "在售"));
const subtotalAmount = computed(() => Number(product.value?.price || 0) * Number(quantity.value || 0));
const shopAmounts = computed(() => {
  const amounts = new Map();
  if (product.value?.shopId) {
    amounts.set(Number(product.value.shopId), subtotalAmount.value);
  }
  return amounts;
});
const selectedCoupon = computed(() => availableCoupons.value
  .find((coupon) => Number(coupon.id) === Number(selectedVoucherId.value)) || null);
const couponDiscountAmount = computed(() => getCouponDiscount(
  selectedCoupon.value,
  subtotalAmount.value,
  shopAmounts.value,
));
const payableAmount = computed(() => Math.max(0, subtotalAmount.value - couponDiscountAmount.value).toFixed(2));
const couponContextKey = computed(() => `${product.value?.shopId || ""}|${subtotalAmount.value.toFixed(2)}`);
const imageList = computed(() => {
  if (!product.value) {
    return [];
  }
  const images = Array.isArray(product.value.images) ? product.value.images : [];
  const list = [product.value.cover, ...images]
    .filter(Boolean)
    .map((item) => toAssetUrl(item))
    .filter(Boolean);
  return [...new Set(list)];
});

const canChatWithSeller = computed(() => {
  const sellerId = getSellerUserId();
  if (!sellerId) {
    return false;
  }
  return Number(sellerId) !== Number(getUser()?.id);
});

watch(() => route.params.id, () => {
  fetchDetail();
}, { immediate: true });

watch(couponContextKey, loadAvailableCoupons);

async function fetchDetail() {
  loading.value = true;
  try {
    const result = await getProductDetailApi(route.params.id);
    product.value = result.data;
    selectedImage.value = imageList.value[0] || "https://images.unsplash.com/photo-1511556820780-d912e42b4980?auto=format&fit=crop&w=900&q=80";
    quantity.value = maxQuantity.value > 0 ? 1 : 0;
    recordCurrentProduct();
  } finally {
    loading.value = false;
  }
}

async function loadAvailableCoupons() {
  const requestId = ++couponRequestId;
  if (!getUser()?.id || !product.value?.shopId || subtotalAmount.value <= 0) {
    availableCoupons.value = [];
    selectedVoucherId.value = null;
    return;
  }
  couponsLoading.value = true;
  try {
    const result = await listCheckoutCouponsApi({
      page: 1,
      pageSize: 100,
      shopIds: String(product.value.shopId),
      totalAmount: subtotalAmount.value.toFixed(2),
    });
    if (requestId !== couponRequestId) return;
    availableCoupons.value = (result?.data?.records || []).filter((coupon) => (
      getCouponDiscount(coupon, subtotalAmount.value, shopAmounts.value) > 0
    ));
    if (!availableCoupons.value.some((coupon) => Number(coupon.id) === Number(selectedVoucherId.value))) {
      selectedVoucherId.value = null;
    }
  } catch {
    if (requestId === couponRequestId) {
      availableCoupons.value = [];
      selectedVoucherId.value = null;
    }
  } finally {
    if (requestId === couponRequestId) couponsLoading.value = false;
  }
}

async function recordCurrentProduct() {
  if (!product.value?.id || !getUser()?.id) {
    return;
  }
  try {
    await recordBrowseHistoryApi({
      productId: product.value.id,
      productType: "NEW",
    });
  } catch {
    // Browse history is helpful but should never block product viewing.
  }
}

function handleAddToCart() {
  if (!product.value) {
    return;
  }
  if (quantity.value > maxQuantity.value) {
    ElMessage.warning("购买数量超过库存");
    return;
  }
  addToCart(product.value, Number(quantity.value));
  ElMessage.success("已加入购物车");
}

async function handleBuyNow() {
  if (!product.value) {
    return;
  }
  if (quantity.value > maxQuantity.value) {
    ElMessage.warning("购买数量超过库存");
    return;
  }
  const selectedAddressId = await confirmAddressAndPickId();
  if (!selectedAddressId) {
    return;
  }
  const result = await createOrderApi({
    addressId: selectedAddressId,
    voucherId: selectedVoucherId.value || null,
    items: [{ productId: product.value.id, quantity: Number(quantity.value) }],
  });
  removeFromCart(product.value.id);
  ElMessage.success("下单成功");
  const orderId = result?.data?.id;
  router.push(orderId ? { path: `/order/${orderId}`, query: { action: "pay" } } : "/order");
}

function handleContactSeller() {
  const sellerUserId = getSellerUserId();
  if (!sellerUserId) {
    ElMessage.warning("当前无法联系卖家");
    return;
  }
  if (!canChatWithSeller.value) {
    ElMessage.warning("这是你自己的商品，不能和自己发起会话");
    return;
  }
  router.push({
    path: "/messages",
    query: {
      participantId: sellerUserId,
      sourceType: "PRODUCT",
      sourceId: product.value.id,
    },
  });
}

function handleEnterShop() {
  if (!product.value?.shopId) {
    ElMessage.warning("当前商品暂未关联店铺");
    return;
  }
  router.push({ name: "publicShop", params: { shopId: product.value.shopId } });
}

function getSellerUserId() {
  return product.value?.sellerUserId || product.value?.sellerId || 2;
}

async function confirmAddressAndPickId() {
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
  if (!confirmed) {
    return null;
  }
  return preferred.id;
}
</script>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-shell {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.1fr);
  gap: 22px;
}

.gallery,
.buy-panel {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 18px;
  box-shadow: var(--shadow-soft);
}

.main-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 8px;
  overflow: hidden;
  background: #f1efe6;
}

.thumbs {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.thumb {
  aspect-ratio: 1 / 1;
  border: 2px solid transparent;
  border-radius: 8px;
  padding: 0;
  overflow: hidden;
  background: #f1efe6;
  cursor: pointer;
}

.thumb.active {
  border-color: var(--brand-primary);
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.buy-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.crumb {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.buy-panel h1 {
  margin: 0;
  font-size: clamp(26px, 4vw, 38px);
  line-height: 1.18;
  letter-spacing: 0;
}

.desc {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.price-box {
  border-radius: 8px;
  background: linear-gradient(90deg, #eaf4ff 0%, #e9fff8 100%);
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  border: 1px solid rgba(60, 146, 255, 0.18);
}

.price-box span {
  color: var(--brand-primary-dark);
  font-weight: 800;
}

.price-box strong {
  color: var(--brand-primary);
  font-size: 36px;
  line-height: 1;
}

.promo-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.promo-line span {
  border: 1px solid rgba(60, 146, 255, 0.28);
  border-radius: 4px;
  background: var(--brand-primary-weak);
  color: var(--brand-primary);
  padding: 5px 9px;
  font-size: 12px;
  font-weight: 900;
}

.info-grid {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.info-grid div {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  padding: 12px;
  background: var(--surface-soft);
}

.info-grid dt {
  color: var(--text-muted);
  font-size: 12px;
}

.info-grid dd {
  margin: 6px 0 0;
  font-weight: 800;
}

.quantity-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 800;
}

.coupon-select-row {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  align-items: center;
  gap: 8px 12px;
  font-weight: 800;
}

.coupon-select-row :deep(.el-select) {
  width: 100%;
}

.coupon-select-row small {
  grid-column: 2;
  color: var(--text-secondary);
  font-weight: 500;
}

.order-preview {
  border: 1px solid rgba(255, 185, 214, 0.5);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(255, 185, 214, 0.16), rgba(137, 199, 255, 0.18));
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: var(--text-secondary);
  font-weight: 800;
}

.order-preview div {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.order-preview small {
  color: #e65b5b;
}

.order-preview strong {
  color: var(--brand-primary);
  font-size: 26px;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.service-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 13px;
}

.service-row span {
  border-radius: 999px;
  background: var(--surface-soft);
  padding: 6px 10px;
  font-weight: 800;
}

@media (max-width: 900px) {
  .detail-shell {
    grid-template-columns: 1fr;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
