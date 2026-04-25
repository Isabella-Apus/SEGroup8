<template>
  <div class="page-card">
    <h2 class="page-title">二手商品详情</h2>
    <el-skeleton v-if="loading" :rows="6" animated />

    <div v-else-if="item" class="detail-wrap">
      <div class="cover-box">
        <el-image v-if="item.cover" :src="toFullImageUrl(item.cover)" fit="cover" class="cover-image" />
        <div v-else class="cover-placeholder">暂无图片</div>
      </div>

      <div class="info-box">
        <h3>{{ item.name }}</h3>
        <p class="price">￥{{ Number(item.salePrice || 0).toFixed(2) }}</p>
        <p class="origin">原价：￥{{ Number(item.originPrice || item.salePrice || 0).toFixed(2) }}</p>
        <p>成色：{{ item.conditionLevel || item.condition || "未知" }}</p>
        <p>状态：{{ item.statusName || "在售" }}</p>
        <p v-if="item.sellerName">卖家：{{ item.sellerName }}</p>
        <p class="desc">{{ item.description || "暂无商品描述" }}</p>

        <p class="desc">二手商品仅支持单件下单购买。</p>

        <el-space wrap>
          <el-button type="primary" @click="handleBuyNow" :disabled="!canBuy">立即购买</el-button>
          <el-button v-if="canChatWithSeller" type="success" plain @click="handleContactSeller">联系卖家</el-button>
          <el-button type="primary" plain @click="router.push('/secondhand/publish')">我也要发布</el-button>
          <el-button text @click="router.push('/secondhand')">返回</el-button>
        </el-space>

        <!-- 举报 / 拉黑（非本人商品时显示） -->
        <el-divider v-if="canChatWithSeller" />
        <el-space v-if="canChatWithSeller" wrap>
          <el-button type="warning" plain size="small" @click="openReportDialog">举报卖家</el-button>
          <el-button v-if="!isSellerBlocked" type="danger" plain size="small" @click="handleBlock">拉黑卖家</el-button>
          <el-button v-else type="info" plain size="small" @click="handleUnblock">取消拉黑</el-button>
        </el-space>
      </div>
    </div>

    <p v-else class="empty-tip">二手商品不存在</p>

    <!-- ===== 举报弹窗 ===== -->
    <el-dialog v-model="reportDialogVisible" title="举报卖家" width="480px">
      <el-form :model="reportForm" label-width="90px">
        <el-form-item label="举报类型" required>
          <el-select v-model="reportForm.reasonType" style="width:100%">
            <el-option label="诈骗/虚假交易" value="FRAUD" />
            <el-option label="商品与描述不符" value="FAKE_ITEM" />
            <el-option label="态度恶劣/骚扰" value="BAD_ATTITUDE" />
            <el-option label="刷单/广告骚扰" value="SPAM" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="补充说明">
          <el-input v-model="reportForm.reasonDesc" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="reportSubmitting" @click="handleSubmitReport">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { buySecondhandApi, getSecondhandDetailApi } from "@/api/secondhand";
import { submitReportApi, blockUserApi, unblockUserApi, isBlockingApi, isBlockedByApi } from "@/api/credit";
import { getUser } from "@/utils/storage";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const item = ref(null);

// 举报弹窗
const reportDialogVisible = ref(false);
const reportSubmitting = ref(false);
const reportForm = ref({ reasonType: "", reasonDesc: "" });

// 拉黑状态
const isSellerBlocked = ref(false);

const canBuy = computed(() => !!item.value && Number(item.value.status || 1) === 1);
const canChatWithSeller = computed(() => {
  if (!item.value?.sellerUserId) return false;
  return Number(item.value.sellerUserId) !== Number(getUser()?.id);
});

onMounted(async () => {
  await fetchDetail();
});

async function fetchDetail() {
  loading.value = true;
  try {
    const result = await getSecondhandDetailApi(route.params.id);
    item.value = result.data;

    // 拉黑检查：任意一方拉黑对方，直接跳回列表
    const sellerUserId = item.value?.sellerUserId;
    const currentUserId = getUser()?.id;
    if (sellerUserId && currentUserId && Number(sellerUserId) !== Number(currentUserId)) {
      const [iBlocked, blockedMe] = await Promise.all([
        isBlockingApi(sellerUserId),
        isBlockedByApi(sellerUserId),
      ]);
      if (iBlocked.data || blockedMe.data) {
        ElMessage.warning("该商品不可访问");
        router.replace("/secondhand");
        return;
      }
      // 记录当前是否已拉黑该卖家（用于按钮切换）
      isSellerBlocked.value = iBlocked.data === true;
    }
  } finally {
    loading.value = false;
  }
}

async function handleBuyNow() {
  if (!canBuy.value) {
    ElMessage.warning("当前商品暂不可购买");
    return;
  }
  await buySecondhandApi(item.value.id, {});
  ElMessage.success("购买成功");
  router.push("/order");
}

function handleContactSeller() {
  if (!item.value?.sellerUserId) {
    ElMessage.warning("当前无法联系卖家");
    return;
  }
  router.push({
    path: "/messages",
    query: {
      participantId: item.value.sellerUserId,
      sourceType: "SECONDHAND",
      sourceId: item.value.id,
    },
  });
}

function openReportDialog() {
  reportForm.value = { reasonType: "", reasonDesc: "" };
  reportDialogVisible.value = true;
}

async function handleSubmitReport() {
  if (!reportForm.value.reasonType) {
    ElMessage.warning("请选择举报类型");
    return;
  }
  reportSubmitting.value = true;
  try {
    await submitReportApi({
      reportedId: item.value.sellerUserId,
      reasonType: reportForm.value.reasonType,
      reasonDesc: reportForm.value.reasonDesc,
    });
    ElMessage.success("举报已提交，等待管理员审核");
    reportDialogVisible.value = false;
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || "举报提交失败");
  } finally {
    reportSubmitting.value = false;
  }
}

async function handleBlock() {
  try {
    await ElMessageBox.confirm(
      `确认拉黑卖家「${item.value.sellerName || item.value.sellerUserId}」？拉黑后对方无法与你发起会话。`,
      "拉黑确认",
      { type: "warning", confirmButtonText: "确认拉黑", cancelButtonText: "取消" }
    );
    await blockUserApi(item.value.sellerUserId);
    isSellerBlocked.value = true;
    ElMessage.success("已拉黑该卖家");
  } catch (e) {
    if (e === "cancel" || e?.toString?.().includes("cancel")) return;
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}

async function handleUnblock() {
  try {
    await ElMessageBox.confirm(
      `确认取消拉黑卖家「${item.value.sellerName || item.value.sellerUserId}」？`,
      "取消拉黑",
      { type: "warning", confirmButtonText: "确认取消", cancelButtonText: "取消" }
    );
    await unblockUserApi(item.value.sellerUserId);
    isSellerBlocked.value = false;
    ElMessage.success("已取消拉黑");
  } catch (e) {
    if (e === "cancel" || e?.toString?.().includes("cancel")) return;
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}

function toFullImageUrl(url) {
  if (!url) return "";
  if (url.startsWith("http://") || url.startsWith("https://")) return url;
  const normalized = url.startsWith("/") ? url : `/${url}`;
  return `http://localhost:8080${normalized}`;
}
</script>

<style scoped>
.detail-wrap {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 24px;
}

.cover-box {
  width: 280px;
  height: 280px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  background: #f9fafb;
}

.price {
  color: #ef4444;
  font-size: 22px;
  font-weight: 700;
  margin: 8px 0;
}

.origin {
  color: #6b7280;
}

.desc {
  color: #4b5563;
  line-height: 1.8;
}

.actions {
  margin: 14px 0;
}

@media (max-width: 900px) {
  .detail-wrap {
    grid-template-columns: 1fr;
  }

  .cover-box {
    width: 100%;
    max-width: 360px;
    margin: 0 auto;
  }
}
</style>