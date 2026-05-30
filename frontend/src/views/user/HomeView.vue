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
            <small>好心情和好商品更配哦</small>
          </div>
        </div>
        <div class="metric-grid">
          <button type="button" class="metric" @click="router.push('/order')">
            <span class="metric-icon">
              <el-icon><Document /></el-icon>
            </span>
            <span class="metric-label">我的订单</span>
          </button>
          <button type="button" class="metric" @click="router.push('/notifications')">
            <span v-if="stats.noticeCount" class="metric-badge">{{ stats.noticeCount }}</span>
            <span class="metric-icon">
              <el-icon><Bell /></el-icon>
            </span>
            <span class="metric-label">通知</span>
          </button>
          <button type="button" class="metric" @click="router.push('/browse-history')">
            <span class="metric-icon">
              <el-icon><Clock /></el-icon>
            </span>
            <span class="metric-label">浏览记录</span>
          </button>
          <button type="button" class="metric" @click="router.push('/coupons')">
            <span class="metric-icon">
              <el-icon><Ticket /></el-icon>
            </span>
            <span class="metric-label">我的优惠券</span>
          </button>
          <button v-if="false" type="button" class="metric" @click="router.push('/order')" >
            <strong>订单数：{{ stats.orderCount }}</strong>
          </button>
          <button v-if="false" type="button" class="metric" @click="router.push('/notifications')">
            <strong>通知数：{{ stats.noticeCount }}</strong>
          </button>
          <button v-if="false" type="button" class="metric" @click="router.push('/browse-history')">
            <strong>浏览记录</strong>
          </button>
        </div>
      </aside>
    </div>

    <section class="promo-grid">
      <button type="button" class="promo-card promo-main" @click="router.push('/product')">
        <span>官方商城</span>
        <strong>新品商城</strong>
        <small>按分类选购在售新品</small>
      </button>
      <button type="button" class="promo-card promo-blue" @click="router.push('/secondhand/publish')">
        <span>出售闲置</span>
        <strong>发布我的闲置</strong>
        <small>把不用的物品转给需要的人</small>
      </button>
      <button type="button" class="promo-card promo-green" @click="router.push('/secondhand')">
        <span>购买二手</span>
        <strong>二手商城</strong>
        <small>发现更多实用二手低价好物</small>
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
          <p>看看大家正在转让的闲置好物，按成色、价格慢慢挑^_^</p>
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
import { toAssetUrl } from "@/utils/url";
import { Bell, Clock, Document, Ticket } from "@element-plus/icons-vue";

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
  const product = products.value.find(hasProductImage) || products.value[0];
  const product2 = products.value.filter(hasProductImage)[1] || products.value[1];
  const second = secondhandItems.value.find(hasProductImage) || secondhandItems.value[0];
  return [
    {
      key: "top-sales",
      tag: "新品热卖",
      title: product ? product.name : "本周热卖好物",
      desc: product ? `到手价 ¥${Number(product.price || 0).toFixed(2)}，库存 ${product.stock ?? 0}` : "精选高热度商品，先到先得。",
      actionText: "查看商品",
      actionPath: product ? `/product/${product.id}` : "/product",
      secondaryText: "逛商品市场",
      secondaryPath: "/product",
      coupons: ["官方新品", "多图展示", "安心交易"],
      background: bannerBackground(product),
    },
    {
      key: "secondhand",
      tag: "二手精选",
      title: second ? second.name : "二手闲置商品",
      desc: second ? `闲置价 ¥${Number(second.salePrice || 0).toFixed(2)}，成色 ${second.conditionLevel || second.condition || "良好"}` : "校园闲置、个人转手、快速沟通。",
      actionText: "查看闲置",
      actionPath: second ? `/secondhand/${second.id}` : "/secondhand",
      secondaryText: "逛二手市场",
      secondaryPath: "/secondhand",
      coupons: ["同城可看", "成色筛选", "一键沟通"],
      background: bannerBackground(second),
    },
    {
      key: "daily",
      tag: "大家都在看",
      title: product2 ? product2.name : "按预算挑选刚刚好",
      desc: "按分类、价格、成色快速筛选，快速找到合适商品。",
      actionText: "价格筛选",
      actionPath: "/product",
      secondaryText: "二手捡漏",
      secondaryPath: "/secondhand",
      coupons: ["分类直达", "价格筛选", "猜你喜欢"],
      background: bannerBackground(product2),
    },
  ].filter((slide) => slide.background);
});

onMounted(async () => {
  await userStore.fetchProfile().catch(() => null);
  await Promise.all([loadProducts(), loadSecondhand(), loadStats()]);
});

async function loadProducts() {
  try {
    const result = await getProductListApi({ pageNum: 1, pageSize: 24 });
    products.value = prioritizeProductsWithImages(result.data?.records || []).slice(0, 8);
  } catch {
    products.value = [];
  }
}

async function loadSecondhand() {
  try {
    const result = await getSecondhandListApi({ pageNum: 1, pageSize: 24 });
    secondhandItems.value = prioritizeProductsWithImages((result.data?.records || []).map((item) => ({
      ...item,
      originPrice: item.originPrice ?? item.salePrice,
      salePrice: item.salePrice ?? item.price,
    }))).slice(0, 8);
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

function bannerBackground(item) {
  const imageUrl = pickProductImageUrl(item);
  return imageUrl ? `url("${imageUrl}")` : "";
}

function pickProductImageUrl(item) {
  if (!item) {
    return "";
  }
  const directUrl = item.cover || item.coverUrl || item.imageUrl;
  if (hasText(directUrl)) {
    return toAssetUrl(directUrl);
  }
  if (Array.isArray(item.images)) {
    return toAssetUrl(item.images.find(hasText) || "");
  }
  if (typeof item.images === "string" && hasText(item.images) && item.images !== "[]") {
    try {
      const parsed = JSON.parse(item.images);
      if (Array.isArray(parsed)) {
        return toAssetUrl(parsed.find(hasText) || "");
      }
    } catch {
      return toAssetUrl(item.images);
    }
  }
  return "";
}

function prioritizeProductsWithImages(list) {
  return [...list]
    .map((item, index) => ({ item, index }))
    .sort((a, b) => Number(hasProductImage(b.item)) - Number(hasProductImage(a.item)) || a.index - b.index)
    .map(({ item }) => item);
}

function hasProductImage(item) {
  if (!item) {
    return false;
  }
  if (hasText(item.cover) || hasText(item.coverUrl) || hasText(item.imageUrl)) {
    return true;
  }
  if (Array.isArray(item.images)) {
    return item.images.some(hasText);
  }
  if (typeof item.images === "string") {
    return hasText(item.images) && item.images !== "[]";
  }
  return false;
}

function hasText(value) {
  return typeof value === "string" && value.trim().length > 0;
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
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(0, 1fr));
  gap: 10px;
  flex: 1;
  min-height: 0;
}

.metric {
  position: relative;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
  color: var(--text-main);
  min-height: 0;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;
  cursor: pointer;
}

.metric:hover {
  border-color: var(--brand-primary);
  background: var(--brand-primary-weak);
}

.metric-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: #ffffff;
  color: var(--brand-primary);
  box-shadow: 0 6px 16px rgba(137, 199, 255, 0.16);
}

.metric-icon :deep(.el-icon) {
  font-size: 24px;
}

.metric-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 1 auto;
  min-width: 0;
  margin-top: 0;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 800;
  line-height: 1.25;
  text-align: center;
}

.metric-badge {
  position: absolute;
  top: 7px;
  right: 7px;
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  padding: 0 5px;
  background: #ef4444;
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 900;
  line-height: 1;
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

}
</style>
