# 图库社区系统 — 前端技术方案

> 版本: v4.2 | 日期: 2026-04-13 | 更新日期: 2026-04-23
> MCP 最新文档验证: ✅ Vue 3 (vuejs/docs) | ✅ Vue Router (router.vuejs.org) | ✅ Pinia (pinia.vuejs.org) | ✅ Ant Design Vue 4 (3x.antdv.com) | ✅ Axios (axios-docs) | ✅ Vite 8 (vitejs/vite)

## 1. 技术选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | ^3.5.32 | 核心框架，使用 `<script setup lang="ts">` + Composition API |
| Vite | ^8.0.4 | 构建工具，开发热更新，开发服务器代理 |
| TypeScript | ~6.0.2 | 类型安全 |
| Vue Router | ^4.6.4 | 前端路由 + 导航守卫 |
| Pinia | ^3.0.4 | 状态管理，使用 Option Store 风格 |
| Ant Design Vue | ^4.2.6 | UI 组件库（Form, Table, Modal, Pagination, Message, Input, Avatar 等） |
| Axios | ^1.15.0 | HTTP 请求，配置请求/响应拦截器 |

### 1.1 选型依据（MCP 最新文档验证 2026-04-13）

#### Vue 3 Composition API（来源: vuejs/docs）

```vue
<script setup>
import { ref, computed, watch, onMounted } from 'vue'

// ref() 创建响应式状态
const count = ref(0)

// computed() 创建计算属性
const doubled = computed(() => count.value * 2)

// watch() 监听变化
watch(count, (newVal, oldVal) => { /* ... */ })

// onMounted() 生命周期钩子
onMounted(() => { console.log('mounted') })
</script>
```

#### Vue Router 4 导航守卫（来源: router.vuejs.org）

```typescript
// beforeEach 全局守卫 — 支持 async + meta 字段
router.beforeEach((to, from) => {
  if (to.meta.requiresAuth && !auth.isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})
```

#### Pinia Option Store（来源: pinia.vuejs.org）

```typescript
// defineStore + Option API 风格 — MCP 确认最新推荐方式
defineStore('counter', {
  state: () => ({ count: 0 }),
  getters: {
    doubleCount: (state) => state.count * 2,
  },
  actions: {
    increment() { this.count++ },
  },
})
```

#### Ant Design Vue 4 表单（来源: 3x.antdv.com）

```vue
<!-- v-model:value 双向绑定, v-model:open 控制弹窗 -->
<a-modal v-model:open="visible" title="编辑" @ok="onOk">
  <a-form ref="formRef" :model="formState" layout="vertical">
    <a-form-item name="name" label="名称" :rules="[{ required: true }]">
      <a-input v-model:value="formState.name" />
    </a-form-item>
  </a-form>
</a-modal>
```

#### Axios 拦截器（来源: axios-docs）

```javascript
// axios.create() + 实例拦截器
const instance = axios.create({ baseURL: '/api', withCredentials: true })
instance.interceptors.request.use((config) => { /* 修改 config */ return config })
instance.interceptors.response.use(
  (response) => { /* 处理成功 */ return response },
  (error) => { /* 处理错误 */ return Promise.reject(error) }
)
```

#### Vite 代理配置（来源: vitejs/vite）

```javascript
// Vite server.proxy — 开发环境反向代理
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

---

## 2. 项目结构

```
frontend/
├── docs/
│   ├── requirements.md        # 需求文档
│   └── tech-design.md         # 技术方案（本文件）
├── public/                    # 静态资源
├── src/
│   ├── api/                   # API 层
│   │   ├── typings.d.ts       # 全局类型声明（declare namespace API）
│   │   ├── index.ts           # 接口注册
│   │   ├── userController.ts  # 用户接口
│   │   ├── pictureController.ts  # 图片接口
│   │   ├── pictureCommentController.ts  # 图片评论接口
│   │   ├── spaceController.ts # 空间接口
│   │   ├── spaceUserController.ts # 空间用户接口
│   │   ├── spaceAnalyzeController.ts # 空间分析接口
│   │   └── fileController.ts  # 文件上传接口
│   ├── assets/
│   │   └── styles/
│   │       ├── variables.css  # CSS 设计令牌
│   │       └── global.css     # 全局样式 + Ant Design 覆盖
│   ├── components/
│   │   ├── GlobalHeader.vue   # 顶部导航栏（品牌、导航、搜索、用户菜单、AI 创作入口）
│   │   ├── AiDialog.vue       # AI 创意助手对话框（弹窗形式）
│   │   └── SidePanel.vue      # 左侧浮动导航面板（可拖拽/收缩）
│   ├── layouts/
│   │   └── BasicLayout.vue    # 基础布局（Content + SidePanel）
│   ├── router/
│   │   └── index.ts           # 路由定义 + 全局守卫
│   ├── stores/
│   │   └── user.ts            # 用户状态（Pinia Option Store）
│   ├── views/
│   │   ├── LoginView.vue      # 登录页
│   │   ├── RegisterView.vue   # 注册页
│   │   ├── HomeView.vue       # 公共图库首页 + 团队空间展示/申请加入
│   │   ├── PictureDetailView.vue  # 图片详情页
│   │   ├── PictureUploadView.vue  # 图片上传页（文件/URL，支持私有空间）
│   │   ├── PictureEditView.vue    # 图片编辑页（元信息编辑）
│   │   ├── SearchView.vue     # 搜索页（关键词/颜色搜索 + 用户/空间搜索 + 历史/热门关键词）
│   │   ├── AiImageView.vue    # AI 创意工坊页（文字生图 + 图片编辑）
│   │   ├── ProfileView.vue    # 个人中心（作品/喜欢/私有图库/团队空间 Tab）
│   │   ├── SpaceDetailView.vue # 团队空间详情页（图片/成员管理/退出团队）
│   │   ├── UserProfileView.vue # 用户主页（他人）
│   │   └── admin/
│   │       ├── UserManageView.vue    # 管理员用户管理
│   │       ├── PictureManageView.vue # 管理员图片管理
│   │       └── SpaceManageView.vue   # 管理员空间管理
│   ├── request.ts             # Axios 实例 + 拦截器
│   ├── App.vue                # 根组件
│   └── main.ts                # 入口（Pinia + Router + Antd 注册）
├── index.html
├── vite.config.ts             # @ 别名 + /api 代理
├── tsconfig.json
├── tsconfig.app.json          # @/* → src/* 路径映射
└── package.json
```

---

## 3. 设计风格

### 3.1 设计令牌（CSS Variables）

基于 DESIGN.md 的古风暖色系，全部通过 CSS 变量管理：

| 令牌 | 值 | 用途 |
|------|------|------|
| `--color-primary` | `#e60023` (朱砂红) | 主操作按钮、品牌强调 |
| `--color-primary-hover` | `#cc001f` | 主色悬浮 |
| `--color-primary-pressed` | `#b3001b` | 主色按下 |
| `--color-bg` | `#ffffff` | 主背景 |
| `--color-bg-warm` | `#f6f6f3` (宣纸白) | 次级背景 |
| `--color-text-primary` | `#211922` (梅色近黑) | 主文本 |
| `--color-text-secondary` | `#62625b` (橄榄灰) | 辅助文本 |
| `--color-text-disabled` | `#91918c` (暖银灰) | 禁用文本 |
| `--color-border` | `#91918c` (暖银) | 输入框边框 |
| `--color-surface-sand` | `#e5e5e0` (砂灰) | 卡片/次级按钮背景 |
| `--color-focus` | `#435ee5` | 聚焦环 |
| `--color-error` | `#9e0a0a` | 错误状态 |
| `--radius-base` | `8px` | 基础圆角 |
| `--radius-button` | `16px` | 按钮/输入框圆角 |
| `--radius-card` | `20px` | 卡片圆角 |
| `--space-*` | `4px ~ 80px` | 标准间距体系 |
| `--shadow-subtle` | `0 1px 3px rgba(33,25,34,0.06)` | 微阴影 |
| `--shadow-card` | `0 2px 8px rgba(33,25,34,0.08)` | 卡片阴影 |
| `--transition-fast` | `150ms ease` | 快速过渡 |
| `--transition-normal` | `250ms ease` | 正常过渡 |

**暗色模式**：`stores/theme.ts` 通过 `document.documentElement.classList.toggle('dark')` 切换暗色主题，`variables.css` 中 `html.dark {}` 选择器覆盖所有色彩令牌，实现完整的暗色体验。

### 3.2 字体

```css
--font-display: "LXGW WenKai", "霞鹜文楷", "Noto Serif SC", serif;  /* 标题 — 古风楷体 */
--font-body: "Pin Sans", -apple-system, system-ui, sans-serif;        /* 正文 — 系统无衬线 */
```

通过 Google Fonts CDN 加载霞鹜文楷：`@import url('https://fonts.googleapis.com/css2?family=LXGW+WenKai:wght@300;400;700&display=swap');`

### 3.3 全局样式覆盖

Ant Design Vue 组件通过全局 CSS 覆盖适配古风主题：

```css
/* 按钮 */
.ant-btn-primary { background: var(--color-primary); border-radius: var(--radius-button); }
.ant-btn-default { background: var(--color-surface-sand); border-radius: var(--radius-button); }
/* 输入框 */
.ant-input { border-radius: var(--radius-button); border-color: var(--color-border); }
.ant-input:focus { border-color: var(--color-focus); box-shadow: 0 0 0 2px rgba(67,94,229,0.15); }
```

---

## 4. 核心设计

### 4.1 Axios 请求封装 (`request.ts`)

**实际实现**（MCP Axios 文档验证）：

```typescript
import axios from 'axios'
import { message } from 'ant-design-vue'

const request = axios.create({
  baseURL: '/api',          // Vite 代理到 localhost:8080
  timeout: 10000,
  withCredentials: true,    // 携带 Cookie（Session 认证）
})

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data.code === 0) return data             // 成功 → 直接返回 data
    if (data.code === 40100) {
      return Promise.reject(new Error('未登录'))  // 40100 静默拒绝，不弹 toast
    }
    message.error(data.message || '请求失败')      // 其他错误 → 全局提示
    return Promise.reject(new Error(data.message))
  },
  (error) => {
    message.error('网络异常，请稍后重试')
    return Promise.reject(error)
  }
)
```

**关键决策**：
- `baseURL: '/api'` — 通过 Vite proxy 解决跨域，非绝对路径
- 40100 **不弹 toast、不强制跳转** — 由路由守卫或业务页面自行决定是否跳转登录
- 统一返回 `data` 层（去掉外层 `response`），业务层直接 `res.data.records` 使用

### 4.2 用户 Store (`stores/user.ts`)

**实际实现**（MCP Pinia 文档验证）：

```typescript
import { defineStore } from 'pinia'
import { getUserLoginUsingGet, userLogoutUsingGet } from '@/api/userController'

export const useUserStore = defineStore('user', {
  state: () => ({
    loginUser: null as API.LoginUserVO | null,
  }),
  getters: {
    isLoggedIn: (state) => !!state.loginUser,
    isAdmin: (state) => state.loginUser?.userRole === 'admin',
  },
  actions: {
    async fetchLoginUser() {
      try {
        const res: any = await getUserLoginUsingGet()
        if (res.data) this.loginUser = res.data
      } catch {
        this.loginUser = null          // 40100 静默失败
      }
    },
    async logout() {
      try {
        await userLogoutUsingGet()
      } finally {
        this.loginUser = null
      }
    },
  },
})
```

### 4.3 路由守卫 (`router/index.ts`)

**实际实现**（MCP Vue Router 文档验证 — `beforeEach` + `meta` 模式）：

```typescript
router.beforeEach(async (to) => {
  const userStore = useUserStore()

  // 仅首次获取登录状态
  if (!userStore.loginUser) {
    await userStore.fetchLoginUser()
  }

  // 管理员页面权限校验
  if (to.meta.requiresAdmin && !userStore.isAdmin) {
    return { path: '/' }
  }

  // 已登录用户不访问 guest 页面
  if (to.meta.guest && userStore.isLoggedIn) {
    return { path: '/' }
  }
})
```

### 4.4 API 层设计

前端 API 层从后端 OpenAPI 生成代码中**适配**而来：

| 差异项 | 后端生成代码 | 前端适配后 |
|--------|-------------|-----------|
| 路径前缀 | `/api/user/login` | `/user/login`（baseURL 已含 `/api`） |
| options 参数 | 有 `options?: {}` | 保留用于扩展 |
| 类型命名空间 | `declare namespace API` | 与后端一致，全局可用 |

### 4.5 管理员隐藏入口

SidePanel Logo 点击计数器：
- 2 秒内连续点击 5 次 → 检查 `userStore.isAdmin` → 跳转 `/admin/user-manage`
- 路由守卫二次校验 `meta.requiresAdmin`

### 4.6 雪花 ID 处理

后端使用雪花算法生成 ID，JavaScript `Number` 最大安全整数为 `2^53 - 1`，雪花 ID 超出此范围。

**解决方案**：
- 路由参数使用 `route.params.id as string`，不 `Number()` 转换
- API 请求中 `id` 参数直接传字符串

### 4.7 图片详情页布局

```
┌─────────────────────────────────────────────────┐
│  flex 容器 (gap: 32px)                           │
│ ┌───────────────────────┐ ┌───────────────────┐ │
│ │  图片面板 (flex: 1)    │ │ 信息面板 (380px)  │ │
│ │  主图展示区              │ │ sticky 定位      │ │
│ │  max-height: 100vh-160px  │ │ 名称/简介        │ │
│ │  object-fit: contain      │ │ 操作按钮          │ │
│ ├───────────────────────┤ │ 分类/标签         │ │
│ │  图片下方信息卡片          │ │ 点赞数              │ │
│ │  上传者/日期/尺寸/格式 │ │ 评论区            │ │
│ └───────────────────────┘ └─────────────────┘ │
└─────────────────────────────────────────────────┘
```

- 左侧图片面板为 `flex-direction: column`，图片展示区 + 下方信息卡片横向排列
- 图片展示区限制 `max-height: calc(100vh - 160px)`，长图不再擑破页面
- 图片下方信息卡片：上传者头像＋名称＋日期 + 分割线 + 技术参数（4 列网格宽屏下）
- 右侧信息面板：上传者可点击跳转用户主页；点赞按钮仅公共空间图片显示
- ≤ 900px 切换为 `flex-direction: column`

### 4.8 图片上传流程

```
用户点击上传按钮
  ├─ 首页 GlobalHeader → /picture/upload
  ├─ 个人中心公共图库 → /picture/upload
  └─ 个人中心私有图库 → 弹出上传 Modal（原地上传，不跳转页面）

私有图库上传 Modal
  ├─ 支持文件/URL 两种上传方式（Tab 切换）
  ├─ 上传成功 → 跳转 /picture/edit/:id?from=space&spaceId=xxx
  └─ 样式与 PictureUploadView 保持一致，但独立实现

PictureUploadView（仅用于公共图库上传）
  ├─ onMounted: 检查 localStorage pending_edit_picture_id
  │   ├─ 有 → Modal.confirm 继续编辑/放弃
  │   └─ 无 → 正常上传流程
  ├─ 上传成功 → localStorage 存 pending_edit_picture_id + pending_edit_space_id
  └─ 跳转 → /picture/edit/:id

PictureEditView
  ├─ 编辑元信息（名称、简介、分类、标签）
  ├─ 「AI 智能标签」按钮 → 调用 POST /api/picture/auto_tag
  │   → 自动填入返回的 { tags, category }
  ├─ 保存 → 清除 localStorage pending keys
  └─ 跳转 → from=space → /profile?tab=private | from=profile → /profile?tab=public | 默认 → /
```

### 4.9 用户跳转策略

图片详情页、搜索结果页的用户头像/名称均可点击跳转：
- 目标用户 === 当前登录用户 → `router.push('/profile')`
- 目标用户 !== 当前登录用户 → `router.push('/user/:id')`
- UserProfileView `onMounted` 兜底：检测到是自己 → `router.replace('/profile')`

### 4.10 AI 功能架构


```
AI 智能标签（PictureEditView）
  └─ 点击按钮 → POST /api/picture/auto_tag { pictureId }
       → 返回 { tags, category } → 自动填入表单字段

AI 创意助手对话框（AiDialog.vue）
  ├─ 入口: GlobalHeader AI 按钮 + SidePanel AI 图标（仅登录用户可见）
  ├─ 文字生图:
  │   └─ 输入描述 → POST /api/picture/text2image/create_task { prompt, size }
  │       → 轮询 GET /api/picture/text2image/get_task?taskId=xxx
  │       → SUCCEEDED → 显示图片 + 下载按钮
  ├─ 图片编辑:
  │   └─ 上传附件图片（POST /api/picture/upload）
  │       + 输入编辑描述 → POST /api/picture/image_edit/create_task { pictureId, prompt }
  │       → 轮询 GET /api/picture/image_edit/get_task?taskId=xxx
  │       → SUCCEEDED → 显示结果图片 + 下载按钮
  └─ 轮询策略: 3s 间隔，最多 60 次（3 分钟超时）
      状态流转: PENDING → RUNNING → SUCCEEDED / FAILED

AI 创意工坊页面（AiImageView.vue）
  ├─ 路由: /ai/image（requiresAuth）
  ├─ 入口: GlobalHeader 导航栏「AI 创作」链接（仅登录用户可见）
  ├─ 功能与 AiDialog 相同（文字生图 + 图片编辑），独立全页面体验
  ├─ 空状态: 功能引导卡片 + 示例提示词快速填充
  ├─ 结果操作: 下载 / 保存到图库 / 继续编辑
  └─ 智能路由: 仅文字 → text2image API；图片+文字 → image_edit API
```

---

## 5. 开发进度

| 阶段 | 内容 | 状态 |
|------|------|------|
| Phase 1 | 项目初始化 + 安装依赖 + 基础配置 | ✅ 完成 |
| Phase 2 | CSS 令牌 + Axios 封装 + API 层 + Pinia Store | ✅ 完成 |
| Phase 3 | 路由配置 + 权限守卫 + BasicLayout + SidePanel | ✅ 完成 |
| Phase 4 | 登录页面 | ✅ 完成 |
| Phase 5 | 注册页面 | ✅ 完成 |
| Phase 6 | 公共图库首页（分类筛选 + 分页） | ✅ 完成 |
| Phase 7 | 图片详情页（自适应展示 + 雪花 ID） | ✅ 完成 |
| Phase 8 | 管理员用户管理 | ✅ 完成 |
| Phase 9 | 个人中心（资料编辑 + 公共/私有图库切换） | ✅ 完成 |
| Phase 10 | 用户模块优化（图片预览自适应、用户主页、头像上传、退出跳转首页） | ✅ 完成 |
| Phase 11 | 搜索功能（文本搜索、以图搜图、颜色搜索） | ✅ 完成 |
| Phase 12 | 图片上传页 + 编辑页（文件/URL 上传、元信息编辑、pending 机制） | ✅ 完成 |
| Phase 13 | 管理员图片管理 + 空间管理 | ✅ 完成 |
| Phase 14 | 顶部导航栏 GlobalHeader（品牌、管理员链接、搜索框、用户菜单） | ✅ 完成 |
| Phase 15 | UX 统一（个人空间上传复用上传页、用户跳转自/他人区分、审核状态徽章） | ✅ 完成 |
| Phase 16 | 私有图库原地上传、管理员空间管理图标修复 | ✅ 完成 |
| Phase 17 | AI 功能（智能标签、文字生图、图片编辑、AI 对话框） | ✅ 完成 |
| Phase 18 | 团队空间（空间头像上传、团队空间 Tab、空间详情页、成员管理） | ✅ 完成 |
| Phase 19 | 搜索页重构（多对象搜索 Tab、共享搜索框、搜索历史/热门关键词、颜色搜索、URL 同步） | ✅ 完成 |
| Phase 20 | 暗色主题修复（variables.css html.dark 变量覆盖） | ✅ 完成 |
| Phase 21 | 图片评论功能（两级评论、回复、删除、分页、权限控制） | ✅ 完成 |
| Phase 22 | 评论功能修复（匿名用户展示、删除权限补入图片上传者、头像跳转个人主页） | ✅ 完成 |
| Phase 23 | 图片详情页布局重构（图片基础信息移至图片下方，右侧信息面板职中交互内容） | ✅ 完成 |

---

## 6. API 对接约定

### 请求基本结构

```typescript
interface BaseResponse<T> {
  code: number      // 0 = 成功, 40100 = 未登录
  data: T
  message: string
}
```

### 认证方式

后端使用 **Session + Cookie** 认证，Axios `withCredentials: true`。

### 跨域处理

```typescript
// vite.config.ts — 开发环境反向代理
export default defineConfig({
  plugins: [vue()],
  resolve: { alias: { '@': path.resolve(__dirname, './src') } },
  server: {
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
```

### 入口文件

```typescript
// main.ts — 全量引入 Ant Design Vue（按需引入可后续优化）
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './assets/styles/variables.css'
import './assets/styles/global.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Antd)
app.mount('#app')
```

---

## 7. 技术决策记录

| 决策 | 选项 | 选择 | 原因 |
|------|------|------|------|
| 组件库 | Ant Design Vue / Element Plus | Ant Design Vue 4.x | 企业级 Table、Form 组件成熟，MCP 文档丰富 |
| 状态管理 | Pinia Option / Setup | Option Store | MCP Pinia 文档推荐，类型推断好，代码简洁 |
| 字体方案 | 系统字体 / 自定义字体 | LXGW WenKai + 系统 sans-serif | 古风需求 + DESIGN.md 要求 |
| 导航栏 | 顶部 Header / 侧边面板 | 左侧浮动 SidePanel | 更沉浸的图库浏览体验，可收缩不占内容宽度 |
| 管理入口 | URL 直接访问 / 隐藏入口 | 5 次点击 Logo | 需求要求"只有开发者自己知道" |
| 40100 处理 | 全局 toast + 跳转 / 静默拒绝 | 静默拒绝 | 避免初始化时 toast 循环，由业务层决定 |
| 雪花 ID | Number / String | String | JavaScript 精度限制，防止 ID 截断 |
| 分页方式 | 无限滚动 / 标准分页 | 标准 `<a-pagination>` | 用户更可控，支持页大小切换和快速跳转 |
| 暗色主题 | JS 切换 class / CSS media query | `html.dark` class + CSS 变量覆盖 | 支持用户手动切换，主题状态持久化 |
| 评论嵌套 | 无限嵌套 / 两级 | 两级（评论 + 回复） | 降低后端复杂度，UI 更清晰 |
| 评论分页 | 翻页 / 追加 | 追加（load-more） | 评论场景加载更多比翻页体验更自然 |

---

## 8. 评论系统架构

### 数据模型

```typescript
interface CommentVO {
  id?: number
  pictureId?: number
  content?: string
  parentId?: number          // null = 顶级评论，非 null = 回复
  replyUserId?: number       // 回复目标用户 ID
  userInfo?: UserVO          // 评论者信息（后端接口返回字段名）
  replyUserInfo?: UserVO     // 被回复者信息（后端接口返回字段名）
  createTime?: string
  replies?: CommentVO[]      // 前 3 条回复（接口返回）
  replyCount?: number        // 总回复数
}
```

### 状态设计（PictureDetailView.vue）

```typescript
const comments = ref<API.CommentVO[]>([])           // 顶级评论列表（追加）
const commentTotal = ref(0)                         // 评论总数
const commentCurrent = ref(1)                       // 当前页码
const commentPageSize = ref(10)                     // 每页条数
const commentLoading = ref(false)
const commentText = ref('')                         // 评论输入
const replyTargetId = ref<number | null>(null)      // 当前展开回复框的顶级评论 id
const replyTargetUser = ref<API.UserVO | null>(null) // 回复目标用户（@用户名）
const replyText = ref('')                           // 回复输入
const expandedReplies = ref<Record<number, {        // 展开的回复状态
  list: API.CommentVO[]
  loading: boolean
  total: number
  current: number
}>>({})
```

### 权限控制

```typescript
function canDelete(comment: API.CommentVO) {
  if (!userStore.isLoggedIn) return false
  // 评论作者 / 管理员 / 图片上传者 均可删除
  return (
    String(comment.userInfo?.id) === String(userStore.loginUser?.id) ||
    userStore.loginUser?.userRole === 'admin' ||
    String(picture.value?.userId) === String(userStore.loginUser?.id)
  )
}
```

### 接口调用链路

```
进入图片详情页
  └─ fetchDetail() → GET /api/picture/get/vo
       └─ fetchComments(1) → GET /api/comment/list?pictureId=xxx&current=1&pageSize=10
            └─ 返回 records（含每条评论前 3 条 replies + replyCount）

发表评论
  └─ submitComment() → POST /api/comment/add { pictureId, content }
       └─ 成功后 unshift 至 comments 列表

回复评论
  └─ submitReply(parentComment) → POST /api/comment/add { pictureId, content, parentId, replyUserId }
       └─ 更新 expandedReplies[cid] 或 comment.replies

展开全部回复
  └─ expandReplies(comment) → GET /api/comment/reply/list?commentId=xxx&current=1&pageSize=20
       └─ 存入 expandedReplies[comment.id!]

加载更多回复
  └─ loadMoreReplies(cid) → GET /api/comment/reply/list?commentId=cid&current=next
       └─ 追加至 expandedReplies[cid].list

删除评论
  └─ handleDeleteComment(comment, parentId?) → Modal.confirm
       └─ POST /api/comment/delete { id }
       └─ 从本地状态移除
```

---

## 9. 错误处理策略

| 场景 | 处理方式 |
|------|---------|
| `code === 0` | 返回 `data`，业务层直接使用 |
| `code === 40100` | `Promise.reject`，不弹 toast，路由守卫或页面自行处理 |
| 其他业务错误 | 全局 `message.error(data.message)` |
| 网络异常 | 全局 `message.error('网络异常')` |
| 图片详情 40100 | 页面 catch 中 `router.push('/login')` |
| 管理员权限不足 | 路由守卫重定向至 `/` |
