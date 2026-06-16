<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  UserOutlined,
  LogoutOutlined,
  HomeOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  TeamOutlined,
  PictureOutlined,
  CloudOutlined,
  RobotOutlined,
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// ===== 面板收缩 =====
const collapsed = ref(false)

// ===== 拖拽 =====
const panelY = ref(100)
const dragging = ref(false)
const dragStartY = ref(0)
const dragStartPanelY = ref(0)

function onDragStart(e: MouseEvent) {
  dragging.value = true
  dragStartY.value = e.clientY
  dragStartPanelY.value = panelY.value
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
}

function onDragMove(e: MouseEvent) {
  if (!dragging.value) return
  const dy = e.clientY - dragStartY.value
  const newY = dragStartPanelY.value + dy
  panelY.value = Math.max(20, Math.min(window.innerHeight - 200, newY))
}

function onDragEnd() {
  dragging.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
}

// ===== 导航 =====
const currentPath = computed(() => route.path)

function handleUserClick() {
  if (userStore.isLoggedIn) {
    router.push('/profile')
  } else {
    router.push('/login')
  }
}

function navigateTo(path: string) {
  router.push(path)
}

// ===== 隐藏管理入口 — 5次点击 Logo =====
const clickCount = ref(0)
let clickTimer: ReturnType<typeof setTimeout> | null = null

function handleLogoClick() {
  clickCount.value++
  if (clickTimer) clearTimeout(clickTimer)
  clickTimer = setTimeout(() => {
    clickCount.value = 0
  }, 2000)
  if (clickCount.value >= 5) {
    clickCount.value = 0
    if (userStore.isAdmin) {
      router.push('/admin/user-manage')
    }
  }
}

// ===== 退出登录 =====
async function handleLogout() {
  await userStore.logout()
  router.push('/')
}
</script>

<template>
  <!-- 收缩时的浮动按钮 -->
  <div
    v-if="collapsed"
    class="panel-toggle-btn"
    :style="{ top: panelY + 'px' }"
    @click="collapsed = false"
  >
    <MenuUnfoldOutlined />
  </div>

  <!-- 侧边面板 -->
  <Transition name="panel-slide">
    <div
      v-show="!collapsed"
      class="side-panel"
      :class="{ 'is-dragging': dragging }"
      :style="{ top: panelY + 'px' }"
    >
      <!-- 拖拽把手 -->
      <div class="panel-drag-handle" @mousedown.prevent="onDragStart">
        <div class="drag-dots">
          <span></span><span></span><span></span>
        </div>
      </div>

      <!-- Logo -->
      <div class="panel-logo" @click="handleLogoClick">
        <span class="logo-icon">📷</span>
      </div>

      <!-- 导航项目 -->
      <div class="nav-section">
        <!-- 第一栏：用户 -->
        <a-tooltip placement="right" :title="userStore.isLoggedIn ? (userStore.loginUser?.userName || '个人主页') : '登录 / 注册'">
          <div class="nav-item" @click="handleUserClick">
            <a-avatar
              v-if="userStore.isLoggedIn && userStore.loginUser?.userAvatar"
              :size="32"
              :src="userStore.loginUser.userAvatar"
            />
            <a-avatar
              v-else-if="userStore.isLoggedIn"
              :size="32"
              style="background: var(--color-primary); font-size: 14px"
            >
              {{ (userStore.loginUser?.userName || '?')[0] }}
            </a-avatar>
            <div v-else class="nav-icon">
              <UserOutlined />
            </div>
          </div>
        </a-tooltip>

        <div class="nav-divider"></div>

        <!-- 第二栏：首页 -->
        <a-tooltip placement="right" title="图库首页">
          <div
            class="nav-item"
            :class="{ active: currentPath === '/' }"
            @click="navigateTo('/')"
          >
            <div class="nav-icon">
              <HomeOutlined />
            </div>
          </div>
        </a-tooltip>

        <!-- 管理员：用户管理 -->
        <a-tooltip v-if="userStore.isAdmin" placement="right" title="用户管理">
          <div
            class="nav-item"
            :class="{ active: currentPath === '/admin/user-manage' }"
            @click="navigateTo('/admin/user-manage')"
          >
            <div class="nav-icon">
              <TeamOutlined />
            </div>
          </div>
        </a-tooltip>

        <!-- 管理员：图片管理 -->
        <a-tooltip v-if="userStore.isAdmin" placement="right" title="图片管理">
          <div
            class="nav-item"
            :class="{ active: currentPath === '/admin/picture-manage' }"
            @click="navigateTo('/admin/picture-manage')"
          >
            <div class="nav-icon">
              <PictureOutlined />
            </div>
          </div>
        </a-tooltip>

        <!-- 管理员：空间管理 -->
        <a-tooltip v-if="userStore.isAdmin" placement="right" title="空间管理">
          <div
            class="nav-item"
            :class="{ active: currentPath === '/admin/space-manage' }"
            @click="navigateTo('/admin/space-manage')"
          >
            <div class="nav-icon">
              <CloudOutlined />
            </div>
          </div>
        </a-tooltip>

        <!-- AI 创作 -->
        <a-tooltip v-if="userStore.isLoggedIn" placement="right" title="AI 创作">
          <div
            class="nav-item"
            :class="{ active: currentPath === '/ai/image' }"
            @click="navigateTo('/ai/image')"
          >
            <div class="nav-icon">
              <RobotOutlined />
            </div>
          </div>
        </a-tooltip>

        <!-- 团队空间 -->
        <a-tooltip v-if="userStore.isLoggedIn" placement="right" title="团队空间">
          <div
            class="nav-item"
            :class="{ active: currentPath === '/team-spaces' }"
            @click="navigateTo('/team-spaces')"
          >
            <div class="nav-icon">
              <TeamOutlined />
            </div>
          </div>
        </a-tooltip>
      </div>

      <!-- 底部 -->
      <div class="panel-bottom">
        <!-- 退出登录 -->
        <a-tooltip v-if="userStore.isLoggedIn" placement="right" title="退出登录">
          <div class="nav-item logout-item" @click="handleLogout">
            <div class="nav-icon">
              <LogoutOutlined />
            </div>
          </div>
        </a-tooltip>

        <!-- 收缩按钮 -->
        <a-tooltip placement="right" title="收起侧栏">
          <div class="nav-item collapse-item" @click="collapsed = true">
            <div class="nav-icon">
              <MenuFoldOutlined />
            </div>
          </div>
        </a-tooltip>
      </div>
    </div>
  </Transition>

</template>

<style scoped>
/* ===== 收缩按钮 ===== */
.panel-toggle-btn {
  position: fixed;
  left: 0;
  z-index: 1000;
  width: 36px;
  height: 48px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border-radius: 0 var(--radius-button) var(--radius-button) 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 2px 0 12px rgba(33, 25, 34, 0.1);
  border: 1px solid var(--color-surface-sand);
  border-left: none;
  color: var(--color-text-secondary);
  font-size: 16px;
  transition: all var(--transition-fast);
}

.panel-toggle-btn:hover {
  width: 42px;
  color: var(--color-primary);
  background: var(--color-bg);
}

/* ===== 侧边面板 ===== */
.side-panel {
  position: fixed;
  left: 16px;
  z-index: 999;
  width: 56px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(16px);
  border-radius: var(--radius-card);
  box-shadow: 0 4px 24px rgba(33, 25, 34, 0.1), 0 0 0 1px rgba(229, 229, 224, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: var(--space-sm);
  user-select: none;
  transition: box-shadow var(--transition-normal);
}

.side-panel.is-dragging {
  box-shadow: 0 8px 32px rgba(33, 25, 34, 0.18);
  cursor: grabbing;
}

/* ===== 拖拽把手 ===== */
.panel-drag-handle {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 6px 0 2px;
  cursor: grab;
}

.panel-drag-handle:active {
  cursor: grabbing;
}

.drag-dots {
  display: flex;
  gap: 3px;
}

.drag-dots span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--color-border-hover);
}

/* ===== Logo ===== */
.panel-logo {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin-bottom: var(--space-xs);
  font-size: 22px;
  user-select: none;
}

/* ===== 导航 ===== */
.nav-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 100%;
  padding: 0 var(--space-sm);
}

.nav-divider {
  width: 24px;
  height: 1px;
  background: var(--color-surface-sand);
  margin: 4px 0;
}

.nav-item {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.nav-item:hover {
  background: var(--color-bg-warm);
}

.nav-item.active {
  background: rgba(230, 0, 35, 0.08);
}

.nav-item.active .nav-icon {
  color: var(--color-primary);
}

.nav-icon {
  font-size: 18px;
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color var(--transition-fast);
}

.nav-item:hover .nav-icon {
  color: var(--color-text-primary);
}

/* ===== 底部 ===== */
.panel-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  width: 100%;
  padding: 0 var(--space-sm);
  margin-top: var(--space-sm);
  border-top: 1px solid var(--color-surface-sand);
  padding-top: var(--space-sm);
}

.logout-item:hover {
  background: rgba(158, 10, 10, 0.06) !important;
}

.logout-item:hover .nav-icon {
  color: var(--color-error) !important;
}

.collapse-item:hover .nav-icon {
  color: var(--color-text-primary);
}

/* ===== 过渡动画 ===== */
.panel-slide-enter-active,
.panel-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.panel-slide-enter-from,
.panel-slide-leave-to {
  opacity: 0;
  transform: translateX(-40px);
}
</style>
