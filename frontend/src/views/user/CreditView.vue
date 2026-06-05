<template>
  <section class="credit-page">
    <div class="credit-hero">
      <div>
        <span>Credit Center</span>
        <h1>我的信用</h1>
        <p>查看我的买家信用、二手卖家信用和举报记录。</p>
      </div>
      <el-button type="primary" @click="openReportDialog()">发起举报</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="10" animated class="page-card" />

    <template v-else-if="credit">
      <div class="score-grid">
        <article v-for="card in scoreCards" :key="card.key" class="score-card">
          <div>
            <span class="score-label">{{ card.label }}</span>
            <strong>{{ card.score }}</strong>
            <small>{{ card.level }}</small>
          </div>
          <el-progress type="dashboard" :percentage="card.score" :width="96" :stroke-width="10" :color="card.color" />
        </article>
      </div>

      <div class="content-grid">
        <article class="panel">
          <div class="panel-head">
            <h2>近期信用变动</h2>
            <span>买家 / 二手卖家</span>
          </div>
          <div class="log-columns">
            <div class="log-box">
              <h3>买家信用</h3>
              <div v-if="buyerLogs.length" class="log-list">
                <div v-for="item in buyerLogs" :key="item.id" class="log-item">
                  <span :class="item.delta >= 0 ? 'plus' : 'minus'">{{ signedDelta(item.delta) }}</span>
                  <div>
                    <strong>{{ item.reasonDesc || "信用调整" }}</strong>
                    <small>{{ formatTime(item.createTime) }}</small>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无变动" />
            </div>

            <div class="log-box">
              <h3>二手卖家信用</h3>
              <div v-if="sellerLogs.length" class="log-list">
                <div v-for="item in sellerLogs" :key="item.id" class="log-item">
                  <span :class="item.delta >= 0 ? 'plus' : 'minus'">{{ signedDelta(item.delta) }}</span>
                  <div>
                    <strong>{{ item.reasonDesc || "信用调整" }}</strong>
                    <small>{{ formatTime(item.createTime) }}</small>
                  </div>
                </div>
              </div>
              <el-empty v-else description="暂无变动" />
            </div>
          </div>
        </article>

        <article class="panel">
          <div class="panel-head">
            <h2>我的举报记录</h2>
            <span>{{ myReports.length }} 条</span>
          </div>
          <div v-if="myReports.length" class="report-list">
            <div v-for="item in myReports" :key="item.id" class="report-item">
              <div>
                <strong>{{ reasonTypeLabel(item.reasonType) }}</strong>
                <p>{{ item.reasonDesc || "暂无补充说明" }}</p>
                <small>{{ formatReportedUser(item) }}  {{ tradeContextLabel(item.tradeContext) }}</small>
              </div>
              <el-tag :type="reportStatusType(item.status)" size="small">{{ reportStatusLabel(item.status) }}</el-tag>
            </div>
          </div>
          <el-empty v-else description="暂无举报记录" />
        </article>
      </div>

      <article class="panel">
        <div class="panel-head">
          <h2>我的拉黑列表</h2>
          <span>{{ blockList.length }} 人</span>
        </div>
        <div v-if="blockList.length" class="block-grid">
          <div v-for="item in blockList" :key="item.id || item.blockedId" class="block-item">
            <div>
              <strong>{{ formatBlockedUser(item.blockedId) }}</strong>
              <small>{{ formatTime(item.createTime) }}</small>
            </div>
            <el-button type="danger" plain size="small" @click="handleUnblock(item.blockedId)">取消拉黑</el-button>
          </div>
        </div>
        <el-empty v-else description="暂无拉黑用户" />
      </article>
    </template>

    <el-dialog v-model="reportDialogVisible" title="发起举报" width="520px" align-center append-to-body>
      <el-form :model="reportForm" label-width="110px">
        <el-form-item label="被举报用户" required>
          <el-select
            v-model="reportForm.reportedId"
            class="reported-user-select"
            filterable
            remote
            reserve-keyword
            clearable
            :remote-method="searchReportUsers"
            :loading="searchingReportUsers"
            placeholder="输入用户ID或昵称搜索"
            @change="handleReportedUserChange"
          >
            <el-option
              v-for="user in reportUserOptions"
              :key="user.id"
              :label="formatUserOption(user)"
              :value="user.id"
            >
              <div class="reported-user-option">
                <strong>{{ user.id }} - {{ user.nickname || user.username || "未命名用户" }}</strong>
                <span>{{ user.username }} · {{ user.role || "USER" }}</span>
              </div>
            </el-option>
          </el-select>
          <div v-if="reportedUserLabel" class="reported-user-preview">已选择：{{ reportedUserLabel }}</div>
        </el-form-item>
        <el-form-item label="举报对象" required>
          <el-select v-model="reportForm.tradeContext" style="width: 260px">
            <el-option label="店铺卖家" value="SHOP" />
            <el-option label="二手卖家" value="SH_BUYER" />
            <el-option label="二手买家" value="SH_SELLER" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报类型" required>
          <el-select v-model="reportForm.reasonType" style="width: 260px">
            <el-option label="诈骗/虚假交易" value="FRAUD" />
            <el-option label="商品与描述不符" value="FAKE_ITEM" />
            <el-option label="态度恶劣/骚扰" value="BAD_ATTITUDE" />
            <el-option label="恶意退款" value="REFUND_ABUSE" />
            <el-option label="刷单/广告骚扰" value="SPAM" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="reportForm.reasonDesc" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="证据图片">
          <input
            ref="evidenceInputRef"
            class="evidence-input"
            type="file"
            accept="image/*"
            multiple
            @change="handleEvidenceSelected"
          />
          <div class="evidence-uploader">
            <el-button :loading="evidenceUploading" @click="openEvidencePicker">上传图片</el-button>
            <span>可上传多张图片，提交时会自动作为证据保存</span>
          </div>
          <div v-if="evidenceImageList.length" class="evidence-preview-grid">
            <div v-for="url in evidenceImageList" :key="url" class="evidence-preview-item">
              <el-image
                :src="toAssetUrl(url)"
                :preview-src-list="evidencePreviewUrls"
                fit="cover"
                preview-teleported
              />
              <el-button link type="danger" @click="removeEvidenceImage(url)">移除</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportSubmitting" @click="handleSubmitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute } from "vue-router";
import {
  getMyBlockListApi,
  getMyCreditApi,
  getMyReportsApi,
  submitReportApi,
  unblockUserApi,
} from "@/api/credit";
import { searchUsersApi } from "@/api/user";
import { uploadImageApi } from "@/api/upload";
import { toAssetUrl } from "@/utils/url";

const route = useRoute();
const loading = ref(false);
const credit = ref(null);
const myReports = ref([]);
const blockList = ref([]);
const reportDialogVisible = ref(false);
const reportSubmitting = ref(false);
const reportForm = ref(defaultReportForm());
const evidenceInputRef = ref(null);
const evidenceUploading = ref(false);
const reportUserOptions = ref([]);
const searchingReportUsers = ref(false);
const userNameMap = ref({});
const REPORT_TARGET_NAMES_KEY = "segroup8_report_target_names";

const buyerLogs = computed(() => credit.value?.buyerLogs || []);
const sellerLogs = computed(() => credit.value?.shSellerLogs || []);
const evidenceImageList = computed(() =>
  String(reportForm.value.evidenceUrls || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean),
);
const evidencePreviewUrls = computed(() => evidenceImageList.value.map((url) => toAssetUrl(url)));
const reportedUserLabel = computed(() => {
  const id = reportForm.value.reportedId;
  if (!id) {
    return "";
  }
  const selected = getSelectedReportUser();
  const name = selected?.nickname || selected?.username || "";
  return name ? `${id} - ${name}` : `用户 ${id}`;
});
const scoreCards = computed(() => [
  {
    key: "buyer",
    label: "买家信用",
    score: Number(credit.value?.buyerScore ?? 100),
    level: credit.value?.buyerLevel || "极好",
    color: "#89c7ff",
  },
  {
    key: "secondhand",
    label: "二手卖家信用",
    score: Number(credit.value?.shSellerScore ?? 100),
    level: credit.value?.shSellerLevel || "极好",
    color: "#5fe6bd",
  },
  {
    key: "shop",
    label: "店铺信用",
    score: Number(credit.value?.shopScore ?? 100),
    level: credit.value?.shopLevel || "极好",
    color: "#ffb9d6",
  },
]);

onMounted(async () => {
  await applyReportQuery();
  await loadAll();
});

watch(
  () => route.query,
  () => applyReportQuery(),
);

async function loadAll() {
  loading.value = true;
  try {
    const [creditRes, reportRes, blockRes] = await Promise.all([
      getMyCreditApi(),
      getMyReportsApi(1, 20),
      getMyBlockListApi(),
    ]);
    credit.value = creditRes.data;
    myReports.value = reportRes.data?.records || [];
    blockList.value = blockRes.data || [];
    await hydrateBlockedUserNames(blockList.value);
  } catch {
    ElMessage.error("加载信用信息失败");
  } finally {
    loading.value = false;
  }
}

function openReportDialog(overrides = {}) {
  reportForm.value = {
    ...reportForm.value,
    ...overrides,
  };
  reportDialogVisible.value = true;
}

async function applyReportQuery() {
  const reportedId = Number(route.query.reportUserId || 0);
  if (!reportedId) {
    return;
  }
  const reportedName = String(route.query.reportUserName || "").trim();
  const tradeContext = String(route.query.reportContext || reportForm.value.tradeContext || "SHOP");
  await fetchReportUserByKeyword(String(reportedId));
  if (!reportUserOptions.value.some((item) => Number(item.id) === reportedId) && reportedName) {
    addReportUserOption({
      id: reportedId,
      nickname: reportedName,
      username: "",
      role: "",
    });
  }
  openReportDialog({
    reportedId,
    tradeContext,
  });
}

async function searchReportUsers(keyword) {
  await fetchReportUserByKeyword(keyword);
}

async function fetchReportUserByKeyword(keyword) {
  searchingReportUsers.value = true;
  try {
    const result = await searchUsersApi(keyword || "");
    reportUserOptions.value = (result.data || []).map(normalizeReportUser);
    rememberUserNames(reportUserOptions.value);
  } finally {
    searchingReportUsers.value = false;
  }
}

async function hydrateBlockedUserNames(records) {
  const ids = Array.from(new Set((records || [])
    .map((item) => Number(item.blockedId))
    .filter(Boolean)));
  const missingIds = ids.filter((id) => !userNameMap.value[id]);
  if (!missingIds.length) {
    return;
  }
  const users = [];
  for (const id of missingIds) {
    try {
      const result = await searchUsersApi(String(id));
      const exact = (result.data || []).find((item) => Number(item.id) === Number(id));
      if (exact) {
        users.push(normalizeReportUser(exact));
      }
    } catch {
      // Keep the ID fallback if a user can no longer be resolved.
    }
  }
  rememberUserNames(users);
}

function handleReportedUserChange(userId) {
  const user = reportUserOptions.value.find((item) => Number(item.id) === Number(userId));
  if (user) {
    rememberReportTargetName(user.id, user.nickname || user.username || "");
  }
}

function addReportUserOption(user) {
  const normalized = normalizeReportUser(user);
  const index = reportUserOptions.value.findIndex((item) => Number(item.id) === Number(normalized.id));
  if (index >= 0) {
    reportUserOptions.value.splice(index, 1, { ...reportUserOptions.value[index], ...normalized });
    return;
  }
  reportUserOptions.value.unshift(normalized);
}

function normalizeReportUser(user) {
  return {
    id: Number(user?.id),
    username: user?.username || "",
    nickname: user?.nickname || "",
    avatar: user?.avatar || "",
    role: user?.role || "",
  };
}

function rememberUserNames(users) {
  if (!users.length) {
    return;
  }
  const next = { ...userNameMap.value };
  users.forEach((user) => {
    if (user?.id) {
      next[user.id] = user.nickname || user.username || "";
    }
  });
  userNameMap.value = next;
}

function getSelectedReportUser() {
  return reportUserOptions.value.find((item) => Number(item.id) === Number(reportForm.value.reportedId)) || null;
}

function formatUserOption(user) {
  return `${user.id} - ${user.nickname || user.username || "未命名用户"}`;
}

async function handleSubmitReport() {
  if (!reportForm.value.reportedId) {
    ElMessage.warning("请选择被举报用户");
    return;
  }
  if (!reportForm.value.reasonType) {
    ElMessage.warning("请选择举报类型");
    return;
  }
  reportSubmitting.value = true;
  try {
    const payload = {
      reportedId: reportForm.value.reportedId,
      tradeContext: reportForm.value.tradeContext,
      reasonType: reportForm.value.reasonType,
      reasonDesc: reportForm.value.reasonDesc,
      evidenceUrls: reportForm.value.evidenceUrls,
    };
    await submitReportApi(payload);
    const selected = getSelectedReportUser();
    rememberReportTargetName(reportForm.value.reportedId, selected?.nickname || selected?.username || "");
    ElMessage.success("举报已提交");
    reportDialogVisible.value = false;
    reportForm.value = defaultReportForm();
    loadAll();
  } finally {
    reportSubmitting.value = false;
  }
}

function openEvidencePicker() {
  evidenceInputRef.value?.click();
}

async function handleEvidenceSelected(event) {
  const files = Array.from(event.target?.files || []);
  if (event.target) {
    event.target.value = "";
  }
  if (!files.length) {
    return;
  }
  evidenceUploading.value = true;
  try {
    const uploadedUrls = [];
    for (const file of files) {
      if (!String(file.type || "").startsWith("image/")) {
        ElMessage.warning(`${file.name} 不是图片文件，已跳过`);
        continue;
      }
      const result = await uploadImageApi(file);
      if (result.data?.url) {
        uploadedUrls.push(result.data.url);
      }
    }
    if (uploadedUrls.length) {
      const next = [...evidenceImageList.value, ...uploadedUrls];
      reportForm.value.evidenceUrls = Array.from(new Set(next)).join(",");
    }
  } finally {
    evidenceUploading.value = false;
  }
}

function removeEvidenceImage(url) {
  reportForm.value.evidenceUrls = evidenceImageList.value
    .filter((item) => item !== url)
    .join(",");
}

async function handleUnblock(blockedId) {
  await ElMessageBox.confirm("确认取消拉黑该用户？", "提示", { type: "warning" });
  await unblockUserApi(blockedId);
  ElMessage.success("已取消拉黑");
  loadAll();
}

function defaultReportForm() {
  return {
    reportedId: null,
    reportedName: "",
    tradeContext: "SH_BUYER",
    reasonType: "",
    reasonDesc: "",
    evidenceUrls: "",
  };
}

function formatReportedUser(item) {
  const id = item?.reportedId;
  if (!id) {
    return "用户 -";
  }
  const name = item.reportedName || getRememberedReportTargetName(id);
  return name ? `用户 ${id} - ${name}` : `用户 ${id}`;
}

function formatBlockedUser(blockedId) {
  const id = Number(blockedId);
  const name = userNameMap.value[id] || getRememberedReportTargetName(id);
  return name ? `${id} - ${name}` : `用户 ${id}`;
}

function getRememberedReportTargetName(userId) {
  const map = readReportTargetNames();
  return map[String(userId)] || "";
}

function rememberReportTargetName(userId, name) {
  const normalizedName = String(name || "").trim();
  if (!userId || !normalizedName) {
    return;
  }
  const map = readReportTargetNames();
  map[String(userId)] = normalizedName;
  localStorage.setItem(REPORT_TARGET_NAMES_KEY, JSON.stringify(map));
}

function readReportTargetNames() {
  try {
    return JSON.parse(localStorage.getItem(REPORT_TARGET_NAMES_KEY) || "{}") || {};
  } catch {
    return {};
  }
}

function signedDelta(value) {
  const delta = Number(value || 0);
  return delta >= 0 ? `+${delta}` : String(delta);
}

function formatTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString("zh-CN", { hour12: false });
}

function reportStatusLabel(status) {
  return ["待审核", "已成立", "已驳回"][Number(status)] || "待审核";
}

function reportStatusType(status) {
  return ["warning", "danger", "info"][Number(status)] || "warning";
}

function reasonTypeLabel(type) {
  const map = {
    FRAUD: "诈骗/虚假交易",
    FAKE_ITEM: "商品与描述不符",
    BAD_ATTITUDE: "态度恶劣/骚扰",
    REFUND_ABUSE: "恶意退款",
    SPAM: "刷单/广告骚扰",
    OTHER: "其他",
  };
  return map[type] || type || "其他";
}

function tradeContextLabel(ctx) {
  const map = {
    SHOP: "店铺卖家",
    SH_BUYER: "二手卖家",
    SH_SELLER: "二手买家",
  };
  return map[ctx] || ctx || "-";
}
</script>

<style scoped>
.credit-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.credit-hero {
  min-height: 160px;
  border: 1px solid rgba(137, 199, 255, 0.36);
  border-radius: 8px;
  padding: 22px;
  background: linear-gradient(135deg, #e9fff8 0%, #eaf4ff 54%, #fff7fb 100%);
  box-shadow: var(--shadow-soft);
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.credit-hero span {
  color: var(--brand-primary);
  font-weight: 900;
}

.credit-hero h1 {
  margin: 10px 0 8px;
  font-size: 34px;
}

.credit-hero p {
  max-width: 680px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
  font-weight: 700;
}

.score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.score-card,
.panel {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  box-shadow: var(--shadow-soft);
}

.score-card {
  min-height: 138px;
  padding: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.score-label,
.score-card small {
  display: block;
  color: var(--text-secondary);
  font-weight: 800;
}

.score-card strong {
  display: block;
  margin: 8px 0 4px;
  color: var(--text-main);
  font-size: 34px;
  line-height: 1;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.8fr);
  gap: 12px;
}

.panel {
  padding: 16px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
}

.panel-head h2,
.log-box h3 {
  margin: 0;
}

.panel-head h2 {
  font-size: 20px;
}

.panel-head span {
  color: var(--text-muted);
  font-weight: 800;
}

.log-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.log-box {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  padding: 14px;
  background: var(--surface-soft);
}

.log-box h3 {
  font-size: 16px;
  margin-bottom: 12px;
}

.log-list,
.report-list,
.block-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.log-item,
.report-item,
.block-item {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 12px;
}

.log-item,
.block-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.log-item > span {
  width: 46px;
  height: 34px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  font-weight: 900;
}

.plus {
  color: #0f8f70;
  background: #e9fff8;
}

.minus {
  color: #d12f64;
  background: #fff1f7;
}

.log-item strong,
.block-item strong {
  display: block;
  color: var(--text-main);
}

.log-item small,
.block-item small {
  display: block;
  margin-top: 4px;
  color: var(--text-muted);
}

.report-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.report-item strong {
  display: block;
}

.report-item p {
  margin: 6px 0;
  color: var(--text-secondary);
  line-height: 1.55;
}

.report-item small {
  color: var(--text-muted);
}

.reported-user-select {
  width: 100%;
}

.reported-user-option {
  display: grid;
  gap: 2px;
  line-height: 1.35;
}

.reported-user-option strong {
  color: var(--text-main);
}

.reported-user-option span {
  color: var(--text-muted);
  font-size: 12px;
}

.reported-user-preview {
  margin-top: 8px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 800;
}

.evidence-input {
  display: none;
}

.evidence-uploader {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.evidence-uploader span {
  color: var(--text-muted);
  font-size: 12px;
}

.evidence-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(92px, 1fr));
  gap: 10px;
  width: 100%;
  margin-top: 12px;
}

.evidence-preview-item {
  display: grid;
  gap: 4px;
  justify-items: center;
}

.evidence-preview-item :deep(.el-image) {
  width: 92px;
  height: 92px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  overflow: hidden;
  background: var(--surface-soft);
}

@media (max-width: 980px) {
  .score-grid,
  .content-grid,
  .log-columns {
    grid-template-columns: 1fr;
  }

  .credit-hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
