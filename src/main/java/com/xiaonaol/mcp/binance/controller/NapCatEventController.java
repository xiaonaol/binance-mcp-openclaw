package com.xiaonaol.mcp.binance.controller;

import com.xiaonaol.mcp.binance.handler.AlertCommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/napcat")
@RequiredArgsConstructor
public class NapCatEventController {

    @Value("${alert.bot-qq}")
    private long botQQ;

    private final AlertCommandHandler commandHandler;

    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> onEvent(@RequestBody JsonNode event) {
        if (!"message".equals(event.path("post_type").asText()) ||
                !"group".equals(event.path("message_type").asText())) {
            return ResponseEntity.ok(Collections.emptyMap());
        }

        String rawMessage = event.path("raw_message").asText().trim();
        String atBot = "[CQ:at,qq=" + botQQ + "]";

        if (rawMessage.contains(atBot)) {
            String command = rawMessage.replace(atBot, "").trim();
            if (command.startsWith("eth预警")) {
                long userId = event.path("user_id").asLong();
                long groupId = event.path("group_id").asLong();
                commandHandler.handle(groupId, userId, command);

                // 告诉NapCat不要再把这条消息转发给其他端
                return ResponseEntity.ok(Map.of("block", true));
            }
        }

        return ResponseEntity.ok(Collections.emptyMap());
    }
}
