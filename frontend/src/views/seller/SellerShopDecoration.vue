<template>
  <section class="decoration-studio">
    <div class="studio-hero">
      <div>
        <span class="hero-kicker">Shop Decoration</span>
        <h1>店铺装修</h1>
        <p>从模板、主图、公告和店铺亮点生成店铺首页，保存后买家进店即可看到。</p>
      </div>
      <div class="hero-actions">
        <el-button :disabled="!shopInfo.id" @click="openPublicShop">
          <el-icon><Shop /></el-icon>
          查看店铺
        </el-button>
        <el-button @click="resetDraft">
          <el-icon><Refresh /></el-icon>
          恢复默认
        </el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          <el-icon><Check /></el-icon>
          保存发布
        </el-button>
      </div>
    </div>

    <div class="studio-grid">
      <aside class="setup-panel">
        <section class="setup-section">
          <div class="section-title">
            <span>01</span>
            <strong>选择模板</strong>
          </div>
          <div class="template-grid">
            <button
              v-for="item in templatePresets"
              :key="item.key"
              class="template-option"
              :class="{ active: form.templateKey === item.key }"
              type="button"
              @click="applyTemplate(item.key, true)"
            >
              <span class="template-swatch" :style="{ background: item.gradient }"></span>
              <strong>{{ item.name }}</strong>
              <small>{{ item.desc }}</small>
            </button>
          </div>
        </section>

        <section class="setup-section">
          <div class="section-title">
            <span>02</span>
            <strong>店铺文案</strong>
          </div>
          <el-form class="quick-form" label-position="top">
            <el-form-item label="主标题">
              <el-input v-model="form.heroTitle" maxlength="24" show-word-limit />
            </el-form-item>
            <el-form-item label="副标题">
              <el-input v-model="form.heroSubtitle" maxlength="64" show-word-limit />
            </el-form-item>
            <el-form-item label="公告">
              <el-input v-model="form.announcement" type="textarea" :rows="3" maxlength="80" show-word-limit />
            </el-form-item>
          </el-form>
        </section>

        <section class="setup-section">
          <div class="section-title">
            <span>03</span>
            <strong>展示模块</strong>
          </div>
          <div class="switch-row">
            <span>显示公告</span>
            <el-switch v-model="form.showAnnouncement" />
          </div>
          <div class="switch-row">
            <span>显示店铺亮点</span>
            <el-switch v-model="form.showFeatures" />
          </div>

          <div v-if="form.showFeatures" class="feature-editor">
            <div v-for="(item, index) in form.features" :key="index" class="feature-input">
              <el-input v-model="item.title" maxlength="12" placeholder="亮点标题" />
              <el-input v-model="item.desc" maxlength="22" placeholder="一句说明" />
            </div>
          </div>
        </section>
      </aside>

      <main class="preview-panel">
        <div class="preview-head">
          <div>
            <span>Live Preview</span>
            <h2>发布效果预览</h2>
          </div>
          <div class="preview-count">{{ previewComponents.length }} 个展示模块</div>
        </div>

        <div class="preview-shell" :style="{ background: form.bgColor }">
          <div class="preview-shop-head" :style="{ background: form.headerGradient }">
            <el-avatar :src="shopLogo" :size="54" class="preview-logo">
              {{ shopInitial }}
            </el-avatar>
            <div>
              <strong>{{ shopInfo.name || "我的店铺" }}</strong>
              <small>{{ shopInfo.description || "暂无店铺简介，可在店铺资料中补充主营品类。" }}</small>
            </div>
          </div>

          <div
            v-for="component in previewComponents"
            :key="component.id"
            class="preview-component"
            :style="{ marginBottom: `${decorationSettings.gap}px` }"
          >
            <ComponentRenderer :component="component" :theme-color="form.themeColor" />
          </div>
        </div>
      </main>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { Check, Refresh, Shop } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { getCurrentSellerShopApi, saveShopDecorationApi } from "@/api/shop";
import { toAssetUrl } from "@/utils/url";
import ComponentRenderer from "./decoration/ComponentRenderer.vue";

const router = useRouter();
const saving = ref(false);

const templatePresets = [
  {
    key: "fresh",
    name: "清新日用",
    desc: "适合日用品、文创和宿舍用品",
    gradient: "linear-gradient(135deg, #20d6a0 0%, #287cff 100%)",
    themeColor: "#20d6a0",
    bgColor: "#f7fbff",
    headerGradient: "linear-gradient(135deg, #20d6a0 0%, #287cff 100%)",
    bannerImage: "https://images.unsplash.com/photo-1472851294608-062f824d29cc?auto=format&fit=crop&w=1400&q=80",
  },
  {
    key: "tech",
    name: "数码蓝调",
    desc: "适合 3C、配件和学习设备",
    gradient: "linear-gradient(135deg, #5f8dff 0%, #22d2c7 100%)",
    themeColor: "#3c92ff",
    bgColor: "#f3f8ff",
    headerGradient: "linear-gradient(135deg, #5f8dff 0%, #22d2c7 100%)",
    bannerImage: "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1400&q=80",
  },
  {
    key: "warm",
    name: "温暖市集",
    desc: "适合服饰、美妆和配饰店铺",
    gradient: "linear-gradient(135deg, #ff8eb8 0%, #ffbd82 100%)",
    themeColor: "#ff8eb8",
    bgColor: "#fff7fb",
    headerGradient: "linear-gradient(135deg, #ff8eb8 0%, #ffbd82 100%)",
    bannerImage: "https://images.unsplash.com/photo-1556742044-3c52d6e88c62?auto=format&fit=crop&w=1400&q=80",
  },
];

const defaultFeatures = [
  { title: "主营清晰", desc: "展示店铺主要品类" },
  { title: "响应及时", desc: "订单与消息按时处理" },
  { title: "规则明确", desc: "售后说明按平台规则执行" },
];

const legacyCopyReplacements = {
  "Kinda 官方好物店": "Kinda 校园数码店",
  "主营校园学习、数码和生活好物。": "销售学习设备、数码配件和宿舍生活用品。",
  "数码装备限时上新": "本店商品与服务说明",
  "精选键盘、鼠标和学习效率装备，今天下单尽快发货。": "查看主营商品、发货安排和售后规则。",
  "新店上新中，欢迎收藏店铺并浏览更多商品。": "下单前可先查看商品详情、库存和店铺说明。",
  "精选好物": "主营清晰",
  "围绕学习与宿舍场景精选": "展示店铺主要品类",
  "及时处理": "响应及时",
  "订单与消息会尽快响应": "订单与消息按时处理",
  "稳定售后": "规则明确",
  "购物问题按平台规则处理": "售后说明按平台规则执行",
};

const form = reactive({
  templateKey: "fresh",
  themeColor: "#20d6a0",
  bgColor: "#f7fbff",
  headerGradient: "linear-gradient(135deg, #20d6a0 0%, #287cff 100%)",
  bannerImage: templatePresets[0].bannerImage,
  heroTitle: "本店商品与服务说明",
  heroSubtitle: "查看主营商品、发货安排和售后规则。",
  announcement: "下单前可先查看商品详情、库存和店铺说明。",
  showAnnouncement: true,
  showFeatures: true,
  features: JSON.parse(JSON.stringify(defaultFeatures)),
});

const shopInfo = reactive({
  id: null,
  name: "",
  description: "",
  logo: "",
});

const decorationSettings = computed(() => ({
  themeColor: form.themeColor,
  bgColor: form.bgColor,
  gap: 12,
}));

const shopLogo = computed(() => toAssetUrl(shopInfo.logo));
const shopInitial = computed(() => (shopInfo.name || "店").slice(0, 1));

const previewComponents = computed(() => buildComponents());
const decorationPayload = computed(() => ({
  meta: {
    editorMode: "simple-v1",
    updatedAt: new Date().toISOString(),
  },
  editorState: JSON.parse(JSON.stringify(form)),
  globalSettings: decorationSettings.value,
  components: previewComponents.value,
}));

function normalizeLegacyCopy(value) {
  return typeof value === "string" ? legacyCopyReplacements[value] || value : value;
}

function normalizeLegacyCopyDeep(value) {
  if (typeof value === "string") {
    return normalizeLegacyCopy(value);
  }
  if (Array.isArray(value)) {
    return value.map((item) => normalizeLegacyCopyDeep(item));
  }
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, normalizeLegacyCopyDeep(item)]));
  }
  return value;
}

function applyTemplate(key, keepText = false) {
  const preset = templatePresets.find((item) => item.key === key) || templatePresets[0];
  form.templateKey = preset.key;
  form.themeColor = preset.themeColor;
  form.bgColor = preset.bgColor;
  form.headerGradient = preset.headerGradient;
  form.bannerImage = preset.bannerImage;
  if (!keepText) {
    form.heroTitle = shopInfo.name ? `${shopInfo.name}店铺首页` : "本店商品与服务说明";
    form.heroSubtitle = shopInfo.description || "查看主营商品、发货安排和售后规则。";
    form.announcement = "下单前可先查看商品详情、库存和店铺说明。";
    form.showAnnouncement = true;
    form.showFeatures = true;
    form.features = JSON.parse(JSON.stringify(defaultFeatures));
  }
}

function buildComponents() {
  const components = [
    {
      id: "quick-banner",
      type: "quick_banner",
      props: {
        badge: "Kinda Goods",
        title: form.heroTitle,
        subtitle: form.heroSubtitle,
        imageUrl: form.bannerImage,
        gradient: form.headerGradient,
      },
    },
  ];

  if (form.showAnnouncement && form.announcement.trim()) {
    components.push({
      id: "quick-announcement",
      type: "announcement",
      props: {
        icon: "公告",
        text: form.announcement.trim(),
        bgColor: "#ffffff",
        textColor: "#123241",
      },
    });
  }

  const featureItems = form.features
    .map((item) => ({
      title: item.title.trim(),
      desc: item.desc.trim(),
    }))
    .filter((item) => item.title || item.desc);

  if (form.showFeatures && featureItems.length) {
    components.push({
      id: "quick-features",
      type: "feature_cards",
      props: {
        title: "店铺亮点",
        items: featureItems,
      },
    });
  }

  return components;
}

function resetDraft() {
  applyTemplate("fresh", false);
  ElMessage.success("已恢复默认装修草稿");
}

function openPublicShop() {
  if (!shopInfo.id) {
    ElMessage.warning("还没有读取到店铺信息");
    return;
  }
  router.push({ name: "publicShop", params: { shopId: shopInfo.id } });
}

async function handleSave() {
  saving.value = true;
  try {
    await saveShopDecorationApi(decorationPayload.value);
    ElMessage.success("店铺装修已保存发布");
  } finally {
    saving.value = false;
  }
}

async function loadData() {
  try {
    const result = await getCurrentSellerShopApi();
    const shop = result.data || {};
    shopInfo.id = shop.id || null;
    shopInfo.name = normalizeLegacyCopy(shop.name || "");
    shopInfo.description = normalizeLegacyCopy(shop.description || "");
    shopInfo.logo = shop.logo || "";

    const parsed = parseDecoration(shop.decorationJson);
    if (parsed?.editorState) {
      Object.assign(form, normalizeLegacyCopyDeep(parsed.editorState));
      if (!Array.isArray(form.features) || !form.features.length) {
        form.features = JSON.parse(JSON.stringify(defaultFeatures));
      }
      return;
    }

    applyTemplate("fresh", false);
  } catch {
    ElMessage.error("加载店铺装修信息失败");
  }
}

function parseDecoration(value) {
  if (!value) {
    return null;
  }
  try {
    return typeof value === "string" ? JSON.parse(value) : value;
  } catch {
    return null;
  }
}

onMounted(loadData);
</script>

<style scoped>
.decoration-studio {
  display: grid;
  gap: 16px;
}

.studio-hero {
  position: relative;
  overflow: hidden;
  min-height: 150px;
  border: 1px solid rgba(137, 199, 255, 0.36);
  border-radius: 8px;
  padding: 22px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  background:
    linear-gradient(115deg, rgba(234, 244, 255, 0.94), rgba(233, 255, 248, 0.82), rgba(255, 247, 251, 0.72)),
    url("https://images.unsplash.com/photo-1556742044-3c52d6e88c62?auto=format&fit=crop&w=1400&q=80");
  background-size: cover;
  background-position: center;
  box-shadow: var(--shadow-soft);
}

.studio-hero::before {
  position: absolute;
  inset: 0;
  content: "";
  background-image:
    linear-gradient(rgba(60, 146, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(53, 216, 171, 0.08) 1px, transparent 1px);
  background-size: 42px 42px;
  pointer-events: none;
}

.studio-hero > * {
  position: relative;
  z-index: 1;
}

.hero-kicker,
.preview-head span {
  color: var(--brand-primary);
  font-size: 13px;
  font-weight: 900;
}

.studio-hero h1 {
  margin: 10px 0 8px;
  color: var(--text-main);
  font-size: clamp(30px, 4vw, 44px);
  line-height: 1.08;
  letter-spacing: 0;
}

.studio-hero p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 16px;
  line-height: 1.7;
  font-weight: 800;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.studio-grid {
  display: grid;
  grid-template-columns: minmax(300px, 380px) minmax(0, 1fr);
  gap: 16px;
}

.setup-panel,
.preview-panel {
  border: 1px solid rgba(137, 199, 255, 0.28);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 251, 255, 0.94)),
    #ffffff;
  box-shadow: var(--shadow-soft);
}

.setup-panel {
  padding: 18px;
  display: grid;
  gap: 18px;
  align-content: start;
}

.setup-section {
  display: grid;
  gap: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title span {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--brand-gradient-strong);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 900;
}

.section-title strong {
  color: var(--text-main);
  font-size: 16px;
}

.template-grid {
  display: grid;
  gap: 10px;
}

.template-option {
  width: 100%;
  min-height: 74px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  padding: 10px;
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  grid-template-rows: auto auto;
  gap: 4px 10px;
  text-align: left;
  cursor: pointer;
}

.template-option.active,
.template-option:hover {
  border-color: var(--brand-primary);
  background: var(--brand-primary-weak);
}

.template-swatch {
  grid-row: 1 / 3;
  width: 54px;
  height: 54px;
  border-radius: 8px;
  box-shadow: 0 10px 22px rgba(40, 124, 255, 0.14);
}

.template-option strong,
.template-option small {
  min-width: 0;
  overflow-wrap: anywhere;
}

.template-option strong {
  color: var(--text-main);
  font-weight: 900;
}

.template-option small {
  color: var(--text-secondary);
  font-weight: 700;
  line-height: 1.35;
}

.quick-form {
  display: grid;
  gap: 2px;
}

.quick-form :deep(.el-form-item) {
  margin-bottom: 10px;
}

.quick-form :deep(.el-form-item__label) {
  color: var(--text-main);
  font-weight: 900;
}

.quick-form :deep(.el-input__wrapper),
.quick-form :deep(.el-textarea__inner) {
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px var(--line-soft) inset;
}

.switch-row {
  min-height: 40px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--text-main);
  font-weight: 800;
}

.feature-editor {
  display: grid;
  gap: 8px;
}

.feature-input {
  display: grid;
  grid-template-columns: 0.8fr 1.2fr;
  gap: 8px;
}

.preview-panel {
  overflow: hidden;
  padding: 18px;
}

.preview-head {
  margin-bottom: 14px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.preview-head h2 {
  margin: 4px 0 0;
  color: var(--text-main);
  font-size: 24px;
}

.preview-count {
  min-height: 32px;
  border: 1px solid rgba(137, 199, 255, 0.34);
  border-radius: 8px;
  background: #ffffff;
  color: var(--brand-primary);
  display: inline-flex;
  align-items: center;
  padding: 0 10px;
  font-weight: 900;
}

.preview-shell {
  min-height: 560px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  overflow: hidden;
  padding: 12px;
}

.preview-shop-head {
  min-height: 92px;
  border-radius: 8px;
  color: #ffffff;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.preview-logo {
  border: 2px solid rgba(255, 255, 255, 0.84);
  flex: 0 0 auto;
}

.preview-shop-head strong,
.preview-shop-head small {
  display: block;
}

.preview-shop-head strong {
  font-size: 18px;
  font-weight: 900;
}

.preview-shop-head small {
  margin-top: 5px;
  opacity: 0.86;
  font-weight: 700;
  line-height: 1.45;
}

.preview-component:last-child {
  margin-bottom: 0 !important;
}

@media (max-width: 1100px) {
  .studio-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .studio-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .feature-input {
    grid-template-columns: 1fr;
  }

  .preview-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
