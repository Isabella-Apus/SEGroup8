<template>
  <article class="product-card" :class="{ secondhand: isSecondhand }">
    <button class="cover-wrap" type="button" @click="goDetail">
      <img class="cover" :src="coverUrl" :alt="product.name" loading="lazy" />
      <span class="badge">{{ badgeText }}</span>
      <span v-if="isLowStock" class="stock-badge">库存紧张</span>
      <span class="quick-view">看详情</span>
    </button>

    <div class="content">
      <button class="title-button" type="button" @click="goDetail">
        <span v-if="isSecondhand" class="title-tag">闲置</span>
        <span v-else class="title-tag official">官方</span>
        {{ product.name }}
      </button>

      <div class="coupon-row">
        <span v-for="tag in productTags" :key="tag">{{ tag }}</span>
      </div>

      <div class="meta">
        <div>
          <strong class="price"><small>¥</small>{{ priceInteger }}<em>.{{ priceDecimal }}</em></strong>
          <span v-if="isSecondhand" class="origin">¥{{ formatPrice(product.originPrice || product.price) }}</span>
        </div>
        <span class="sales-text">{{ salesText }}</span>
      </div>

      <div class="seller-row">
        <span class="seller-avatar">{{ sellerInitial }}</span>
        <span class="seller-name">{{ sellerText }}</span>
        <span class="sub-text">{{ subText }}</span>
      </div>

      <div class="actions">
        <el-button v-if="!isSecondhand" size="small" @click.stop="handleAddCart">加购</el-button>
        <el-button size="small" type="primary" @click.stop="goDetail">
          {{ isSecondhand ? "聊一聊" : "去看看" }}
        </el-button>
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
    return "个人发布";
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

const formattedMainPrice = computed(() => formatPrice(mainPrice.value));
const priceInteger = computed(() => formattedMainPrice.value.split(".")[0]);
const priceDecimal = computed(() => formattedMainPrice.value.split(".")[1] || "00");

const productTags = computed(() => {
  if (isSecondhand.value) {
    return uniqueTags([
      props.product.conditionLevel || props.product.condition || "成色良好",
      hasMultipleImages(props.product) ? "多图展示" : "",
      resolveCategoryTag(props.product),
      "可沟通",
    ]).slice(0, 3);
  }

  const tags = [
    resolveStockTag(props.product.stock),
    hasMultipleImages(props.product) ? "多图展示" : "",
    resolveCategoryTag(props.product),
  ];
  const supplements = rotateBySeed(["支持下单", "订单追踪", "价格清晰", "现货在售"], props.product.id);

  return uniqueTags([...tags, ...supplements]).slice(0, 3);
});

function resolveStockTag(stockValue) {
  const stock = Number(stockValue ?? 0);
  if (stock <= 0) {
    return "暂时缺货";
  }
  if (stock <= 10) {
    return "少量库存";
  }
  if (stock > 50) {
    return "库存充足";
  }
  return "库存可查";
}

function hasMultipleImages(product) {
  const images = normalizeImages(product?.images);
  const cover = product?.cover ? [product.cover] : [];
  return uniqueTags([...images, ...cover]).length > 1;
}

function normalizeImages(images) {
  if (Array.isArray(images)) {
    return images.filter(Boolean);
  }
  if (typeof images !== "string" || !images.trim()) {
    return [];
  }
  try {
    const parsed = JSON.parse(images);
    return Array.isArray(parsed) ? parsed.filter(Boolean) : [];
  } catch (error) {
    return images.split(",").map((item) => item.trim()).filter(Boolean);
  }
}

function resolveCategoryTag(product) {
  const categoryText = [
    product?.categoryName,
    product?.category,
    product?.subCategoryName,
    product?.name,
  ]
    .filter(Boolean)
    .join(" ");

  const categoryTags = [
    { keywords: ["电子数码", "数码闲置", "数码"], tag: "数码好物" },
    { keywords: ["学习办公", "办公", "学习"], tag: "学习办公" },
    { keywords: ["生活百货", "宿舍生活", "百货", "生活"], tag: "生活百货" },
    { keywords: ["服装鞋包", "服饰鞋包", "服装", "服饰", "鞋包"], tag: "服装鞋包" },
    { keywords: ["运动户外", "运动器材", "运动", "户外"], tag: "运动户外" },
    { keywords: ["教材书籍", "教材", "书籍"], tag: "教材资料" },
  ];

  return categoryTags.find((item) => item.keywords.some((keyword) => categoryText.includes(keyword)))?.tag || "";
}

function rotateBySeed(list, seedValue) {
  const seed = Number(seedValue || 0);
  const start = Math.abs(seed) % list.length;
  return list.slice(start).concat(list.slice(0, start));
}

function uniqueTags(tags) {
  return [...new Set(tags.filter(Boolean))];
}

const salesText = computed(() => {
  const seed = Number(props.product.id || 1);
  return isSecondhand.value ? `${20 + (seed % 76)} 人想要` : `${100 + (seed * 17) % 900} 热度`;
});

const sellerInitial = computed(() => sellerText.value.slice(0, 1).toUpperCase());

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
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  min-width: 0;
}

.product-card:hover {
  transform: translateY(-2px);
  border-color: rgba(60, 146, 255, 0.45);
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
.stock-badge,
.quick-view {
  position: absolute;
  border-radius: 999px;
  padding: 5px 9px;
  font-size: 12px;
  font-weight: 900;
  line-height: 1;
}

.badge {
  top: 8px;
  left: 8px;
  background: rgba(234, 244, 255, 0.9);
  color: var(--brand-primary);
  backdrop-filter: blur(8px);
}

.secondhand .badge {
  background: rgba(233, 255, 248, 0.92);
  color: var(--brand-accent-strong);
}

.stock-badge {
  top: 8px;
  right: 8px;
  background: var(--brand-warm);
  color: var(--text-main);
}

.quick-view {
  left: 8px;
  bottom: 8px;
  background: var(--brand-gradient-strong);
  color: #ffffff;
  opacity: 0;
  transform: translateY(4px);
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.product-card:hover .quick-view {
  opacity: 1;
  transform: translateY(0);
}

.content {
  padding: 10px;
}

.title-button {
  width: 100%;
  min-height: 42px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  border: 0;
  background: transparent;
  padding: 0;
  text-align: left;
  color: var(--text-main);
  font-size: 14px;
  font-weight: 800;
  line-height: 1.45;
  cursor: pointer;
}

.title-button:hover {
  color: var(--brand-primary);
}

.title-tag {
  display: inline-flex;
  vertical-align: 1px;
  margin-right: 4px;
  border-radius: 4px;
  background: var(--brand-accent);
  color: #ffffff;
  padding: 1px 4px;
  font-size: 11px;
  line-height: 1.35;
  font-weight: 900;
}

.title-tag.official {
  background: var(--brand-primary);
}

.coupon-row {
  min-height: 24px;
  margin-top: 7px;
  display: flex;
  gap: 5px;
  overflow: hidden;
}

.coupon-row span {
  border: 1px solid rgba(60, 146, 255, 0.24);
  border-radius: 4px;
  color: var(--brand-primary);
  background: var(--brand-primary-weak);
  padding: 2px 5px;
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}

.seller-row {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 12px;
  min-width: 0;
}

.seller-avatar {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: var(--brand-gradient);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  font-size: 10px;
  font-weight: 900;
}

.seller-name,
.sub-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seller-name {
  min-width: 0;
  flex: 1;
}

.sub-text {
  flex: 0 0 auto;
}

.meta {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
}

.price {
  display: inline-flex;
  align-items: baseline;
  color: var(--brand-primary);
  font-size: 24px;
  line-height: 1;
  letter-spacing: 0;
}

.price small,
.price em {
  font-style: normal;
  font-size: 13px;
  font-weight: 900;
}

.origin {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
  text-decoration: line-through;
}

.sales-text {
  color: var(--text-muted);
  font-size: 12px;
  white-space: nowrap;
}

.actions {
  margin-top: 10px;
  display: flex;
  gap: 6px;
}

.actions :deep(.el-button) {
  flex: 1;
  min-width: 0;
  border-radius: 6px;
  font-weight: 900;
}

@media (max-width: 680px) {
  .content {
    padding: 10px;
  }

  .title-button {
    font-size: 14px;
  }

  .actions {
    display: none;
  }
}
</style>
