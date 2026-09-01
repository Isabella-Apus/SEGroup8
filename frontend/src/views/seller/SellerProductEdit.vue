<template>
  <div>
    <div class="page-header">
      <h2 class="page-title">{{ isEdit ? '编辑商品' : '发布新商品' }}</h2>
      <el-button @click="$router.back()">返回</el-button>
    </div>

    <el-card>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        v-loading="loading"
      >
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

        <el-form-item label="价格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0.01"
            :precision="2"
            :step="1"
            style="width: 200px"
          />
          <span class="field-unit">元</span>
        </el-form-item>

        <el-form-item label="库存" prop="stock">
          <el-input-number
            v-model="form.stock"
            :min="0"
            :step="1"
            style="width: 200px"
          />
          <span class="field-unit">件</span>
        </el-form-item>

        <el-form-item label="商品分类" prop="subCategoryId">
          <div class="category-picker">
            <el-input
              :model-value="sellerMainCategory?.name || ''"
              disabled
              placeholder="主营一级类目"
              style="width: 180px"
            />
            <el-select
              v-model="form.subCategoryId"
              :disabled="!subCategoryOptions.length"
              placeholder="请选择二级分类"
              style="width: 220px"
            >
              <el-option
                v-for="item in subCategoryOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </div>
        </el-form-item>

        <el-form-item label="商品图片" prop="images">
          <div class="image-uploader">
            <div v-if="form.images.length" class="image-list">
              <div v-for="(url, index) in form.images" :key="url" class="image-item">
                <el-image :src="toFullImageUrl(url)" fit="cover" class="image-thumb" />
                <span v-if="index === 0" class="cover-badge">封面</span>
                <button type="button" class="remove-image" @click="removeImage(index)">
                  <el-icon><DeleteIcon /></el-icon>
                </button>
              </div>
            </div>

            <el-upload
              v-if="form.images.length < 9"
              :show-file-list="false"
              :before-upload="beforeUpload"
              :http-request="handleUpload"
              accept="image/*"
              multiple
            >
              <div class="upload-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <span>上传图片</span>
              </div>
            </el-upload>
          </div>
          <div class="upload-tip">支持 jpg / png，最多 9 张，第一张会作为封面</div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            @click="handleSubmit"
          >
            {{ isEdit ? '保存修改' : '发布商品' }}
          </el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete as DeleteIcon, Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  getProductDetail,
  createProduct,
  updateProduct,
  uploadImage,
  getCategoryTree
} from '@/api/seller'
import { toAssetUrl } from '@/utils/url'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const categoryTree = ref([])
const userStore = useUserStore()

const isEdit = computed(() => !!route.params.id)
const sellerMainCategory = computed(() => categoryTree.value.find((item) => Number(item.id) === Number(form.categoryId)) || null)
const subCategoryOptions = computed(() => sellerMainCategory.value?.children || [])

const form = reactive({
  name: '',
  description: '',
  price: 0.01,
  stock: 0,
  categoryId: null,
  subCategoryId: null,
  images: []
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  subCategoryId: [{ required: true, message: '请选择二级分类', trigger: 'change' }],
  images: [{
    validator: (_, value, callback) => {
      if (value?.length) callback()
      else callback(new Error('请至少上传一张商品图片'))
    },
    trigger: 'change'
  }]
}

async function loadCategories() {
  const res = await getCategoryTree('NEW')
  categoryTree.value = res.data || []
  await userStore.fetchProfile()
  const mainCategoryId = resolveSellerMainCategoryId(userStore.userInfo || {})
  if (mainCategoryId) {
    form.categoryId = mainCategoryId
  } else {
    ElMessage.warning('未找到店铺主营一级类目，请确认入驻资料')
  }
}

async function loadDetail() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await getProductDetail(route.params.id)
    const d = res.data
    form.name = d.name
    form.description = d.description
    form.price = d.price
    form.stock = d.stock
    if (!form.categoryId && d.categoryId) {
      form.categoryId = d.categoryId
    }
    form.subCategoryId = isSubCategoryOfCurrentMain(d.subCategoryId) ? d.subCategoryId : null
    form.images = Array.isArray(d.images) && d.images.length ? [...d.images] : (d.cover ? [d.cover] : [])
  } catch (e) {
    ElMessage.error('加载商品信息失败')
  } finally {
    loading.value = false
  }
}

function resolveSellerMainCategoryId(info) {
  const directId = Number(info.categoryId ?? info.category)
  if (Number.isFinite(directId) && directId > 0) {
    return directId
  }
  const categoryName = String(info.category || '').trim()
  return categoryTree.value.find((item) => item.name === categoryName)?.id || null
}

function isSubCategoryOfCurrentMain(subCategoryId) {
  return subCategoryOptions.value.some((item) => Number(item.id) === Number(subCategoryId))
}

function toFullImageUrl(url) {
  return toAssetUrl(url)
}

function beforeUpload(file) {
  if (form.images.length >= 9) {
    ElMessage.warning('商品图片最多上传 9 张')
    return false
  }
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

async function handleUpload({ file }) {
  try {
    if (form.images.length >= 9) return
    const res = await uploadImage(file)
    const url = res.data.url
    if (url && !form.images.includes(url)) {
      form.images.push(url)
      formRef.value?.validateField('images')
    }
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败')
  }
}

function removeImage(index) {
  form.images.splice(index, 1)
  formRef.value?.validateField('images')
}

async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        name: form.name,
        description: form.description,
        price: form.price,
        stock: form.stock,
        cover: form.images[0] || '',
        images: [...form.images],
        categoryId: form.categoryId,
        subCategoryId: form.subCategoryId,
        status: 1
      }
      if (isEdit.value) {
        await updateProduct(route.params.id, payload)
        ElMessage.success('修改成功')
      } else {
        await createProduct(payload)
        ElMessage.success('发布成功')
      }
      // Carry the persisted name to the list so the seller immediately sees
      // the created/updated product, even when their catalogue spans pages.
      router.push({ path: '/merchant/seller-products', query: { keyword: form.name } })
    } catch (e) {
      ElMessage.error(isEdit.value ? '修改失败' : '发布失败')
    } finally {
      submitting.value = false
    }
  })
}

onMounted(async () => {
  await loadCategories()
  await loadDetail()
})
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

.field-unit {
  margin-left: 8px;
  color: #999;
}

.category-picker {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.image-uploader {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-start;
}

.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.image-item {
  position: relative;
  width: 104px;
  height: 104px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  background: #f7f8fa;
}

.image-thumb {
  width: 100%;
  height: 100%;
}

.cover-badge {
  position: absolute;
  left: 6px;
  top: 6px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.92);
  color: #fff;
  padding: 2px 7px;
  font-size: 12px;
  font-weight: 800;
}

.remove-image {
  position: absolute;
  right: 5px;
  top: 5px;
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.52);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.upload-placeholder {
  width: 104px;
  height: 104px;
  border: 1px dashed #cfd5df;
  border-radius: 8px;
  color: #909399;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}

.upload-placeholder:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-icon {
  font-size: 26px;
}

.upload-tip {
  color: #999;
  font-size: 12px;
  margin-top: 6px;
}
</style>
