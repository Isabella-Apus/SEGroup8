<template>
  <section class="coupon-page">
    <div class="coupon-hero">
      <div>
        <span class="eyebrow">{{ pageCopy.eyebrow }}</span>
        <h1>{{ pageCopy.title }}</h1>
        <p>{{ pageCopy.desc }}</p>
      </div>
      <el-button type="primary" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="coupon-tabs">
      <el-tab-pane label="可领取" name="available" />
      <el-tab-pane label="我的券" name="mine" />
    </el-tabs>

    <el-skeleton v-if="loading" :rows="6" animated class="page-card" />

    <div v-else-if="visibleCoupons.length" class="coupon-grid">
      <article v-for="coupon in visibleCoupons" :key="coupon.id" class="coupon-card">
        <div class="coupon-value">
          <strong>{{ couponValue(coupon) }}</strong>
          <span>{{ coupon.typeName || "优惠券" }}</span>
        </div>
        <div class="coupon-info">
          <h3>{{ coupon.name }}</h3>
          <p>{{ coupon.sellerName }}</p>
          <small>{{ coupon.mallTypeName || pageCopy.title }}</small>
          <small>满 ¥{{ Number(coupon.minAmount || 0).toFixed(0) }} 可用</small>
          <small>{{ formatDate(coupon.endTime) }} 到期</small>
        </div>
        <div class="coupon-action">
          <span>剩余 {{ coupon.remainCount ?? 0 }} 张</span>
          <el-button
            type="primary"
            :disabled="coupon.claimed || activeTab === 'mine'"
            :loading="claimingId === coupon.id"
            @click="claimCoupon(coupon)"
          >
            {{ coupon.claimed ? "已领取" : "立即领取" }}
          </el-button>
        </div>
      </article>
    </div>

    <el-empty v-else :description="activeTab === 'mine' ? '还没有领取优惠券' : '暂无可领取优惠券'" />
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { claimCouponApi, listCouponCenterApi, listMyCouponsApi } from "@/api/coupon";

const route = useRoute();
const activeTab = ref("available");
const loading = ref(false);
const claimingId = ref(null);
const availableCoupons = ref([]);
const myCoupons = ref([]);

const visibleCoupons = computed(() => (activeTab.value === "mine" ? myCoupons.value : availableCoupons.value));
const couponScope = computed(() => {
  const scope = String(route.meta?.couponScope || "").toUpperCase();
  return scope === "SECONDHAND" ? "SECONDHAND" : "NEW";
});
const pageCopy = computed(() => {
  if (couponScope.value === "SECONDHAND") {
    return {
      eyebrow: "Secondhand Coupons",
      title: "二手领券中心",
      desc: "二手商城的交易优惠集中在这里，适合淘闲置前先看一眼。",
    };
  }
  return {
    eyebrow: "New Goods Coupons",
    title: "新品领券中心",
    desc: "新品商城专属优惠券，先领券再下单更清楚。",
  };
});

onMounted(loadAll);

watch(activeTab, () => {
  loadAll();
});

watch(couponScope, () => {
  activeTab.value = "available";
  loadAll();
});

async function loadAll() {
  loading.value = true;
  try {
    const params = { pageNum: 1, pageSize: 50, mallType: couponScope.value };
    const [available, mine] = await Promise.all([
      listCouponCenterApi(params),
      listMyCouponsApi(params),
    ]);
    availableCoupons.value = available.data?.records || [];
    myCoupons.value = mine.data?.records || [];
  } finally {
    loading.value = false;
  }
}

async function claimCoupon(coupon) {
  claimingId.value = coupon.id;
  try {
    await claimCouponApi(coupon.id);
    ElMessage.success("领取成功");
    await loadAll();
  } finally {
    claimingId.value = null;
  }
}

function couponValue(coupon) {
  if (Number(coupon.type) === 2) {
    return `${Number(coupon.discountRate || 1) * 10}折`;
  }
  return `¥${Number(coupon.discountAmount || 0).toFixed(0)}`;
}

function formatDate(value) {
  if (!value) {
    return "长期";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "长期";
  }
  return date.toLocaleDateString("zh-CN");
}
</script>

<style scoped>
.coupon-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.coupon-hero {
  min-height: 150px;
  border: 1px solid rgba(137, 199, 255, 0.36);
  border-radius: 8px;
  padding: 22px;
  background: linear-gradient(135deg, #e9fff8 0%, #eaf4ff 54%, #fff7fb 100%);
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  box-shadow: var(--shadow-soft);
}

.eyebrow {
  color: var(--brand-primary);
  font-weight: 900;
}

.coupon-hero h1 {
  margin: 10px 0 8px;
  font-size: 34px;
}

.coupon-hero p {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 700;
}

.coupon-tabs {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 0 14px;
}

.coupon-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.coupon-card {
  min-height: 150px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: var(--shadow-soft);
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr) auto;
  overflow: hidden;
}

.coupon-value {
  background: linear-gradient(135deg, #5fe6bd 0%, #89c7ff 100%);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.coupon-value strong {
  font-size: 32px;
  line-height: 1;
}

.coupon-value span {
  font-weight: 900;
}

.coupon-info {
  min-width: 0;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.coupon-info h3 {
  margin: 0;
  font-size: 18px;
}

.coupon-info p,
.coupon-info small {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 700;
}

.coupon-action {
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
  gap: 12px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 900px) {
  .coupon-grid,
  .coupon-card {
    grid-template-columns: 1fr;
  }

  .coupon-action {
    align-items: stretch;
  }
}
</style>
