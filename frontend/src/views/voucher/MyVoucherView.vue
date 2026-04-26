<template>
  <div class="page-card">
    <div class="header">
      <div>
        <h2 class="title">我的优惠券</h2>
        <p class="desc">查看已领取、未使用、已使用的优惠券</p>
      </div>
      <el-button type="primary" @click="$router.push('/vouchers/claim')">去抢券</el-button>
    </div>

    <el-tabs v-model="activeTab" style="margin-bottom: 12px">
      <el-tab-pane label="未使用" name="unused" />
      <el-tab-pane label="已使用" name="used" />
      <el-tab-pane label="已过期" name="expired" />
    </el-tabs>

    <el-table :data="filteredRecords" v-loading="loading" border stripe>
      <el-table-column prop="name" label="优惠券" min-width="180" />
      <el-table-column prop="scopeTypeName" label="范围" width="100" />
      <el-table-column prop="typeName" label="类型" width="100" />
      <el-table-column label="规则" min-width="220">
        <template #default="{ row }">
          <span v-if="row.type === 1">
            {{ row.minAmount > 0 ? `满${row.minAmount}减${row.discountAmount}` : `无门槛减${row.discountAmount}` }}
          </span>
          <span v-else>
            {{ row.minAmount > 0 ? `满${row.minAmount}打${(row.discountRate * 10).toFixed(1)}折` : `无门槛打${(row.discountRate * 10).toFixed(1)}折` }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="myStatusName" label="状态" width="100" />
      <el-table-column label="使用有效期" min-width="300">
        <template #default="{ row }">
          {{ formatTime(row.startTime) }} 至 {{ formatTime(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="距离到期" width="180">
        <template #default="{ row }">
          <span v-if="Number(row.myStatus) === 1">{{ formatRemaining(row.endTime) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { myVoucherApi } from '@/api/voucher'

const loading = ref(false)
const records = ref([])
const activeTab = ref('unused')

const filteredRecords = computed(() => {
  const statusMap = {
    unused: 1,
    used: 2,
    expired: 3
  }
  const target = statusMap[activeTab.value]
  return records.value
    .filter(item => Number(item.myStatus) === target)
    .sort((a, b) => new Date(a.endTime || 0).getTime() - new Date(b.endTime || 0).getTime())
})

async function load() {
  loading.value = true
  try {
    const res = await myVoucherApi({ page: 1, pageSize: 200 })
    records.value = res.data.records || []
  } finally {
    loading.value = false
  }
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function formatRemaining(endTime) {
  if (!endTime) return '-'
  const diffMs = new Date(endTime).getTime() - Date.now()
  if (diffMs <= 0) return '已到期'

  const dayMs = 24 * 60 * 60 * 1000
  if (diffMs >= dayMs) {
    const days = Math.ceil(diffMs / dayMs)
    return `还有 ${days} 天`
  }

  const totalMinutes = Math.ceil(diffMs / (60 * 1000))
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return `还有 ${hours} 小时 ${minutes} 分钟`
}

onMounted(load)
</script>

<style scoped>
.header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.title{margin:0}.desc{margin:6px 0 0;color:#6b7280}
</style>
