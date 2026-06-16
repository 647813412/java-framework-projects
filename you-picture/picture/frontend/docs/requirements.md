# 图库社区系统 — 前端需求文档

> 版本: v4.2 | 日期: 2026-04-13 | 更新日期: 2026-04-23

## 1. 项目概述

图库社区系统是一个以图片发现与分享为核心的在线平台，用户可以浏览、上传、收藏图片，创建个人空间进行图片管理。前端涵盖以下模块：

- **用户模块**: 认证（登录/注册/退出）、管理员用户管理
- **图库模块**: 公共图库首页、图片详情、分类筛选、搜索
- **上传模块**: 图片上传（文件/URL）、图片编辑、AI 智能标签
- **个人中心模块**: 个人资料查看/编辑、公共图库/收藏/私有图库/团队空间 Tab 切换、私有图库原地上传
- **空间模块**: 私有空间管理（查询、创建、升级）、团队空间管理（创建、查看、成员管理）、空间头像上传
- **管理模块**: 用户管理、图片管理、空间管理
- **AI 模块**: AI 智能分类标签、文字生图、图片编辑、AI 创意助手对话框、AI 创意工坊页面

---

## 2. 功能需求

### 2.1 用户登录

| 项目 | 说明 |
|------|------|
| 路由 | `/login` |
| 后端接口 | `POST /api/user/login` |
| 请求体 | `UserLoginRequest { userAccount: string, userPassword: string }` |
| 响应体 | `BaseResponse<LoginUserVO>` — 含 id, userAccount, userName, userAvatar, userProfile, userRole, createTime, updateTime |
| 交互流程 | 1. 输入账号密码 → 2. 点击登录 → 3. 成功跳转首页 / 失败提示错误 |
| 校验规则 | 账号 ≥ 4 位，密码 ≥ 8 位（前后端一致） |
| 页面风格 | 全屏水墨山水背景 + 居中暖白卡片表单 + 朱砂红 CTA 按钮 |

### 2.2 用户注册

| 项目 | 说明 |
|------|------|
| 路由 | `/register` |
| 后端接口 | `POST /api/user/register` |
| 请求体 | `UserRegisterRequest { userAccount, userPassword, checkPassword }` |
| 响应体 | `BaseResponse<number>` — 返回新用户 id |
| 交互流程 | 1. 输入账号/密码/确认密码 → 2. 点击注册 → 3. 成功跳转登录页 |
| 校验规则 | 账号 ≥ 4 位，密码 ≥ 8 位，两次密码一致 |

### 2.3 退出登录

| 项目 | 说明 |
|------|------|
| 入口 | 左侧 SidePanel 退出登录按钮 / GlobalHeader 头像下拉菜单 |
| 后端接口 | `GET /api/user/logout` |
| 响应体 | `BaseResponse<boolean>` |
| 交互流程 | 1. 点击退出 → 2. 清除本地用户状态 → 3. 跳转首页 `/` |

### 2.4 获取当前登录用户

| 项目 | 说明 |
|------|------|
| 触发时机 | 应用首次加载 / 路由守卫 `beforeEach` |
| 后端接口 | `GET /api/user/get/login` |
| 响应体 | `BaseResponse<LoginUserVO>` |
| 说明 | 用于恢复登录状态，仅首次调用 |

### 2.5 管理员 — 用户管理

| 项目 | 说明 |
|------|------|
| 路由 | `/admin/user-manage` |
| 入口 | **隐藏入口** — SidePanel Logo 连续点击 5 次，仅 `userRole === 'admin'` 可访问 |
| 路由守卫 | `meta.requiresAdmin` — 非管理员重定向至首页 |
| 功能列表 | 分页查询用户、添加用户、编辑用户、删除用户 |

#### 2.5.1 分页查询用户

| 项目 | 说明 |
|------|------|
| 后端接口 | `POST /api/user/list/page/vo` |
| 请求体 | `UserQueryRequest { current, pageSize, userAccount, userName, userRole, sortField, sortOrder }` |
| 响应体 | `BaseResponse<PageUserVO_>` — 含 records, total, current, size, pages |

#### 2.5.2 添加用户

| 项目 | 说明 |
|------|------|
| 后端接口 | `POST /api/user/add` |
| 请求体 | `UserAddRequest { userAccount, userName, userAvatar, userProfile, userRole }` |
| 响应体 | `BaseResponse<number>` — 新用户 id |

#### 2.5.3 编辑用户

| 项目 | 说明 |
|------|------|
| 后端接口 | `POST /api/user/update` |
| 请求体 | `UserUpdateRequest { id, userName, userAvatar, userProfile, userRole }` |
| 响应体 | `BaseResponse<boolean>` |

#### 2.5.4 删除用户

| 项目 | 说明 |
|------|------|
| 后端接口 | `POST /api/user/delete` |
| 请求体 | `DeleteRequest { id }` |
| 响应体 | `BaseResponse<boolean>` |
| 交互 | 二次确认后删除 |

### 2.6 公共图库首页

| 项目 | 说明 |
|------|------|
| 路由 | `/` |
| 后端接口 | `POST /api/picture/list/page/vo/cache` |
| 请求体 | `PictureQueryRequest { current, pageSize, nullSpaceId: true, category?, searchText?, tags?, sortField, sortOrder }` |
| 响应体 | `BaseResponse<PagePictureVO_>` |
| 功能 | 分类筛选栏 + 搜索框 + 图片卡片网格 + 标准分页（12/20/40 每页 + 快速跳转） |
| 辅助接口 | `GET /api/picture/tag_category` → 获取分类和标签列表 |
| 团队空间展示 | `POST /api/space/list/page/vo` — `SpaceQueryRequest { spaceType: 1, current: 1, pageSize: 12 }` — 展示所有团队空间卡片 |
| 申请加入 | `POST /api/spaceUser/apply` — `SpaceUserApplyRequest { spaceId }` — 点击卡片上「申请加入」按钮提交申请，等待管理员审批 |
| 团队卡片信息 | 空间头像、空间名称、创建者名称 |
| 点击跳转 | 点击团队卡片 → 跳转 `/space/:id`；未审批通过则显示"无权限" |

### 2.7 图片详情页

| 项目 | 说明 |
|------|------|
| 路由 | `/picture/:id` |
| 后端接口 | `GET /api/picture/get/vo?id=` |
| 请求参数 | `{ id: string }` （雪花 ID，使用字符串避免精度丢失） |
| 响应体 | `BaseResponse<PictureVO>` |
| 布局 | flex 双栏：**左侧**图片预览区（图片 + 图片下方基础信息卡片） + **右侧** 380px 固定宽度信息面板（sticky 定位，`overflow-y: auto`） |
| 左侧图片区 | 图片展示（`max-height: calc(100vh - 160px)`，长图自适应压缩，`object-fit: contain` 保持比例）+ 图片下方信息卡片（上传者、上传日期、尺寸、比例、大小、格式、主色调） |
| 右侧信息面板 | 图片名称、简介、操作按钮（点赞/分享/下载/删除）、分类与标签、点赞数、评论区 |
| 评论区 | 信息面板底部，详见 2.20 图片评论 |
| 响应式 | ≤ 900px 切换为纵向堆叠，信息面板取消 sticky |
| 错误处理 | 40100 → 跳转登录页 |

### 2.8 个人中心

| 项目 | 说明 |
|------|------|
| 路由 | `/profile` |
| 入口 | SidePanel 头像点击（已登录 → `/profile`，未登录 → `/login`） |
| 功能 | 个人资料卡（头像、昵称、账号、注册日期、简介） + 编辑资料弹窗 + 公共图库/私有图库 Tab 切换 |

#### 2.8.1 编辑个人资料

| 项目 | 说明 |
|------|------|
| 后端接口 | `POST /api/user/update` |
| 请求体 | `UserUpdateRequest { id, userName, userAvatar, userProfile }` |
| 可编辑字段 | 昵称（≤ 20 字）、头像链接 URL（含预览）、个人简介（≤ 200 字） |
| 更新后 | 重新调用 `GET /api/user/get/login` 刷新全局用户状态 |

#### 2.8.2 公共图库 Tab

| 项目 | 说明 |
|------|------|
| 后端接口 | `POST /api/picture/list/page/vo/cache` |
| 请求体 | `PictureQueryRequest { nullSpaceId: true, userId: 当前用户ID, current, pageSize, sortField, sortOrder }` |
| 说明 | 展示当前用户上传到公共空间的图片，独立分页 |

#### 2.8.3 私有图库 Tab

| 项目 | 说明 |
|------|------|
| 空间查询 | `POST /api/space/list/page/vo` — `SpaceQueryRequest { userId: 当前用户ID }` |
| 图片查询 | `POST /api/picture/list/page/vo/cache` — `PictureQueryRequest { spaceId: 空间ID }` |
| 说明 | 先查询用户私有空间，再按空间 ID 查图片。无空间时显示空状态 |
| 空间信息栏 | 空间名称、已用数量/最大数量、已用容量/最大容量 |
| 上传方式 | 点击上传按钮 → 弹出上传 Modal（文件/URL Tab 切换），不跳转到公共上传页，上传后跳转编辑页 |
| 空间头像 | 创建空间时可上传空间头像（文件上传: `POST /api/file/upload`，URL 上传: `POST /api/file/upload/url`） |

#### 2.8.4 团队空间 Tab

| 项目 | 说明 |
|------|------|
| 后端接口 | `POST /api/spaceUser/list/my` |
| 响应体 | `BaseResponse<List<SpaceUserVO>>` |
| 说明 | 展示当前用户加入或创建的所有团队空间列表 |
| 卡片信息 | 空间头像、空间名称、用户角色（管理员/编辑者/查看者）、加入时间 |
| 新建团队 | 与私有空间创建一致，`spaceType: 1`，可上传空间头像 |
| 点击跳转 | 点击团队卡片 → 跳转到 `/space/:id` 团队空间详情页 |

#### 2.8.5 团队空间详情页

| 项目 | 说明 |
|------|------|
| 路由 | `/space/:id` |
| 空间信息 | `GET /api/space/get/vo?id=` — 展示空间头像、名称、创建者、创建时间 |
| 图片列表 | `POST /api/picture/list/page/vo` — `PictureQueryRequest { spaceId }` |
| 成员列表 | `POST /api/spaceUser/list` — `SpaceUserQueryRequest { spaceId }` |
| 权限控制 | 非成员只能查看空间基本信息，无法查看图片（显示"无权限访问"） |
| 成员功能 | 查看图片、上传图片（文件/URL）、编辑图片信息、删除图片 |
| 管理员功能 | 移除成员 — `POST /api/spaceUser/delete` — `DeleteRequest { id }` |
| 退出团队 | 普通成员可退出团队 — `POST /api/spaceUser/delete` — `DeleteRequest { id }` — 退出后跳转个人中心 |
| Tab 切换 | 图片 Tab + 成员 Tab（仅成员可见） |

### 2.9 用户主页（他人）

| 项目 | 说明 |
|------|------|
| 路由 | `/user/:id` |
| 后端接口 | `GET /api/user/get/vo?id=` |
| 请求参数 | `{ id: number }` |
| 响应体 | `BaseResponse<UserVO>` |
| 入口 | 图片详情页中点击上传者头像/名称跳转 |
| 布局 | 复用 ProfileView 风格的水墨横幅 + 用户基本信息 + 公共图库展示（无编辑权限） |

### 2.10 头像上传/URL 切换

| 项目 | 说明 |
|------|------|
| 涉及位置 | 个人中心编辑资料弹窗、管理员新增用户弹窗、管理员编辑用户弹窗 |
| 后端接口 | `POST /api/file/test/upload`（上传模式） |
| 请求格式 | `multipart/form-data`，field name: `file` |
| 响应体 | `BaseResponse<string>` — 返回文件 URL |
| 交互 | 头像字段旁有「切换为上传/切换为URL」链接按钮，点击切换两种输入模式 |
| URL 模式 | 文本输入框，直接填写头像 URL |
| 上传模式 | 文件选择器，选择图片后自动上传并回填 URL |

### 2.11 搜索页

| 项目 | 说明 |
|------|------|
| 路由 | `/search?q=&type=&mode=` |
| 搜索对象 | 图片（默认）、用户、空间 — 通过顶部下划线 Tab 切换 |
| 共享搜索框 | 顶部胶囊形搜索框，各 Tab 共享同一关键词，切 Tab 时保留输入 |
| 图片搜索 | 关键词模式：`POST /api/picture/list/page/vo/cache`；颜色模式：`POST /api/picture/search/color` |
| 用户搜索 | `POST /api/user/search`（`searchUserUsingPost`） |
| 空间搜索 | `POST /api/space/list/page/vo` |
| 搜索历史 | 每个 Tab 独立历史记录；`GET /api/search/history`、`POST /api/search/history/delete` |
| 热门搜索 | 仅图片 Tab 显示，`GET /api/search/hot`，2 列网格，前 3 名红色排序 + 热门徽章 |
| URL 同步 | `?q=关键词&type=picture\|user\|space&mode=keyword\|color` |
| 颜色搜索 | 图片 Tab 下切换颜色模式，系统取色器 + 7 个预设色块 |
| 结果展示 | 图片卡片网格 / 用户卡片列表 / 空间卡片列表 + 分页 |

### 2.12 图片上传

| 项目 | 说明 |
|------|------|
| 路由 | `/picture/upload` 或 `/picture/upload?spaceId=xxx` |
| 后端接口 | `POST /api/picture/upload`（文件上传）、`POST /api/picture/upload/url`（URL 抓取） |
| 请求格式 | 文件上传: `multipart/form-data`，field: `file`；URL 上传: JSON `{ fileUrl, spaceId? }` |
| 交互流程 | 1. 选择上传方式（拖拽/选择文件 或 输入 URL） → 2. 上传 → 3. 成功后自动跳转编辑页 |
| 私有空间 | 携带 `spaceId` query 参数时，图片上传至对应私有空间 |
| 未完成编辑 | 上传后将 `pending_edit_picture_id` 和 `pending_edit_space_id` 存入 localStorage，下次进入上传页时弹窗提示继续编辑或放弃 |
| 页面风格 | 全屏上传体验，顶部返回栏 + Tab 切换（文件/URL）+ 拖拽上传区域 |

### 2.13 图片编辑

| 项目 | 说明 |
|------|------|
| 路由 | `/picture/edit/:id?from=&spaceId=` |
| 后端接口 | `POST /api/picture/edit` |
| 请求体 | `PictureEditRequest { id, name, introduction, category, tags }` |
| 交互流程 | 1. 填写图片元信息（名称、简介、分类、标签） → 2. 保存 → 3. 跳转回来源页面 |
| 来源跳转 | `from=space` → `/profile?tab=private`；`from=profile` → `/profile?tab=public`；默认 → `/` |
| 清理 | 保存时清除 `pending_edit_picture_id` 和 `pending_edit_space_id` |
| AI 标签 | 分类字段旁有「AI 智能标签」按钮，点击调用 `POST /api/picture/auto_tag`，返回 `{ tags, category }` 自动填入 |

### 2.14 管理员 — 图片管理

| 项目 | 说明 |
|------|------|
| 路由 | `/admin/picture-manage` |
| 入口 | GlobalHeader 管理员导航链接 |
| 路由守卫 | `meta.requiresAdmin` |
| 后端接口 | `POST /api/picture/list/page/vo`（分页查询）、`POST /api/picture/review`（审核）、`POST /api/picture/delete`（删除） |
| 功能 | 分页查询所有图片、按审核状态筛选、审核通过/拒绝、删除图片 |

### 2.15 管理员 — 空间管理

| 项目 | 说明 |
|------|------|
| 路由 | `/admin/space-manage` |
| 入口 | GlobalHeader 管理员导航链接 |
| 路由守卫 | `meta.requiresAdmin` |
| 后端接口 | `POST /api/space/list/page`（分页查询）、`POST /api/space/update`（编辑）、`POST /api/space/delete`（删除）、`GET /api/space/list/level`（等级列表） |
| 功能 | 分页查询所有空间、按空间名/等级筛选、编辑空间信息（含等级预设自动填充）、删除空间 |

### 2.17 AI 创意助手对话框

| 项目 | 说明 |
|------|------|
| 组件 | `AiDialog.vue` — 全局弹窗组件 |
| 入口 | GlobalHeader 右侧 AI 按钮（登录后可见）、SidePanel AI 导航图标（登录后可见） |
| 风格 | DeepSeek 风格暗色主题（#1e1e2e 背景 + #6366f1 / #a78bfa 紫色主色调） |
| 功能 | 1. **文字生图** — 输入描述，调用 `POST /api/picture/text2image/create_task` 创建任务，轮询 `GET /api/picture/text2image/get_task` 获取结果 |
| | 2. **图片编辑** — 上传图片附件 + 输入编辑描述，调用 `POST /api/picture/image_edit/create_task`，轮询 `GET /api/picture/image_edit/get_task` |
| 轮询策略 | 3 秒间隔，最多 60 次，状态流转: PENDING → RUNNING → SUCCEEDED / FAILED |
| 交互 | 聊天气泡式消息列表，支持图片附件上传、图片结果下载 |
| 初始消息 | AI 欢迎语介绍支持的功能 |

### 2.18 AI 创意工坊页面

| 项目 | 说明 |
|------|------|
| 路由 | `/ai/image` |
| 组件 | `AiImageView.vue` |
| 入口 | GlobalHeader 导航栏「AI 创作」链接（登录后可见） |
| 路由守卫 | `meta.requiresAuth` |
| 功能 | 独立全页面 AI 图片生成工坊，支持文字生图和图片编辑两种模式 |
| 文字生图 | 输入描述 → 调用 `POST /api/picture/text2image/create_task` → 轮询 `GET /api/picture/text2image/get_task` |
| 图片编辑 | 上传图片 + 输入描述 → 调用 `POST /api/picture/image_edit/create_task` → 轮询 `GET /api/picture/image_edit/get_task` |
| 智能路由 | 仅文字 → 文生图接口；图片+文字 → 图像编辑接口 |
| 轮询策略 | 2 秒间隔，最多 60 次，状态: PENDING → RUNNING → SUCCEEDED / FAILED |
| 空状态 | 引导卡片展示功能说明和示例提示词，点击可快速填充 |
| 尺寸选择 | 支持 1:1 (1024×1024)、9:16 (720×1280)、16:9 (1280×720) |
| 结果操作 | 下载图片、保存到图库、继续编辑（将结果图作为编辑源图） |
| UI 风格 | 暗色主题，紫色渐变主色调，聊天气泡式交互 |

### 2.20 图片评论

| 项目 | 说明 |
|------|------|
| 位置 | 图片详情页右侧信息面板底部（点赞区域之后，用分割线分隔） |
| 结构 | 两级评论：顶级评论 + 回复（仅两级，不无限嵌套） |
| 发表评论 | 已登录用户可在文本框输入（最多 500 字），支持 Ctrl+Enter 提交 |
| 删除评论 | **评论作者**、**管理员**（`userRole === 'admin'`）、**图片上传者**均可删除，删除前弹窗二次确认 |
| 回复功能 | 点击「回复」展开内联文本框，支持回复顶级评论或某条回复 |
| 回复展示 | 默认显示前 3 条回复；点击「展开全部 N 条回复」加载完整回复列表（分页，每次加载 20 条） |
| 分页 | 顶级评论每页 10 条；加载更多（追加模式，不翻页） |
| 权限 | 未登录用户：可查看评论，发表/回复/删除按钮不显示；已登录用户：仅对本人评论显示删除按钮 |
| 后端接口 | 见接口汇总 — 评论相关；`/comment/add` 返回 `CommentVO`，已包含 `userInfo` 字段（当前评论者信息）和 `replyUserInfo` 字段（被回复者信息） |

### 2.21 用户跳转逻辑

| 项目 | 说明 |
|------|------|
| 场景 | 图片详情页点击上传者、搜索结果页点击用户、用户主页访问 |
| 逻辑 | 若目标用户 ID 等于当前登录用户 ID → 跳转个人中心 `/profile`；否则 → 跳转用户主页 `/user/:id` |
| UserProfileView | `onMounted` 检测如果是本人则 `router.replace('/profile')` |

---

## 3. 用户角色

| 角色 | userRole 值 | 权限 |
|------|-------------|------|
| 普通用户 | `user` | 浏览图库、上传图片、管理自己的内容、个人中心 |
| 管理员 | `admin` | 所有权限 + 用户管理 + 图片管理 + 空间管理 |

---

## 4. 数据模型

### LoginUserVO（登录用户视图）

```typescript
interface LoginUserVO {
  id?: number
  userAccount?: string
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string    // 'user' | 'admin'
  createTime?: string
  updateTime?: string
}
```

### UserVO（用户公开视图）

```typescript
interface UserVO {
  id?: number
  userAccount?: string
  userName?: string
  userAvatar?: string
  userProfile?: string
  userRole?: string
  createTime?: string
}
```

### PictureVO（图片视图）

```typescript
interface PictureVO {
  id?: number
  url?: string
  thumbnailUrl?: string
  name?: string
  introduction?: string
  category?: string
  tags?: string[]
  picWidth?: number
  picHeight?: number
  picScale?: number
  picSize?: number
  picFormat?: string
  picColor?: string
  spaceId?: number
  userId?: number
  user?: UserVO
  createTime?: string
  editTime?: string
  updateTime?: string
}
```

### SpaceVO（空间视图）

```typescript
interface SpaceVO {
  id?: number
  spaceName?: string
  spaceLevel?: number
  maxCount?: number
  maxSize?: number
  totalCount?: number
  totalSize?: number
  userId?: number
  user?: UserVO
  createTime?: string
  editTime?: string
  updateTime?: string
}
```

---

## 5. 全局导航

### 5.1 SidePanel

| 项目 | 说明 |
|------|------|
| 位置 | 左侧浮动面板（56px 宽，可拖拽、可收缩） |
| 头像图标 | 已登录 → 跳转 `/profile`；未登录 → 跳转 `/login` |
| 首页图标 | 跳转 `/` |
| Logo | 📷 图标，连续 5 次点击 → 管理员入口 |
| AI 助手图标 | 已登录 → 打开 AI 创意助手对话框 |
| 空间管理图标 | 仅管理员可见，跳转 `/admin/space-manage` |
| 退出按钮 | 仅已登录时显示 |

### 5.2 GlobalHeader（顶部导航栏）

| 项目 | 说明 |
|------|------|
| 位置 | 页面顶部，BasicLayout 内 |
| 品牌 | Logo + 站名「图库」 |
| 导航链接 | 首页、上传图片（需登录） |
| 管理员链接 | 用户管理、图片管理、空间管理（仅 admin 可见） |
| AI 按钮 | 右侧 AI 创意助手按钮（登录后可见），点击打开 AiDialog |
| 搜索框 | 回车跳转 `/search?q=关键词` |
| 用户菜单 | 头像下拉：个人中心、退出登录 |

---

## 6. 路由配置

| 路由 | 组件 | meta | 说明 |
|------|------|------|------|
| `/login` | LoginView | `{ guest: true }` | 已登录重定向至首页 |
| `/register` | RegisterView | `{ guest: true }` | 已登录重定向至首页 |
| `/` | BasicLayout → HomeView | — | 公共图库首页 |
| `/picture/upload` | BasicLayout → PictureUploadView | `{ requiresAuth: true }` | 图片上传（支持 `?spaceId=` 私有空间上传） |
| `/picture/edit/:id` | BasicLayout → PictureEditView | `{ requiresAuth: true }` | 图片编辑 |
| `/picture/:id` | BasicLayout → PictureDetailView | — | 图片详情 |
| `/search` | BasicLayout → SearchView | — | 搜索页 |
| `/user/:id` | BasicLayout → UserProfileView | — | 用户主页 |
| `/profile` | BasicLayout → ProfileView | — | 个人中心 |
| `/admin/user-manage` | BasicLayout → UserManageView | `{ requiresAdmin: true }` | 管理员用户管理 |
| `/admin/picture-manage` | BasicLayout → PictureManageView | `{ requiresAdmin: true }` | 管理员图片管理 |
| `/admin/space-manage` | BasicLayout → SpaceManageView | `{ requiresAdmin: true }` | 管理员空间管理 |

---

## 7. 后端接口汇总

### 用户相关

| 接口 | 方法 | 路径 |
|------|------|------|
| 用户登录 | POST | `/api/user/login` |
| 用户注册 | POST | `/api/user/register` |
| 退出登录 | GET | `/api/user/logout` |
| 获取当前用户 | GET | `/api/user/get/login` |
| 分页查询用户 | POST | `/api/user/list/page/vo` |
| 添加用户 | POST | `/api/user/add` |
| 更新用户 | POST | `/api/user/update` |
| 删除用户 | POST | `/api/user/delete` |
| 查询用户 VO | GET | `/api/user/get/vo` |

### 文件相关

| 接口 | 方法 | 路径 |
|------|------|------|
| 上传文件 | POST | `/api/file/test/upload` |

### 图片相关

| 接口 | 方法 | 路径 |
|------|------|------|
| 分页查询图片（缓存） | POST | `/api/picture/list/page/vo/cache` |
| 分页查询图片 | POST | `/api/picture/list/page/vo` |
| 获取图片详情 | GET | `/api/picture/get/vo` |
| 获取标签分类 | GET | `/api/picture/tag_category` |
| 上传图片（文件） | POST | `/api/picture/upload` |
| 上传图片（URL） | POST | `/api/picture/upload/url` |
| 编辑图片 | POST | `/api/picture/edit` |
| 删除图片 | POST | `/api/picture/delete` |
| 审核图片 | POST | `/api/picture/review` |
| 以图搜图 | POST | `/api/picture/search/picture` |
| 颜色搜索 | POST | `/api/picture/search/color` |
| AI 智能标签 | POST | `/api/picture/auto_tag` |
| 文字生图（创建任务） | POST | `/api/picture/text2image/create_task` |
| 文字生图（查询任务） | GET | `/api/picture/text2image/get_task` |
| 图片编辑（创建任务） | POST | `/api/picture/image_edit/create_task` |
| 图片编辑（查询任务） | GET | `/api/picture/image_edit/get_task` |

### 空间相关

| 接口 | 方法 | 路径 |
|------|------|------|
| 分页查询空间 VO | POST | `/api/space/list/page/vo` |
| 分页查询空间（管理员） | POST | `/api/space/list/page` |
| 获取空间详情 | GET | `/api/space/get/vo` |
| 创建空间 | POST | `/api/space/add` |
| 编辑空间 | POST | `/api/space/edit` |
| 更新空间（管理员） | POST | `/api/space/update` |
| 删除空间 | POST | `/api/space/delete` |
| 获取空间等级列表 | GET | `/api/space/list/level` |

### 评论相关

| 接口 | 方法 | 路径 |
|------|------|------|
| 发表评论/回复 | POST | `/api/comment/add` |
| 删除评论 | POST | `/api/comment/delete` |
| 分页获取顶级评论 | GET | `/api/comment/list` |
| 分页获取回复 | GET | `/api/comment/reply/list` |

### 搜索相关

| 接口 | 方法 | 路径 |
|------|------|------|
| 搜索历史 | GET | `/api/search/history` |
| 删除搜索历史 | POST | `/api/search/history/delete` |
| 热门关键词 | GET | `/api/search/hot` |

---

## 8. 非功能需求

- **响应式**: 适配桌面端（≥ 1024px）和平板（≥ 768px），移动端可降级
- **无障碍**: 表单正确关联 label，按钮有明确文本，对比度 ≥ 4.5:1
- **安全**: 不在前端存储明文密码，XSS 防护，CSRF 保护（Cookie + `withCredentials`）
- **性能**: 首屏加载 < 3s，路由级懒加载，图片使用 `thumbnailUrl` + `loading="lazy"`
- **错误处理**: 40100 静默拒绝（由业务层/路由守卫决定跳转），其他错误全局 `message.error`
- **ID 精度**: 雪花 ID 使用字符串传递，避免 JavaScript `Number` 精度丢失
