<template>
  <div class="page-card">
    <div class="head-row">
      <h2 class="page-title">商品管理</h2>
      <el-button type="primary" @click="openCreate">新增商品</el-button>
    </div>

    <el-form :inline="true" :model="query" class="query-form">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="商品名称" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">搜索</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="商品名称" min-width="180" />
      <el-table-column label="封面" width="96">
        <template #default="scope">
          <el-image v-if="scope.row.cover" :src="toFullImageUrl(scope.row.cover)" fit="cover" class="table-cover" />
          <span v-else class="empty-tip">无</span>
        </template>
      </el-table-column>
      <el-table-column label="分类" min-width="150">
        <template #default="scope">
          {{ scope.row.subCategoryName || scope.row.categoryName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="110" />
      <el-table-column prop="stock" label="库存" width="90" />
      <el-table-column prop="statusName" label="状态" width="90" />
      <el-table-column label="操作" min-width="280">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link @click="toggleStatus(scope.row)">{{ scope.row.status === 1 ? '下架' : '上架' }}</el-button>
          <el-button link @click="openAdjustStock(scope.row)">调库存</el-button>
          <el-button link type="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑商品' : '新增商品'" width="620px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="92px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="一级分类">
          <el-input :model-value="mainCategoryName" disabled placeholder="与注册时店铺主营分类一致" />
        </el-form-item>
        <el-form-item label="二级分类" prop="subCategoryId">
          <el-select v-model="form.subCategoryId" placeholder="请选择二级分类" style="width: 220px">
            <el-option
              v-for="item in subCategoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="商品图片">
          <el-space>
            <el-upload
              :show-file-list="false"
              multiple
              :disabled="!canUploadMore"
              :http-request="uploadCover"
              accept="image/*"
            >
              <el-button :loading="uploading">{{ canUploadMore ? '上传图片' : '最多 9 张' }}</el-button>
            </el-upload>
            <el-image v-if="form.cover" :src="toFullImageUrl(form.cover)" fit="cover" class="dialog-cover" />
          </el-space>
          <draggable
            v-if="form.images.length"
            v-model="form.images"
            class="image-list"
            :item-key="imageKey"
            handle=".drag-handle"
            :animation="160"
            @change="syncCover"
          >
            <template #item="{ element: url, index }">
              <div class="image-item">
                <el-image :src="toFullImageUrl(url)" fit="cover" class="image-thumb" />
                <el-tag v-if="index === 0" class="cover-tag" size="small">封面</el-tag>
                <div class="drag-handle" aria-label="调整图片顺序"></div>
                <el-button class="image-remove" size="small" text type="danger" @click="removeImage(index)">删除</el-button>
              </div>
            </template>
          </draggable>
        </el-form-item>
        <el-form-item label="商品价格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="10" style="width: 180px" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 180px" />
        </el-form-item>
        <el-form-item label="上架状态">
          <el-switch v-model="statusSwitch" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stockVisible" title="调整库存" width="420px">
      <el-form label-width="90px">
        <el-form-item label="商品">
          <span>{{ currentRow?.name || '-' }}</span>
        </el-form-item>
        <el-form-item label="调整数量">
          <el-input-number v-model="stockDelta" :step="1" />
          <span class="empty-tip stock-tip">可输入负数减少库存</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdjustStock">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import draggable from 'vuedraggable';
import { useUserStore } from '@/stores/user';
import {
  getSellerProductListApi,
  createSellerProductApi,
  updateSellerProductApi,
  deleteSellerProductApi,
  changeSellerProductStatusApi,
  adjustSellerProductStockApi,
} from '@/api/product';
import { uploadImageApi } from '@/api/upload';
import { findMainCategory } from '@/constants/categories';
import { toApiAssetUrl } from '@/utils/url';

const userStore = useUserStore();
const loading = ref(false);
const list = ref([]);
const total = ref(0);
const uploading = ref(false);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
});

const formVisible = ref(false);
const formRef = ref();
const form = reactive({
  id: null,
  name: '',
  cover: '',
  images: [],
  description: '',
  price: 1,
  stock: 0,
  categoryId: null,
  subCategoryId: null,
});
const statusSwitch = ref(true);
const canUploadMore = computed(() => form.images.length < 9);
const mainCategory = computed(() => findMainCategory(form.categoryId));
const mainCategoryName = computed(() => mainCategory.value?.label || '');
const subCategoryOptions = computed(() => mainCategory.value?.children || []);

const stockVisible = ref(false);
const currentRow = ref(null);
const stockDelta = ref(1);

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  subCategoryId: [{ required: true, message: '请选择二级分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入商品价格', trigger: 'change' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'change' }],
};

onMounted(async () => {
  await loadSellerCategory();
  fetchList();
});

async function loadSellerCategory() {
  const info = await userStore.fetchProfile();
  const categoryId = Number(info?.categoryId || info?.category);
  if (categoryId) {
    form.categoryId = categoryId;
  }
}

async function fetchList() {
  loading.value = true;
  try {
    const result = await getSellerProductListApi(query);
    list.value = result.data?.records || [];
    total.value = result.data?.total || 0;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  fetchList();
}

function reset() {
  query.pageNum = 1;
  query.pageSize = 10;
  query.keyword = '';
  query.status = undefined;
  fetchList();
}

function handlePageChange(pageNum) {
  query.pageNum = pageNum;
  fetchList();
}

function handleSizeChange(pageSize) {
  query.pageSize = pageSize;
  query.pageNum = 1;
  fetchList();
}

function resetForm() {
  const categoryId = form.categoryId;
  Object.assign(form, {
    id: null,
    name: '',
    cover: '',
    images: [],
    description: '',
    price: 1,
    stock: 0,
    categoryId,
    subCategoryId: null,
  });
  statusSwitch.value = true;
}

function openCreate() {
  resetForm();
  formVisible.value = true;
}

function openEdit(row) {
  form.id = row.id;
  form.name = row.name || '';
  form.cover = row.cover || '';
  form.images = Array.isArray(row.images) && row.images.length ? row.images : (row.cover ? [row.cover] : []);
  form.description = row.description || '';
  form.price = Number(row.price || 0);
  form.stock = Number(row.stock || 0);
  form.categoryId = row.categoryId || form.categoryId;
  form.subCategoryId = row.subCategoryId || null;
  statusSwitch.value = row.status === 1;
  formVisible.value = true;
}

async function submit() {
  if (!form.categoryId) {
    ElMessage.warning('未获取到店铺主营分类，请刷新后重试');
    return;
  }
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  const payload = {
    name: form.name,
    cover: form.images[0] || form.cover,
    images: form.images,
    description: form.description,
    price: form.price,
    stock: form.stock,
    categoryId: form.categoryId,
    subCategoryId: form.subCategoryId,
    status: statusSwitch.value ? 1 : 0,
  };
  if (form.id) {
    await updateSellerProductApi(form.id, payload);
    ElMessage.success('商品已保存');
  } else {
    await createSellerProductApi(payload);
    ElMessage.success('商品已新增');
  }
  formVisible.value = false;
  fetchList();
}

async function toggleStatus(row) {
  const targetStatus = row.status === 1 ? 0 : 1;
  await changeSellerProductStatusApi(row.id, targetStatus);
  ElMessage.success(targetStatus === 1 ? '商品已上架' : '商品已下架');
  fetchList();
}

function openAdjustStock(row) {
  currentRow.value = row;
  stockDelta.value = 1;
  stockVisible.value = true;
}

async function confirmAdjustStock() {
  if (!currentRow.value) return;
  if (!stockDelta.value) {
    ElMessage.warning('请输入调整数量');
    return;
  }
  await adjustSellerProductStockApi(currentRow.value.id, Number(stockDelta.value));
  ElMessage.success('库存已调整');
  stockVisible.value = false;
  fetchList();
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除商品“${row.name}”吗？`, '删除确认', { type: 'warning' });
  await deleteSellerProductApi(row.id);
  ElMessage.success('商品已删除');
  fetchList();
}

async function uploadCover(option) {
  if (!canUploadMore.value) {
    ElMessage.warning('商品图片最多上传 9 张');
    option.onError?.(new Error('image limit exceeded'));
    return;
  }
  uploading.value = true;
  try {
    const result = await uploadImageApi(option.file);
    const url = result.data?.url || result.data || '';
    if (url && !form.images.includes(url)) {
      form.images.push(url);
    }
    form.cover = form.images[0] || url;
    option.onSuccess?.(result);
  } catch (error) {
    option.onError?.(error);
  } finally {
    uploading.value = false;
  }
}

function removeImage(index) {
  form.images.splice(index, 1);
  syncCover();
}

function syncCover() {
  form.cover = form.images[0] || '';
}

function imageKey(url) {
  return url;
}

function toFullImageUrl(url) {
  return url ? toApiAssetUrl(url) : '';
}
</script>

<style scoped>
.head-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.query-form {
  margin-bottom: 12px;
}
.pager-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.table-cover {
  width: 56px;
  height: 56px;
  border-radius: 6px;
}
.dialog-cover {
  width: 64px;
  height: 64px;
  border-radius: 6px;
}
.image-list {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.image-item {
  position: relative;
  width: 64px;
  height: 64px;
}
.image-thumb {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}
.cover-tag {
  position: absolute;
  left: 2px;
  top: 2px;
  z-index: 2;
}
.drag-handle {
  position: absolute;
  inset: 0;
  cursor: move;
  user-select: none;
  z-index: 1;
}
.image-remove {
  position: absolute;
  right: 2px;
  bottom: 2px;
  padding: 2px 4px;
  background: rgba(255, 255, 255, 0.9);
  z-index: 2;
}
.stock-tip {
  margin-left: 8px;
}
</style>
