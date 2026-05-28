<template>
  <section class="rating-summary">
    <div class="score-main">
      <span>{{ title }}</span>
      <strong>{{ mainScore }}</strong>
      <em>{{ mainLevel }}</em>
    </div>
    <div class="score-grid">
      <div>
        <span>{{ scoreLabel }}</span>
        <strong>{{ typedScore }}</strong>
      </div>
      <div>
        <span>{{ soldLabel }}</span>
        <strong>{{ soldCount }}</strong>
      </div>
      <div>
        <span>好评率</span>
        <strong>{{ goodRate }}</strong>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from "vue";

const props = defineProps({
  rating: {
    type: Object,
    default: () => ({}),
  },
  type: {
    type: String,
    default: "shop",
  },
});

const isSecondhand = computed(() => props.type === "secondhand");
const title = computed(() => (isSecondhand.value ? "二手卖家评分" : "店铺卖家评分"));
const scoreLabel = computed(() => (isSecondhand.value ? "二手信用分" : "店铺健康分"));
const soldLabel = computed(() => (isSecondhand.value ? "二手售出" : "店铺售出"));
const mainScore = computed(() => formatNumber(props.rating?.overallScore ?? typedScore.value));
const mainLevel = computed(() => props.rating?.overallLevel || typedLevel.value || "暂无等级");
const typedScore = computed(() => formatNumber(isSecondhand.value ? props.rating?.shSellerScore : props.rating?.shopScore));
const typedLevel = computed(() => isSecondhand.value ? props.rating?.shSellerLevel : props.rating?.shopLevel);
const soldCount = computed(() => formatNumber(isSecondhand.value ? props.rating?.shSellerSoldCount : props.rating?.shopSoldCount));
const goodRate = computed(() => {
  const value = isSecondhand.value ? props.rating?.shSellerGoodRate : props.rating?.shopGoodRate;
  return value === null || value === undefined || value === "" ? "暂无" : `${Number(value).toFixed(1)}%`;
});

function formatNumber(value) {
  return value === null || value === undefined || value === "" ? "暂无" : value;
}
</script>

<style scoped>
.rating-summary {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 14px;
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 14px;
  box-shadow: var(--shadow-soft);
}

.score-main {
  min-height: 118px;
  border-radius: 8px;
  background: linear-gradient(135deg, #eaf4ff 0%, #e9fff8 100%);
  border: 1px solid rgba(60, 146, 255, 0.18);
  padding: 14px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.score-main span,
.score-grid span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 900;
}

.score-main strong {
  margin-top: 5px;
  color: var(--brand-primary);
  font-size: 42px;
  line-height: 1;
}

.score-main em {
  margin-top: 6px;
  color: var(--brand-primary-dark);
  font-style: normal;
  font-weight: 900;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.score-grid div {
  min-height: 118px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
  padding: 14px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.score-grid strong {
  margin-top: 8px;
  color: var(--text-main);
  font-size: 26px;
  line-height: 1.1;
}

@media (max-width: 760px) {
  .rating-summary,
  .score-grid {
    grid-template-columns: 1fr;
  }
}
</style>
