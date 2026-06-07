<template>
  <div class="page-card fade-in-up">
    <h2 class="page-title">举报审核</h2>

    <!-- 筛选栏 -->
    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="审核状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width:140px">
          <el-option label="待审核" :value="0" />
          <el-option label="已成立" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="被举报用户ID">
        <el-input
          v-model="query.reportedId"
          clearable placeholder="不填=全部"
          style="width:160px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 举报列表 -->
    <div class="table-mobile-wrap">
      <el-table v-loading="loading" :data="records" border class="kg-table">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="reporterId" label="举报人ID" width="100" />
      <el-table-column prop="reportedId" label="被举报ID" width="100" />
      <el-table-column prop="reporterRole" label="举报人身份" width="110">
        <template #default="{ row }">
          <el-tag class="tag-soft" size="small" :type="row.reporterRole === 'SELLER' ? 'warning' : 'primary'" effect="plain">
            {{ row.reporterRole === 'SELLER' ? '卖家' : '买家' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reasonType" label="举报类型" min-width="140">
        <template #default="{ row }">{{ reasonTypeLabel(row.reasonType) }}</template>
      </el-table-column>
      <el-table-column label="说明" min-width="220">
        <template #default="{ row }">
          <div class="reason-desc-cell">
            <span>{{ reportDescPreview(row.reasonDesc) }}</span>
            <el-button
              v-if="shouldExpandReportDesc(row.reasonDesc)"
              link
              type="primary"
              size="small"
              class="reason-desc-expand"
              @click="openReportDescDialog(row)"
            >
              展开
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="evidenceUrls" label="证据" width="80">
        <template #default="{ row }">
          <el-button
            v-if="row.evidenceUrls"
            link type="primary" size="small"
            @click="showEvidence(row.evidenceUrls)"
          >查看</el-button>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag class="status-tag" :class="reportStatusClass(row.status)" size="small" effect="plain">
            {{ reportStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="adminRemark" label="管理员备注" min-width="160" show-overflow-tooltip />
      <el-table-column prop="createTime" label="提交时间" min-width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <div class="table-actions">
            <el-button
              v-if="row.status === 0"
              link type="primary" size="small"
              @click="openAudit(row)"
            >审核</el-button>
            <span v-else class="muted-pill">已处理</span>
          </div>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">暂无举报记录</div>
      </template>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pager">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </div>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="审核举报" width="480px" append-to-body draggable>
      <el-descriptions :column="1" border size="small" style="margin-bottom:16px">
        <el-descriptions-item label="举报人ID">{{ auditRow.reporterId }}</el-descriptions-item>
        <el-descriptions-item label="被举报ID">{{ auditRow.reportedId }}</el-descriptions-item>
        <el-descriptions-item label="举报类型">{{ reasonTypeLabel(auditRow.reasonType) }}</el-descriptions-item>
        <el-descriptions-item label="说明">
          <div class="reason-desc-cell">
            <span>{{ reportDescPreview(auditRow.reasonDesc) }}</span>
            <el-button
              v-if="shouldExpandReportDesc(auditRow.reasonDesc)"
              link
              type="primary"
              size="small"
              class="reason-desc-expand"
              @click="openReportDescDialog(auditRow)"
            >
              展开
            </el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <el-form :model="auditForm" label-width="110px" @submit.prevent>
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="auditForm.decision">
            <el-radio :label="1">成立（扣分）</el-radio>
            <el-radio :label="2">不成立（驳回）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="auditForm.decision === 1" label="自定义扣分">
          <el-input-number
            v-model="auditForm.customDelta"
            :min="1" :max="30"
            style="width:170px"
            placeholder="不填自动计算"
          />
          <span class="form-hint">不填则按举报类型自动计算</span>
        </el-form-item>
        <el-form-item label="管理员备注">
          <el-input
            v-model="auditForm.adminRemark"
            type="textarea" :rows="3"
            maxlength="500" show-word-limit
            placeholder="驳回时建议填写原因"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button
          type="primary" :loading="auditSubmitting"
          @click="handleAudit"
        >确认提交</el-button>
      </template>
    </el-dialog>

    <!-- 证据图片弹窗 -->
    <el-dialog v-model="evidenceVisible" title="证据图片" width="560px">
      <div class="evidence-wrap">
        <el-image
          v-for="(url, idx) in evidenceList"
          :key="idx"
          :src="url"
          fit="contain"
          style="width:240px;height:180px;margin:6px"
          :preview-src-list="evidenceList"
        />
      </div>
    </el-dialog>

    <el-dialog v-model="reportDescDialogVisible" title="举报说明" width="560px" append-to-body>
      <div class="report-desc-dialog">
        <div class="report-desc-dialog__meta">{{ reportDescDialogMeta }}</div>
        <div class="report-desc-dialog__content">{{ reportDescDialogContent }}</div>
      </div>
    </el-dialog>

    <!-- 信用分调整弹窗 -->
    <el-divider />
    <div class="page-card" style="margin-top:0;padding-top:0;box-shadow:none">
      <h3 class="section-title">手动调整用户信用分</h3>
      <el-form :inline="true" :model="adjustForm">
        <el-form-item label="用户ID">
          <el-input v-model="adjustForm.userId" style="width:120px" />
        </el-form-item>
        <el-form-item label="身份">
          <el-select v-model="adjustForm.role" style="width:110px">
            <el-option label="买家" value="BUYER" />
            <el-option label="卖家" value="SELLER" />
          </el-select>
        </el-form-item>
        <el-form-item label="调整值">
          <el-input-number v-model="adjustForm.delta" :step="1" :min="-100" :max="100" style="width:130px" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="adjustForm.remark" style="width:200px" placeholder="调整原因" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :loading="adjustSubmitting" @click="handleAdjust">
            确认调整
          </el-button>
        </el-form-item>
      </el-form>
    </div>

  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  adminListReportsApi,
  adminAuditReportApi,
  adminCreditAdjustApi,
  getUserCreditApi,
} from "@/api/credit";

// -------- 列表 --------
const loading = ref(false);
const records = ref([]);
const total = ref(0);
const query = reactive({
  page: 1,
  size: 10,
  status: null,
  reportedId: null,
});
const REPORT_DESC_PREVIEW_LIMIT = 58;

onMounted(() => load());

async function load() {
  loading.value = true;
  try {
    const res = await adminListReportsApi(
      query.page, query.size,
      query.status,
      query.reportedId || null
    );
    records.value = res.data?.records || [];
    total.value = res.data?.total || 0;
  } catch {
    ElMessage.error("加载举报列表失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  load();
}

function reset() {
  query.status = null;
  query.reportedId = null;
  query.page = 1;
  load();
}

// -------- 审核 --------
const auditVisible = ref(false);
const auditSubmitting = ref(false);
const auditRow = ref({});
const auditForm = reactive({
  reportId: null,
  decision: 1,
  adminRemark: "",
  customDelta: null,
});
const reportDescDialogVisible = ref(false);
const reportDescDialogContent = ref("");
const reportDescDialogMeta = ref("");

function openAudit(row) {
  auditRow.value = row;
  auditForm.reportId = row.id;
  auditForm.decision = 1;
  auditForm.adminRemark = "";
  auditForm.customDelta = null;
  auditVisible.value = true;
}

async function handleAudit() {
  if (!auditForm.decision) {
    ElMessage.warning("请选择审核结果");
    return;
  }
  const label = auditForm.decision === 1 ? "成立并扣分" : "驳回";
  await ElMessageBox.confirm(`确认将此举报标记为「${label}」？`, "提示", { type: "warning" });
  auditSubmitting.value = true;
  try {
    await adminAuditReportApi({
      reportId: auditForm.reportId,
      decision: auditForm.decision,
      adminRemark: auditForm.adminRemark,
      customDelta: auditForm.decision === 1 ? auditForm.customDelta : null,
    });
    ElMessage.success("审核完成");
    auditVisible.value = false;
    load();
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || "审核失败");
  } finally {
    auditSubmitting.value = false;
  }
}

// -------- 证据 --------
const evidenceVisible = ref(false);
const evidenceList = ref([]);

function showEvidence(urls) {
  evidenceList.value = urls ? urls.split(",").map((u) => u.trim()) : [];
  evidenceVisible.value = true;
}

function shouldExpandReportDesc(value) {
  return String(value || "").length > REPORT_DESC_PREVIEW_LIMIT;
}

function reportDescPreview(value) {
  const text = String(value || "暂无补充说明");
  return text.length > REPORT_DESC_PREVIEW_LIMIT ? `${text.slice(0, REPORT_DESC_PREVIEW_LIMIT)}...` : text;
}

function openReportDescDialog(row) {
  reportDescDialogContent.value = row?.reasonDesc || "暂无补充说明";
  reportDescDialogMeta.value = `举报 #${row?.id || "-"} · ${reasonTypeLabel(row?.reasonType)} · 被举报用户 ${row?.reportedId || "-"}`;
  reportDescDialogVisible.value = true;
}

// -------- 信用分调整 --------
const adjustForm = reactive({
  userId: "",
  role: "BUYER",
  delta: 0,
  remark: "",
});
const adjustSubmitting = ref(false);

async function handleAdjust() {
  if (!adjustForm.userId) {
    ElMessage.warning("请填写用户ID");
    return;
  }
  const delta = Number(adjustForm.delta || 0);
  if (!Number.isInteger(delta)) {
    ElMessage.warning("调整值必须为整数");
    return;
  }
  if (delta === 0) {
    ElMessage.warning("调整值不能为0");
    return;
  }
  const currentScore = await loadAdjustCurrentScore();
  if (currentScore === null) {
    return;
  }
  const nextScore = currentScore + delta;
  if (nextScore < 0 || nextScore > 100) {
    ElMessage.warning(`当前${adjustRoleLabel(adjustForm.role)}信用分为 ${currentScore}，调整后为 ${nextScore}，必须介于 0-100 之间`);
    return;
  }
  await ElMessageBox.confirm(
    `确认对用户 ${adjustForm.userId} 的${adjustRoleLabel(adjustForm.role)}信用分调整 ${delta} 分？当前 ${currentScore}，调整后 ${nextScore}。`,
    "提示", { type: "warning" }
  );
  adjustSubmitting.value = true;
  try {
    await adminCreditAdjustApi(
      adjustForm.userId,
      adjustForm.role,
      delta,
      adjustForm.remark || "管理员手动调整"
    );
    ElMessage.success("调整成功");
    adjustForm.userId = "";
    adjustForm.delta = 0;
    adjustForm.remark = "";
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || "调整失败");
  } finally {
    adjustSubmitting.value = false;
  }
}

async function loadAdjustCurrentScore() {
  try {
    const res = await getUserCreditApi(adjustForm.userId);
    return scoreByRole(res.data, adjustForm.role);
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || "获取当前信用分失败");
    return null;
  }
}

function scoreByRole(credit, role) {
  if (role === "SELLER") {
    return Number(credit?.shopScore ?? 100);
  }
  if (role === "SH_SELLER") {
    return Number(credit?.shSellerScore ?? 100);
  }
  return Number(credit?.buyerScore ?? 100);
}

function adjustRoleLabel(role) {
  if (role === "SELLER") return "卖家";
  if (role === "SH_SELLER") return "二手卖家";
  return "买家";
}

// -------- 工具函数 --------
function reasonTypeLabel(type) {
  const map = {
    FRAUD: "诈骗/虚假交易",
    FAKE_ITEM: "商品与描述不符",
    BAD_ATTITUDE: "态度恶劣/骚扰",
    REFUND_ABUSE: "恶意退款",
    SPAM: "刷单/广告骚扰",
    OTHER: "其他",
  };
  return map[type] || type;
}

function reportStatusLabel(status) {
  return ["待审核", "已成立", "已驳回"][status] ?? "-";
}

function reportStatusClass(status) {
  return ["status-pending", "status-danger", "status-muted"][status] ?? "status-muted";
}
</script>

<style scoped>
.query-form {
  margin-bottom: 16px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.text-muted {
  color: #909399;
  font-size: 13px;
}
.evidence-wrap {
  display: flex;
  flex-wrap: wrap;
}
.reason-desc-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}
.reason-desc-cell span {
  min-width: 0;
  overflow-wrap: anywhere;
  line-height: 1.5;
}
.reason-desc-expand {
  flex: 0 0 auto;
  height: 22px;
  padding: 0;
}
.report-desc-dialog {
  display: grid;
  gap: 12px;
}
.report-desc-dialog__meta {
  color: #909399;
  font-size: 13px;
}
.report-desc-dialog__content {
  max-height: 360px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  padding: 12px;
  color: #1f2937;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.form-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
}
</style>
