<template>
  <div class="fade-in-up">
    <div class="page-header">
      <h2 class="page-title">Shop Information</h2>
    </div>

    <!-- 店铺预览卡片 -->
    <el-card class="preview-card">
      <div class="shop-preview">
        <div class="shop-banner" :style="bannerStyle">
          <div class="shop-avatar-wrap">
            <el-avatar :src="form.avatarUrl" :size="70" class="shop-avatar">
              {{ form.shopName?.[0] || 'S' }}
            </el-avatar>
          </div>
        </div>
        <div class="shop-preview-info">
          <div class="shop-preview-name">{{ form.shopName || '店铺名称' }}</div>
          <div class="shop-preview-desc">{{ form.shopDesc || '暂无简介' }}</div>
          <div class="shop-preview-tags">
            <el-tag size="small" type="success" v-if="form.region">📍 {{ form.region }}</el-tag>
            <el-tag size="small" type="info" v-if="form.category">🏷️ {{ form.category }}</el-tag>
            <el-tag size="small" v-if="form.phone">📞 {{ form.phone }}</el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 表单 -->
    <el-card style="margin-top: 16px" v-loading="loading">
      <el-tabs v-model="activeTab">

        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width: 600px; margin-top: 16px">
            <el-form-item label="店铺名称" prop="shopName">
              <el-input v-model="form.shopName" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="店铺简介" prop="shopDesc">
              <el-input v-model="form.shopDesc" type="textarea" :rows="3" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item label="主营类目" prop="category">
              <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
                <el-option label="电子数码" value="电子数码" />
                <el-option label="服装鞋帽" value="服装鞋帽" />
                <el-option label="食品饮料" value="食品饮料" />
                <el-option label="家居用品" value="家居用品" />
                <el-option label="运动户外" value="运动户外" />
                <el-option label="图书文具" value="图书文具" />
                <el-option label="美妆护肤" value="美妆护肤" />
                <el-option label="母婴用品" value="母婴用品" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="13800000000" style="width: 240px" />
            </el-form-item>
            <el-form-item label="所在地区" prop="region">
              <el-input v-model="form.region" placeholder="例如：广东省广州市" style="width: 240px" />
            </el-form-item>
            <el-form-item label="营业时间">
              <el-input v-model="form.businessHours" placeholder="例如：周一至周日 9:00-21:00" style="width: 300px" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 头像与封面 -->
        <el-tab-pane label="头像与封面" name="media">
          <div style="margin-top: 16px">
            <div class="media-section">
              <div class="media-label">店铺头像</div>
              <el-upload
                :show-file-list="false"
                :before-upload="beforeUpload"
                :http-request="(opt) => handleUpload(opt, 'avatarUrl')"
                accept="image/*"
              >
                <div class="avatar-wrap">
                  <el-avatar :src="form.avatarUrl" :size="80">
                    {{ form.shopName?.[0] || 'S' }}
                  </el-avatar>
                  <div class="avatar-mask">更换</div>
                </div>
              </el-upload>
              <div class="media-tip">建议尺寸 200x200，支持 jpg/png</div>
            </div>

            <el-divider />

            <div class="media-section">
              <div class="media-label">店铺封面</div>
              <el-upload
                :show-file-list="false"
                :before-upload="beforeUpload"
                :http-request="(opt) => handleUpload(opt, 'bannerUrl')"
                accept="image/*"
              >
                <div class="banner-upload">
                  <el-image
                    v-if="form.bannerUrl"
                    :src="form.bannerUrl"
                    fit="cover"
                    style="width: 100%; height: 100%; border-radius: 8px"
                  />
                  <div v-else class="banner-placeholder">
                    <el-icon style="font-size: 32px; color: #bbb"><Plus /></el-icon>
                    <div style="color: #bbb; margin-top: 8px">点击上传封面图</div>
                  </div>
                </div>
              </el-upload>
              <div class="media-tip">建议尺寸 1200x300，支持 jpg/png</div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 店铺政策 -->
        <el-tab-pane label="店铺政策" name="policy">
          <el-form label-width="110px" style="max-width: 600px; margin-top: 16px">
            <el-form-item label="退换货政策">
              <el-input
                v-model="form.returnPolicy"
                type="textarea"
                :rows="4"
                placeholder="例如：支持7天无理由退换货，商品需保持原包装..."
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="发货说明">
              <el-input
                v-model="form.shippingPolicy"
                type="textarea"
                :rows="3"
                placeholder="例如：付款后1-3个工作日内发货..."
                maxlength="300"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="店铺公告">
              <el-input
                v-model="form.announcement"
                type="textarea"
                :rows="3"
                placeholder="店铺最新公告或活动信息..."
                maxlength="300"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>

      </el-tabs>

      <div style="margin-top: 20px; padding-left: 110px">
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存设置</el-button>
        <el-button @click="resetForm">重置</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { uploadImage } from '@/api/seller'
import { updateShopProfile } from '@/api/seller'

const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('basic')

const form = reactive({
  shopName: '',
  shopDesc: '',
  category: '',
  phone: '',
  region: '',
  businessHours: '',
  avatarUrl: '',
  bannerUrl: '',
  returnPolicy: '',
  shippingPolicy: '',
  announcement: ''
})

let initialForm = {}

const bannerStyle = computed(() => ({
  background: form.bannerUrl
    ? `url(${form.bannerUrl}) center/cover`
    : 'linear-gradient(135deg, #1d9e75 0%, #0f6e56 100%)'
}))

const rules = {
  shopName: [
    { required: true, message: '请输入店铺名称', trigger: 'blur' },
    { min: 2, max: 50, message: '2~50个字符', trigger: 'blur' }
  ],
  phone: [{
    pattern: /^1[3-9]\d{9}$/,
    message: '请输入正确的手机号',
    trigger: 'blur'
  }]
}

async function loadShopInfo() {
  loading.value = true
  try {
    await userStore.fetchProfile()
    const info = userStore.userInfo || {}
    form.shopName = info.shopName || info.nickname || ''
    form.shopDesc = info.shopDesc || ''
    form.category = info.category || ''
    form.phone = info.phone || ''
    form.region = info.region || ''
    form.businessHours = info.businessHours || ''
    form.avatarUrl = info.avatar ? (info.avatar.startsWith('http') ? info.avatar : 'http://localhost:8080' + info.avatar) : ''
    form.bannerUrl = info.bannerUrl ? (info.bannerUrl.startsWith('http') ? info.bannerUrl : 'http://localhost:8080' + info.bannerUrl) : ''
    form.returnPolicy = info.returnPolicy || ''
    form.shippingPolicy = info.shippingPolicy || ''
    form.announcement = info.announcement || ''
    initialForm = { ...form }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) { ElMessage.error('只能上传图片'); return false }
  if (!isLt2M) { ElMessage.error('图片不能超过2MB'); return false }
  return true
}

async function handleUpload({ file }, field) {
  try {
    const res = await uploadImage(file)
    form[field] = res.data.url  // 只存相对路径，不拼前缀
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

async function handleSubmit() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await updateShopProfile({
        nickname: form.shopName,
        shopName: form.shopName,
        shopDesc: form.shopDesc,
        bannerUrl: form.bannerUrl,
        category: form.category,
        phone: form.phone,
        region: form.region,
        businessHours: form.businessHours,
        avatar: form.avatarUrl,
        returnPolicy: form.returnPolicy,
        shippingPolicy: form.shippingPolicy,
        announcement: form.announcement
      })
      await userStore.fetchProfile()
      ElMessage.success('保存成功')
      initialForm = { ...form }
    } catch (e) {
      ElMessage.error('保存失败')
    } finally {
      submitting.value = false
    }
  })
}

function resetForm() {
  Object.assign(form, initialForm)
  formRef.value?.clearValidate()
}

onMounted(loadShopInfo)
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-title { margin: 0; font-size: 20px; }
.preview-card { margin-bottom: 4px; }
.shop-preview { }
.shop-banner {
  height: 120px;
  border-radius: 8px;
  position: relative;
  margin-bottom: 12px;
}
.shop-avatar-wrap {
  position: absolute;
  bottom: -30px;
  left: 20px;
  border: 3px solid #fff;
  border-radius: 50%;
}
.shop-preview-info {
  padding: 36px 16px 8px;
}
.shop-preview-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}
.shop-preview-desc {
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}
.shop-preview-tags { display: flex; gap: 8px; flex-wrap: wrap; }
.media-section { margin-bottom: 16px; }
.media-label {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 10px;
  color: #333;
}
.media-tip { font-size: 12px; color: #999; margin-top: 6px; }
.avatar-wrap {
  position: relative;
  width: 80px;
  height: 80px;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}
.avatar-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.4);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s;
  font-size: 13px;
}
.avatar-wrap:hover .avatar-mask { opacity: 1; }
.banner-upload {
  width: 480px;
  height: 120px;
  border: 1px dashed #ddd;
  border-radius: 8px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
}
.banner-upload:hover { border-color: #1d9e75; }
.banner-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
}
</style>