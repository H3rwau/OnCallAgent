# OMR 无点无线校正的适用条件与版本边界

来源为《20260108-OMR无点无线矫正引擎上线材料》以及当前 Java HTTP 调用与 CV 接口。发布说明属于历史记录，不能单凭它判断现网当前部署版本。

## OMR 作业无点无线的 Java 入口

普通作业校正与页码二维码流程存在无点无线补救分支，最终调用 `Correct.checkCorrectWithoutPoint` 或 `checkCorrectWithoutPointV2`。两者通过 HTTP 请求 `maasWithoutPointUrl`。

并非每次扫描都会触发。校正成功等分支可提前返回，STUADM_THIRD 等卡型路径也不能直接套用普通分支。Go 未直接调用 PWL 符号不能证明 Java 没有接入这项能力。

## OMR 无点无线请求与结果

请求包含 header、parameter、payload，复用业务 trace；图像参数包含模板与待补救图。V2 使用 without_page_check，由 Java 对应业务流程处理页码。

响应的 header.code、输出图和变换矩阵需要按实际包装协议检查，不能用某个 engineCode 直接查询通用 OMR 码表。输入图可能是当前校正阶段的工作图，不可一概当作原始扫描图。

## OMR 无点无线的算法与服务包装边界

CV 仓库 `omr-engine-cv-common` 提供 `EnginePWLCorrectImage`、`EnginePWLGetPageNumber` 等接口。Java → HTTP 调用由源码确认，但 HTTP 包装层如何加载原生库及实际发布版本需另行核验。

`libomr_engine_cv_common.so` 与 `libai_omr_engine_common.so` 是不同库。头文件、符号、协议和对应版本一起核对，不能只凭同名导出符号判定 ABI 兼容。

## OMR 无点无线历史发布记录

云文档记录 1.1.2 首次上线、1.1.3 修复、1.1.4 新版页码能力，以及 2026-01-08 的 1.1.5 黑白边优化。它们是源文档的发布历史，不表示所有环境都处于其中某一版本。

历史发布包包含算法库、pwl_config.cfg、HTTP 包装服务及运行配置。组件关系可以作为排查线索；旧机器、端口、并发数与直接回滚命令不作为当前通用配置或修复步骤。

## OMR 无点无线失败的排查顺序

先确认本次请求应进入补救分支，再核对 Spring Bean/配置中心给出的有效 URL、HTTP 服务可达性、真实发送的图像与模板、响应分层码、图像结果及实际算法版本。

图像标记缺损、定位失败和包装错误需分开判断。V2 后续会上传校正图，该识别链路具有写入副作用，不能拿识别请求作只读探测。

## OMR 无点无线知识的来源

Java 源码：`web/api-omr-engine/.../Service/homework/Correct.java`、`ZXHomeworkServiceImpl.java`、`ImageProcessController.java`。算法接口：`omr-engine-cv-common/src/correct_point_and_wire_less/engine_api.h`。

配套资料：Java 仓库 `docs/cross-repository/without-point-http.md`、`version-drift.md`，以及云文档《20260108-OMR无点无线矫正引擎上线材料.md》。当前本地源码与历史发布记录均不替代运行实例核验。
