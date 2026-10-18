# OMR 离线知识资源

项目内共 60 份可导入文档：50 份历史问题案例与 10 份基础参考资料。
案例来自 `srv-pt-scan-recognization/docs/omr-issues`；基础资料由云文档与本地源码核对、结构化整理。
本资源不依赖原始 Downloads 目录或外部代码仓库，源位置以逻辑路径记录在清单和正文中。
15 份云文档的逐篇处理与未覆盖边界见 [来源覆盖表](SOURCE-COVERAGE.md)。

## 文件目录

- `cases/`：50 份独立案例，一案一份。
- `references/`：错误码、服务架构、排查流程、无点无线等基础知识。
- `manifest.json`：所有可导入文档的编号、分类、来源、SHA-256；使用二版清单，导入器兼容旧版案例包。
- 本说明和清单用于维护，不作为知识正文上传。

## 基础参考资料

| 文档 | 内容与范围 |
| --- | --- |
| [原生引擎历史错误码](references/OMR-REF-ENGINE-CODES.md) | 云文档原生码表 61 条，保留名称和释义，明确 API/库版本待核验 |
| [Java ErrorType](references/OMR-REF-JAVA-ERROR-TYPE.md) | 当前 Java 引擎客户端 80 个枚举项，含原生映射和 Java 自定义告警 |
| [Java DetailErrorStatus](references/OMR-REF-JAVA-DETAIL-CODES.md) | Java Web 业务详细异常 16 项，独立于原生引擎码 |
| [Go 选项接口状态](references/OMR-REF-GO-STATUS.md) | /v1/option 外层、图片级、题块级和填涂规则分层 |
| [业务职责与服务调用链](references/OMR-REF-ARCHITECTURE.md) | 主引擎、卡型路由、Java/Go/算法职责、上游业务边界 |
| [识别问题排查](references/OMR-REF-TROUBLESHOOTING.md) | 日志、归属、考号、客观题、打分条和依赖故障的检查顺序 |
| [无点无线](references/OMR-REF-WITHOUT-POINT.md) | Java HTTP 入口、适用条件、算法/包装层边界与历史版本 |
| [外部依赖与环境](references/OMR-REF-DEPENDENCIES.md) | 文件服务、配置中心、ZooKeeper、Redis 与资源定位原则 |
| [Go/cgo 环境与版本匹配](references/OMR-REF-ENGINE-ENVIRONMENT.md) | 编译、链接、运行、协议和动态库的版本边界 |
| [考号与条码异常分类](references/OMR-REF-ADMISSION.md) | 识别、图像质量、裁剪、业务匹配与设计方案的边界 |

错误码按层级分开，每节保留层级和适用条件。例如 20 在 Java ErrorType 为折角，在 DetailErrorStatus 为结构错误；
历史原生表中 1000/1001 的含义又与 Java 自定义值不同。未知版本或未说明的原生含义不会补写成确定根因。

云文档中的访问口令、机器清单、图片签名 URL 和原始业务数据未复制进基础参考资料。
实际部署状态与历史文档分开；源码存在某能力不等于当前全部实例已升级。

## 案例范围

| 分类 | 数量 |
| --- | ---: |
| 校正与页码 | 7 |
| 客观题识别 | 9 |
| 主观评分与打分条 | 8 |
| 考号与条码 | 4 |
| 接口、环境与入参 | 16 |
| 取证、构建操作与工具 | 6 |

案例证据状态：24 条原因已验证、21 条链路已定位但仍待验证、3 条待补证据、2 条规则已确认。
原因已验证不等于修复上线，相似现象不等于相同根因。样本坐标、阈值和临时绕过不能作为全局方案。
案例原台账截止时间为清单中的 `caseSourceUpdated`；`sourceUpdated` 表示本知识资源的整理日期，不代表运行验证。
示例：[页码异常 C02](cases/OMR-C02.md)、[多涂规则 O09](cases/OMR-O09.md)、[条码默认错误码 I04](cases/OMR-I04.md)。

## 在完整项目中导入

目标 Agent 后端及其 Embedding 服务、Milvus 需可用。项目根目录执行：

```bash
make knowledge-list
make knowledge-import
make knowledge-import API_BASE_URL=https://agent.example.com
```

默认导入全部 60 份。如果之前已经导入案例，只补充基础参考资料：

```bash
make knowledge-list KNOWLEDGE_KIND=references
make knowledge-import KNOWLEDGE_KIND=references
```

`KNOWLEDGE_KIND=cases` 只选择案例。Makefile 使用项目 `.env` 中的后端端口，显式 `API_BASE_URL` 可覆盖。
导入是独立命令，不会在 `make deploy` 时自动发生。

## 直接使用 Python 脚本

Python 3.9+，仅使用标准库，脚本按自身位置定位资源，与当前工作目录无关。
基础地址支持应用路径，如 `https://example.com/oncall`，最终请求该地址下的 `/api/upload`。

```bash
python3 scripts/import_knowledge.py --dry-run
python3 scripts/import_knowledge.py --api-url https://agent.example.com --report /tmp/omr-import-result.json
python3 scripts/import_knowledge.py --kind references --api-url https://agent.example.com
python3 scripts/import_knowledge.py --id OMR-REF-ENGINE-CODES --api-url https://agent.example.com
python3 scripts/import_knowledge.py --case OMR-C02 --api-url https://agent.example.com
```

`--id` 可重复传入，`--case` 保留为同义参数以兼容旧命令。
每文件超时默认 300 秒，可用 `--timeout` 修改。任一失败返回非零，继续处理其余文档并打印失败编号；
`--report` 保存逐文档结果及案例/参考资料类型，不包含正文，可按编号重试。

## 可独立迁移的离线包

```bash
make knowledge-package
```

产物为 `target/omr-knowledge.zip`，包含全部案例、参考资料、清单、来源覆盖表、说明，以及
`scripts/import_knowledge.py`、`scripts/upload_docs.py`。它不包含凭证、业务源码、日志或数据库数据。

复制 ZIP 到目标机器并解压，在解压目录执行：

```bash
python3 scripts/import_knowledge.py --dry-run
python3 scripts/import_knowledge.py --api-url https://agent.example.com
```

也可通过脚本绝对路径执行，不需要目标机器的 Java/Maven、前端或 `.env`。
离线指资料和工具可携带；实际入库仍需要目标后端能调用配置的 Embedding 服务并连接 Milvus。
当前使用 DashScope 时仍需相应网络及 API Key。

## 校验与更新

导入前先校验全部清单、数量、编号、UTF-8 文件及 SHA-256。即使只选择参考资料，也会先检查整包完整性；
任一文件缺失或不一致时，不会开始上传。每次发送前再次核对内容校验值。

更新时保留稳定文件名并同步 SHA-256；新增文档更新对应列表、分类统计、caseCount/referenceCount/total。
案例使用 `OMR-C02` 一类编号，参考资料使用 `OMR-REF-ENGINE-CODES` 一类编号。
同名文件按既有接口更新来源，重命名视为新文档；现有更新不是整批事务，失败时按资源编号重试。
所有会话共享导入后的知识库。
