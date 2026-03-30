package com.xiaonaol.mcp.binance.handler;

import com.xiaonaol.mcp.binance.bean.AlertStore;
import com.xiaonaol.mcp.binance.enums.AlertDirection;
import com.xiaonaol.mcp.binance.model.PriceAlert;
import com.xiaonaol.mcp.binance.service.qq.QQNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCommandHandler {

    private final AlertStore alertStore;
    private final QQNotifyService qqNotifyService;

    // 指令前缀
    private static final String PREFIX = "eth预警";

    public void handle(long groupId, long userId, String message) {
        if (!message.startsWith(PREFIX)) return;

        String[] parts = message.substring(PREFIX.length()).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            sendHelp(groupId, userId);
            return;
        }

        switch (parts[0]) {
            case "上", "下" -> handleAdd(groupId, userId, parts);
            case "列表"     -> handleList(groupId, userId);
            case "删除"     -> handleDelete(groupId, userId, parts);
            case "帮助"     -> sendHelp(groupId, userId);
            default         -> sendHelp(groupId, userId);
        }
    }

    // eth预警 下 1900 止损位
    private void handleAdd(long groupId, long userId, String[] parts) {
        if (parts.length < 2) {
            qqNotifyService.sendGroupMsg(groupId, atUser(userId) + " 格式错误，示例：eth预警 下 1900 止损");
            return;
        }
        try {
            AlertDirection direction = "上".equals(parts[0]) ? AlertDirection.ABOVE : AlertDirection.BELOW;
            BigDecimal targetPrice = new BigDecimal(parts[1]);
            String note = parts.length >= 3 ? parts[2] : null;

            PriceAlert alert = PriceAlert.builder()
                    .id(UUID.randomUUID().toString())
                    .groupId(groupId)
                    .targetPrice(targetPrice)
                    .direction(direction)
                    .note(note)
                    .triggered(false)
                    .build();

            alertStore.add(userId, alert);

            String dirText = direction == AlertDirection.ABOVE ? "📈 突破" : "📉 跌破";
            qqNotifyService.sendGroupMsg(groupId,
                    atUser(userId) + String.format(" 预警已设置✅\n%s $%s\n备注：%s\nID：%s",
                            dirText, targetPrice.toPlainString(),
                            note != null ? note : "无",
                            alert.getId().substring(0, 8))  // 只显示ID前8位
            );
        } catch (NumberFormatException e) {
            qqNotifyService.sendGroupMsg(groupId, atUser(userId) + " 价格格式不对，请输入数字");
        }
    }

    // eth预警 列表
    private void handleList(long groupId, long userId) {
        List<PriceAlert> alerts = alertStore.getByUser(userId);
        if (alerts.isEmpty()) {
            qqNotifyService.sendGroupMsg(groupId, atUser(userId) + " 你还没有设置任何预警");
            return;
        }

        StringBuilder sb = new StringBuilder(atUser(userId) + " 你的ETH预警列表：\n");
        for (PriceAlert a : alerts) {
            String dirText = a.getDirection() == AlertDirection.ABOVE ? "📈突破" : "📉跌破";
            String status = a.isTriggered() ? "⏸冷却中" : "✅监控中";
            sb.append(String.format("• [%s] %s $%s  %s  %s\n",
                    a.getId().substring(0, 8),
                    dirText,
                    a.getTargetPrice().toPlainString(),
                    a.getNote() != null ? a.getNote() : "",
                    status
            ));
        }
        qqNotifyService.sendGroupMsg(groupId, sb.toString().trim());
    }

    // eth预警 删除 a1b2c3d4
    private void handleDelete(long groupId, long userId, String[] parts) {
        if (parts.length < 2) {
            qqNotifyService.sendGroupMsg(groupId, atUser(userId) + " 请输入要删除的预警ID（列表里显示的前8位）");
            return;
        }
        String idPrefix = parts[1];
        List<PriceAlert> alerts = alertStore.getByUser(userId);
        Optional<PriceAlert> target = alerts.stream()
                .filter(a -> a.getId().startsWith(idPrefix))
                .findFirst();

        if (target.isEmpty()) {
            qqNotifyService.sendGroupMsg(groupId, atUser(userId) + " 未找到该预警，请检查ID");
            return;
        }
        alertStore.remove(userId, target.get().getId());
        qqNotifyService.sendGroupMsg(groupId, atUser(userId) + " 预警已删除✅");
    }

    private void sendHelp(long groupId, long userId) {
        String help = atUser(userId) + """
             ETH价格预警使用说明：
            ─────────────────
            设置预警：eth预警 上/下 <价格> [备注]
              例：eth预警 下 1900 止损位
              例：eth预警 上 2500 止盈
            
            查看列表：eth预警 列表
            删除预警：eth预警 删除 <ID前8位>
            ─────────────────
            价格触发后会@你通知 🔔
            """;
        qqNotifyService.sendGroupMsg(groupId, help);
    }

    private String atUser(long userId) {
        return "[CQ:at,qq=" + userId + "]";
    }
}
