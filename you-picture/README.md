# 优图（YouPicture）

面向个人与团队的图片管理与分享平台，支持私有空间、团队协作与公共图库三种场景，集成 AI 辅助能力。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.6-brightgreen)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D)](https://vuejs.org/)
[![JDK](https://img.shields.io/badge/JDK-1.8+-orange)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/license-MIT-blue)](./LICENSE)

---

## 目录

- [核心功能](#核心功能)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [接口文档](#接口文档)
- [配置说明](#配置说明)
- [安全设计](#安全设计)

---

## 核心功能

### 用户系统
- **注册 / 登录** — 基于 Sa-Token + Redis Session，Cookie 有效期 30 天
- **角色体系** — `user`（普通用户）/ `admin`（管理员），支持自定义 AOP 权限注解
- **个人中心** — 修改昵称、头像、简介；查看他人主页

### 图片管理
- **上传方式** — 本地文件上传 + URL 远程抓取，模板方法模式统一处理流程
- **元数据管理** — 名称、分类、标签、简介，支持批量编辑
- **图片审核** — 公共图库图片需管理员审核通过方可公开；私有空间自动过审
- **AI 能力** — 扩图、文生图、图像编辑、自动标签识别（阿里云 DashScope）

### 空间协作
- **空间类型** — 私有空间（个人图库）/ 团队空间（协同管理）
- **三级配额** — 普通版（100 张 / 100 MB）、专业版（1000 张 / 1000 MB）、旗舰版（10000 张 / 10000 MB）
- **成员角色** — 浏览者 / 编辑者 / 管理员，细粒度权限矩阵
- **入驻流程** — 管理员直接添加 或 用户申请 → 管理员审批

### 社交互动
- **评论系统** — 两级嵌套（顶级评论 + 回复），支持分页查询
- **点赞系统** — 点赞 / 取消点赞，事务性更新计数

### 数据分析
- 空间使用概览（容量、图片数、上传趋势）
- 分类 / 标签 / 大小分布统计
- 用户上传行为分析
- 空间排行

### 搜索
- 图片、用户、空间多维模糊搜索
- 搜索历史自动记录（每人每类型保留最近 20 条）
- 热搜词排行（Redis ZSet 实现）

---

## 技术栈

### 后端

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 基础框架 | Spring Boot | 2.7.6 | Web 应用框架 |
| ORM | MyBatis-Plus | 3.5.9 | 含分页插件、逻辑删除、代码生成 |
| 数据库 | MySQL | 8.0+ | 主数据库，ShardingSphere 分表 |
| 缓存 | Redis | 6.0+ | Session 持久化 + 接口缓存 + 热词 |
| 本地缓存 | Caffeine | 3.1.8 | 双层缓存第一级，5 分钟 LRU |
| 对象存储 | 腾讯云 COS | 5.6.227 | 图片存储与缩略图处理 |
| 权限框架 | Sa-Token | 1.39.0 | 登录态 + 自定义空间鉴权域扩展 |
| 分库分表 | ShardingSphere JDBC | 5.2.0 | 按 spaceId 动态路由分表 |
| AI 服务 | 阿里云 DashScope | — | 扩图 / 文生图 / 图像编辑 / 多模态标签 |
| 图片搜索 | 360 以图搜图 | — | 聚合 API 实现相似图片搜索 |
| HTML 解析 | Jsoup | 1.15.3 | 批量图片抓取 |
| 工具库 | Hutool | 5.8.26 | 字符串、HTTP、JSON 工具 |
| 接口文档 | Knife4j | 4.4.0 | OpenAPI 2 自动生成 |
| AOP | Spring AOP | — | `@AuthCheck` 权限切面 |

### 前端

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 框架 | Vue | 3.5 | Composition API |
| 构建工具 | Vite | 8.0 | 极速 HMR |
| UI 组件库 | Ant Design Vue | 4.2 | 企业级 UI |
| 状态管理 | Pinia | 3.0 | Vue 3 官方推荐 |
| 路由 | Vue Router | 4.6 | SPA 路由 |
| HTTP | Axios | 1.15 | 请求封装与拦截 |
| 图表 | ECharts | 6.0 | 空间数据分析可视化 |
| 语言 | TypeScript | 6.0 | 类型安全 |

---

## 系统架构

```
Browser / Client
       │
  ┌────▼──────────────────────────────────────────┐
  │              Spring Boot 应用                   │
  │                                                │
  │  ┌──────────────────────────────────────────┐ │
  │  │            Controller 层                   │ │
  │  │  User  Picture  Space  Comment  Like      │ │
  │  │  Search  File  SpaceAnalyze  DataImport   │ │
  │  └──────────────────┬───────────────────────┘ │
  │  ┌──────────────────▼───────────────────────┐ │
  │  │           AOP / Filter 层                  │ │
  │  │  @AuthCheck  SaTokenConfigure             │ │
  │  │  SaSpaceCheckPermission                   │ │
  │  └──────────────────┬───────────────────────┘ │
  │  ┌──────────────────▼───────────────────────┐ │
  │  │             Service 层                     │ │
  │  │  业务逻辑编排、事务管理、缓存策略            │ │
  │  └──────────────────┬───────────────────────┘ │
  │  ┌──────────────────▼───────────────────────┐ │
  │  │           Manager / API 层                 │ │
  │  │  CosManager  FileManager                  │ │
  │  │  PictureUploadTemplate（模板方法）          │ │
  │  │  AliYunAiApi  SoImageSearchApi            │ │
  │  └──────────────────┬───────────────────────┘ │
  │  ┌──────────────────▼───────────────────────┐ │
  │  │            Mapper / DAO 层                 │ │
  │  │  MyBatis-Plus Mapper + XML                │ │
  │  └──────────────────────────────────────────┘ │
  └───────────────────┬──────────────────────────--┘
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
       MySQL       Redis     腾讯云 COS
   (ShardingSphere)
```

### 关键设计模式

- **模板方法模式** — `PictureUploadTemplate` 定义上传骨架，`FilePictureUpload` / `UrlPictureUpload` 实现具体策略
- **AOP 切面** — `@AuthCheck` 注解驱动角色校验，`AuthInterceptor` 统一拦截
- **双层缓存** — Caffeine（本地 LRU）→ Redis（分布式）→ MySQL，防雪崩随机过期
- **异步解耦** — 搜索历史记录、COS 文件清理通过 `CompletableFuture` 异步执行

---

## 快速开始

### 环境要求

| 依赖 | 版本 | 安装指引 |
|------|------|----------|
| JDK | 1.8+ | [AdoptOpenJDK](https://adoptium.net/) |
| Maven | 3.6+ | [Maven 官方](https://maven.apache.org/) |
| Node.js | 18+ | [Node.js 官方](https://nodejs.org/) |
| MySQL | 8.0+ | [MySQL 官方](https://dev.mysql.com/) |
| Redis | 6.0+ | [Redis 官方](https://redis.io/) |

### 1. 克隆仓库

```bash
git clone https://github.com/647813412/java-framework-projects.git
cd java-framework-projects/you-picture
```

### 2. 初始化数据库

```bash
mysql -u root -p < you-picture-backend/sql/picture.sql
```

### 3. 配置环境变量

```bash
# 数据库
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=picture
export DB_USERNAME=root
export DB_PASSWORD=your_db_password

# 密码加密
export PASSWORD_SALT=your_salt_value
export DEFAULT_PASSWORD=12345678

# 腾讯云 COS（可选，用于图片存储）
export COS_SECRET_ID=your_cos_secret_id
export COS_SECRET_KEY=your_cos_secret_key
export COS_REGION=ap-guangzhou
export COS_BUCKET=your_bucket_name

# 阿里云 AI（可选，用于 AI 功能）
export ALIYUN_AI_API_KEY=your_aliyun_api_key
export AI_DASHSCOPE_API_KEY=your_dashscope_api_key
```

### 4. 启动后端

```bash
cd you-picture-backend
mvn spring-boot:run
```

访问地址：
- **API 服务**：`http://localhost:8080/api`
- **接口文档**（Knife4j）：`http://localhost:8080/api/doc.html`

### 5. 启动前端

```bash
cd picture/frontend
npm install
npm run dev
```

默认在 `http://localhost:5173` 启动开发服务器。

---

## 项目结构

```
you-picture/
├── README.md
│
├── you-picture-backend/                    # Spring Boot 后端
│   ├── pom.xml                             # Maven 配置
│   ├── sql/
│   │   └── picture.sql                     # 数据库建表脚本
│   ├── docs/
│   │   └── 需求文档与技术方案.md              # 详细设计文档
│   └── src/main/
│       ├── java/com/wuyou/youpicturebackend/
│       │   ├── YouPictureBackendApplication.java   # 启动入口
│       │   ├── annotation/                 # 自定义注解（@AuthCheck）
│       │   ├── aop/                        # AOP 切面（AuthInterceptor）
│       │   ├── api/                        # 第三方 API 封装
│       │   │   ├── aliyunai/              # 阿里云 DashScope AI
│       │   │   └── imagesearch/           # 以图搜图
│       │   ├── common/                     # 通用类（BaseResponse、PageRequest）
│       │   ├── config/                     # Spring 配置（CORS、COS、MyBatis-Plus）
│       │   ├── controller/                 # 控制器层（10 个 Controller）
│       │   ├── exception/                  # 全局异常处理
│       │   ├── manager/                    # 业务管理器
│       │   │   ├── auth/                  # Sa-Token 空间鉴权扩展
│       │   │   ├── sharding/              # 动态分表管理
│       │   │   └── upload/                # 图片上传模板（策略模式）
│       │   ├── mapper/                     # MyBatis Mapper 接口
│       │   ├── model/                      # 数据模型
│       │   │   ├── constant/              # 常量定义
│       │   │   ├── dto/                   # 数据传输对象
│       │   │   ├── entity/                # 数据库实体
│       │   │   ├── enums/                 # 枚举类
│       │   │   └── vo/                    # 视图对象
│       │   ├── service/                    # 业务接口
│       │   │   └── impl/                  # 业务实现
│       │   └── utils/                      # 工具类
│       └── resources/
│           ├── application.yml             # 主配置文件
│           ├── biz/
│           │   └── spaceUserAuthConfig.json # 空间权限映射配置
│           └── mapper/                     # MyBatis XML 映射
│
└── picture/                                # Vue 3 前端
    ├── DESIGN.md                           # 设计系统文档（Pinterest 风格）
    ├── frontend/
    │   ├── package.json                    # 依赖与脚本
    │   ├── vite.config.ts                  # Vite 构建配置
    │   ├── index.html                      # 入口 HTML
    │   ├── openapi.config.js               # API 代码生成配置
    │   ├── src/
    │   │   ├── main.ts                     # 应用入口
    │   │   ├── App.vue                     # 根组件
    │   │   ├── request.ts                  # Axios 请求封装
    │   │   ├── api/                        # 接口层（按模块拆分）
    │   │   ├── components/                 # 通用组件
    │   │   │   ├── GlobalHeader.vue       # 全局导航栏
    │   │   │   ├── CommentSection.vue     # 评论区
    │   │   │   ├── AiDialog.vue           # AI 功能弹窗
    │   │   │   └── SidePanel.vue          # 侧边面板
    │   │   ├── layouts/
    │   │   │   └── BasicLayout.vue        # 基础布局
    │   │   ├── router/                     # 路由配置
    │   │   ├── stores/                     # Pinia 状态管理
    │   │   ├── views/                      # 页面视图
    │   │   │   ├── HomeView.vue           # 首页（瀑布流）
    │   │   │   ├── PictureDetailView.vue  # 图片详情
    │   │   │   ├── ProfileView.vue        # 个人中心
    │   │   │   ├── SpaceDetailView.vue    # 空间详情
    │   │   │   ├── AiImageView.vue        # AI 图片生成
    │   │   │   ├── SearchView.vue         # 搜索页面
    │   │   │   └── admin/                 # 管理后台
    │   │   └── assets/                     # 静态资源
    │   └── public/                         # 公共资源
    └── docs/                               # 前端设计文档
```

---

## 接口文档

启动后端后，访问 Knife4j 在线文档：`http://localhost:8080/api/doc.html`

### 接口概览

| 模块 | 路径前缀 | 主要接口 |
|------|---------|---------|
| 用户 | `/api/user` | 注册、登录、登出、信息修改、管理员 CRUD |
| 图片 | `/api/picture` | 上传（文件/URL）、编辑、删除、分页查询、审核、AI 扩图/文生图/编辑/标签 |
| 空间 | `/api/space` | 创建、删除、更新、分页查询、级别列表 |
| 空间成员 | `/api/spaceUser` | 添加、移除、查询、权限编辑、申请、审批、退出 |
| 评论 | `/api/comment` | 发表、回复、删除、分页查询 |
| 点赞 | `/api/like` | 点赞/取消、状态查询、总数、我的点赞列表 |
| 搜索 | `/api/search` | 历史查询、历史删除、历史清空、热搜词 |
| 文件 | `/api/file` | 头像上传（文件/URL） |
| 数据分析 | `/api/space/analyze` | 使用情况、分类、标签、大小、用户分析、排行 |
| 数据导入 | `/api/picture/data` | 批量导入公共图库数据（管理员） |

---

## 配置说明

所有敏感配置均通过环境变量注入，**不硬编码**在配置文件中：

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `DB_HOST` | MySQL 主机 | `localhost` |
| `DB_PORT` | MySQL 端口 | `3306` |
| `DB_NAME` | 数据库名 | `picture` |
| `DB_USERNAME` | 数据库用户 | `root` |
| `DB_PASSWORD` | 数据库密码 | *必填* |
| `PASSWORD_SALT` | 密码加密盐值 | `wuyou` |
| `DEFAULT_PASSWORD` | 管理员创建用户默认密码 | `12345678` |
| `COS_SECRET_ID` | 腾讯云 COS SecretId | — |
| `COS_SECRET_KEY` | 腾讯云 COS SecretKey | — |
| `COS_REGION` | COS 地域 | `ap-guangzhou` |
| `COS_BUCKET` | COS 存储桶名称 | — |
| `ALIYUN_AI_API_KEY` | 阿里云 DashScope API Key | — |
| `AI_DASHSCOPE_API_KEY` | Spring AI DashScope Key | — |

> 生产环境务必更换 `PASSWORD_SALT` 和 `DEFAULT_PASSWORD`。

---

## 安全设计

| 层面 | 措施 |
|------|------|
| 密码存储 | MD5 + 盐值加密（可通过配置替换盐值） |
| 登录态 | Sa-Token + Redis Session，30 天过期 |
| 接口鉴权 | `@AuthCheck(mustRole)` 注解 + AOP 切面，运行时校验角色 |
| 空间权限 | Sa-Token 自定义鉴权域 `StpKit.SPACE`，支持 `viewer / editor / admin` 三级细粒度控制 |
| 数据隔离 | 私有空间图片仅空间成员可见；团队空间基于角色鉴权 |
| 防爬虫 | 分页接口 `pageSize` 最大 20 |
| 文件安全 | 上传限制 10 MB；URL 上传校验 HTTP/HTTPS 协议 |
| SQL 注入 | MyBatis-Plus 参数化查询；搜索使用 `LIKE` 模糊匹配 |

---

## License

MIT License

Copyright (c) 2025 无忧
