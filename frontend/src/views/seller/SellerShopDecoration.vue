<template>
  <div class="decoration-editor">
    <!-- 顶部工具栏 -->
    <div class="editor-toolbar">
      <div class="toolbar-left">
        <h2 class="toolbar-title">🎨 店铺装修</h2>
        <el-radio-group v-model="previewMode" size="small">
          <el-radio-button label="pc">💻 电脑端</el-radio-button>
          <el-radio-button label="mobile">📱 手机端</el-radio-button>
        </el-radio-group>
      </div>
      <div class="toolbar-right">
        <el-button @click="handlePreview">👁 预览</el-button>
        <el-button @click="handleClear" plain>🗑 清空</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">💾 保存发布</el-button>
      </div>
    </div>

    <div class="editor-body">
      <!-- 左侧：组件面板 -->
      <div class="editor-panel">
        <div class="panel-title">添加组件</div>
        <div class="component-list">
          <div
            v-for="tmpl in COMPONENT_TEMPLATES"
            :key="tmpl.type"
            class="component-item"
            @click="addComponent(tmpl.type)"
          >
            <span class="component-icon">{{ tmpl.icon }}</span>
            <div class="component-info">
              <div class="component-name">{{ tmpl.label }}</div>
              <div class="component-desc">{{ tmpl.description }}</div>
            </div>
            <el-icon class="add-icon"><Plus /></el-icon>
          </div>
        </div>

        <!-- 全局设置 -->
        <div class="panel-title" style="margin-top: 16px">全局设置</div>
        <div class="global-settings">
          <div class="prop-group">
            <div class="prop-label">主题色</div>
            <div class="color-row">
              <div
                v-for="c in themeColors"
                :key="c"
                class="color-dot"
                :style="{
                  background: c,
                  outline: globalSettings.themeColor === c ? `3px solid ${c}` : 'none',
                  outlineOffset: '2px'
                }"
                @click="globalSettings.themeColor = c"
              />
              <el-color-picker v-model="globalSettings.themeColor" size="small" />
            </div>
          </div>
          <div class="prop-group">
            <div class="prop-label">页面背景色</div>
            <el-color-picker v-model="globalSettings.bgColor" />
          </div>
          <div class="prop-group">
            <div class="prop-label">组件间距（px）</div>
            <el-slider v-model="globalSettings.gap" :min="0" :max="32" />
          </div>
        </div>

        <!-- 已添加组件数 -->
        <div class="panel-count" v-if="components.length > 0">
          已添加 {{ components.length }} 个组件
        </div>
      </div>

      <!-- 中间：画布 -->
      <div class="editor-canvas-wrap">
        <div
          class="editor-canvas"
          :class="previewMode"
          :style="{ background: globalSettings.bgColor }"
        >
          <!-- 店铺头部（固定展示） -->
          <div class="canvas-shop-header" :style="{ background: globalSettings.themeColor }">
            <el-avatar :src="shopInfo.avatarUrl" :size="50" style="flex-shrink:0">
              {{ shopInfo.shopName?.[0] || '店' }}
            </el-avatar>
            <div class="canvas-shop-info">
              <div class="canvas-shop-name">{{ shopInfo.shopName || '我的店铺' }}</div>
              <div class="canvas-shop-desc">{{ shopInfo.shopDesc || '暂无简介' }}</div>
            </div>
          </div>

          <!-- 拖拽组件区 -->
          <draggable
            v-model="components"
            item-key="id"
            handle=".drag-handle"
            ghost-class="drag-ghost"
            animation="200"
          >
            <template #item="{ element }">
              <div
                class="canvas-component"
                :class="{ selected: selectedId === element.id }"
                :style="{ marginBottom: globalSettings.gap + 'px' }"
                @click.stop="selectedId = element.id"
              >
                <div class="component-controls">
                  <el-icon class="drag-handle"><Rank /></el-icon>
                  <span class="component-type-label">
                    {{ COMPONENT_TEMPLATES.find(t => t.type === element.type)?.icon }}
                    {{ COMPONENT_TEMPLATES.find(t => t.type === element.type)?.label }}
                  </span>
                  <el-icon class="delete-btn" @click.stop="deleteComponent(element.id)"><Delete /></el-icon>
                </div>
                <div class="component-content" style="padding: 8px">
                  <ComponentRenderer :component="element" :theme-color="globalSettings.themeColor" />
                </div>
              </div>
            </template>
          </draggable>

          <div v-if="components.length === 0" class="canvas-empty">
            <div style="font-size:32px;margin-bottom:8px">🎨</div>
            <div>从左侧点击组件，添加到页面</div>
          </div>
        </div>
      </div>

      <!-- 右侧：属性面板 -->
      <div class="editor-props">
        <div v-if="selectedComponent">
          <PropEditor
            :component="selectedComponent"
            @delete="deleteComponent(selectedId)"
          />
        </div>
        <div v-else class="props-empty">
          <div style="font-size:24px;margin-bottom:8px">✏️</div>
          <div>点击画布中的组件<br>进行属性编辑</div>
        </div>
      </div>
    </div>

    <!-- 预览弹窗 -->
    <el-dialog v-model="previewVisible" title="📱 店铺预览" width="420px" align-center>
      <div class="preview-modal" :style="{ background: globalSettings.bgColor }">
        <div class="canvas-shop-header" :style="{ background: globalSettings.themeColor }">
          <el-avatar :src="shopInfo.avatarUrl" :size="40" style="flex-shrink:0">
            {{ shopInfo.shopName?.[0] || '店' }}
          </el-avatar>
          <div class="canvas-shop-info">
            <div class="canvas-shop-name" style="font-size:14px">{{ shopInfo.shopName || '我的店铺' }}</div>
            <div class="canvas-shop-desc" style="font-size:11px">{{ shopInfo.shopDesc }}</div>
          </div>
        </div>
        <div v-for="comp in components" :key="comp.id" style="padding: 8px 12px">
          <ComponentRenderer :component="comp" :theme-color="globalSettings.themeColor" />
        </div>
        <div v-if="components.length === 0" style="text-align:center;padding:40px;color:#999">
          暂未添加任何组件
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Rank } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import { useUserStore } from '@/stores/user'
import { getCurrentSellerShopApi, saveShopDecorationApi } from '@/api/shop'
import ComponentRenderer from './decoration/ComponentRenderer.vue'
import PropEditor from './decoration/PropEditor.vue'
import { COMPONENT_TEMPLATES, createComponent } from './decoration/componentConfig.js'

const userStore = useUserStore()
const DECORATION_STORAGE_KEY = 'shop_decoration_v2'
const saving = ref(false)
const previewVisible = ref(false)
const previewMode = ref('pc')
const selectedId = ref(null)

const shopInfo = reactive({
  shopName: '',
  shopDesc: '',
  avatarUrl: ''
})

const themeColors = ['#1d9e75', '#409eff', '#e4393c', '#ff6600', '#9b59b6', '#2c3e50', '#f39c12']

const globalSettings = reactive({
  themeColor: '#1d9e75',
  bgColor: '#f5f7fa',
  gap: 12
})

const components = ref([])

const selectedComponent = computed(() =>
  components.value.find(c => c.id === selectedId.value) || null
)

function addComponent(type) {
  const comp = createComponent(type)
  if (comp) {
    components.value.push(comp)
    selectedId.value = comp.id
    ElMessage.success(`已添加「${COMPONENT_TEMPLATES.find(t => t.type === type)?.label}」`)
  }
}

function deleteComponent(id) {
  components.value = components.value.filter(c => c.id !== id)
  if (selectedId.value === id) selectedId.value = null
}

async function handleClear() {
  if (components.value.length === 0) return ElMessage.info('画布已经是空的')
  await ElMessageBox.confirm('确定清空所有组件吗？此操作不可撤销。', '清空画布', {
    confirmButtonText: '确定清空',
    cancelButtonText: '取消',
    type: 'warning'
  })
  components.value = []
  selectedId.value = null
  ElMessage.success('已清空画布')
}

function handlePreview() {
  previewVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    const decoration = {
      globalSettings: { ...globalSettings },
      components: components.value
    }
    await saveShopDecorationApi(decoration)
    localStorage.setItem(DECORATION_STORAGE_KEY, JSON.stringify(decoration))
    ElMessage.success('店铺装修已保存发布！')
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function loadData() {
  try {
    await userStore.fetchProfile()
    const info = userStore.userInfo || {}
    shopInfo.shopName = info.shopName || info.nickname || ''
    shopInfo.shopDesc = info.shopDesc || ''
    shopInfo.avatarUrl = info.avatar
      ? (info.avatar.startsWith('http') ? info.avatar : 'http://localhost:8080' + info.avatar)
      : ''

    const shopResult = await getCurrentSellerShopApi()
    const serverDecoration = shopResult.data?.decorationJson
    const saved = serverDecoration || localStorage.getItem(DECORATION_STORAGE_KEY)
    if (saved) {
      try {
        const parsed = JSON.parse(saved)
        Object.assign(globalSettings, parsed.globalSettings || {})
        components.value = parsed.components || []
      } catch {
        // 本地数据损坏，忽略
      }
    }
  } catch {
    ElMessage.error('加载店铺信息失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.decoration-editor {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 82px);
  height: calc(100dvh - 82px);
  min-height: 0;
  margin: -18px -22px -36px;
}
.editor-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.toolbar-left { display: flex; align-items: center; gap: 16px; }
.toolbar-right { display: flex; gap: 8px; }
.toolbar-title { margin: 0; font-size: 18px; font-weight: 600; }

.editor-body { display: flex; flex: 1; min-height: 0; overflow: hidden; }

.editor-panel {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  overflow-y: auto;
  padding: 12px;
}
.panel-title {
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}
.panel-count {
  text-align: center;
  font-size: 12px;
  color: #1d9e75;
  margin-top: 12px;
  padding: 6px;
  background: #f0fdf4;
  border-radius: 6px;
}
.component-list { display: flex; flex-direction: column; gap: 6px; }
.component-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.component-item:hover { border-color: #1d9e75; background: #f0fdf4; }
.component-icon { font-size: 20px; flex-shrink: 0; }
.component-info { flex: 1; min-width: 0; }
.component-name { font-size: 13px; font-weight: 500; color: #111827; }
.component-desc { font-size: 11px; color: #9ca3af; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.add-icon { color: #9ca3af; flex-shrink: 0; }

.global-settings { padding: 4px 0; }
.prop-group { margin-bottom: 12px; }
.prop-label { font-size: 12px; color: #555; margin-bottom: 6px; }
.color-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.color-dot {
  width: 24px; height: 24px;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.2s;
}
.color-dot:hover { transform: scale(1.2); }

.editor-canvas-wrap {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  background: #f0f2f5;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 20px 20px 48px;
}
.editor-canvas {
  background: #fff;
  width: 100%;
  max-width: 900px;
  min-height: 100%;
  flex: 0 0 auto;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.editor-canvas.mobile { max-width: 390px; }

.canvas-shop-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  color: #fff;
}
.canvas-shop-name { font-size: 16px; font-weight: 600; }
.canvas-shop-desc { font-size: 12px; opacity: 0.85; margin-top: 2px; }

.canvas-component {
  border: 2px solid transparent;
  border-radius: 4px;
  cursor: pointer;
  transition: border-color 0.2s;
  position: relative;
  margin: 0 8px;
}
.canvas-component:hover { border-color: #93c5fd; }
.canvas-component.selected { border-color: #1d9e75; }
.component-controls {
  display: none;
  position: absolute;
  top: -28px;
  left: 0;
  background: #1d9e75;
  color: #fff;
  border-radius: 4px 4px 0 0;
  padding: 4px 8px;
  font-size: 12px;
  align-items: center;
  gap: 8px;
  z-index: 10;
  white-space: nowrap;
}
.canvas-component.selected .component-controls,
.canvas-component:hover .component-controls { display: flex; }
.drag-handle { cursor: grab; }
.drag-handle:active { cursor: grabbing; }
.delete-btn { cursor: pointer; margin-left: 8px; }
.delete-btn:hover { color: #fca5a5; }
.component-type-label { font-size: 11px; }

.canvas-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #9ca3af;
  font-size: 14px;
}
.drag-ghost { opacity: 0.4; background: #e0f2fe; }

.editor-props {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  overflow-y: auto;
  padding: 16px;
}
.props-empty {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  margin-top: 40px;
  line-height: 1.8;
}

.preview-modal {
  border-radius: 8px;
  overflow: hidden;
  max-height: 70vh;
  overflow-y: auto;
}
</style>
