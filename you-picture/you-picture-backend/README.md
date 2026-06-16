# 优图后端（YouPicture Backend）

基于 Spring Boot 2.7.6 的图片管理平台后端服务。

> 完整项目文档请参阅 [you-picture/README.md](../README.md)

## 快速启动

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+

### 配置环境变量

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=picture
export DB_USERNAME=root
export DB_PASSWORD=your_db_password
export PASSWORD_SALT=your_salt_value
export DEFAULT_PASSWORD=12345678

# 可选：腾讯云 COS
export COS_SECRET_ID=your_secret_id
export COS_SECRET_KEY=your_secret_key
export COS_REGION=ap-guangzhou
export COS_BUCKET=your_bucket

# 可选：阿里云 AI
export ALIYUN_AI_API_KEY=your_api_key
```

### 初始化数据库

```bash
mysql -u root -p < sql/picture.sql
```

### 运行

```bash
mvn spring-boot:run
```

- API 服务：`http://localhost:8080/api`
- 接口文档（Knife4j）：`http://localhost:8080/api/doc.html`

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 2.7.6 | 基础框架 |
| MyBatis-Plus | 3.5.9 | ORM + 分页 + 逻辑删除 |
| MySQL | 8.0 | 主数据库 |
| Redis | 6.0 | Session + 热词 + 缓存 |
| Caffeine | 3.1.8 | 本地缓存（LRU） |
| ShardingSphere JDBC | 5.2.0 | 按 spaceId 动态分表 |
| Sa-Token | 1.39.0 | 权限认证 + 空间鉴权域扩展 |
| 腾讯云 COS | 5.6.227 | 对象存储 |
| 阿里云 DashScope | — | AI 扩图 / 文生图 / 标签 |
| Jsoup | 1.15.3 | HTML 解析 |
| Hutool | 5.8.26 | 通用工具库 |
| Knife4j | 4.4.0 | 接口文档 |

## 项目结构

```
you-picture-backend/
├── pom.xml
├── sql/picture.sql                      # 建表脚本
├── docs/需求文档与技术方案.md              # 详细设计文档
└── src/main/
    ├── java/com/wuyou/youpicturebackend/
    │   ├── annotation/                   # @AuthCheck
    │   ├── aop/                          # AuthInterceptor
    │   ├── api/                          # 阿里云 AI / 以图搜图
    │   ├── common/                       # BaseResponse、ResultUtils
    │   ├── config/                       # CORS、COS、MyBatis-Plus
    │   ├── controller/                   # 10 个 Controller
    │   ├── exception/                    # 全局异常处理
    │   ├── manager/                      # COS、上传模板、权限管理
    │   ├── mapper/                       # MyBatis Mapper
    │   ├── model/                        # DTO / Entity / VO / Enum
    │   ├── service/                      # 业务逻辑
    │   └── utils/                        # 工具类
    └── resources/
        ├── application.yml
        ├── biz/spaceUserAuthConfig.json   # 空间权限配置
        └── mapper/                        # MyBatis XML
```
