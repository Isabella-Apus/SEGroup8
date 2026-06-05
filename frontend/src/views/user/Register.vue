<template>
  <div class="register-commerce">
    <div class="gradient-lane lane-one" aria-hidden="true"></div>
    <div class="gradient-lane lane-two" aria-hidden="true"></div>

    <header class="register-top">
      <button class="brand-link" type="button" @click="router.push('/')">
        <img :src="logoUrl" alt="Kinda Goods" />
      </button>
      <button class="home-link" type="button" @click="router.push('/')">返回商城</button>
    </header>

    <main class="register-stage fade-in-up">
      <section class="showcase-panel">
        <div class="showcase-copy">
          <span class="eyebrow">Kinda Goods</span>
          <h1>创建你的商城账号</h1>
          <p>Kinda Goods 连接官方商品、个人闲置和校园卖家，账号用于统一管理交易、地址与信用记录。</p>
        </div>

        <!-- <div class="feature-grid">
          <div v-for="item in featureCards" :key="item.label" class="feature-card">
            <span class="feature-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </span>
            <strong>{{ item.label }}</strong>
            <small>{{ item.desc }}</small>
          </div>
        </div> -->

        <!-- <div class="account-preview" aria-hidden="true">
          <div class="preview-profile">
            <span class="preview-avatar">KG</span>
            <div>
              <strong>新账号</strong>
              <small>待填写资料</small>
            </div>
          </div>
          <span class="preview-line"></span>
          <div class="preview-chip chip-mint">购物车</div>
          <div class="preview-chip chip-blue">订单</div>
          <div class="preview-chip chip-rose">二手</div>
        </div> -->
      </section>

      <section class="register-panel">
        <div class="panel-head">
          <span class="panel-kicker">Create account</span>
          <h2>注册 Kinda Goods</h2>
          <p>填写基础资料后，可用于登录、下单和接收交易通知。</p>
        </div>

        <el-form ref="formRef" class="register-form" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" size="large">
              <template #prefix>
                <el-icon>
                  <User />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large">
              <template #prefix>
                <el-icon>
                  <Lock />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="form.nickname" placeholder="请输入昵称" size="large">
              <template #prefix>
                <el-icon>
                  <EditPen />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="form-grid">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" size="large">
                <template #prefix>
                  <el-icon>
                    <Phone />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" size="large">
                <template #prefix>
                  <el-icon>
                    <Message />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>
          </div>

          <el-button class="register-submit" type="primary" :loading="loading" @click="handleRegister">
            <span>创建账号</span>
            <el-icon>
              <ArrowRight />
            </el-icon>
          </el-button>
        </el-form>

        <div class="auth-switch">
          <span>已有账号？</span>
          <button type="button" @click="router.push('/login')">返回登录</button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { markRaw, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  ArrowRight,
  EditPen,
  Goods,
  Lock,
  Message,
  Phone,
  ShoppingCart,
  Ticket,
  User,
} from "@element-plus/icons-vue";
import { useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import logoUrl from "@/assets/kinda-goods-logo.svg";

const router = useRouter();
const userStore = useUserStore();
const formRef = ref();
const loading = ref(false);

const form = reactive({
  username: "",
  password: "",
  nickname: "",
  phone: "",
  email: "",
});

const featureCards = [
  { label: "购物车", desc: "统一管理待购商品", icon: markRaw(ShoppingCart) },
  { label: "新品商城", desc: "查看在售新品", icon: markRaw(Goods) },
  { label: "领券中心", desc: "下单前领取优惠", icon: markRaw(Ticket) },
];

const rules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }, { min: 6, message: "密码至少6位", trigger: "blur" }],
  nickname: [{ required: true, message: "请输入昵称", trigger: "blur" }],
  phone: [{ pattern: /^$|^1\d{10}$/, message: "手机号需为11位", trigger: "blur" }],
};

async function handleRegister() {
  await formRef.value.validate();
  loading.value = true;
  try {
    await userStore.register(form);
    ElMessage.success("注册成功，请登录");
    router.push("/login");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.register-commerce {
  box-sizing: border-box;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 24px;
  background:
    linear-gradient(135deg, rgba(248, 255, 252, 0.96), rgba(243, 251, 255, 0.92) 45%, rgba(255, 247, 251, 0.9));
  background-size: cover;
  background-position: center;
}

.register-commerce::before {
  position: absolute;
  inset: 0;
  content: "";
  background-image:
    linear-gradient(rgba(60, 146, 255, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(53, 216, 171, 0.07) 1px, transparent 1px);
  background-size: 48px 48px;
  mask-image: linear-gradient(120deg, transparent 0%, black 20%, black 78%, transparent 100%);
}

.register-commerce::after {
  position: absolute;
  inset: 0;
  content: "";
  background:
    linear-gradient(110deg, transparent 0%, rgba(233, 255, 248, 0.42) 26%, transparent 48%),
    linear-gradient(250deg, transparent 8%, rgba(234, 244, 255, 0.38) 42%, transparent 70%);
  animation: surfaceFlow 12s ease-in-out infinite alternate;
  pointer-events: none;
}

.gradient-lane {
  position: absolute;
  width: 78vw;
  height: 220px;
  border: 1px solid rgba(255, 255, 255, 0.48);
  background: linear-gradient(90deg, rgba(95, 230, 189, 0.28), rgba(137, 199, 255, 0.3), rgba(255, 185, 214, 0.24));
  filter: blur(4px);
  transform: rotate(-11deg);
  opacity: 0.78;
  animation: laneDrift 10s ease-in-out infinite alternate;
}

.lane-one {
  top: 76px;
  left: -18vw;
}

.lane-two {
  right: -22vw;
  bottom: 36px;
  transform: rotate(-11deg) scaleX(0.82);
  animation-delay: -4s;
}

.register-top {
  position: relative;
  z-index: 2;
  width: min(1120px, 100%);
  margin: 0 auto 18px;
  min-height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.brand-link,
.home-link {
  border: 1px solid rgba(137, 199, 255, 0.36);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
  box-shadow: 0 12px 28px rgba(137, 199, 255, 0.14);
  cursor: pointer;
}

.brand-link {
  width: 178px;
  height: 56px;
  padding: 6px 10px;
}

.brand-link img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
  object-position: left center;
}

.home-link {
  height: 40px;
  padding: 0 15px;
  color: var(--text-main);
  font-weight: 900;
}

.home-link:hover {
  border-color: var(--brand-primary);
  color: var(--brand-primary);
  background: var(--brand-primary-weak);
}

.register-stage {
  position: relative;
  z-index: 2;
  width: min(1120px, 100%);
  min-height: 600px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 0.96fr) minmax(430px, 1.04fr);
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.84);
  box-shadow:
    0 24px 64px rgba(40, 124, 255, 0.18),
    0 1px 0 rgba(255, 255, 255, 0.9) inset;
  backdrop-filter: blur(18px) saturate(1.06);
}

.showcase-panel {
  position: relative;
  min-height: 600px;
  padding: 36px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 28px;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(234, 244, 255, 0.94), rgba(233, 255, 248, 0.78), rgba(255, 247, 251, 0.58));
  background-size: cover;
  background-position: center;
}

.showcase-panel::before {
  position: absolute;
  inset: auto 0 0 0;
  height: 170px;
  content: "";
  background: linear-gradient(0deg, rgba(255, 255, 255, 0.92), transparent);
}

.showcase-copy,
.feature-grid,
.account-preview {
  position: relative;
  z-index: 1;
}

.eyebrow {
  display: inline-flex;
  width: fit-content;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--text-main);
  padding: 6px 12px;
  font-size: 20px;
  font-weight: 900;
}

.showcase-copy h1 {
  max-width: 520px;
  margin: 18px 0 12px;
  color: var(--text-main);
  font-size: clamp(34px, 4.7vw, 54px);
  line-height: 1.04;
  letter-spacing: 0;
}

.showcase-copy p {
  max-width: 500px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 17px;
  line-height: 1.75;
  font-weight: 800;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.feature-card {
  min-height: 112px;
  border: 1px solid rgba(137, 199, 255, 0.38);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.76);
  padding: 14px;
  display: grid;
  gap: 7px;
  backdrop-filter: blur(10px);
  box-shadow: 0 12px 24px rgba(137, 199, 255, 0.12);
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--brand-gradient-strong);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.feature-icon :deep(.el-icon) {
  font-size: 22px;
}

.feature-card strong,
.feature-card small {
  display: block;
  overflow-wrap: anywhere;
}

.feature-card strong {
  color: var(--text-main);
  font-size: 16px;
  font-weight: 900;
}

.feature-card small {
  color: var(--text-secondary);
  font-weight: 700;
  line-height: 1.45;
}

.account-preview {
  min-height: 128px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.82), rgba(234, 244, 255, 0.72)),
    rgba(255, 255, 255, 0.78);
  box-shadow: 0 18px 38px rgba(40, 124, 255, 0.16);
  overflow: hidden;
  padding: 18px;
}

.account-preview::before {
  position: absolute;
  top: -28%;
  bottom: -28%;
  left: -36%;
  width: 32%;
  content: "";
  background: linear-gradient(110deg, transparent 0%, rgba(255, 255, 255, 0.72) 48%, transparent 100%);
  transform: skewX(-13deg);
  animation: previewSweep 6s ease-in-out infinite;
}

.preview-profile {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--text-main);
}

.preview-avatar {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  background: var(--brand-gradient-strong);
  color: #ffffff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}

.preview-profile strong,
.preview-profile small {
  display: block;
  line-height: 1.2;
}

.preview-profile strong {
  font-size: 18px;
  font-weight: 900;
}

.preview-profile small {
  margin-top: 4px;
  color: var(--text-secondary);
  font-weight: 800;
}

.preview-line {
  position: absolute;
  right: 18px;
  bottom: 31px;
  left: 18px;
  height: 9px;
  border-radius: 999px;
  background: linear-gradient(90deg, rgba(32, 214, 160, 0.16), rgba(40, 124, 255, 0.18));
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.64) inset;
}

.preview-chip {
  position: absolute;
  bottom: 18px;
  min-width: 70px;
  min-height: 36px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  border-radius: 8px;
  color: var(--text-main);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 12px 24px rgba(40, 124, 255, 0.13);
  animation: chipFloat 4.8s ease-in-out infinite;
}

.chip-mint {
  left: 28px;
  background: linear-gradient(135deg, #e9fff8 0%, #d7fff2 100%);
}

.chip-blue {
  left: 120px;
  background: linear-gradient(135deg, #eaf4ff 0%, #d8e9ff 100%);
  animation-delay: -1.6s;
}

.chip-rose {
  left: 212px;
  background: linear-gradient(135deg, #fff1f7 0%, #ffddec 100%);
  animation-delay: -3.2s;
}

.register-panel {
  position: relative;
  overflow: hidden;
  padding: 28px 42px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 251, 255, 0.94)),
    #ffffff;
  isolation: isolate;
}

.register-panel::before,
.register-panel::after {
  position: absolute;
  content: "";
  pointer-events: none;
  z-index: 0;
}

.register-panel::before {
  inset: 0;
  background:
    linear-gradient(118deg, transparent 0%, transparent 26%, rgba(95, 230, 189, 0.14) 38%, rgba(137, 199, 255, 0.12) 48%, transparent 62%),
    linear-gradient(250deg, transparent 8%, rgba(255, 185, 214, 0.16) 34%, transparent 58%);
  animation: panelSheen 8s ease-in-out infinite alternate;
}

.register-panel::after {
  inset: 14px;
  border: 1px solid rgba(137, 199, 255, 0.25);
  border-radius: 8px;
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.78) inset,
    0 18px 42px rgba(40, 124, 255, 0.08);
}

.panel-head,
.register-form,
.auth-switch {
  position: relative;
  z-index: 1;
}

.panel-head {
  margin-bottom: 18px;
}

.panel-kicker {
  color: var(--brand-primary);
  font-size: 13px;
  font-weight: 900;
}

.panel-head h2 {
  margin: 9px 0 8px;
  color: var(--text-main);
  font-size: 31px;
  line-height: 1.15;
  letter-spacing: 0;
}

.panel-head p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 15px;
  line-height: 1.65;
  font-weight: 700;
}

.register-form {
  display: grid;
  gap: 2px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 10px;
}

.register-form :deep(.el-form-item__label) {
  color: var(--text-main);
  font-weight: 900;
}

.register-form :deep(.el-input__wrapper) {
  min-height: 46px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 0 0 1px var(--line-soft) inset;
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}

.register-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(60, 146, 255, 0.38) inset;
}

.register-form :deep(.el-input__wrapper.is-focus) {
  transform: translateY(-1px);
  box-shadow:
    0 0 0 1px var(--brand-primary) inset,
    0 12px 24px rgba(137, 199, 255, 0.18);
}

.register-form :deep(.el-input__prefix) {
  color: var(--brand-primary);
}

.register-submit {
  width: 100%;
  height: 50px;
  border: 0;
  border-radius: 8px;
  background: var(--brand-gradient-strong);
  box-shadow: 0 16px 30px rgba(40, 124, 255, 0.22);
  color: #ffffff;
  display: inline-flex;
  gap: 8px;
  font-size: 16px;
  font-weight: 900;
  overflow: hidden;
  position: relative;
}

.register-submit::before {
  position: absolute;
  inset: 0;
  content: "";
  background: linear-gradient(110deg, transparent 0%, rgba(255, 255, 255, 0.35) 44%, transparent 64%);
  transform: translateX(-120%);
  transition: transform 0.5s ease;
}

.register-submit:hover::before {
  transform: translateX(120%);
}

.auth-switch {
  margin-top: 16px;
  border-top: 1px solid var(--line-soft);
  padding-top: 14px;
  display: flex;
  justify-content: center;
  gap: 8px;
  color: var(--text-secondary);
  font-weight: 700;
}

.auth-switch button {
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--brand-primary);
  cursor: pointer;
  font-weight: 900;
}

.auth-switch button:hover {
  color: var(--brand-primary-dark);
}

@keyframes laneDrift {
  from {
    transform: translate3d(-18px, -8px, 0) rotate(-11deg);
  }

  to {
    transform: translate3d(18px, 10px, 0) rotate(-9deg);
  }
}

@keyframes surfaceFlow {
  from {
    opacity: 0.72;
    transform: translateX(-18px);
  }

  to {
    opacity: 1;
    transform: translateX(18px);
  }
}

@keyframes panelSheen {
  from {
    opacity: 0.72;
    transform: translateX(-16px);
  }

  to {
    opacity: 1;
    transform: translateX(16px);
  }
}

@keyframes previewSweep {

  0%,
  32% {
    opacity: 0;
    transform: translateX(0) skewX(-13deg);
  }

  52% {
    opacity: 1;
  }

  100% {
    opacity: 0;
    transform: translateX(420%) skewX(-13deg);
  }
}

@keyframes chipFloat {

  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-5px);
  }
}

@media (max-width: 980px) {
  .register-stage {
    grid-template-columns: 1fr;
  }

  .showcase-panel {
    min-height: 430px;
  }

  .register-panel {
    padding: 34px;
  }
}

@media (max-width: 680px) {
  .register-commerce {
    padding: 14px;
  }

  .register-top {
    margin-bottom: 12px;
  }

  .brand-link {
    width: 148px;
    height: 50px;
  }

  .showcase-panel {
    min-height: unset;
    padding: 22px;
  }

  .register-panel {
    order: -1;
    padding: 24px 18px;
  }

  .showcase-copy h1 {
    font-size: 34px;
  }

  .feature-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .feature-card {
    min-height: 96px;
  }

  .account-preview {
    min-height: 116px;
  }

  .preview-chip {
    min-width: 62px;
  }

  .chip-blue {
    left: 100px;
  }

  .chip-rose {
    left: 172px;
  }

  .panel-head h2 {
    font-size: 28px;
  }

  .auth-switch {
    flex-wrap: wrap;
  }
}
</style>
