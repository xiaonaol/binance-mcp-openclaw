package com.xiaonaol.mcp.binance.service.qq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QQNotifyService {
    private final RestTemplate restTemplate;

    @Value("${alert.qq-bot-api}")
    private String botApi;

    @Value("${alert.qq-user-id}")
    private Long qqUserId;

    @Value("${alert.napcat-token:}")  // 没设置token就留空
    private String accessToken;

    public void sendPrivateMsg(String message) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("user_id", qqUserId);
            body.put("message", message);

            restTemplate.postForObject(
                    botApi + "/send_private_msg",
                    body,
                    String.class
            );
            log.info("QQ通知发送成功");
        } catch (Exception e) {
            log.error("QQ通知发送失败", e);
        }
    }

    public void sendGroupMsg(long groupId, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(accessToken)) {
                headers.set("Authorization", "Bearer " + accessToken);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("group_id", groupId);
            body.put("message", message);

            restTemplate.postForEntity(
                    botApi + "/send_group_msg",
                    new HttpEntity<>(body, headers),
                    String.class
            );
        } catch (Exception e) {
            log.error("群消息发送失败, groupId={}", groupId, e);
        }
    }
}
