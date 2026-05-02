<template>
  <article class="product-card" :class="{ clickable }" @click="goDetail">
    <div class="cover-wrap">
      <img class="cover" :src="coverUrl" :alt="product.name" loading="lazy" />
      <span class="badge">{{ badgeText }}</span>
    </div>
    <div class="content">
      <h3 class="title" v-html="highlightedTitle"></h3>
      <p class="desc">{{ descriptionText }}</p>
      <div class="meta">
        <strong class="price">￥{{ formatPrice(mainPrice) }}</strong>
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
  },
  highlightKeyword: {
    type: String,
    default: ''
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

const highlightedTitle = computed(() => {
  const source = String(props.product.name || '');
  const escaped = escapeHtml(source);
  const keyword = String(props.highlightKeyword || '').trim();
  if (!keyword) {
    return escaped;
  }
  const escapedKeyword = escapeRegExp(keyword);
  const reg = new RegExp(`(${escapedKeyword})`, 'ig');
  return escaped.replace(reg, '<span class="title-highlight">$1</span>');
});

const mainPrice = computed(() => {
  return isSecondhand.value ? props.product.salePrice : props.product.price;
});

const subText = computed(() => {
  if (isSecondhand.value) {
    return `原价 ￥${formatPrice(props.product.originPrice)}`;
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

function escapeHtml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function escapeRegExp(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
</script>

<style scoped>
.product-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #eeeeee;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 8px 20px rgba(30, 34, 40, 0.04);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.product-card.clickable {
  cursor: pointer;
}

.product-card.clickable:hover {
  transform: translateY(-3px);
  border-color: #ffe100;
  box-shadow: 0 16px 34px rgba(30, 34, 40, 0.12);
}

.cover-wrap {
  position: relative;
  aspect-ratio: 1 / 1;
  background: #eeeeee;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.badge {
  position: absolute;
  left: 10px;
  top: 10px;
  max-width: calc(100% - 20px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  border-radius: 999px;
  padding: 4px 9px;
  background: rgba(32, 36, 45, 0.78);
  color: #fff;
  font-size: 12px;
}

.content {
  padding: 12px 13px 13px;
}

.title {
  min-height: 40px;
  margin: 0;
  color: #20242d;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.title :deep(.title-highlight) {
  color: #e6a23c;
  font-weight: 700;
}

.desc {
  min-height: 18px;
  margin: 7px 0 9px;
  color: #858585;
  font-size: 12px;
  line-height: 1.5;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 8px;
}

.price {
  color: #ff4d00;
  font-size: 21px;
  line-height: 1;
  white-space: nowrap;
}

.sub {
  min-width: 0;
  color: #999;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 760px) {
  .content {
    padding: 9px;
  }

  .title {
    font-size: 14px;
  }

  .price {
    font-size: 18px;
  }
}
</style>
