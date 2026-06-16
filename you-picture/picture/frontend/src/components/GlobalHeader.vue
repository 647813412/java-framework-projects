<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  UserOutlined,
  LogoutOutlined,
  HomeOutlined,
  UploadOutlined,
  TeamOutlined,
  PictureOutlined,
  SearchOutlined,
  BulbOutlined,
  CloudOutlined,
  RobotOutlined,
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()

const currentPath = computed(() => route.path)

// ===== 导航 =====
function goHome() {
  router.push('/')
}

function navigateTo(path: string) {
  router.push(path)
}

// ===== 用户菜单 =====
function handleMenuClick({ key }: { key: string }) {
  switch (key) {
    case 'profile':
      router.push('/profile')
      break
    case 'theme':
      themeStore.toggle()
      break
    case 'logout':
      handleLogout()
      break
  }
}

// ===== 退出登录 =====
async function handleLogout() {
  await userStore.logout()
  router.push('/')
}
</script>

<template>
  <header class="global-header">
    <div class="header-inner">
      <!-- 左侧：品牌 Logo -->
      <div class="header-brand" @click="goHome">
        <span class="brand-icon">📷</span>
        <span class="brand-text">拾光图库</span>
      </div>

      <!-- 中间：导航链接 -->
      <nav class="header-nav">
        <a
          class="nav-link"
          :class="{ active: currentPath === '/' }"
          @click.prevent="navigateTo('/')"
        >
          <HomeOutlined />
          <span>主页</span>
        </a>
        <a
          v-if="userStore.isLoggedIn"
          class="nav-link"
          :class="{ active: currentPath === '/picture/upload' }"
          @click.prevent="navigateTo('/picture/upload')"
        >
          <UploadOutlined />
          <span>上传</span>
        </a>
        <a
          v-if="userStore.isLoggedIn"
          class="nav-link"
          :class="{ active: currentPath === '/ai/image' }"
          @click.prevent="navigateTo('/ai/image')"
        >
          <RobotOutlined />
          <span>AI 创作</span>
        </a>
        <a
          v-if="userStore.isLoggedIn"
          class="nav-link"
          :class="{ active: currentPath === '/team-spaces' }"
          @click.prevent="navigateTo('/team-spaces')"
        >
          <TeamOutlined />
          <span>团队空间</span>
        </a>
        <a
          class="nav-link"
          :class="{ active: currentPath === '/search' }"
          @click.prevent="navigateTo('/search')"
        >
          <SearchOutlined />
          <span>搜索</span>
        </a>
        <a
          v-if="userStore.isAdmin"
          class="nav-link"
          :class="{ active: currentPath === '/admin/user-manage' }"
          @click.prevent="navigateTo('/admin/user-manage')"
        >
          <TeamOutlined />
          <span>用户管理</span>
        </a>
        <a
          v-if="userStore.isAdmin"
          class="nav-link"
          :class="{ active: currentPath === '/admin/picture-manage' }"
          @click.prevent="navigateTo('/admin/picture-manage')"
        >
          <PictureOutlined />
          <span>图片管理</span>
        </a>
        <a
          v-if="userStore.isAdmin"
          class="nav-link"
          :class="{ active: currentPath === '/admin/space-manage' }"
          @click.prevent="navigateTo('/admin/space-manage')"
        >
          <CloudOutlined />
          <span>空间管理</span>
        </a>
      </nav>

      <!-- 右侧：用户 -->
      <div class="header-right">

        <!-- 已登录：用户头像下拉 -->
        <a-dropdown v-if="userStore.isLoggedIn" :trigger="['click']">
          <div class="user-trigger">
            <a-avatar
              :size="34"
              :src="userStore.loginUser?.userAvatar"
              style="background-color: var(--color-surface-sand); color: var(--color-text-primary)"
            >
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <span class="user-name">{{ userStore.loginUser?.userName || '用户' }}</span>
          </div>
          <template #overlay>
            <a-menu @click="handleMenuClick">
              <a-menu-item key="profile">
                <UserOutlined />
                <span style="margin-left: 8px">个人中心</span>
              </a-menu-item>
              <a-menu-item key="theme">
                <BulbOutlined />
                <span style="margin-left: 8px">主题切换</span>
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout" class="logout-item">
                <LogoutOutlined />
                <span style="margin-left: 8px">退出登录</span>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>

        <!-- 未登录：登录按钮 -->
        <a-button v-else type="primary" @click="router.push('/login')">
          登录
        </a-button>
      </div>
    </div>
  </header>

</template>

<style scoped>
.global-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(229, 229, 224, 0.6);
  box-shadow: 0 1px 8px rgba(33, 25, 34, 0.04);
}

.header-inner {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 var(--space-lg);
  height: 64px;
  display: flex;
  align-items: center;
  gap: var(--space-lg);
}

/* ===== 品牌区 ===== */
.header-brand {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
  transition: opacity var(--transition-fast);
}

.header-brand:hover {
  opacity: 0.8;
}

.brand-icon {
  font-size: 26px;
}

.brand-text {
  font-family: var(--font-display);
  font-size: 1.35rem;
  font-weight: var(--weight-bold);
  color: var(--color-text-primary);
  letter-spacing: 0.06em;
}

/* ===== 导航区 ===== */
.header-nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: var(--radius-button);
  font-size: 14px;
  font-weight: var(--weight-medium);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  text-decoration: none;
  white-space: nowrap;
  user-select: none;
}

.nav-link:hover {
  color: var(--color-text-primary);
  background: rgba(33, 25, 34, 0.04);
}

.nav-link.active {
  color: var(--color-primary);
  background: rgba(230, 0, 35, 0.06);
}

/* ===== 右侧区 ===== */
.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  margin-left: auto;
  flex-shrink: 0;
}

/* ===== 用户触发区 ===== */
.user-trigger {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  cursor: pointer;
  padding: 4px 10px;
  border-radius: var(--radius-button);
  transition: background var(--transition-fast);
}

.user-trigger:hover {
  background: rgba(33, 25, 34, 0.04);
}

.user-name {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: var(--weight-medium);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 下拉菜单退出项 ===== */
.logout-item {
  color: var(--color-error);
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .header-inner {
    padding: 0 var(--space-md);
    gap: var(--space-sm);
  }

  .brand-text {
    display: none;
  }

  .header-nav {
    gap: 2px;
  }

  .nav-link span {
    display: none;
  }

  .nav-link {
    padding: 8px 10px;
  }

  .user-name {
    display: none;
  }
}
</style>
