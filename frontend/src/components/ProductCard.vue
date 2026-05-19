<template>
  <article class="product-card" :class="{ secondhand: isSecondhand }">
    <button class="cover-wrap" type="button" @click="goDetail">
      <img class="cover" :src="coverUrl" :alt="product.name" loading="lazy" />
      <span class="badge">{{ badgeText }}</span>
      <span v-if="isLowStock" class="stock-badge">库存紧张</span>
    </button>

    <div class="content">
      <button class="title-button" type="button" @click="goDetail">
        {{ product.name }}
      </button>
      <p class="desc">{{ descriptionText }}</p>

      <div class="seller-row">
        <span>{{ sellerText }}</span>
        <span>{{ subText }}</span>
      </div>

      <div class="meta">
        <div>
          <strong class="price">¥{{ formatPrice(mainPrice) }}</strong>
          <span v-if="isSecondhand" class="origin">¥{{ formatPrice(product.originPrice || product.price) }}</span>
        </div>
        <div class="actions">
          <el-button v-if="!isSecondhand" size="small" @click.stop="handleAddCart">加购</el-button>
          <el-button size="small" type="primary" @click.stop="goDetail">查看</el-button>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { addToCart } from "@/utils/cart";
import { toAssetUrl } from "@/utils/url";

const props = defineProps({
  product: {
    type: Object,
    required: true,
  },
  mode: {
    type: String,
    default: "product",
  },
  clickable: {
    type: Boolean,
    default: true,
  },
  routeBase: {
    type: String,
    default: "/product",
  },
});

const router = useRouter();

const isSecondhand = computed(() => props.mode === "secondhand");

const badgeText = computed(() => {
  if (isSecondhand.value) {
    return props.product.conditionLevel || props.product.condition || "二手";
  }
  return props.product.statusName || "在售";
});

const descriptionText = computed(() => {
  return props.product.description || (isSecondhand.value ? "个人闲置，详情页可联系卖家" : "官方好物，支持下单和售后");
});

const mainPrice = computed(() => {
  return isSecondhand.value ? props.product.salePrice ?? props.product.price : props.product.price;
});

const subText = computed(() => {
  if (isSecondhand.value) {
    return "单件闲置";
  }
  return `库存 ${props.product.stock ?? 0}`;
});

const sellerText = computed(() => {
  return props.product.sellerName || props.product.shopName || (isSecondhand.value ? "个人卖家" : "官方商家");
});

const coverUrl = computed(() => {
  return toAssetUrl(props.product.cover) || "https://images.unsplash.com/photo-1511556820780-d912e42b4980?auto=format&fit=crop&w=900&q=80";
});

const isLowStock = computed(() => !isSecondhand.value && Number(props.product.stock || 0) > 0 && Number(props.product.stock || 0) <= 5);

function goDetail() {
  if (!props.clickable) {
    return;
  }
  router.push(`${props.routeBase}/${props.product.id}`);
}

function handleAddCart() {
  if (Number(props.product.stock || 0) <= 0) {
    ElMessage.warning("商品库存不足");
    return;
  }
  addToCart(props.product, 1);
  ElMessage.success("已加入购物车");
}

function formatPrice(value) {
  const num = Number(value || 0);
  return num.toFixed(2);
}
</script>

<style scoped>
.product-card {
  background: var(--surface);
  border: 1px solid var(--line-soft);
  border-radius: 18px;
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  min-width: 0;
}

.product-card:hover {
  transform: translateY(-3px);
  border-color: #d9c989;
  box-shadow: var(--shadow-float);
}

.cover-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  border: 0;
  padding: 0;
  background: #f1efe6;
  display: block;
  overflow: hidden;
  cursor: pointer;
}

.cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.22s ease;
}

.product-card:hover .cover {
  transform: scale(1.035);
}

.badge,
.stock-badge {
  position: absolute;
  top: 10px;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 900;
  line-height: 1;
}

.badge {
  left: 10px;
  background: var(--brand-accent);
  color: var(--brand-primary);
}

.secondhand .badge {
  background: #ffffff;
}

.stock-badge {
  right: 10px;
  background: var(--brand-warm);
  color: #ffffff;
}

.content {
  padding: 13px;
}

.title-button {
  width: 100%;
  min-height: 44px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  border: 0;
  background: transparent;
  padding: 0;
  text-align: left;
  color: var(--text-main);
  font-size: 16px;
  font-weight: 800;
  line-height: 1.38;
  cursor: pointer;
}

.title-button:hover {
  color: #000000;
}

.desc {
  margin: 8px 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.5;
  min-height: 38px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.seller-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: var(--text-muted);
  font-size: 12px;
  min-width: 0;
}

.seller-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 10px;
}

.price {
  display: block;
  color: var(--brand-warm);
  font-size: 22px;
  line-height: 1;
  letter-spacing: 0;
}

.origin {
  display: block;
  margin-top: 5px;
  color: var(--text-muted);
  font-size: 12px;
  text-decoration: line-through;
}

.actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

@media (max-width: 680px) {
  .content {
    padding: 10px;
  }

  .title-button {
    font-size: 14px;
  }

  .desc {
    min-height: 0;
  }

  .meta {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
