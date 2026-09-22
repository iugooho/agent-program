# 错误码

前端只根据 `code` 判分支，**不要解析 `message` 文案**（文案会改，码不会）。
`code = 0` 表示成功，其余一律是失败；失败时 `data` 为 `null`，`message` 只给人看。

## 响应形状

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

失败时同时给出 HTTP 状态码与业务码：HTTP 状态用于网关、日志、监控的粗粒度判断，
`code` 用于前端精确定位。两者一一对应，见下表。

## 段位划分

新增模块先挑一个没被占用的段位，再往 `ErrorCode` 里加枚举、往本文件登记，不要复用别的模块的号。

| 段位 | 归属 |
| --- | --- |
| 0 | 成功（`ApiResponse.CODE_SUCCESS`） |
| 10xx | 通用：参数、请求格式、路由、鉴权 |
| 20xx | 行程 itinerary |
| 21xx | 用户与登录（预留，未使用） |
| 90xx | 系统与依赖 |

## 码表

| code | HTTP | 含义 | 触发场景 |
| --- | --- | --- | --- |
| 0 | 200 | 成功 | - |
| 1000 | 400 | 参数校验失败 | `@Valid` / `@Validated` 不通过，message 为「字段名 + 原因」 |
| 1001 | 400 | 请求体格式错误 | body 不是合法 JSON、字段类型不匹配 |
| 1002 | 404 | 资源不存在 | 路径没匹配到任何接口 |
| 1003 | 405 | 请求方法不支持 | 路径存在但 HTTP 方法不对 |
| 1004 | 401 | 未认证或登录状态已失效 | 预留，等登录落地后由 Security 层返回 |
| 1005 | 403 | 没有访问权限 | 预留，同上 |
| 2000 | 404 | 行程不存在 | 按 ID 查行程查不到 |
| 2001 | 422 | 行程生成失败 | 大模型或外部工具的结果拼不成行程 |
| 9000 | 500 | 服务器内部错误 | 未预期的异常，细节只进日志 |

## 怎么用

业务代码直接抛，不要自己 try-catch 拼响应：

```java
throw new BusinessException(ErrorCode.ITINERARY_NOT_FOUND);
throw new BusinessException(ErrorCode.ITINERARY_NOT_FOUND, "行程 " + id + " 不存在");
```

- `ErrorCode`、`BusinessException` 在 `com.travelagent.travelplanner.common.error`，
  **不依赖任何框架类型**（HTTP 状态用 int 表达），所以 domain 与 application 层可以直接抛。
- 转换发生在 `com.travelagent.travelplanner.api.GlobalExceptionHandler`（`@RestControllerAdvice`）。
- 日志策略：业务异常、参数校验失败打 warn 且不带堆栈；未预期的异常打 error 并带堆栈。
  两者都给前端回同一形状的响应。
- Security 过滤器链里的 401 / 403 不经过这个处理器，等登录落地后在那一层返回 1004 / 1005。

## 注意

- **已有码的含义不要改。** 前端可能已经在判 `code == 2000`，改含义等于静默改行为；换语义就新增码。
- 不要把异常堆栈、SQL、下游返回的原始报文放进 `message` 返回给前端。
- 前端拿到失败响应的落点在 `travel-planner/frontend/src/api/http.ts` 的响应拦截器（目前只是原样抛出），
  接入业务时在这里按 `code` 分支处理，不要按文案判断。
