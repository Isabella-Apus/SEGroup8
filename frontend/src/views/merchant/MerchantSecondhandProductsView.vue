<template>
  <div class="page-card">
    <div class="head">
      <h2 class="title">二手商品管理</h2>
      <div class="actions">
        <el-button type="primary" @click="goPublish">发布二手</el-button>
      </div>
    </div>

    <el-form :inline="true" class="query" @submit.prevent>
      <el-form-item>
        <el-input
          v-model="query.keyword"
          placeholder="搜索商品名"
          clearable
          style="width: 220px"
          @keyup.enter="fetchList(true)"
        />
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 140px">
          <el-option label="在售" :value="1" />
          <el-option label="下架/已售" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="fetchList(true)">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" style="width: 100%">
      <el-table-column prop="id" label="ID" width="88" />
      <el-table-column label="商品" min-width="260">
        <template #default="{ row }">
          <div class="prod">
            <el-image v-if="row.cover" :src="toFullImageUrl(row.cover)" fit="cover" class="cover" />
            <div v-else class="cover placeholder">暂无图片</div>
            <div class="meta">
              <div class="name">{{ row.name }}</div>
              <div class="sub">
                <span v-if="row.categoryName">{{ row.categoryName }}</span>
                <span v-if="row.subCategoryName"> / {{ row.subCategoryName }}</span>
                <span v-if="row.conditionLevel"> · {{ row.conditionLevel }}</span>
              </div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="售价" width="120">
        <template #default="{ row }">￥{{ Number(row.salePrice || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="议价" width="90">
        <template #default="{ row }">{{ Number(row.isNegotiable) === 1 ? "可议价" : "不可议价" }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
            {{ row.statusName || (Number(row.status) === 1 ? '在售' : '下架') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goDetail(row.id)">查看详情</el-button>
          <el-button link type="warning" @click="goDetail(row.id, true)">去拍卖</el-button>
          <el-button
            v-if="Number(row.status) === 1"
            link
            type="danger"
            @click="toggleStatus(row, 2)"
          >
            下架
          </el-button>
          <el-button
            v-else
            link
            type="success"
            @click="toggleStatus(row, 1)"
          >
            上架
          </el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="prev, pager, next"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import { changeSellerSecondhandStatusApi, deleteSellerSecondhandApi, getSellerSecondhandListApi } from "@/api/secondhand";

const router = useRouter();
const loading = ref(false);
const records = ref([]);
const total = ref(0);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  status: undefined,
});

onMounted(() => {
  fetchList(true);
});

async function fetchList(reset = false) {
  if (loading.value) return;
  loading.value = true;
  try {
    if (reset) query.pageNum = 1;
    const res = await getSellerSecondhandListApi({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword.trim() || undefined,
      status: query.status,
    });
    records.value = res.data?.records || [];
    total.value = Number(res.data?.total || 0);
  } finally {
    loading.value = false;
  }
}

function onPageChange(page) {
  query.pageNum = page;
  fetchList(false);
}

function resetQuery() {
  query.keyword = "";
  query.status = undefined;
  fetchList(true);
}

function goDetail(id, toAuction = false) {
  router.push(toAuction ? `/secondhand/${id}#auction` : `/secondhand/${id}`);
}

function goPublish() {
  router.push("/secondhand/publish");
}

async function toggleStatus(row, status) {
  try {
    await ElMessageBox.confirm(
      `确认将商品「${row.name}」${status === 1 ? "上架" : "下架"}？`,
      "操作确认",
      { type: "warning" }
    );
    await changeSellerSecondhandStatusApi(row.id, status);
    ElMessage.success("操作成功");
    fetchList(false);
  } catch (e) {
    if (e === "cancel" || e?.toString?.().includes("cancel")) return;
    ElMessage.error(e?.response?.data?.message || "操作失败");
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除二手商品「${row.name}」？删除后不可恢复。`,
      "删除确认",
      { type: "warning", confirmButtonText: "确认删除", cancelButtonText: "取消" }
    );
    await deleteSellerSecondhandApi(row.id);
    ElMessage.success("删除成功");
    fetchList(true);
  } catch (e) {
    if (e === "cancel" || e?.toString?.().includes("cancel")) return;
    ElMessage.error(e?.response?.data?.message || "删除失败");
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
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.title {
  margin: 0;
  font-size: 20px;
}

.query {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;
}

.prod {
  display: flex;
  gap: 12px;
  align-items: center;
}

.cover {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  background: #f9fafb;
  font-size: 12px;
}

.meta .name {
  font-weight: 700;
  color: #111827;
  line-height: 1.2;
}

.meta .sub {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
