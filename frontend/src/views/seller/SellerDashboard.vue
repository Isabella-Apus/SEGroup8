<template>
  <div class="dashboard">
    <!-- 顶部时间选择 -->
    <div class="page-header">
      <h2 class="page-title">Business Insights</h2>
      <el-radio-group v-model="timeRange" @change="loadAll">
        <el-radio-button label="7">近7天</el-radio-button>
        <el-radio-button label="30">近30天</el-radio-button>
        <el-radio-button label="90">近90天</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 汇总卡片 -->
    <el-row :gutter="16" class="stats-row" v-loading="summaryLoading">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value income">¥{{ summary.totalRevenue }}</div>
          <div class="stat-label">销售额</div>
          <div class="stat-sub">较上期
            <span :class="summary.revenueGrowth >= 0 ? 'up' : 'down'">
              {{ summary.revenueGrowth >= 0 ? '▲' : '▼' }}
              {{ Math.abs(summary.revenueGrowth) }}%
            </span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ summary.totalOrders }}</div>
          <div class="stat-label">订单数</div>
          <div class="stat-sub">较上期
            <span :class="summary.ordersGrowth >= 0 ? 'up' : 'down'">
              {{ summary.ordersGrowth >= 0 ? '▲' : '▼' }}
              {{ Math.abs(summary.ordersGrowth) }}%
            </span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ summary.completedOrders }}</div>
          <div class="stat-label">已完成订单</div>
          <div class="stat-sub">完成率 {{ summary.completionRate }}%</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-value">{{ summary.refundOrders }}</div>
          <div class="stat-label">退款订单</div>
          <div class="stat-sub">退款率 {{ summary.refundRate }}%</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势图 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="14">
        <el-card v-loading="chartLoading">
          <template #header>
            <div class="card-header">
              <span>销售额趋势</span>
              <el-radio-group v-model="chartType" size="small">
                <el-radio-button label="revenue">销售额</el-radio-button>
                <el-radio-button label="orders">订单数</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="lineChartRef" style="height: 260px"></div>
        </el-card>
      </el-col>

      <el-col :span="10">
        <el-card v-loading="chartLoading">
          <template #header>订单状态分布</template>
          <div ref="pieChartRef" style="height: 260px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 商品销量排行 + 最近订单 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="10">
        <el-card v-loading="rankLoading">
          <template #header>
            <div class="card-header">
              <span>商品销量排行</span>
            </div>
          </template>
          <div v-if="productRank.length === 0" class="empty-tip">暂无数据</div>
          <div
            v-for="(item, idx) in productRank"
            :key="item.productId"
            class="rank-item"
          >
            <span class="rank-num" :class="idx < 3 ? 'top' : ''">{{ idx + 1 }}</span>
            <span class="rank-name">{{ item.productName }}</span>
            <el-progress
              :percentage="item.percentage"
              :show-text="false"
              style="flex: 1; margin: 0 12px"
            />
            <span class="rank-count">{{ item.count }} 件</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card v-loading="ordersLoading">
          <template #header>
            <div class="card-header">
              <span>最近订单</span>
              <el-button text type="primary" @click="$router.push('/merchant/orders')">
                查看全部
              </el-button>
            </div>
          </template>
          <el-table :data="recentOrders" stripe size="small">
            <el-table-column prop="orderNo" label="订单号" width="170" />
            <el-table-column label="金额" width="90">
              <template #default="{ row }">
                <span class="price-text">¥{{ row.totalAmount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.orderStatus)" size="small">
                  {{ row.orderStatusName }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="时间">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getMyOrders } from '@/api/seller'

const timeRange = ref('7')
const chartType = ref('revenue')
const summaryLoading = ref(false)
const chartLoading = ref(false)
const rankLoading = ref(false)
const ordersLoading = ref(false)

const summary = reactive({
  totalRevenue: '0.00',
  totalOrders: 0,
  completedOrders: 0,
  refundOrders: 0,
  completionRate: 0,
  refundRate: 0,
  revenueGrowth: 0,
  ordersGrowth: 0
})

const recentOrders = ref([])
const productRank = ref([])

const lineChartRef = ref(null)
const pieChartRef = ref(null)
let lineChart = null
let pieChart = null

// 获取日期范围内的所有订单
async function fetchOrders(days) {
  const res = await getMyOrders({ page: 1, pageSize: 100 })
  const allOrders = res.data.records || []
  const cutoff = new Date()
  cutoff.setDate(cutoff.getDate() - days)
  return allOrders.filter(o => new Date(o.createTime) >= cutoff)
}

async function fetchPrevOrders(days) {
  const res = await getMyOrders({ page: 1, pageSize: 100 })
  const allOrders = res.data.records || []
  const start = new Date()
  start.setDate(start.getDate() - days * 2)
  const end = new Date()
  end.setDate(end.getDate() - days)
  return allOrders.filter(o => {
    const t = new Date(o.createTime)
    return t >= start && t < end
  })
}

async function loadSummary() {
  summaryLoading.value = true
  try {
    const days = parseInt(timeRange.value)
    const [curr, prev] = await Promise.all([
      fetchOrders(days),
      fetchPrevOrders(days)
    ])

    const revenue = curr.filter(o => o.payStatus === 1)
      .reduce((s, o) => s + Number(o.totalAmount), 0)
    const prevRevenue = prev.filter(o => o.payStatus === 1)
      .reduce((s, o) => s + Number(o.totalAmount), 0)

    summary.totalRevenue = revenue.toFixed(2)
    summary.totalOrders = curr.length
    summary.completedOrders = curr.filter(o => o.orderStatus === 4).length
    summary.refundOrders = curr.filter(o => o.refundStatus > 0).length
    summary.completionRate = curr.length
      ? Math.round(summary.completedOrders / curr.length * 100) : 0
    summary.refundRate = curr.length
      ? Math.round(summary.refundOrders / curr.length * 100) : 0
    summary.revenueGrowth = prevRevenue
      ? Math.round((revenue - prevRevenue) / prevRevenue * 100) : 0
    summary.ordersGrowth = prev.length
      ? Math.round((curr.length - prev.length) / prev.length * 100) : 0
  } catch (e) {
    ElMessage.error('加载统计数据失败')
  } finally {
    summaryLoading.value = false
  }
}

async function loadCharts() {
  chartLoading.value = true
  try {
    const days = parseInt(timeRange.value)
    const orders = await fetchOrders(days)

    // 生成日期序列
    const dateMap = {}
    for (let i = days - 1; i >= 0; i--) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      const key = `${d.getMonth() + 1}/${d.getDate()}`
      dateMap[key] = { revenue: 0, count: 0 }
    }

    orders.forEach(o => {
      const d = new Date(o.createTime)
      const key = `${d.getMonth() + 1}/${d.getDate()}`
      if (dateMap[key]) {
        dateMap[key].count++
        if (o.payStatus === 1) {
          dateMap[key].revenue += Number(o.totalAmount)
        }
      }
    })

    const dates = Object.keys(dateMap)
    const revenues = dates.map(d => dateMap[d].revenue.toFixed(2))
    const counts = dates.map(d => dateMap[d].count)

    // 折线图
    await nextTick()
    if (!lineChart) lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11 } },
      yAxis: { type: 'value', axisLabel: { fontSize: 11 } },
      series: [{
        name: chartType.value === 'revenue' ? '销售额' : '订单数',
        type: 'line',
        smooth: true,
        data: chartType.value === 'revenue' ? revenues : counts,
        itemStyle: { color: '#1d9e75' },
        areaStyle: { color: 'rgba(29,158,117,0.1)' }
      }],
      grid: { left: 50, right: 20, top: 20, bottom: 30 }
    })

    // 饼图
    const statusMap = {}
    orders.forEach(o => {
      const name = o.orderStatusName || '未知'
      statusMap[name] = (statusMap[name] || 0) + 1
    })
    const pieData = Object.entries(statusMap).map(([name, value]) => ({ name, value }))

    if (!pieChart) pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0, textStyle: { fontSize: 11 } },
      series: [{
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        data: pieData,
        label: { show: false }
      }]
    })
  } catch (e) {
    ElMessage.error('加载图表失败')
  } finally {
    chartLoading.value = false
  }
}

async function loadRank() {
  rankLoading.value = true
  try {
    const days = parseInt(timeRange.value)
    const orders = await fetchOrders(days)
    const itemMap = {}
    orders.forEach(o => {
      (o.items || []).forEach(item => {
        if (!itemMap[item.productId]) {
          itemMap[item.productId] = { productName: item.productName, count: 0 }
        }
        itemMap[item.productId].count += item.quantity
      })
    })
    const sorted = Object.entries(itemMap)
      .map(([id, v]) => ({ productId: id, ...v }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 5)
    const max = sorted[0]?.count || 1
    productRank.value = sorted.map(item => ({
      ...item,
      percentage: Math.round(item.count / max * 100)
    }))
  } catch (e) {
    ElMessage.error('加载排行失败')
  } finally {
    rankLoading.value = false
  }
}

async function loadRecentOrders() {
  ordersLoading.value = true
  try {
    const res = await getMyOrders({ page: 1, pageSize: 8 })
    recentOrders.value = res.data.records || []
  } catch (e) {
    ElMessage.error('加载订单失败')
  } finally {
    ordersLoading.value = false
  }
}

async function loadAll() {
  await Promise.all([
    loadSummary(),
    loadCharts(),
    loadRank()
  ])
}

watch(chartType, () => loadCharts())

onMounted(async () => {
  await loadRecentOrders()
  await loadAll()
})

function statusTagType(status) {
  const map = { 0: 'info', 1: 'warning', 2: 'primary', 3: 'success', 4: 'success', 9: 'danger' }
  return map[status] ?? 'info'
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style scoped>
.dashboard { padding-bottom: 24px; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title { margin: 0; font-size: 20px; }
.stats-row { margin-bottom: 4px; }
.stat-card { text-align: center; padding: 4px 0; }
.stat-value { font-size: 28px; font-weight: 600; color: #303133; line-height: 1.3; }
.stat-value.income { color: #e4393c; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }
.stat-sub { font-size: 12px; color: #bbb; margin-top: 2px; }
.up { color: #e4393c; }
.down { color: #1d9e75; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.price-text { color: #e4393c; font-weight: 500; }
.empty-tip { text-align: center; color: #999; padding: 30px 0; font-size: 13px; }
.rank-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
}
.rank-item:last-child { border-bottom: none; }
.rank-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f0f0f0;
  color: #999;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 10px;
}
.rank-num.top { background: #1d9e75; color: #fff; }
.rank-name { font-size: 13px; width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rank-count { font-size: 13px; color: #666; flex-shrink: 0; }
</style>