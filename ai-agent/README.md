# Knowledge AI Agent

基于 FastAPI + LangChain + ChromaDB 的智能知识管理助手，使用 DeepSeek API 作为 LLM 后端。

## 功能特性

- **智能问答 (RAG)** - 基于用户笔记的语义检索问答系统
- **向量检索** - 使用 ChromaDB 进行笔记的语义搜索
- **知识图谱提取** - 从文本中自动提取概念及其关系
- **学习计划生成** - 根据学习目标自动生成结构化学习路径
- **聊天记忆** - 对话历史的向量化存储与语义检索
- **间隔重复算法** - 实现 SM-2 算法的复习提醒系统
- **文本摘要** - 自动生成笔记摘要
- **难度评估** - 评估学习材料的难度等级

## 技术栈

- **框架**: FastAPI
- **LLM**: LangChain + DeepSeek API
- **向量数据库**: ChromaDB
- **嵌入模型**: Sentence Transformers

## 快速开始

### 环境要求

- Python 3.10+
- pip 或 uv 包管理器

### 安装步骤

1. 克隆项目
```bash
git clone <repository-url>
cd ai-agent
```

2. 创建虚拟环境
```bash
python -m venv .venv
# Windows
.venv\Scripts\activate
# Linux/macOS
source .venv/bin/activate
```

3. 安装依赖
```bash
pip install -r requirements.txt
```

4. 配置环境变量
```bash
cp .env.example .env
# 编辑 .env 文件，填入你的 DeepSeek API Key
```

5. 启动服务
```bash
uvicorn app.main:app --reload --port 8000
```

服务启动后访问:
- API 文档: http://localhost:8000/docs
- 健康检查: http://localhost:8000/health

## API 接口

### 健康检查

```http
GET /health
```

返回服务运行状态。

### 智能问答

```http
POST /agent/chat
Content-Type: application/json

{
  "user_id": 1,
  "note_id": 123,  // 可选，指定笔记则只基于该笔记回答
  "question": "什么是机器学习？"
}
```

### 笔记向量同步

```http
POST /agent/embeddings/sync
Content-Type: application/json

{
  "note_id": 123,
  "user_id": 1,
  "content": "笔记内容...",
  "title": "笔记标题"
}
```

### 语义搜索

```http
POST /agent/search-notes
Content-Type: application/json

{
  "user_id": 1,
  "query": "机器学习基础",
  "top_k": 5
}
```

### 文本摘要

```http
POST /agent/summarize
Content-Type: application/json

{
  "content": "需要摘要的长文本..."
}
```

### 难度评估

```http
POST /agent/assess-difficulty
Content-Type: application/json

{
  "content": "学习材料内容..."
}
```

### 知识图谱提取

```http
POST /agent/extract-graph
Content-Type: application/json

{
  "content": "文本内容..."
}
```

### 学习计划生成

```http
POST /agent/gen-learning-plan
Content-Type: application/json

{
  "goal": "我想学习 Python 数据分析"
}
```

### 聊天记忆同步

```http
POST /agent/chat-memory/sync
Content-Type: application/json

{
  "user_id": 1,
  "question": "什么是机器学习？",
  "answer": "机器学习是人工智能的一个分支..."
}
```

### 聊天记忆检索

```http
POST /agent/chat-memory/search
Content-Type: application/json

{
  "user_id": 1,
  "query": "之前讨论过的算法",
  "top_k": 5
}
```

### 复习间隔计算 (SM-2 算法)

```http
POST /agent/next-review
Content-Type: application/json

{
  "user_id": 1,
  "note_id": 123,
  "quality": 4,           // 0-5，用户自评记忆质量
  "interval_days": 6,     // 当前间隔天数
  "ease_factor": 2.5      // 难度因子
}
```

**SM-2 算法说明**：基于 SuperMemo SM-2 间隔重复算法，根据用户自评质量（quality 0-5）动态调整复习间隔：

- quality ≥ 3：逐步延长复习间隔（1 → 6 → interval × ease_factor），并调整难度因子
- quality < 3：重置间隔为 1 天，说明需要重新学习
- ease_factor 下限 1.3，确保长期记忆不会过于频繁

## 项目结构

```
ai-agent/
├── app/
│   ├── __init__.py
│   ├── main.py              # FastAPI 应用入口
│   ├── config.py            # 配置管理
│   ├── models/
│   │   ├── __init__.py
│   │   └── schemas.py       # Pydantic 数据模型
│   ├── routers/
│   │   ├── __init__.py
│   │   ├── chat.py          # 问答接口
│   │   ├── chat_memory.py   # 聊天记忆接口
│   │   ├── embeddings.py    # 向量同步接口
│   │   ├── graph.py         # 知识图谱接口
│   │   ├── plan.py          # 学习计划接口
│   │   ├── review.py        # 复习间隔接口
│   │   └── summarize.py     # 摘要和评估接口
│   └── services/
│       ├── __init__.py
│       ├── embedding_service.py  # 向量嵌入服务
│       ├── graph_service.py      # 知识图谱服务
│       ├── llm_service.py        # LLM 服务
│       ├── chat_memory_service.py # 聊天记忆服务
│       ├── plan_service.py       # 学习计划服务
│       ├── rag_service.py        # RAG 问答服务
│       ├── review_service.py     # 间隔重复算法
│       └── vector_store.py       # ChromaDB 存储
├── .env.example             # 环境变量示例
├── requirements.txt         # Python 依赖
└── README.md
```

## 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| DEEPSEEK_API_KEY | DeepSeek API 密钥 | - |
| DEEPSEEK_BASE_URL | DeepSeek API 地址 | https://api.deepseek.com |
| DEEPSEEK_MODEL | 使用的模型名称 | deepseek-chat |
| CHROMA_PERSIST_DIR | ChromaDB 数据存储路径 | ./chroma_data |
| EMBEDDING_MODEL | 嵌入模型 | sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2 |
| SPRING_BOOT_URL | Spring Boot 后端地址 | http://localhost:8080 |

## Docker 部署

```bash
# 构建镜像
docker build -t knowledge-ai-agent .

# 运行容器
docker run -d \
  --name ai-agent \
  -p 8000:8000 \
  -e DEEPSEEK_API_KEY=your-api-key \
  -v $(pwd)/chroma_data:/app/chroma_data \
  knowledge-ai-agent
```

或使用 docker-compose:

```bash
docker-compose up -d
```

## 开发指南

### 运行测试

```bash
pytest tests/
```

### 代码格式化

```bash
# 格式化代码
black app/

# 类型检查
mypy app/
```

## 许可证

MIT License