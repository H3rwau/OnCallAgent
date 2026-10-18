# OMR 业务职责与服务调用链

本说明整理自云文档《OMR-交接》《OMR-服务》及本地源码资料。仅保留可复用的职责、流程和调用边界，未复制口令、机器清单、访问入口或业务样本。

## OMR Java、Go 与算法层职责

Java 扫描服务接收图像和结构化答题卡模板，编排校正、考号、题目识别和结果整理。Go 封装负责 RPC/HTTP 协议、参数转换和 cgo 调用。图像算法由原生动态库执行。

典型路径是 Java Web → engine-jni 客户端 JAR → gRPC → Go → cgo → 算法库。`engine-jni` 名称不代表这些调用都由 Java 本地 JNI 执行。ZooKeeper 提供服务地址，不执行识别算法。

## OMR Java 常见接口入口

`/imageProcess/scanProcess` 是扫描识别主入口，`/imageProcess/objectCheck` 为客观题审查相关入口，`/imageProcess/detectDirtyLine` 为脏线检测入口。它们的请求结构和结果不能互相套用，完整访问地址还取决于部署 context-path。

云文档《OMR-服务》对 objectCheck 的概括较宽，当前 Controller 实际进入审查流程；不能把接口名理解为所有云端客观题识别能力的统一入口。

## OMR 三类主引擎与独立路由维度

智学/网阅、作业/手阅、三方分别有适用能力与调用分支。`answerSheetFrom` 选择主引擎来源，`answerSheetType` 选择普通、V2、V3 卡型流程，`recognizeTypeEnum` 选择正常扫描、首张校验或异常恢复；题型与 scoreType 再决定逐题能力。

不能仅凭“作业卡”或一个监控字段推断所有后续算法都一致。AB 卡模板页选择与 AB 卷填涂是不同概念。

## OMR 主要仓库的边界

Java 服务：`srv-pt-scan-recognization`，重点为 `web/api-omr-engine` 和 `service/engine-jni`。Go 封装：`srv-pt-recognization-engine-container`。条码/二维码及部分 Common 算法：`srv-pt-recognization-engine`。无点无线 CV 算法：`omr-engine-cv-common`。

`ai_omr_engine_common` 与 `omr_engine_cv_common` 是不同库。协议、头文件和随包动态库可能不与当前源码 checkout 完全同步，不能按文件名相似直接替换。

## OMR 无点无线与主引擎路径的区别

作业的无点无线兜底存在独立 HTTP 调用：Java `Correct.checkCorrectWithoutPoint/V2` → `maasWithoutPointUrl`。它不是所有请求必经的步骤，也不等同于所有名为 titleCorrect 的 gRPC 能力。

HTTP 服务包装层、原生库版本和本地源码是不同证据，需分别核验。详细知识见无点无线参考文档。

## OMR 识别与上游业务处理的职责边界

识别出考号不代表学生名册匹配已成功。本地源码没有证明 Java 扫描服务负责考试创建、学生关系维护、阅卷分发或成绩入库。主观题读取老师批改痕迹，也不能描述为理解任意主观答案并自动评分。

业务归属应结合接口、模板、题型、实际结果和调用链，而不是只凭错误提示或有没有 traceId。

## OMR 架构说明的来源

云文档：《OMR-交接.md》《OMR-服务.md》。代码资料：Java 仓库 `docs/java-service/responsibilities.md`、`scan-workflow.md`、`docs/cross-repository/architecture.md`、`without-point-http.md` 和 `version-drift.md`。

源码入口为 `ImageProcessController`、各 ServiceImpl、`engine-jni` 的引擎客户端及 Go 服务处理器。本说明是本地代码和历史资料摘要，不作为现网节点、端口或发布版本的证明。
