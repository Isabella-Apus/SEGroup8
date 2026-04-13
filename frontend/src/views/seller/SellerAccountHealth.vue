<template>
  <div class="fade-in-up">
    <div class="page-header">
      <h2 class="page-title">Account Health</h2>
      <el-tag :type="overallTagType" size="large" style="font-size: 14px">
        {{ overallLabel }}
      </el-tag>
    </div>

    <!-- 综合评分 -->
    <el-card class="score-card" v-loading="loading">
      <div class="score-main">
        <div class="score-circle" :style="{ borderColor: scoreColor }">
          <div class="score-num" :style="{ color: scoreColor }">{{ overallScore }}</div>
          <div class="score-sub">综合评分</div>
        </div>
        <div class="score-desc">
          <p>综合评分基于好评率、按时发货率、退款率等指标计算得出。</p>
          <p>保持良好的店铺表现有助于提升曝光度和买家信任度。</p>
        </div>
      </div>
    </el-card>

    <!-- 四大指标 -->
    <el-row :gutter="16" style="margin-top: 16px" v-loading="loading">
      <el-col :span="6" v-for="metric in metrics" :key="metric.key">
        <el-card class="metric-card">
          <div class="metric-icon" :style="{ background: metric.color + '20', color: metric.color }">
            {{ metric.icon }}
          </div>
          <div class="metric-value" :style="{ color: metric.valueColor }">
            {{ metric.value }}
          </div>
          <div class="metric-label">{{ metric.label }}</div>
          <el-progress
            :percentage="metric.percentage"
            :color="metric.color"
            :show-text="false"
            style="margin-top: 8px"
          />
          <div class="metric-target">目标：{{ metric.target }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 详细数据 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card v-loading="loading">
          <template #header>订单统计</template>
          <div class="stat-row" v-for="item in orderStats" :key="item.label">
            <span class="stat-label">{{ item.label }}</span>
            <span class="stat-value" :style="{ color: item.color }">{{ item.value }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card v-loading="loading">
          <template #header>改善建议</template>
          <div v-if="suggestions.length === 0" class="empty-tip">
            🎉 店铺表现良好，继续保持！
          </div>
          <div
            v-for="(s, idx) in suggestions"
            :key="idx"
            class="suggestion-item"
          >
            <el-tag :type="s.type" size="small" style="margin-right: 8px">
              {{ s.level }}
            </el-tag>
            {{ s.text }}
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyOrders, getMyReviews } from '@/api/seller'

const loading = ref(false)
const orders = ref([])
const reviews = ref([])

async function loadData() {
  loading.value = true
  try {
    const [ordersRes, reviewsRes] = await Promise.all([
      getMyOrders({ page: 1, pageSize: 100 }),
      getMyReviews({ page: 1, pageSize: 100, keyword: '' })
    ])
    orders.value = ordersRes.data.records || []
    reviews.value = reviewsRes.data.records || []
  } catch (e) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 计算指标
const totalOrders = computed(() => orders.value.length)
const completedOrders = computed(() => orders.value.filter(o => o.orderStatus === 4).length)
const refundOrders = computed(() => orders.value.filter(o => o.refundStatus > 0).length)
const shippedOrders = computed(() => orders.value.filter(o => o.orderStatus >= 2).length)
const paidOrders = computed(() => orders.value.filter(o => o.payStatus === 1).length)

const positiveReviews = computed(() =>
  reviews.value.filter(r => r.rating >= 4).length
)

// 好评率
const positiveRate = computed(() => {
  if (!reviews.value.length) return 100
  return Math.round(positiveReviews.value / reviews.value.length * 100)
})

// 退款率
const refundRate = computed(() => {
  if (!paidOrders.value) return 0
  return Math.round(refundOrders.value / paidOrders.value * 100)
})

// 发货率（已付款订单中已发货比例）
const shipRate = computed(() => {
  if (!paidOrders.value) return 100
  return Math.round(shippedOrders.value / paidOrders.value * 100)
})

// 完成率
const completionRate = computed(() => {
  if (!paidOrders.value) return 0
  return Math.round(completedOrders.value / paidOrders.value * 100)
})

// 综合评分（满分100）
const overallScore = computed(() => {
  const score = positiveRate.value * 0.4
    + (100 - refundRate.value) * 0.3
    + shipRate.value * 0.2
    + completionRate.value * 0.1
  return Math.round(score)
})

const scoreColor = computed(() => {
  if (overallScore.value >= 80) return '#1d9e75'
  if (overallScore.value >= 60) return '#e6a23c'
  return '#f56c6c'
})

const overallLabel = computed(() => {
  if (overallScore.value >= 80) return '店铺表现优秀'
  if (overallScore.value >= 60) return '店铺表现良好'
  return '需要改善'
})

const overallTagType = computed(() => {
  if (overallScore.value >= 80) return 'success'
  if (overallScore.value >= 60) return 'warning'
  return 'danger'
})

const metrics = computed(() => [
  {
    key: 'positive',
    label: '好评率',
    value: positiveRate.value + '%',
    percentage: positiveRate.value,
    target: '≥ 90%',
    color: positiveRate.value >= 90 ? '#1d9e75' : '#e6a23c',
    valueColor: positiveRate.value >= 90 ? '#1d9e75' : '#e6a23c',
    icon: '⭐'
  },
  {
    key: 'refund',
    label: '退款率',
    value: refundRate.value + '%',
    percentage: 100 - refundRate.value,
    target: '≤ 5%',
    color: refundRate.value <= 5 ? '#1d9e75' : '#f56c6c',
    valueColor: refundRate.value <= 5 ? '#1d9e75' : '#f56c6c',
    icon: '↩️'
  },
  {
    key: 'ship',
    label: '按时发货率',
    value: shipRate.value + '%',
    percentage: shipRate.value,
    target: '≥ 95%',
    color: shipRate.value >= 95 ? '#1d9e75' : '#e6a23c',
    valueColor: shipRate.value >= 95 ? '#1d9e75' : '#e6a23c',
    icon: '🚚'
  },
  {
    key: 'completion',
    label: '订单完成率',
    value: completionRate.value + '%',
    percentage: completionRate.value,
    target: '≥ 80%',
    color: completionRate.value >= 80 ? '#1d9e75' : '#e6a23c',
    valueColor: completionRate.value >= 80 ? '#1d9e75' : '#e6a23c',
    icon: '✅'
  }
])

const orderStats = computed(() => [
  { label: '总订单数', value: totalOrders.value, color: '#303133' },
  { label: '已付款订单', value: paidOrders.value, color: '#409eff' },
  { label: '已完成订单', value: completedOrders.value, color: '#1d9e75' },
  { label: '退款订单', value: refundOrders.value, color: '#f56c6c' },
  { label: '收到评价', value: reviews.value.length, color: '#303133' },
  { label: '好评数', value: positiveReviews.value, color: '#1d9e75' }
])

const suggestions = computed(() => {
  const list = []
  if (positiveRate.value < 90) {
    list.push({ level: '建议', type: 'warning', text: '好评率低于90%，注意提升商品质量和服务' })
  }
  if (refundRate.value > 5) {
    list.push({ level: '警告', type: 'danger', text: '退款率超过5%，检查商品描述是否准确' })
  }
  if (shipRate.value < 95) {
    list.push({ level: '建议', type: 'warning', text: '发货率低于95%，请及时处理待发货订单' })
  }
  if (completionRate.value < 80) {
    list.push({ level: '建议', type: 'info', text: '订单完成率偏低，关注买家收货状态' })
  }
  return list
})

onMounted(loadData)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title { margin: 0; font-size: 20px; }
.score-card { margin-bottom: 4px; }
.score-main {
  display: flex;
  align-items: center;
  gap: 32px;
}
.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 6px solid #1d9e75;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.score-num {
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
}
.score-sub {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.score-desc {
  color: #666;
  font-size: 14px;
  line-height: 1.8;
}
.score-desc p { margin: 0 0 4px; }
.metric-card { text-align: center; }
.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin: 0 auto 8px;
}
.metric-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.2;
}
.metric-label {
  font-size: 13px;
  color: #999;
  margin: 4px 0;
}
.metric-target {
  font-size: 11px;
  color: #bbb;
  margin-top: 4px;
}
.stat-row {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 14px;
}
.stat-row:last-child { border-bottom: none; }
.stat-label { color: #666; }
.stat-value { font-weight: 600; }
.suggestion-item {
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 14px;
  color: #444;
}
.suggestion-item:last-child { border-bottom: none; }
.empty-tip {
  text-align: center;
  color: #999;
  padding: 20px 0;
}
</style>