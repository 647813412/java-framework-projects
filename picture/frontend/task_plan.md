# 图库社区系统 — 任务计划

> 创建日期: 2026-04-13 | 更新日期: 2026-04-13

## 项目目标

开发图库社区系统前端，包含登录/注册、公共图库首页、图片详情、个人中心、管理员用户管理等功能。

## 阶段规划

### Phase 1: 项目初始化 `complete`
- [x] Vite + Vue 3 + TypeScript 项目创建
- [x] 需求文档沉淀 → v2.0
- [x] 技术方案沉淀 → v2.0
- [x] MCP 获取最新库文档（Vue 3 / Router / Pinia / Antd / Axios / Vite）

### Phase 2: 基础设施搭建 `complete`
- [x] 安装依赖 (vue-router, pinia, ant-design-vue, axios)
- [x] CSS 设计令牌变量配置 (`variables.css` + `global.css`)
- [x] Axios 请求封装 (`request.ts`，baseURL `/api`，40100 静默拒绝)
- [x] 类型声明文件适配 (`api/typings.d.ts`)
- [x] API 层搭建 (userController + pictureController + spaceController)
- [x] Pinia User Store (Option Store 风格)
- [x] Vite 代理配置 (`/api` → `localhost:8080`)

### Phase 3: 路由与布局 `complete`
- [x] 路由定义 + meta 配置 (guest / requiresAdmin)
- [x] 全局路由守卫 (beforeEach，首次获取登录态)
- [x] BasicLayout 布局组件
- [x] SidePanel 左侧浮动导航面板（替代原 GlobalHeader 设计）

### Phase 4: 登录页面 `complete`
- [x] 页面 UI 实现（水墨山水背景 + 暖白卡片）
- [x] 表单验证 (账号≥4, 密码≥8)
- [x] 登录 API 对接
- [x] 登录成功跳转逻辑

### Phase 5: 注册页面 `complete`
- [x] 页面 UI 实现
- [x] 表单验证 (含确认密码)
- [x] 注册 API 对接
- [x] 注册成功跳转登录页

### Phase 6: 公共图库首页 `complete`
- [x] 分类标签筛选（从 API 获取 tagCategory）
- [x] 搜索功能
- [x] 图片瀑布流/卡片列表
- [x] 标准分页 (`a-pagination`)
- [x] 点击卡片跳转详情

### Phase 7: 图片详情页 `complete`
- [x] Flex 布局 — 左图片自适应 + 右 380px 信息面板
- [x] 图片自然尺寸展示 (`max-width: 100%`)
- [x] 雪花 ID 字符串处理（避免 Number 精度丢失）
- [x] 响应式折叠 (≤768px column 布局)

### Phase 8: 管理员用户管理 `complete`
- [x] 用户列表分页表格
- [x] 搜索筛选功能
- [x] 新增用户 Modal
- [x] 编辑用户 Modal
- [x] 删除用户确认
- [x] SidePanel 5 次点击 Logo 隐藏入口

### Phase 9: 个人中心 `complete`
- [x] 个人资料卡片展示
- [x] 编辑个人信息 Modal
- [x] 公共图库 Tab（个人公开图片 + 分页）
- [x] 私有图库 Tab（空间图片 + 分页）

### Phase 10: 文档更新 `complete`
- [x] 需求文档更新至 v2.0
- [x] 技术方案更新至 v2.0
- [x] 任务计划状态同步
- [x] 进度日志补全

### Phase 11: 导航栏重构 `complete`
- [x] GlobalHeader 重写为顶部导航栏（半透明毛玻璃 + backdrop-filter）
- [x] 布局：左品牌→中导航→右搜索+用户下拉
- [x] 导航链接：主页、上传（登录后）、用户管理/图片管理（管理员）
- [x] 用户下拉菜单：个人中心、主题切换（预留）、退出登录
- [x] BasicLayout 替换 SidePanel 为 GlobalHeader
- [x] HomeView hero 区域去除重复搜索框，接收路由 query.search
- [x] ProfileView 编辑弹窗保留用户原有头像信息
- [x] 响应式适配（≤768px 隐藏文字仅显示图标）

## 技术决策记录

| 决策 | 选项 | 选择 | 原因 |
|------|------|------|------|
| 组件库 | Ant Design Vue / Element Plus | Ant Design Vue 4.x | 企业级 Table、Form 组件成熟 |
| 状态管理 | Pinia Option / Setup | Option Store | MCP 文档推荐，类型推断好 |
| 字体方案 | 系统字体 / 自定义字体 | LXGW WenKai + Pin Sans | 古风需求 + DESIGN.md 要求 |
| 导航栏 | 顶部 Header / 侧边面板 | 顶部导航栏(GlobalHeader) | 用户要求改为顶部导航，半透明毛玻璃，含搜索+用户下拉 |
| 管理入口 | URL 直接访问 / 隐藏入口 | 5 次点击 Logo | 需求要求"只有开发者自己知道" |
| 40100 处理 | toast + 跳转 / 静默拒绝 | 静默拒绝 | 避免初始化 toast 循环 |
| 雪花 ID | Number / String | String | JS 精度限制 |

## 错误记录

| 错误 | 尝试 | 解决方案 |
|------|------|---------|
| 40100 toast 循环 | 拦截器弹 toast | 40100 静默 `Promise.reject`，不弹 toast |
| 分页 `total` 不更新 | 初始 total=0 | 每次请求后赋值 `total = res.data.total` |
| 雪花 ID 精度丢失 | `Number(id)` | 全程使用 `string` 类型传递 ID |
