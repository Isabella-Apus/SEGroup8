<template>
  <div class="page-card">
    <h2 class="page-title">
      {{ texts.title }}
      <span v-if="!loading && allHistory.length" class="page-title-sub">
        {{ texts.openSub }}{{ total }}{{ texts.closeSub }}
      </span>
    </h2>

    <div v-if="!loading" class="toolbar-wrap">
      <div class="toolbar">
        <el-tabs v-model="query.recordType" class="type-tabs" @tab-change="handleRecordTypeChange">
          <el-tab-pane :label="texts.productTab" name="product" />
          <el-tab-pane :label="texts.storeTab" name="store" />
        </el-tabs>

        <el-input
          v-model="query.keyword"
          :placeholder="searchPlaceholder"
          clearable
          style="max-width: 320px"
          @keyup.enter="handleSearch"
        />

        <el-date-picker
          v-model="query.singleDate"
          type="date"
          :placeholder="texts.pickDate"
          value-format="YYYY-MM-DD"
          clearable
          style="width: 170px"
          @change="handleDateChange"
        />

        <el-button type="primary" @click="handleSearch">{{ texts.search }}</el-button>
        <el-button @click="handleReset">{{ texts.reset }}</el-button>

        <div class="toolbar-spacer" />
        <el-button v-if="filteredList.length && !manageMode" @click="enterManageMode">{{ texts.manage }}</el-button>
      </div>

      <div v-if="query.recordType === 'product'" class="product-subtype-tags">
        <el-check-tag
          :checked="query.productSubtype === 'all'"
          @change="() => setProductSubtype('all')"
        >
          {{ texts.productAllTab }}
        </el-check-tag>
        <el-check-tag
          :checked="query.productSubtype === 'new'"
          @change="() => setProductSubtype('new')"
        >
          {{ texts.productNewTab }}
        </el-check-tag>
        <el-check-tag
          :checked="query.productSubtype === 'secondhand'"
          @change="() => setProductSubtype('secondhand')"
        >
          {{ texts.productSecondhandTab }}
        </el-check-tag>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="5" animated />

    <template v-else>
      <el-empty v-if="allHistory.length === 0" :description="texts.empty" />
      <el-empty v-else-if="filteredByType.length === 0" :description="texts.emptyByType" />
      <el-empty v-else-if="filteredByDate.length === 0" :description="texts.emptyByDate" />
      <el-empty v-else-if="filteredList.length === 0" :description="texts.noMatch" />

      <div v-else class="history-wrap">
        <div v-for="group in groupedPagedList" :key="group.key" class="history-group">
          <div class="group-header">
            <span class="group-title">{{ group.label }}</span>
            <span class="group-line" />
          </div>

          <div class="history-grid">
            <el-card v-for="item in group.items" :key="item.id" shadow="hover" class="history-card">
              <div v-if="manageMode" class="card-check">
                <el-checkbox
                  :model-value="selectedIds.includes(item.id)"
                  @change="(checked) => onCheckOne(item.id, checked)"
                />
              </div>

              <div class="card-main">
                <el-image
                  v-if="isProductRecord(item) && item.product?.cover"
                  :src="toFullImageUrl(item.product.cover)"
                  fit="cover"
                  class="product-cover"
                />
                <div v-else class="product-cover placeholder">{{ texts.noImage }}</div>

                <div class="card-content">
                  <div class="name">{{ getRecordName(item) }}</div>
                  <div v-if="isProductRecord(item)" class="price">
                    {{ texts.currency }}{{ Number(item.product?.price || 0).toFixed(2) }}
                  </div>
                  <div v-else class="store-tag">{{ texts.storeRecordTag }}</div>
                  <div class="time">{{ formatTime(item.browseTime) }}</div>
                </div>
              </div>

              <div class="card-actions">
                <el-button
                  v-if="isProductRecord(item)"
                  type="primary"
                  @click="viewProduct(item)"
                  :disabled="!item.product?.id"
                >
                  {{ texts.viewProduct }}
                </el-button>
                <el-button v-else type="primary" @click="viewStore(item)" :disabled="!getStoreId(item)">
                  {{ texts.viewStore }}
                </el-button>
              </div>
            </el-card>
          </div>
        </div>

        <div class="pager-wrap">
          <el-pagination
            background
            layout="total, prev, pager, next, sizes"
            :total="total"
            :page-size="query.pageSize"
            :current-page="query.pageNum"
            :page-sizes="[8, 12, 24]"
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
          />
        </div>

        <div v-if="manageMode" class="manage-bar">
          <el-checkbox :model-value="allSelected" :indeterminate="isIndeterminate" @change="toggleAll">
            {{ texts.selectAll }}
          </el-checkbox>
          <el-space>
            <el-button type="danger" :disabled="selectedIds.length === 0" @click="handleDeleteSelected">
              {{ texts.deleteBtn }}
            </el-button>
            <el-button @click="exitManageMode">{{ texts.cancel }}</el-button>
          </el-space>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { deleteBrowseHistoryBatchApi, getBrowseHistoryApi } from "@/api/user";
import { searchBrowseHistory } from "@/utils/search";
import { useRouter } from "vue-router";
import { toAssetUrl } from "@/utils/url";

const router = useRouter();
const loading = ref(false);
const allHistory = ref([]);
const selectedIds = ref([]);
const manageMode = ref(false);

const query = reactive({
  keyword: "",
  recordType: "product",
  productSubtype: "all",
  singleDate: "",
  pageNum: 1,
  pageSize: 12,
});

const texts = {
  title: "浏览记录",
  productTab: "商品",
  storeTab: "店铺",
  productAllTab: "全部",
  productNewTab: "商品",
  productSecondhandTab: "二手",
  pickDate: "筛选日期",
  openSub: "（共 ",
  closeSub: " 条）",
  empty: "暂无浏览记录，去逛逛吧",
  emptyByType: "当前类型暂无浏览记录",
  emptyByDate: "当前日期筛选下暂无浏览记录",
  noMatch: "未找到匹配的浏览记录",
  searchPlaceholderProduct: "搜索商品名称或商品 ID",
  searchPlaceholderStore: "搜索店铺名称或店铺 ID",
  search: "搜索",
  reset: "重置",
  manage: "管理",
  selectAll: "全选",
  deleteBtn: "删除",
  cancel: "取消",
  noImage: "暂无图片",
  productOffline: "商品已下架",
  storeOffline: "店铺信息不可用",
  storeRecordTag: "店铺浏览",
  currency: "￥",
  viewProduct: "查看商品",
  viewStore: "查看店铺",
  groupToday: "今天",
  groupYesterday: "昨天",
  confirmTitle: "提示",
  confirmDeleteSelectedPrefix: "确认删除已选的 ",
  confirmDeleteSelectedSuffix: " 条浏览记录吗？",
  opSuccessDelete: "删除成功",
};

const searchPlaceholder = computed(() => {
  return query.recordType === "store" ? texts.searchPlaceholderStore : texts.searchPlaceholderProduct;
});

const filteredByType = computed(() => {
  return allHistory.value.filter((record) => {
    const topType = getRecordType(record);
    if (topType !== query.recordType) {
      return false;
    }

    if (topType !== "product") {
      return true;
    }

    if (query.productSubtype === "all") {
      return true;
    }

    const productType = getProductSubtype(record);
    if (query.productSubtype === "secondhand") {
      return productType === "SECONDHAND";
    }
    return productType !== "SECONDHAND";
  });
});

const filteredByDate = computed(() => {
  if (!query.singleDate) {
    return filteredByType.value;
  }
  return filteredByType.value.filter((record) => dateKey(record.browseTime) === query.singleDate);
});

const filteredList = computed(() => {
  const kw = query.keyword.trim();
  if (!kw) {
    return filteredByDate.value;
  }

  if (query.recordType === "product") {
    return searchBrowseHistory({ records: filteredByDate.value, keyword: kw });
  }

  const normalized = kw.toLowerCase();
  return filteredByDate.value.filter((record) => {
    const storeName = String(getStoreName(record) || "").toLowerCase();
    const storeId = String(getStoreId(record) || "").toLowerCase();
    return storeName.includes(normalized) || storeId.includes(normalized);
  });
});

const total = computed(() => filteredList.value.length);

const pagedList = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize;
  return filteredList.value.slice(start, start + query.pageSize);
});

const groupedPagedList = computed(() => {
  const map = new Map();
  pagedList.value.forEach((item) => {
    const key = dateKey(item?.browseTime);
    if (!map.has(key)) {
      map.set(key, []);
    }
    map.get(key).push(item);
  });

  return Array.from(map.entries()).map(([key, items]) => ({
    key,
    label: dateGroupLabel(key),
    items,
  }));
});

const selectedInFilterCount = computed(
  () => filteredList.value.filter((r) => selectedIds.value.includes(r.id)).length
);

const allSelected = computed(
  () => filteredList.value.length > 0 && selectedInFilterCount.value === filteredList.value.length
);

const isIndeterminate = computed(
  () => selectedInFilterCount.value > 0 && selectedInFilterCount.value < filteredList.value.length
);

function getRecordType(record) {
  const explicit = String(record?.recordType || record?.historyType || "").toLowerCase();
  if (explicit === "store") {
    return "store";
  }
  if (String(record?.productType || "").toUpperCase() === "SHOP") {
    return "store";
  }
  if (record?.store || record?.storeId) {
    return "store";
  }
  return "product";
}

function getProductSubtype(record) {
  return String(record?.productType || record?.type || "NEW").toUpperCase();
}

function isProductRecord(record) {
  return getRecordType(record) === "product";
}

function getStoreId(record) {
  return record?.store?.id || record?.storeId || (String(record?.productType || "").toUpperCase() === "SHOP" ? record?.product?.id : null);
}

function getStoreName(record) {
  return record?.store?.name || record?.storeName || (String(record?.productType || "").toUpperCase() === "SHOP" ? record?.product?.name : "");
}

function getRecordName(record) {
  if (isProductRecord(record)) {
    return record?.product?.name || texts.productOffline;
  }
  return getStoreName(record) || texts.storeOffline;
}

function clampPage() {
  const maxPage = Math.max(1, Math.ceil(filteredList.value.length / query.pageSize) || 1);
  if (query.pageNum > maxPage) {
    query.pageNum = maxPage;
  }
}

async function loadBrowseHistory() {
  loading.value = true;
  try {
    const response = await getBrowseHistoryApi();
    allHistory.value = response?.data || [];
    selectedIds.value = [];
    query.pageNum = 1;
    query.keyword = "";
    manageMode.value = false;
  } catch {
    allHistory.value = [];
    selectedIds.value = [];
    manageMode.value = false;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  clampPage();
}

function handleRecordTypeChange() {
  query.pageNum = 1;
  selectedIds.value = [];
  if (manageMode.value) {
    manageMode.value = false;
  }
  clampPage();
}

function setProductSubtype(type) {
  query.productSubtype = type;
  query.pageNum = 1;
  selectedIds.value = [];
  clampPage();
}

function handleDateChange() {
  query.pageNum = 1;
  clampPage();
}

function handleReset() {
  query.keyword = "";
  query.pageNum = 1;
  query.recordType = "product";
  query.productSubtype = "all";
  query.singleDate = "";
  clampPage();
}

function handlePageChange(pageNum) {
  query.pageNum = pageNum;
}

function handleSizeChange(pageSize) {
  query.pageSize = pageSize;
  query.pageNum = 1;
  clampPage();
}

function viewProduct(item) {
  const productId = item?.product?.id;
  if (!productId) {
    return;
  }
  const type = getProductSubtype(item);
  const path = type === "SECONDHAND" ? `/secondhand/${productId}` : `/product/${productId}`;
  router.push({ path, query: { from: "browse-history" } });
}

function viewStore(item) {
  const storeId = getStoreId(item);
  if (!storeId) {
    return;
  }
  router.push({ path: `/shop/${storeId}`, query: { from: "browse-history" } });
}

function formatTime(time) {
  if (!time) {
    return "-";
  }
  return new Date(time).toLocaleString();
}

function dateKey(time) {
  if (!time) {
    return "-";
  }
  const d = new Date(time);
  if (Number.isNaN(d.getTime())) {
    return "-";
  }
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function dateGroupLabel(key) {
  const today = dateKey(new Date());
  const yesterdayDate = new Date();
  yesterdayDate.setDate(yesterdayDate.getDate() - 1);
  const yesterday = dateKey(yesterdayDate);

  if (key === today) {
    return texts.groupToday;
  }
  if (key === yesterday) {
    return texts.groupYesterday;
  }
  return key;
}

function toFullImageUrl(url) {
  return toAssetUrl(url);
}

function onCheckOne(id, checked) {
  if (checked) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id);
    }
    return;
  }
  selectedIds.value = selectedIds.value.filter((x) => x !== id);
}

function toggleAll(checked) {
  if (checked) {
    const ids = filteredList.value.map((item) => item.id);
    selectedIds.value = [...new Set([...selectedIds.value, ...ids])];
    return;
  }
  const drop = new Set(filteredList.value.map((item) => item.id));
  selectedIds.value = selectedIds.value.filter((id) => !drop.has(id));
}

function enterManageMode() {
  manageMode.value = true;
}

function exitManageMode() {
  manageMode.value = false;
  selectedIds.value = [];
}

async function handleDeleteSelected() {
  if (selectedIds.value.length === 0) {
    return;
  }
  await ElMessageBox.confirm(
    `${texts.confirmDeleteSelectedPrefix}${selectedIds.value.length}${texts.confirmDeleteSelectedSuffix}`,
    texts.confirmTitle,
    { type: "warning" }
  );
  await deleteBrowseHistoryBatchApi(selectedIds.value);
  const idSet = new Set(selectedIds.value);
  allHistory.value = allHistory.value.filter((item) => !idSet.has(item.id));
  exitManageMode();
  clampPage();
  ElMessage.success(texts.opSuccessDelete);
}

onMounted(() => {
  loadBrowseHistory();
});
</script>

<style scoped>
.page-title-sub {
  margin-left: 8px;
  font-size: 14px;
  font-weight: normal;
  color: #6b7280;
}

.toolbar-wrap {
  margin-bottom: 14px;
}

.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.type-tabs {
  margin-right: 4px;
}

.product-subtype-tags {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
  margin-top: -2px;
}

.product-subtype-tags :deep(.el-check-tag) {
  border-radius: 999px;
  padding: 6px 12px;
}

.toolbar-spacer {
  flex: 1;
  min-width: 8px;
}

.history-wrap {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.history-group {
  margin-bottom: 18px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.group-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
}

.group-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, #dcdfe6, rgba(220, 223, 230, 0.2));
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 14px;
}

.history-card {
  position: relative;
  border-radius: 12px;
}

.card-check {
  position: absolute;
  right: 12px;
  top: 10px;
  z-index: 2;
}

.card-main {
  display: flex;
  gap: 14px;
}

.product-cover {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  flex-shrink: 0;
}

.placeholder {
  background: #f5f7fa;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
}

.card-content {
  min-width: 0;
}

.name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.5;
}

.price {
  color: #f56c6c;
  font-weight: 700;
  margin-bottom: 8px;
}

.store-tag {
  color: #409eff;
  margin-bottom: 8px;
  font-size: 13px;
}

.time {
  color: #909399;
  font-size: 13px;
}

.card-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.pager-wrap {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.manage-bar {
  position: sticky;
  bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  margin-top: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}
</style>
