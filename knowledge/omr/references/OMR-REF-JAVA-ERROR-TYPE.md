# OMR Java 引擎客户端 ErrorType

来源：当前本地 Java 工作区 `service/engine-jni/.../utils/ErrorType.java`。这是 Java 客户端的错误和告警枚举，包含透传原生码及 Java 自定义状态；不等同于 DetailErrorStatus 或 Go RecCode。

以下描述来自源码映射/注释，未核对全部现网发布版本。当前客户端对未知编号的 getEnum 会回退到 ErrorNO，因此查询映射得到“正常”不能替代检查真实原始返回码。

## OMR Java ErrorType -4：ErrorNoIdleObject

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：无可用连接

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType -3：ErrorEngineCrash

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：引擎崩溃

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType -2：ErrorTimeOut

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：超时错误

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType -1：ErrorCallFail

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：调用失败

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 0：ErrorNO

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：识别正常

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1：ErrorLocaltionInfo

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：定位信息参数错误

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 2：ErrorCodeBug

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：引擎内部代码BUG，被异常捕获

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 3：ErrorModify

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：大图矫正错误

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 4：ErrorThreeBlock

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：正常矫正，不过为三点矫正

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 5：ErrorCalibPtMemFailed

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：定位信息为空

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 6：ErrorLocationBlockMiss

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：未找到定位点

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 7：ErrorNoImg

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：没有图片

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 8：ErrorTransMatrixMemFailed

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：转化矩阵失败

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 9：ErrorCalibPtMiss

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：定位点丢失，暂时停用

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 10：ErrorExternInputError

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：外部输入参数错误

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 11：ErrorScoreFail

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：计算分数错误，暂时停用

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 12：ErrorAnswerPosDet

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题定位失败

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 13：ErrorDirectBlock

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：方向块（标题）检测失败

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 14：ErrorBufferNotEnough

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题存放结果内存不够

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 15：ErrorArtificialRejionBlock

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：手动矫正失败

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 16：ErrorInvalidConfig

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：参数错误，无法创建引擎

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 17：ErrorFailed

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：制卡失败

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 18：ErrorBlankImage

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：空白图

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 19：ErrorInvalidLine

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：无法找到线条

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 20：ErrorPaperBuckling

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：存在折角

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 21：ErrorMultiLocationBlock

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：多个定位点

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 22：ErrorTitleCorrectFailed

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：标题计算角度失败

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 23：ErrorLineRegion

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：画线段区域无效

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 24：ErrorTitlewordsNum

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：标题字数少

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 25：ErrorLocateObjSmear

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：制卡识别客观题的时候定位成功但是检测到模板有涂抹

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 26：ErrorSmiliarTitleArea

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：标题周围存在类似标题

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 27：ErrorBigObjModifyError

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：修正过成功出现错误，停止识别

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 28：ErrorBigObjModifyCheck

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：当前客观题大图有超过一半的题块被修正，需要人工过一遍所有的题块

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 101：ErrorObjOffset

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题偏移告警

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 102：ErrorObjCheck

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题识别不确定预警（非浓淡等其他场景）

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 103：ErrorObjPrintDeep

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题打印浓场景预警

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 104：Error0bjPrintLight

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题打印淡场景

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 105：ErrorObjTitleLocation

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题标题定位

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 111：ErrorModifyResult

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题置信度修改场景预警

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 121：ErrorObjEraser

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题擦除预警

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 197：ErrorObjReliability

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题异常置信度

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 198：ErrorObjStructure

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题结构偏移

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 199：ErrorObjManualConfirm

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：人工确认

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 998：ErrorObjUncertain

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题浓淡场景结果不确信

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 999：ErrorOldCorrect

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：需要确认图片是否矫正准确，不覆盖点线矫正图片

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1000：ErrorNewCorrect

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：需要确认图片是否矫正准确，特征校准通过且图片覆盖原矫正图片

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType -999：ErrorImgNull

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：图片初始化为空

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType -1000：ErrorObjCheckDefault

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题审查默认状态码(拒识）

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1001：ErrorPublicNoImg

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：图片不存在

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1002：ErrorPublicImdecodeFail

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：图片二进制转化mat失败

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1003：ErrorPublicParamInputError

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：通用参数错误

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1004：ErrorPublicTitleError

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：标题匹配错误

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1013：ErrorCorrectTitleError

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：特征矫正通过-标题审查未通过

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1101：ErrorTemplateNOFeature

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：模板结构没特征点

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1102：ErrorSrcNoFeature

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：扫描结构没特征点

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1103：ErrorLessFeaturePoint

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：匹配后点太少

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1104：ErrorTemplateDataNull

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：模板结构无数据

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1105：ErrorsrcDataNull

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：扫描结构无数据

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1200：ErrorCheckError

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：审查异常

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1300：ErrorObjFastCheck

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：弱审查告警状态码

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1301：ErrorObjFastCheckOffset

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：弱审查结构偏移

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1302：ErrorObjFastCheckStructError

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：弱审查图片结构化异常

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1303：ErrorObjFastCheckObjNumError

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：弱审查题号无法定位

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1401：ErrorExactCheckOffset

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：强审查结构偏移

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1402：ErrorExactStructError

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：强审查图片结构化异常

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1501：ErrorModleCheckOffset

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题单选项模型检测错误码

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 1601：ErrorAllObjCheckOffset

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：客观题全局检测错误码

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 29：ErrorGenGrid

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：老模式的打分条定位错误

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 30：ErrorModelBlockNotValid

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：输入的定位点信息无效

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 31：ErrorImgBlockNotValid

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：图像的定位点无效

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 32：ErrorModelError

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：输入定位点信息缺失

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 33：ErrorHVbarsWrong

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：打分条横竖线超出图像范围

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 34：ErrorGenGridNewMode

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：打分条横竖线数量不对

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 35：ErrorRotateAngleWrong

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：打分条旋转角度超过8°

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 201：ErrorBarScoringBgColorWrong

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：打分条背景颜色异常

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 202：ErrorBarScoringRedCrossMultiGrids

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：打分条红笔跨过多个格子

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 203：ErrorBarScoringWarp

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：打分条扭曲严重

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 204：ErrorBarScoringGridRed

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：打分条格子泛红竖线

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 205：ErrorBarScoringWarp2

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：打分条格子计数异常

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## OMR Java ErrorType 206：ErrorBarScoringRedTooSmall

所属层级：Java engine-jni 客户端 ErrorType，需结合实际引擎调用及消费 JAR 版本。

源码含义：打分条格子红笔过小

边界：该编号不用于解释 Java DetailErrorStatus、Go 外层/图片级/题块级状态。是否中断、继续识别或仅标记复核，需要看调用点分支，不能只按非零判断。

## 源码与特殊值核对

来源：`srv-pt-scan-recognization/service/engine-jni/src/main/java/com/iflytek/edu/zx/engine/jni/utils/ErrorType.java`。当前本地工作区不代表所有服务器的实际 JAR。

其中 20 是折角；1000 是 Java 自定义的特征校准后覆盖图片并需确认校正的告警；1001 是通用图片不存在。它们不能套用历史原生码表中 1000/1001 的制卡行列解释。
