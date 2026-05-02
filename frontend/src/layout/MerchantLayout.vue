<template>
  <div class="merchant-shell">
    <header class="merchant-topbar">
      <div class="topbar-inner">
        <button class="merchant-brand" type="button" @click="router.push('/merchant')">
          <span class="brand-mark">kg</span>
          <span>
            <strong>卖家工作台</strong>
            <em>kinda goods 商家中心</em>
          </span>
        </button>

        <nav class="top-actions" aria-label="卖家快捷入口">
          <button type="button" @click="router.push('/merchant/seller-products/edit')">发布商品</button>
          <button type="button" @click="router.push('/merchant/messages')">买家消息</button>
          <button type="button" @click="router.push('/')">返回商城</button>
        </nav>
      </div>

      <div class="top-nav">
        <button
          v-for="item in flatNav"
          :key="item.path"
          class="top-nav-item"
          :class="{ active: isActive(item.path) }"
          type="button"
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </button>
      </div>
    </header>

    <div class="merchant-workspace">
      <main class="merchant-main fade-in-up">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();

const navGroups = [
  {
    key: "order",
    label: "订单",
    children: [
      { label: "订单管理", path: "/merchant/orders", icon: "≡" },
    ],
  },
  {
    key: "product",
    label: "商品",
    children: [
      { label: "我的商品", path: "/merchant", icon: "□" },
      { label: "发布商品", path: "/merchant/seller-products/edit", icon: "+" },
    ],
  },
  {
    key: "marketing",
    label: "营销",
    children: [
      { label: "优惠券", path: "/merchant/vouchers", icon: "%" },
    ],
  },
  {
    key: "finance",
    label: "财务",
    children: [
      { label: "财务看板", path: "/merchant/finance", icon: "￥" },
    ],
  },
  {
    key: "data",
    label: "数据",
    children: [
      { label: "数据分析", path: "/merchant/seller-dashboard", icon: "↗" },
      { label: "账户健康", path: "/merchant/account-health", icon: "✓" },
    ],
  },
  {
    key: "service",
    label: "客服",
    children: [
      { label: "评价管理", path: "/merchant/reviews", icon: "☆" },
      { label: "买家消息", path: "/merchant/messages", icon: "◌" },
      { label: "通知", path: "/merchant/notifications", icon: "!" },
    ],
  },
  {
    key: "shop",
    label: "店铺",
    children: [
      { label: "店铺资料", path: "/merchant/seller-shop", icon: "⌂" },
      { label: "店铺装修", path: "/merchant/shop-decoration", icon: "◇" },
    ],
  },
];

const flatNav = computed(() => navGroups.flatMap((group) => group.children));

function isActive(path) {
  if (path === "/merchant") return route.path === "/merchant";
  return route.path.startsWith(path);
}
</script>

<style scoped>
.merchant-shell {
  min-height: 100vh;
  background: #f2f4f6;
}

.merchant-topbar {
  position: sticky;
  top: 0;
  z-index: 40;
  background: #ffe100;
  box-shadow: 0 2px 12px rgba(31, 35, 43, 0.08);
}

.topbar-inner {
  width: min(1440px, calc(100% - 44px));
  height: 76px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.merchant-brand,
.top-actions button,
.top-nav-item,
.nav-link {
  font: inherit;
}

.merchant-brand {
  min-width: 0;
  border: 0;
  padding: 0;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  background: transparent;
  color: #20242d;
  cursor: pointer;
  text-align: left;
}

.brand-mark {
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border: 3px solid #20242d;
  border-radius: 14px;
  font-size: 18px;
  letter-spacing: 0;
  text-transform: lowercase;
  line-height: 1;
  font-weight: 900;
}

.merchant-brand strong,
.merchant-brand em {
  display: block;
}

.merchant-brand strong {
  font-size: 24px;
  line-height: 1.1;
  font-weight: 900;
}

.merchant-brand em {
  margin-top: 4px;
  color: #4f5662;
  font-size: 13px;
  font-style: normal;
  font-weight: 700;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.top-actions button {
  height: 36px;
  border: 0;
  border-radius: 999px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.62);
  color: #20242d;
  cursor: pointer;
  font-weight: 800;
}

.top-actions button:hover {
  background: #fff;
}

.top-nav {
  width: min(1440px, calc(100% - 44px));
  margin: 0 auto;
  padding: 0 0 12px;
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.top-nav-item {
  height: 34px;
  border: 0;
  border-radius: 999px;
  padding: 0 14px;
  background: transparent;
  color: #2b2f38;
  cursor: pointer;
  font-weight: 700;
  white-space: nowrap;
}

.top-nav-item:hover,
.top-nav-item.active {
  background: #fff;
}

.merchant-workspace {
  width: min(1440px, calc(100% - 44px));
  margin: 18px auto 32px;
}

.merchant-main {
  min-width: 0;
}

.merchant-main :deep(.page-card),
.merchant-main :deep(.el-card) {
  border: 1px solid #eeeeee;
  border-radius: 20px;
  box-shadow: 0 8px 20px rgba(30, 34, 40, 0.04);
}

.merchant-main :deep(.page-header) {
  min-height: 56px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.merchant-main :deep(.page-title) {
  margin: 0;
  color: #20242d;
  font-size: 24px;
  font-weight: 900;
  letter-spacing: 0;
}

.merchant-main :deep(.el-button--primary) {
  --el-button-bg-color: #20242d;
  --el-button-border-color: #20242d;
  --el-button-hover-bg-color: #333842;
  --el-button-hover-border-color: #333842;
}

.merchant-main :deep(.el-button--warning) {
  --el-button-bg-color: #ffe100;
  --el-button-border-color: #ffe100;
  --el-button-text-color: #20242d;
  --el-button-hover-bg-color: #ffe83f;
  --el-button-hover-border-color: #ffe83f;
  --el-button-hover-text-color: #20242d;
}

.merchant-main :deep(.el-table) {
  border-radius: 14px;
  overflow: hidden;
}

.merchant-main :deep(.el-table th.el-table__cell) {
  background: #fafafa;
  color: #333842;
  font-weight: 800;
}

.merchant-main :deep(.el-input__wrapper),
.merchant-main :deep(.el-select__wrapper) {
  border-radius: 12px;
}

@media (max-width: 1180px) {
}

@media (max-width: 760px) {
  .topbar-inner {
    width: calc(100% - 20px);
    height: auto;
    padding: 10px 0;
    align-items: flex-start;
    flex-direction: column;
  }

  .merchant-brand strong {
    font-size: 21px;
  }

  .top-actions {
    width: 100%;
    overflow-x: auto;
  }

  .top-nav,
  .merchant-workspace {
    width: calc(100% - 16px);
  }

  .merchant-workspace {
    margin-top: 10px;
  }

}
</style>
