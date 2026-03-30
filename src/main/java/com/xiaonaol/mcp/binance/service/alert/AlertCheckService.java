package com.xiaonaol.mcp.binance.service.alert;

import com.xiaonaol.mcp.binance.bean.AlertStore;
import com.xiaonaol.mcp.binance.enums.AlertDirection;
import com.xiaonaol.mcp.binance.model.PriceAlert;
import com.xiaonaol.mcp.binance.service.qq.QQNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertCheckService {

    private final AlertStore alertStore;
    private final QQNotifyService qqNotifyService;

    @Value("${alert.cooldown-seconds:300}")
    private int cooldownSeconds;

    private volatile BigDecimal lastPrice;

    public BigDecimal getLastPrice() {
        return lastPrice;
    }

    public void check(BigDecimal currentPrice) {
        this.lastPrice = currentPrice;
        alertStore.getAll().forEach((userId, alerts) -> {
            for (PriceAlert alert : alerts) {
                if (shouldSkip(alert)) continue;
                if (isHit(alert, currentPrice)) {
                    triggerAlert(userId, alert, currentPrice);
                }
            }
        });
    }

    private boolean isHit(PriceAlert alert, BigDecimal price) {
        return switch (alert.getDirection()) {
            case ABOVE -> price.compareTo(alert.getTargetPrice()) >= 0;
            case BELOW -> price.compareTo(alert.getTargetPrice()) <= 0;
        };
    }

    private boolean shouldSkip(PriceAlert alert) {
        if (!alert.isTriggered()) return false;
        return alert.getLastTriggeredAt()
                .plusSeconds(cooldownSeconds)
                .isAfter(LocalDateTime.now());
    }

    private void triggerAlert(long userId, PriceAlert alert, BigDecimal currentPrice) {
        // 标记冷却
        alert.setTriggered(true);
        alert.setLastTriggeredAt(LocalDateTime.now());
        alertStore.update(userId, alert);

        String dirText = alert.getDirection() == AlertDirection.ABOVE ? "📈 突破" : "📉 跌破";
        String msg = String.format(
                "[CQ:at,qq=%d] 🚨 ETH价格预警触发！\n" +
                        "当前价格：$%s\n" +
                        "%s 目标价：$%s\n" +
                        "备注：%s",
                userId,
                currentPrice.toPlainString(),
                dirText,
                alert.getTargetPrice().toPlainString(),
                alert.getNote() != null ? alert.getNote() : "无"
        );

        // 在设置预警的群里发消息
        qqNotifyService.sendGroupMsg(alert.getGroupId(), msg);
    }
}
