<template>
  <section class="credit-page">
    <div class="credit-hero">
      <div>
        <span>Credit Center</span>
        <h1>我的信用</h1>
        <p>买家信用、二手卖家信用和举报记录集中查看，交易前后都更清楚。</p>
      </div>
      <el-button type="primary" @click="reportDialogVisible = true">发起举报</el-button>
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
                <small>用户 {{ item.reportedId }} · {{ tradeContextLabel(item.tradeContext) }}</small>
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
              <strong>用户 {{ item.blockedId }}</strong>
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
        <el-form-item label="被举报用户ID" required>
          <el-input-number v-model="reportForm.reportedId" :min="1" style="width: 220px" />
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
        <el-form-item label="证据图片URL">
          <el-input v-model="reportForm.evidenceUrls" placeholder="多张用逗号分隔" />
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
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  getMyBlockListApi,
  getMyCreditApi,
  getMyReportsApi,
  submitReportApi,
  unblockUserApi,
} from "@/api/credit";

const loading = ref(false);
const credit = ref(null);
const myReports = ref([]);
const blockList = ref([]);
const reportDialogVisible = ref(false);
const reportSubmitting = ref(false);
const reportForm = ref(defaultReportForm());

const buyerLogs = computed(() => credit.value?.buyerLogs || []);
const sellerLogs = computed(() => credit.value?.shSellerLogs || []);
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

onMounted(loadAll);

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
  } catch {
    ElMessage.error("加载信用信息失败");
  } finally {
    loading.value = false;
  }
}

async function handleSubmitReport() {
  if (!reportForm.value.reportedId) {
    ElMessage.warning("请填写被举报用户ID");
    return;
  }
  if (!reportForm.value.reasonType) {
    ElMessage.warning("请选择举报类型");
    return;
  }
  reportSubmitting.value = true;
  try {
    await submitReportApi(reportForm.value);
    ElMessage.success("举报已提交");
    reportDialogVisible.value = false;
    reportForm.value = defaultReportForm();
    loadAll();
  } finally {
    reportSubmitting.value = false;
  }
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
    tradeContext: "SH_BUYER",
    reasonType: "",
    reasonDesc: "",
    evidenceUrls: "",
  };
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
