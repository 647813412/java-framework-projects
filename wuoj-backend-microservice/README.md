# wuoj-backend-microservice

> 基于 Spring Cloud Alibaba 微服务架构的在线判题系统（Online Judge）后端

## 📖 项目简介

**wuoj-backend-microservice** 是一个基于 Spring Cloud Alibaba 微服务架构的在线编程判题系统后端。项目采用 Maven 多模块管理，集成了服务注册与发现、API 网关、负载均衡、代码沙箱判题、分布式会话等核心功能，可作为微服务学习和 OJ 系统开发的参考项目。

## 🏗️ 系统架构

```
┌─────────────────────────────────────────────────┐
│                   Client (前端)                   │
└─────────────────────┬───────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────┐
│           API Gateway (8101)                     │
│         wuoj-backend-gateway                     │
│     Spring Cloud Gateway + CORS + Auth           │
└────────┬──────────┬──────────┬──────────────────┘
         │          │          │
         ▼          ▼          ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│  User    │ │ Question │ │  Judge   │
│ Service  │ │ Service  │ │ Service  │
│  :8102   │ │  :8103   │ │  :8104   │
└────┬─────┘ └────┬─────┘ └────┬─────┘
     │            │            │
     └────────────┼────────────┘
                  │
     ┌────────────┼────────────┐
     ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│  MySQL   │ │  Redis   │ │ RabbitMQ │
└──────────┘ └──────────┘ └──────────┘
```

## 📦 模块说明

| 模块 | 说明 | 端口 |
|------|------|------|
| `wuoj-backend-common` | 公共模块：通用工具类、异常处理、注解、常量定义 | - |
| `wuoj-backend-model` | 模型模块：实体类、DTO、VO、枚举、代码沙箱模型 | - |
| `wuoj-backend-gateway` | 网关模块：统一入口、路由转发、CORS 跨域、权限过滤 | 8101 |
| `wuoj-backend-user-service` | 用户服务：注册、登录、用户管理、Session 管理 | 8102 |
| `wuoj-backend-question-service` | 题目服务：题目管理、题目提交管理 | 8103 |
| `wuoj-backend-judge-service` | 判题服务：代码沙箱调用、判题逻辑、结果判定 | 8104 |
| `wuoj-backend-service-client` | 服务调用客户端：Feign 接口声明 | - |

## 🛠️ 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 编程语言 |
| Spring Boot | 2.6.13 | 基础框架 |
| Spring Cloud | 2021.0.5 | 微服务框架 |
| Spring Cloud Alibaba | 2021.0.5.0 | 微服务生态（Nacos + Sentinel） |
| Nacos | - | 服务注册与发现 |
| Sentinel | - | 流量控制、熔断降级 |
| Spring Cloud Gateway | - | API 网关 |
| MyBatis-Plus | 3.5.2 | ORM 框架 |
| MySQL | - | 关系型数据库 |
| Redis | - | 缓存 & 分布式 Session |
| RabbitMQ | - | 消息队列（异步判题） |
| Knife4j | - | API 文档（Swagger 增强） |
| Hutool | 5.8.8 | Java 工具库 |
| EasyExcel | 3.1.1 | Excel 读写 |
| Lombok | - | 简化代码 |

## 🚀 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+
- Nacos 2.x

### 本地开发

1. **克隆项目**

```bash
git clone <your-repo-url>
cd java-framework-projects/wuoj-backend-microservice
```

2. **配置数据库**

创建 `wuoj` 数据库，导入初始化 SQL 脚本（如有），修改各服务模块的 `application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/wuoj
    username: your-db-username
    password: your-db-password
```

3. **配置 Redis**

确保 Redis 服务已启动，默认连接 `localhost:6379`。

4. **配置 RabbitMQ**

确保 RabbitMQ 服务已启动，默认连接 `localhost:5672`。

5. **配置 Nacos**

确保 Nacos 服务已启动（默认 `127.0.0.1:8848`），或修改各服务的 Nacos 连接地址。

6. **启动服务**

按照以下顺序启动各模块：

```bash
# 1. 启动网关
cd wuoj-backend-gateway
mvn spring-boot:run

# 2. 启动用户服务
cd wuoj-backend-user-service
mvn spring-boot:run

# 3. 启动题目服务
cd wuoj-backend-question-service
mvn spring-boot:run

# 4. 启动判题服务
cd wuoj-backend-judge-service
mvn spring-boot:run
```

7. **访问 API 文档**

- 用户服务文档：`http://localhost:8102/api/user/doc.html`
- 题目服务文档：`http://localhost:8103/api/question/doc.html`
- 判题服务文档：`http://localhost:8104/api/judge/doc.html`
- 网关聚合文档：`http://localhost:8101/doc.html`

## 📋 API 路由表

| 路径前缀 | 目标服务 | 说明 |
|----------|----------|------|
| `/api/user/**` | wuoj-backend-user-service | 用户相关接口 |
| `/api/question/**` | wuoj-backend-question-service | 题目相关接口 |
| `/api/judge/**` | wuoj-backend-judge-service | 判题相关接口 |

## ⚙️ 核心功能

### 用户服务
- 用户注册（MD5 加盐加密）
- 用户登录 / 注销
- 分布式 Session（基于 Redis）
- 用户信息 CRUD
- 角色权限控制（用户 / 管理员）

### 题目服务
- 题目 CRUD
- 题目提交管理
- 分页查询与条件筛选
- 题目判题配置（时间限制、内存限制、测试用例）

### 判题服务
- 代码沙箱执行（支持远程 / 第三方沙箱）
- 判题策略模式（支持多语言扩展）
- 异步消息队列处理
- 判题结果解析与存储

### 网关模块
- 统一路由转发
- CORS 跨域配置
- 内部接口保护（`/inner/**` 路径禁止外部访问）
- 全局鉴权过滤

## 🔐 安全说明

⚠️ **重要提示：**

- 所有配置文件中的密码、密钥均为示例占位符，实际部署前请替换为真实值
- `wuoj-backend-judge-service` 中的 `AUTH_REQUEST_SECRET` 请配置真实的鉴权密钥
- `wuoj-backend-gateway` 中的鉴权逻辑需要根据实际需求完善（如集成 JWT）
- 生产环境请启用 HTTPS，关闭 Swagger/Knife4j 文档
- 建议将敏感配置移至 Nacos 配置中心或环境变量中管理

## 📂 项目结构

```
wuoj-backend-microservice
├── pom.xml                                    # 父 POM（Maven 多模块管理）
├── README.md
├── src/main/resources
│   └── application.properties                 # 主配置（Nacos、Sentinel 等）
├── wuoj-backend-common/                       # 公共模块
│   └── src/main/java/.../
│       ├── annotation/                        # 自定义注解（@AuthCheck）
│       ├── common/                            # 通用响应、分页请求等
│       ├── constant/                          # 常量定义
│       ├── exception/                         # 全局异常处理
│       └── utils/                             # 工具类
├── wuoj-backend-model/                        # 模型模块
│   └── src/main/java/.../
│       ├── codesandbox/                       # 代码沙箱请求/响应模型
│       ├── dto/                               # 数据传输对象
│       ├── entity/                            # 数据库实体
│       ├── enums/                             # 枚举类
│       └── vo/                                # 视图对象
├── wuoj-backend-gateway/                      # 网关模块
│   └── src/main/java/.../
│       ├── config/                            # CORS 配置
│       └── filter/                            # 全局过滤器
├── wuoj-backend-user-service/                 # 用户服务
│   └── src/main/java/.../
│       ├── controller/                        # REST 控制器
│       ├── mapper/                            # MyBatis Mapper
│       └── service/                           # 业务逻辑层
├── wuoj-backend-question-service/             # 题目服务
│   └── src/main/java/.../
│       ├── controller/
│       ├── mapper/
│       └── service/
├── wuoj-backend-judge-service/                # 判题服务
│   └── src/main/java/.../
│       ├── controller/
│       ├── judge/
│       │   ├── codesandbox/                   # 代码沙箱（工厂+策略模式）
│       │   └── strategy/                      # 判题策略
│       └── rabbitMq/                          # RabbitMQ 配置
└── wuoj-backend-service-client/               # Feign 客户端
    └── src/main/java/.../
        └── service/                           # Feign 接口声明
```

## 🎨 设计模式

- **工厂模式**：`CodeSandboxFactory` 根据配置创建不同的代码沙箱实例
- **策略模式**：`JudgeStrategy` 支持不同编程语言的判题策略
- **代理模式**：`CodeSandboxProxy` 对代码沙箱进行增强（日志、监控）
- **模板方法模式**：通用判题流程定义在 `DefaultJudgeStrategy`

## 📝 待办事项

- [ ] 集成 JWT 替代 Session 认证
- [ ] 完善 Sentinel 流控/降级规则
- [ ] 接入 Elasticsearch 实现题目搜索
- [ ] 完善单元测试和集成测试
- [ ] 实现 CI/CD 自动化部署
- [ ] 完善监控告警（Spring Boot Actuator + Prometheus）

## 📄 License

本项目仅用于学习交流目的。
