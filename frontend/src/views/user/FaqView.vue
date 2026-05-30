<template>
  <section class="faq-page">
    <div class="faq-hero">
      <span>Help Center</span>
      <h1>常见问题</h1>
      <p>在这里你可以找到关于购物、二手交易、售后和信用等方面的常见问题解答。</p>
      <p>如果你有其他疑问，欢迎通过邮箱 24373876@buaa.edu.cn 联系我们的客服团队，我们会尽快为你提供帮助！</p>
    </div>

    <div class="faq-grid">
      <article v-for="group in faqGroups" :key="group.title" class="faq-card">
        <h2>{{ group.title }}</h2>
        <el-collapse>
          <el-collapse-item v-for="item in group.items" :key="item.q" :title="item.q">
            <p>{{ item.a }}</p>
          </el-collapse-item>
        </el-collapse>
      </article>
    </div>

    <div class="support-strip">
      <button type="button" @click="router.push('/messages')">联系平台消息</button>
      <button type="button" @click="router.push('/product/after-sale')">新品售后</button>
      <button type="button" @click="router.push('/secondhand/after-sale')">二手售后</button>
      <button type="button" @click="router.push('/credit')">查看信用</button>
    </div>
  </section>
</template>

<script setup>
import { useRouter } from "vue-router";

const router = useRouter();

const faqGroups = [
  {
    title: "购物下单",
    items: [
      { q: "怎么搜索商品？", a: "顶部搜索栏可以在新品商城和二手商城之间切换，输入关键词后会去对应商城搜索。" },
      { q: "为什么要先确认地址？", a: "下单前会优先使用默认地址，避免订单地址缺失或发错。" },
      { q: "加购后在哪里结算？", a: "新品和二手都在统一购物车中结算，页面会按官方商城订单和个人闲置订单自动分组。" },
    ],
  },
  {
    title: "二手交易",
    items: [
      { q: "二手商品可以议价吗？", a: "可以。进入二手详情页点击“我要议价”，系统会打开与卖家的聊天窗口并预填议价消息。" },
      { q: "怎么发布闲置？", a: "进入二手商城后点击发布闲置，填写标题、分类、价格、成色和图片即可发布。" },
      { q: "遇到异常卖家怎么办？", a: "可以在二手详情页举报或拉黑，也可以到信用中心查看举报记录。" },
    ],
  },
  {
    title: "售后与信用",
    items: [
      { q: "订单出问题在哪里处理？", a: "进入我的订单查看新品和二手交易，也可以按订单类型筛选后处理售后。" },
      { q: "信用分有什么用？", a: "信用分用于展示买家和二手卖家的交易表现，便于交易双方判断可信度。" },
      { q: "优惠券在哪里领？", a: "新品商城和二手商城都有自己的领券中心，进入对应商城领取对应优惠券。" },
    ],
  },
];
</script>

<style scoped>
.faq-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.faq-hero {
  min-height: 150px;
  border: 1px solid rgba(137, 199, 255, 0.36);
  border-radius: 8px;
  padding: 22px;
  background: linear-gradient(135deg, #eaf4ff 0%, #e9fff8 52%, #fff7fb 100%);
  box-shadow: var(--shadow-soft);
}

.faq-hero span {
  color: var(--brand-primary);
  font-weight: 900;
}

.faq-hero h1 {
  margin: 10px 0 8px;
  font-size: 34px;
}

.faq-hero p {
  max-width: 720px;
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
  font-weight: 700;
}

.faq-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.faq-card {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  padding: 16px;
  box-shadow: var(--shadow-soft);
}

.faq-card h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.faq-card p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.support-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.support-strip button {
  min-height: 46px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  color: var(--text-main);
  font-weight: 900;
  cursor: pointer;
}

.support-strip button:hover {
  border-color: var(--brand-primary);
  color: var(--brand-primary);
  background: var(--brand-primary-weak);
}

@media (max-width: 980px) {
  .faq-grid,
  .support-strip {
    grid-template-columns: 1fr;
  }
}
</style>
