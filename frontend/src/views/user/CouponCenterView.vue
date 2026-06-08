<template>
  <section class="coupon-page">
    <div class="coupon-hero">
      <div>
        <span class="eyebrow">{{ pageCopy.eyebrow }}</span>
        <h1>{{ pageCopy.title }}</h1>
        <p>{{ pageCopy.desc }}</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab" class="coupon-tabs">
      <el-tab-pane :label="`未开始 ${notStartedCoupons.length}`" name="notStarted" />
      <el-tab-pane :label="`可领取 ${claimableCoupons.length}`" name="claimable" />
      <el-tab-pane :label="`待使用 ${unusedCoupons.length}`" name="unused" />
      <el-tab-pane :label="`已使用 ${usedCoupons.length}`" name="used" />
    </el-tabs>

    <el-skeleton v-if="loading" :rows="6" animated class="page-card" />

    <div v-else-if="visibleCoupons.length" class="coupon-grid">
      <article
        v-for="coupon in visibleCoupons"
        :key="`${activeTab}-${coupon.id}`"
        class="coupon-card"
        :class="{
          'coupon-card--disabled': activeTab === 'notStarted' || activeTab === 'used',
          'coupon-card--mine': activeTab === 'unused'
        }"
      >
        <div class="coupon-value">
          <strong>{{ couponValue(coupon) }}</strong>
          <span>{{ coupon.typeName || "优惠券" }}</span>
        </div>

        <div class="coupon-info">
          <div class="coupon-title-row">
            <h3>{{ coupon.name }}</h3>
            <el-tag size="small" :type="isPlatformCoupon(coupon) ? 'primary' : 'success'">
              {{ isPlatformCoupon(coupon) ? "平台券" : "店铺券" }}
            </el-tag>
          </div>
          <p>{{ coupon.sellerName || coupon.issuerTypeName || pageCopy.title }}</p>
          <small>{{ thresholdText(coupon) }}</small>
          <small>{{ formatTime(coupon.startTime) }} - {{ formatTime(coupon.endTime) }}</small>
          <small v-if="activeTab === 'notStarted'" class="countdown">距开抢 {{ countdownTo(coupon.grabStartTime) }}</small>
          <small v-else-if="activeTab === 'unused'" class="countdown">距过期 {{ countdownTo(coupon.endTime) }}</small>
        </div>

        <div class="coupon-action">
          <span v-if="activeTab === 'notStarted'">剩余 {{ coupon.remainCount ?? 0 }} 张</span>
          <span v-else-if="activeTab === 'claimable'">剩余 {{ coupon.remainCount ?? 0 }} 张</span>
          <span v-else>{{ coupon.myStatusName || tabStatusText }}</span>

          <el-button
            v-if="activeTab === 'claimable'"
            type="primary"
            :disabled="coupon.claimed"
            :loading="claimingId === coupon.id"
            @click="claimCoupon(coupon)"
          >
            {{ coupon.claimed ? "已领取" : "立即领取" }}
          </el-button>
          <el-button v-else-if="activeTab === 'notStarted'" disabled>即将开抢</el-button>
          <el-button v-else-if="activeTab === 'unused'" type="primary" @click="goShopping">去使用</el-button>
          <el-button v-else disabled>已使用</el-button>
        </div>
      </article>
    </div>

    <el-empty v-else :description="emptyText" />
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { claimCouponApi, listCouponCenterApi, listMyCouponsApi } from "@/api/coupon";

const route = useRoute();
const router = useRouter();
const activeTab = ref("claimable");
const loading = ref(false);
const claimingId = ref(null);
const centerCoupons = ref([]);
const myCoupons = ref([]);
const now = ref(Date.now());

let timer = 0;

const couponScope = computed(() => {
  const scope = String(route.meta?.couponScope || "").toUpperCase();
  return scope === "SECONDHAND" ? "SECONDHAND" : "NEW";
});

const pageCopy = computed(() => {
  if (couponScope.value === "SECONDHAND") {
    return {
      eyebrow: "Secondhand Coupons",
      title: "二手领券中心",
      desc: "二手商城的交易优惠集中在这里，先领券再下单。"
    };
  }
  return {
    eyebrow: "New Goods Coupons",
    title: "新品领券中心",
    desc: "新品商城专属优惠券，先领券再下单更清楚。"
  };
});

const notStartedCoupons = computed(() => centerCoupons.value.filter((coupon) => Number(coupon.status) === 2));
const claimedCouponIds = computed(() => new Set(myCoupons.value.map((coupon) => Number(coupon.id))));
const claimableCoupons = computed(() => centerCoupons.value.filter((coupon) => {
  if (Number(coupon.status) !== 1) return false;
  return !coupon.claimed && !claimedCouponIds.value.has(Number(coupon.id));
}));
const unusedCoupons = computed(() => myCoupons.value.filter((coupon) => Number(coupon.myStatus) === 1));
const usedCoupons = computed(() => myCoupons.value.filter((coupon) => Number(coupon.myStatus) === 2));

const visibleCoupons = computed(() => {
  if (activeTab.value === "notStarted") return notStartedCoupons.value;
  if (activeTab.value === "unused") return unusedCoupons.value;
  if (activeTab.value === "used") return usedCoupons.value;
  return claimableCoupons.value;
});

const tabStatusText = computed(() => {
  if (activeTab.value === "unused") return "待使用";
  if (activeTab.value === "used") return "已使用";
  return "";
});

const emptyText = computed(() => ({
  notStarted: "暂无即将开抢的优惠券",
  claimable: "暂无可领取优惠券",
  unused: "暂无待使用优惠券",
  used: "暂无已使用优惠券"
}[activeTab.value] || "暂无优惠券"));

onMounted(() => {
  loadAll();
  timer = window.setInterval(() => {
    now.value = Date.now();
  }, 1000);
});

onBeforeUnmount(() => {
  window.clearInterval(timer);
});

watch(couponScope, () => {
  activeTab.value = "claimable";
  loadAll();
});

async function loadAll() {
  loading.value = true;
  try {
    const params = { page: 1, pageSize: 100, mallType: couponScope.value };
    const [center, mine] = await Promise.all([
      listCouponCenterApi(params),
      listMyCouponsApi(params)
    ]);
    centerCoupons.value = center.data?.records || [];
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
    activeTab.value = "unused";
    await loadAll();
  } finally {
    claimingId.value = null;
  }
}

function goShopping() {
  router.push(couponScope.value === "SECONDHAND" ? "/secondhand" : "/product");
}

function isPlatformCoupon(coupon) {
  return Number(coupon?.scopeType) === 2 || Number(coupon?.voucherType) === 2 || Number(coupon?.issuerType) === 2;
}

function couponValue(coupon) {
  if (Number(coupon.type) === 2) {
    return `${(Number(coupon.discountRate || 1) * 10).toFixed(1)}折`;
  }
  return `¥${Number(coupon.discountAmount || 0).toFixed(0)}`;
}

function thresholdText(coupon) {
  const min = Number(coupon.minAmount || 0);
  return min > 0 ? `满 ¥${min.toFixed(2)} 可用` : "无门槛可用";
}

function formatTime(value) {
  if (!value) return "长期有效";
  return String(value).replace("T", " ").slice(0, 16);
}

function countdownTo(value) {
  if (!value) return "--";
  const target = new Date(value).getTime();
  const diff = Math.max(0, target - now.value);
  if (diff <= 0) return "00:00:00";
  const totalSeconds = Math.floor(diff / 1000);
  const days = Math.floor(totalSeconds / 86400);
  const hours = Math.floor((totalSeconds % 86400) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const time = `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
  return days > 0 ? `${days}天 ${time}` : time;
}

function pad(value) {
  return String(value).padStart(2, "0");
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
  min-height: 158px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: var(--shadow-soft);
  display: grid;
  grid-template-columns: 128px minmax(0, 1fr) auto;
  overflow: hidden;
}

.coupon-card--mine .coupon-value {
  background: linear-gradient(135deg, #3f8cff 0%, #5fe6bd 100%);
}

.coupon-card--disabled {
  opacity: 0.78;
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

.coupon-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
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

.coupon-info .countdown {
  color: #f97316;
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
