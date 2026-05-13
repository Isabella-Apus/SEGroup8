<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">{{ isEdit ? '编辑商品' : '发布商品' }}</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" v-loading="loading">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请输入商品描述"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="一级分类">
          <el-input :model-value="mainCategoryName" disabled placeholder="与注册时店铺主营分类一致" />
        </el-form-item>

        <el-form-item label="二级分类" prop="subCategoryId">
          <el-select v-model="form.subCategoryId" placeholder="请选择二级分类" style="width: 240px">
            <el-option
              v-for="item in subCategoryOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" style="width: 200px" />
          <span class="unit">元</span>
        </el-form-item>

        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="1" style="width: 200px" />
          <span class="unit">件</span>
        </el-form-item>

        <el-form-item label="商品图片" prop="imageUrl">
          <div class="upload-area">
            <el-upload
              :show-file-list="false"
              multiple
              :disabled="!canUploadMore"
              :before-upload="beforeUpload"
              :http-request="handleUpload"
              accept="image/*"
            >
              <div class="upload-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <div class="upload-text">{{ canUploadMore ? '上传图片' : '最多 9 张' }}</div>
              </div>
            </el-upload>
          </div>
          <draggable
            v-if="form.imageUrls.length"
            v-model="form.imageUrls"
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
                <el-button class="image-remove" size="small" text type="danger" @click.stop="removeImage(index)">
                  删除
                </el-button>
              </div>
            </template>
          </draggable>
          <div class="tip">支持 jpg / png，单张不超过 2MB，第一张将作为列表封面。</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '发布商品' }}
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import draggable from 'vuedraggable';
import { useUserStore } from '@/stores/user';
import { findMainCategory } from '@/constants/categories';
import { toApiAssetUrl } from '@/utils/url';
import {
  getProductDetail,
  createProduct,
  updateProduct,
  uploadImage,
} from '@/api/seller';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const formRef = ref(null);
const loading = ref(false);
const submitting = ref(false);

const isEdit = computed(() => !!route.params.id);

const form = reactive({
  name: '',
  description: '',
  price: 0.01,
  stock: 0,
  categoryId: null,
  subCategoryId: null,
  imageUrl: '',
  imageUrls: [],
});

const mainCategory = computed(() => findMainCategory(form.categoryId));
const mainCategoryName = computed(() => mainCategory.value?.label || '');
const subCategoryOptions = computed(() => mainCategory.value?.children || []);
const canUploadMore = computed(() => form.imageUrls.length < 9);

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  subCategoryId: [{ required: true, message: '请选择二级分类', trigger: 'change' }],
  imageUrl: [{ required: true, message: '请上传至少一张商品图片', trigger: 'change' }],
};

async function loadSellerCategory() {
  const info = await userStore.fetchProfile();
  const categoryId = Number(info?.categoryId || info?.category);
  if (categoryId) {
    form.categoryId = categoryId;
  }
}

async function loadDetail() {
  if (!isEdit.value) return;
  const res = await getProductDetail(route.params.id);
  const d = res.data;
  form.name = d.name || '';
  form.description = d.description || '';
  form.price = Number(d.price || 0.01);
  form.stock = Number(d.stock || 0);
  form.categoryId = d.categoryId || form.categoryId;
  form.subCategoryId = d.subCategoryId || null;
  form.imageUrls = Array.isArray(d.images) && d.images.length ? d.images : (d.cover ? [d.cover] : []);
  form.imageUrl = form.imageUrls[0] || '';
}

function toFullImageUrl(url) {
  return url ? toApiAssetUrl(url) : '';
}

function beforeUpload(file) {
  if (!canUploadMore.value) {
    ElMessage.warning('商品图片最多上传 9 张');
    return false;
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.error('只能上传图片文件');
    return false;
  }
  if (file.size / 1024 / 1024 >= 2) {
    ElMessage.error('单张图片不能超过 2MB');
    return false;
  }
  return true;
}

async function handleUpload({ file }) {
  try {
    const res = await uploadImage(file);
    const url = res.data?.url || res.data;
    if (url && !form.imageUrls.includes(url)) {
      form.imageUrls.push(url);
    }
    form.imageUrl = form.imageUrls[0] || '';
    ElMessage.success('图片上传成功');
  } catch {
    ElMessage.error('图片上传失败');
  }
}

function removeImage(index) {
  form.imageUrls.splice(index, 1);
  syncCover();
}

function syncCover() {
  form.imageUrl = form.imageUrls[0] || '';
}

function imageKey(url) {
  return url;
}

async function handleSubmit() {
  if (!form.categoryId) {
    ElMessage.warning('未获取到店铺主营分类，请刷新后重试');
    return;
  }
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitting.value = true;
  try {
    const payload = {
      name: form.name,
      description: form.description,
      price: form.price,
      stock: form.stock,
      categoryId: form.categoryId,
      subCategoryId: form.subCategoryId,
      cover: form.imageUrls[0] || form.imageUrl,
      images: form.imageUrls,
      status: 1,
    };
    if (isEdit.value) {
      await updateProduct(route.params.id, payload);
      ElMessage.success('商品已保存');
    } else {
      await createProduct(payload);
      ElMessage.success('商品已发布');
    }
    router.push('/merchant');
  } catch (e) {
    ElMessage.error(e?.message || (isEdit.value ? '保存失败' : '发布失败'));
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  loading.value = true;
  try {
    await loadSellerCategory();
    await loadDetail();
  } catch (e) {
    ElMessage.error(e?.message || '加载商品信息失败');
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  font-size: 20px;
}
.unit {
  margin-left: 8px;
  color: #999;
}
.upload-area {
  display: inline-block;
}
.upload-placeholder {
  width: 120px;
  height: 120px;
  border: 1px dashed #ddd;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s;
}
.upload-placeholder:hover {
  border-color: #409eff;
}
.upload-icon {
  font-size: 28px;
  color: #bbb;
}
.upload-text {
  margin-top: 8px;
  color: #999;
  font-size: 13px;
}
.image-list {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.image-item {
  position: relative;
  width: 72px;
  height: 72px;
}
.image-thumb {
  width: 72px;
  height: 72px;
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
.tip {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}
</style>
