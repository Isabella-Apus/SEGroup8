<template>
  <div class="page-card">
    <div class="header">
      <div>
        <h2 class="title">抢券中心</h2>
        <p class="desc">这里显示卖家和管理员发布的可领取优惠券</p>
      </div>
      <el-button @click="$router.push('/vouchers')">我的优惠券</el-button>
    </div>

    <el-table :data="displayRecords" v-loading="loading" border stripe>
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
      <el-table-column label="抢券时间" min-width="300">
        <template #default="{ row }">
          {{ formatTime(row.grabStartTime) }} 至 {{ formatTime(row.grabEndTime) }}
        </template>
      </el-table-column>
      <el-table-column label="使用有效期" min-width="300">
        <template #default="{ row }">
          有效期：{{ formatDate(row.startTime) }} 至 {{ formatDate(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            :disabled="buttonState(row).disabled"
            @click="claim(row)"
          >
            {{ buttonState(row).text }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { claimVoucherApi, myVoucherApi, pageAvailableVoucherApi } from '@/api/voucher'

const loading = ref(false)
const records = ref([])
const claimedSet = ref(new Set())

const displayRecords = computed(() => {
  const now = Date.now()
  return records.value
    .filter(row => Number(row.remainCount || 0) > 0)
    .filter(row => {
      const grabEnd = row.grabEndTime ? new Date(row.grabEndTime).getTime() : 0
      return !grabEnd || now <= grabEnd
    })
    .sort((a, b) => new Date(a.grabStartTime || 0).getTime() - new Date(b.grabStartTime || 0).getTime())
})

function buttonState(row) {
  if (claimedSet.value.has(row.id)) {
    return { text: '已领取', disabled: true }
  }
  const now = Date.now()
  const grabStart = row.grabStartTime ? new Date(row.grabStartTime).getTime() : 0
  if (grabStart && now < grabStart) {
    return { text: '未开始', disabled: true }
  }
  return { text: '立即领取', disabled: false }
}

async function load() {
  loading.value = true
  try {
    const [availableRes, mineRes] = await Promise.all([
      pageAvailableVoucherApi({ page: 1, pageSize: 200 }),
      myVoucherApi({ page: 1, pageSize: 500 })
    ])
    records.value = availableRes.data.records || []
    const mine = mineRes.data.records || []
    claimedSet.value = new Set(mine.map(v => v.id))
  } finally {
    loading.value = false
  }
}

async function claim(row) {
  const state = buttonState(row)
  if (state.disabled) return
  await claimVoucherApi(row.id)
  ElMessage.success('领取成功')
  await load()
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function formatDate(time) {
  if (!time) return '-'
  return new Date(time).toLocaleDateString('zh-CN')
}

onMounted(load)
</script>

<style scoped>
.header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.title{margin:0}.desc{margin:6px 0 0;color:#6b7280}
</style>
