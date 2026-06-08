<template>
  <div class="component-wrapper">
    <!-- Banner -->
    <div v-if="component.type === 'banner'" class="comp-banner"
      :style="{ borderRadius: component.props.borderRadius + 'px', height: component.props.height + 'px', overflow: 'hidden' }">
      <el-image v-if="component.props.imageUrl" :src="component.props.imageUrl" fit="cover" style="width:100%;height:100%" />
      <div v-else class="comp-banner-placeholder">
        <span>🖼️ 点击编辑上传 Banner 图片</span>
      </div>
    </div>

    <!-- 商品展示 -->
    <div v-else-if="component.type === 'product_grid'" class="comp-product-grid">
      <div v-if="component.props.title" class="comp-section-title" :style="{ color: themeColor }">
        {{ component.props.title }}
      </div>
      <div class="comp-products" :style="{ gridTemplateColumns: `repeat(${component.props.columns}, 1fr)` }">
        <div v-for="p in component.props.products" :key="p.id" class="comp-product-card">
          <el-image v-if="p.cover" :src="toFullImageUrl(p.cover)" fit="cover" class="comp-product-img" />
          <div v-else class="comp-product-img-placeholder">📦</div>
          <div class="comp-product-name">{{ p.name }}</div>
          <div class="comp-product-price" :style="{ color: themeColor }">¥{{ p.price }}</div>
        </div>
        <div v-if="component.props.products.length === 0" class="comp-products-empty">
          点击编辑选择商品
        </div>
      </div>
    </div>

    <!-- 文字内容 -->
    <div v-else-if="component.type === 'text_block'" class="comp-text"
      :style="{ textAlign: component.props.align }">
      <div v-if="component.props.title" class="comp-text-title"
        :style="{ fontSize: component.props.titleSize + 'px' }">
        {{ component.props.title }}
      </div>
      <div v-if="component.props.content" class="comp-text-content"
        :style="{ fontSize: component.props.contentSize + 'px' }">
        {{ component.props.content }}
      </div>
      <div v-if="!component.props.title && !component.props.content" class="comp-empty-tip">
        点击编辑添加文字内容
      </div>
    </div>

    <!-- 公告栏 -->
    <div v-else-if="component.type === 'announcement'" class="comp-announcement"
      :style="{ background: component.props.bgColor, color: component.props.textColor }">
      <span>{{ component.props.icon }}</span>
      <span style="margin-left: 8px">{{ component.props.text }}</span>
    </div>

    <!-- 图文组合 -->
    <div v-else-if="component.type === 'image_text'" class="comp-image-text"
      :class="component.props.layout">
      <div class="comp-image-text-img" :style="{ width: component.props.imageWidth + '%' }">
        <el-image v-if="component.props.imageUrl" :src="component.props.imageUrl" fit="cover" style="width:100%;height:140px;border-radius:6px" />
        <div v-else class="comp-banner-placeholder" style="height:140px">🖼️ 上传图片</div>
      </div>
      <div class="comp-image-text-content">
        <div class="comp-text-title">{{ component.props.title || '标题' }}</div>
        <div class="comp-text-content">{{ component.props.content || '描述内容' }}</div>
      </div>
    </div>

    <!-- 分割线 -->
    <div v-else-if="component.type === 'divider'"
      :style="{ margin: component.props.margin + 'px 0', borderTop: `1px ${component.props.style} ${component.props.color}` }" />

    <!-- 倒计时 -->
    <div v-else-if="component.type === 'countdown'" class="comp-countdown"
      :style="{ background: component.props.bgColor, color: component.props.textColor }">
      <div class="comp-countdown-title">{{ component.props.title }}</div>
      <div class="comp-countdown-timer">
        <span class="comp-countdown-block">{{ countdown.hours }}</span>
        <span class="comp-countdown-sep">:</span>
        <span class="comp-countdown-block">{{ countdown.minutes }}</span>
        <span class="comp-countdown-sep">:</span>
        <span class="comp-countdown-block">{{ countdown.seconds }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { toAssetUrl } from '@/utils/url'

const props = defineProps({
  component: { type: Object, required: true },
  themeColor: { type: String, default: '#1d9e75' }
})

const countdown = ref({ hours: '00', minutes: '00', seconds: '00' })
let timer = null

function toFullImageUrl(url) {
  return toAssetUrl(url)
}

function updateCountdown() {
  if (props.component.type !== 'countdown' || !props.component.props.endTime) return
  const end = new Date(props.component.props.endTime).getTime()
  const now = Date.now()
  const diff = Math.max(0, end - now)
  const h = Math.floor(diff / 3600000)
  const m = Math.floor((diff % 3600000) / 60000)
  const s = Math.floor((diff % 60000) / 1000)
  countdown.value = {
    hours: String(h).padStart(2, '0'),
    minutes: String(m).padStart(2, '0'),
    seconds: String(s).padStart(2, '0')
  }
}

onMounted(() => {
  if (props.component.type === 'countdown') {
    updateCountdown()
    timer = setInterval(updateCountdown, 1000)
  }
})

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<style scoped>
.component-wrapper { width: 100%; }
.comp-banner { background: #f3f4f6; }
.comp-banner-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
  font-size: 14px;
  border-radius: 8px;
}
.comp-section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}
.comp-products {
  display: grid;
  gap: 10px;
}
.comp-product-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  padding-bottom: 8px;
}
.comp-product-img {
  width: 100%;
  height: 120px;
}
.comp-product-img-placeholder {
  width: 100%;
  height: 120px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
}
.comp-product-name {
  padding: 6px 8px 2px;
  font-size: 12px;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.comp-product-price {
  padding: 0 8px;
  font-size: 14px;
  font-weight: 600;
}
.comp-products-empty {
  grid-column: 1 / -1;
  text-align: center;
  color: #9ca3af;
  padding: 30px 0;
  font-size: 13px;
}
.comp-text { padding: 4px 0; }
.comp-text-title {
  font-weight: 600;
  color: #111827;
  margin-bottom: 6px;
}
.comp-text-content {
  color: #4b5563;
  line-height: 1.6;
  white-space: pre-wrap;
}
.comp-empty-tip {
  color: #9ca3af;
  font-size: 13px;
  text-align: center;
  padding: 16px 0;
}
.comp-announcement {
  padding: 10px 16px;
  border-radius: 6px;
  font-size: 14px;
  display: flex;
  align-items: center;
}
.comp-image-text {
  display: flex;
  gap: 16px;
  align-items: center;
}
.comp-image-text.image-right {
  flex-direction: row-reverse;
}
.comp-image-text-content { flex: 1; }
.comp-countdown {
  padding: 16px;
  border-radius: 8px;
  text-align: center;
}
.comp-countdown-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 10px;
}
.comp-countdown-timer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}
.comp-countdown-block {
  background: rgba(0,0,0,0.2);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 24px;
  font-weight: 700;
  min-width: 50px;
  text-align: center;
}
.comp-countdown-sep {
  font-size: 24px;
  font-weight: 700;
}
</style>
