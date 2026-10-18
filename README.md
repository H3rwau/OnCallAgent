# OnCallAgent

Spring AI 运维助手，提供普通聊天、流式聊天、知识库检索和 AI Ops，前端独立使用 Vue。

普通聊天支持 RAG：Agent 按问题需要调用 `InternalDocsTools`，检索 Milvus 中的文档分片，再结合结果回答。
上传文档存入共享知识库，供各会话使用；整篇文档不会自动附加到每条消息。
检索时保留本次原始问题，避免模型改写时遗漏错误码；类名/字段名与数字组合优先精确匹配已入库片段，未命中时使用向量检索。
知识库不会绑定会话创建时的版本，旧会话也可查询后来导入的资料；历史回答中的“未找到”或“不可用”需重新检索核实。
AI Ops 同样可以检索知识库，并结合真实告警和云日志生成报告。

## 本地一键启动

需要 Java 21、Maven、Node.js/npm、Python 3，以及可访问的 Milvus。
首次在新环境参考 `.env.example` 创建项目 `.env`，填写 `DASHSCOPE_API_KEY`；
使用本地 SLS MCP 时，还需填写项目专用的 SLS 只读 AK/SK。

首次安装本地官方 SLS MCP 二进制（固定 v2.0.7）：

```bash
python3 scripts/aliyun_sls_mcp.py --install
```

Milvus 单独管理。如果本机尚未启动 Milvus，先启动 Docker Desktop，再执行：

```bash
make up
```

一键构建并启动前端、后端和 MCP：

```bash
make deploy
```

默认访问 `http://127.0.0.1:8080`。后端监听 `9900`，本地 SLS MCP 的 SSE 地址为 `http://127.0.0.1:8181/sse`。
Makefile 按顺序检查端口、构建产物、确保 MCP 可用、重启后端 JAR 和前端，再检查页面及 API 转发。
前端使用 Vite preview 提供构建后的页面和 `/api` 转发。启动不会上传任何知识文档。

已运行的外部 MCP 服务可复用。后端或前端端口被 IDE 等外部进程占用时，会明确报错，保留外部进程；
请先自行停止它，或指定其他端口：

```bash
make deploy SERVER_PORT=19900 WEB_PORT=18080
```

端口也可写入项目 `.env`。命令行覆盖优先，前端代理与健康检查会使用相同的后端地址。
使用远程或其他 MCP 时，先准备配置中启用的服务，再执行 `make deploy SKIP_LOCAL_MCP=1`。
显式关闭本地 SLS 或开启模拟日志模式时，会自动跳过本地 SLS 启动。

## 日常命令

```bash
make start       # 启动已经构建好的前端、后端 JAR 和 MCP，等待就绪
make restart     # 重启前后端，确保 MCP 可用
make stop        # 只停止 Makefile 创建的前端、后端及 MCP 进程
make status      # 查看三项服务状态
make check       # 检查后端数据库连接、前端页面及 API 转发
make dev         # 启动 Vite 热更新前端和 Maven 后端
make down        # 单独停止本地 Milvus 容器，保留数据
```

切换开发模式前先执行 `make stop`；开发前端同样使用 `WEB_PORT`（默认 `8080`）。
自定义端口时，后续生命周期命令使用相同的 Make 变量，或统一把端口写入 `.env`。
进程记录和日志位于 `.run/agent.log`、`.run/web.log`、`.run/mcp.log`。
脚本以 PID、启动时间和进程组确认归属，停止时保留 IDE 和外部进程。
启动标识固定使用 C locale 与 UTC，不受调用终端的语言或时区影响；进程仍存活但归属无法确认时，保留记录并报错，不删除记录或发送信号。
后台 Java/前端/MCP 进程由脚本管理，当前不提供崩溃自动拉起或开机自启。

单独构建：

```bash
make build       # 后端 target/OnCallAgent-*.jar；前端 frontend/dist/
make package
make frontend-build
```

`make run-jar` 可在前台运行后端。直接使用 `java -jar` 时由运行环境注入变量；
`scripts/run-agent.sh` 会加载项目 `.env`。IDE 启动也需要加载 `.env` 或设置同名环境变量。

## 配置文件职责

| 文件 | 用途 |
| --- | --- |
| `.env` / `.env.example` | 本项目凭证、端口和环境变量覆盖 / 无凭证的配置模板 |
| `src/main/resources/application.yaml` | Java、模型、检索、Milvus、Prometheus 和多云 MCP 连接 |
| `config/aliyun-sls-mcp.yaml` | 独立 SLS MCP 的监听地址及工具范围 |
| `docker-compose.milvus.yml` | 本地 Milvus、etcd、MinIO 及数据卷 |
| `frontend/.env.example` | 单独运行前端时的开发代理地址示例 |

常用 `.env` 设置：

```dotenv
SERVER_PORT=9900
WEB_PORT=8080
DASHSCOPE_CHAT_MODEL='qwen-plus'
DASHSCOPE_HTTP_TIMEOUT_MS=180000
RAG_TOP_K=3
```

快速聊天、流式聊天和 AI Ops 共用模型，所选模型须支持工具调用。
聊天 temperature 为 `0.7`、最大输出为 `2000` tokens；AI Ops 分别为 `0.3` 和 `8000`。
`DASHSCOPE_HTTP_TIMEOUT_MS` 控制同步请求连接、读写和总超时，以及流式请求连接、读空闲超时。
持续收到内容的流可超过读空闲时长，仍受接口自身的 SSE 超时限制。
向量化独立使用 `dashscope.embedding.model`，默认 `text-embedding-v4`。

## 上传自己的知识文档

在前端输入框的 `+` 菜单上传非空 `.md` 文件，后端会保存、分片、生成向量并写入 Milvus。
也可显式选择目录批量导入：

```bash
make list-docs DOCS_DIR=/path/to/your/docs
make upload DOCS_DIR=/path/to/your/docs
```

只有文件保存且向量索引完成后才返回成功。索引失败返回 HTTP 500，保留文件以便修复后重新上传。
批量脚本核对 HTTP 状态、业务码和文件确认信息，任一文档失败时以非零状态退出。

## 项目离线知识资源

`knowledge/omr/` 随项目保存 50 份历史案例和 10 份基础参考资料（共 60 份），包括错误码、Java/Go 状态、架构、排查、无点无线、外部依赖和考号专题。
资料不依赖 Downloads、原始云文档链接或外部源码目录，来源和适用边界见 [知识资源说明](knowledge/omr/README.md)，
15 份云文档的处理情况见 [来源覆盖表](knowledge/omr/SOURCE-COVERAGE.md)。

```bash
make knowledge-list       # 校验并预览，不写入知识库
make knowledge-import     # 导入本环境后端
make knowledge-import API_BASE_URL=https://agent.example.com
make knowledge-package    # 生成 target/omr-knowledge.zip
make knowledge-import KNOWLEDGE_KIND=references  # 只补充 10 份基础参考资料
```

ZIP 包包含文档、清单和标准库 Python 脚本，可复制到其他环境解压后执行：

```bash
python3 scripts/import_knowledge.py --api-url https://agent.example.com
```

Python 3.9+ 即可运行导入工具，不需要本机的 Java/Maven、前端或 `.env`。
导入前校验全部清单和文件 SHA-256；失败返回非零，可用 `--case OMR-C02` 或 `--id OMR-REF-ENGINE-CODES` 选择单个资源重试，
用 `--kind references` 仅导入基础参考资料，用 `--report /path/to/result.json` 保存逐文件结果。
离线指资料与工具可随包携带；实际入库仍需目标后端及其 Embedding 服务、Milvus 可用。
`make deploy` 不会自动导入，启动与数据导入保持独立。

## 多云日志与监控

`application.yaml` 的 `oncall.logs.servers` 定义 SSE MCP 连接，`sources` 定义业务名称、别名及原生资源参数。
默认启用阿里云 SLS；腾讯云 CLS 和华为云 LTS 是禁用的配置示例，可按需填写并启用。
多个服务可同时使用。默认日志源需要属于启用的服务。

MCP 独立运行，Java 负责连接；所有启用的服务需要先可用，不可达时初始化会失败。
工具自动添加服务 ID 前缀，例如 `aliyun-sls__sls_execute_sql`，避免同名冲突。
日志参数和查询语法遵循各服务实际定义。云凭证由 MCP 使用，Java 启动脚本会移除云 AK/SK 环境变量。
其他服务通过新增 SSE 连接和资源映射接入；华为云服务端尚未部署或验证。

Prometheus 告警地址通过 `PROMETHEUS_BASE_URL` 设置，默认 `http://127.0.0.1:9090`。
暂未运行不阻止后端启动；真实告警查询及依赖告警的 AI Ops 需要它可访问。
默认查询真实数据，失败不会自动替换为模拟告警。

## 验证

```bash
make test    # 本地回归测试，不调用云模型或写入真实知识库
```

真实 SLS 验证需先启动 MCP，再显式开启：

```bash
SLS_MCP_INTEGRATION_TEST=true mvn -Dtest=AliyunSlsMcpIntegrationTest test
```

每个日志库最多读取最近 5 分钟内一条日志，不输出正文或凭证，不调用模型，不依赖 Milvus。
更多前端说明见 [frontend/README.md](frontend/README.md)。
第三方迁入代码的许可证保存在 `third-party/SuperBizAgent-LICENSE`。
