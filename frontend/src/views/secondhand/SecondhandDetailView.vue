<template>
  <section class="detail-page">
    <el-skeleton v-if="loading" :rows="8" animated class="page-card" />

    <div v-else-if="item" class="detail-shell">
      <div class="gallery">
        <el-image :src="selectedImage" fit="cover" class="main-image" />
        <div class="thumbs" v-if="imageList.length > 1">
          <button
            v-for="image in imageList"
            :key="image"
            class="thumb"
            :class="{ active: selectedImage === image }"
            type="button"
            @click="selectedImage = image"
          >
            <img :src="image" :alt="item.name" />
          </button>
        </div>
      </div>

      <div class="buy-panel">
        <div class="crumb">二手市场 / 个人闲置</div>
        <h1>{{ item.name }}</h1>
        <p class="desc">{{ item.description || "暂无商品描述" }}</p>

        <div class="price-box">
          <span>闲置价</span>
          <strong>¥{{ Number(item.salePrice || 0).toFixed(2) }}</strong>
          <em>原价 ¥{{ Number(item.originPrice || item.salePrice || 0).toFixed(2) }}</em>
        </div>

        <dl class="info-grid">
          <div>
            <dt>成色</dt>
            <dd>{{ item.conditionLevel || item.condition || "未知" }}</dd>
          </div>
          <div>
            <dt>状态</dt>
            <dd>{{ item.statusName || "在售" }}</dd>
          </div>
          <div>
            <dt>卖家</dt>
            <dd>{{ item.sellerName || item.sellerUserId || "个人卖家" }}</dd>
          </div>
        </dl>

        <div class="notice-box">
          二手商品默认单件交易，请先阅读描述并确认卖家信息。付款后可在订单页查看进度。
        </div>

        <div class="actions">
          <el-button type="warning" size="large" :disabled="!canBuy" @click="handleBuyNow">
            立即购买
          </el-button>
          <el-button v-if="canChatWithSeller" type="primary" size="large" @click="handleContactSeller">
            联系卖家
          </el-button>
          <el-button size="large" @click="router.push('/secondhand/publish')">我也要发布</el-button>
        </div>

        <div v-if="canChatWithSeller" class="risk-actions">
          <el-button type="warning" plain size="small" @click="openReportDialog">举报卖家</el-button>
          <el-button v-if="!isSellerBlocked" type="danger" plain size="small" @click="handleBlock">拉黑卖家</el-button>
          <el-button v-else type="info" plain size="small" @click="handleUnblock">取消拉黑</el-button>
        </div>
      </div>
    </div>

    <p v-else class="empty-tip page-card">二手商品不存在</p>

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
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { buySecondhandApi, getSecondhandDetailApi } from "@/api/secondhand";
import { submitReportApi, blockUserApi, unblockUserApi, isBlockingApi, isBlockedByApi } from "@/api/credit";
import { getUser } from "@/utils/storage";
import { toAssetUrl } from "@/utils/url";

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const item = ref(null);
const selectedImage = ref("");
const reportDialogVisible = ref(false);
const reportSubmitting = ref(false);
const reportForm = ref({ reasonType: "", reasonDesc: "" });
const isSellerBlocked = ref(false);

const imageList = computed(() => {
  if (!item.value) {
    return [];
  }
  const images = Array.isArray(item.value.images) ? item.value.images : [];
  const list = [item.value.cover, ...images]
    .filter(Boolean)
    .map((source) => toAssetUrl(source))
    .filter(Boolean);
  return [...new Set(list)];
});

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
    selectedImage.value = imageList.value[0] || "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=900&q=80";

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
      tradeContext: "SH_BUYER",
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
      { type: "warning", confirmButtonText: "确认拉黑", cancelButtonText: "取消" },
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
      { type: "warning", confirmButtonText: "确认取消", cancelButtonText: "取消" },
    );
    await unblockUserApi(item.value.sellerUserId);
    isSellerBlocked.value = false;
    ElMessage.success("已取消拉黑");
  } catch (e) {
    if (e === "cancel" || e?.toString?.().includes("cancel")) return;
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}
</script>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-shell {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.1fr);
  gap: 22px;
}

.gallery,
.buy-panel {
  border: 1px solid var(--line-soft);
  border-radius: 24px;
  background: #ffffff;
  padding: 18px;
  box-shadow: var(--shadow-soft);
}

.main-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 18px;
  overflow: hidden;
  background: #f1efe6;
}

.thumbs {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.thumb {
  aspect-ratio: 1 / 1;
  border: 2px solid transparent;
  border-radius: 12px;
  padding: 0;
  overflow: hidden;
  background: #f1efe6;
  cursor: pointer;
}

.thumb.active {
  border-color: var(--brand-primary);
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.buy-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.crumb {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.buy-panel h1 {
  margin: 0;
  font-size: clamp(26px, 4vw, 38px);
  line-height: 1.18;
  letter-spacing: 0;
}

.desc {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.price-box {
  border-radius: 18px;
  background: #fff5d1;
  padding: 16px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: baseline;
}

.price-box span {
  color: #6d5a15;
  font-weight: 800;
}

.price-box strong {
  color: var(--brand-warm);
  font-size: 36px;
  line-height: 1;
}

.price-box em {
  color: var(--text-muted);
  font-style: normal;
  text-decoration: line-through;
}

.info-grid {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.info-grid div {
  border: 1px solid var(--line-soft);
  border-radius: 14px;
  padding: 12px;
}

.info-grid dt {
  color: var(--text-muted);
  font-size: 12px;
}

.info-grid dd {
  margin: 6px 0 0;
  font-weight: 800;
}

.notice-box {
  border-radius: 16px;
  background: var(--surface-soft);
  color: var(--text-secondary);
  padding: 13px 14px;
  line-height: 1.7;
}

.actions,
.risk-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.risk-actions {
  border-top: 1px solid var(--line-soft);
  padding-top: 14px;
}

@media (max-width: 900px) {
  .detail-shell {
    grid-template-columns: 1fr;
  }

  .info-grid,
  .price-box {
    grid-template-columns: 1fr;
  }
}
</style>
