<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { userLoginUsingPost } from '@/api/userController'
import { useUserStore } from '@/stores/user'
import type { Rule } from 'ant-design-vue/es/form'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)

const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const rules: Record<string, Rule[]> = {
  userAccount: [
    { required: true, message: '请输入账号' },
    { min: 4, message: '账号不少于4位' },
  ],
  userPassword: [
    { required: true, message: '请输入密码' },
    { min: 8, message: '密码不少于8位' },
  ],
}

async function handleLogin() {
  loading.value = true
  try {
    const res: any = await userLoginUsingPost(formState)
    if (res.data) {
      userStore.setLoginUser(res.data)
      message.success('登录成功，欢迎回来！')
      const redirect = (route.query.redirect as string) || '/'
      router.push(redirect)
    }
  } catch {
    // 拦截器已处理错误提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="login-bg">
      <div class="bg-ink-wash"></div>
      <div class="bg-overlay"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-card">
      <div class="card-header">
        <h1 class="card-title">拾光图库</h1>
        <p class="card-subtitle">— 以光影之名，拾时光之美 —</p>
      </div>

      <a-form
        :model="formState"
        :rules="rules"
        layout="vertical"
        @finish="handleLogin"
        class="login-form"
      >
        <a-form-item name="userAccount">
          <a-input
            v-model:value="formState.userAccount"
            placeholder="请输入账号"
            size="large"
            :maxlength="20"
          >
            <template #prefix>
              <UserOutlined style="color: var(--color-text-disabled)" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item name="userPassword">
          <a-input-password
            v-model:value="formState.userPassword"
            placeholder="请输入密码"
            size="large"
            :maxlength="20"
          >
            <template #prefix>
              <LockOutlined style="color: var(--color-text-disabled)" />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            :loading="loading"
            block
            size="large"
            class="login-btn"
          >
            登录
          </a-button>
        </a-form-item>
      </a-form>

      <div class="card-footer">
        <span class="footer-text">还没有账号？</span>
        <router-link to="/register" class="footer-link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

/* ===== 水墨背景 ===== */
.login-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.bg-ink-wash {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 20% 80%, rgba(230, 0, 35, 0.05) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 20%, rgba(16, 60, 37, 0.04) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(33, 25, 34, 0.02) 0%, transparent 70%),
    linear-gradient(180deg, #f6f6f3 0%, #e5e5e0 50%, #f6f6f3 100%);
}

.bg-overlay {
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      0deg,
      transparent,
      transparent 2px,
      rgba(33, 25, 34, 0.008) 2px,
      rgba(33, 25, 34, 0.008) 4px
    );
  /* 宣纸纹理 */
}

/* ===== 登录卡片 ===== */
.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: var(--space-2xl) var(--space-xl);
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  border: 1px solid rgba(229, 229, 224, 0.6);
}

.card-header {
  text-align: center;
  margin-bottom: var(--space-xl);
}

.card-title {
  font-family: var(--font-display);
  font-size: 2rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-sm);
  letter-spacing: 0.1em;
}

.card-subtitle {
  font-family: var(--font-display);
  font-size: 0.88rem;
  color: var(--color-text-secondary);
  letter-spacing: 0.15em;
}

/* ===== 表单 ===== */
.login-form {
  margin-top: var(--space-lg);
}

.login-btn {
  height: 44px;
  font-size: 16px;
  font-weight: var(--weight-semibold);
  letter-spacing: 0.05em;
}

/* ===== 底部 ===== */
.card-footer {
  text-align: center;
  margin-top: var(--space-md);
}

.footer-text {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.footer-link {
  font-size: 14px;
  color: var(--color-primary);
  font-weight: var(--weight-medium);
  margin-left: 4px;
}

.footer-link:hover {
  text-decoration: underline;
}

/* ===== 响应式 ===== */
@media (max-width: 480px) {
  .login-card {
    margin: var(--space-md);
    padding: var(--space-xl) var(--space-lg);
  }
}
</style>
