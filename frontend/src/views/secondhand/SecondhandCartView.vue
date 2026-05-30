<template>
  <section class="cart-page secondhand-cart">
    <div class="cart-hero">
      <div>
        <span class="eyebrow">Want List</span>
        <h1>想买清单</h1>
        <p>二手商品会进入统一购物车，结算时按个人闲置订单拆分。</p>
      </div>
      <div class="hero-total">
        <span>已选合计</span>
        <strong>￥{{ selectedTotalAmount }}</strong>
        <small>{{ selectedItems.length }} / {{ items.length }} 件闲置</small>
      </div>
    </div>

    <el-empty v-if="items.length === 0" class="empty-cart" description="想买清单暂无商品">
      <el-button type="primary" @click="router.push('/secondhand')">去逛二手商城</el-button>
    </el-empty>

    <div v-else class="cart-layout">
      <section class="cart-list-panel">
        <div class="list-toolbar">
          <el-checkbox :model-value="allSelected" :indeterminate="isIndeterminate" @change="toggleAll">
            全选
          </el-checkbox>
          <span>{{ items.length }} 件闲置</span>
          <el-button text type="danger" @click="clear">清空</el-button>
        </div>

        <article
          v-for="item in items"
          :key="item.productId"
          class="cart-item"
          :class="{ selected: isSelected(item) }"
        >
          <el-checkbox class="item-check" :model-value="isSelected(item)" @change="(checked) => toggleItem(item, checked)" />
          <img class="item-cover" :src="toAssetUrl(item.cover) || fallbackCover" :alt="item.name" />

          <div class="item-info">
            <button type="button" class="item-name" @click="router.push(`/secondhand/${item.productId}`)">
              {{ item.name }}
            </button>
            <div class="item-tags">
              <span>{{ item.conditionLevel || "成色良好" }}</span>
              <span>{{ item.sellerName || "个人卖家" }}</span>
              <span>可沟通</span>
            </div>
          </div>

          <div class="item-price">
            <span>闲置价</span>
            <strong>￥{{ Number(item.price || 0).toFixed(2) }}</strong>
            <small v-if="item.originPrice">原价 ￥{{ Number(item.originPrice || 0).toFixed(2) }}</small>
          </div>

          <div class="item-subtotal">
            <span>交易方式</span>
            <strong>一件一单</strong>
            <button type="button" @click="remove(item)">移除</button>
          </div>
        </article>
      </section>

      <aside class="checkout-panel">
        <div class="checkout-card">
          <span class="checkout-kicker">Secondhand Checkout</span>
          <h2>二手结算预览</h2>
          <dl>
            <div>
              <dt>已选闲置</dt>
              <dd>{{ selectedItems.length }} 件</dd>
            </div>
            <div>
              <dt>预估金额</dt>
              <dd>￥{{ selectedTotalAmount }}</dd>
            </div>
            <div>
              <dt>订单规则</dt>
              <dd>每件生成独立二手订单</dd>
            </div>
          </dl>
          <div class="coupon-note">
            <strong>二手交易提醒</strong>
            <span>付款前可以先和卖家聊一聊；确认购买后，可在我的订单里查看状态。</span>
          </div>
          <div class="payable">
            <span>应付合计</span>
            <strong>￥{{ selectedTotalAmount }}</strong>
          </div>
          <el-button type="primary" size="large" :disabled="selectedItems.length === 0" @click="checkout">
            购买选中闲置
          </el-button>
          <el-button size="large" @click="router.push('/secondhand')">继续逛二手</el-button>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import { buySecondhandApi, getSecondhandDetailApi } from "@/api/secondhand";
import { listAddressesApi } from "@/api/user";
import {
  clearSecondhandCart,
  getSecondhandCartItems,
  removeSecondhandFromCart,
  saveSecondhandCartItems,
} from "@/utils/secondhandCart";
import { toAssetUrl } from "@/utils/url";

const router = useRouter();
const items = ref([]);
const selectedItems = ref([]);
const fallbackCover = "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=900&q=80";

const selectedTotalAmount = computed(() => selectedItems.value
  .reduce((sum, item) => sum + Number(item.price || 0), 0)
  .toFixed(2));
const allSelected = computed(() => items.value.length > 0 && selectedItems.value.length === items.value.length);
const isIndeterminate = computed(() => selectedItems.value.length > 0 && selectedItems.value.length < items.value.length);

onMounted(refreshCartItems);

async function refreshCartItems() {
  const rawItems = getSecondhandCartItems();
  const checks = await Promise.allSettled(rawItems.map((item) => getSecondhandDetailApi(item.productId)));
  const validItems = rawItems.filter((_, index) => checks[index].status === "fulfilled");
  items.value = validItems;
  selectedItems.value = [];
  saveSecondhandCartItems(validItems);
}

function isSelected(row) {
  return selectedItems.value.some((item) => Number(item.productId) === Number(row.productId));
}

function toggleItem(row, checked) {
  selectedItems.value = checked
    ? (isSelected(row) ? selectedItems.value : selectedItems.value.concat(row))
    : selectedItems.value.filter((item) => Number(item.productId) !== Number(row.productId));
}

function toggleAll(checked) {
  selectedItems.value = checked ? [...items.value] : [];
}

function remove(row) {
  items.value = removeSecondhandFromCart(row.productId);
  selectedItems.value = selectedItems.value.filter((item) => Number(item.productId) !== Number(row.productId));
}

function clear() {
  clearSecondhandCart();
  items.value = [];
  selectedItems.value = [];
}

async function checkout() {
  const toCheckout = [...selectedItems.value];
  if (!toCheckout.length) {
    ElMessage.warning("请先选择要购买的二手商品");
    return;
  }
  const selectedAddressId = await confirmAddressAndPickId();
  if (!selectedAddressId) {
    return;
  }
  const results = [];
  for (const item of toCheckout) {
    results.push(await buySecondhandApi(item.productId, { addressId: selectedAddressId }).then(
      (res) => ({ ok: true, item, res }),
      () => ({ ok: false, item }),
    ));
  }
  const successIds = new Set(results.filter((result) => result.ok).map((result) => Number(result.item.productId)));
  const remain = items.value.filter((item) => !successIds.has(Number(item.productId)));
  saveSecondhandCartItems(remain);
  items.value = remain;
  selectedItems.value = [];
  if (successIds.size) {
    ElMessage.success(`已生成 ${successIds.size} 个二手订单`);
    router.push({ path: "/order", query: { type: "SECONDHAND" } });
    return;
  }
  await refreshCartItems();
}

async function confirmAddressAndPickId() {
  const result = await listAddressesApi();
  const addresses = result?.data || [];
  if (!addresses.length) {
    ElMessage.warning("请先新增收货地址后再下单");
    router.push({ name: "addressManager" });
    return null;
  }
  const preferred = addresses.find((a) => Number(a.isDefault) === 1) || addresses[0];
  const summary = `${preferred.receiverName} ${preferred.receiverPhone}\n${preferred.province}${preferred.city}${preferred.detailAddress}`;
  const confirmed = await ElMessageBox.confirm(`请确认本次收货地址：\n${summary}`, "确认收货地址", {
    confirmButtonText: "确认购买",
    cancelButtonText: "去改地址",
    type: "warning",
  }).then(() => true).catch(() => {
    router.push({ name: "addressManager" });
    return false;
  });
  return confirmed ? preferred.id : null;
}
</script>

<style scoped>
.cart-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.cart-hero {
  min-height: 170px;
  border: 1px solid rgba(53, 216, 171, 0.24);
  border-radius: 12px;
  background:
    linear-gradient(120deg, rgba(233, 255, 248, 0.94), rgba(234, 244, 255, 0.86), rgba(255, 247, 251, 0.74)),
    url("https://images.unsplash.com/photo-1526178613552-2b45c6c302f0?auto=format&fit=crop&w=1400&q=80");
  background-size: cover;
  background-position: center;
  color: var(--text-main);
  padding: 24px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
  box-shadow: var(--shadow-soft);
}

.eyebrow {
  display: inline-flex;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-main);
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 900;
}

.cart-hero h1 {
  margin: 12px 0 8px;
  font-size: clamp(32px, 5vw, 48px);
  line-height: 1;
}

.cart-hero p,
.list-toolbar span {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 800;
}

.hero-total {
  min-width: 190px;
  border: 1px solid rgba(137, 199, 255, 0.45);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  padding: 14px;
  text-align: right;
  backdrop-filter: blur(10px);
}

.hero-total span,
.hero-total strong,
.hero-total small {
  display: block;
}

.hero-total span,
.hero-total small {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.hero-total strong {
  margin: 5px 0;
  color: var(--brand-accent-strong);
  font-size: 28px;
}

.empty-cart {
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.86);
  padding: 40px 12px;
}

.cart-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 14px;
  align-items: start;
}

.cart-list-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.list-toolbar,
.cart-item,
.checkout-card {
  border: 1px solid var(--line-soft);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow-soft);
}

.list-toolbar {
  min-height: 52px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.list-toolbar .el-button {
  margin-left: auto;
}

.cart-item {
  padding: 14px;
  display: grid;
  grid-template-columns: auto 88px minmax(0, 1fr) 130px 118px;
  gap: 14px;
  align-items: center;
}

.cart-item.selected,
.cart-item:hover {
  border-color: rgba(53, 216, 171, 0.38);
  box-shadow: var(--shadow-float);
}

.item-cover {
  width: 88px;
  height: 88px;
  border-radius: 12px;
  object-fit: cover;
}

.item-name {
  border: 0;
  background: transparent;
  padding: 0;
  color: var(--text-main);
  font-size: 16px;
  font-weight: 900;
  line-height: 1.45;
  text-align: left;
  cursor: pointer;
}

.item-tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.item-tags span {
  border: 1px solid rgba(53, 216, 171, 0.32);
  border-radius: 999px;
  background: var(--brand-mint-weak);
  color: var(--brand-accent-strong);
  padding: 4px 8px;
  font-size: 12px;
  font-weight: 800;
}

.item-price span,
.item-subtotal span {
  display: block;
  margin-bottom: 6px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.item-price strong,
.item-subtotal strong {
  display: block;
  color: var(--text-main);
  font-size: 16px;
}

.item-price small {
  display: block;
  margin-top: 5px;
  color: var(--text-muted);
  text-decoration: line-through;
}

.item-subtotal button {
  margin-top: 8px;
  border: 0;
  background: transparent;
  color: var(--danger);
  padding: 0;
  font-weight: 800;
  cursor: pointer;
}

.checkout-panel {
  position: sticky;
  top: 166px;
}

.checkout-card {
  border-color: rgba(53, 216, 171, 0.24);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.checkout-kicker {
  width: fit-content;
  border-radius: 999px;
  background: var(--brand-mint-weak);
  color: var(--brand-accent-strong);
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 900;
}

.checkout-card h2 {
  margin: 0;
}

.checkout-card dl {
  margin: 0;
  display: grid;
  gap: 10px;
}

.checkout-card dl div,
.payable {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.checkout-card dt {
  color: var(--text-secondary);
}

.checkout-card dd {
  margin: 0;
  color: var(--text-main);
  font-weight: 900;
}

.coupon-note {
  border: 1px solid rgba(53, 216, 171, 0.32);
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(233, 255, 248, 0.9), rgba(234, 244, 255, 0.68));
  padding: 12px;
}

.coupon-note strong,
.coupon-note span {
  display: block;
}

.coupon-note span {
  margin-top: 5px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.payable {
  border-top: 1px solid var(--line-soft);
  padding-top: 14px;
  align-items: baseline;
}

.payable span {
  color: var(--text-secondary);
  font-weight: 900;
}

.payable strong {
  color: var(--brand-accent-strong);
  font-size: 28px;
}

.checkout-card :deep(.el-button) {
  width: 100%;
  margin-left: 0;
}

@media (max-width: 980px) {
  .cart-layout {
    grid-template-columns: 1fr;
  }

  .checkout-panel {
    position: static;
  }

  .cart-item {
    grid-template-columns: auto 82px minmax(0, 1fr);
  }

  .item-price,
  .item-subtotal {
    grid-column: 3;
  }
}

@media (max-width: 720px) {
  .cart-hero {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-total {
    text-align: left;
  }

  .cart-item {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .item-cover,
  .item-info,
  .item-price,
  .item-subtotal {
    grid-column: 1 / -1;
  }

  .item-cover {
    width: 100%;
    height: auto;
    aspect-ratio: 16 / 10;
  }
}
</style>
