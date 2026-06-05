<template>
  <div class="auth-page">
    <header class="auth-top">
      <button class="brand-link" type="button" @click="router.push('/')">
        <img :src="logoUrl" alt="Kinda Goods" />
      </button>
    </header>

    <main class="auth-layout fade-in-up">
      <section class="auth-copy" aria-labelledby="auth-title">
        <p class="auth-eyebrow">Kinda Goods</p>
        <h1 id="auth-title">欢迎回来</h1>
        <p class="auth-lede">Kinda Goods 是面向校园用户的综合交易平台，支持新品选购、闲置转让、卖家入驻与订单售后。</p>
        <div class="gradient-line" aria-hidden="true"></div>
      </section>

      <section class="login-box" aria-labelledby="login-title">
        <div class="login-head">
          <img class="brand-mark" :src="logoUrl" alt="Kinda Goods" />
          <h2 id="login-title">登录 Kinda Goods</h2>
        </div>

        <el-form class="login-form" :model="form" label-position="top" @submit.prevent @keyup.enter="handleLogin">
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="请输入账号" size="large">
              <template #prefix>
                <el-icon>
                  <User />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large">
              <template #prefix>
                <el-icon>
                  <Lock />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-button class="login-submit" type="primary" :loading="loading" @click="handleLogin">
            <span>登录</span>
            <el-icon>
              <ArrowRight />
            </el-icon>
          </el-button>
        </el-form>

        <div class="auth-switch">
          <span>还没有账号？</span>
          <button type="button" @click="router.push('/register')">创建账号</button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { ArrowRight, Lock, User } from "@element-plus/icons-vue";
import { useRoute, useRouter } from "vue-router";
import { useUserStore } from "@/stores/user";
import logoUrl from "@/assets/kinda-goods-logo.svg";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const loading = ref(false);

const form = reactive({
  username: "user",
  password: "user123",
});

const redirectPath = computed(() => {
  const value = route.query.redirect;
  if (typeof value !== "string" || !value.startsWith("/") || value.startsWith("//") || value === "/login") {
    return "";
  }
  return value;
});

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning("请填写完整账号密码");
    return;
  }
  loading.value = true;
  try {
    const userInfo = await userStore.login(form);
    ElMessage.success("登录成功");
    if (redirectPath.value) {
      router.push(redirectPath.value);
      return;
    }
    if (userInfo.role === "ADMIN") {
      router.push("/admin");
      return;
    }
    if (userInfo.role === "OFFICIAL_SELLER" || userInfo.role === "SELLER") {
      router.push("/merchant");
      return;
    }
    router.push("/");
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 26px;
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(120deg, rgba(95, 230, 189, 0.12), rgba(137, 199, 255, 0.12) 44%, rgba(255, 185, 214, 0.1)),
    linear-gradient(180deg, #fbfffd 0%, #f6fbff 54%, #fff9fc 100%);
}

.auth-page::before {
  content: "";
  position: absolute;
  inset: -26%;
  background: conic-gradient(from 150deg at 50% 48%,
      rgba(95, 230, 189, 0.16),
      rgba(137, 199, 255, 0.18),
      rgba(255, 185, 214, 0.13),
      rgba(95, 230, 189, 0.16));
  filter: blur(52px);
  opacity: 0.75;
  pointer-events: none;
  transform: translate3d(0, 0, 0);
}

.auth-page::after {
  content: "";
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(18, 50, 65, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(18, 50, 65, 0.035) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: linear-gradient(180deg, transparent, #000 24%, #000 72%, transparent);
  pointer-events: none;
}

@media (prefers-reduced-motion: no-preference) {
  .auth-page::before {
    animation: auth-gradient-drift 18s ease-in-out infinite alternate;
  }
}

@keyframes auth-gradient-drift {
  from {
    transform: translate3d(-1.5%, -1%, 0) rotate(-2deg);
  }

  to {
    transform: translate3d(1.5%, 1%, 0) rotate(2deg);
  }
}

.auth-top {
  width: min(1040px, 100%);
  min-height: 56px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
}

.brand-link {
  border: 0;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
}

.brand-link {
  width: 158px;
  height: 56px;
  padding: 4px 0;
}

.brand-link img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: contain;
  object-position: left center;
}

.auth-layout {
  width: min(980px, 100%);
  min-height: calc(100vh - 116px);
  margin: 0 auto;
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 396px;
  align-items: center;
  gap: clamp(52px, 9vw, 112px);
}

.auth-copy {
  min-width: 0;
  padding-bottom: 26px;
}

.auth-eyebrow {
  margin: 0;
  color: var(--brand-primary);
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 0;
}

.auth-copy h1 {
  margin: 18px 0 18px;
  color: var(--text-main);
  font-size: clamp(48px, 6vw, 70px);
  line-height: 1.06;
  letter-spacing: 0;
}

.auth-lede {
  max-width: 430px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 18px;
  line-height: 1.75;
  font-weight: 650;
}

.gradient-line {
  width: min(360px, 68%);
  height: 4px;
  margin-top: 34px;
  border-radius: 999px;
  background: var(--brand-gradient);
  box-shadow: 0 12px 26px rgba(40, 124, 255, 0.16);
}

.login-box {
  width: 100%;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(137, 199, 255, 0.34);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.91);
  padding: 36px 34px 30px;
  box-shadow:
    0 28px 70px rgba(40, 124, 255, 0.12),
    0 2px 0 rgba(255, 255, 255, 0.9) inset;
  backdrop-filter: blur(18px);
}

.login-box::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: 4px;
  background: var(--brand-gradient);
}

.login-head {
  margin-bottom: 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.brand-mark {
  width: 96px;
  height: 52px;
  margin-bottom: 20px;
  display: block;
  object-fit: contain;
}

.login-head h2 {
  margin: 0;
  color: var(--text-main);
  font-size: 26px;
  line-height: 1.22;
  letter-spacing: 0;
}

.login-form {
  display: grid;
  gap: 8px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.login-form :deep(.el-form-item__label) {
  color: var(--text-main);
  font-weight: 800;
  line-height: 1.2;
  padding-bottom: 9px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 52px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 0 0 1px var(--line-soft) inset;
  transition:
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(60, 146, 255, 0.38) inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--brand-primary) inset,
    0 10px 22px rgba(137, 199, 255, 0.16);
}

.login-form :deep(.el-input__prefix) {
  color: var(--brand-primary);
}

.login-submit {
  width: 100%;
  height: 52px;
  margin-top: 2px;
  border: 0;
  border-radius: 8px;
  background: var(--brand-gradient-strong);
  box-shadow: 0 16px 32px rgba(40, 124, 255, 0.2);
  color: #ffffff;
  display: inline-flex;
  gap: 8px;
  font-size: 16px;
  font-weight: 900;
  transition:
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.login-submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 18px 36px rgba(40, 124, 255, 0.24);
}

.auth-switch {
  margin-top: 24px;
  border-top: 1px solid var(--line-soft);
  padding-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-secondary);
  font-weight: 700;
}

.auth-switch button {
  border: 0;
  border-radius: 6px;
  padding: 6px 0;
  background: transparent;
  color: var(--brand-primary);
  cursor: pointer;
  font-weight: 900;
}

.auth-switch button:hover {
  color: var(--brand-primary-dark);
}

@media (max-width: 860px) {
  .auth-layout {
    min-height: auto;
    padding: 42px 0 28px;
    grid-template-columns: 1fr;
    gap: 30px;
  }

  .auth-copy {
    order: 2;
    padding-bottom: 0;
    text-align: center;
  }

  .login-box {
    order: 1;
    max-width: 440px;
    margin: 0 auto;
  }

  .auth-copy h1 {
    font-size: 44px;
  }

  .auth-lede {
    max-width: 520px;
    margin: 0 auto;
  }

  .gradient-line {
    margin-inline: auto;
  }
}

@media (max-width: 560px) {
  .auth-page {
    padding: 16px;
  }

  .auth-top {
    min-height: 50px;
  }

  .brand-link {
    width: 142px;
    height: 50px;
  }

  .auth-layout {
    padding-top: 24px;
  }

  .login-box {
    padding: 30px 22px 24px;
  }

  .brand-mark {
    margin-bottom: 15px;
  }

  .login-head h2 {
    font-size: 23px;
  }

  .auth-copy h1 {
    font-size: 38px;
  }

  .auth-lede {
    font-size: 15px;
  }

  .auth-switch {
    justify-content: center;
    flex-wrap: wrap;
  }
}
</style>
