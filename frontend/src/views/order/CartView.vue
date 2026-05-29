<template>
  <section class="cart-page">
    <div class="cart-hero">
      <div>
        <span class="eyebrow">Unified Cart</span>
        <h1>购物车</h1>
        <p>新品和二手可以放在同一个入口里确认，提交时会按交易规则自动拆成不同订单。</p>
      </div>
      <div class="hero-total">
        <span>已选合计</span>
        <strong>¥{{ selectedTotalAmount }}</strong>
        <small>{{ selectedCount }} / {{ totalCount }} 件商品</small>
      </div>
    </div>

    <el-empty v-if="totalCount === 0" class="empty-cart" description="购物车暂无商品">
      <el-button type="primary" @click="router.push('/product')">去逛新品商城</el-button>
      <el-button @click="router.push('/secondhand')">去逛二手市场</el-button>
    </el-empty>

    <div v-else class="cart-layout">
      <section class="cart-list-panel">
        <div class="list-toolbar">
          <el-checkbox
            :model-value="allSelected"
            :indeterminate="isIndeterminate"
            @change="toggleAll"
          >
            全选
          </el-checkbox>
          <span>{{ totalCount }} 件商品</span>
          <el-button text type="danger" @click="clear">清空</el-button>
        </div>

        <div v-if="items.length" class="order-group official-group">
          <div class="group-head">
            <div>
              <strong>官方商城订单</strong>
              <span>新品支持多件合并结算、库存扣减和统一发货</span>
            </div>
            <em>{{ items.length }} 件</em>
          </div>

          <article
            v-for="item in items"
            :key="`new-${item.productId}`"
            class="cart-item"
            :class="{ selected: isOfficialSelected(item) }"
          >
            <el-checkbox
              class="item-check"
              :model-value="isOfficialSelected(item)"
              @change="(checked) => toggleOfficialItem(item, checked)"
            />

            <img class="item-cover" :src="toAssetUrl(item.cover) || fallbackCover" :alt="item.name" />

            <div class="item-info">
              <button type="button" class="item-name" @click="router.push(`/product/${item.productId}`)">
                {{ item.name }}
              </button>
              <div class="item-tags">
                <span>官方商品</span>
                <span>多件合并</span>
                <span>库存同步</span>
              </div>
            </div>

            <div class="item-price">
              <span>单价</span>
              <strong>¥{{ Number(item.price || 0).toFixed(2) }}</strong>
            </div>

            <div class="item-quantity">
              <span>数量</span>
              <el-input-number
                v-model="item.quantity"
                :min="1"
                :max="999"
                size="small"
                @change="handleQtyChange"
              />
            </div>

            <div class="item-subtotal">
              <span>小计</span>
              <strong>¥{{ officialSubtotal(item) }}</strong>
              <button type="button" @click="removeOfficial(item)">移除</button>
            </div>
          </article>
        </div>

        <div v-if="secondhandItems.length" class="order-group secondhand-group">
          <div class="group-head">
            <div>
              <strong>个人闲置订单</strong>
              <span>二手商品按一件一单拆分，保留个人卖家、成色和沟通规则</span>
            </div>
            <em>{{ secondhandItems.length }} 件</em>
          </div>

          <article
            v-for="item in secondhandItems"
            :key="`secondhand-${item.productId}`"
            class="cart-item secondhand-item"
            :class="{ selected: isSecondhandSelected(item) }"
          >
            <el-checkbox
              class="item-check"
              :model-value="isSecondhandSelected(item)"
              @change="(checked) => toggleSecondhandItem(item, checked)"
            />

            <img class="item-cover" :src="toAssetUrl(item.cover) || secondhandFallbackCover" :alt="item.name" />

            <div class="item-info">
              <button type="button" class="item-name" @click="router.push(`/secondhand/${item.productId}`)">
                {{ item.name }}
              </button>
              <div class="item-tags">
                <span>{{ item.conditionLevel || "成色良好" }}</span>
                <span>{{ item.sellerName || "个人卖家" }}</span>
                <span>一件一单</span>
              </div>
            </div>

            <div class="item-price">
              <span>闲置价</span>
              <strong>¥{{ Number(item.price || 0).toFixed(2) }}</strong>
              <small v-if="item.originPrice">原价 ¥{{ Number(item.originPrice || 0).toFixed(2) }}</small>
            </div>

            <div class="item-quantity one-piece">
              <span>卖家</span>
              <strong>{{ item.sellerName || `卖家 ${item.sellerUserId || ""}` }}</strong>
            </div>

            <div class="item-subtotal">
              <span>订单规则</span>
              <strong>独立订单</strong>
              <button type="button" @click="removeSecondhand(item)">移除</button>
            </div>
          </article>
        </div>
      </section>

      <aside class="checkout-panel">
        <div class="checkout-card">
          <span class="checkout-kicker">Checkout</span>
          <h2>结算分组</h2>
          <dl>
            <div>
              <dt>已选商品</dt>
              <dd>{{ selectedCount }} 件</dd>
            </div>
            <div>
              <dt>预计生成订单</dt>
              <dd>{{ checkoutGroups.length }} 个</dd>
            </div>
            <div>
              <dt>商品金额</dt>
              <dd>¥{{ selectedTotalAmount }}</dd>
            </div>
          </dl>

          <div class="checkout-groups">
            <div v-for="group in checkoutGroups" :key="group.key" class="checkout-group">
              <strong>{{ group.title }}</strong>
              <span>{{ group.desc }}</span>
              <em>¥{{ group.amount }}</em>
            </div>
          </div>

          <div class="coupon-note">
            <strong>拆单说明</strong>
            <span>官方商品会合并成一个订单；二手商品会逐件生成个人闲置订单，便于分别沟通、发货和售后。</span>
          </div>

          <div class="payable">
            <span>应付合计</span>
            <strong>¥{{ selectedTotalAmount }}</strong>
          </div>

          <el-button
            type="primary"
            size="large"
            :disabled="selectedCount === 0"
            @click="checkout"
          >
            按分组结算
          </el-button>
          <el-button size="large" @click="router.push('/product')">继续逛新品</el-button>
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
import { createOrderApi } from "@/api/order";
import { getProductDetailApi } from "@/api/product";
import { buySecondhandApi, getSecondhandDetailApi } from "@/api/secondhand";
import { listAddressesApi } from "@/api/user";
import { clearCart, getCartItems, removeFromCart, saveCartItems } from "@/utils/cart";
import {
  clearSecondhandCart,
  getSecondhandCartItems,
  removeSecondhandFromCart,
  saveSecondhandCartItems,
} from "@/utils/secondhandCart";
import { toAssetUrl } from "@/utils/url";

const router = useRouter();
const items = ref([]);
const secondhandItems = ref([]);
const selectedItems = ref([]);
const selectedSecondhandItems = ref([]);
const fallbackCover = "https://images.unsplash.com/photo-1511556820780-d912e42b4980?auto=format&fit=crop&w=900&q=80";
const secondhandFallbackCover = "https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=900&q=80";

const totalCount = computed(() => items.value.length + secondhandItems.value.length);
const selectedCount = computed(() => selectedItems.value.length + selectedSecondhandItems.value.length);

const selectedTotalAmount = computed(() => {
  const officialTotal = selectedItems.value
    .reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0);
  const secondhandTotal = selectedSecondhandItems.value
    .reduce((sum, item) => sum + Number(item.price || 0), 0);
  return (officialTotal + secondhandTotal).toFixed(2);
});

const allSelected = computed(() => totalCount.value > 0 && selectedCount.value === totalCount.value);
const isIndeterminate = computed(() => selectedCount.value > 0 && selectedCount.value < totalCount.value);

const checkoutGroups = computed(() => {
  const groups = [];
  if (selectedItems.value.length) {
    groups.push({
      key: "official",
      title: "官方商城订单",
      desc: selectedItems.value.map((item) => item.name).join("、"),
      amount: selectedItems.value
        .reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0)
        .toFixed(2),
    });
  }
  selectedSecondhandItems.value.forEach((item) => {
    groups.push({
      key: `secondhand-${item.productId}`,
      title: "个人闲置订单",
      desc: `${item.name}${item.sellerName ? `，卖家 ${item.sellerName}` : ""}`,
      amount: Number(item.price || 0).toFixed(2),
    });
  });
  return groups;
});

onMounted(async () => {
  await refreshCartItems();
});

async function refreshCartItems() {
  const rawItems = getCartItems();
  const officialChecks = await Promise.allSettled(
    rawItems.map((item) => getProductDetailApi(item.productId)),
  );
  const validOfficialItems = rawItems.filter((_, index) => officialChecks[index].status === "fulfilled");

  const rawSecondhandItems = getSecondhandCartItems();
  const secondhandChecks = await Promise.allSettled(
    rawSecondhandItems.map((item) => getSecondhandDetailApi(item.productId)),
  );
  const validSecondhandItems = rawSecondhandItems.filter((_, index) => secondhandChecks[index].status === "fulfilled");

  items.value = validOfficialItems;
  secondhandItems.value = validSecondhandItems;
  selectedItems.value = [];
  selectedSecondhandItems.value = [];
  saveCartItems(validOfficialItems);
  saveSecondhandCartItems(validSecondhandItems);
}

function handleQtyChange() {
  saveCartItems(items.value);
}

function officialSubtotal(item) {
  return (Number(item.price || 0) * Number(item.quantity || 0)).toFixed(2);
}

function isOfficialSelected(row) {
  return selectedItems.value.some((item) => Number(item.productId) === Number(row.productId));
}

function isSecondhandSelected(row) {
  return selectedSecondhandItems.value.some((item) => Number(item.productId) === Number(row.productId));
}

function toggleOfficialItem(row, checked) {
  selectedItems.value = checked
    ? (isOfficialSelected(row) ? selectedItems.value : selectedItems.value.concat(row))
    : selectedItems.value.filter((item) => Number(item.productId) !== Number(row.productId));
}

function toggleSecondhandItem(row, checked) {
  selectedSecondhandItems.value = checked
    ? (isSecondhandSelected(row) ? selectedSecondhandItems.value : selectedSecondhandItems.value.concat(row))
    : selectedSecondhandItems.value.filter((item) => Number(item.productId) !== Number(row.productId));
}

function toggleAll(checked) {
  selectedItems.value = checked ? [...items.value] : [];
  selectedSecondhandItems.value = checked ? [...secondhandItems.value] : [];
}

function removeOfficial(row) {
  items.value = removeFromCart(row.productId);
  selectedItems.value = selectedItems.value.filter((item) => Number(item.productId) !== Number(row.productId));
}

function removeSecondhand(row) {
  secondhandItems.value = removeSecondhandFromCart(row.productId);
  selectedSecondhandItems.value = selectedSecondhandItems.value
    .filter((item) => Number(item.productId) !== Number(row.productId));
}

function clear() {
  clearCart();
  clearSecondhandCart();
  items.value = [];
  secondhandItems.value = [];
  selectedItems.value = [];
  selectedSecondhandItems.value = [];
}

async function checkout() {
  const officialCheckout = [...selectedItems.value];
  const secondhandCheckout = [...selectedSecondhandItems.value];
  if (!officialCheckout.length && !secondhandCheckout.length) {
    ElMessage.warning("请先选择要结算的商品");
    return;
  }

  const selectedAddressId = await confirmAddressAndPickId();
  if (!selectedAddressId) {
    return;
  }

  let officialOrderId = null;
  const createdSecondhandIds = new Set();

  try {
    if (officialCheckout.length) {
      const checks = await Promise.allSettled(
        officialCheckout.map((item) => getProductDetailApi(item.productId)),
      );
      const validOfficialItems = officialCheckout.filter((_, index) => checks[index].status === "fulfilled");
      if (validOfficialItems.length) {
        const result = await createOrderApi({
          addressId: selectedAddressId,
          items: validOfficialItems.map((item) => ({
            productId: item.productId,
            quantity: Number(item.quantity || 0),
          })),
        });
        officialOrderId = result?.data?.id || null;
        const checkoutIds = new Set(validOfficialItems.map((item) => Number(item.productId)));
        const remain = items.value.filter((item) => !checkoutIds.has(Number(item.productId)));
        saveCartItems(remain);
        items.value = remain;
      }
    }

    for (const item of secondhandCheckout) {
      try {
        await buySecondhandApi(item.productId, { addressId: selectedAddressId });
        createdSecondhandIds.add(Number(item.productId));
      } catch {
        // Keep failed secondhand items in the cart for the user to retry.
      }
    }

    if (createdSecondhandIds.size) {
      const remain = secondhandItems.value
        .filter((item) => !createdSecondhandIds.has(Number(item.productId)));
      saveSecondhandCartItems(remain);
      secondhandItems.value = remain;
    }

    selectedItems.value = [];
    selectedSecondhandItems.value = [];

    const createdOrderCount = (officialOrderId ? 1 : 0) + createdSecondhandIds.size;
    if (!createdOrderCount) {
      ElMessage.warning("下单未成功，请检查商品状态后重试");
      await refreshCartItems();
      return;
    }

    ElMessage.success(`下单成功，已生成 ${createdOrderCount} 个订单`);
    if (officialOrderId && createdSecondhandIds.size === 0) {
      router.push({ path: `/order/${officialOrderId}`, query: { action: "pay" } });
      return;
    }
    if (!officialOrderId && createdSecondhandIds.size) {
      router.push("/secondhand/orders");
      return;
    }
    router.push("/order");
  } catch {
    await refreshCartItems();
  }
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
    confirmButtonText: "确认下单",
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
  border: 1px solid rgba(60, 146, 255, 0.22);
  border-radius: 12px;
  background:
    linear-gradient(120deg, rgba(233, 255, 248, 0.94), rgba(234, 244, 255, 0.86), rgba(255, 247, 251, 0.74)),
    url("https://images.unsplash.com/photo-1607082349566-187342175e2f?auto=format&fit=crop&w=1400&q=80");
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

.cart-hero p {
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
  color: var(--brand-primary);
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
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 14px;
  align-items: start;
}

.cart-list-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-toolbar,
.order-group,
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

.list-toolbar span {
  color: var(--text-secondary);
  font-weight: 800;
}

.list-toolbar .el-button {
  margin-left: auto;
}

.order-group {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.group-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 2px 2px 8px;
}

.group-head strong,
.group-head span {
  display: block;
}

.group-head strong {
  color: var(--text-main);
  font-size: 18px;
}

.group-head span {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.group-head em {
  flex: 0 0 auto;
  border-radius: 999px;
  background: var(--brand-primary-weak);
  color: var(--brand-primary);
  padding: 5px 10px;
  font-style: normal;
  font-size: 12px;
  font-weight: 900;
}

.secondhand-group .group-head em {
  background: var(--brand-mint-weak);
  color: var(--brand-accent-strong);
}

.cart-item {
  border: 1px solid var(--line-soft);
  border-radius: 10px;
  background: #ffffff;
  padding: 14px;
  display: grid;
  grid-template-columns: auto 88px minmax(0, 1fr) 110px 150px 118px;
  gap: 14px;
  align-items: center;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.cart-item.selected,
.cart-item:hover {
  border-color: rgba(60, 146, 255, 0.35);
  box-shadow: var(--shadow-float);
}

.secondhand-item.selected,
.secondhand-item:hover {
  border-color: rgba(53, 216, 171, 0.38);
}

.cart-item:hover {
  transform: translateY(-1px);
}

.item-check {
  width: 22px;
}

.item-cover {
  width: 88px;
  height: 88px;
  border-radius: 12px;
  object-fit: cover;
  background: var(--surface-soft);
}

.item-info {
  min-width: 0;
}

.item-name {
  max-width: 100%;
  border: 0;
  background: transparent;
  padding: 0;
  color: var(--text-main);
  font-size: 16px;
  font-weight: 900;
  line-height: 1.45;
  text-align: left;
  cursor: pointer;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.item-name:hover {
  color: var(--brand-primary);
}

.item-tags {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
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

.official-group .item-tags span {
  border-color: rgba(60, 146, 255, 0.24);
  background: var(--brand-primary-weak);
  color: var(--brand-primary);
}

.item-price span,
.item-quantity span,
.item-subtotal span {
  display: block;
  margin-bottom: 6px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.item-price strong,
.item-quantity strong,
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

.item-subtotal strong {
  color: var(--brand-primary);
}

.secondhand-item .item-subtotal strong {
  color: var(--brand-accent-strong);
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

.one-piece strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.checkout-panel {
  position: sticky;
  top: 166px;
}

.checkout-card {
  border-color: rgba(60, 146, 255, 0.22);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.checkout-kicker {
  width: fit-content;
  border-radius: 999px;
  background: var(--brand-primary-weak);
  color: var(--brand-primary);
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 900;
}

.checkout-card h2 {
  margin: 0;
  font-size: 22px;
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

.checkout-groups {
  display: grid;
  gap: 8px;
}

.checkout-group {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: var(--surface-soft);
  padding: 10px;
}

.checkout-group strong,
.checkout-group span,
.checkout-group em {
  display: block;
}

.checkout-group strong {
  color: var(--text-main);
}

.checkout-group span {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.45;
}

.checkout-group em {
  margin-top: 7px;
  color: var(--brand-primary);
  font-style: normal;
  font-weight: 900;
}

.coupon-note {
  border: 1px solid rgba(255, 185, 214, 0.5);
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(255, 185, 214, 0.22), rgba(137, 199, 255, 0.22));
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
  color: var(--brand-primary);
  font-size: 28px;
  line-height: 1;
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
  .item-quantity,
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

  .item-cover {
    grid-column: 1 / -1;
    width: 100%;
    height: auto;
    aspect-ratio: 16 / 10;
  }

  .item-info,
  .item-price,
  .item-quantity,
  .item-subtotal {
    grid-column: 1 / -1;
  }
}
</style>
