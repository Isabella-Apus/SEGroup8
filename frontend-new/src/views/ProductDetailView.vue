<template>
  <section class="detail" v-loading="loading">
    <button class="back" type="button" @click="$router.back()">返回</button>
    <div v-if="product" class="panel">
      <div class="left">
        <img class="cover" :src="coverUrl" :alt="product.name" />
      </div>
      <div class="right">
        <h2>{{ product.name }}</h2>
        <p class="desc">{{ product.description || '暂无商品描述' }}</p>
        <p class="price">¥{{ formatPrice(product.price) }}</p>
        <p class="meta">库存：{{ product.stock }} · 状态：{{ product.statusName }}</p>
        <div class="actions">
          <el-input-number v-model="quantity" :min="1" :max="Math.max(1, Number(product.stock || 1))" />
          <el-button type="primary" @click="handleAddToCart" :disabled="Number(product.stock || 0) <= 0">加入购物车</el-button>
          <el-button @click="handleBuyNow" :disabled="Number(product.stock || 0) <= 0">立即下单</el-button>
          <el-button text @click="$router.push('/product')">返回列表</el-button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getProductDetailApi } from '@/api/product';
import { addToCart } from '@/utils/cart';

const route = useRoute();
const loading = ref(false);
const product = ref(null);
const quantity = ref(1);

const coverUrl = computed(() => {
  const cover = product.value?.cover || '';
  if (!cover) {
    return 'https://images.unsplash.com/photo-1491933382434-500287f9b54b?auto=format&fit=crop&w=1200&q=80';
  }
  if (cover.startsWith('http')) {
    return cover;
  }
  return `http://localhost:8080${cover}`;
});

onMounted(async () => {
  await fetchDetail();
});

async function fetchDetail() {
  loading.value = true;
  try {
    const res = await getProductDetailApi(route.params.id);
    product.value = res.data;
  } finally {
    loading.value = false;
  }
}

function formatPrice(value) {
  return Number(value || 0).toFixed(2);
}

function handleAddToCart() {
  if (!product.value) {
    return;
  }
  addToCart(product.value, quantity.value);
  ElMessage.success('加入购物车成功');
}

function handleBuyNow() {
  ElMessage.success('立即下单按钮已复现');
}
</script>

<style scoped>
.detail {
  padding: 8px;
}

.back {
  border: 0;
  background: #fff;
  color: #555;
  border-radius: 10px;
  padding: 8px 14px;
  cursor: pointer;
  margin-bottom: 10px;
}

.panel {
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  background: #fff;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  padding: 14px;
}

.cover {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  border-radius: 12px;
}

.right h2 {
  margin: 4px 0 8px;
}

.desc {
  color: #666;
  line-height: 1.45;
}

.price {
  color: var(--brand-orange);
  font-size: 34px;
  margin: 16px 0 8px;
  font-weight: 800;
}

.meta {
  color: #81858f;
}

.actions {
  margin-top: 14px;
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

@media (max-width: 760px) {
  .panel {
    grid-template-columns: 1fr;
  }
}
</style>
