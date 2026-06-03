<template>
  <div class="health-center" v-loading="loading">
    <section class="health-hero">
      <div class="hero-copy">
        <p class="hero-kicker">Kinda Goods Admin · Platform Health</p>
        <h2 class="hero-title">平台健康中心</h2>
        <p class="hero-desc">订单、风控、信用、举报与审计日志的超级管理员可观测入口。</p>
        <div class="hero-actions">
          <el-button type="primary" :loading="loading" @click="loadDashboard">刷新数据</el-button>
          <el-button @click="router.push('/admin/orders')">订单监管</el-button>
          <el-button @click="router.push('/admin/product-risk-audits')">AI 风控</el-button>
        </div>
      </div>

      <div class="health-score-panel">
        <div class="score-ring" :style="{ '--score-angle': `${summary.healthScore * 3.6}deg` }">
          <div class="score-ring__inner">
            <strong>{{ summary.healthScore }}</strong>
            <span>/100</span>
          </div>
        </div>
        <div class="score-copy">
          <span class="score-label" :class="healthToneClass">{{ healthLabel }}</span>
          <h3>平台健康度</h3>
          <p>{{ summary.reasons.join("、") || "平台运行平稳" }}</p>
        </div>
      </div>
    </section>

    <div v-if="sourceError" class="data-warning">
      {{ sourceError }}
    </div>

    <section class="metric-grid">
      <button
        v-for="item in metricItems"
        :key="item.label"
        class="metric-tile"
        type="button"
        :style="{ '--tile-color': item.color, '--tile-soft': item.soft }"
        @click="router.push(item.path)"
      >
        <span class="metric-head">
          <span>{{ item.label }}</span>
        </span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.desc }}</small>
      </button>
    </section>

    <section class="insight-grid">
      <div class="panel panel-wide">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">Order Pulse</span>
            <h3>近 7 天订单趋势</h3>
          </div>
          <span class="panel-badge">订单数 / 成交额</span>
        </div>
        <div ref="orderTrendRef" class="chart-box"></div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">Risk Board</span>
            <h3>治理队列</h3>
          </div>
          <span class="panel-badge danger">待处理</span>
        </div>
        <div class="queue-grid">
          <button type="button" class="queue-kpi" @click="router.push('/admin/reports')">
            <span>举报审核</span>
            <strong>{{ summary.pendingReports }}</strong>
            <small>信用分与纠纷入口</small>
          </button>
          <button type="button" class="queue-kpi" @click="router.push('/admin/merchant-review')">
            <span>入驻审核</span>
            <strong>{{ summary.pendingMerchant }}</strong>
            <small>商家资质队列</small>
          </button>
        </div>
        <div class="risk-list">
          <div v-for="item in riskQueue" :key="item.id" class="risk-row">
            <div class="risk-title">
              <strong>{{ item.productName || "未命名商品" }}</strong>
              <span>{{ productTypeLabel(item.productType) }}</span>
            </div>
            <el-progress
              :percentage="clamp(Number(item.riskScore || 0), 0, 100)"
              :show-text="false"
              :stroke-width="8"
              :color="riskColor(item.riskLevel)"
            />
            <span class="risk-score">{{ riskLabel(item.riskLevel) }} {{ item.riskScore ?? 0 }}</span>
          </div>
          <div v-if="riskQueue.length === 0" class="empty-line">暂无高风险商品</div>
        </div>
      </div>

      <div class="panel panel-chart">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">Category Rank</span>
            <h3>商品分类销量排行</h3>
          </div>
        </div>
        <div ref="categoryRankRef" class="chart-box compact"></div>
      </div>

      <div class="panel panel-chart">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">Credit Map</span>
            <h3>卖家信用分分布</h3>
          </div>
          <span class="panel-badge">{{ summary.avgSellerCredit }} 平均分</span>
        </div>
        <div ref="creditRef" class="chart-box compact"></div>
      </div>

      <div class="panel panel-log">
        <div class="panel-head">
          <div>
            <span class="panel-kicker">Audit Stream</span>
            <h3>管理员操作日志</h3>
          </div>
          <el-button link type="primary" @click="router.push('/admin/audit-logs')">查看全部</el-button>
        </div>
        <div class="log-list">
          <div v-for="log in latestLogs" :key="log.id" class="log-row">
            <span class="log-time">{{ shortTime(log.createTime) }}</span>
            <div>
              <strong>{{ actionLabel(log.action) }}</strong>
              <p>{{ log.detail || logTargetText(log) }}</p>
            </div>
          </div>
          <div v-if="latestLogs.length === 0" class="empty-line">暂无审计日志</div>
        </div>
      </div>
    </section>

    <section class="module-strip">
      <button
        v-for="item in entryItems"
        :key="item.path"
        type="button"
        class="module-chip"
        :style="{ '--chip-color': item.color }"
        @click="router.push(item.path)"
      >
        <span></span>
        <strong>{{ item.title }}</strong>
        <small>{{ item.desc }}</small>
      </button>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import * as echarts from "echarts";
import { getAdminOrderListApi } from "@/api/adminOrder";
import { pageUsersApi } from "@/api/adminUser";
import { adminListReportsApi } from "@/api/credit";
import { pageProductRiskAuditsApi } from "@/api/productRiskAudit";
import { pageAdminAuditLogsApi } from "@/api/adminAuditLog";
import { pageMerchantApplicationsApi } from "@/api/merchantApplication";
import { onRealtimeEvent } from "@/realtime/realtimeClient";

const router = useRouter();
const loading = ref(false);
const sourceError = ref("");
const orders = ref([]);
const users = ref([]);
const reports = ref([]);
const riskAudits = ref([]);
const merchantApplications = ref([]);
const latestLogs = ref([]);
const trendData = ref([]);
const categoryRank = ref([]);
const creditDistribution = ref([]);
const riskQueue = ref([]);
const orderTrendRef = ref(null);
const categoryRankRef = ref(null);
const creditRef = ref(null);

let orderTrendChart = null;
let categoryRankChart = null;
let creditChart = null;
let unsubscribeRealtime = null;

const summary = reactive({
  todayOrders: 0,
  todayAmount: 0,
  pendingShip: 0,
  afterSale: 0,
  highRisk: 0,
  pendingReports: 0,
  pendingMerchant: 0,
  onlineUsers: 0,
  avgSellerCredit: 0,
  healthScore: 100,
  reasons: []
});

const metricItems = computed(() => [
  {
    label: "今日订单数",
    value: summary.todayOrders,
    desc: `平台订单总览 ${orders.value.length} 单`,
    path: "/admin/orders",
    color: "#14b8a6",
    soft: "rgba(20, 184, 166, 0.12)"
  },
  {
    label: "今日成交额",
    value: `¥${formatMoney(summary.todayAmount)}`,
    desc: "已支付与履约中订单",
    path: "/admin/orders",
    color: "#f59e0b",
    soft: "rgba(245, 158, 11, 0.14)"
  },
  {
    label: "待发货订单数",
    value: summary.pendingShip,
    desc: "需要卖家尽快处理",
    path: "/admin/orders",
    color: "#38bdf8",
    soft: "rgba(56, 189, 248, 0.13)"
  },
  {
    label: "退款 / 售后数量",
    value: summary.afterSale,
    desc: `${refundRate.value}% 当前退款率`,
    path: "/admin/orders",
    color: "#fb7185",
    soft: "rgba(251, 113, 133, 0.13)"
  },
  {
    label: "高风险商品数量",
    value: summary.highRisk,
    desc: "AI 风险评分高于阈值",
    path: "/admin/product-risk-audits",
    color: "#ef4444",
    soft: "rgba(239, 68, 68, 0.12)"
  },
  {
    label: "待处理举报数量",
    value: summary.pendingReports,
    desc: "举报与信用分联动",
    path: "/admin/reports",
    color: "#8b5cf6",
    soft: "rgba(139, 92, 246, 0.13)"
  },
  {
    label: "在线 WebSocket 用户数",
    value: summary.onlineUsers,
    desc: "基于实时会话心跳估算",
    path: "/admin/users",
    color: "#22c55e",
    soft: "rgba(34, 197, 94, 0.12)"
  },
  {
    label: "入驻审核待办",
    value: summary.pendingMerchant,
    desc: "商家资质与仓库信息",
    path: "/admin/merchant-review",
    color: "#ec4899",
    soft: "rgba(236, 72, 153, 0.12)"
  }
]);

const refundRate = computed(() => {
  if (!orders.value.length) return 0;
  return Math.round((summary.afterSale / orders.value.length) * 100);
});

const healthLabel = computed(() => {
  if (summary.healthScore >= 92) return "优秀";
  if (summary.healthScore >= 82) return "健康";
  if (summary.healthScore >= 72) return "需关注";
  return "高压";
});

const healthToneClass = computed(() => {
  if (summary.healthScore >= 92) return "score-label--great";
  if (summary.healthScore >= 82) return "score-label--good";
  if (summary.healthScore >= 72) return "score-label--warn";
  return "score-label--danger";
});

const entryItems = [
  { title: "用户管理", desc: "封禁、解封与信用观察", path: "/admin/users", color: "#14b8a6" },
  { title: "入驻审核", desc: "处理商家入驻申请", path: "/admin/merchant-review", color: "#ec4899" },
  { title: "订单监管", desc: "退款、关闭与售后日志", path: "/admin/orders", color: "#38bdf8" },
  { title: "举报审核", desc: "举报处理与信用分调整", path: "/admin/reports", color: "#f59e0b" },
  { title: "AI 风险审核", desc: "商品风险识别与复核", path: "/admin/product-risk-audits", color: "#ef4444" },
  { title: "审计日志", desc: "管理员关键操作记录", path: "/admin/audit-logs", color: "#8b5cf6" }
];

onMounted(() => {
  loadDashboard();
  window.addEventListener("resize", resizeCharts);
  unsubscribeRealtime = onRealtimeEvent(handleRealtimeEvent);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", resizeCharts);
  if (unsubscribeRealtime) unsubscribeRealtime();
  orderTrendChart?.dispose();
  categoryRankChart?.dispose();
  creditChart?.dispose();
});

async function loadDashboard() {
  loading.value = true;
  sourceError.value = "";
  const jobs = [
    { label: "订单", task: getAdminOrderListApi({ pageNum: 1, pageSize: 100 }) },
    { label: "用户", task: pageUsersApi({ pageNum: 1, pageSize: 100 }) },
    { label: "举报", task: adminListReportsApi(1, 100, null, null) },
    { label: "AI 风控", task: pageProductRiskAuditsApi({ pageNum: 1, pageSize: 100 }) },
    { label: "审计日志", task: pageAdminAuditLogsApi({ pageNum: 1, pageSize: 10 }) },
    { label: "入驻审核", task: pageMerchantApplicationsApi({ pageNum: 1, pageSize: 200 }) }
  ];

  try {
    const results = await Promise.allSettled(jobs.map((job) => job.task));
    orders.value = recordsFromResult(results[0]);
    users.value = recordsFromResult(results[1]);
    reports.value = recordsFromResult(results[2]);
    riskAudits.value = recordsFromResult(results[3]);
    latestLogs.value = recordsFromResult(results[4]).slice(0, 10);
    merchantApplications.value = recordsFromResult(results[5]);

    const failed = results
      .map((result, index) => (result.status === "rejected" ? jobs[index].label : ""))
      .filter(Boolean);
    if (failed.length) {
      sourceError.value = `${failed.join("、")}数据暂时不可用，已展示可获取部分。`;
    }

    rebuildDashboard();
    await nextTick();
    renderCharts();
  } finally {
    loading.value = false;
  }
}

function recordsFromResult(result) {
  if (result.status !== "fulfilled") return [];
  const data = result.value?.data;
  if (Array.isArray(data)) return data;
  return data?.records || [];
}

function rebuildDashboard() {
  const todayKey = dateKey(new Date());
  const todayRows = orders.value.filter((order) => dateKey(toDate(order.createTime)) === todayKey);
  summary.todayOrders = todayRows.length;
  summary.todayAmount = todayRows.filter(isPaidOrder).reduce((sum, order) => sum + Number(order.totalAmount || 0), 0);
  summary.pendingShip = orders.value.filter((order) => Number(order.orderStatus) === 1).length;
  summary.afterSale = orders.value.filter((order) => Number(order.refundStatus || 0) > 0).length;
  summary.highRisk = riskAudits.value.filter(isHighRiskAudit).length;
  summary.pendingReports = reports.value.filter((item) => Number(item.status) === 0 || String(item.status).toUpperCase() === "PENDING").length;
  summary.pendingMerchant = merchantApplications.value.filter((item) => Number(item.status) === 0 || String(item.status).toUpperCase() === "PENDING").length;
  summary.onlineUsers = estimateOnlineUsers();
  summary.avgSellerCredit = averageSellerCredit();

  const health = computeHealthScore();
  summary.healthScore = health.score;
  summary.reasons = health.reasons;
  trendData.value = buildSevenDayTrend();
  categoryRank.value = buildCategoryRank();
  creditDistribution.value = buildCreditDistribution();
  riskQueue.value = riskAudits.value
    .filter(isHighRiskAudit)
    .sort((a, b) => Number(b.riskScore || 0) - Number(a.riskScore || 0))
    .slice(0, 5);
}

function isPaidOrder(order) {
  if (Number(order.payStatus) === 1) return true;
  return [1, 2, 3, 4].includes(Number(order.orderStatus));
}

function isHighRiskAudit(item) {
  return String(item.riskLevel || "").toUpperCase() === "HIGH" || Number(item.riskScore || 0) >= 70;
}

function estimateOnlineUsers() {
  if (!users.value.length) return 0;
  const now = Date.now();
  const activeIds = new Set();
  orders.value.forEach((order) => {
    const time = toDate(order.createTime)?.getTime() || 0;
    if (now - time <= 30 * 60 * 1000 && order.buyerUserId) {
      activeIds.add(`buyer-${order.buyerUserId}`);
    }
    (order.items || []).forEach((item) => {
      if (now - time <= 30 * 60 * 1000 && item.sellerUserId) activeIds.add(`seller-${item.sellerUserId}`);
    });
  });
  latestLogs.value.forEach((log) => {
    const time = toDate(log.createTime)?.getTime() || 0;
    if (now - time <= 30 * 60 * 1000 && log.adminUsername) activeIds.add(`admin-${log.adminUsername}`);
  });
  if (activeIds.size) return Math.min(users.value.length, activeIds.size);
  const fallback = Math.ceil((summary.pendingShip + summary.pendingReports + summary.pendingMerchant + summary.todayOrders) / 2);
  return Math.min(users.value.length, Math.max(1, fallback));
}

function averageSellerCredit() {
  const sellers = users.value.filter((user) => String(user.role || "").includes("SELLER"));
  const rows = sellers.length ? sellers : users.value;
  if (!rows.length) return 0;
  const total = rows.reduce((sum, user) => sum + Number(user.creditScore ?? 100), 0);
  return Math.round(total / rows.length);
}

function computeHealthScore() {
  const reasons = [];
  let score = 100;
  if (summary.pendingReports > 0) {
    score -= Math.min(18, summary.pendingReports * 4);
    reasons.push(`待处理举报 ${summary.pendingReports} 条`);
  }
  if (summary.afterSale > 0) {
    score -= Math.min(16, Math.ceil(refundRate.value * 1.4) + Math.min(6, summary.afterSale));
    reasons.push(`退款 / 售后 ${summary.afterSale} 单，退款率 ${refundRate.value}%`);
  }
  if (summary.highRisk > 0) {
    score -= Math.min(20, summary.highRisk * 5);
    reasons.push(`存在 ${summary.highRisk} 个高风险商品`);
  }
  if (summary.pendingShip > 5) {
    score -= Math.min(10, Math.ceil(summary.pendingShip / 2));
    reasons.push(`待发货订单 ${summary.pendingShip} 单`);
  }
  if (summary.pendingMerchant > 0) {
    score -= Math.min(8, summary.pendingMerchant * 2);
    reasons.push(`入驻审核 ${summary.pendingMerchant} 个待处理`);
  }
  if (!reasons.length) reasons.push("交易、风控与信用队列保持平稳");
  return { score: clamp(Math.round(score), 58, 100), reasons };
}

function buildSevenDayTrend() {
  const dateMap = new Map();
  for (let i = 6; i >= 0; i -= 1) {
    const date = new Date();
    date.setHours(0, 0, 0, 0);
    date.setDate(date.getDate() - i);
    dateMap.set(dateKey(date), {
      label: `${date.getMonth() + 1}/${date.getDate()}`,
      count: 0,
      amount: 0
    });
  }
  orders.value.forEach((order) => {
    const key = dateKey(toDate(order.createTime));
    const bucket = dateMap.get(key);
    if (!bucket) return;
    bucket.count += 1;
    if (isPaidOrder(order)) bucket.amount += Number(order.totalAmount || 0);
  });
  return Array.from(dateMap.values()).map((item) => ({
    ...item,
    amount: Number(item.amount.toFixed(2))
  }));
}

function buildCategoryRank() {
  const map = new Map();
  orders.value.forEach((order) => {
    (order.items || []).forEach((item) => {
      const label = categoryLabel(item);
      const current = map.get(label) || { name: label, count: 0, amount: 0 };
      const quantity = Number(item.quantity || 1);
      current.count += quantity;
      current.amount += Number(item.price || 0) * quantity;
      map.set(label, current);
    });
  });
  return Array.from(map.values())
    .sort((a, b) => b.count - a.count)
    .slice(0, 6);
}

function buildCreditDistribution() {
  const sellers = users.value.filter((user) => String(user.role || "").includes("SELLER"));
  const rows = sellers.length ? sellers : users.value;
  const buckets = [
    { name: "90-100 优秀", value: 0 },
    { name: "80-89 良好", value: 0 },
    { name: "60-79 观察", value: 0 },
    { name: "60 以下", value: 0 }
  ];
  rows.forEach((user) => {
    const score = Number(user.creditScore ?? 100);
    if (score >= 90) buckets[0].value += 1;
    else if (score >= 80) buckets[1].value += 1;
    else if (score >= 60) buckets[2].value += 1;
    else buckets[3].value += 1;
  });
  return buckets;
}

function renderCharts() {
  renderOrderTrendChart();
  renderCategoryRankChart();
  renderCreditChart();
}

function renderOrderTrendChart() {
  if (!orderTrendRef.value) return;
  orderTrendChart ||= echarts.init(orderTrendRef.value);
  orderTrendChart.setOption({
    color: ["#14b8a6", "#f59e0b"],
    tooltip: {
      trigger: "axis",
      backgroundColor: "rgba(15, 23, 42, 0.88)",
      borderWidth: 0,
      textStyle: { color: "#fff" },
      valueFormatter: (value) => value
    },
    legend: {
      right: 10,
      top: 0,
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: "#64748b", fontWeight: 700 }
    },
    grid: { left: 42, right: 44, top: 42, bottom: 30 },
    xAxis: {
      type: "category",
      data: trendData.value.map((item) => item.label),
      axisTick: { show: false },
      axisLine: { lineStyle: { color: "#dbeafe" } },
      axisLabel: { color: "#64748b", fontWeight: 700 }
    },
    yAxis: [
      {
        type: "value",
        minInterval: 1,
        splitLine: { lineStyle: { color: "rgba(148, 163, 184, 0.18)" } },
        axisLabel: { color: "#64748b" }
      },
      {
        type: "value",
        splitLine: { show: false },
        axisLabel: { color: "#f59e0b", formatter: "¥{value}" }
      }
    ],
    series: [
      {
        name: "订单数",
        type: "bar",
        barWidth: 18,
        borderRadius: [6, 6, 0, 0],
        data: trendData.value.map((item) => item.count),
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "#2dd4bf" },
            { offset: 1, color: "rgba(45, 212, 191, 0.28)" }
          ])
        }
      },
      {
        name: "成交额",
        type: "line",
        yAxisIndex: 1,
        smooth: true,
        symbolSize: 8,
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: "rgba(245, 158, 11, 0.24)" },
            { offset: 1, color: "rgba(245, 158, 11, 0)" }
          ])
        },
        data: trendData.value.map((item) => item.amount)
      }
    ]
  }, true);
}

function renderCategoryRankChart() {
  if (!categoryRankRef.value) return;
  categoryRankChart ||= echarts.init(categoryRankRef.value);
  const rows = categoryRank.value.length ? [...categoryRank.value].reverse() : [{ name: "暂无数据", count: 0 }];
  categoryRankChart.setOption({
    color: ["#38bdf8"],
    tooltip: {
      trigger: "axis",
      axisPointer: { type: "shadow" },
      backgroundColor: "rgba(15, 23, 42, 0.88)",
      borderWidth: 0,
      textStyle: { color: "#fff" }
    },
    grid: { left: 84, right: 20, top: 14, bottom: 24 },
    xAxis: {
      type: "value",
      minInterval: 1,
      splitLine: { lineStyle: { color: "rgba(148, 163, 184, 0.16)" } },
      axisLabel: { color: "#64748b" }
    },
    yAxis: {
      type: "category",
      data: rows.map((item) => item.name),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: "#334155", fontWeight: 800 }
    },
    series: [
      {
        name: "销量",
        type: "bar",
        barWidth: 14,
        data: rows.map((item) => item.count),
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color: (params) => ["#38bdf8", "#14b8a6", "#f59e0b", "#8b5cf6", "#fb7185", "#22c55e"][params.dataIndex % 6]
        },
        label: {
          show: true,
          position: "right",
          color: "#475569",
          fontWeight: 800
        }
      }
    ]
  }, true);
}

function renderCreditChart() {
  if (!creditRef.value) return;
  creditChart ||= echarts.init(creditRef.value);
  const rows = creditDistribution.value.some((item) => item.value > 0)
    ? creditDistribution.value
    : [{ name: "暂无数据", value: 1 }];
  creditChart.setOption({
    color: ["#22c55e", "#38bdf8", "#f59e0b", "#fb7185"],
    tooltip: {
      trigger: "item",
      backgroundColor: "rgba(15, 23, 42, 0.88)",
      borderWidth: 0,
      textStyle: { color: "#fff" }
    },
    legend: {
      bottom: 0,
      left: "center",
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: "#64748b", fontWeight: 700 }
    },
    series: [
      {
        type: "pie",
        radius: ["46%", "72%"],
        center: ["50%", "43%"],
        avoidLabelOverlap: true,
        label: { show: false },
        emphasis: { scale: true, scaleSize: 8 },
        data: rows
      }
    ]
  }, true);
}

function resizeCharts() {
  orderTrendChart?.resize();
  categoryRankChart?.resize();
  creditChart?.resize();
}

function handleRealtimeEvent(event) {
  const type = event?.detail?.eventType || "";
  if (/ORDER|AFTER_SALE|REPORT|RISK|MERCHANT|AUDIT/.test(type)) {
    loadDashboard();
  }
}

function categoryLabel(item) {
  const raw = item.categoryName || item.category || item.productCategory || item.productType || "未分类";
  return productTypeLabel(raw);
}

function productTypeLabel(value) {
  const key = String(value || "").toUpperCase();
  const map = {
    NEW: "普通商品",
    NORMAL: "普通商品",
    SECONDHAND: "二手好物",
    SECOND_HAND: "二手好物"
  };
  return map[key] || value || "未分类";
}

function riskLabel(value) {
  const map = { LOW: "低风险", MEDIUM: "中风险", HIGH: "高风险" };
  return map[String(value || "").toUpperCase()] || "风险";
}

function riskColor(value) {
  const map = { LOW: "#22c55e", MEDIUM: "#f59e0b", HIGH: "#ef4444" };
  return map[String(value || "").toUpperCase()] || "#94a3b8";
}

function actionLabel(action) {
  const map = {
    BAN_USER: "封禁用户",
    UNBAN_USER: "解封用户",
    APPROVE_MERCHANT_APPLICATION: "入驻通过",
    REJECT_MERCHANT_APPLICATION: "入驻驳回",
    CREDIT_ADJUST: "信用分调整",
    PRODUCT_RISK_DECISION: "商品风控处理",
    AUDIT_REPORT: "举报审核"
  };
  return map[action] || action || "管理员操作";
}

function logTargetText(log) {
  const target = log.targetType ? `${log.targetType} #${log.targetId || "-"}` : `#${log.targetId || "-"}`;
  return target;
}

function shortTime(value) {
  const date = toDate(value);
  if (!date) return "-";
  return `${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function toDate(value) {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(String(value).replace(" ", "T"));
  return Number.isNaN(date.getTime()) ? null : date;
}

function dateKey(date) {
  if (!date) return "";
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function pad(value) {
  return String(value).padStart(2, "0");
}

function formatMoney(value) {
  return new Intl.NumberFormat("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Number(value || 0));
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}
</script>

<style scoped>
.health-center {
  display: grid;
  gap: 16px;
  padding-bottom: 24px;
}

.health-hero {
  min-height: 220px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 18px;
  padding: 24px;
  border-radius: 8px;
  border: 1px solid rgba(125, 211, 252, 0.32);
  background:
    linear-gradient(120deg, rgba(236, 253, 245, 0.92), rgba(239, 246, 255, 0.9) 48%, rgba(253, 242, 248, 0.76)),
    repeating-linear-gradient(135deg, rgba(20, 184, 166, 0.06) 0 1px, transparent 1px 18px);
  box-shadow: 0 18px 44px rgba(14, 165, 233, 0.11);
  overflow: hidden;
}

.hero-copy {
  display: grid;
  align-content: center;
  gap: 12px;
}

.hero-kicker,
.panel-kicker {
  margin: 0;
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
}

.hero-title {
  margin: 0;
  color: #102033;
  font-size: 42px;
  line-height: 1.08;
  font-weight: 950;
}

.hero-desc {
  max-width: 640px;
  margin: 0;
  color: #475569;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
}

.health-score-panel {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.08);
}

.score-ring {
  position: relative;
  width: 142px;
  height: 142px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: conic-gradient(#14b8a6 var(--score-angle), rgba(148, 163, 184, 0.18) 0);
  box-shadow: 0 16px 30px rgba(20, 184, 166, 0.22);
}

.score-ring::after {
  position: absolute;
  inset: 12px;
  content: "";
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.96);
}

.score-ring__inner {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: baseline;
  gap: 2px;
  color: #0f172a;
}

.score-ring__inner strong {
  font-size: 40px;
  line-height: 1;
  font-weight: 950;
}

.score-ring__inner span {
  color: #64748b;
  font-weight: 900;
}

.score-copy {
  display: grid;
  gap: 8px;
}

.score-label {
  width: fit-content;
  min-height: 26px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 950;
}

.score-label--great,
.score-label--good {
  color: #047857;
  background: rgba(20, 184, 166, 0.14);
}

.score-label--warn {
  color: #b45309;
  background: rgba(245, 158, 11, 0.16);
}

.score-label--danger {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.14);
}

.score-copy h3 {
  margin: 0;
  color: #102033;
  font-size: 22px;
  font-weight: 950;
}

.score-copy p {
  margin: 0;
  color: #64748b;
  font-weight: 800;
  line-height: 1.55;
}

.data-warning {
  min-height: 38px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid rgba(245, 158, 11, 0.26);
  color: #92400e;
  background: rgba(255, 251, 235, 0.92);
  font-weight: 800;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-tile {
  min-height: 132px;
  display: grid;
  align-content: space-between;
  gap: 10px;
  padding: 16px;
  cursor: pointer;
  text-align: left;
  color: #0f172a;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background:
    linear-gradient(135deg, var(--tile-soft), rgba(255, 255, 255, 0.92) 50%),
    #fff;
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.06);
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease;
}

.metric-tile:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--tile-color) 45%, #fff);
  box-shadow: 0 20px 38px rgba(15, 23, 42, 0.1);
}

.metric-head {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
  font-weight: 950;
}

.metric-tile strong {
  min-width: 0;
  color: #102033;
  font-size: 28px;
  line-height: 1.1;
  font-weight: 950;
  word-break: break-word;
}

.metric-tile small {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.45;
}

.insight-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 14px;
}

.panel {
  min-width: 0;
  display: grid;
  gap: 12px;
  padding: 18px;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.06);
}

.panel-wide {
  grid-column: span 8;
}

.panel:not(.panel-wide) {
  grid-column: span 4;
}

.panel-chart,
.panel-log {
  grid-column: span 4;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-head h3 {
  margin: 4px 0 0;
  color: #102033;
  font-size: 18px;
  line-height: 1.2;
  font-weight: 950;
}

.panel-badge {
  min-height: 26px;
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  color: #0369a1;
  background: rgba(56, 189, 248, 0.14);
  font-size: 12px;
  font-weight: 950;
  white-space: nowrap;
}

.panel-badge.danger {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.12);
}

.chart-box {
  width: 100%;
  height: 286px;
}

.chart-box.compact {
  height: 252px;
}

.queue-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.queue-kpi {
  min-height: 94px;
  display: grid;
  align-content: center;
  gap: 4px;
  padding: 12px;
  cursor: pointer;
  text-align: left;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(135deg, rgba(254, 242, 242, 0.86), rgba(255, 255, 255, 0.92));
}

.queue-kpi span,
.queue-kpi small {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.queue-kpi strong {
  color: #b91c1c;
  font-size: 28px;
  line-height: 1;
  font-weight: 950;
}

.risk-list,
.log-list {
  display: grid;
  gap: 10px;
}

.risk-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 130px 68px;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 10px 0;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
}

.risk-title {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.risk-title strong {
  overflow: hidden;
  color: #1e293b;
  font-size: 13px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.risk-title span,
.risk-score {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
}

.risk-score {
  text-align: right;
}

.empty-line {
  min-height: 84px;
  display: grid;
  place-items: center;
  color: #94a3b8;
  font-weight: 800;
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.84);
}

.log-row {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr);
  gap: 10px;
  min-height: 56px;
  padding: 10px 0;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
}

.log-time {
  color: #64748b;
  font-size: 12px;
  font-weight: 950;
}

.log-row strong {
  display: block;
  overflow: hidden;
  color: #102033;
  font-size: 13px;
  font-weight: 950;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-row p {
  margin: 4px 0 0;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  font-weight: 750;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.module-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.module-chip {
  min-height: 88px;
  display: grid;
  gap: 6px;
  align-content: center;
  padding: 14px;
  cursor: pointer;
  text-align: left;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
  transition: transform 0.16s ease, border-color 0.16s ease;
}

.module-chip:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--chip-color) 42%, #fff);
}

.module-chip span {
  width: 28px;
  height: 4px;
  border-radius: 999px;
  background: var(--chip-color);
}

.module-chip strong {
  color: #102033;
  font-size: 14px;
  font-weight: 950;
}

.module-chip small {
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.35;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-wide,
  .panel:not(.panel-wide),
  .panel-chart,
  .panel-log {
    grid-column: span 6;
  }

  .module-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .health-hero {
    grid-template-columns: 1fr;
  }

  .health-score-panel {
    grid-template-columns: 130px minmax(0, 1fr);
  }

  .score-ring {
    width: 126px;
    height: 126px;
  }

  .panel-wide,
  .panel:not(.panel-wide),
  .panel-chart,
  .panel-log {
    grid-column: 1 / -1;
  }
}

@media (max-width: 640px) {
  .health-hero {
    padding: 18px;
  }

  .hero-title {
    font-size: 30px;
  }

  .health-score-panel {
    grid-template-columns: 1fr;
  }

  .metric-grid,
  .queue-grid,
  .module-strip {
    grid-template-columns: 1fr;
  }

  .risk-row {
    grid-template-columns: 1fr;
  }

  .risk-score {
    text-align: left;
  }
}
</style>
