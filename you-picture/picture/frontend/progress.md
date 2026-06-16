# 开发进度日志

> 创建日期: 2026-04-13

## Session 1 — 2026-04-13

### 已完成

- [x] Vite + Vue 3 + TypeScript 项目脚手架创建
- [x] 需求文档 v1.0 编写
- [x] 技术方案 v1.0 编写
- [x] MCP 获取最新库文档 (Vue 3, Vue Router, Pinia, Ant Design Vue, Axios)
- [x] 任务计划创建
- [x] 研究发现沉淀 (`findings.md`)

### 文件创建记录

| 文件 | 操作 |
|------|------|
| `frontend/` | Vite 创建项目 |
| `docs/requirements.md` | 新建 v1.0 |
| `docs/tech-design.md` | 新建 v1.0 |
| `task_plan.md` | 新建 |
| `findings.md` | 新建 |
| `progress.md` | 新建 |

---

## Session 2 — 2026-04-13

### 已完成

- [x] Phase 2: 基础设施搭建
  - 安装 vue-router / pinia / ant-design-vue / axios
  - CSS 设计令牌 (`variables.css` + `global.css`)
  - Axios 实例封装 (`request.ts`，baseURL `/api`，40100 静默拒绝)
  - API 类型声明 (`api/typings.d.ts`)
  - API 控制器 (`userController.ts` + `pictureController.ts` + `spaceController.ts`)
  - Pinia User Store (Option Store 风格)
  - Vite 代理配置
- [x] Phase 3: 路由与布局
  - 路由定义 + meta.guest / meta.requiresAdmin
  - 全局 beforeEach 守卫
  - BasicLayout 布局组件
  - SidePanel 左侧浮动导航面板（56px 宽，可拖拽/收缩）
- [x] Phase 4: 登录页面（水墨背景 + 暖白卡片 + 表单验证）
- [x] Phase 5: 注册页面（含确认密码校验）
- [x] Phase 6: 公共图库首页（分类筛选 + 搜索 + 分页）
- [x] Phase 7: 图片详情页（自适应展示 + 雪花 ID 字符串处理）
- [x] Phase 8: 管理员用户管理（表格 + 搜索 + CRUD + 隐藏入口）
- [x] Phase 9: 个人中心（资料卡 + 编辑 + 公共/私有图库 Tab）

### Bug 修复

- 40100 toast 循环 → 拦截器改为静默 reject
- 分页 total 不更新 → 请求后赋值
- 雪花 ID 精度丢失 → 全程字符串
- SidePanel 从 GlobalHeader 重构为左侧浮动面板

### 文件创建/修改记录

| 文件 | 操作 |
|------|------|
| `src/assets/styles/variables.css` | 新建 — CSS 令牌 |
| `src/assets/styles/global.css` | 新建 — 全局样式 |
| `src/request.ts` | 新建 — Axios 封装 |
| `src/api/typings.d.ts` | 新建 — 类型声明 |
| `src/api/userController.ts` | 新建 — 用户 API |
| `src/api/pictureController.ts` | 新建 — 图片 API |
| `src/api/spaceController.ts` | 新建 — 空间 API |
| `src/stores/user.ts` | 新建 — Pinia Store |
| `src/router/index.ts` | 新建 — 路由 + 守卫 |
| `src/layouts/BasicLayout.vue` | 新建 — 基础布局 |
| `src/components/SidePanel.vue` | 新建 — 侧边导航 |
| `src/views/LoginView.vue` | 新建 — 登录页 |
| `src/views/RegisterView.vue` | 新建 — 注册页 |
| `src/views/HomeView.vue` | 新建 — 首页 |
| `src/views/PictureDetailView.vue` | 新建 — 图片详情 |
| `src/views/ProfileView.vue` | 新建 — 个人中心 |
| `src/views/admin/UserManageView.vue` | 新建 — 用户管理 |
| `src/main.ts` | 修改 — 注册插件 |
| `src/App.vue` | 修改 — router-view |
| `vite.config.ts` | 修改 — 别名 + 代理 |

---

## Session 3 — 2026-04-13

### 已完成

- [x] Phase 10: 文档更新
  - MCP 重新获取 6 个库最新文档（Vue 3, Vue Router, Pinia, Ant Design Vue, Axios, Vite）
  - 需求文档更新至 v2.0（覆盖全部 8 个功能模块 + 路由表 + API 汇总）
  - 技术方案更新至 v2.0（MCP 验证代码示例 + 实际实现对齐 + 设计令牌完整表）
  - 任务计划状态全部同步
  - 进度日志补全所有 Session
