<template>
  <div class="page-card risk-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">商品风险审核</h2>
        <p class="page-subtitle">系统自动筛查商品风险，管理员负责最终审核处理。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="商品类型">
        <el-select v-model="query.productType" clearable placeholder="全部" style="width: 140px">
          <el-option label="普通商品" value="NEW" />
          <el-option label="二手商品" value="SECONDHAND" />
        </el-select>
      </el-form-item>
      <el-form-item label="风险等级">
        <el-select v-model="query.riskLevel" clearable placeholder="全部" style="width: 130px">
          <el-option label="低" value="LOW" />
          <el-option label="中" value="MEDIUM" />
          <el-option label="高" value="HIGH" />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态">
        <el-select v-model="query.auditStatus" clearable placeholder="全部" style="width: 150px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="要求修改" value="CHANGE_REQUESTED" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="商品名" style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-mobile-wrap">
      <el-table v-loading="loading" :data="records" border class="kg-table">
      <el-table-column prop="id" label="ID" width="74" />
      <el-table-column label="商品" min-width="220">
        <template #default="{ row }">
          <div class="product-cell">
            <strong>{{ row.productName || "-" }}</strong>
            <span>{{ productTypeLabel(row.productType) }} #{{ row.productId }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="sellerUserId" label="卖家ID" width="90" />
      <el-table-column prop="sellerName" label="卖家" width="120" show-overflow-tooltip />
      <el-table-column label="风险" width="120">
        <template #default="{ row }">
          <el-tag class="tag-soft" :type="riskTagType(row.riskLevel)" effect="plain">
            {{ riskLabel(row.riskLevel) }} · {{ row.riskScore }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="风险原因" min-width="260">
        <template #default="{ row }">
          <div v-if="row.riskReasons?.length" class="reason-list">
            <el-tag v-for="reason in row.riskReasons" :key="reason" class="tag-soft" size="small" effect="plain">
              {{ reason }}
            </el-tag>
          </div>
          <span v-else class="muted">未发现明显风险</span>
        </template>
      </el-table-column>
      <el-table-column label="建议" width="130">
        <template #default="{ row }">{{ suggestionLabel(row.suggestion) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag class="status-tag" :class="auditStatusClass(row.auditStatus)" size="small" effect="plain">
            {{ statusLabel(row.auditStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="生成时间" width="170" />
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <div v-if="row.auditStatus === 'PENDING'" class="table-actions">
            <el-button link type="success" size="small" @click="submitDecision(row, 'APPROVED')">通过</el-button>
            <el-button link type="warning" size="small" @click="openRemark(row, 'CHANGE_REQUESTED')">要求修改</el-button>
            <el-button link type="danger" size="small" @click="openRemark(row, 'REJECTED')">驳回</el-button>
          </div>
          <span v-else class="muted-pill">已处理</span>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">暂无风险审核记录</div>
      </template>
      </el-table>
    </div>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="remarkVisible" :title="decisionTitle" width="460px" append-to-body>
      <el-input
        v-model="decisionForm.adminRemark"
        type="textarea"
        :rows="4"
        maxlength="500"
        show-word-limit
        placeholder="填写管理员处理意见"
      />
      <template #footer>
        <el-button @click="remarkVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmRemarkDecision">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  decideProductRiskAuditApi,
  pageProductRiskAuditsApi,
} from "@/api/productRiskAudit";

const loading = ref(false);
const submitting = ref(false);
const records = ref([]);
const total = ref(0);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  productType: "",
  riskLevel: "",
  auditStatus: "PENDING",
  keyword: "",
});
const remarkVisible = ref(false);
const decisionForm = reactive({
  row: null,
  decision: "",
  adminRemark: "",
});

const decisionTitle = computed(() => {
  return decisionForm.decision === "REJECTED" ? "驳回商品" : "要求卖家修改";
});

onMounted(() => load());

async function load() {
  loading.value = true;
  try {
    const res = await pageProductRiskAuditsApi(cleanQuery());
    records.value = (res.data?.records || []).map(normalizeAuditRecord);
    total.value = res.data?.total || 0;
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "加载风险审核列表失败");
  } finally {
    loading.value = false;
  }
}

function cleanQuery() {
  return Object.fromEntries(
    Object.entries(query).filter(([, value]) => value !== "" && value !== null && value !== undefined)
  );
}

function normalizeAuditRecord(row) {
  const score = Number(row.riskScore ?? row.score ?? 0);
  return {
    ...row,
    riskScore: score,
    riskLevel: row.riskLevel || inferRiskLevel(score),
    riskReasons: normalizeReasons(row.riskReasons),
    suggestion: row.suggestion || inferSuggestion(score),
    auditStatus: row.auditStatus || "PENDING",
  };
}

function normalizeReasons(value) {
  if (Array.isArray(value)) return value;
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed : [String(value)];
  } catch {
    return [String(value)];
  }
}

function inferRiskLevel(score) {
  if (score >= 70) return "HIGH";
  if (score >= 35) return "MEDIUM";
  return "LOW";
}

function inferSuggestion(score) {
  if (score >= 70) return "ADMIN_REVIEW";
  if (score >= 35) return "REQUIRE_PROOF";
  return "AUTO_PASS";
}

function search() {
  query.pageNum = 1;
  load();
}

function reset() {
  query.pageNum = 1;
  query.productType = "";
  query.riskLevel = "";
  query.auditStatus = "PENDING";
  query.keyword = "";
  load();
}

async function submitDecision(row, decision, adminRemark = "") {
  const label = statusLabel(decision);
  await ElMessageBox.confirm(`确认将该审核结果标记为「${label}」？`, "提示", { type: "warning" });
  submitting.value = true;
  try {
    await decideProductRiskAuditApi(row.id, { decision, adminRemark });
    ElMessage.success("处理成功");
    remarkVisible.value = false;
    load();
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || "处理失败");
  } finally {
    submitting.value = false;
  }
}

function openRemark(row, decision) {
  decisionForm.row = row;
  decisionForm.decision = decision;
  decisionForm.adminRemark = "";
  remarkVisible.value = true;
}

function confirmRemarkDecision() {
  submitDecision(decisionForm.row, decisionForm.decision, decisionForm.adminRemark);
}

function productTypeLabel(value) {
  return value === "SECONDHAND" ? "二手商品" : "普通商品";
}

function riskLabel(value) {
  return { LOW: "低", MEDIUM: "中", HIGH: "高" }[value] || value;
}

function riskTagType(value) {
  return { LOW: "success", MEDIUM: "warning", HIGH: "danger" }[value] || "info";
}

function suggestionLabel(value) {
  return { AUTO_PASS: "自动通过", REQUIRE_PROOF: "建议补充凭证", ADMIN_REVIEW: "管理员复核" }[value] || value;
}

function statusLabel(value) {
  return { PENDING: "待处理", APPROVED: "已通过", REJECTED: "已驳回", CHANGE_REQUESTED: "要求修改" }[value] || value;
}

function auditStatusClass(value) {
  return {
    PENDING: "status-pending",
    APPROVED: "status-success",
    REJECTED: "status-danger",
    CHANGE_REQUESTED: "status-info"
  }[value] || "status-muted";
}
</script>

<style scoped>
.risk-page {
  padding: 18px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.page-title {
  margin: 0;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.query-form {
  padding: 12px;
  margin-bottom: 16px;
}

.product-cell {
  display: grid;
  gap: 4px;
}

.product-cell span,
.muted {
  color: #909399;
  font-size: 12px;
}

.reason-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
