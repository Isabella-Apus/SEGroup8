<template>
  <section class="home-page">
    <section class="hero-panel">
      <div class="hero-copy">
        <p>校园好物正在流转</p>
        <h1>买新品、淘闲置、管订单，都在一个清爽首页里</h1>
        <div class="hero-actions">
          <el-button type="primary" round @click="$router.push('/product')">逛商品市场</el-button>
          <el-button type="warning" round @click="$router.push('/secondhand/publish')">发布闲置</el-button>
        </div>
      </div>

      <div class="hero-feature">
        <button
          v-for="item in featureItems"
          :key="item.id"
          class="feature-card"
          type="button"
          @click="goProduct(item.id)"
        >
          <img v-if="item.cover" :src="resolveCover(item.cover)" :alt="item.name" />
          <span v-else class="feature-placeholder"></span>
          <strong>{{ item.name || '校园好物' }}</strong>
          <em>￥{{ formatPrice(item.price) }}</em>
        </button>

        <button
          v-for="index in placeholderCount"
          :key="`feature-empty-${index}`"
          class="feature-card muted"
          type="button"
          @click="$router.push('/product')"
        >
          <span class="feature-placeholder"></span>
          <strong>去发现更多好物</strong>
          <em>立即浏览</em>
        </button>
      </div>
    </section>

    <section class="quick-grid" aria-label="快捷入口">
      <button
        v-for="item in quickItems"
        :key="item.path"
        class="quick-item"
        type="button"
        @click="$router.push(item.path)"
      >
        <span>{{ item.icon }}</span>
        <strong>{{ item.label }}</strong>
        <small>{{ item.desc }}</small>
      </button>
    </section>

    <section class="recommend-panel">
      <div class="section-head">
        <div>
          <p>猜你喜欢</p>
          <h2>为你挑选的商品</h2>
        </div>
        <el-button text @click="$router.push('/product')">查看更多</el-button>
      </div>

      <div v-if="recommendedItems.length" class="product-grid">
        <ProductCard
          v-for="item in recommendedItems"
          :key="item.id"
          :product="item"
          mode="product"
          route-base="/product"
        />
      </div>

      <div v-else class="empty-recommend">
        <div class="empty-mark">闲</div>
        <p>暂时没有推荐商品</p>
        <el-button type="warning" round @click="$router.push('/secondhand/publish')">先发布一个闲置</el-button>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import ProductCard from '@/components/ProductCard.vue';
import { getProductListApi } from '@/api/product';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const userStore = useUserStore();
const products = ref([]);

const quickItems = [
  { label: '购物车', desc: '继续结算未完成商品', path: '/cart', icon: '▣' },
  { label: '我的订单', desc: '查看付款、发货和售后', path: '/order', icon: '≡' },
  { label: '我的优惠券', desc: '管理已领取优惠', path: '/vouchers', icon: '%' },
  { label: '地址管理', desc: '维护收货地址', path: '/addresses', icon: '⌖' },
  { label: '售后退款', desc: '处理退款与售后申请', path: '/after-sale', icon: '↺' },
  { label: '我的评价', desc: '查看已发布评价', path: '/my-reviews', icon: '☆' },
  { label: '浏览记录', desc: '找回看过的商品', path: '/browse-history', icon: '◷' },
  { label: '我的信用', desc: '查看信用信息', path: '/credit', icon: '✓' },
];

const featureItems = computed(() => products.value.slice(0, 2));
const recommendedItems = computed(() => products.value.slice(2, 14));
const placeholderCount = computed(() => Math.max(0, 2 - featureItems.value.length));

onMounted(async () => {
  await userStore.fetchProfile();
  const result = await getProductListApi({ pageNum: 1, pageSize: 14 });
  products.value = result.data?.records || [];
});

function goProduct(id) {
  if (id) {
    router.push(`/product/${id}`);
    return;
  }
  router.push('/product');
}

function resolveCover(cover) {
  if (!cover) return '';
  if (cover.startsWith('http')) return cover;
  return `http://localhost:8080${cover}`;
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2);
}
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero-panel,
.recommend-panel {
  border: 1px solid #eeeeee;
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(30, 34, 40, 0.04);
}

.hero-panel {
  min-height: 340px;
  padding: 22px;
  display: grid;
  grid-template-columns: minmax(320px, 1fr) 500px;
  gap: 22px;
  overflow: hidden;
}

.hero-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-radius: 18px;
  padding: 28px;
  background:
    radial-gradient(circle at 82% 18%, rgba(255, 225, 0, 0.42), transparent 30%),
    linear-gradient(135deg, #fffdf0, #ffffff 58%);
}

.hero-copy p {
  margin: 0 0 10px;
  color: #8a7100;
  font-weight: 900;
}

.hero-copy h1 {
  max-width: 620px;
  margin: 0;
  color: #20242d;
  font-size: 34px;
  line-height: 1.2;
  letter-spacing: 0;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.hero-feature {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.feature-card {
  position: relative;
  min-height: 296px;
  border: 0;
  border-radius: 18px;
  padding: 0;
  overflow: hidden;
  background: #eeeeee;
  cursor: pointer;
  text-align: left;
}

.feature-card img,
.feature-placeholder {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.feature-placeholder {
  background:
    linear-gradient(135deg, rgba(255, 225, 0, 0.18), transparent),
    #eeeeee;
}

.feature-card::after {
  position: absolute;
  inset: 38% 0 0;
  content: "";
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.56));
}

.feature-card strong,
.feature-card em {
  position: absolute;
  left: 16px;
  right: 16px;
  z-index: 1;
}

.feature-card strong {
  bottom: 48px;
  color: #fff;
  font-size: 18px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.28);
}

.feature-card em {
  bottom: 18px;
  color: #ffe100;
  font-style: normal;
  font-size: 20px;
  font-weight: 900;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

.feature-card.muted {
  cursor: pointer;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.quick-item {
  min-height: 102px;
  border: 1px solid #eeeeee;
  border-radius: 18px;
  padding: 16px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(30, 34, 40, 0.04);
  cursor: pointer;
  text-align: left;
}

.quick-item:hover {
  border-color: #ffe100;
  transform: translateY(-2px);
}

.quick-item span {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #fff7c2;
  color: #20242d;
  font-weight: 900;
}

.quick-item strong {
  display: block;
  margin-top: 10px;
  color: #20242d;
}

.quick-item small {
  display: block;
  margin-top: 4px;
  color: #858b95;
}

.recommend-panel {
  min-height: 388px;
  padding: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 18px;
}

.section-head p {
  margin: 0 0 4px;
  color: #8a8f99;
  font-size: 13px;
}

.section-head h2 {
  margin: 0;
  color: #20242d;
  font-size: 25px;
  letter-spacing: 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.empty-recommend {
  min-height: 292px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  color: #20242d;
}

.empty-mark {
  width: 106px;
  height: 106px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #ffe100;
  color: #20242d;
  font-size: 42px;
  font-weight: 900;
  box-shadow: 0 0 34px rgba(255, 225, 0, 0.42);
}

.empty-recommend p {
  margin: 0;
  font-size: 17px;
}

@media (max-width: 1180px) {
  .hero-panel {
    grid-template-columns: 1fr;
  }

  .quick-grid,
  .product-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero-panel,
  .recommend-panel {
    border-radius: 18px;
    padding: 14px;
  }

  .hero-copy {
    padding: 18px;
  }

  .hero-copy h1 {
    font-size: 25px;
  }

  .hero-feature,
  .quick-grid,
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .feature-card {
    min-height: 218px;
  }

  .quick-item {
    min-height: 96px;
    padding: 13px;
  }
}
</style>
