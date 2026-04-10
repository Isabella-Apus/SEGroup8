<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.adminUsername" placeholder="管理员账号" clearable style="width: 180px" />
      <el-select v-model="query.action" clearable placeholder="操作类型" style="width: 220px">
        <el-option label="BAN_USER" value="BAN_USER" />
        <el-option label="UNBAN_USER" value="UNBAN_USER" />
        <el-option label="APPROVE_MERCHANT_APPLICATION" value="APPROVE_MERCHANT_APPLICATION" />
        <el-option label="REJECT_MERCHANT_APPLICATION" value="REJECT_MERCHANT_APPLICATION" />
      </el-select>
      <el-select v-model="query.targetType" clearable placeholder="目标类型" style="width: 160px">
        <el-option label="USER" value="USER" />
        <el-option label="MERCHANT_APPLICATION" value="MERCHANT_APPLICATION" />
      </el-select>
      <el-button type="primary" @click="loadLogs">查询</el-button>
    </div>

    <el-table :data="records" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="adminUsername" label="管理员" width="120" />
      <el-table-column prop="action" label="操作" min-width="220" />
      <el-table-column prop="targetType" label="目标类型" width="180" />
      <el-table-column prop="targetId" label="目标ID" width="100" />
      <el-table-column prop="detail" label="详情" min-width="220" />
      <el-table-column prop="createTime" label="时间" min-width="180" />
    </el-table>

    <div class="pager">
      <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
        layout="total, prev, pager, next" :total="total" @current-change="loadLogs" />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { pageAdminAuditLogsApi } from "@/api/adminAuditLog";

const records = ref([]);
const total = ref(0);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  action: "",
  targetType: "",
  adminUsername: ""
});

onMounted(loadLogs);

async function loadLogs() {
  try {
    const result = await pageAdminAuditLogsApi(query);
    records.value = result.data.records || [];
    total.value = result.data.total || 0;
  } catch (error) {
    records.value = [];
    total.value = 0;
  }
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
