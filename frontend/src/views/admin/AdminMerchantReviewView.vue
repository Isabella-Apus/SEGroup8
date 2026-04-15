<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="query.status" clearable placeholder="审核状态" style="width: 180px">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="records" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="申请用户" width="120" />
      <el-table-column prop="storeName" label="店名" min-width="150" />
      <el-table-column prop="categoryId" label="类目ID" width="90" />
      <el-table-column prop="warehouseProvince" label="仓库省份" width="120" />
      <el-table-column prop="warehouseCity" label="仓库城市" width="120" />
      <el-table-column prop="contactName" label="负责人" width="120" />
      <el-table-column prop="contactPhone" label="电话" width="140" />
      <el-table-column prop="idCardNo" label="身份证(脱敏)" width="160" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">{{ statusText(row.status) }}</template>
      </el-table-column>
      <el-table-column prop="applyTime" label="申请时间" min-width="180" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" link type="success" @click="approve(row.id)">通过</el-button>
          <el-button v-if="row.status === 0" link type="danger" @click="openReject(row.id)">驳回</el-button>
          <span v-if="row.status !== 0">-</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
        layout="total, prev, pager, next" :total="total" @current-change="load" />
    </div>

    <el-dialog v-model="rejectVisible" title="驳回申请" width="460px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请输入驳回理由" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  approveMerchantApplicationApi,
  pageMerchantApplicationsApi,
  rejectMerchantApplicationApi
} from "@/api/merchantApplication";

const records = ref([]);
const total = ref(0);
const rejectVisible = ref(false);
const rejectReason = ref("");
const rejectId = ref(null);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null
});

onMounted(load);

function statusText(status) {
  if (status === 1) return "通过";
  if (status === 2) return "驳回";
  return "待审核";
}

async function load() {
  try {
    const result = await pageMerchantApplicationsApi(query);
    records.value = result.data.records || [];
    total.value = result.data.total || 0;
  } catch (error) {
    records.value = [];
    total.value = 0;
  }
}

async function approve(id) {
  await approveMerchantApplicationApi(id);
  ElMessage.success("审核通过，用户已升级为官方卖家");
  await load();
}

function openReject(id) {
  rejectId.value = id;
  rejectReason.value = "";
  rejectVisible.value = true;
}

async function confirmReject() {
  if (!rejectReason.value) {
    ElMessage.warning("请输入驳回理由");
    return;
  }
  await rejectMerchantApplicationApi(rejectId.value, { rejectReason: rejectReason.value });
  ElMessage.success("已驳回申请");
  rejectVisible.value = false;
  await load();
}
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
