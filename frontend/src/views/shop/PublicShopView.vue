<template>
  <section class="shop-page">
    <el-skeleton v-if="loadingShop" :rows="10" animated class="page-card" />

    <template v-else-if="shop">
      <section class="shop-hero" :style="heroStyle">
        <div class="hero-shade">
          <button class="back-button" type="button" @click="router.back()">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </button>
          <div class="hero-content">
            <el-avatar :src="logoUrl" :size="78" class="shop-logo">
              {{ shopInitial }}
            </el-avatar>
            <div class="hero-copy">
              <span class="eyebrow">{{ shop.category || "官方店铺" }}</span>
              <h1>{{ shop.name || "店铺" }}</h1>
              <p>{{ shop.description || "店主暂未填写简介，欢迎浏览店内商品。" }}</p>
              <div class="shop-tags">
                <span v-if="shop.region">{{ shop.region }}</span>
                <span v-if="shop.businessHours">{{ shop.businessHours }}</span>
                <span v-if="shop.shippingPolicy">发货说明已完善</span>
                <span v-if="shop.returnPolicy">支持售后政策</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <SellerRatingSummary
        v-if="shop.rating"
        :rating="shop.rating"
        type="shop"
      />

      <section v-if="shop.announcement" class="announcement-strip">
        <strong>公告</strong>
        <span>{{ shop.announcement }}</span>
      </section>

      <section
        v-if="hasDecoration"
        class="decoration-preview"
        :style="{ background: decorationSettings.bgColor }"
      >
        <div
          v-for="component in decorationComponents"
          :key="component.id"
          class="decoration-component"
          :style="{ marginBottom: `${decorationSettings.gap}px` }"
        >
          <ComponentRenderer
            :component="component"
            :theme-color="decorationSettings.themeColor"
          />
        </div>
      </section>

      <section v-else class="default-decoration">
        <div>
          <strong>店铺装修待发布</strong>
          <span>先看看这家店已经上架的商品。</span>
        </div>
      </section>

      <section class="product-section">
        <div class="section-head">
          <div>
            <span class="section-kicker">Shop Products</span>
            <h2>店内商品</h2>
          </div>
          <span>{{ total }} 件在售</span>
        </div>

        <div class="product-toolbar">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="搜索本店商品"
            @keyup.enter="fetchProducts(true)"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="query.sortBy" placeholder="排序" @change="fetchProducts(true)">
            <el-option label="最新上架" value="time_desc" />
            <el-option label="价格从低到高" value="price_asc" />
            <el-option label="价格从高到低" value="price_desc" />
            <el-option label="销量优先" value="sales_desc" />
          </el-select>
          <el-button type="primary" @click="fetchProducts(true)">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetSearch">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </div>

        <div v-if="products.length" class="product-grid">
          <ProductCard
            v-for="item in products"
            :key="item.id"
            :product="item"
            mode="product"
            route-base="/product"
          />
        </div>

        <el-empty v-else-if="!loadingProducts" description="没有找到商品" />

        <div class="pager-row" v-if="total > query.pageSize">
          <el-pagination
            v-model:current-page="query.pageNum"
            :page-size="query.pageSize"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="handlePageChange"
          />
        </div>
      </section>

      <section v-if="hasPolicies" class="policy-section">
        <div class="section-head">
          <div>
            <span class="section-kicker">Shop Policies</span>
            <h2>店铺政策</h2>
          </div>
        </div>
        <div class="policy-list">
          <div v-if="shop.shippingPolicy" class="policy-item">
            <span>发货政策</span>
            <p>{{ shop.shippingPolicy }}</p>
          </div>
          <div v-if="shop.returnPolicy" class="policy-item">
            <span>售后政策</span>
            <p>{{ shop.returnPolicy }}</p>
          </div>
        </div>
      </section>
    </template>

    <el-empty v-else description="店铺不存在或已关闭" />
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowLeft, Refresh, Search } from "@element-plus/icons-vue";
import ProductCard from "@/components/ProductCard.vue";
import SellerRatingSummary from "@/components/SellerRatingSummary.vue";
import ComponentRenderer from "@/views/seller/decoration/ComponentRenderer.vue";
import { getPublicShopApi, getPublicShopProductsApi } from "@/api/shop";
import { recordBrowseHistoryApi } from "@/api/user";
import { toAssetUrl } from "@/utils/url";

const route = useRoute();
const router = useRouter();

const loadingShop = ref(false);
const loadingProducts = ref(false);
const shop = ref(null);
const products = ref([]);
const total = ref(0);

const query = reactive({
  keyword: "",
  sortBy: "time_desc",
  pageNum: 1,
  pageSize: 20,
});

const defaultHero = "https://images.unsplash.com/photo-1472851294608-062f824d29cc?auto=format&fit=crop&w=1400&q=80";
const heroUrl = computed(() => toAssetUrl(shop.value?.bannerUrl) || defaultHero);
const logoUrl = computed(() => toAssetUrl(shop.value?.logo));
const shopInitial = computed(() => (shop.value?.name || "店").slice(0, 1));

const heroStyle = computed(() => ({
  backgroundImage: `url("${heroUrl.value}")`,
}));

const decoration = computed(() => {
  if (!shop.value?.decorationJson) {
    return null;
  }
  try {
    return JSON.parse(shop.value.decorationJson);
  } catch {
    return null;
  }
});

const decorationSettings = computed(() => ({
  themeColor: decoration.value?.globalSettings?.themeColor || "#1d9e75",
  bgColor: decoration.value?.globalSettings?.bgColor || "#f5f7fa",
  gap: Number(decoration.value?.globalSettings?.gap ?? 12),
}));

const decorationComponents = computed(() => {
  return Array.isArray(decoration.value?.components) ? decoration.value.components : [];
});

const hasDecoration = computed(() => decorationComponents.value.length > 0);
const hasPolicies = computed(() => Boolean(shop.value?.shippingPolicy || shop.value?.returnPolicy));

onMounted(async () => {
  await fetchShop();
  await fetchProducts(true);
});

watch(
  () => route.params.shopId,
  async () => {
    await fetchShop();
    await fetchProducts(true);
  },
);

async function fetchShop() {
  loadingShop.value = true;
  try {
    const result = await getPublicShopApi(route.params.shopId);
    shop.value = result.data;
    await recordShopBrowseHistory();
  } catch {
    shop.value = null;
  } finally {
    loadingShop.value = false;
  }
}

async function recordShopBrowseHistory() {
  if (!shop.value?.id) {
    return;
  }
  try {
    await recordBrowseHistoryApi({
      productId: shop.value.id,
      productType: "SHOP",
    });
  } catch {
    // Browse history should never block shop viewing.
  }
}

async function fetchProducts(resetPage = true) {
  if (!route.params.shopId) {
    return;
  }
  if (resetPage) {
    query.pageNum = 1;
  }
  loadingProducts.value = true;
  try {
    const result = await getPublicShopProductsApi(route.params.shopId, {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      sortBy: query.sortBy,
      ...(query.keyword.trim() ? { keyword: query.keyword.trim() } : {}),
    });
    const records = result.data?.records || [];
    products.value = records;
    total.value = Number(result.data?.total || records.length || 0);
  } catch {
    products.value = [];
    total.value = 0;
  } finally {
    loadingProducts.value = false;
  }
}

function resetSearch() {
  query.keyword = "";
  query.sortBy = "time_desc";
  fetchProducts(true);
}

function handlePageChange(page) {
  query.pageNum = page;
  fetchProducts(false);
}
</script>

<style scoped>
.shop-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.shop-hero {
  min-height: 260px;
  border-radius: 8px;
  background-position: center;
  background-size: cover;
  overflow: hidden;
  border: 1px solid var(--line-soft);
}

.hero-shade {
  min-height: 260px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: linear-gradient(90deg, rgba(12, 18, 30, 0.72), rgba(12, 18, 30, 0.28));
  color: #ffffff;
}

.back-button {
  width: fit-content;
  height: 34px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  cursor: pointer;
}

.hero-content {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

.shop-logo {
  flex: 0 0 auto;
  border: 3px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.24);
}

.hero-copy {
  max-width: 760px;
  min-width: 0;
}

.eyebrow,
.section-kicker {
  display: inline-flex;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
}

.eyebrow {
  color: #d5ecff;
}

.hero-copy h1 {
  margin: 6px 0 8px;
  font-size: clamp(34px, 6vw, 58px);
  line-height: 1;
  letter-spacing: 0;
}

.hero-copy p {
  margin: 0;
  max-width: 680px;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.7;
  font-weight: 700;
}

.shop-tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.shop-tags span {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 900;
}

.announcement-strip,
.default-decoration,
.policy-section,
.product-section,
.decoration-preview {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
}

.announcement-strip {
  min-height: 48px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.announcement-strip strong {
  color: var(--brand-primary);
}

.default-decoration {
  padding: 20px;
}

.default-decoration div {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.default-decoration strong {
  font-size: 18px;
}

.default-decoration span {
  color: var(--text-secondary);
}

.decoration-preview {
  padding: 12px;
  overflow: hidden;
}

.decoration-component:last-child {
  margin-bottom: 0 !important;
}

.product-section {
  padding: 14px;
}

.policy-section {
  padding: 14px;
}

.policy-list {
  display: grid;
  gap: 12px;
}

.policy-item {
  border-top: 1px solid var(--line-soft);
  padding-top: 12px;
}

.policy-item:first-child {
  border-top: 0;
  padding-top: 0;
}

.policy-item span {
  display: block;
  color: var(--brand-primary);
  font-size: 13px;
  font-weight: 900;
  margin-bottom: 6px;
}

.policy-item p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.section-kicker {
  color: var(--brand-primary);
}

.section-head h2 {
  margin: 4px 0 0;
  font-size: 24px;
}

.section-head > span {
  color: var(--text-muted);
  font-weight: 800;
}

.product-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px auto auto;
  gap: 10px;
  margin-bottom: 14px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.pager-row {
  display: flex;
  justify-content: center;
  padding-top: 18px;
}

@media (max-width: 980px) {
  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .product-toolbar {
    grid-template-columns: 1fr 160px;
  }
}

@media (max-width: 680px) {
  .hero-shade {
    padding: 14px;
  }

  .hero-content {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-copy h1 {
    font-size: 34px;
  }

  .product-toolbar {
    grid-template-columns: 1fr;
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}
</style>
