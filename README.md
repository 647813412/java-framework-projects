# Java Framework Projects

基于 Java 生态的全栈项目实践集合，涵盖 Spring Boot 后端、Vue 前端、分布式中间件等技术栈的完整案例。

## 项目列表

| 项目 | 说明 | 技术栈 |
|------|------|--------|
| [you-picture](./you-picture) | 优图 — 图片管理与分享平台 | Spring Boot 2.7 + Vue 3 + MyBatis-Plus + Redis + COS + ShardingSphere |

## 目录结构

```
java-framework-projects/
├── you-picture/                       # 优图（全栈项目）
│   ├── you-picture-backend/           # Spring Boot 后端
│   ├── picture/                       # Vue 3 前端
│   └── README.md                      # 子项目文档
├── .gitignore
└── README.md
```

## 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 1.8+ | 后端运行环境 |
| Maven | 3.6+ | 后端构建工具 |
| Node.js | 18+ | 前端构建工具 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 缓存与 Session |

## 使用说明

每个子项目均包含独立的 README 文档，涵盖功能说明、技术架构、部署步骤与项目结构，请进入对应目录查阅。

## License

MIT
