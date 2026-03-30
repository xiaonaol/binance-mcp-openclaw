# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run a single test
./mvnw test -Dtest=BinanceApplicationTests
```

Requires Java 21.

## Architecture

This is a **Spring Boot 4.0.5** app (with Spring AI MCP Server) that monitors ETH/USDT price on Binance and sends alerts via a QQ group bot (NapCat/OneBot protocol).

### Core Data Flow

```
Binance WebSocket
    └─> BinancePriceListener (CommandLineRunner, OkHttp WS, auto-reconnects)
            └─> AlertCheckService.check(price)
                    └─> AlertStore (in-memory, ConcurrentHashMap keyed by QQ user ID)
                    └─> QQNotifyService.sendGroupMsg() on trigger
```

### QQ Command Flow

```
NapCat HTTP callback POST /napcat/event
    └─> NapCatEventController (filters @bot mentions in group messages)
            └─> AlertCommandHandler.handle()
                    └─> AlertStore (add/list/delete alerts)
                    └─> QQNotifyService.sendGroupMsg() (reply)
```

### Key Components

- **`BinancePriceListener`** — connects to Binance WebSocket on startup, parses `c` field (last price) from ticker JSON, triggers check on every tick.
- **`AlertStore`** — sole state store, in-memory only (no DB persistence despite MySQL dependency in pom.xml). Alerts are lost on restart.
- **`AlertCheckService`** — evaluates ABOVE/BELOW conditions; enforces per-alert cooldown (`alert.cooldown-seconds`, default 300s) to prevent spam.
- **`AlertCommandHandler`** — parses Chinese QQ commands prefixed with `eth预警` (上/下/列表/删除/帮助).
- **`QQNotifyService`** — calls NapCat HTTP API (`/send_group_msg`, `/send_private_msg`) with optional Bearer token auth.
- **`AlertController`** — REST admin API at `/api/alerts` for managing alerts without QQ.

### Configuration (`application.yaml`)

| Key | Purpose |
|-----|---------|
| `alert.binance-ws-url` | Binance WebSocket stream URL (default: `ethusdt@ticker`) |
| `alert.qq-bot-api` | NapCat HTTP API base URL |
| `alert.bot-qq` | Bot's own QQ number (used to detect @mentions) |
| `alert.qq-user-id` | Default QQ user for private message fallback |
| `alert.napcat-token` | Bearer token for NapCat API auth (optional) |
| `alert.cooldown-seconds` | Alert re-trigger cooldown in seconds (default: 300) |

### Alert State

`PriceAlert` has a `triggered` flag and `lastTriggeredAt` timestamp. When triggered, the alert enters cooldown rather than being removed — it will fire again after cooldown expires. Use `POST /api/alerts/{alertId}/reset` to manually clear cooldown.
