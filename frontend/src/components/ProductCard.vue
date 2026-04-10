<template>
  <article class="product-card" :class="{ clickable }" @click="goDetail">
    <div class="cover-wrap">
      <img class="cover" :src="coverUrl" :alt="product.name" loading="lazy" />
      <span class="badge">{{ badgeText }}</span>
    </div>
    <div class="content">
      <h3 class="title">{{ product.name }}</h3>
      <p class="desc">{{ descriptionText }}</p>
      <div class="meta">
        <strong class="price">¥{{ formatPrice(mainPrice) }}</strong>
        <span class="sub">{{ subText }}</span>
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
  },
  mode: {
    type: String,
    default: 'product'
  },
  clickable: {
    type: Boolean,
    default: true
  },
  routeBase: {
    type: String,
    default: '/product'
  }
});

const router = useRouter();

const isSecondhand = computed(() => props.mode === 'secondhand');

const badgeText = computed(() => {
  if (isSecondhand.value) {
    return props.product.conditionLevel || props.product.condition || '二手';
  }
  return props.product.statusName || '在售';
});

const descriptionText = computed(() => {
  return props.product.description || '品质好物，支持快速发货';
});

const mainPrice = computed(() => {
  return isSecondhand.value ? props.product.salePrice : props.product.price;
});

const subText = computed(() => {
  if (isSecondhand.value) {
    return `原价 ¥${formatPrice(props.product.originPrice)}`;
  }
  return `库存 ${props.product.stock ?? 0}`;
});

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
  if (!props.clickable) {
    return;
  }
  router.push(`${props.routeBase}/${props.product.id}`);
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
  transition: transform .2s ease, box-shadow .2s ease;
}

.product-card.clickable {
  cursor: pointer;
}

.product-card.clickable:hover {
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

.sub {
  color: #8c8c8c;
  font-size: 12px;
}
</style>
