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

        <p class="desc">二手商品仅支持单件购买，提交后将自动下单。</p>

        <el-space>
          <el-button type="primary" @click="handleBuyNow" :disabled="!canBuy">立即购买</el-button>
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
import { buySecondhandApi, getSecondhandDetailApi } from '@/api/secondhand';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const item = ref(null);

const canBuy = computed(() => !!item.value && Number(item.value.status || 1) === 1);

onMounted(async () => {
  await fetchDetail();
});

async function fetchDetail() {
  loading.value = true;
  try {
    const result = await getSecondhandDetailApi(route.params.id);
    item.value = result.data;
  } finally {
    loading.value = false;
  }
}

async function handleBuyNow() {
  if (!canBuy.value) {
    ElMessage.warning('当前商品不可购买');
    return;
  }
  await buySecondhandApi(item.value.id, {});
  ElMessage.success('购买成功');
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
