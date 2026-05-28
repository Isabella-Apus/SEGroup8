<template>
  <section class="seller-page">
    <el-skeleton v-if="loadingSeller" :rows="8" animated class="page-card" />

    <template v-else-if="seller">
      <section class="seller-hero">
        <button class="back-button" type="button" @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </button>
        <div class="seller-profile">
          <el-avatar :src="avatarUrl" :size="82" class="seller-avatar">
            {{ sellerInitial }}
          </el-avatar>
          <div>
            <span class="eyebrow">Secondhand Seller</span>
            <h1>{{ seller.nickname || "个人卖家" }}</h1>
            <p>{{ seller.region ? `${seller.region} 的闲置卖家` : "个人闲置卖家，看看他正在出售的商品。" }}</p>
            <div class="seller-tags">
              <span>二手卖家</span>
              <span v-if="seller.rating?.shSellerLevel">{{ seller.rating.shSellerLevel }}</span>
              <span>{{ Number(seller.rating?.shSellerGoodRate ?? 100).toFixed(1) }}% 好评率</span>
            </div>
          </div>
        </div>
      </section>

      <SellerRatingSummary
        v-if="seller.rating"
        :rating="seller.rating"
        type="secondhand"
      />

      <section class="product-section">
        <div class="section-head">
          <div>
            <span class="section-kicker">Seller Items</span>
            <h2>他的二手商品</h2>
          </div>
          <span>{{ total }} 件在售</span>
        </div>

        <div class="product-toolbar">
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="搜索这个卖家的商品"
            @keyup.enter="fetchProducts(true)"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="query.sortBy" placeholder="排序" @change="fetchProducts(true)">
            <el-option label="最新发布" value="time_desc" />
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
            v-for="product in products"
            :key="product.id"
            :product="product"
            mode="secondhand"
            route-base="/secondhand"
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
    </template>

    <el-empty v-else description="卖家不存在" />
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowLeft, Refresh, Search } from "@element-plus/icons-vue";
import ProductCard from "@/components/ProductCard.vue";
import SellerRatingSummary from "@/components/SellerRatingSummary.vue";
import {
  getPublicSecondhandSellerApi,
  getPublicSecondhandSellerProductsApi,
} from "@/api/secondhand";
import { toAssetUrl } from "@/utils/url";

const route = useRoute();
const router = useRouter();

const loadingSeller = ref(false);
const loadingProducts = ref(false);
const seller = ref(null);
const products = ref([]);
const total = ref(0);

const query = reactive({
  keyword: "",
  sortBy: "time_desc",
  pageNum: 1,
  pageSize: 20,
});

const avatarUrl = computed(() => toAssetUrl(seller.value?.avatar));
const sellerInitial = computed(() => (seller.value?.nickname || "卖").slice(0, 1));

onMounted(async () => {
  await fetchSeller();
  await fetchProducts(true);
});

watch(
  () => route.params.sellerId,
  async () => {
    await fetchSeller();
    await fetchProducts(true);
  },
);

async function fetchSeller() {
  loadingSeller.value = true;
  try {
    const result = await getPublicSecondhandSellerApi(route.params.sellerId);
    seller.value = result.data;
  } catch {
    seller.value = null;
  } finally {
    loadingSeller.value = false;
  }
}

async function fetchProducts(resetPage = true) {
  if (!route.params.sellerId) {
    return;
  }
  if (resetPage) {
    query.pageNum = 1;
  }
  loadingProducts.value = true;
  try {
    const result = await getPublicSecondhandSellerProductsApi(route.params.sellerId, {
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
.seller-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.seller-hero {
  min-height: 250px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(233, 255, 248, 0.94), rgba(234, 244, 255, 0.88), rgba(255, 247, 251, 0.72)),
    url("https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=1400&q=80");
  background-position: center;
  background-size: cover;
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  box-shadow: var(--shadow-soft);
}

.back-button {
  width: fit-content;
  height: 34px;
  border: 1px solid rgba(60, 146, 255, 0.24);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--text-main);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  cursor: pointer;
  font-weight: 900;
}

.seller-profile {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

.seller-avatar {
  flex: 0 0 auto;
  border: 3px solid rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.16);
}

.eyebrow,
.section-kicker {
  display: inline-flex;
  color: var(--brand-accent-strong);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
  text-transform: uppercase;
}

.seller-profile h1 {
  margin: 6px 0 8px;
  font-size: clamp(34px, 6vw, 56px);
  line-height: 1;
  letter-spacing: 0;
}

.seller-profile p {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 800;
  line-height: 1.7;
}

.seller-tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.seller-tags span {
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--brand-accent-strong);
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 900;
}

.product-section {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 14px;
  box-shadow: var(--shadow-soft);
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
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
  .seller-profile {
    align-items: flex-start;
    flex-direction: column;
  }

  .seller-profile h1 {
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
