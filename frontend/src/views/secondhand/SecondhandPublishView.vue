<template>
  <section class="publish-page">
    <div class="hero">
      <div>
        <h1>发布二手商品</h1>
      </div>
      <div class="hero-dot"></div>
    </div>

    <div class="form-shell">
      <el-form :model="form" label-width="96px">
        <el-form-item label="商品名称">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>

        <el-form-item label="商品图片">
          <el-space alignment="flex-start">
            <el-upload
              :show-file-list="false"
              multiple
              :disabled="!canUploadMore"
              :http-request="uploadCover"
              accept="image/*"
            >
              <el-button :loading="coverUploading">{{ canUploadMore ? '上传图片' : '最多 9 张' }}</el-button>
            </el-upload>
            <el-image v-if="form.cover" :src="toFullImageUrl(form.cover)" fit="cover" class="cover-preview" />
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
                <el-button class="image-remove" size="small" text type="danger" @click="removeImage(index)">
                  删除
                </el-button>
              </div>
            </template>
          </draggable>
        </el-form-item>

        <el-form-item label="原价">
          <el-input-number v-model="form.originPrice" :min="1" :precision="2" :step="10" />
        </el-form-item>

        <el-form-item label="售价">
          <el-input-number v-model="form.salePrice" :min="1" :precision="2" :step="10" />
        </el-form-item>

        <el-form-item label="商品分类">
          <el-cascader
            v-model="form.categoryPath"
            :options="SECONDHAND_CATEGORY_TREE"
            :props="cascaderProps"
            clearable
            filterable
            placeholder="先选择一级分类，再选择二级分类"
            style="width: 320px"
          />
        </el-form-item>

        <el-form-item label="成色">
          <el-select v-model="form.condition" style="width: 180px">
            <el-option label="全新" value="全新" />
            <el-option label="99新" value="99新" />
            <el-option label="9成新" value="9成新" />
            <el-option label="8成新及以下" value="8成新及以下" />
          </el-select>
        </el-form-item>

        <el-form-item label="可议价">
          <el-switch
            v-model="form.isNegotiable"
            :active-value="1"
            :inactive-value="0"
            active-text="支持议价"
            inactive-text="不议价"
          />
        </el-form-item>

        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submit">发布二手</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button text @click="$router.push('/secondhand')">返回二手市场</el-button>
        </el-form-item>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import draggable from 'vuedraggable';
import { publishSecondhandApi } from '@/api/secondhand';
import { uploadImageApi } from '@/api/upload';
import { SECONDHAND_CATEGORY_TREE } from '@/constants/categories';
import { toApiAssetUrl } from '@/utils/url';

const cascaderProps = {
  emitPath: true,
  checkStrictly: false,
  value: 'value',
  label: 'label',
  children: 'children',
};

const form = reactive({
  name: '',
  cover: '',
  images: [],
  originPrice: 100,
  salePrice: 80,
  categoryPath: [],
  condition: '9成新',
  isNegotiable: 1,
  description: '',
});

const submitting = ref(false);
const coverUploading = ref(false);
const canUploadMore = computed(() => form.images.length < 9);

async function submit() {
  submitting.value = true;
  try {
    const [categoryId, subCategoryId] = form.categoryPath || [];
    if (!categoryId || !subCategoryId) {
      ElMessage.warning('请先选择一级与二级分类');
      return;
    }
    await publishSecondhandApi({
      name: form.name,
      cover: form.images[0] || form.cover,
      images: form.images,
      description: form.description,
      originPrice: form.originPrice,
      salePrice: form.salePrice,
      categoryId,
      subCategoryId,
      conditionLevel: form.condition,
      isNegotiable: form.isNegotiable,
    });
    ElMessage.success('二手商品发布成功');
    reset();
  } finally {
    submitting.value = false;
  }
}

async function uploadCover(option) {
  if (!canUploadMore.value) {
    ElMessage.warning('商品图片最多上传 9 张');
    option.onError?.(new Error('image limit exceeded'));
    return;
  }
  coverUploading.value = true;
  try {
    const result = await uploadImageApi(option.file);
    const url = result.data?.url || result.data || '';
    if (url && !form.images.includes(url)) {
      form.images.push(url);
    }
    syncCover();
    option.onSuccess?.(result);
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '图片上传失败');
    option.onError?.(error);
  } finally {
    coverUploading.value = false;
  }
}

function reset() {
  form.name = '';
  form.cover = '';
  form.images = [];
  form.originPrice = 100;
  form.salePrice = 80;
  form.categoryPath = [];
  form.condition = '9成新';
  form.isNegotiable = 1;
  form.description = '';
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
.publish-page {
  padding: 8px 10px 20px;
}

.hero {
  border-radius: 22px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  margin-bottom: 14px;
  background: linear-gradient(120deg, #ff6f2f, #ff9822);
}

.hero h1 {
  margin: 0;
  font-size: 30px;
}

.hero-dot {
  width: 86px;
  height: 86px;
  border-radius: 50%;
  background: radial-gradient(circle at 25% 25%, #fff5, #fff1 60%, transparent 70%);
}

.form-shell {
  background: #fff;
  border: 1px solid var(--line-soft);
  border-radius: 16px;
  padding: 16px;
}

.cover-preview {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
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
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}

@media (max-width: 760px) {
  .publish-page {
    padding: 6px;
  }

  .hero {
    padding: 14px;
    border-radius: 16px;
  }

  .hero h1 {
    font-size: 24px;
  }
}
</style>
