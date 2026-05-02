<template>
  <div class="seller-shell">
    <header class="seller-topbar">
      <div class="topbar-inner">
        <button class="seller-brand" type="button" @click="router.push('/merchant')">
          <span class="brand-mark">卖</span>
          <span>
            <strong>卖家中心</strong>
            <em>校园闲置商家中心</em>
          </span>
        </button>

        <nav class="top-actions" aria-label="卖家快捷入口">
          <button type="button" @click="router.push('/merchant/seller-products/edit')">发布商品</button>
          <button type="button" @click="router.push('/merchant/orders')">订单管理</button>
          <button type="button" @click="router.push('/')">返回商城</button>
        </nav>
      </div>
    </header>

    <div class="seller-workspace">
      <aside class="seller-aside">
        <button
          v-for="item in navItems"
          :key="item.path"
          class="nav-link"
          :class="{ active: isActive(item.path) }"
          type="button"
          @click="router.push(item.path)"
        >
          <span>{{ item.icon }}</span>
          <strong>{{ item.label }}</strong>
        </button>
      </aside>

      <main class="seller-main fade-in-up">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()

const navItems = [
  { label: '商品管理', path: '/merchant/seller-products', icon: '□' },
  { label: '订单管理', path: '/merchant/orders', icon: '≡' },
  { label: '优惠券管理', path: '/merchant/vouchers', icon: '%' },
  { label: '数据看板', path: '/merchant/seller-dashboard', icon: '↗' },
  { label: '账户健康', path: '/merchant/account-health', icon: '✓' },
  { label: '评价管理', path: '/merchant/reviews', icon: '☆' },
  { label: '店铺信息', path: '/merchant/seller-shop', icon: '⌂' },
  { label: '店铺装修', path: '/merchant/shop-decoration', icon: '◇' },
]

function isActive(path) {
  return route.path.startsWith(path)
}
</script>

<style scoped>
.seller-shell {
  min-height: 100vh;
  background: #f2f4f6;
}

.seller-topbar {
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
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.seller-brand,
.top-actions button,
.nav-link {
  font: inherit;
}

.seller-brand {
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
  display: grid;
  place-items: center;
  border: 3px solid #20242d;
  border-radius: 14px;
  font-size: 28px;
  line-height: 1;
  font-weight: 900;
}

.seller-brand strong,
.seller-brand em {
  display: block;
}

.seller-brand strong {
  font-size: 24px;
  line-height: 1.1;
  font-weight: 900;
}

.seller-brand em {
  margin-top: 4px;
  color: #4f5662;
  font-size: 13px;
  font-style: normal;
  font-weight: 700;
}

.top-actions {
  display: flex;
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

.seller-workspace {
  width: min(1440px, calc(100% - 44px));
  margin: 18px auto 32px;
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  gap: 18px;
}

.seller-aside {
  position: sticky;
  top: 94px;
  align-self: start;
  border: 1px solid #eeeeee;
  border-radius: 20px;
  background: #fff;
  padding: 12px;
  box-shadow: 0 8px 20px rgba(30, 34, 40, 0.04);
}

.nav-link {
  width: 100%;
  height: 42px;
  border: 0;
  border-radius: 12px;
  padding: 0 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: transparent;
  color: #333842;
  cursor: pointer;
  text-align: left;
}

.nav-link span {
  width: 24px;
  height: 24px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #fff7c2;
  color: #20242d;
  font-weight: 900;
}

.nav-link:hover,
.nav-link.active {
  background: #fff7c2;
}

.seller-main {
  min-width: 0;
}

@media (max-width: 900px) {
  .seller-workspace {
    width: calc(100% - 16px);
    grid-template-columns: 1fr;
  }

  .seller-aside {
    position: static;
    display: flex;
    gap: 8px;
    overflow-x: auto;
  }

  .nav-link {
    width: auto;
    min-width: 96px;
    justify-content: center;
  }
}
</style>
