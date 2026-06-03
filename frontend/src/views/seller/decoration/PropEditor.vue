<template>
  <div class="prop-editor">
    <div class="prop-editor-title">
      <span>编辑组件</span>
      <el-button text type="danger" size="small" @click="$emit('delete')">删除</el-button>
    </div>

    <!-- Banner 编辑 -->
    <template v-if="component.type === 'banner'">
      <div class="prop-group">
        <div class="prop-label">Banner 图片</div>
        <el-upload :show-file-list="false" :before-upload="beforeUpload"
          :http-request="(opt) => handleUpload(opt, 'imageUrl')" accept="image/*">
          <el-image v-if="component.props.imageUrl" :src="component.props.imageUrl"
            style="width:100%;height:80px;border-radius:6px" fit="cover" />
          <el-button v-else size="small" style="width:100%">上传图片</el-button>
        </el-upload>
      </div>
      <div class="prop-group">
        <div class="prop-label">高度 (px)</div>
        <el-slider v-model="component.props.height" :min="100" :max="400" show-input />
      </div>
      <div class="prop-group">
        <div class="prop-label">圆角 (px)</div>
        <el-slider v-model="component.props.borderRadius" :min="0" :max="24" show-input />
      </div>
    </template>

    <!-- 商品展示编辑 -->
    <template v-else-if="component.type === 'product_grid'">
      <div class="prop-group">
        <div class="prop-label">标题</div>
        <el-input v-model="component.props.title" placeholder="例如：本周上架" />
      </div>
      <div class="prop-group">
        <div class="prop-label">列数</div>
        <el-radio-group v-model="component.props.columns">
          <el-radio-button :value="2">2列</el-radio-button>
          <el-radio-button :value="4">4列</el-radio-button>
        </el-radio-group>
      </div>
      <div class="prop-group">
        <div class="prop-label">选择商品</div>
        <div class="selected-products">
          <el-tag
            v-for="p in component.props.products"
            :key="p.id"
            closable
            @close="removeProduct(p.id)"
            style="margin: 0 4px 4px 0"
          >
            {{ p.name }}
          </el-tag>
        </div>
        <el-button size="small" @click="productPickerVisible = true" style="margin-top: 8px">
          + 添加商品
        </el-button>
      </div>
    </template>

    <!-- 文字内容编辑 -->
    <template v-else-if="component.type === 'text_block'">
      <div class="prop-group">
        <div class="prop-label">标题</div>
        <el-input v-model="component.props.title" placeholder="输入标题（可留空）" />
      </div>
      <div class="prop-group">
        <div class="prop-label">正文</div>
        <el-input v-model="component.props.content" type="textarea" :rows="4"
          placeholder="输入正文内容" />
      </div>
      <div class="prop-group">
        <div class="prop-label">对齐方式</div>
        <el-radio-group v-model="component.props.align">
          <el-radio-button value="left">左对齐</el-radio-button>
          <el-radio-button value="center">居中</el-radio-button>
          <el-radio-button value="right">右对齐</el-radio-button>
        </el-radio-group>
      </div>
      <div class="prop-group">
        <div class="prop-label">标题字号</div>
        <el-slider v-model="component.props.titleSize" :min="12" :max="32" show-input />
      </div>
      <div class="prop-group">
        <div class="prop-label">正文字号</div>
        <el-slider v-model="component.props.contentSize" :min="12" :max="24" show-input />
      </div>
    </template>

    <!-- 公告栏编辑 -->
    <template v-else-if="component.type === 'announcement'">
      <div class="prop-group">
        <div class="prop-label">公告内容</div>
        <el-input v-model="component.props.text" type="textarea" :rows="2" />
      </div>
      <div class="prop-group">
        <div class="prop-label">图标</div>
        <div class="icon-picker">
          <span v-for="icon in icons" :key="icon"
            class="icon-option"
            :class="{ active: component.props.icon === icon }"
            @click="component.props.icon = icon">
            {{ icon }}
          </span>
        </div>
      </div>
      <div class="prop-group">
        <div class="prop-label">背景色</div>
        <el-color-picker v-model="component.props.bgColor" />
      </div>
      <div class="prop-group">
        <div class="prop-label">文字颜色</div>
        <el-color-picker v-model="component.props.textColor" />
      </div>
    </template>

    <!-- 图文组合编辑 -->
    <template v-else-if="component.type === 'image_text'">
      <div class="prop-group">
        <div class="prop-label">图片</div>
        <el-upload :show-file-list="false" :before-upload="beforeUpload"
          :http-request="(opt) => handleUpload(opt, 'imageUrl')" accept="image/*">
          <el-image v-if="component.props.imageUrl" :src="component.props.imageUrl"
            style="width:100%;height:80px;border-radius:6px" fit="cover" />
          <el-button v-else size="small" style="width:100%">上传图片</el-button>
        </el-upload>
      </div>
      <div class="prop-group">
        <div class="prop-label">布局</div>
        <el-radio-group v-model="component.props.layout">
          <el-radio-button value="image-left">图左文右</el-radio-button>
          <el-radio-button value="image-right">图右文左</el-radio-button>
        </el-radio-group>
      </div>
      <div class="prop-group">
        <div class="prop-label">标题</div>
        <el-input v-model="component.props.title" />
      </div>
      <div class="prop-group">
        <div class="prop-label">描述</div>
        <el-input v-model="component.props.content" type="textarea" :rows="3" />
      </div>
      <div class="prop-group">
        <div class="prop-label">图片宽度 %</div>
        <el-slider v-model="component.props.imageWidth" :min="20" :max="60" show-input />
      </div>
    </template>

    <!-- 分割线编辑 -->
    <template v-else-if="component.type === 'divider'">
      <div class="prop-group">
        <div class="prop-label">线条样式</div>
        <el-radio-group v-model="component.props.style">
          <el-radio-button value="solid">实线</el-radio-button>
          <el-radio-button value="dashed">虚线</el-radio-button>
          <el-radio-button value="dotted">点线</el-radio-button>
        </el-radio-group>
      </div>
      <div class="prop-group">
        <div class="prop-label">颜色</div>
        <el-color-picker v-model="component.props.color" />
      </div>
      <div class="prop-group">
        <div class="prop-label">上下间距 (px)</div>
        <el-slider v-model="component.props.margin" :min="0" :max="48" show-input />
      </div>
    </template>

    <!-- 倒计时编辑 -->
    <template v-else-if="component.type === 'countdown'">
      <div class="prop-group">
        <div class="prop-label">标题</div>
        <el-input v-model="component.props.title" />
      </div>
      <div class="prop-group">
        <div class="prop-label">结束时间</div>
        <el-date-picker
          v-model="component.props.endTime"
          type="datetime"
          placeholder="选择结束时间"
          format="YYYY-MM-DD HH:mm"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 100%"
        />
      </div>
      <div class="prop-group">
        <div class="prop-label">背景色</div>
        <el-color-picker v-model="component.props.bgColor" />
      </div>
      <div class="prop-group">
        <div class="prop-label">文字颜色</div>
        <el-color-picker v-model="component.props.textColor" />
      </div>
    </template>

    <!-- 商品选择弹窗 -->
    <el-dialog v-model="productPickerVisible" title="选择商品" width="480px" append-to-body>
      <el-table :data="allProducts" @row-click="addProduct" style="cursor:pointer">
        <el-table-column prop="name" label="商品名称" />
        <el-table-column prop="price" label="价格" width="90">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column width="60">
          <template #default="{ row }">
            <el-icon v-if="isSelected(row.id)" color="#1d9e75"><Check /></el-icon>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { uploadImage, getMyProducts } from '@/api/seller'

const props = defineProps({
  component: { type: Object, required: true }
})
defineEmits(['delete'])

const productPickerVisible = ref(false)
const allProducts = ref([])
const icons = ['📢', '🔥', '⭐', '🎉', '💥', '🎁', '⚡', '✨']

onMounted(async () => {
  const res = await getMyProducts({ page: 1, pageSize: 100 })
  allProducts.value = res.data.records || []
})

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
    props.component.props[field] = 'http://localhost:8080' + res.data.url
    ElMessage.success('上传成功')
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

function addProduct(row) {
  if (!props.component.props.products) props.component.props.products = []
  if (!isSelected(row.id)) {
    props.component.props.products.push({ id: row.id, name: row.name, price: row.price, cover: row.cover })
  }
}

function removeProduct(id) {
  props.component.props.products = props.component.props.products.filter(p => p.id !== id)
}

function isSelected(id) {
  return props.component.props.products?.some(p => p.id === id)
}
</script>

<style scoped>
.prop-editor { padding: 4px 0; }
.prop-editor-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}
.prop-group { margin-bottom: 16px; }
.prop-label {
  font-size: 13px;
  color: #555;
  margin-bottom: 6px;
  font-weight: 500;
}
.icon-picker { display: flex; gap: 8px; flex-wrap: wrap; }
.icon-option {
  width: 36px;
  height: 36px;
  border: 2px solid #e5e7eb;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: border-color 0.2s;
}
.icon-option.active { border-color: #1d9e75; background: #f0fdf4; }
.icon-option:hover { border-color: #1d9e75; }
.selected-products { display: flex; flex-wrap: wrap; }
</style>
