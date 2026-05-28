<template>
  <section class="rating-summary" :class="{ 'rating-summary--compact': isSecondhand }">
    <div v-if="!isSecondhand" class="score-main">
      <span>{{ title }}</span>
      <strong>{{ mainScore }}</strong>
      <em>{{ mainLevel }}</em>
    </div>
    <div class="score-grid">
      <div v-for="metric in metrics" :key="metric.label">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
        <em v-if="metric.note">{{ metric.note }}</em>
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
const title = computed(() => "综合评分");
const mainScore = computed(() => {
  if (isSecondhand.value) {
    return formatNumber(props.rating?.shSellerRatingScore);
  }
  return formatNumber(props.rating?.overallScore);
});
const mainLevel = computed(() => {
  if (isSecondhand.value) {
    return props.rating?.shSellerRatingLevel || "暂无等级";
  }
  return props.rating?.overallLevel || "暂无等级";
});
const metrics = computed(() => {
  if (isSecondhand.value) {
    return [
      {
        label: "二手卖家信用",
        value: formatNumber(props.rating?.shSellerScore),
        note: props.rating?.shSellerLevel,
      },
      {
        label: "二手买家信用",
        value: formatNumber(props.rating?.buyerScore),
        note: props.rating?.buyerLevel,
      },
    ];
  }
  return [
    {
      label: "卖家信用分",
      value: formatNumber(props.rating?.shopScore),
      note: props.rating?.shopLevel,
    },
    {
      label: "店铺售出",
      value: formatNumber(props.rating?.shopSoldCount),
    },
    {
      label: "好评率",
      value: formatRate(props.rating?.shopGoodRate),
    },
  ];
});

function formatNumber(value) {
  return value === null || value === undefined || value === "" ? "暂无" : value;
}

function formatRate(value) {
  return value === null || value === undefined || value === "" ? "暂无" : `${Number(value).toFixed(1)}%`;
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

.rating-summary--compact {
  grid-template-columns: 1fr;
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
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
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

.score-grid em {
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 12px;
  font-style: normal;
  font-weight: 800;
}

@media (max-width: 760px) {
  .rating-summary,
  .score-grid {
    grid-template-columns: 1fr;
  }
}
</style>
