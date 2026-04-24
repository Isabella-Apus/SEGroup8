<template>
  <div class="page-card">
    <h2 class="page-title">我的信用</h2>

    <el-skeleton v-if="loading" :rows="8" animated />

    <template v-else-if="credit">

      <!-- ========== 商家：账户健康 ========== -->
      <template v-if="isSeller">
        <div class="credit-section">
          <h3 class="section-title">账户健康</h3>
          <div class="score-bar-wrap">
            <div class="score-bar">
              <div
                v-for="lv in levels"
                :key="lv"
                class="bar-segment"
                :class="{ active: credit.sellerLevel === lv }"
              >{{ lv }}</div>
            </div>
            <div class="score-num">{{ credit.sellerScore }} 分</div>
          </div>

          <el-descriptions :column="2" border class="credit-desc">
            <el-descriptions-item label="健康等级">
              <el-tag :type="levelTagType(credit.sellerLevel)">{{ credit.sellerLevel }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="好评率">
              {{ credit.sellerGoodRate }}%
            </el-descriptions-item>
            <el-descriptions-item label="累计售出">
              {{ credit.sellerSoldCount }} 单
            </el-descriptions-item>
            <el-descriptions-item label="好评数">
              {{ credit.sellerGoodReviewCount }} 条
            </el-descriptions-item>
            <el-descriptions-item label="近2年纠纷">
              {{ credit.sellerDisputeCount }} 次
            </el-descriptions-item>
          </el-descriptions>

          <div class="log-title">近期变动记录</div>
          <el-table :data="credit.sellerLogs" border size="small" style="margin-top:6px">
            <el-table-column prop="createTime" label="时间" min-width="160" />
            <el-table-column prop="reasonDesc" label="原因" min-width="200" />
            <el-table-column label="变动" width="90">
              <template #default="{ row }">
                <span :class="row.delta >= 0 ? 'delta-plus' : 'delta-minus'">
                  {{ row.delta >= 0 ? '+' + row.delta : row.delta }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <!-- ========== 普通用户：个人信用 ========== -->
      <template v-else>
        <div class="credit-section">
          <h3 class="section-title">个人信用</h3>
          <p class="section-tip">您的信用分由与您交易的买家和卖家共同影响</p>
          <div class="score-bar-wrap">
            <div class="score-bar buyer">
              <div
                v-for="lv in levels"
                :key="lv"
                class="bar-segment"
                :class="{ active: credit.buyerLevel === lv }"
              >{{ lv }}</div>
            </div>
            <div class="score-num">{{ credit.buyerScore }} 分</div>
          </div>

          <el-descriptions :column="2" border class="credit-desc">
            <el-descriptions-item label="信用等级">
              <el-tag :type="levelTagType(credit.buyerLevel)">{{ credit.buyerLevel }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="好评率">
              {{ credit.buyerGoodRate }}%
            </el-descriptions-item>
            <el-descriptions-item label="累计交易">
              {{ credit.buyerOrderCount }} 单
            </el-descriptions-item>
            <el-descriptions-item label="好评数">
              {{ credit.buyerGoodReviewCount }} 条
            </el-descriptions-item>
            <el-descriptions-item label="近2年纠纷">
              {{ credit.buyerDisputeCount }} 次
            </el-descriptions-item>
          </el-descriptions>

          <div class="log-title">近期变动记录</div>
          <el-table :data="credit.buyerLogs" border size="small" style="margin-top:6px">
            <el-table-column prop="createTime" label="时间" min-width="160" />
            <el-table-column prop="reasonDesc" label="原因" min-width="200" />
            <el-table-column label="变动" width="90">
              <template #default="{ row }">
                <span :class="row.delta >= 0 ? 'delta-plus' : 'delta-minus'">
                  {{ row.delta >= 0 ? '+' + row.delta : row.delta }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </template>

      <el-divider />

      <!-- ========== 我的举报 ========== -->
      <div class="credit-section">
        <div class="section-header">
          <h3 class="section-title" style="margin:0">我的举报记录</h3>
          <el-button type="primary" size="small" @click="reportDialogVisible = true">
            发起举报
          </el-button>
        </div>

        <el-table :data="myReports" border size="small" style="margin-top:10px">
          <el-table-column prop="reportedId" label="被举报用户ID" width="130" />
          <el-table-column prop="reasonType" label="举报类型" min-width="130">
            <template #default="{ row }">{{ reasonTypeLabel(row.reasonType) }}</template>
          </el-table-column>
          <el-table-column prop="reasonDesc" label="说明" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="reportStatusType(row.status)" size="small">
                {{ reportStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" min-width="160" />
        </el-table>
      </div>

      <el-divider />

      <!-- ========== 我的拉黑 ========== -->
      <div class="credit-section">
        <h3 class="section-title">我的拉黑列表</h3>
        <el-empty v-if="blockList.length === 0" description="暂无拉黑用户" />
        <el-table v-else :data="blockList" border size="small">
          <el-table-column prop="blockedId" label="被拉黑用户ID" min-width="140" />
          <el-table-column prop="createTime" label="拉黑时间" min-width="180" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button
                type="danger" size="small" text
                @click="handleUnblock(row.blockedId)"
              >取消拉黑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

    </template>

    <!-- ========== 举报弹窗 ========== -->
    <el-dialog v-model="reportDialogVisible" title="发起举报" width="500px">
      <el-form :model="reportForm" label-width="100px">
        <el-form-item label="被举报用户ID" required>
          <el-input-number
            v-model="reportForm.reportedId"
            :min="1" style="width:200px"
            placeholder="请输入用户ID"
          />
        </el-form-item>
        <el-form-item label="举报类型" required>
          <el-select v-model="reportForm.reasonType" style="width:200px">
            <el-option label="诈骗/虚假交易" value="FRAUD" />
            <el-option label="商品与描述不符" value="FAKE_ITEM" />
            <el-option label="态度恶劣/骚扰" value="BAD_ATTITUDE" />
            <el-option label="恶意退款" value="REFUND_ABUSE" />
            <el-option label="刷单/广告骚扰" value="SPAM" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input
            v-model="reportForm.reasonDesc"
            type="textarea" :rows="3"
            maxlength="500" show-word-limit
          />
        </el-form-item>
        <el-form-item label="证据图片URL">
          <el-input
            v-model="reportForm.evidenceUrls"
            placeholder="多张用逗号分隔"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportSubmitting" @click="handleSubmitReport">
          提交举报
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useUserStore } from "@/stores/user";
import {
  getMyCreditApi,
  getMyReportsApi,
  getMyBlockListApi,
  submitReportApi,
  unblockUserApi,
} from "@/api/credit";

const userStore = useUserStore();

// 判断是否是商家角色
const isSeller = computed(() => {
  const role = userStore.currentRole;
  return role === "SELLER" || role === "OFFICIAL_SELLER";
});

const loading = ref(false);
const credit = ref(null);
const myReports = ref([]);
const blockList = ref([]);

const levels = ["较差", "良好", "优秀", "极好"];

// 举报弹窗
const reportDialogVisible = ref(false);
const reportSubmitting = ref(false);
const reportForm = ref({
  reportedId: null,
  reasonType: "",
  reasonDesc: "",
  evidenceUrls: "",
});

onMounted(() => {
  loadAll();
});

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
  } catch (e) {
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
    ElMessage.success("举报已提交，等待管理员审核");
    reportDialogVisible.value = false;
    reportForm.value = { reportedId: null, reasonType: "", reasonDesc: "", evidenceUrls: "" };
    loadAll();
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || "举报提交失败");
  } finally {
    reportSubmitting.value = false;
  }
}

async function handleUnblock(blockedId) {
  await ElMessageBox.confirm("确认取消拉黑该用户？", "提示", { type: "warning" });
  try {
    await unblockUserApi(blockedId);
    ElMessage.success("已取消拉黑");
    loadAll();
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}

function levelTagType(level) {
  const map = { 较差: "danger", 良好: "warning", 优秀: "", 极好: "success" };
  return map[level] || "";
}

function reportStatusLabel(status) {
  return ["待审核", "已成立", "已驳回"][status] ?? "-";
}

function reportStatusType(status) {
  return ["warning", "danger", "info"][status] ?? "";
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
  return map[type] || type;
}
</script>

<style scoped>
.credit-section {
  margin-bottom: 8px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #303133;
}
.section-tip {
  font-size: 13px;
  color: #909399;
  margin: 0 0 12px 0;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.score-bar-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}
.score-bar {
  display: flex;
  border-radius: 20px;
  overflow: hidden;
  background: #e8f4ff;
  height: 36px;
}
.bar-segment {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #909399;
  cursor: default;
  padding: 0 12px;
  transition: background 0.2s;
}
.score-bar .bar-segment.active {
  background: #409eff;
  color: #fff;
  font-weight: 600;
  border-radius: 20px;
}
.score-bar.buyer .bar-segment.active {
  background: #e6a23c;
}
.score-num {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  min-width: 70px;
}
.credit-desc {
  margin-bottom: 12px;
}
.log-title {
  font-size: 13px;
  color: #606266;
  margin-top: 10px;
}
.delta-plus {
  color: #67c23a;
  font-weight: 600;
}
.delta-minus {
  color: #f56c6c;
  font-weight: 600;
}
</style>