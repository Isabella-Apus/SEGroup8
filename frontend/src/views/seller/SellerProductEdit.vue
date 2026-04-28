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
        <!-- 商品名称 -->
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="100" show-word-limit />
        </el-form-item>

        <!-- 商品描述 -->
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

        <!-- 价格 -->
        <el-form-item label="价格" prop="price">
          <el-input-number
            v-model="form.price"
            :min="0.01"
            :precision="2"
            :step="1"
            style="width: 200px"
          />
          <span style="margin-left: 8px; color: #999">元</span>
        </el-form-item>

        <!-- 库存 -->
        <el-form-item label="库存" prop="stock">
          <el-input-number
            v-model="form.stock"
            :min="0"
            :step="1"
            style="width: 200px"
          />
          <span style="margin-left: 8px; color: #999">件</span>
        </el-form-item>

        <!-- 商品分类 -->
        <el-form-item label="商品分类" prop="categoryPath">
          <el-cascader
            v-model="form.categoryPath"
            :options="allowedCategoryTree"
            :props="cascaderProps"
            clearable
            filterable
            placeholder="先选一级，再选二级"
            style="width: 320px"
          />
        </el-form-item>

        <!-- 商品图片 -->
        <el-form-item label="商品图片" prop="imageUrl">
          <div class="upload-area">
            <el-upload
              :show-file-list="false"
              :before-upload="beforeUpload"
              :http-request="handleUpload"
              accept="image/*"
            >
              <div v-if="form.imageUrl" class="preview-wrap">
                <el-image
                  :src="toFullImageUrl(form.imageUrl)"
                  fit="cover"
                  style="width: 120px; height: 120px; border-radius: 6px"
                />
                <div class="preview-mask">点击更换</div>
              </div>
              <div v-else class="upload-placeholder">
                <el-icon style="font-size: 28px; color: #bbb"><Plus /></el-icon>
                <div style="margin-top: 8px; color: #bbb; font-size: 13px">点击上传图片</div>
              </div>
            </el-upload>
          </div>
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            支持 jpg / png，大小不超过 2MB
          </div>
        </el-form-item>

        <!-- 提交按钮 -->
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
import { Plus } from '@element-plus/icons-vue'
import {
  getProductDetail,
  createProduct,
  updateProduct,
  uploadImage
} from '@/api/seller'
import { getMyMerchantApplicationApi } from '@/api/merchantApplication';
import { buildCategoryPath, cascaderForMainCategory } from '@/constants/categories';

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)

// 判断是编辑还是新建
const isEdit = computed(() => !!route.params.id)

const form = reactive({
  name: '',
  description: '',
  price: 0.01,
  stock: 0,
  categoryPath: [],
  imageUrl: ''
})

const allowedCategoryTree = ref([]);
const sellerMainCategoryId = ref(null);

const cascaderProps = {
  emitPath: true,
  checkStrictly: false,
  value: 'value',
  label: 'label',
  children: 'children',
};

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
  categoryPath: [{ required: true, message: '请选择一级和二级分类', trigger: 'change' }],
  imageUrl: [{ required: true, message: '请上传商品图片', trigger: 'change' }]
}

// 如果是编辑，加载已有数据
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
    form.imageUrl = d.cover || ''
    form.categoryPath = buildCategoryPath(d.categoryId, d.subCategoryId)
  } catch (e) {
    ElMessage.error('加载商品信息失败')
  } finally {
    loading.value = false
  }
}

async function loadMerchantCategoryConstraint() {
  try {
    const res = await getMyMerchantApplicationApi();
    const categoryId = Number(res.data?.categoryId || 0);
    if (categoryId > 0) {
      sellerMainCategoryId.value = categoryId;
      allowedCategoryTree.value = cascaderForMainCategory(categoryId);
      return;
    }
  } catch (e) {
    // ignore and fallback
  }
  allowedCategoryTree.value = [];
}

function toFullImageUrl(url) {
  if (!url) {
    return ''
  }
  return url.startsWith('http') ? url : `http://localhost:8080${url}`
}

// 上传前校验
function beforeUpload(file) {
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

// 自定义上传
async function handleUpload({ file }) {
  try {
    const res = await uploadImage(file)
    form.imageUrl = res.data.url
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败')
  }
}

// 提交表单
async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        categoryId: form.categoryPath?.[0],
        subCategoryId: form.categoryPath?.[1],
        name: form.name,
        description: form.description,
        price: form.price,
        stock: form.stock,
        cover: form.imageUrl,
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
  await loadMerchantCategoryConstraint();
  await loadDetail();
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
.upload-area {
  display: inline-block;
}
.preview-wrap {
  position: relative;
  width: 120px;
  height: 120px;
  cursor: pointer;
}
.preview-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  opacity: 0;
  transition: opacity 0.2s;
  font-size: 13px;
}
.preview-wrap:hover .preview-mask {
  opacity: 1;
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
</style>