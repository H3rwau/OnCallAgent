# OMR 云文档知识覆盖表

本表逐篇说明 15 份业务导出文件的处理方式。已收录指可复用文字知识已经提炼，不表示原文逐字复制或所有案例已复现。

本地导出正文的逐篇筛选与可复用知识核对；未逐条复现所有长案例、未读取缺失图片附件、未打开全部外链，未核验现网状态。

本表和使用说明是维护资料，不上传为业务知识。

| 原文档 | 处理方式 | 项目内知识文档 | 处理说明 |
| --- | --- | --- | --- |
| 20260108-OMR无点无线矫正引擎上线材料.md | 已整理收录 | [OMR-REF-WITHOUT-POINT](references/OMR-REF-WITHOUT-POINT.md) | 提炼能力、组件及版本历史；不把旧机器、端口或回滚命令当作当前配置。 |
| AI-OMR API文档.md | 空文档未收录 | — | 本地导出为 0 字节，未获取 API 正文；不能据文件名补造协议。 |
| OMR-交接.md | 已整理收录 | [OMR-REF-ARCHITECTURE](references/OMR-REF-ARCHITECTURE.md) | 提炼引擎与仓库职责，并按源码修正调用链；不复制口令和访问入口。 |
| OMR-优化方案.md | 空文档未收录 | — | 本地导出为 0 字节，等待补充有效正文。 |
| OMR-引擎.md | 合并收录 | [OMR-REF-ARCHITECTURE](references/OMR-REF-ARCHITECTURE.md)、[OMR-REF-WITHOUT-POINT](references/OMR-REF-WITHOUT-POINT.md) | 可用的主引擎/无点无线概念合并，链接和未导出附件不当作已读正文。 |
| OMR-引擎错误码.md | 已整理收录 | [OMR-REF-ENGINE-CODES](references/OMR-REF-ENGINE-CODES.md)、[OMR-REF-JAVA-DETAIL-CODES](references/OMR-REF-JAVA-DETAIL-CODES.md)、[OMR-REF-JAVA-ERROR-TYPE](references/OMR-REF-JAVA-ERROR-TYPE.md)、[OMR-REF-GO-STATUS](references/OMR-REF-GO-STATUS.md) | 原生表及 Java 详细错误码分层整理，再补客户端和 Go 状态对照；不混用同数值。 |
| OMR-数智作业-现网.md | 仅资源台账 | — | 主要是机器地址，适合带日期的资源台账，不单独向量化。 |
| OMR-智批-问题排查.md | 已整理收录 | [OMR-REF-TROUBLESHOOTING](references/OMR-REF-TROUBLESHOOTING.md)、[OMR-REF-ADMISSION](references/OMR-REF-ADMISSION.md) | 提炼业务归属、日志排查和异常分类；历史保留策略与维护口径不泛化为现状。 |
| OMR-服务.md | 合并收录 | [OMR-REF-ARCHITECTURE](references/OMR-REF-ARCHITECTURE.md) | 服务和接口入口合并进架构；objectCheck 按当前 Controller 标明为审查相关入口。 |
| OMR引擎封装服务环境配置.md | 提炼通用知识 | [OMR-REF-ENGINE-ENVIRONMENT](references/OMR-REF-ENGINE-ENVIRONMENT.md) | 提炼 cgo 构建、运行和版本匹配；不保留旧虚拟机口令、IDE 路径或直接删库换库操作。 |
| OMR现网资源维护.md | 提炼通用知识 | [OMR-REF-DEPENDENCIES](references/OMR-REF-DEPENDENCIES.md) | 保留多地域、多业务集群的定位原则；完整机器清单、数量与保护状态不作为长期知识。 |
| OMR系统维护.md | 仅导航 | — | 主要为维护、工具和日志入口导航，保留来源说明，不作为独立知识正文。 |
| OMR部署.md | 提炼通用知识 | [OMR-REF-DEPENDENCIES](references/OMR-REF-DEPENDENCIES.md) | 提炼 Java、Go、无点无线和注册中心的拓扑；历史发布路径与环境地址留在原资源记录。 |
| 考号填涂异常专项分析.md | 已整理收录 | [OMR-REF-ADMISSION](references/OMR-REF-ADMISSION.md)、[OMR-REF-TROUBLESHOOTING](references/OMR-REF-TROUBLESHOOTING.md) | 提炼原因分类、识别与匹配边界；不复制原始案例标识、学生信息或签名图片 URL；设计方案不写成已实现。 |
| 高教接入OMR现网资源维护.md | 提炼通用知识 | [OMR-REF-DEPENDENCIES](references/OMR-REF-DEPENDENCIES.md) | 提炼配置中心、ZooKeeper、Redis、对象/文件服务的依赖分类，不复制连接信息。 |

## 其他文件

5 个 ZIP 只有同名 Markdown 的重复正文，不重复入库。下载说明和清单保留作导出记录。图片、附件及云链接正文未获取的部分不作为已读知识。

项目另有 50 份由代码仓库问题台账提炼的案例，它们的来源与状态见 `manifest.json`，不等同于原云文档的每一条原始工单。
