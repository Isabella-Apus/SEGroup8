<template>
  <div class="page-card">
    <div class="page-head">
      <div>
        <h2 class="page-title">我的评价</h2>
        <p class="page-subtitle">查看已完成订单的评价记录和卖家回复</p>
      </div>
      <el-button @click="router.push('/order')">返回订单</el-button>
    </div>

    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索评价内容"
        clearable
        style="max-width: 260px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.score" placeholder="评分" clearable style="width: 140px">
        <el-option v-for="n in [5, 4, 3, 2, 1]" :key="n" :label="`${n} 星`" :value="n" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />
    <el-empty v-else-if="groupedReviews.length === 0" description="暂无评价记录" />

    <div v-else class="review-list">
      <el-card v-for="group in groupedReviews" :key="group.key" class="review-card" shadow="hover">
        <div class="review-header">
          <div>
            <div class="product-title">
              {{ group.productName || "商品" }}
              <el-tag v-if="group.productType === 'SECONDHAND'" size="small" type="warning">二手</el-tag>
            </div>
            <div class="meta">订单号：{{ group.orderNo || "-" }}</div>
          </div>
          <el-rate :model-value="group.latestScore" disabled />
        </div>

        <div class="timeline">
          <div v-for="item in group.items" :key="item.id" class="timeline-item">
            <div class="timeline-top">
              <el-tag size="small" :type="item.reviewType === 'FOLLOWUP' ? 'success' : 'info'">
                {{ item.reviewType === "FOLLOWUP" ? "追评" : "评价" }}
              </el-tag>
              <span>{{ formatTime(item.createTime) }}</span>
            </div>
            <div class="review-content">
              <el-rate :model-value="item.score" disabled size="small" />
              <span>{{ item.content }}</span>
            </div>
            <div v-if="item.sellerReply" class="seller-reply">
              <strong>卖家回复</strong>
              <span>{{ item.sellerReply }}</span>
              <em>{{ formatTime(item.sellerReplyTime) }}</em>
            </div>
          </div>
        </div>

        <div class="review-actions">
          <el-button
            v-if="!group.hasFollowUp && group.original"
            link
            type="primary"
            @click="openFollowDialog(group.original)"
          >
            追加评价
          </el-button>
        </div>
      </el-card>
    </div>

    <div class="pager-wrap">
      <el-pagination
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNum"
        :page-sizes="[10, 20, 50]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog v-model="followDialogVisible" title="追加评价" width="460px">
      <el-form label-width="72px">
        <el-form-item label="评分">
          <el-rate v-model="followForm.score" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="followForm.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入追加评价内容"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitFollowUp">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { getMyReviewListApi, submitFollowUpReviewApi } from "@/api/review";

const router = useRouter();

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  score: null
});

const loading = ref(false);
const submitting = ref(false);
const records = ref([]);
const total = ref(0);
const followDialogVisible = ref(false);
const followTarget = ref(null);

const followForm = reactive({
  score: 5,
  content: ""
});

const groupedReviews = computed(() => {
  const map = new Map();
  for (const item of records.value) {
    const key = `${item.orderId}-${item.productType}-${item.productId}`;
    if (!map.has(key)) {
      map.set(key, {
        key,
        orderNo: item.orderNo,
        productName: item.productName,
        productType: item.productType,
        items: []
      });
    }
    map.get(key).items.push(item);
  }

  return Array.from(map.values()).map((group) => {
    group.items.sort((a, b) => String(a.createTime || "").localeCompare(String(b.createTime || "")));
    group.original = group.items.find((item) => item.reviewType === "ORIGINAL") || group.items[0];
    group.hasFollowUp = group.items.some((item) => item.reviewType === "FOLLOWUP");
    group.latestScore = group.items[group.items.length - 1]?.score || group.original?.score || 0;
    return group;
  });
});

onMounted(fetchReviews);

async function fetchReviews() {
  loading.value = true;
  try {
    const result = await getMyReviewListApi(query);
    records.value = result.data?.records || [];
    total.value = result.data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  fetchReviews();
}

function handleReset() {
  query.pageNum = 1;
  query.pageSize = 10;
  query.keyword = "";
  query.score = null;
  fetchReviews();
}

function handlePageChange(pageNum) {
  query.pageNum = pageNum;
  fetchReviews();
}

function handleSizeChange(pageSize) {
  query.pageSize = pageSize;
  query.pageNum = 1;
  fetchReviews();
}

function openFollowDialog(row) {
  followTarget.value = row;
  followForm.score = row.score || 5;
  followForm.content = "";
  followDialogVisible.value = true;
}

async function submitFollowUp() {
  if (!followTarget.value) return;
  if (!followForm.score || !followForm.content.trim()) {
    ElMessage.warning("请填写评分和追加评价内容");
    return;
  }

  submitting.value = true;
  try {
    await submitFollowUpReviewApi({
      orderId: followTarget.value.orderId,
      productType: followTarget.value.productType,
      productId: followTarget.value.productId,
      score: followForm.score,
      content: followForm.content.trim()
    });
    ElMessage.success("追评已提交");
    followDialogVisible.value = false;
    await fetchReviews();
  } finally {
    submitting.value = false;
  }
}

function formatTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 19);
}
</script>

<style scoped>
.page-card {
  max-width: 1100px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 18px;
}

.page-title {
  margin: 0;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #7a8599;
  font-size: 14px;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.review-list {
  display: grid;
  gap: 12px;
}

.review-card {
  border-radius: 8px;
}

.review-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef0f4;
}

.product-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: #1f2937;
}

.meta,
.timeline-top,
.seller-reply em {
  color: #8a93a5;
  font-size: 13px;
}

.timeline {
  display: grid;
  gap: 12px;
  margin-top: 12px;
}

.timeline-item {
  display: grid;
  gap: 8px;
}

.timeline-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: #303745;
  line-height: 1.7;
}

.seller-reply {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  background: #f7f9fc;
  border-radius: 8px;
  color: #4b5563;
}

.seller-reply strong {
  color: #303745;
  font-size: 13px;
}

.review-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 640px) {
  .page-head,
  .review-header,
  .review-content {
    flex-direction: column;
  }
}
</style>
