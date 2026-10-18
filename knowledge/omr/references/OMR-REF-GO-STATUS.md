# OMR Go 选项接口的返回状态分层

适用范围：当前 Go 封装服务的 `/v1/option`。外层响应、图片级状态、题块级识别码分别判断；不把这些数字套用到 Java ErrorType、DetailErrorStatus 或其他 C/C++ 接口。

## Go /v1/option 外层 Code

所属层级：`OptionResponse.Code`。0 表示处理器正常组装返回，-1 是初始状态及读取、反序列化、请求校验失败路径的状态，-2 表示识别封装返回 Go error。HTTP 200 或外层 0 不保证每张图片和每个题块成功。

请求中的 traceId 可复用，未提供时由处理器生成。OMR 自身也使用 traceId，不能据此直接把问题归为其他批改服务。

## Go /v1/option 图片级 ImgCode

所属层级：图片结果 `ImgCode`。0 表示进入题块处理，1 表示下载失败，2 表示 Base64 解码失败。其数字 2 不能解释为 Java/原生通用错误码的“引擎 Bug”。

图片失败可形成该图片的错误结果，其他图片仍有可能继续处理。使用 ImageIndex 对应输入，不能仅按返回数组位置判断。

## Go /v1/option 题块级 RecCode

所属层级：`OptionResult.RecCode`。0 表示正常识别，40 为 NotConfirm（有结果但不确定），37 为 Reject。原生调用中除 0、40 外的错误会在该封装路径形成拒识结果，不能把题块 37 当成所有算法接口的统一根因。

题块结果还需按 TopicIndex、实际答案及题型检查，不能只看外层成功。

## Go /v1/option 填涂规则的负数

所属层级：题块填涂状态。-1 为未涂，-2 为多选题少涂，-3 为单选题多涂，-4 为未推导。OptionTag 0/1/2 分别为单选、多选、推导。

这些负数与 Java 客户端的连接、调用、超时错误不属于同一层。规则判断不能解释为与标准答案比对后的判分结果。

## Go /v1/option 来源与边界

源码：`srv-pt-recognization-engine-container/src/iflytek.com/OMRService/common/constant/resp_code_const.go`、`server/http_handler/option_handler.go`、`server/grpc_handler/engine_option.go`。

以上为本地源码行为摘要，未发送真实识别请求。其他 HTTP/gRPC 处理器可能有不同的外层包装和错误传播，应核对实际接口、运行版本和字段。
