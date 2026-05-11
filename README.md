# jyue Burp 插件

基于 Burp Montoya API 的越权检测辅助插件。插件面板支持代理自动采集、右键手动发送、低权限认证重放、未授权 Header 移除重放，以及同域名同方法同路径 API 自动去重。

## 说明

本项目参考 `xia_yue` 的使用思路进行调整，重点修复和增强了以下问题：

- 兼容新旧 Burp Montoya API，避免旧版 Burp 因 `pathWithoutQuery()`、`mimeType()` 等方法缺失导致插件报错。
- 自动检测会实时显示队列状态，请求进入队列后先显示 `排队中`，处理时显示 `检测中`，响应返回后实时更新表格和请求/响应面板。
- 自动检测复用 Proxy 已捕获的原始请求/响应，低权限和未授权请求单独重放。
- 低权限和未授权请求增加超时控制，避免长时间卡在 `检测中`。
- 原始、低权限、未授权数据包分开显示 Request/Response，减少旧版 API 下展示空白的问题。
- 导出 CSV 使用 UTF-8 BOM，改善中文乱码；长请求/响应会截断，完整内容可通过右键复制。
- 错误会写入 Burp `Extender -> Errors`，同时在面板中保留失败记录。

## 构建

```bash
gradle jar
```

生成的插件 jar 位于：

```text
build/libs/jyue-1.0.0.jar
```

在 Burp Suite 的 `Extensions -> Installed -> Add` 中选择 Java 类型并加载该 jar。

## 使用

- 勾选 `启动插件` 后，Proxy 流量会自动进入检测队列。
- `白名单域名` 支持域名、IP、URL、`*.example.com`，多个请用逗号、空格或换行分隔；只有勾选 `启动白名单` 后才按白名单过滤。
- `启动白名单` 后白名单输入框会锁定；此时只有框里的 host/IP 会自动检测。
- `低权限认证信息` 每行填写一个请求头，例如 `Authorization: Bearer low-token`。
- `未授权移除头` 每行填写一个要删除的 Header 名称，例如 `Cookie`。
- 自动采集会按 `scheme + host + port + method + path` 去重；右键手动发送不受该去重限制。
- 结果表中 `低权限长度差` 和 `未授权长度差` 显示为对应响应长度减去原始响应长度。
- 原始、低权限、未授权数据包使用 Burp 原生 Request/Response 编辑器分栏展示，减少中文内容被插件二次转码导致的乱码。
- 结果表支持关键词和类型筛选；右键可复制 URL、原始/低权限/未授权请求包和响应包。
- `导出当前表格` 会导出当前筛选后的结果，默认文件名为 `jyue_yyyyMMdd_HHmmss.csv`，并把三类请求/响应一起写入 UTF-8 BOM CSV；长请求/响应会截断，右键复制可获取完整内容。
