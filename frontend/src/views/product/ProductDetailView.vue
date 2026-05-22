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
          <span>新人券</span>
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
            <dd>{{ product.statusName || "在售" }}</dd>
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

        <div class="actions">
          <el-button type="warning" size="large" :disabled="maxQuantity <= 0" @click="handleBuyNow">
            立即购买
          </el-button>
          <el-button type="primary" size="large" :disabled="maxQuantity <= 0" @click="handleAddToCart">
            加入新品购物车
          </el-button>
          <el-button size="large" @click="handleContactSeller">
            和卖家聊一聊
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
import { useRoute, useRouter } from "vue-router";
import { getProductDetailApi } from "@/api/product";
import { createOrderApi } from "@/api/order";
import { listAddressesApi, recordBrowseHistoryApi } from "@/api/user";
import { addToCart, removeFromCart } from "@/utils/cart";
import { getUser } from "@/utils/storage";
import { toAssetUrl } from "@/utils/url";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const product = ref(null);
const quantity = ref(1);
const selectedImage = ref("");

const maxQuantity = computed(() => Number(product.value?.stock || 0));
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
  ElMessage.success("已加入新品购物车");
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
