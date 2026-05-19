<template>
  <section class="home-page">
    <div class="home-hero">
      <el-carousel height="385px" indicator-position="outside" class="hero-carousel">
        <el-carousel-item v-for="slide in bannerSlides" :key="slide.key">
          <article class="banner-slide" :style="{ backgroundImage: slide.background }">
            <div class="banner-copy">
              <span class="eyebrow">{{ slide.tag }}</span>
              <h1>{{ slide.title }}</h1>
              <p>{{ slide.desc }}</p>
              <div class="hero-actions">
                <el-button type="primary" size="large" @click="router.push(slide.actionPath)">
                  {{ slide.actionText }}
                </el-button>
                <el-button size="large" @click="router.push(slide.secondaryPath)">
                  {{ slide.secondaryText }}
                </el-button>
              </div>
            </div>
          </article>
        </el-carousel-item>
      </el-carousel>

      <aside class="hero-panel">
        <div class="panel-head">
          <strong>Kinda Goods</strong>
          <span>{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
        </div>
        <div class="metric-grid">
          <button type="button" class="metric" @click="router.push('/order')">
            <strong>{{ stats.orderCount }}</strong>
            <span>我的订单</span>
          </button>
          <button type="button" class="metric" @click="router.push('/notifications')">
            <strong>{{ stats.noticeCount }}</strong>
            <span>未读通知</span>
          </button>
          <button type="button" class="metric" @click="sellerEntry">
            <strong>{{ userStore.currentRole === 'OFFICIAL_SELLER' ? '店' : '入' }}</strong>
            <span>{{ sellerEntryTitle }}</span>
          </button>
        </div>
      </aside>
    </div>

    <section class="category-strip">
      <button
        v-for="item in categoryCards"
        :key="item.label"
        type="button"
        class="category-card"
        @click="router.push(item.path)"
      >
        <span>{{ item.label }}</span>
        <small>{{ item.desc }}</small>
      </button>
    </section>

    <section class="section-block">
      <div class="section-head">
        <div>
          <h2>Top Sales</h2>
          <p>近期热度更高的一手商品。</p>
        </div>
        <el-button text type="primary" @click="router.push('/product')">查看全部</el-button>
      </div>
      <div class="product-grid">
        <ProductCard
          v-for="item in products"
          :key="item.id"
          :product="item"
          mode="product"
          route-base="/product"
        />
      </div>
    </section>

    <section class="section-block">
      <div class="section-head">
        <div>
          <h2>二手闲置</h2>
          <p>个人发布的闲置好物，适合捡漏和快速转手。</p>
        </div>
        <el-button text type="primary" @click="router.push('/secondhand')">查看全部</el-button>
      </div>
      <div class="product-grid">
        <ProductCard
          v-for="item in secondhandItems"
          :key="item.id"
          :product="item"
          mode="secondhand"
          route-base="/secondhand"
        />
      </div>
    </section>

    <section class="workflow-strip">
      <button type="button" class="workflow-card" @click="router.push('/addresses')">
        <strong>收货地址</strong>
        <span>管理常用地址，下单更顺畅。</span>
      </button>
      <button type="button" class="workflow-card" @click="router.push('/after-sale')">
        <strong>售后中心</strong>
        <span>退款、退货、订单问题集中处理。</span>
      </button>
      <button type="button" class="workflow-card" @click="router.push('/credit')">
        <strong>我的信用</strong>
        <span>查看买家和二手卖家信用状态。</span>
      </button>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import ProductCard from "@/components/ProductCard.vue";
import { getProductListApi } from "@/api/product";
import { getSecondhandListApi } from "@/api/secondhand";
import { getOrderListApi } from "@/api/order";
import { listNotificationsApi } from "@/api/notification";
import { useUserStore } from "@/stores/user";

const router = useRouter();
const userStore = useUserStore();
const products = ref([]);
const secondhandItems = ref([]);
const stats = reactive({
  orderCount: 0,
  noticeCount: 0,
});

const categoryCards = [
  { label: "电子数码", desc: "键盘、耳机、平板", path: "/product?category=电子数码" },
  { label: "服装鞋包", desc: "日常穿搭好物", path: "/product?category=服装鞋包" },
  { label: "学习办公", desc: "效率工具和资料", path: "/product?category=学习办公" },
  { label: "二手数码", desc: "更轻预算淘设备", path: "/secondhand?category=数码闲置" },
  { label: "宿舍生活", desc: "小家具和生活用品", path: "/secondhand?category=宿舍生活" },
];

const bannerSlides = computed(() => {
  const product = products.value[0];
  const product2 = products.value[1];
  const second = secondhandItems.value[0];
  return [
    {
      key: "top-sales",
      tag: "Top Sales",
      title: product ? product.name : "本周热卖好物",
      desc: product ? `到手价 ¥${Number(product.price || 0).toFixed(2)}，库存 ${product.stock ?? 0}` : "精选高热度商品，适合快速下单。",
      actionText: "查看商品",
      actionPath: product ? `/product/${product.id}` : "/product",
      secondaryText: "逛商品市场",
      secondaryPath: "/product",
      background: "linear-gradient(120deg, rgba(220,239,233,.94), rgba(241,240,251,.72)), url('https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=1400&q=80')",
    },
    {
      key: "secondhand",
      tag: "二手精选",
      title: second ? second.name : "把闲置重新流动起来",
      desc: second ? `闲置价 ¥${Number(second.salePrice || 0).toFixed(2)}，成色 ${second.conditionLevel || second.condition || "良好"}` : "校园闲置、个人转手、快速沟通。",
      actionText: "查看闲置",
      actionPath: second ? `/secondhand/${second.id}` : "/secondhand",
      secondaryText: "发布闲置",
      secondaryPath: "/secondhand/publish",
      background: "linear-gradient(120deg, rgba(247,239,229,.95), rgba(183,216,238,.7)), url('https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=1400&q=80')",
    },
    {
      key: "daily",
      tag: "Daily Picks",
      title: product2 ? product2.name : "按预算挑选刚刚好",
      desc: "按分类、价格、成色快速筛选，少翻页也能找到合适商品。",
      actionText: "价格筛选",
      actionPath: "/product",
      secondaryText: "二手捡漏",
      secondaryPath: "/secondhand",
      background: "linear-gradient(120deg, rgba(241,240,251,.95), rgba(220,239,233,.78)), url('https://images.unsplash.com/photo-1472851294608-062f824d29cc?auto=format&fit=crop&w=1400&q=80')",
    },
  ];
});

const sellerEntryTitle = computed(() =>
  userStore.currentRole === "OFFICIAL_SELLER" ? "卖家工作台" : "申请开店",
);

onMounted(async () => {
  await userStore.fetchProfile();
  await Promise.all([loadProducts(), loadSecondhand(), loadStats()]);
});

async function loadProducts() {
  const result = await getProductListApi({ pageNum: 1, pageSize: 8 });
  products.value = result.data?.records || [];
}

async function loadSecondhand() {
  const result = await getSecondhandListApi({ pageNum: 1, pageSize: 8 });
  secondhandItems.value = (result.data?.records || []).map((item) => ({
    ...item,
    originPrice: item.originPrice ?? item.salePrice,
    salePrice: item.salePrice ?? item.price,
  }));
}

async function loadStats() {
  const [orders, notices] = await Promise.allSettled([
    getOrderListApi({ pageNum: 1, pageSize: 1 }),
    listNotificationsApi("buyer"),
  ]);
  if (orders.status === "fulfilled") {
    stats.orderCount = Number(orders.value.data?.total || orders.value.data?.records?.length || 0);
  }
  if (notices.status === "fulfilled") {
    stats.noticeCount = (notices.value.data || []).filter((item) => Number(item.isRead) === 0).length;
  }
}

function sellerEntry() {
  router.push(userStore.currentRole === "OFFICIAL_SELLER" ? "/merchant" : "/merchant-apply");
}
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.home-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 248px;
  gap: 18px;
  align-items: stretch;
}

.hero-carousel {
  border-radius: 28px;
  overflow: hidden;
  box-shadow: var(--shadow-soft);
}

.banner-slide {
  height: 100%;
  border-radius: 28px;
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  padding: 42px;
}

.banner-copy {
  max-width: 620px;
}

.eyebrow {
  display: inline-flex;
  width: fit-content;
  border-radius: 999px;
  padding: 6px 13px;
  font-weight: 900;
  background: #ffffff;
  color: var(--brand-primary);
}

.banner-copy h1 {
  margin: 18px 0 12px;
  max-width: 620px;
  font-size: clamp(34px, 5vw, 56px);
  line-height: 1.04;
  letter-spacing: 0;
}

.banner-copy p {
  max-width: 560px;
  margin: 0;
  color: #3f4745;
  font-size: 16px;
  line-height: 1.8;
  font-weight: 700;
}

.hero-actions {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-panel {
  border-radius: 22px;
  background: linear-gradient(145deg, #27323a 0%, #33434b 100%);
  color: #ffffff;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  box-shadow: var(--shadow-float);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #dcefe9;
}

.panel-head strong {
  font-size: 18px;
}

.metric-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.metric {
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
  min-height: 68px;
  padding: 12px;
  text-align: left;
  cursor: pointer;
}

.metric:hover {
  background: rgba(255, 255, 255, 0.14);
}

.metric strong {
  display: block;
  font-size: 24px;
  line-height: 1;
}

.metric span {
  display: block;
  margin-top: 6px;
  color: #dcefe9;
  font-size: 13px;
  font-weight: 700;
}

.category-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.category-card {
  min-height: 92px;
  border: 1px solid var(--line-soft);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  padding: 16px;
  text-align: left;
  cursor: pointer;
  box-shadow: var(--shadow-soft);
}

.category-card:hover {
  transform: translateY(-2px);
  background: #ffffff;
}

.category-card span {
  display: block;
  font-weight: 900;
  font-size: 17px;
}

.category-card small {
  display: block;
  margin-top: 8px;
  color: var(--text-secondary);
}

.section-block {
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--line-soft);
  padding: 20px;
  box-shadow: var(--shadow-soft);
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.section-head h2 {
  margin: 0;
  font-size: 24px;
}

.section-head p {
  margin: 6px 0 0;
  color: var(--text-secondary);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.workflow-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.workflow-card {
  min-height: 112px;
  border: 1px solid var(--line-soft);
  border-radius: 20px;
  background: #ffffff;
  padding: 18px;
  text-align: left;
  cursor: pointer;
  box-shadow: var(--shadow-soft);
}

.workflow-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-float);
}

.workflow-card strong {
  display: block;
  font-size: 18px;
}

.workflow-card span {
  display: block;
  margin-top: 8px;
  color: var(--text-secondary);
  line-height: 1.55;
}

@media (max-width: 980px) {
  .home-hero,
  .workflow-strip {
    grid-template-columns: 1fr;
  }

  .category-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .hero-panel {
    padding: 16px;
  }

  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .hero-carousel,
  .banner-slide,
  .hero-panel,
  .section-block {
    border-radius: 20px;
  }

  .banner-slide,
  .hero-panel,
  .section-block {
    padding: 18px;
  }

  .category-strip,
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .hero-carousel :deep(.el-carousel__container) {
    height: 330px !important;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
