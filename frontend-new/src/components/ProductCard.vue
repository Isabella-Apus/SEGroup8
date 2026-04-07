<template>
  <article class="product-card" @click="goDetail">
    <div class="cover-wrap">
      <img class="cover" :src="coverUrl" :alt="product.name" loading="lazy" />
      <span class="badge">{{ product.statusName || '在售' }}</span>
    </div>
    <div class="content">
      <h3 class="title">{{ product.name }}</h3>
      <p class="desc">{{ product.description || '品质好物，支持快速发货' }}</p>
      <div class="meta">
        <strong class="price">¥{{ formatPrice(product.price) }}</strong>
        <span class="stock">库存 {{ product.stock ?? 0 }}</span>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';

const props = defineProps({
  product: {
    type: Object,
    required: true
  }
});

const router = useRouter();

const coverUrl = computed(() => {
  const cover = props.product.cover || '';
  if (!cover) {
    return 'https://images.unsplash.com/photo-1511556820780-d912e42b4980?auto=format&fit=crop&w=900&q=80';
  }
  if (cover.startsWith('http')) {
    return cover;
  }
  return `http://localhost:8080${cover}`;
});

function goDetail() {
  router.push(`/product/${props.product.id}`);
}

function formatPrice(value) {
  const num = Number(value || 0);
  return num.toFixed(2);
}
</script>

<style scoped>
.product-card {
  background: var(--card-bg);
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  overflow: hidden;
  cursor: pointer;
  transition: transform .2s ease, box-shadow .2s ease;
}

.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 30px rgba(0, 0, 0, .08);
}

.cover-wrap {
  position: relative;
  aspect-ratio: 4 / 3;
  background: #f7f7f7;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.badge {
  position: absolute;
  right: 10px;
  top: 10px;
  background: rgba(17, 17, 17, .82);
  color: #fff;
  border-radius: 999px;
  padding: 4px 10px;
  font-size: 12px;
}

.content {
  padding: 12px;
}

.title {
  margin: 0;
  font-size: 16px;
  line-height: 1.3;
  color: #1f2329;
}

.desc {
  margin: 8px 0;
  color: #666;
  font-size: 13px;
  line-height: 1.35;
  min-height: 34px;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: var(--brand-orange);
  font-size: 22px;
}

.stock {
  color: #8c8c8c;
  font-size: 12px;
}
</style>
