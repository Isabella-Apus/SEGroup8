<template>
  <div class="page-card">
    <div class="header">
      <div>
        <h2 class="title">我的优惠券</h2>
        <p class="desc">查看已领取优惠券，并可在下单时使用</p>
      </div>
      <el-button type="primary" @click="goClaim">去抢券</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
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
      <el-table-column prop="statusName" label="状态" width="100" />
      <el-table-column prop="startTime" label="开始时间" width="180" />
      <el-table-column prop="endTime" label="结束时间" width="180" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { myVoucherApi } from '@/api/voucher'

const router = useRouter()
const loading = ref(false)
const records = ref([])

async function load() {
  loading.value = true
  try {
    const res = await myVoucherApi({ page: 1, pageSize: 50 })
    records.value = res.data.records || []
  } finally {
    loading.value = false
  }
}

function goClaim() {
  router.push({ name: 'voucherClaim' })
}

onMounted(load)
</script>

<style scoped>
.header{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:16px}.title{margin:0}.desc{margin:6px 0 0;color:#6b7280}
</style>
