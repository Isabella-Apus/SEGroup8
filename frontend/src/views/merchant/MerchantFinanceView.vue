<template>
  <div class="fade-in-up">
    <div class="page-header">
      <h2 class="page-title">财务看板</h2>
    </div>

    <!-- 余额卡片 -->
    <el-row :gutter="16" v-loading="loading" style="margin-bottom: 16px">
      <el-col :span="12">
        <el-card class="balance-card business">
          <div class="balance-label">经营账户余额</div>
          <div class="balance-value">¥{{ Number(finance.businessBalance || 0).toFixed(2) }}</div>
          <div class="balance-desc">官方商品经营相关收入</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="balance-card income">
          <div class="balance-label">累计收入</div>
          <div class="balance-value income-text">¥{{ totalIncome }}</div>
          <div class="balance-desc">已完成订单收入总计</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 流水记录 -->
    <el-card>
      <template #header>
        <div class="card-header">
          <span>交易流水</span>
          <el-select
            v-model="filterType"
            placeholder="全部类型"
            clearable
            style="width: 160px"
            @change="filterRecords"
          >
            <el-option label="订单收入" value="ORDER_INCOME" />
            <el-option label="订单退款" value="ORDER_REFUND" />
            <el-option label="充值" value="RECHARGE" />
            <el-option label="提现" value="WITHDRAW" />
          </el-select>
        </div>
      </template>

      <el-table :data="filteredRecords" v-loading="loading" stripe>
        <el-table-column prop="orderId" label="订单ID" width="100" />
        <el-table-column label="类型" width="140">
          <template #default="{ row }">
            <el-tag :type="tradeTagType(row)" size="small">
              {{ resolveTradeTypeLabel(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span :class="Number(row.amount) >= 0 ? 'amount-plus' : 'amount-minus'">
              {{ Number(row.amount) >= 0 ? '+' : '' }}{{ Number(row.amount || 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" />
        <el-table-column label="时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
      </el-table>

      <div v-if="filteredRecords.length === 0 && !loading" class="empty-tip">
        暂无交易记录
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, computed } from 'vue'
import { getBusinessRecordsApi, getFinanceDashboardApi } from '@/api/finance'
import { resolveTradeTypeLabel } from '@/utils/finance'

const loading = ref(false)
const filterType = ref('')
const finance = reactive({
  businessBalance: 0
})
const businessRecords = ref([])

const totalIncome = computed(() => {
  const income = businessRecords.value
    .reduce((sum, r) => sum + Number(r.amount), 0)
  return Math.max(0, income).toFixed(2)
})

const filteredRecords = computed(() => {
  if (!filterType.value) return businessRecords.value
  return businessRecords.value.filter(r => r.tradeType === filterType.value)
})

function filterRecords() {}

function tradeTagType(row) {
  const type = row.tradeType || ''
  if (type.includes('INCOME') || Number(row.amount) > 0) return 'success'
  if (type.includes('REFUND') || Number(row.amount) < 0) return 'danger'
  return 'info'
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(async () => {
  loading.value = true
  try {
    const result = await getFinanceDashboardApi()
    finance.businessBalance = result.data?.businessBalance || 0
    const business = await getBusinessRecordsApi()
    businessRecords.value = business.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title { margin: 0; font-size: 20px; }
.balance-card { text-align: center; padding: 8px 0; }
.balance-card.business { border-top: 3px solid #1d9e75; }
.balance-card.income { border-top: 3px solid #e4393c; }
.balance-label { font-size: 13px; color: #999; }
.balance-value { font-size: 28px; font-weight: 700; color: #303133; margin: 8px 0; }
.balance-value.income-text { color: #e4393c; }
.income-text { color: #e4393c; }
.balance-desc { font-size: 12px; color: #bbb; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.amount-plus { color: #16a34a; font-weight: 600; }
.amount-minus { color: #dc2626; font-weight: 600; }
.empty-tip { text-align: center; color: #999; padding: 30px 0; }
</style>
