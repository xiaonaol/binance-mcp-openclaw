# binance-mcp-openclaw

基于 Spring Boot 的 ETH/USDT 价格预警机器人，通过 Binance WebSocket 实时监控价格，在 QQ 群内发送预警通知，并以 MCP Server 形式供 AI 助手（如 openclaw）调用。

## 功能

- 实时监控 ETH/USDT 价格（Binance WebSocket）
- QQ 群内通过 `@机器人 eth预警` 指令管理预警
- 价格触发时 @ 用户发送群通知，触发后进入冷却期防止刷屏
- 以 MCP Server 暴露工具接口，供 AI 助手直接调用

## 架构

```
Binance WebSocket
    └─> BinancePriceListener
            └─> AlertCheckService.check(price)
                    ├─> AlertStore（内存存储）
                    └─> QQNotifyService.sendGroupMsg()（触发时通知）

NapCat HTTP 回调 POST /napcat/event
    └─> NapCatEventController（过滤 @机器人 消息）
            └─> AlertCommandHandler（解析 eth预警 指令）
                    ├─> AlertStore
                    └─> QQNotifyService

MCP Server（SSE，供 openclaw 等 AI 助手接入）
    └─> BinanceAlertMcpTools
            ├─> getCurrentEthPrice
            ├─> addPriceAlert
            ├─> listPriceAlerts
            └─> deletePriceAlert
```

## 快速开始

### 环境要求

- Java 21
- Maven（或使用项目内置的 `./mvnw`）

### 配置

复制配置模板并填入真实值：

```bash
cp src/main/resources/application.yaml src/main/resources/application-local.yaml
```

编辑 `application-local.yaml`：

```yaml
alert:
  qq-bot-api: http://你的NapCat地址:3000
  bot-qq: 机器人QQ号
  qq-user-id: 默认通知的QQ号
  napcat-token: NapCat的Bearer Token
```

### 运行

```bash
# 使用本地配置文件启动
JAVA_HOME=/path/to/jdk-21 ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# 或通过环境变量配置
QQ_BOT_API=http://... BOT_QQ=... NAPCAT_TOKEN=... ./mvnw spring-boot:run
```

### 构建

```bash
./mvnw clean package -DskipTests
java -jar target/binance-0.0.1-SNAPSHOT.jar
```

## QQ 指令

在 QQ 群内 @ 机器人发送以下指令：

| 指令 | 说明 |
|------|------|
| `eth预警 上 2500 止盈` | 价格突破 $2500 时通知 |
| `eth预警 下 1900 止损` | 价格跌破 $1900 时通知 |
| `eth预警 列表` | 查看我的预警列表 |
| `eth预警 删除 <ID前8位>` | 删除指定预警 |
| `eth预警 帮助` | 显示使用说明 |

## MCP Server 接入

服务启动后，AI 助手可通过 SSE 协议接入：

```
http://你的服务器IP:8080/sse
```

openclaw 配置示例：

```json
{
  "mcpServers": {
    "binance-alert": {
      "url": "http://你的服务器IP:8080/sse"
    }
  }
}
```

接入后 AI 助手可使用以下工具：

| Tool | 说明 |
|------|------|
| `getCurrentEthPrice` | 查询 ETH 当前实时价格 |
| `addPriceAlert` | 为指定 QQ 用户添加价格预警 |
| `listPriceAlerts` | 查询用户的预警列表 |
| `deletePriceAlert` | 删除指定预警 |

## REST API

服务同时提供 HTTP 管理接口：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/alerts/{userId}` | 查询用户预警 |
| POST | `/api/alerts/{userId}` | 添加预警 |
| DELETE | `/api/alerts/{userId}/{alertId}` | 删除预警 |
| POST | `/api/alerts/{alertId}/reset` | 重置冷却状态 |

## 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `alert.binance-ws-url` | `wss://stream.binance.com:9443/ws/ethusdt@ticker` | Binance WebSocket 地址 |
| `alert.qq-bot-api` | — | NapCat HTTP API 地址 |
| `alert.bot-qq` | — | 机器人自身 QQ 号 |
| `alert.napcat-token` | — | NapCat Bearer Token（可选） |
| `alert.cooldown-seconds` | `300` | 预警触发后冷却时间（秒） |

## 技术栈

- Spring Boot 4.0.5
- Spring AI MCP Server 2.0.0-M3
- OkHttp（Binance WebSocket）
- NapCat / OneBot v11 协议
