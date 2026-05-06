<template>
  <section class="detail-page">
    <el-skeleton v-if="loading" :rows="10" animated />

    <template v-else-if="product">
      <section class="seller-bar">
        <div class="seller-profile">
          <span class="seller-avatar">{{ sellerInitial }}</span>
          <div>
            <div class="seller-name">
              <strong>{{ product.sellerName || "kinda goods 卖家" }}</strong>
              <em>官方认证</em>
              <em class="soft">品质保障</em>
            </div>
            <p>kinda goods 店铺 · 最近上新 · 好评率 {{ praiseRate }}%</p>
          </div>
        </div>
        <el-button round @click="goBack">返回列表</el-button>
      </section>

      <section class="detail-card">
        <aside class="thumb-rail">
          <button
            v-for="(image, index) in galleryImages"
            :key="`${image}-${index}`"
            class="thumb"
            :class="{ active: activeImage === image }"
            type="button"
            @click="activeImage = image"
          >
            <img v-if="image" :src="image" :alt="`${product.name}-${index + 1}`" />
            <span v-else>暂无图片</span>
          </button>
        </aside>

        <div class="image-stage">
          <div class="image-frame">
            <img v-if="activeImage" :src="activeImage" :alt="product.name" />
            <div v-else class="cover-placeholder">暂无图片</div>
          </div>
          <div class="image-footer">
            <button class="trust-link" type="button" @click="openGuaranteeInfo">担保交易</button>
            <button type="button" @click="openReportDialog">举报</button>
          </div>
        </div>

        <article class="info-panel">
          <div class="price-row">
            <div>
              <span class="currency">￥</span>
              <strong>{{ Number(product.price || 0).toFixed(2) }}</strong>
              <em>包邮</em>
            </div>
            <p>{{ wantCount }}人想要｜{{ viewCount }}浏览</p>
          </div>

          <div class="service-strip">描述不符包邮退 · 7天无理由退货</div>

          <h1>{{ product.name }}</h1>

          <div class="detail-text">
            <p>{{ product.description || "暂无商品描述，详情可联系卖家咨询。" }}</p>
            <p>【库存】当前库存 {{ product.stock ?? 0 }} 件。</p>
            <p>【状态】{{ product.statusName || "在售" }}。</p>
            <p>【购买】选择数量后可以加入购物车，也可以直接购买并选择地址与优惠券。</p>
            <p>【售后】请在下单前确认商品规格与收货信息，售后流程可在订单中心处理。</p>
          </div>

          <div class="quantity-row">
            <span>购买数量</span>
            <el-input-number v-model="quantity" :min="1" :max="maxQuantity" @change="recalcPreview" />
          </div>

          <div class="action-row">
            <el-button class="chat-btn" round :disabled="!canChatWithSeller" @click="handleContactSeller">
              聊一聊
            </el-button>
            <el-button class="buy-btn" round :disabled="maxQuantity <= 0" @click="openBuyNow">
              立即购买
            </el-button>
            <el-button class="cart-btn" round :disabled="maxQuantity <= 0" @click="handleAddToCart">
              加入购物车
            </el-button>
          </div>
        </article>
      </section>

      <section class="recommend-panel">
        <div class="section-head">
          <h2>为你推荐</h2>
          <el-button text @click="$router.push('/product')">查看更多</el-button>
        </div>
        <div v-if="recommendedItems.length" class="recommend-grid">
          <ProductCard
            v-for="item in recommendedItems"
            :key="item.id"
            :product="item"
            mode="product"
            route-base="/product"
          />
        </div>
        <el-empty v-else description="暂无推荐商品" />
      </section>
    </template>

    <p v-else class="empty-tip">商品不存在</p>

    <el-dialog v-model="reportDialogVisible" title="举报卖家" width="480px" append-to-body>
      <el-form :model="reportForm" label-width="90px" @submit.prevent>
        <el-form-item label="举报类型" required>
          <el-select v-model="reportForm.reasonType" placeholder="请选择举报类型" style="width: 100%">
            <el-option label="诈骗/虚假交易" value="FRAUD" />
            <el-option label="商品与描述不符" value="FAKE_ITEM" />
            <el-option label="态度恶劣/骚扰" value="BAD_ATTITUDE" />
            <el-option label="退款纠纷" value="REFUND_ABUSE" />
            <el-option label="刷单/广告骚扰" value="SPAM" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input
            v-model="reportForm.reasonDesc"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请描述你遇到的问题，便于管理员审核"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportSubmitting" @click="handleSubmitReport">提交举报</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="buyDialogVisible" title="立即购买" width="860px" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="请选择收货地址并可直接使用我的优惠券，选中后应付金额会实时更新。"
        style="margin-bottom: 12px"
      />
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
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import ProductCard from "@/components/ProductCard.vue";
import { getProductDetailApi, getProductListApi } from "@/api/product";
import { createOrderApi } from "@/api/order";
import { submitReportApi } from "@/api/credit";
import { listAddressesApi } from "@/api/user";
import { myAvailableVoucherApi } from "@/api/voucher";
import { addToCart, removeFromCart } from "@/utils/cart";
import { getUser } from "@/utils/storage";
import { toApiAssetUrl } from "@/utils/url";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const product = ref(null);
const quantity = ref(1);
const activeImage = ref("");
const recommendations = ref([]);
const buyDialogVisible = ref(false);
const buySubmitting = ref(false);
const selectedAddress = ref(null);
const availableVouchers = ref([]);
const selectedVoucherId = ref(null);
const previewItems = ref([]);
const reportDialogVisible = ref(false);
const reportSubmitting = ref(false);
const reportForm = ref({ reasonType: "", reasonDesc: "" });

const maxQuantity = computed(() => Number(product.value?.stock || 0));
const sellerInitial = computed(() => (product.value?.sellerName || "店").slice(0, 1).toUpperCase());
const praiseRate = computed(() => 90 + (Number(product.value?.id || 0) % 8));
const wantCount = computed(() => 20 + (Number(product.value?.id || 0) % 31));
const viewCount = computed(() => 300 + (Number(product.value?.id || 0) % 220));
const galleryImages = computed(() => {
  const cover = toFullImageUrl(product.value?.cover || "");
  return [cover, cover, cover].filter(Boolean);
});
const recommendedItems = computed(() => {
  return recommendations.value.filter((item) => Number(item.id) !== Number(product.value?.id)).slice(0, 4);
});
const canChatWithSeller = computed(() => {
  if (!product.value?.sellerUserId) return false;
  return Number(product.value.sellerUserId) !== Number(getUser()?.id);
});
const sellerUserId = computed(() => product.value?.sellerUserId || product.value?.sellerId || product.value?.merchantUserId || null);
const canReportSeller = computed(() => {
  if (!sellerUserId.value) return false;
  return Number(sellerUserId.value) !== Number(getUser()?.id);
});
const previewTotal = computed(() => previewItems.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0));
const selectedVoucher = computed(() => availableVouchers.value.find((v) => v.id === selectedVoucherId.value) || null);
const previewDiscount = computed(() => calcDiscount(selectedVoucher.value, previewTotal.value));
const previewPayable = computed(() => Math.max(0.01, previewTotal.value - previewDiscount.value));

onMounted(async () => {
  await fetchDetail();
  await fetchRecommendations();
});

watch(selectedVoucherId, () => {});

async function fetchDetail() {
  loading.value = true;
  try {
    const result = await getProductDetailApi(route.params.id);
    product.value = result.data;
    activeImage.value = toFullImageUrl(product.value?.cover || "");
    if (maxQuantity.value > 0) quantity.value = 1;
  } finally {
    loading.value = false;
  }
}

async function fetchRecommendations() {
  const result = await getProductListApi({ pageNum: 1, pageSize: 8 });
  recommendations.value = result.data?.records || [];
}

function recalcPreview() {}

function openGuaranteeInfo() {
  ElMessageBox.alert(
    "平台会在下单后先托管交易资金，买家确认收货后再结算给卖家。若商品存在描述不符、未发货等问题，可以在订单售后中发起处理。",
    "担保交易说明",
    {
      confirmButtonText: "知道了",
      type: "info",
    }
  );
}

function openReportDialog() {
  if (!getUser()?.id) {
    ElMessage.warning("请先登录后再举报");
    router.push("/login");
    return;
  }
  if (!sellerUserId.value) {
    ElMessage.warning("当前商品缺少卖家信息，暂时无法举报");
    return;
  }
  if (!canReportSeller.value) {
    ElMessage.warning("不能举报自己发布的商品");
    return;
  }
  reportForm.value = { reasonType: "", reasonDesc: "" };
  reportDialogVisible.value = true;
}

async function handleSubmitReport() {
  if (!reportForm.value.reasonType) {
    ElMessage.warning("请选择举报类型");
    return;
  }
  reportSubmitting.value = true;
  try {
    await submitReportApi({
      reportedId: sellerUserId.value,
      tradeContext: "SHOP",
      reasonType: reportForm.value.reasonType,
      reasonDesc: reportForm.value.reasonDesc,
    });
    ElMessage.success("举报已提交，等待管理员审核");
    reportDialogVisible.value = false;
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || "举报提交失败");
  } finally {
    reportSubmitting.value = false;
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
    shopIds: shopIds.join(","),
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
  return `${voucher.name} - ${rule}`;
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
  return toApiAssetUrl(url);
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
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.seller-bar,
.detail-card,
.recommend-panel {
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(30, 34, 40, 0.04);
}

.seller-bar {
  min-height: 72px;
  padding: 14px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.seller-profile {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.seller-avatar {
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #20242d;
  color: #ffe100;
  font-size: 20px;
  font-weight: 900;
}

.seller-name {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.seller-name strong {
  color: #20242d;
  font-size: 17px;
}

.seller-name em {
  border-radius: 999px;
  padding: 2px 7px;
  background: #00c8df;
  color: #fff;
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

.seller-name em.soft {
  background: #ffe100;
  color: #20242d;
}

.seller-profile p {
  margin: 5px 0 0;
  color: #6f7682;
  font-size: 13px;
}

.detail-card {
  min-height: 620px;
  padding: 16px;
  display: grid;
  grid-template-columns: 112px minmax(360px, 1fr) minmax(360px, 540px);
  gap: 18px;
}

.thumb-rail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.thumb {
  width: 92px;
  height: 92px;
  border: 2px solid transparent;
  border-radius: 16px;
  padding: 0;
  overflow: hidden;
  background: #f1f1f1;
  color: #8a8f99;
  cursor: pointer;
}

.thumb.active {
  border-color: #20242d;
}

.thumb img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.image-stage {
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  border-radius: 18px;
  overflow: hidden;
}

.image-frame {
  min-height: 560px;
  flex: 1;
  display: grid;
  place-items: center;
  padding: 22px;
}

.image-frame img {
  max-width: 100%;
  max-height: 560px;
  object-fit: contain;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 420px;
  display: grid;
  place-items: center;
  color: #7c838f;
}

.image-footer {
  height: 36px;
  padding: 0 14px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  color: #6f7682;
  font-size: 13px;
}

.image-footer .trust-link {
  color: #1677ff;
}

.image-footer button {
  border: 0;
  background: transparent;
  color: #6f7682;
  cursor: pointer;
}

.image-footer button:disabled {
  color: #b5bac2;
  cursor: not-allowed;
}

.info-panel {
  min-width: 0;
  padding: 4px 0 0 24px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.price-row > div {
  color: #ff4d00;
}

.currency {
  font-size: 20px;
  font-weight: 900;
}

.price-row strong {
  font-size: 34px;
  line-height: 1;
}

.price-row em {
  margin-left: 8px;
  color: #20242d;
  font-size: 13px;
  font-style: normal;
}

.price-row p {
  margin: 8px 0 0;
  color: #9a9fa8;
  font-size: 13px;
  white-space: nowrap;
}

.service-strip {
  margin: 14px 0 20px;
  border-radius: 14px;
  padding: 13px 16px;
  background: #f4f4f4;
  color: #333842;
  font-size: 14px;
}

.info-panel h1 {
  margin: 0 0 24px;
  color: #20242d;
  font-size: 22px;
  line-height: 1.45;
  letter-spacing: 0;
  font-weight: 600;
}

.detail-text {
  max-height: 290px;
  overflow: auto;
  color: #20242d;
  font-size: 16px;
  line-height: 1.65;
}

.detail-text p {
  margin: 0 0 8px;
}

.quantity-row {
  margin-top: 22px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: #333842;
}

.action-row {
  margin-top: 28px;
  display: grid;
  grid-template-columns: 1.1fr 1.2fr 1fr;
  gap: 0;
  overflow: hidden;
  border-radius: 999px;
}

.action-row :deep(.el-button) {
  height: 50px;
  margin: 0;
  border-radius: 0;
  border: 0;
  font-size: 16px;
  font-weight: 900;
}

.chat-btn {
  --el-button-bg-color: #ffe100;
  --el-button-text-color: #20242d;
  --el-button-hover-bg-color: #ffe83f;
  --el-button-hover-text-color: #20242d;
}

.buy-btn {
  --el-button-bg-color: #333333;
  --el-button-text-color: #ffffff;
  --el-button-hover-bg-color: #222222;
  --el-button-hover-text-color: #ffffff;
}

.cart-btn {
  --el-button-bg-color: #f4f4f4;
  --el-button-text-color: #20242d;
  --el-button-hover-bg-color: #eeeeee;
  --el-button-hover-text-color: #20242d;
}

.recommend-panel {
  padding: 20px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-head h2 {
  margin: 0;
  font-size: 22px;
  letter-spacing: 0;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.address-box {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
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

.payable {
  font-size: 18px;
  color: #ef4444;
  font-weight: 700;
}

@media (max-width: 1180px) {
  .detail-card {
    grid-template-columns: 92px minmax(0, 1fr);
  }

  .info-panel {
    grid-column: 1 / -1;
    padding: 8px 0 0;
  }

  .recommend-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .seller-bar {
    align-items: flex-start;
    gap: 12px;
  }

  .detail-card {
    padding: 12px;
    grid-template-columns: 1fr;
  }

  .thumb-rail {
    order: 2;
    flex-direction: row;
    overflow-x: auto;
  }

  .image-stage {
    order: 1;
  }

  .info-panel {
    order: 3;
  }

  .image-frame {
    min-height: 320px;
  }

  .image-frame img {
    max-height: 320px;
  }

  .price-row {
    display: block;
  }

  .detail-text {
    max-height: none;
    font-size: 15px;
  }

  .action-row {
    grid-template-columns: 1fr;
    border-radius: 16px;
    gap: 8px;
    overflow: visible;
  }

  .action-row :deep(.el-button) {
    border-radius: 999px;
  }

  .recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}
</style>
