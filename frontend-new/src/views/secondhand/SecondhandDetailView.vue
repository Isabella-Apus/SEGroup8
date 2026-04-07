<template>
  <div class="page-card">
    <h2 class="page-title">二手商品详情</h2>
    <el-skeleton v-if="loading" :rows="6" animated />

    <div v-else-if="item" class="detail-wrap">
      <div class="cover-box">
        <el-image v-if="item.cover" :src="toFullImageUrl(item.cover)" fit="cover" class="cover-image" />
        <div v-else class="cover-placeholder">暂无图片</div>
      </div>

      <div class="info-box">
        <h3>{{ item.name }}</h3>
        <p class="price">￥{{ Number(item.salePrice || 0).toFixed(2) }}</p>
        <p class="origin">原价：￥{{ Number(item.originPrice || item.salePrice || 0).toFixed(2) }}</p>
        <p>成色：{{ item.conditionLevel || item.condition || '未标注' }}</p>
        <p>状态：{{ item.statusName || '在售' }}</p>
        <p class="desc">{{ item.description || '暂无商品描述' }}</p>

        <div class="actions">
          <span>购买数量：</span>
          <el-input-number v-model="quantity" :min="1" :max="maxQuantity" />
        </div>

        <el-space>
          <el-button type="primary" @click="handleAddToCart" :disabled="maxQuantity <= 0">加入购物车</el-button>
          <el-button @click="handleBuyNow" :disabled="maxQuantity <= 0">立即下单</el-button>
          <el-button type="primary" plain @click="router.push('/secondhand/publish')">我也要发布</el-button>
          <el-button text @click="router.push('/secondhand')">返回列表</el-button>
        </el-space>
      </div>
    </div>

    <p v-else class="empty-tip">二手商品不存在或已下架</p>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import { getSecondhandDetailApi } from '@/api/secondhand';
import { createOrderApi } from '@/api/order';
import { addToCart, removeFromCart } from '@/utils/cart';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const item = ref(null);
const quantity = ref(1);

const maxQuantity = computed(() => {
  if (!item.value) {
    return 0;
  }
  const stock = Number(item.value.stock || 1);
  return stock > 0 ? stock : 0;
});

onMounted(async () => {
  await fetchDetail();
});

async function fetchDetail() {
  loading.value = true;
  try {
    const result = await getSecondhandDetailApi(route.params.id);
    item.value = result.data;
    if (maxQuantity.value > 0) {
      quantity.value = 1;
    }
  } finally {
    loading.value = false;
  }
}

function handleAddToCart() {
  if (!item.value) {
    return;
  }
  if (quantity.value > maxQuantity.value) {
    ElMessage.warning('购买数量超过库存');
    return;
  }
  addToCart(item.value, Number(quantity.value), {
    itemType: 'SECONDHAND',
    unitPrice: item.value.salePrice,
    stock: maxQuantity.value
  });
  ElMessage.success('已加入购物车');
}

async function handleBuyNow() {
  if (!item.value) {
    return;
  }
  if (quantity.value > maxQuantity.value) {
    ElMessage.warning('购买数量超过库存');
    return;
  }
  await createOrderApi({
    items: [{
      productId: item.value.id,
      itemType: 'SECONDHAND',
      quantity: Number(quantity.value)
    }]
  });
  removeFromCart(item.value.id, 'SECONDHAND');
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

.origin {
  color: #6b7280;
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
