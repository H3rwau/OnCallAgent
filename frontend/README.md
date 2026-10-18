# OnCall Agent 前端

独立的 Vue 3 + TypeScript + Vite 应用，保持 ChatGPT 风格的黑白灰布局。
接口来自现有后端 Controller，本前端不要求修改后端代码。

## 开发与热重载

要求 Node.js 20.19+ 或 22.12+（推荐当前 LTS）。

```bash
cd frontend
npm install
npm run dev
```

打开 <http://localhost:5173>。修改 Vue、TypeScript 或 CSS 后 Vite 自动热更新。
前端与 Java 后端分别运行，前端不需要任何云服务凭证。

默认代理 `/api` 到 `http://127.0.0.1:9900`，无需修改后端 CORS。
若后端地址不同，在 `frontend/.env.local` 中设置并重启 Vite：

```dotenv
API_PROXY_TARGET=http://127.0.0.1:9900
```

不要把 DashScope Key 或 AK/SK 放入前端环境文件。

## 功能与实际接口

| 入口 | 请求 | 行为 |
| --- | --- | --- |
| 输入框右侧「快速对话」 | `POST /api/chat` | 一次显示完整回答 |
| 输入框右侧「流式对话」 | `POST /api/chat_stream` | 根据 SSE `content` 事件逐段显示 |
| 输入框左侧 `+` | `POST /api/upload` | 选择并上传单个 `.md` 文档 |
| 右上角「AI Ops」 | `POST /api/ai_ops` | 启动智能运维分析，执行期间锁定页面 |
| 左侧「新建对话」与历史列表 | 本地存储 + 每段独立的后端会话 ID | 新建、保存和切换多段对话 |

没有加入登录、注册、问号帮助、搜索对话、图片工具等额外入口。

### 快速与流式对话

两个聊天接口使用相同的请求结构：

```json
{
  "Id": "本段对话固定的 UUID",
  "Question": "用户输入的问题"
}
```

在同一对话中切换快速和流式模式仍复用同一个 `Id`，后端上下文可以延续。
每段对话分别保存模式、消息和上传记录。Enter 发送，Shift + Enter 换行，⌘ / Ctrl + K 新建对话。

快速接口成功响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": { "success": true, "answer": "Markdown 回复", "errorMessage": null }
}
```

后端失败时仍可能返回 HTTP 200 和 `code: 200`，因此还会检查 `data.success` 和 `data.errorMessage`。

### SSE 格式

聊天流和 AI Ops 均通过 POST 发起，使用 `fetch` 读取响应流，不能直接使用 GET 方式的 `EventSource`。
后端发送名为 `message` 的事件，`data` 为 JSON：

```text
event: message
data: {"type":"content","data":"增量内容"}

event: message
data: {"type":"error","data":"失败原因"}

event: message
data: {"type":"done","data":null}

```

前端处理 UTF-8 字符拆分、跨网络包事件、多事件合包、CRLF 和多行 data。
收到 `done` 才认定成功，`error`、意外断流、错误格式和超时均会结束等待并保留已接收的内容。
流式内容即时渲染 Markdown，并用 DOMPurify 清理 HTML。

### Markdown 上传

使用 `multipart/form-data`，字段名为 `file`，让浏览器自动设置 boundary。
上传期间显示文件名和等待状态，完成后显示「已上传」，失败时显示后端错误；
仅允许非空 `.md` 文件，文件大小上限由后端决定，HTTP 413 会显示明确提示。

`FileUploadController` 在文件保存并且向量索引完成后返回成功。
索引失败时返回 HTTP 500，并保留已保存的文件，修复后可重新上传。
上传的是后端共享文档库，不是单次聊天的附件；`ChatRequest` 没有文件字段，前端不会额外构造附件参数。
上传后可在对话中让助手查询相应内部文档。

### AI Ops

`ChatController.aiOps()` 无请求参数，前端 POST 不发送虚构的用户请求或会话字段。
点击后生成独立的本地分析记录，实时接收初始进度和最终报告。

运行时整个应用区域设为 `inert`，显示不可关闭的等待遮罩，禁用发送、上传、
新建、历史切换和重复触发；收到完成、错误、断流或超时后自动恢复操作。
报告保存在左侧历史列表，可随时重新打开。AI Ops 不属于后端普通聊天会话，报告页为只读，
可以新建普通对话或再次点击 AI Ops 发起新分析。

前端快速请求超时为 240 秒，聊天 SSE 为 310 秒，AI Ops 为 610 秒；
Vite 代理超时为 650 秒，以覆盖后端 10 分钟的 AI Ops 等待窗口。
超时只能结束浏览器等待，不能保证后端停止执行。

## 历史记录

本地保存最多 100 段对话，刷新后恢复消息、模式、上传记录和 AI Ops 报告。
刷新前未完成的流会标记为中断，并保留部分内容，不会永久停留在加载状态。

后端会话保存在内存中，仅保留最近六轮普通聊天上下文；后端重启后上下文会重置，
本地历史仍可展示，但不会自动重新上传到后端。

## 检查与部署

```bash
npm test
npm run build
npm run preview
```

测试涵盖接口字段与错误处理、SSE 拆包和增量输出、上传协议、模式切换、
历史恢复、AI Ops 页面锁定及各种退出路径的解锁。

产物位于 `dist/`。在项目根目录运行 `make deploy`，会构建前后端并管理前端、后端 JAR 和 MCP。
前端以 Vite preview 提供构建后的页面，默认端口 `8080`，`/api` 转发到实际后端地址。
`make dev` 使用 Vite 热更新和 Maven 后端；`make stop` 停止 Makefile 管理的进程。
端口可通过项目 `.env` 的 `SERVER_PORT`、`WEB_PORT` 或同名 Make 变量设置。
