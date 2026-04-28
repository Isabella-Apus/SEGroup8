<template>
  <section class="home-page">
    <div class="hero">
      <div>
        <h1>精选分类导航</h1>
        <p>从 7 大一级类目快速进入，支持一级/二级精细筛选</p>
      </div>
    </div>

    <div class="category-nav">
      <button
        v-for="item in topCategories"
        :key="item.value"
        type="button"
        class="cat-item"
        @click="goCategory(item.value)"
      >
        <span class="cat-icon" v-html="iconSvgMap[item.icon]"></span>
        <span class="cat-name">{{ item.label }}</span>
      </button>
    </div>

    <h3 class="section-title">推荐商品</h3>
    <div class="product-grid">
      <div class="p-card" v-for="item in products" :key="item.id">
        <div class="p-name">{{ item.name }}</div>
        <div class="p-meta">¥{{ Number(item.price || 0).toFixed(2) }} · 库存 {{ item.stock }}</div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getProductListApi } from '@/api/product';
import { useUserStore } from '@/stores/user';
import { CATEGORY_TREE } from '@/constants/categories';

const userStore = useUserStore();
const router = useRouter();
const products = ref([]);
const topCategories = CATEGORY_TREE.filter((item) => item.value !== 8);

const iconSvgMap = {
  phone: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="7" y="2" width="10" height="20" rx="2.2" fill="#f6fbff" stroke="#1f6fb2" stroke-width="1.5"/><circle cx="12" cy="18.2" r="1.2" fill="#1f6fb2"/></svg>',
  bag: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="7" width="16" height="13" rx="2" fill="#fff6ea" stroke="#bf6d1d" stroke-width="1.5"/><path d="M8 9V7a4 4 0 0 1 8 0v2" fill="none" stroke="#bf6d1d" stroke-width="1.6"/></svg>',
  home: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 11.5 12 4l9 7.5" fill="none" stroke="#2a7b4f" stroke-width="1.6"/><path d="M6 10.5V20h12v-9.5" fill="#f1fff6" stroke="#2a7b4f" stroke-width="1.6"/></svg>',
  makeup: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="11" width="14" height="9" rx="2" fill="#fff3f7" stroke="#c04d7a" stroke-width="1.5"/><rect x="9" y="4" width="6" height="7" rx="1" fill="#ffd5e4" stroke="#c04d7a" stroke-width="1.5"/></svg>',
  sport: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="8" cy="8" r="3" fill="#eff8ff" stroke="#2a74b8" stroke-width="1.5"/><circle cx="16" cy="16" r="3" fill="#eff8ff" stroke="#2a74b8" stroke-width="1.5"/><path d="M10.2 10.2 13.8 13.8" stroke="#2a74b8" stroke-width="1.6"/></svg>',
  book: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 5a2 2 0 0 1 2-2h5v17H6a2 2 0 0 0-2 2V5z" fill="#fff9e7" stroke="#946f24" stroke-width="1.5"/><path d="M20 5a2 2 0 0 0-2-2h-5v17h5a2 2 0 0 1 2 2V5z" fill="#fff9e7" stroke="#946f24" stroke-width="1.5"/></svg>',
  food: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 3v7" stroke="#9d5f17" stroke-width="1.6"/><path d="M10 3v7" stroke="#9d5f17" stroke-width="1.6"/><path d="M6 10h5" stroke="#9d5f17" stroke-width="1.6"/><path d="M16 3v18" stroke="#9d5f17" stroke-width="1.6"/><path d="M16 8c2.2 0 3.5-1.7 3.5-3.8V3H16z" fill="#ffeecf" stroke="#9d5f17" stroke-width="1.2"/></svg>',
};

onMounted(async () => {
  await userStore.fetchProfile();
  const result = await getProductListApi({ pageNum: 1, pageSize: 5 });
  products.value = result.data?.records || [];
});

function goCategory(categoryId) {
  router.push({ path: '/product', query: { categoryId } });
}
</script>

<style scoped>
.home-page {
  padding: 8px;
}

.hero {
  background: radial-gradient(circle at 10% 15%, #ffe7bf, #fff6e7 60%, #fff 100%);
  border: 1px solid #f5deba;
  border-radius: 20px;
  padding: 20px;
}

.hero h1 {
  margin: 0;
  font-size: 28px;
}

.hero p {
  margin: 8px 0 0;
  color: #78582f;
}

.category-nav {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.cat-item {
  border: 1px solid #efe0c8;
  background: #fff;
  border-radius: 14px;
  padding: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
}

.cat-item:hover {
  background: #fff6e8;
  border-color: #f3c98d;
}

.cat-icon {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: #fffaf2;
}

.cat-icon :deep(svg) {
  width: 22px;
  height: 22px;
}

.cat-name {
  font-weight: 700;
  color: #453624;
}

.section-title {
  margin: 20px 0 10px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.p-card {
  border: 1px solid #e9eef4;
  border-radius: 12px;
  padding: 10px;
  background: #fff;
}

.p-name {
  font-weight: 700;
  color: #24354a;
}

.p-meta {
  margin-top: 6px;
  color: #657385;
  font-size: 13px;
}

@media (max-width: 760px) {
  .category-nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .product-grid {
    grid-template-columns: 1fr;
  }
}
</style>
