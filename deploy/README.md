# 生产部署说明

## 1. 准备环境变量

在项目根目录执行：

```bash
cp .env.example .env
chmod 600 .env
```

编辑 `.env`，填写 MySQL 密码、至少 32 位的 JWT 密钥和 DeepSeek API Key。

## 2. 选择入口模式

### Docker 直接对外提供 HTTP

保持 `.env` 中的 `PUBLIC_HTTP_PORT=80`，服务器只需将 80/443 对公网放行。启动后通过 `http://服务器公网IP` 访问。

### 宝塔 Nginx 管理域名与 HTTPS（推荐）

1. 将 `.env` 改为 `PUBLIC_HTTP_PORT=127.0.0.1:8088`，前端容器只监听本机回环地址。
2. 在宝塔「网站」中新建站点，域名填写你的域名。
3. 将 `deploy/baota/knowledge-assistant.conf.example` 内容粘贴到站点配置，替换域名。
4. 在宝塔 SSL 页面申请证书并开启强制 HTTPS。

## 3. 启动与更新

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f --tail=100
```

之后更新代码：

```bash
docker compose up -d --build
```

## 4. 数据持久化

以下 Docker 卷会保留在服务器上，重新构建容器不会清空：

- `mysql_data`：业务数据
- `chroma_data`：ChromaDB 向量数据
- `uploads_data`：用户头像及上传文件

不要执行 `docker compose down -v`，该命令会删除上述数据卷。

## 5. 安全组建议

仅开放公网 80、443；SSH 22 与宝塔面板端口仅允许你的固定 IP。不要开放 3306、8080、8000。
