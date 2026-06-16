# 研究发现

> 创建日期: 2026-04-13

## MCP 库版本确认

| 库 | Library ID | 最新版本 | Benchmark |
|------|-----------|---------|-----------|
| Vue 3 | /vuejs/docs | ^3.5.32 (已安装) | 76.62 |
| Vue Router | /websites/router_vuejs_zh | ^4.x | 89.9 |
| Pinia | /vuejs/pinia | ^3.x (v3 branch) | 90.15 |
| Ant Design Vue | /vuecomponent/ant-design-vue | 3.2.20+ / 4.x | 75.85 |
| Axios | /axios/axios-docs | ^1.x | 93.17 |
| Vite | /vitejs/vite | ^8.0.4 (已安装) | 81.24 |

## Vue 3 最新用法要点

- 使用 `<script setup lang="ts">` 语法，无需手动 return
- `ref()` 创建响应式基本值，`reactive()` 创建响应式对象
- `defineProps<T>()` 泛型式 props 声明
- `onMounted()` 等生命周期钩子在 setup 中直接导入使用
- `$ref` reactivity transform 已标记实验性，**不使用**

## Vue Router 最新用法要点

- `createRouter({ history: createWebHistory(), routes })` 创建路由器
- `router.beforeEach((to, from) => {})` 全局前置守卫
- `to.meta.requiresAuth` 检查路由元信息
- 守卫返回 `false` 取消导航，返回路由对象重定向
- `onBeforeRouteUpdate` 组件内守卫 (Composition API)

## Pinia 最新用法要点

- `defineStore('name', { state, getters, actions })` Option Store 模式
- state 必须是返回对象的函数 `() => ({})`
- getters 接收 state 参数或使用 `this`
- actions 中用 `this` 访问 state
- 跨 store：在 getters/actions 中调用 `useOtherStore()`

## Ant Design Vue 最新用法要点

- `v-model:value` 双向绑定 Input
- `v-model:open` 控制 Modal 显隐（不再使用 visible）
- `a-form :model="formState"` + `a-form-item` 表单
- 全局 `message.success()` / `message.error()` 提示
- `notification.success()` 通知提醒
- 推荐按需引入：`import { Button, Form } from 'ant-design-vue'`

## Axios 最新用法要点

- `axios.create({ baseURL, timeout, withCredentials })` 创建实例
- 请求拦截器: `instance.interceptors.request.use(config => config)`
- 响应拦截器: `instance.interceptors.response.use(response, error)`
- 401 处理: 拦截器中 `error.response?.status === 401` → 刷新 token 或跳转登录

## 后端 API 结构分析

- 统一返回 `{ code, data, message }`，`code === 0` 表示成功
- 认证方式: Session + Cookie（withCredentials = true）
- 用户角色: `user` / `admin`
- 分页参数: `current`, `pageSize`, `sortField`, `sortOrder`
- 分页返回: `{ records, total, current, size, pages }`
