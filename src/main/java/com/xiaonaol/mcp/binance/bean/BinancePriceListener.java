package com.xiaonaol.mcp.binance.bean;

import com.xiaonaol.mcp.binance.service.alert.AlertCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinancePriceListener implements CommandLineRunner {

    private final AlertCheckService alertCheckService;

    @Value("${alert.binance-ws-url}")
    private String wsUrl;


    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)  // 心跳，防止连接被断
            .build();

    @Override
    public void run(String... args) {
        connectWebSocket();
    }

    private void connectWebSocket() {
        Request request = new Request.Builder().url(wsUrl).build();

        okHttpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("Binance WebSocket 连接成功");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JsonNode node = new ObjectMapper().readTree(text);
                    BigDecimal price = new BigDecimal(node.get("c").asText());
                    log.debug("ETH/USDT: {}", price);
                    alertCheckService.check(price);
                } catch (Exception e) {
                    log.error("解析行情数据失败", e);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("WS连接失败，5秒后重连...", t);
                reconnect();
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.warn("WS连接断开：{}，5秒后重连...", reason);
                reconnect();
            }
        });
    }

    private void reconnect() {
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS)
                .execute(this::connectWebSocket);
    }
}
