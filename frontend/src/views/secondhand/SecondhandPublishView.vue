<template>
  <section class="publish-page">
    <div class="publish-hero">
      <div>
        <span class="eyebrow">Secondhand Studio</span>
        <h1>发布二手商品</h1>
        <p>把闲置整理成清爽的商品卡片，更容易被买家看见。</p>
      </div>
      <div class="hero-summary">
        <span>预估展示价</span>
        <strong>￥{{ Number(form.salePrice || 0).toFixed(2) }}</strong>
        <small>{{ discountText }}</small>
      </div>
    </div>

    <div class="publish-grid">
      <section class="form-shell">
        <div class="panel-head">
          <div>
            <h2>商品信息</h2>
            <p>标题、图片、价格和成色会同步到右侧预览。</p>
          </div>
          <span>{{ form.condition }}</span>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="publish-form"
        >
          <div class="form-grid">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" maxlength="120" show-word-limit placeholder="例如：九成新办公椅" />
            </el-form-item>
            <el-form-item label="闲置分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
                <el-option
                  v-for="item in secondhandCategoryOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </div>

          <div class="form-grid">
            <el-form-item label="成色" prop="condition">
              <el-segmented v-model="form.condition" :options="conditionOptions" />
            </el-form-item>
          </div>

          <div class="form-grid">
            <el-form-item label="封面链接">
              <el-input v-model="form.cover" placeholder="请输入图片 URL" />
            </el-form-item>
          </div>

          <div class="form-grid price-grid">
            <el-form-item label="原价">
              <el-input-number v-model="form.originPrice" :min="1" :precision="2" :step="10" />
            </el-form-item>
            <el-form-item label="售价" prop="salePrice">
              <el-input-number v-model="form.salePrice" :min="1" :precision="2" :step="10" />
            </el-form-item>
          </div>

          <el-form-item label="商品描述">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="5"
              maxlength="500"
              show-word-limit
              placeholder="写清楚使用情况、配件、瑕疵和交易方式"
            />
          </el-form-item>

          <div class="action-bar">
            <el-button type="primary" size="large" :loading="submitting" @click="submit">发布二手</el-button>
            <el-button size="large" @click="reset">重置</el-button>
            <el-button text @click="router.push('/secondhand')">返回列表</el-button>
          </div>
        </el-form>
      </section>

      <aside class="preview-shell">
        <article class="preview-card">
          <div class="preview-cover">
            <img v-if="previewCover" :src="previewCover" :alt="previewName" />
            <div v-else class="cover-placeholder">
              <strong>KG</strong>
              <span>封面预览</span>
            </div>
            <span class="condition-badge">{{ form.condition }}</span>
          </div>
          <div class="preview-body">
            <div class="preview-tags">
              <span>个人闲置</span>
              <span>{{ form.category }}</span>
              <span>可议价</span>
            </div>
            <h3>{{ previewName }}</h3>
            <p>{{ previewDesc }}</p>
            <div class="preview-price">
              <strong>￥{{ Number(form.salePrice || 0).toFixed(2) }}</strong>
              <span>原价 ￥{{ Number(form.originPrice || 0).toFixed(2) }}</span>
            </div>
          </div>
        </article>

        <div class="publish-meter">
          <div>
            <span>价格差</span>
            <strong>￥{{ priceGap }}</strong>
          </div>
          <div>
            <span>展示完整度</span>
            <strong>{{ completionScore }}%</strong>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { publishSecondhandApi } from '@/api/secondhand';
import { ALL_CATEGORY, secondhandCategories } from '@/utils/categoryRules';

const router = useRouter();
const formRef = ref();
const form = reactive({
  name: '',
  cover: '',
  originPrice: 100,
  salePrice: 80,
  category: '宿舍生活',
  condition: '90%',
  description: ''
});

const submitting = ref(false);
const conditionOptions = ['95%', '90%', '80%'];
const secondhandCategoryOptions = secondhandCategories.filter((item) => item !== ALL_CATEGORY);

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择闲置分类', trigger: 'change' }],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'change' }],
};

const previewCover = computed(() => form.cover.trim());
const previewName = computed(() => form.name.trim() || '你的闲置商品');
const previewDesc = computed(() => form.description.trim() || '补充使用情况、配件和交易说明后，商品卡片会更完整。');
const priceGap = computed(() => Math.max(0, Number(form.originPrice || 0) - Number(form.salePrice || 0)).toFixed(2));
const discountText = computed(() => {
  const origin = Number(form.originPrice || 0);
  const sale = Number(form.salePrice || 0);
  if (!origin || !sale) {
    return '等待定价';
  }
  return `${Math.max(1, Math.round((sale / origin) * 100))}% 原价`;
});

const completionScore = computed(() => {
  const checks = [
    form.name.trim(),
    form.cover.trim(),
    Number(form.salePrice || 0) > 0,
    form.category,
    form.condition,
    form.description.trim(),
  ];
  return Math.round((checks.filter(Boolean).length / checks.length) * 100);
});

async function submit() {
  await formRef.value.validate();
  submitting.value = true;
  try {
    await publishSecondhandApi({
      name: form.name,
      cover: form.cover,
      description: form.description,
      originPrice: form.originPrice,
      salePrice: form.salePrice,
      categoryName: form.category,
      category: form.category,
      conditionLevel: form.condition,
      isNegotiable: 1,
    });
    ElMessage.success('二手商品发布成功');
    reset();
    router.push('/secondhand');
  } catch {
    // API layer already shows the backend message; keep the form state for retry.
  } finally {
    submitting.value = false;
  }
}

function reset() {
  form.name = '';
  form.cover = '';
  form.originPrice = 100;
  form.salePrice = 80;
  form.category = '宿舍生活';
  form.condition = '90%';
  form.description = '';
}
</script>

<style scoped>
.publish-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.publish-hero {
  min-height: 188px;
  border: 1px solid rgba(53, 216, 171, 0.24);
  border-radius: 12px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  color: var(--text-main);
  gap: 18px;
  background:
    linear-gradient(120deg, rgba(233, 255, 248, 0.94), rgba(234, 244, 255, 0.86), rgba(255, 247, 251, 0.72)),
    url("https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1400&q=80");
  background-size: cover;
  background-position: center;
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.eyebrow {
  display: inline-flex;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-main);
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 900;
}

.publish-hero h1 {
  margin: 12px 0 8px;
  font-size: clamp(32px, 5vw, 48px);
  line-height: 1;
}

.publish-hero p {
  margin: 0;
  max-width: 560px;
  color: var(--text-secondary);
  font-weight: 800;
  line-height: 1.7;
}

.hero-summary {
  min-width: 190px;
  border: 1px solid rgba(137, 199, 255, 0.45);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  padding: 14px;
  text-align: right;
  backdrop-filter: blur(10px);
}

.hero-summary span,
.hero-summary strong,
.hero-summary small {
  display: block;
}

.hero-summary span,
.hero-summary small {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.hero-summary strong {
  margin: 5px 0;
  color: var(--brand-primary);
  font-size: 28px;
}

.publish-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 14px;
  align-items: start;
}

.form-shell {
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  padding: 18px;
  box-shadow: var(--shadow-soft);
}

.panel-head {
  margin-bottom: 18px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.panel-head h2 {
  margin: 0;
  font-size: 22px;
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--text-secondary);
}

.panel-head span {
  border-radius: 999px;
  background: var(--brand-mint-weak);
  color: var(--brand-accent-strong);
  padding: 6px 10px;
  font-weight: 900;
}

.publish-form :deep(.el-segmented) {
  --el-segmented-item-selected-color: #073949;
  --el-segmented-item-selected-bg-color: #a4f4e0;
  width: 100%;
}

.publish-form :deep(.el-input-number) {
  width: 100%;
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 14px;
}

.price-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.action-bar {
  border-top: 1px solid var(--line-soft);
  padding-top: 16px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.preview-shell {
  position: sticky;
  top: 166px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preview-card,
.publish-meter {
  border: 1px solid rgba(60, 146, 255, 0.2);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
}

.preview-cover {
  position: relative;
  aspect-ratio: 1 / 1;
  background: linear-gradient(135deg, #e9fff8, #eaf4ff);
}

.preview-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: var(--brand-primary);
}

.cover-placeholder strong {
  width: 68px;
  height: 68px;
  border-radius: 18px;
  background: var(--brand-gradient);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 900;
}

.cover-placeholder span {
  color: var(--text-secondary);
  font-weight: 900;
}

.condition-badge {
  position: absolute;
  left: 12px;
  top: 12px;
  border-radius: 999px;
  background: rgba(53, 216, 171, 0.92);
  color: #073949;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 900;
}

.preview-body {
  padding: 14px;
}

.preview-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.preview-tags span {
  border: 1px solid rgba(53, 216, 171, 0.32);
  border-radius: 999px;
  background: var(--brand-mint-weak);
  color: var(--brand-accent-strong);
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 800;
}

.preview-body h3 {
  margin: 12px 0 8px;
  font-size: 18px;
  line-height: 1.35;
}

.preview-body p {
  min-height: 44px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.preview-price {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 10px;
}

.preview-price strong {
  color: var(--brand-primary);
  font-size: 24px;
}

.preview-price span {
  color: var(--text-muted);
  font-size: 12px;
  text-decoration: line-through;
}

.publish-meter {
  padding: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.publish-meter div {
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(233, 255, 248, 0.9), rgba(234, 244, 255, 0.9));
  padding: 12px;
}

.publish-meter span,
.publish-meter strong {
  display: block;
}

.publish-meter span {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.publish-meter strong {
  margin-top: 6px;
  color: var(--text-main);
  font-size: 20px;
}

@media (max-width: 760px) {
  .publish-hero,
  .panel-head {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-summary {
    text-align: left;
  }

  .publish-grid,
  .form-grid,
  .price-grid,
  .publish-meter {
    grid-template-columns: 1fr;
  }

  .preview-shell {
    position: static;
  }
}
</style>
