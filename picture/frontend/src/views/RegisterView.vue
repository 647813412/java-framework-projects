<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, SafetyOutlined, SmileOutlined } from '@ant-design/icons-vue'
import { useRegisterUsingPost } from '@/api/userController'
import type { Rule } from 'ant-design-vue/es/form'

const router = useRouter()
const loading = ref(false)

const formState = reactive({
  userName: '',
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const rules: Record<string, Rule[]> = {
  userName: [
    { required: true, message: '请输入用户名' },
    { min: 2, max: 10, message: '用户名 2~10 个字符' },
  ],
  userAccount: [
    { required: true, message: '请输入账号' },
    { min: 4, max: 10, message: '账号 4~10 位' },
  ],
  userPassword: [
    { required: true, message: '请输入密码' },
    {
      validator: async (_rule: Rule, value: string) => {
        if (value && !/^[a-zA-Z0-9]{6,8}$/.test(value)) {
          return Promise.reject('密码为 6~8 位英文或数字')
        }
        return Promise.resolve()
      },
    },
  ],
  checkPassword: [
    { required: true, message: '请确认密码' },
    {
      validator: async (_rule: Rule, value: string) => {
        if (value && value !== formState.userPassword) {
          return Promise.reject('两次密码不一致')
        }
        return Promise.resolve()
      },
    },
  ],
}

async function handleRegister() {
  loading.value = true
  try {
    const res: any = await useRegisterUsingPost(formState)
    if (res.data) {
      message.success('注册成功！请登录')
      router.push('/login')
    }
  } catch {
    // 拦截器已处理错误提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <!-- 背景装饰 -->
    <div class="register-bg">
      <div class="bg-ink-wash"></div>
      <div class="bg-overlay"></div>
    </div>

    <!-- 注册卡片 -->
    <div class="register-card">
      <div class="card-header">
        <h1 class="card-title">加入拾光</h1>
        <p class="card-subtitle">— 记录你的每一帧美好 —</p>
      </div>

      <a-form
        :model="formState"
        :rules="rules"
        layout="vertical"
        @finish="handleRegister"
        class="register-form"
      >
        <a-form-item name="userName">
          <a-input
            v-model:value="formState.userName"
            placeholder="请输入用户名（2~10个字符）"
            size="large"
            :maxlength="10"
          >
            <template #prefix>
              <SmileOutlined style="color: var(--color-text-disabled)" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item name="userAccount">
          <a-input
            v-model:value="formState.userAccount"
            placeholder="请输入账号（4~10位）"
            size="large"
            :maxlength="10"
          >
            <template #prefix>
              <UserOutlined style="color: var(--color-text-disabled)" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item name="userPassword">
          <a-input-password
            v-model:value="formState.userPassword"
            placeholder="请输入密码（6~8位英文或数字）"
            size="large"
            :maxlength="8"
          >
            <template #prefix>
              <LockOutlined style="color: var(--color-text-disabled)" />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item name="checkPassword">
          <a-input-password
            v-model:value="formState.checkPassword"
            placeholder="请再次输入密码"
            size="large"
            :maxlength="8"
          >
            <template #prefix>
              <SafetyOutlined style="color: var(--color-text-disabled)" />
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
            class="register-btn"
          >
            注册
          </a-button>
        </a-form-item>
      </a-form>

      <div class="card-footer">
        <span class="footer-text">已有账号？</span>
        <router-link to="/login" class="footer-link">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

/* ===== 水墨背景 ===== */
.register-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.bg-ink-wash {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 80% 80%, rgba(230, 0, 35, 0.04) 0%, transparent 50%),
    radial-gradient(ellipse at 20% 20%, rgba(16, 60, 37, 0.05) 0%, transparent 50%),
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
}

/* ===== 注册卡片 ===== */
.register-card {
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
.register-form {
  margin-top: var(--space-lg);
}

.register-btn {
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

@media (max-width: 480px) {
  .register-card {
    margin: var(--space-md);
    padding: var(--space-xl) var(--space-lg);
  }
}
</style>
