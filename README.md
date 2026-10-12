# OnCallAgent

## 本地 Milvus

项目使用 `localhost:19530` 连接本地 Milvus。首次启动前请确保 Docker Desktop 正在运行：

```bash
docker compose -f docker-compose.milvus.yml up -d
docker compose -f docker-compose.milvus.yml ps
```

Milvus Web UI：<http://localhost:9091/webui/>。

停止服务（保留数据）：

```bash
docker compose -f docker-compose.milvus.yml down
```

运行数据保存在项目根目录的 `volumes/`，该目录不会提交到 Git。
