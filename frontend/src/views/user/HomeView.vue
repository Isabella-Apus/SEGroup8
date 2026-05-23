<template>
  <section class="home-page">
    <div class="commerce-shell">
      <aside class="category-rail">
        <div class="rail-title">热门分类</div>
        <button
          v-for="item in categoryCards"
          :key="item.label"
          type="button"
          class="rail-item"
          @click="router.push(item.path)"
        >
          <span class="rail-icon" :style="{ background: item.color }">{{ item.short }}</span>
          <span class="rail-copy">
            <strong>{{ item.label }}</strong>
            <small>{{ item.desc }}</small>
          </span>
        </button>
      </aside>

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
            <div class="voucher-row">
              <span v-for="coupon in slide.coupons" :key="coupon">{{ coupon }}</span>
            </div>
          </article>
        </el-carousel-item>
      </el-carousel>

      <aside class="member-panel">
        <div class="panel-head">
          <span class="member-avatar">{{ avatarText }}</span>
          <div>
            <strong>{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</strong>
            <small>今天也有新好价</small>
          </div>
        </div>
        <div class="metric-grid">
          <button type="button" class="metric" @click="router.push('/order')">
            <strong>{{ stats.orderCount }}</strong>
            <span>新品订单</span>
          </button>
          <button type="button" class="metric" @click="router.push('/notifications')">
            <strong>{{ stats.noticeCount }}</strong>
            <span>通知</span>
          </button>
          <button type="button" class="metric" @click="router.push('/browse-history')">
            <strong>历史</strong>
            <span>浏览记录</span>
          </button>
        </div>
      </aside>
    </div>

    <section class="promo-grid">
      <button type="button" class="promo-card promo-main" @click="router.push('/product')">
        <span>限时精选</span>
        <strong>热卖商品直达</strong>
        <small>数码、学习、生活用品集中看</small>
      </button>
      <button type="button" class="promo-card promo-blue" @click="router.push('/secondhand')">
        <span>二手好价</span>
        <strong>预算更轻</strong>
        <small>个人闲置快速沟通</small>
      </button>
      <button type="button" class="promo-card promo-green" @click="router.push('/secondhand')">
        <span>二手商城</span>
        <strong>闲置专区</strong>
        <small>购物车、订单和服务都在二手商城里</small>
      </button>
    </section>

    <section class="section-block discover-block">
      <div class="section-head">
        <div>
          <h2>今日热卖</h2>
          <p>官方商品、快速下单、售后更清楚。</p>
        </div>
        <div class="section-tabs">
          <button v-for="tab in productTabs" :key="tab.label" type="button" @click="goTab(tab)">{{ tab.label }}</button>
          <el-button text type="primary" @click="router.push('/product')">查看全部</el-button>
        </div>
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

    <section class="section-block secondhand-block">
      <div class="section-head">
        <div>
          <h2>闲置捡漏</h2>
          <p>像逛闲置集市一样刷，优先看价格、成色和卖家。</p>
        </div>
        <div class="section-tabs">
          <button v-for="tab in secondhandTabs" :key="tab.label" type="button" @click="goTab(tab)">{{ tab.label }}</button>
          <el-button text type="primary" @click="router.push('/secondhand')">查看全部</el-button>
        </div>
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

const avatarText = computed(() => {
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || "U";
  return name.slice(0, 1).toUpperCase();
});

const categoryCards = [
  { label: "电子数码", short: "数", desc: "键盘、耳机、平板", path: "/product?category=电子数码", color: "#2563eb" },
  { label: "服装鞋包", short: "穿", desc: "日常穿搭好物", path: "/product?category=服装鞋包", color: "#ffb9d6" },
  { label: "学习办公", short: "学", desc: "效率工具和资料", path: "/product?category=学习办公", color: "#8b5cf6" },
  { label: "生活百货", short: "家", desc: "宿舍与日用", path: "/product?category=生活百货", color: "#12a594" },
  { label: "二手数码", short: "闲", desc: "更轻预算淘设备", path: "/secondhand?category=数码闲置", color: "#ffd36e" },
  { label: "教材书籍", short: "书", desc: "教材资料低价淘", path: "/secondhand?category=教材书籍", color: "#b7a6ff" },
];

const productTabs = [
  { label: "热卖", path: "/product" },
  { label: "数码", path: "/product", query: { category: "电子数码" } },
  { label: "百货", path: "/product", query: { category: "生活百货" } },
  { label: "学习", path: "/product", query: { category: "学习办公" } },
];
const secondhandTabs = [
  { label: "新发布", path: "/secondhand" },
  { label: "九成新", path: "/secondhand", query: { condition: "90%" } },
  { label: "低价", path: "/secondhand", query: { sort: "price-asc" } },
  { label: "教材", path: "/secondhand", query: { category: "教材书籍" } },
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
      coupons: ["满 99 减 10", "48h 发货", "支持售后"],
      background: "linear-gradient(100deg, rgba(11,63,82,.78), rgba(60,146,255,.56), rgba(53,216,171,.28)), url('https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=1400&q=80')",
    },
    {
      key: "secondhand",
      tag: "二手精选",
      title: second ? second.name : "把闲置重新流动起来",
      desc: second ? `闲置价 ¥${Number(second.salePrice || 0).toFixed(2)}，成色 ${second.conditionLevel || second.condition || "良好"}` : "校园闲置、个人转手、快速沟通。",
      actionText: "查看闲置",
      actionPath: second ? `/secondhand/${second.id}` : "/secondhand",
      secondaryText: "逛二手市场",
      secondaryPath: "/secondhand",
      coupons: ["同城可看", "成色筛选", "一键沟通"],
      background: "linear-gradient(100deg, rgba(53,216,171,.78), rgba(11,63,82,.52), rgba(255,185,214,.22)), url('https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=1400&q=80')",
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
      coupons: ["分类直达", "价格筛选", "猜你喜欢"],
      background: "linear-gradient(100deg, rgba(60,146,255,.72), rgba(183,166,255,.46), rgba(255,185,214,.2)), url('https://images.unsplash.com/photo-1472851294608-062f824d29cc?auto=format&fit=crop&w=1400&q=80')",
    },
  ];
});

onMounted(async () => {
  await userStore.fetchProfile().catch(() => null);
  await Promise.all([loadProducts(), loadSecondhand(), loadStats()]);
});

async function loadProducts() {
  try {
    const result = await getProductListApi({ pageNum: 1, pageSize: 8 });
    products.value = result.data?.records || [];
  } catch {
    products.value = [];
  }
}

async function loadSecondhand() {
  try {
    const result = await getSecondhandListApi({ pageNum: 1, pageSize: 8 });
    secondhandItems.value = (result.data?.records || []).map((item) => ({
      ...item,
      originPrice: item.originPrice ?? item.salePrice,
      salePrice: item.salePrice ?? item.price,
    }));
  } catch {
    secondhandItems.value = [];
  }
}

async function loadStats() {
  const [orders, notices] = await Promise.allSettled([
    getOrderListApi({ pageNum: 1, pageSize: 1, orderType: "NEW" }),
    listNotificationsApi("buyer"),
  ]);
  if (orders.status === "fulfilled") {
    stats.orderCount = Number(orders.value.data?.total || orders.value.data?.records?.length || 0);
  }
  if (notices.status === "fulfilled") {
    stats.noticeCount = (notices.value.data || []).filter((item) => Number(item.isRead) === 0).length;
  }
}

function goTab(tab) {
  router.push({
    path: tab.path,
    query: tab.query || {},
  });
}

</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.commerce-shell {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr) 230px;
  gap: 12px;
  align-items: stretch;
}

.category-rail,
.member-panel,
.section-block,
.promo-card {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: var(--shadow-soft);
}

.category-rail {
  padding: 12px;
}

.rail-title {
  padding: 4px 4px 10px;
  color: var(--text-main);
  font-size: 15px;
  font-weight: 900;
}

.rail-item {
  width: 100%;
  min-height: 52px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  padding: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  cursor: pointer;
}

.rail-item:hover {
  background: var(--surface-soft);
}

.rail-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  font-weight: 900;
}

.rail-copy {
  min-width: 0;
}

.rail-copy strong,
.rail-copy small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rail-copy strong {
  font-size: 14px;
}

.rail-copy small {
  margin-top: 3px;
  color: var(--text-muted);
  font-size: 12px;
}

.hero-carousel {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: var(--shadow-soft);
  background: #ffffff;
}

.banner-slide {
  height: 100%;
  border-radius: 8px;
  background-size: cover;
  background-position: center;
  display: grid;
  align-items: center;
  padding: 36px;
  position: relative;
  overflow: hidden;
}

.banner-copy {
  max-width: 600px;
  color: var(--text-main);
}

.eyebrow {
  display: inline-flex;
  width: fit-content;
  border-radius: 999px;
  padding: 6px 12px;
  font-weight: 900;
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-main);
}

.banner-copy h1 {
  margin: 16px 0 10px;
  max-width: 620px;
  font-size: clamp(30px, 4.8vw, 52px);
  line-height: 1.05;
  letter-spacing: 0;
  text-shadow: none;
}

.banner-copy p {
  max-width: 560px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1.7;
  font-weight: 700;
}

.hero-actions {
  margin-top: 22px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.voucher-row {
  position: absolute;
  left: 36px;
  right: 36px;
  bottom: 22px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.voucher-row span {
  border: 1px solid rgba(137, 199, 255, 0.45);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--brand-primary);
  padding: 6px 10px;
  font-size: 12px;
  font-weight: 800;
  backdrop-filter: blur(8px);
}

.member-panel {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-head {
  border-radius: 8px;
  background: linear-gradient(135deg, #e9fff8 0%, #eaf4ff 100%);
  color: var(--text-main);
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: var(--brand-gradient);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  font-size: 18px;
  font-weight: 900;
}

.panel-head strong,
.panel-head small {
  display: block;
}

.panel-head strong {
  font-size: 15px;
}

.panel-head small {
  margin-top: 3px;
  color: var(--text-secondary);
  font-size: 12px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.metric {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
  color: var(--text-main);
  min-height: 64px;
  padding: 10px 6px;
  text-align: center;
  cursor: pointer;
}

.metric:hover {
  border-color: var(--brand-primary);
  background: var(--brand-primary-weak);
}

.metric strong {
  display: block;
  color: var(--brand-primary);
  font-size: 22px;
  line-height: 1;
}

.metric span {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.promo-grid {
  display: grid;
  grid-template-columns: 1.2fr 0.9fr 0.9fr;
  gap: 12px;
}

.promo-card {
  min-height: 116px;
  padding: 18px;
  text-align: left;
  cursor: pointer;
  overflow: hidden;
}

.promo-card span,
.promo-card strong,
.promo-card small {
  display: block;
}

.promo-card span {
  color: var(--text-secondary);
  font-weight: 800;
}

.promo-card strong {
  margin-top: 8px;
  color: var(--text-main);
  font-size: 22px;
  font-weight: 900;
}

.promo-card small {
  margin-top: 8px;
  color: var(--text-secondary);
}

.promo-main {
  background:
    linear-gradient(100deg, rgba(60, 146, 255, 0.92), rgba(53, 216, 171, 0.72)),
    url("https://images.unsplash.com/photo-1607083206968-13611e3d76db?auto=format&fit=crop&w=900&q=80");
  background-size: cover;
  background-position: center;
}

.promo-blue {
  background:
    linear-gradient(100deg, rgba(234, 244, 255, 0.92), rgba(183, 166, 255, 0.34)),
    url("https://images.unsplash.com/photo-1526178613552-2b45c6c302f0?auto=format&fit=crop&w=900&q=80");
  background-size: cover;
  background-position: center;
}

.promo-green {
  background:
    linear-gradient(100deg, rgba(233, 255, 248, 0.92), rgba(137, 199, 255, 0.32)),
    url("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80");
  background-size: cover;
  background-position: center;
}

.section-block {
  padding: 16px;
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
  font-size: 22px;
}

.section-head p {
  margin: 6px 0 0;
  color: var(--text-secondary);
}

.section-tabs {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.section-tabs button {
  height: 30px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: #ffffff;
  padding: 0 12px;
  color: var(--text-secondary);
  font-weight: 800;
  cursor: pointer;
}

.section-tabs button:hover {
  color: var(--brand-primary);
  border-color: var(--brand-primary);
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 980px) {
  .commerce-shell,
  .promo-grid {
    grid-template-columns: 1fr;
  }

  .category-rail {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 6px;
  }

  .rail-title {
    grid-column: 1 / -1;
  }

  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .member-panel {
    padding: 16px;
  }
}

@media (max-width: 680px) {
  .category-rail,
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-head {
    flex-direction: column;
    align-items: stretch;
  }

  .section-tabs {
    justify-content: flex-start;
  }

  .banner-slide,
  .member-panel,
  .section-block {
    padding: 18px;
  }

  .hero-carousel :deep(.el-carousel__container) {
    height: 330px !important;
  }

  .voucher-row {
    left: 18px;
    right: 18px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
