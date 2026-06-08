<template>
  <section class="address-page">
    <div class="address-topbar">
      <div class="title-copy">
        <span>收货地址</span>
        <h1>地址管理</h1>
        <p>默认地址会在结算时自动带入，方便快速下单。</p>
      </div>
      <button class="add-address-btn" type="button" @click="openDialog()">
        <span>+</span>
        新增地址
      </button>
    </div>

    <div class="address-content">
      <section class="address-list-panel">
        <div class="panel-head">
          <div>
            <strong>我的收货地址</strong>
            <span>{{ addresses.length }} 个地址 · {{ defaultAddress ? "已设置默认" : "未设置默认" }}</span>
          </div>
        </div>

        <el-empty v-if="!addresses.length" description="暂无收货地址" class="empty-address">
          <el-button type="primary" @click="openDialog()">添加第一个地址</el-button>
        </el-empty>

        <div v-else class="address-list">
          <article
            v-for="item in addresses"
            :key="item.id"
            class="address-card"
            :class="{ default: isDefault(item) }"
          >
            <div class="card-main">
              <div class="card-top">
                <span class="receiver-badge">{{ avatarText(item.receiverName) }}</span>
                <div class="receiver-info">
                  <div class="name-row">
                    <strong>{{ item.receiverName }}</strong>
                    <span>{{ maskPhone(item.receiverPhone) }}</span>
                  </div>
                  <div class="region-row">
                    <span>{{ item.province || "未填省份" }}</span>
                    <span>{{ item.city || "未填城市" }}</span>
                  </div>
                </div>
                <span v-if="isDefault(item)" class="default-label">默认地址</span>
              </div>

              <p class="address-detail">{{ item.detailAddress || fullAddress(item) }}</p>
            </div>

            <div class="card-footer">
              <label class="default-radio" :class="{ active: isDefault(item) }">
                <input type="radio" name="defaultAddress" :checked="isDefault(item)" @change="setDefault(item)" />
                <span></span>
                {{ isDefault(item) ? "当前默认" : "设为默认" }}
              </label>

              <div class="footer-actions">
                <button type="button" @click="openDialog(item)">编辑</button>
                <el-popconfirm title="确定删除该地址吗？" @confirm="removeAddress(item.id)">
                  <template #reference>
                    <button type="button" class="danger">删除</button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </article>
        </div>
      </section>

      <aside class="address-preview">
        <div class="preview-card">
          <span class="preview-kicker">结算默认地址</span>
          <template v-if="defaultAddress">
            <strong>{{ defaultAddress.receiverName }}</strong>
            <p class="preview-phone">{{ maskPhone(defaultAddress.receiverPhone) }}</p>
            <p>{{ fullAddress(defaultAddress) }}</p>
          </template>
          <template v-else>
            <strong>还没有默认地址</strong>
            <p>把常用地址设为默认后，结算时会优先使用它。</p>
          </template>
        </div>

        <div class="address-tips">
          <strong>填写建议</strong>
          <p>收件人、手机号和门牌号尽量完整，卖家发货时不容易出错。</p>
        </div>
      </aside>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑地址' : '新增地址'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="address-form">
        <div class="form-grid">
          <el-form-item label="收件人" prop="receiverName">
            <el-input v-model="form.receiverName" placeholder="请输入收件人姓名" />
          </el-form-item>
          <el-form-item label="联系电话" prop="receiverPhone">
            <el-input v-model="form.receiverPhone" placeholder="请输入 11 位手机号" />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="省份" prop="province">
            <el-select v-model="form.province" filterable placeholder="请选择省份" style="width: 100%">
              <el-option v-for="province in provinceOptions" :key="province" :label="province" :value="province" />
            </el-select>
          </el-form-item>
          <el-form-item label="城市" prop="city">
            <el-input v-model="form.city" placeholder="请输入城市" />
          </el-form-item>
        </div>

        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="form.detailAddress"
            type="textarea"
            :rows="4"
            maxlength="180"
            show-word-limit
            placeholder="例如：海淀区中关村软件园 1 号楼 608"
          />
        </el-form-item>

        <div class="default-switch">
          <div>
            <strong>设为默认地址</strong>
            <span>打开后，结算时会优先使用这个地址。</span>
          </div>
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAddress">保存地址</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { createAddressApi, deleteAddressApi, listAddressesApi, updateAddressApi } from "@/api/user";
import { provinceOptions } from "@/utils/provinces";

const addresses = ref([]);
const dialogVisible = ref(false);
const saving = ref(false);
const formRef = ref();
const form = reactive(resetForm());

const defaultAddress = computed(() => addresses.value.find((item) => isDefault(item)));

const rules = {
  receiverName: [{ required: true, message: "请输入收件人", trigger: "blur" }],
  receiverPhone: [
    { required: true, message: "请输入联系电话", trigger: "blur" },
    { pattern: /^1\d{10}$/, message: "手机号需为11位", trigger: "blur" },
  ],
  province: [{ required: true, message: "请选择省份", trigger: "change" }],
  city: [{ required: true, message: "请输入城市", trigger: "blur" }],
  detailAddress: [{ required: true, message: "请输入详细地址", trigger: "blur" }],
};

onMounted(loadAddresses);

function resetForm() {
  return {
    id: null,
    receiverName: "",
    receiverPhone: "",
    province: "",
    city: "",
    detailAddress: "",
    isDefault: 0,
  };
}

async function loadAddresses() {
  try {
    const result = await listAddressesApi();
    addresses.value = result.data || [];
  } catch {
    addresses.value = [];
  }
}

function openDialog(row) {
  Object.assign(form, resetForm(), row || {});
  dialogVisible.value = true;
}

async function saveAddress() {
  await formRef.value.validate();
  saving.value = true;
  try {
    const payload = buildPayload(form);
    if (form.id) {
      await updateAddressApi(form.id, payload);
    } else {
      await createAddressApi(payload);
    }
    ElMessage.success("保存成功");
    dialogVisible.value = false;
    await loadAddresses();
  } finally {
    saving.value = false;
  }
}

async function setDefault(row) {
  if (isDefault(row)) {
    return;
  }
  try {
    await updateAddressApi(row.id, buildPayload({ ...row, isDefault: 1 }));
    ElMessage.success("已设为默认地址");
    await loadAddresses();
  } catch {
    // API layer already shows the backend message.
  }
}

async function removeAddress(id) {
  try {
    await deleteAddressApi(id);
    ElMessage.success("删除成功");
    await loadAddresses();
  } catch {
    // API layer already shows the backend message.
  }
}

function buildPayload(source) {
  return {
    receiverName: source.receiverName,
    receiverPhone: source.receiverPhone,
    province: source.province,
    city: source.city,
    detailAddress: source.detailAddress,
    isDefault: source.isDefault,
  };
}

function isDefault(item) {
  return Number(item?.isDefault) === 1;
}

function fullAddress(item) {
  return [item.province, item.city, item.detailAddress].filter(Boolean).join(" ");
}

function avatarText(name) {
  return String(name || "收").slice(0, 1);
}

function maskPhone(phone) {
  const value = String(phone || "");
  if (value.length < 7) {
    return value || "未填写电话";
  }
  return `${value.slice(0, 3)}****${value.slice(-4)}`;
}
</script>

<style scoped>
.address-page {
  min-height: calc(100vh - 172px);
  border: 1px solid rgba(137, 199, 255, 0.28);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(233, 255, 248, 0.88), rgba(234, 244, 255, 0.78) 48%, rgba(255, 247, 251, 0.88)),
    #f8fcff;
  padding: 16px;
  box-shadow: var(--shadow-soft);
}

.address-topbar {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(137, 199, 255, 0.34);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  padding: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.address-topbar::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: 5px;
  background: linear-gradient(90deg, #7ee8cb 0%, #9bd8ff 52%, #ffc6dc 100%);
}

.title-copy {
  position: relative;
}

.title-copy span {
  display: inline-flex;
  border-radius: 999px;
  background: #e9fff8;
  color: #159d7d;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 900;
}

.title-copy h1 {
  margin: 10px 0 6px;
  font-size: 28px;
  line-height: 1.1;
  letter-spacing: 0;
}

.title-copy p {
  margin: 0;
  color: var(--text-secondary);
  font-weight: 700;
}

.add-address-btn {
  position: relative;
  height: 42px;
  border: 0;
  border-radius: 8px;
  background: linear-gradient(135deg, #5fe6bd 0%, #69b9ff 100%);
  color: #ffffff;
  padding: 0 17px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 900;
  box-shadow: 0 10px 22px rgba(60, 146, 255, 0.18);
  cursor: pointer;
  white-space: nowrap;
}

.add-address-btn span {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.28);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  line-height: 1;
}

.address-content {
  margin-top: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 292px;
  gap: 14px;
  align-items: start;
}

.address-list-panel,
.address-preview {
  border: 1px solid rgba(137, 199, 255, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow-soft);
}

.address-list-panel {
  padding: 14px;
}

.panel-head {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-head strong,
.panel-head span {
  display: block;
}

.panel-head strong {
  font-size: 18px;
}

.panel-head span {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.empty-address {
  min-height: 260px;
}

.address-list {
  display: grid;
  gap: 12px;
}

.address-card {
  overflow: hidden;
  border: 1px solid #deebf6;
  border-radius: 8px;
  background: #ffffff;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.address-card:hover {
  transform: translateY(-1px);
  border-color: #abd9fb;
  box-shadow: 0 14px 28px rgba(77, 148, 214, 0.12);
}

.address-card.default {
  border-color: rgba(53, 216, 171, 0.62);
  background: linear-gradient(135deg, rgba(233, 255, 248, 0.95), #ffffff 58%);
}

.card-main {
  padding: 15px;
}

.card-top {
  display: flex;
  align-items: flex-start;
  gap: 11px;
}

.receiver-badge {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: linear-gradient(135deg, #61e8c0 0%, #6db7ff 100%);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  font-size: 18px;
  font-weight: 900;
  box-shadow: 0 8px 18px rgba(53, 216, 171, 0.2);
}

.receiver-info {
  min-width: 0;
  flex: 1;
}

.name-row {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
}

.name-row strong {
  color: var(--text-main);
  font-size: 17px;
}

.name-row span,
.region-row {
  color: var(--text-secondary);
  font-weight: 800;
}

.region-row {
  margin-top: 7px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 12px;
}

.region-row span {
  border-radius: 999px;
  background: #f0f8ff;
  padding: 4px 8px;
}

.default-label {
  border-radius: 999px;
  background: #dffaf1;
  color: #109272;
  padding: 5px 10px;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.address-detail {
  margin: 13px 0 0 53px;
  color: var(--text-main);
  font-size: 15px;
  line-height: 1.65;
}

.card-footer {
  border-top: 1px solid #edf3f8;
  background: linear-gradient(90deg, rgba(247, 251, 255, 0.95), rgba(255, 247, 251, 0.72));
  padding: 10px 15px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.default-radio {
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-weight: 900;
  cursor: pointer;
}

.default-radio input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}

.default-radio span {
  width: 16px;
  height: 16px;
  border: 2px solid #bdd7ec;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: inset 0 0 0 3px #ffffff;
}

.default-radio.active {
  color: #109272;
}

.default-radio.active span {
  border-color: #35d8ab;
  background: #35d8ab;
}

.footer-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.footer-actions button {
  height: 30px;
  border: 1px solid #d8e8f5;
  border-radius: 8px;
  background: #ffffff;
  color: var(--brand-primary);
  padding: 0 12px;
  font-weight: 900;
  cursor: pointer;
}

.footer-actions button:hover {
  border-color: #9bd8ff;
  background: #eef8ff;
}

.footer-actions .danger {
  color: #f0647d;
}

.footer-actions .danger:hover {
  border-color: #ffc2cf;
  background: #fff4f7;
}

.address-preview {
  position: sticky;
  top: 166px;
  padding: 14px;
}

.preview-card {
  border-radius: 8px;
  background: linear-gradient(135deg, #e9fff8 0%, #eaf4ff 100%);
  padding: 14px;
}

.preview-kicker,
.preview-card strong,
.preview-card p,
.address-tips strong,
.address-tips p {
  display: block;
}

.preview-kicker {
  color: #159d7d;
  font-size: 12px;
  font-weight: 900;
}

.preview-card strong {
  margin-top: 10px;
  color: var(--text-main);
  font-size: 20px;
}

.preview-card p {
  margin: 8px 0 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.preview-phone {
  font-weight: 900;
}

.address-tips {
  margin-top: 12px;
  border: 1px solid #edf3f8;
  border-radius: 8px;
  background: #ffffff;
  padding: 12px;
}

.address-tips strong {
  font-size: 15px;
}

.address-tips p {
  margin: 7px 0 0;
  color: var(--text-secondary);
  line-height: 1.6;
}

.address-form {
  padding-top: 4px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.default-switch {
  border: 1px solid var(--line-soft);
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(233, 255, 248, 0.9), rgba(234, 244, 255, 0.88));
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.default-switch strong,
.default-switch span {
  display: block;
}

.default-switch span {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 760px) {
  .address-page {
    padding: 10px;
  }

  .address-topbar,
  .card-footer,
  .default-switch {
    flex-direction: column;
    align-items: stretch;
  }

  .address-content {
    grid-template-columns: 1fr;
  }

  .address-preview {
    position: static;
  }

  .address-detail {
    margin-left: 0;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
