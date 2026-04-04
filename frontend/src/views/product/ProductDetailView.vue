<template>
  <div class="page-card">
    <h2 class="page-title">商品详情</h2>
    <el-skeleton v-if="loading" :rows="6" animated />

    <div v-else-if="product" class="detail-wrap">
      <div class="cover-box">
        <el-image v-if="product.cover" :src="toFullImageUrl(product.cover)" fit="cover" class="cover-image" />
        <div v-else class="cover-placeholder">暂无图片</div>
      </div>

      <div class="info-box">
        <h3>{{ product.name }}</h3>
        <p class="price">￥{{ Number(product.price || 0).toFixed(2) }}</p>
        <p>库存：{{ product.stock }}</p>
        <p>状态：{{ product.statusName }}</p>
        <p class="desc">{{ product.description || '暂无商品描述' }}</p>

        <div class="actions">
          <span>购买数量：</span>
          <el-input-number v-model="quantity" :min="1" :max="maxQuantity" />
        </div>

        <el-space>
          <el-button type="primary" @click="handleAddToCart" :disabled="maxQuantity <= 0">加入购物车</el-button>
          <el-button @click="handleBuyNow" :disabled="maxQuantity <= 0">立即下单</el-button>
          <el-button text @click="router.push('/product')">返回列表</el-button>
        </el-space>
      </div>
    </div>

    <p v-else class="empty-tip">商品不存在或已下架</p>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { getProductDetailApi } from '@/api/product';
import { createOrderApi } from '@/api/order';
import { addToCart, removeFromCart } from '@/utils/cart';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const product = ref(null);
const quantity = ref(1);

const maxQuantity = computed(() => {
  if (!product.value) {
    return 0;
  }
  return Number(product.value.stock || 0);
});

onMounted(async () => {
  await fetchDetail();
});

async function fetchDetail() {
  loading.value = true;
  try {
    const result = await getProductDetailApi(route.params.id);
    product.value = result.data;
    if (maxQuantity.value > 0) {
      quantity.value = 1;
    }
  } finally {
    loading.value = false;
  }
}

function handleAddToCart() {
  if (!product.value) {
    return;
  }
  if (quantity.value > maxQuantity.value) {
    ElMessage.warning('购买数量超过库存');
    return;
  }
  addToCart(product.value, Number(quantity.value));
  ElMessage.success('已加入购物车');
}

async function handleBuyNow() {
  if (!product.value) {
    return;
  }
  if (quantity.value > maxQuantity.value) {
    ElMessage.warning('购买数量超过库存');
    return;
  }
  await createOrderApi({
    items: [{ productId: product.value.id, quantity: Number(quantity.value) }]
  });
  removeFromCart(product.value.id);
  ElMessage.success('下单成功');
  router.push('/order');
}

function toFullImageUrl(url) {
  if (!url) {
    return '';
  }
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  const normalized = url.startsWith('/') ? url : `/${url}`;
  return `http://localhost:8080${normalized}`;
}
</script>

<style scoped>
.detail-wrap {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
}

.cover-box {
  width: 280px;
  height: 280px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  background: #f9fafb;
}

.price {
  color: #ef4444;
  font-size: 22px;
  font-weight: 700;
  margin: 8px 0;
}

.desc {
  color: #4b5563;
  line-height: 1.8;
}

.actions {
  margin: 14px 0;
}

@media (max-width: 900px) {
  .detail-wrap {
    grid-template-columns: 1fr;
  }

  .cover-box {
    width: 100%;
    max-width: 360px;
    margin: 0 auto;
  }
}
</style>
