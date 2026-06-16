# Java Framework Projects

一套基于 Java 生态的全栈项目集合，涵盖 Spring Boot 后端、Vue 前端、中间件集成等实践案例。

## 项目列表

### [优图（YouPicture）](./you-picture-backend)

一个面向个人与团队的图片管理与分享平台，采用 **Spring Boot 2.7 + MyBatis-Plus + MySQL + Redis + 腾讯云 COS** 技术栈构建。

**前端：** Vue 3 + Vite + Ant Design Vue + Pinia

**核心功能：**
- 用户注册/登录（Sa-Token + Redis Session）
- 图片上传与管理（本地文件 + URL 抓取）
- 私有空间 / 团队空间 / 公共图库
- 空间成员管理与细粒度权限控制
- AI 能力（扩图、文生图、自动标签）
- 评论、点赞、搜索
- 图片数据分析（空间使用情况、分类统计等）
- 分库分表（ShardingSphere 按空间动态分表）

**技术亮点：**
- 自定义 AOP 权限注解（`@AuthCheck` + Sa-Token 空间鉴权域扩展）
- 模板方法模式实现图片上传（文件上传 / URL 上传）
- Caffeine + Redis 双层缓存架构
- 阿里云 DashScope AI 集成
- 编程式事务保证数据一致性
- Knife4j 自动生成接口文档

---

## 开发环境要求

| 工具 | 版本 |
|------|------|
| JDK | 1.8+ |
| Maven | 3.6+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |

## 快速开始

每个子项目包含独立的 README 文档，请进入对应目录查看详细说明。

## 项目结构

```
java-framework-projects/
├── you-picture-backend/          # 优图后端（Spring Boot）
│   ├── src/                      # 源代码
│   ├── sql/                      # 数据库初始化脚本
│   ├── docs/                     # 需求文档与技术方案
│   └── README.md
├── picture/                      # 优图前端
│   ├── frontend/                 # Vue 3 前端应用
│   └── DESIGN.md                 # 设计系统文档
└── README.md
```

## License

MIT
