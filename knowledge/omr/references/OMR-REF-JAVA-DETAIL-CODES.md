# OMR Java 业务详细错误码 DetailErrorStatus

来源：当前本地 Java Web 服务 `constants/DetailErrorStatus.java`。这些是 Java 业务详细异常，不是统一原生引擎返回码。每条保留所属层级，避免独立向量片段丢失表头。

## OMR Java DetailErrorStatus 4：THREE_POINT

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：三点校正

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 4 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 17：TICKING_INCOMPLETE

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：对错框残缺

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 17 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 18：BLANK_PAPER

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：空白页

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 18 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 19：NOT_FOUND_LINE

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：找不到线条

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 19 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 20：AnswerSheetStructError

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：答题卡结构错误

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 20 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 2：DETAILERRORSTATUS_UNCORRECT_FILLINGBLANK_ERROR

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：打分条未批改（填空题）

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 2 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 11：DETAILERRORSTATUS_UNCORRECT_ANSWERQUESTION_ERROR

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：打分条未批改（解答题）

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 11 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 301：DETAILERRORSTATUS_ENGINE_ERROR

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：打分条引擎异常

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 301 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 302：DETAILERRORSTATUS_CALCULATE_SCORE_ERROR

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：打分条计算得分异常

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 302 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 303：DETAILERRORSTATUS_EMPTY_ERROR

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：打分条没有红笔分数线

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 303 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 304：DETAILERRORSTATUS_MULTIGRID_ERROR

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：打分条划了多个格子

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 304 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 305：DETAILERRORSTATUS_MULTIGRID_CONFIDENCE_ERROR

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：打分条划了多个格子 前两个格子综合置信度得分差值小于0.1

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 305 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 306：DETAILERRORSTATUS_SCORE_ERROR

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：打分条识别结果分大于标准分

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 306 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 307：DETAILERRORSTATUS_BLANK_ERROR

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：打分条存在红笔划在空白格的情况

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 307 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 308：DETAILERRORSTATUS_CROSS_ERROR

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：打分条存在红笔跨格子的情况

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 308 套用原生算法码或 Java ErrorType。

## OMR Java DetailErrorStatus 309：DETAILERRORSTATUS_TOOSMALL_ERROR

所属层级：Java Web 业务详细错误码 DetailErrorStatus。

源码含义：打分条存在红笔过小可能是杂点的情况

使用前提：先确认日志/响应中该字段确实由 DetailErrorStatus 生成。不能仅凭数值 309 套用原生算法码或 Java ErrorType。

## 常见混淆与来源

DetailErrorStatus 的 2 是填空题打分条未批改，11 是解答题打分条未批改，17 是对错框残缺，20 是答题卡结构错误。相同数字在 Java ErrorType 中分别可表示引擎 Bug、计算分数错误、制卡失败和折角，必须按字段来源区分。

来源：`srv-pt-scan-recognization/web/api-omr-engine/src/main/java/com/iflytek/edu/zx/omr/web/constants/DetailErrorStatus.java`，以及云文档《OMR-引擎错误码.md》的“服务错误码”章节。实际部署仍需核对版本。
