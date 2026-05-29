<template>
  <div class="merchant-layout">
    <aside class="merchant-sidebar">
      <button class="brand-block" type="button" @click="router.push('/merchant')">
        <img :src="logoUrl" alt="Kinda Goods" />
        <span>卖家工作台</span>
      </button>

      <nav class="side-groups">
        <section v-for="group in navGroups" :key="group.title" class="side-group">
          <p>{{ group.title }}</p>
          <button
            v-for="item in group.items"
            :key="item.path"
            type="button"
            class="side-nav-item"
            :class="{ active: isActive(item.path) }"
            @click="router.push(item.path)"
          >
            <span class="nav-mark" :style="{ background: item.color }"></span>
            {{ item.label }}
          </button>
        </section>
      </nav>
    </aside>

    <section class="merchant-shell">
      <header class="merchant-topbar">
        <div>
          <span class="top-kicker">Seller Center</span>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="top-actions">
          <button type="button" @click="router.push('/merchant/seller-products/edit')">发布商品</button>
          <button type="button" @click="router.push('/')">返回商城</button>
        </div>
      </header>

      <main class="merchant-main fade-in-up">
        <router-view />
      </main>
    </section>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import logoUrl from "@/assets/kinda-goods-logo.svg";

const route = useRoute();
const router = useRouter();

const navGroups = [
  {
    title: "经营",
    items: [
      { label: "商品管理", path: "/merchant", color: "#35d8ab" },
      { label: "发布商品", path: "/merchant/seller-products/edit", color: "#69b9ff" },
      { label: "订单管理", path: "/merchant/orders", color: "#ffc6dc" },
      { label: "优惠券", path: "/merchant/vouchers", color: "#ffd36e" },
    ],
  },
  {
    title: "数据",
    items: [
      { label: "财务看板", path: "/merchant/finance", color: "#7ee8cb" },
      { label: "数据分析", path: "/merchant/seller-dashboard", color: "#b7a6ff" },
      { label: "账户健康", path: "/merchant/account-health", color: "#9bd8ff" },
    ],
  },
  {
    title: "服务",
    items: [
      { label: "评价管理", path: "/merchant/reviews", color: "#ffb9d6" },
      { label: "买家消息", path: "/merchant/messages", color: "#35d8ab" },
      { label: "通知", path: "/merchant/notifications", color: "#69b9ff" },
    ],
  },
  {
    title: "店铺",
    items: [
      { label: "店铺资料", path: "/merchant/seller-shop", color: "#ffd36e" },
      { label: "店铺装修", path: "/merchant/shop-decoration", color: "#b7a6ff" },
    ],
  },
];

const allItems = computed(() => navGroups.flatMap((group) => group.items));
const currentTitle = computed(() => allItems.value.find((item) => isActive(item.path))?.label || "卖家工作台");

function isActive(path) {
  if (path === "/merchant") {
    return route.path === "/merchant";
  }
  return route.path === path || route.path.startsWith(`${path}/`);
}
</script>

<style scoped>
.merchant-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  background:
    radial-gradient(circle at 12% 6%, rgba(126, 232, 203, 0.42), transparent 28%),
    radial-gradient(circle at 92% 0%, rgba(255, 198, 220, 0.4), transparent 28%),
    linear-gradient(135deg, #f8fffc 0%, #f3fbff 48%, #fff7fb 100%);
}

.merchant-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  border-right: 1px solid rgba(137, 199, 255, 0.28);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(16px);
  padding: 18px 14px;
}

.brand-block {
  width: 100%;
  border: 0;
  background: transparent;
  display: grid;
  gap: 8px;
  justify-items: start;
  cursor: pointer;
  color: var(--text-main);
}

.brand-block img {
  width: 172px;
  height: 60px;
  object-fit: contain;
  object-position: left center;
}

.brand-block span {
  border-radius: 999px;
  background: #e9fff8;
  color: #159d7d;
  padding: 5px 11px;
  font-size: 12px;
  font-weight: 900;
}

.side-groups {
  margin-top: 18px;
  display: grid;
  gap: 16px;
}

.side-group p {
  margin: 0 0 7px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 900;
}

.side-nav-item {
  width: 100%;
  min-height: 40px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 900;
  cursor: pointer;
  text-align: left;
}

.side-nav-item + .side-nav-item {
  margin-top: 6px;
}

.side-nav-item:hover,
.side-nav-item.active {
  border-color: rgba(137, 199, 255, 0.42);
  background: rgba(255, 255, 255, 0.9);
  color: var(--brand-primary);
  box-shadow: var(--shadow-soft);
}

.nav-mark {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex: 0 0 auto;
}

.merchant-shell {
  min-width: 0;
}

.merchant-topbar {
  position: sticky;
  top: 0;
  z-index: 12;
  min-height: 82px;
  border-bottom: 1px solid rgba(137, 199, 255, 0.24);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px);
  padding: 14px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.top-kicker {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 900;
}

.merchant-topbar h1 {
  margin: 4px 0 0;
  font-size: 24px;
  line-height: 1.1;
}

.top-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.top-actions button {
  height: 36px;
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: #ffffff;
  color: var(--text-main);
  padding: 0 13px;
  font-weight: 900;
  cursor: pointer;
}

.top-actions button:first-child {
  border: 0;
  background: linear-gradient(135deg, #5fe6bd 0%, #69b9ff 100%);
  color: #ffffff;
}

.top-actions button:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-soft);
}

.merchant-main {
  padding: 18px 22px 36px;
}

.merchant-main :deep(.page-card),
.merchant-main :deep(.el-card) {
  border: 1px solid rgba(137, 199, 255, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-soft);
}

.merchant-main :deep(.toolbar),
.merchant-main :deep(.query-form),
.merchant-main :deep(.search-card) {
  border: 1px solid rgba(137, 199, 255, 0.24);
  border-radius: 8px;
  background: linear-gradient(90deg, rgba(233, 255, 248, 0.72), rgba(234, 244, 255, 0.68));
}

.merchant-main :deep(.el-table th.el-table__cell) {
  background: #f4fbff;
}

@media (max-width: 900px) {
  .merchant-layout {
    grid-template-columns: 1fr;
  }

  .merchant-sidebar {
    position: static;
    height: auto;
  }

  .side-groups {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
