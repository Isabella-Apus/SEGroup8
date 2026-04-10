<template>
  <div class="page-card fade-in-up">
    <h2 class="page-title">财务看板</h2>
    <div class="finance-card business" v-loading="loading">
      <div class="label">经营账户余额</div>
      <div class="value">￥{{ Number(finance.businessBalance || 0).toFixed(2) }}</div>
      <div class="desc">仅展示官方商品经营相关流水</div>
    </div>
    <el-table :data="businessRecords" border class="records-table" v-loading="loading">
      <el-table-column prop="orderId" label="订单ID" width="100" />
      <el-table-column label="类型" min-width="180">
        <template #default="scope">{{ resolveTradeTypeLabel(scope.row) }}</template>
      </el-table-column>
      <el-table-column prop="amount" label="金额" width="120">
        <template #default="scope">
          <span :class="Number(scope.row.amount || 0) >= 0 ? 'amount-plus' : 'amount-minus'">
            {{ Number(scope.row.amount || 0).toFixed(2) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="220" />
      <el-table-column prop="createTime" label="时间" min-width="180" />
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { getBusinessRecordsApi, getFinanceDashboardApi } from '@/api/finance';
import { resolveTradeTypeLabel } from '@/utils/finance';

const loading = ref(false);
const finance = reactive({
  personalBalance: 0,
  businessBalance: 0
});
const businessRecords = ref([]);

onMounted(() => {
  fetchFinance();
});

async function fetchFinance() {
  loading.value = true;
  try {
    const result = await getFinanceDashboardApi();
    finance.businessBalance = result.data?.businessBalance || 0;
    const business = await getBusinessRecordsApi();
    businessRecords.value = business.data || [];
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.finance-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  background: linear-gradient(140deg, #ffffff 0%, #f9fafb 100%);
}

.finance-card.personal {
  border-color: #fde68a;
  background: linear-gradient(140deg, #fffdf5 0%, #fffbeb 100%);
}

.finance-card.business {
  border-color: #bfdbfe;
  background: linear-gradient(140deg, #f8fbff 0%, #eff6ff 100%);
}

.label {
  color: #4b5563;
  font-size: 14px;
}

.value {
  font-size: 28px;
  font-weight: 700;
  margin: 8px 0;
  color: #111827;
}

.desc {
  color: #6b7280;
  font-size: 13px;
}

.records-table {
  margin-top: 12px;
}

.amount-plus {
  color: #16a34a;
  font-weight: 600;
}

.amount-minus {
  color: #dc2626;
  font-weight: 600;
}
</style>
