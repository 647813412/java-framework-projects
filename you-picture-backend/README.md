# 优图（YouPicture）

一个面向个人与团队的图片管理与分享平台。

> 技术栈：Java 8 / Spring Boot 2.7.6 / MyBatis-Plus 3.5.9 / MySQL / Redis / 腾讯云 COS / ShardingSphere / Sa-Token / Caffeine

## 功能模块

### 用户模块
- 用户注册/登录（Sa-Token + Redis Session，Cookie 30 天有效）
- 个人信息管理（昵称、头像、简介）
- 管理员用户管理（CRUD）
- 角色体系：`user` / `admin`

### 图片模块
- 文件上传 & URL 抓取上传（模板方法模式）
- 图片元数据管理（名称、分类、标签、简介）
- AI 辅助：扩图、文生图、图像编辑、自动打标签（阿里云 DashScope）
- 图片审核（公共图库需管理员审核）
- 颜色相似搜索 & 以图搜图

### 空间模块
- 私有空间 / 团队空间
- 三级空间配额（普通版 100 张 / 专业版 1000 张 / 旗舰版 10000 张）
- 空间成员管理（浏览者 / 编辑者 / 管理员）
- 申请-审批入驻流程
- 细粒度权限控制（Sa-Token 扩展空间鉴权域）

### 社交互动
- 评论（两级嵌套，顶级 + 回复）
- 点赞/取消点赞（事务性更新）

### 数据分析
- 空间使用情况分析
- 分类/标签/大小分布/用户上传统计
- 空间排行

### 搜索
- 图片/用户/空间模糊搜索
- 搜索历史（个人最近 20 条）
- 热词排行（Redis ZSet）

## 技术架构

```
Browser / Client
       │
  ┌────▼────────────────────────────────────┐
  │         Spring Boot 应用                 │
  │  ┌───────────────────────────────────┐  │
  │  │           Controller 层            │  │
  │  └──────────────┬────────────────────┘  │
  │  ┌──────────────▼────────────────────┐  │
  │  │         AOP / Filter 层            │  │
  │  │  @AuthCheck / Sa-Token 鉴权        │  │
  │  └──────────────┬────────────────────┘  │
  │  ┌──────────────▼────────────────────┐  │
  │  │           Service 层               │  │
  │  └──────────────┬────────────────────┘  │
  │  ┌──────────────▼────────────────────┐  │
  │  │         Manager / API 层           │  │
  │  │  COS / 阿里云 AI / 图片搜索         │  │
  │  └──────────────┬────────────────────┘  │
  │  ┌──────────────▼────────────────────┐  │
  │  │           Mapper / DAO 层          │  │
  │  └──────────────┬────────────────────┘  │
  └─────────────────┼──────────────────────-┘
                    │
         ┌──────────┼──────────┐
         ▼          ▼          ▼
      MySQL       Redis    腾讯云 COS
    (ShardingSphere)
```

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- 腾讯云 COS（可选，用于图片存储）

### 1. 初始化数据库

```bash
mysql -u root -p < sql/picture.sql
```

### 2. 配置环境变量

```bash
# 数据库配置
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=picture
export DB_USERNAME=root
export DB_PASSWORD=your_password

# 密码加密盐值（生产环境请更换）
export PASSWORD_SALT=your_salt_value

# 管理员创建用户时的默认密码
export DEFAULT_PASSWORD=12345678

# 阿里云 AI（可选）
export ALIYUN_AI_API_KEY=your_aliyun_api_key
export AI_DASHSCOPE_API_KEY=your_dashscope_api_key

# 腾讯云 COS（可选）
export COS_SECRET_ID=your_secret_id
export COS_SECRET_KEY=your_secret_key
export COS_REGION=ap-guangzhou
export COS_BUCKET=your_bucket_name
```

### 3. 启动后端

```bash
cd you-picture-backend
mvn spring-boot:run
```

服务启动后访问：
- API 地址：`http://localhost:8080/api`
- 接口文档（Knife4j）：`http://localhost:8080/api/doc.html`

### 4. 启动前端

```bash
cd picture/frontend
npm install
npm run dev
```

## 项目结构

```
you-picture-backend/
├── src/main/java/com/wuyou/youpicturebackend/
│   ├── annotation/          # 自定义注解（@AuthCheck）
│   ├── api/                 # 第三方 API 封装（阿里云 AI、图片搜索）
│   ├── common/              # 通用类（BaseResponse、PageRequest 等）
│   ├── config/              # Spring 配置
│   ├── controller/          # 控制器层
│   ├── exception/           # 异常处理
│   ├── manager/             # 业务管理器（COS、文件上传、权限）
│   ├── mapper/              # MyBatis Mapper
│   ├── model/               # 数据模型
│   │   ├── constant/        # 常量
│   │   ├── dto/             # 数据传输对象
│   │   ├── entity/          # 实体类
│   │   ├── enums/           # 枚举
│   │   └── vo/              # 视图对象
│   └── service/             # 服务层
├── src/main/resources/
│   ├── application.yml      # 主配置
│   ├── biz/                 # 业务配置（权限映射）
│   └── mapper/              # MyBatis XML
├── sql/                     # 数据库脚本
├── docs/                    # 文档
└── pom.xml
```

## 接口文档

在线文档：启动后访问 `http://localhost:8080/api/doc.html`

### 主要接口

| 模块 | 路径前缀 | 说明 |
|------|---------|------|
| 用户 | `/api/user` | 注册、登录、信息管理 |
| 图片 | `/api/picture` | 上传、编辑、删除、查询、AI |
| 空间 | `/api/space` | 创建、管理、配额 |
| 空间成员 | `/api/spaceUser` | 成员管理、申请审批 |
| 评论 | `/api/comment` | 发表、回复、查询 |
| 点赞 | `/api/like` | 点赞/取消、状态查询 |
| 搜索 | `/api/search` | 历史、热词 |
| 文件 | `/api/file` | 头像上传 |
| 数据分析 | `/api/space/analyze` | 空间使用分析 |
| 数据导入 | `/api/picture/data` | 批量导入 |

## 安全说明

- 密码使用 MD5 + 盐值加密存储
- 接口权限基于 Sa-Token + 自定义 AOP 注解
- 空间权限细粒度控制（浏览者 / 编辑者 / 管理员）
- 敏感配置通过环境变量注入，不硬编码在配置文件中
- 上传文件大小限制 10MB，URL 协议校验

## License

MIT
