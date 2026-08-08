# 知识管理助手 - 前端

基于 Vue 3 + TypeScript + Vite 构建的智能知识管理前端界面，配合后端 AI Agent 提供智能问答、知识图谱、学习计划等功能。

## 功能模块

| 模块 | 说明 |
|------|------|
| **学习仪表盘** | 学习数据概览、统计图表 |
| **知识笔记** | Markdown 笔记编辑与管理，支持 Vditor 编辑器 |
| **知识图谱** | 可视化的知识概念关系图 |
| **AI 助手** | 基于 RAG 的智能问答，可针对指定笔记提问 |
| **学习计划** | AI 生成的个性化学习路径与计划管理 |
| **复习管理** | 基于间隔重复算法的复习提醒 |
| **分类/标签** | 笔记的分类与标签管理 |
| **个人中心** | 用户信息与设置 |

## 技术栈

- **框架**: Vue 3 + TypeScript
- **构建工具**: Vite 8
- **UI 组件库**: Element Plus（中文国际化）
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **图标**: Element Plus Icons
- **编辑器**: Vditor（Markdown）
- **图表**: ECharts 6
- **日期处理**: dayjs

## 快速开始

### 环境要求

- Node.js 18+
- npm 或 pnpm

### 安装步骤

```bash
# 进入项目目录
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

服务默认运行在 `http://localhost:5173`

### 构建生产版本

```bash
npm run build
```

构建产物输出到 `dist/` 目录。

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口封装
│   │   ├── index.ts      # Axios 实例与拦截器
│   │   ├── auth.ts       # 认证相关接口
│   │   ├── notes.ts      # 笔记接口
│   │   ├── ai.ts         # AI 助手接口
│   │   ├── dashboard.ts  # 仪表盘接口
│   │   ├── categories.ts # 分类接口
│   │   ├── tags.ts       # 标签接口
│   │   ├── reviews.ts    # 复习接口
│   │   ├── plans.ts      # 学习计划接口
│   │   └── chatHistory.ts # 聊天历史接口
│   ├── components/       # 通用组件
│   │   └── icons/        # SVG 图标组件
│   ├── layouts/          # 布局组件
│   │   └── MainLayout.vue
│   ├── router/           # 路由配置
│   │   └── index.ts
│   ├── stores/           # Pinia 状态管理
│   │   └── user.ts
│   ├── styles/           # 样式文件
│   │   ├── variables.css # CSS 变量
│   │   ├── global.css    # 全局样式
│   │   └── theme.ts      # 主题配置
│   ├── views/            # 页面视图
│   │   ├── Login/        # 登录页
│   │   ├── Dashboard/    # 学习仪表盘
│   │   ├── Notes/        # 笔记列表
│   │   ├── NoteEditor/   # 笔记编辑器
│   │   ├── KnowledgeGraph/ # 知识图谱
│   │   ├── LearningPlans/  # 学习计划
│   │   ├── AiAssistant/  # AI 助手
│   │   ├── Categories/   # 分类管理
│   │   ├── Tags/         # 标签管理
│   │   └── Profile/      # 个人中心
│   ├── App.vue
│   └── main.ts           # 应用入口
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

## 环境配置

Vite 开发服务器通过代理将 `/api` 请求转发到后端：

| 配置项 | 值 |
|--------|-----|
| 开发端口 | 5173 |
| API 代理目标 | http://localhost:8080 |
| API 基础路径 | /api |

> 后端服务地址在 `vite.config.ts` 中配置，默认代理到 Spring Boot 后端 `http://localhost:8080`。

## 相关项目

- **后端 API**: Spring Boot（提供业务 API）
- **AI Agent**: [ai-agent](https://github.com/your-repo/ai-agent)（FastAPI + LangChain + ChromaDB，提供 AI 能力）