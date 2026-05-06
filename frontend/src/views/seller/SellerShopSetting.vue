<template>
  <div class="fade-in-up">
    <div class="page-header">
      <h2 class="page-title">🏪 店铺信息</h2>
    </div>

    <!-- 店铺预览卡片 -->
    <el-card class="preview-card">
      <div class="shop-preview">
        <div class="shop-banner" :style="bannerStyle">
          <div class="shop-avatar-wrap">
            <el-avatar :src="form.avatarUrl" :size="70" class="shop-avatar">
              {{ form.shopName?.[0] || '店' }}
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
            <el-tag size="small" type="warning" v-if="form.businessHours">🕐 {{ form.businessHours }}</el-tag>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 表单 -->
    <el-card style="margin-top: 16px" v-loading="loading">
      <el-tabs v-model="activeTab">

        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="110px"
            style="max-width: 620px; margin-top: 16px"
          >
            <el-form-item label="店铺名称" prop="shopName">
              <el-input v-model="form.shopName" disabled placeholder="来自入驻申请，审核后自动回填" />
            </el-form-item>
            <el-form-item label="店铺简介" prop="shopDesc">
              <el-input
                v-model="form.shopDesc"
                type="textarea"
                :rows="3"
                maxlength="200"
                show-word-limit
                placeholder="简短介绍你的店铺，吸引买家..."
              />
            </el-form-item>
            <el-form-item label="主营类目" prop="category">
              <el-input v-model="form.category" disabled placeholder="来自入驻申请，审核后自动回填" />
            </el-form-item>
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="13800000000" style="width: 240px" />
            </el-form-item>
            <el-form-item label="负责人姓名" prop="shopContactName">
              <el-input v-model="form.shopContactName" disabled placeholder="来自入驻申请，审核后自动回填" style="width: 240px" />
            </el-form-item>
            <el-form-item label="负责人电话" prop="shopContactPhone">
              <el-input v-model="form.shopContactPhone" disabled placeholder="来自入驻申请，审核后自动回填" style="width: 240px" />
            </el-form-item>
            <el-form-item label="所在地区" prop="region">
              <el-input v-model="form.region" disabled placeholder="来自入驻申请，审核后自动回填" style="width: 240px" />
            </el-form-item>
            <el-form-item label="仓库地址" prop="warehouseAddr">
              <el-input
                v-model="form.warehouseAddr"
                disabled
                type="textarea"
                :rows="2"
                maxlength="255"
                show-word-limit
                placeholder="来自入驻申请，审核后自动回填"
                style="width: 360px"
              />
            </el-form-item>
            <el-form-item label="身份证(脱敏)">
              <el-input v-model="form.idCardNoMasked" disabled style="width: 240px" />
            </el-form-item>
            <el-form-item label="营业时间">
              <el-input
                v-model="form.businessHours"
                placeholder="例如：周一至周日 9:00-21:00"
                style="width: 300px"
              />
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
                    {{ form.shopName?.[0] || '店' }}
                  </el-avatar>
                  <div class="avatar-mask">更换头像</div>
                </div>
              </el-upload>
              <div class="media-tip">建议尺寸 200×200，支持 jpg / png，大小不超过 2MB</div>
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
                    <div style="color: #bbb; margin-top: 8px; font-size: 13px">点击上传封面图</div>
                  </div>
                </div>
              </el-upload>
              <div class="media-tip">建议尺寸 1200×300，支持 jpg / png，大小不超过 2MB</div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 店铺政策 -->
        <el-tab-pane label="店铺政策" name="policy">
          <el-form label-width="110px" style="max-width: 620px; margin-top: 16px">
            <el-form-item label="退换货政策">
              <el-input
                v-model="form.returnPolicy"
                type="textarea"
                :rows="4"
                placeholder="例如：支持7天无理由退换货，商品需保持原包装未使用..."
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="发货说明">
              <el-input
                v-model="form.shippingPolicy"
                type="textarea"
                :rows="3"
                placeholder="例如：付款后1-3个工作日内发货，默认顺丰快递..."
                maxlength="300"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="店铺公告">
              <el-input
                v-model="form.announcement"
                type="textarea"
                :rows="3"
                placeholder="店铺最新公告或活动信息，买家进店可见..."
                maxlength="300"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>

      </el-tabs>

      <!-- 底部按钮 -->
      <div class="form-footer">
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          💾 保存设置
        </el-button>
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
import { uploadImage, updateShopProfile } from '@/api/seller'
import { toApiAssetUrl } from '@/utils/url'

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
  shopContactName: '',
  shopContactPhone: '',
  warehouseAddr: '',
  idCardNoMasked: '',
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
    ? `url(${form.bannerUrl}) center/cover no-repeat`
    : 'linear-gradient(135deg, #1d9e75 0%, #0f6e56 100%)'
}))

const rules = {
  phone: [{
    pattern: /^1[3-9]\d{9}$/,
    message: '请输入正确的手机号格式',
    trigger: 'blur'
  }]
}

// 拼接图片完整URL用于预览
function toFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return toApiAssetUrl(url)
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
    form.shopContactName = info.shopContactName || ''
    form.shopContactPhone = info.shopContactPhone || ''
    form.warehouseAddr = info.warehouseAddr || ''
    form.idCardNoMasked = info.idCardNoMasked || ''
    form.businessHours = info.businessHours || ''
    form.avatarUrl = toFullUrl(info.avatar)
    form.bannerUrl = toFullUrl(info.bannerUrl)
    form.returnPolicy = info.returnPolicy || ''
    form.shippingPolicy = info.shippingPolicy || ''
    form.announcement = info.announcement || ''
    initialForm = { ...form }
  } catch {
    ElMessage.error('加载店铺信息失败，请刷新重试')
  } finally {
    loading.value = false
  }
}

function beforeUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) { ElMessage.error('只能上传图片文件'); return false }
  if (!isLt2M) { ElMessage.error('图片大小不能超过 2MB'); return false }
  return true
}

async function handleUpload({ file }, field) {
  try {
    const res = await uploadImage(file)
    // 只存相对路径，预览时再拼完整URL
    const url = res.data?.url || res.data
    form[field] = toFullUrl(url)
    ElMessage.success('上传成功')
  } catch {
    ElMessage.error('图片上传失败，请重试')
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updateShopProfile({
      shopDesc: form.shopDesc,
      bannerUrl: form.bannerUrl,
      phone: form.phone,
      businessHours: form.businessHours,
      avatar: form.avatarUrl,
      returnPolicy: form.returnPolicy,
      shippingPolicy: form.shippingPolicy,
      announcement: form.announcement
    })
    await userStore.fetchProfile()
    ElMessage.success('店铺信息保存成功！')
    initialForm = { ...form }
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  Object.assign(form, initialForm)
  formRef.value?.clearValidate()
  ElMessage.info('已重置为上次保存的内容')
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
.page-title { margin: 0; font-size: 20px; font-weight: 600; }

.preview-card { margin-bottom: 4px; }
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
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.shop-preview-info { padding: 36px 16px 8px; }
.shop-preview-name { font-size: 18px; font-weight: 600; margin-bottom: 4px; }
.shop-preview-desc { font-size: 13px; color: #666; margin-bottom: 8px; }
.shop-preview-tags { display: flex; gap: 8px; flex-wrap: wrap; }

.media-section { margin-bottom: 16px; }
.media-label { font-size: 14px; font-weight: 500; margin-bottom: 10px; color: #333; }
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
  background: rgba(0,0,0,0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s;
  font-size: 12px;
  text-align: center;
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

.form-footer {
  margin-top: 24px;
  padding-left: 110px;
  display: flex;
  gap: 12px;
}
</style>
