# Java Framework Projects

> 基于不同 Java 框架构建的项目集合，涵盖 Spring Boot、Spring Cloud Alibaba、Spring AI、MyBatis-Plus 等主流技术栈的完整实践案例。

## 🎯 仓库定位

本仓库用于集中存放和展示使用 **不同 Java 框架** 构建的独立项目。每个项目围绕特定的框架组合与技术场景，涵盖后端开发、微服务架构、AI 集成、数据处理等方向，适合作为学习参考或项目脚手架。

## 📂 项目列表

### 微服务架构

| 项目 | 说明 | 核心框架 |
|------|------|----------|
| [wuoj-backend-microservice](./wuoj-backend-microservice) | 在线判题系统（OJ）— 微服务后端 | Spring Boot 2.6 · Spring Cloud Alibaba 2021 · Nacos · Sentinel · Gateway · Redis · RabbitMQ |

### 全栈应用

| 项目 | 说明 | 核心框架 |
|------|------|----------|
| [you-picture](./you-picture) | 优图 — 图片管理与分享平台 | Spring Boot 2.7 · Vue 3 · MyBatis-Plus · Redis · COS · ShardingSphere |

## 🗂️ 仓库结构

```
java-framework-projects/
│
├── README.md                          ← 本文件
│
├── wuoj-backend-microservice/         # 微服务架构 · Spring Cloud Alibaba
│   ├── wuoj-backend-common/           # 公共模块
│   ├── wuoj-backend-model/            # 数据模型
│   ├── wuoj-backend-gateway/          # API 网关
│   ├── wuoj-backend-user-service/     # 用户服务
│   ├── wuoj-backend-question-service/ # 题目服务
│   ├── wuoj-backend-judge-service/    # 判题服务
│   ├── wuoj-backend-service-client/   # Feign 客户端
│   └── README.md
│
├── you-picture/                       # 全栈应用 · Spring Boot + Vue 3
│   ├── you-picture-backend/           # Spring Boot 后端
│   ├── picture/                       # Vue 3 前端
│   └── README.md
│
└── .gitignore
```

## 🛠️ 技术生态

本仓库涵盖的 Java 框架与中间件：

| 类别 | 框架 / 工具 |
|------|-------------|
| **核心框架** | Spring Boot 2.x / 3.x、Spring Cloud、Spring Cloud Alibaba、Spring AI |
| **微服务治理** | Nacos（注册/配置）、Sentinel（流控/熔断）、Gateway（网关）、OpenFeign |
| **ORM / 数据** | MyBatis-Plus、Spring Data JPA、ShardingSphere |
| **缓存与存储** | Redis、MySQL、PostgreSQL、Elasticsearch |
| **消息队列** | RabbitMQ、Kafka |
| **安全认证** | Spring Security、Sa-Token、JWT、OAuth2 |
| **AI / LLM** | Spring AI、Spring AI Alibaba、LangChain4j |
| **前端（全栈项目）** | Vue 3、React、Node.js |
| **构建工具** | Maven、Gradle |

> 💡 未来将持续加入更多框架组合的项目，每个项目都是独立可运行的完整实践。

## 🚀 使用方式

```bash
# 克隆仓库
git clone https://github.com/647813412/java-framework-projects.git

# 进入具体项目目录
cd wuoj-backend-microservice    # 微服务项目
# 或
cd you-picture                  # 全栈项目

# 按照各项目 README 中的指引运行
```

每个子项目包含独立的 README 文档，详细介绍了功能说明、技术架构、快速开始与环境配置。

## 📋 环境要求

| 工具 | 推荐版本 | 说明 |
|------|----------|------|
| JDK | 17 / 21 | 不同项目对 JDK 版本要求不同，详见各项目文档 |
| Maven | 3.8+ | Java 项目构建 |
| Gradle | 8+ | 部分项目使用 |
| Node.js | 18+ | 全栈项目前端构建 |
| Docker | 24+ | 微服务中间件快速部署 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 7.0+ | 缓存 / Session 共享 |

## 🤝 贡献

欢迎提交新的 Java 框架实践项目。添加新项目时请：

1. 在仓库根目录创建独立的项目文件夹
2. 项目内包含完整的 `README.md`
3. 更新本文件的「项目列表」
4. 确保敏感信息（密码、密钥、Token）已脱敏

## 📄 License

MIT
