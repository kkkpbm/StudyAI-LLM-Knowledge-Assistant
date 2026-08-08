# 知识管理助手 - 后端 API

基于 Spring Boot 3.2.6 + MyBatis-Plus 构建的后端业务 API，提供用户认证、笔记管理、知识图谱、学习计划、复习提醒等功能。

## 功能模块

| 模块 | 端点 | 说明 |
|------|------|------|
| **用户认证** | `/api/auth/*` | 注册、登录（JWT） |
| **笔记管理** | `/api/notes/*` | 笔记 CRUD、标签关联、自动同步向量嵌入 |
| **分类管理** | `/api/categories/*` | 分类 CRUD |
| **标签管理** | `/api/tags/*` | 标签 CRUD |
| **知识图谱** | `/api/knowledge-graph/*` | 概念关系构建、查询、删除 |
| **学习计划** | `/api/plans/*` | 计划 CRUD、任务项管理 |
| **复习提醒** | `/api/reviews/*` | 复习记录管理、SM-2 间隔重复 |
| **学习打卡** | `/api/records/*` | 打卡、日历热力图、周统计 |
| **聊天记录** | `/api/chat-history/*` | 对话历史管理 |
| **数据仪表盘** | `/api/dashboard/*` | 学习概览、分类分布、学习趋势 |
| **AI 代理** | `AiAgentClient` | 转发给 AI Agent 的 RAG 问答（WebClient 异步调用） |

## 技术栈

- **框架**: Spring Boot 3.2.6
- **语言**: Java 17
- **ORM**: MyBatis-Plus 3.5.7
- **安全**: Spring Security + JWT (jjwt 0.12.5)
- **数据库**: MySQL 8.0
- **HTTP 客户端**: WebClient (Spring WebFlux)
- **构建工具**: Maven

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0

### 配置数据库

在 `src/main/resources/application.yml` 中修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/knowledge_assistant?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your-password
```

首次启动会自动执行 `schema.sql` 初始化数据库表结构。

### 安装运行

```bash
cd backend
mvn clean package -DskipTests
java -jar target/knowledge-assistant-1.0.0.jar
```

服务默认运行在 `http://localhost:8080`。

## 项目结构

```
backend/
├── src/main/java/com/ka/
│   ├── common/           # 通用工具
│   │   ├── Result.java           # 统一响应体
│   │   ├── BusinessException.java # 业务异常
│   │   └── GlobalExceptionHandler.java # 全局异常处理
│   ├── config/           # 配置类
│   │   ├── SecurityConfig.java    # Spring Security 配置
│   │   ├── CorsConfig.java       # 跨域配置
│   │   ├── MyBatisPlusConfig.java # 分页插件
│   │   └── MetaObjectHandlerConfig.java # 自动填充
│   ├── controller/       # 12 个 REST 控制器
│   │   ├── AuthController.java
│   │   ├── UserController.java
│   │   ├── NoteController.java
│   │   ├── CategoryController.java
│   │   ├── TagController.java
│   │   ├── KnowledgeRelationController.java
│   │   ├── LearningPlanController.java
│   │   ├── ReviewReminderController.java
│   │   ├── LearningRecordController.java
│   │   ├── DashboardController.java
│   │   ├── ChatHistoryController.java
│   │   └── AiController.java
│   ├── dto/              # 数据传输对象
│   ├── entity/           # 11 个实体类
│   ├── mapper/           # 11 个 MyBatis-Plus Mapper
│   ├── security/         # JWT 鉴权
│   ├── service/          # 9 个业务服务
│   │   ├── AuthService.java
│   │   ├── UserService.java
│   │   ├── NoteService.java
│   │   ├── CategoryService.java
│   │   ├── TagService.java
│   │   ├── ChatHistoryService.java
│   │   ├── KnowledgeRelationService.java
│   │   ├── ReviewReminderService.java
│   │   ├── LearningPlanServiceImpl.java
│   │   └── AiAgentClient.java  # AI Agent HTTP 客户端
│   └── KnowledgeApplication.java
├── src/main/resources/
│   ├── application.yml   # 应用配置
│   ├── schema.sql        # 数据库初始化 DDL
│   ├── migration.sql     # 数据库迁移脚本
│   └── mapper/           # MyBatis XML
│       └── NoteMapper.xml
├── Dockerfile            # 容器化构建
└── pom.xml
```

## API 概览

### 认证

```http
POST /api/auth/register   # 注册
POST /api/auth/login      # 登录，返回 JWT Token
```

### 笔记

```http
GET    /api/notes          # 分页列表（支持关键词/分类筛选）
GET    /api/notes/{id}     # 笔记详情
POST   /api/notes          # 创建笔记（自动同步向量）
PUT    /api/notes/{id}     # 更新笔记（自动同步向量）
DELETE /api/notes/{id}     # 删除（自动清理向量+知识关系）
GET    /api/notes/{id}/tags # 笔记标签
```

### 仪表盘

```http
GET /api/dashboard/overview              # 学习概览
GET /api/dashboard/category-distribution # 分类分布
GET /api/dashboard/study-trend           # 学习趋势
```

> 完整 API 列表请参照各个 Controller 源码。

## 外部依赖

| 服务 | 地址 | 说明 |
|------|------|------|
| **MySQL** | `localhost:3306` | 业务数据存储 |
| **AI Agent** | `http://localhost:8000` | FastAPI AI 服务（RAG、向量、图谱等） |

配置项在 `application.yml` 中的 `ai-agent.base-url`。

## 相关项目

- **前端**: Vue 3 前端界面，位于 `frontend/`
- **AI Agent**: FastAPI + LangChain + ChromaDB 智能服务，位于 `ai-agent/`