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
  background: var(--card-bg);
  border: 1px solid var(--line-soft);
  border-radius: 20px;
  overflow: hidden;
  transition: transform .22s ease, box-shadow .22s ease, border-color .22s ease;
}

.product-card.clickable {
  cursor: pointer;
}

.product-card.clickable:hover {
  transform: translateY(-4px);
  border-color: #d6e5f6;
  box-shadow: 0 16px 34px rgba(20, 38, 69, .16);
}

.cover-wrap {
  position: relative;
  aspect-ratio: 4 / 3;
  background: linear-gradient(145deg, #f4f7fc, #eef3f9);
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
  background: rgba(23, 34, 52, .84);
  color: #fff;
  border-radius: 999px;
  padding: 5px 11px;
  font-size: 12px;
  letter-spacing: .2px;
}

.content {
  padding: 14px;
}

.title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.3;
  color: #1f2329;
}

.title :deep(.title-highlight) {
  color: #e6a23c;
  font-weight: 700;
}

.desc {
  margin: 8px 0;
  color: #68748a;
  font-size: 13px;
  line-height: 1.45;
  min-height: 34px;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price {
  color: #e06c1d;
  font-size: 23px;
  letter-spacing: .2px;
}

.sub {
  color: #7f8b9f;
  font-size: 12px;
}
</style>
