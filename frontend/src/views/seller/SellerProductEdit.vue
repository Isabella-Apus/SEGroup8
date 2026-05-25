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

        <el-form-item label="商品分类" prop="categoryIds">
          <el-cascader
            v-model="form.categoryIds"
            :options="categoryOptions"
            :props="{ value: 'id', label: 'name', children: 'children', emitPath: true }"
            placeholder="请选择商品分类"
            style="width: 280px"
          />
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
import {
  getProductDetail,
  createProduct,
  updateProduct,
  uploadImage,
  getCategoryTree
} from '@/api/seller'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const categoryOptions = ref([])

const isEdit = computed(() => !!route.params.id)

const form = reactive({
  name: '',
  description: '',
  price: 0.01,
  stock: 0,
  categoryIds: [],
  images: []
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  categoryIds: [{ required: true, message: '请选择分类', trigger: 'change' }],
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
  categoryOptions.value = res.data || []
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
    form.categoryIds = d.categoryId && d.subCategoryId ? [d.categoryId, d.subCategoryId] : []
    form.images = Array.isArray(d.images) && d.images.length ? [...d.images] : (d.cover ? [d.cover] : [])
  } catch (e) {
    ElMessage.error('加载商品信息失败')
  } finally {
    loading.value = false
  }
}

function toFullImageUrl(url) {
  if (!url) return ''
  return url.startsWith('http') ? url : `http://localhost:8080${url}`
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
        categoryId: form.categoryIds?.[0],
        subCategoryId: form.categoryIds?.[1],
        status: 1
      }
      if (isEdit.value) {
        await updateProduct(route.params.id, payload)
        ElMessage.success('修改成功')
      } else {
        await createProduct(payload)
        ElMessage.success('发布成功')
      }
      router.push('/merchant')
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
