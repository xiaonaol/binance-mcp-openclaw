package com.xiaonaol.mcp.binance.mcp;

import com.xiaonaol.mcp.binance.bean.AlertStore;
import com.xiaonaol.mcp.binance.enums.AlertDirection;
import com.xiaonaol.mcp.binance.model.PriceAlert;
import com.xiaonaol.mcp.binance.service.alert.AlertCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BinanceAlertMcpTools {

    private final AlertStore alertStore;
    private final AlertCheckService alertCheckService;

    @Tool(description = "获取 ETH/USDT 当前实时价格（来自 Binance WebSocket）")
    public String getCurrentEthPrice() {
        BigDecimal price = alertCheckService.getLastPrice();
        if (price == null) {
            return "暂无价格数据，WebSocket 可能尚未建立连接";
        }
        return "ETH/USDT 当前价格：$" + price.toPlainString();
    }

    @Tool(description = "为指定 QQ 用户设置 ETH 价格预警，价格突破或跌破目标值时会在群内 @ 通知该用户")
    public String addPriceAlert(
            @ToolParam(description = "QQ 用户 ID") long qqUserId,
            @ToolParam(description = "QQ 群 ID，触发预警时在此群发送通知") long groupId,
            @ToolParam(description = "预警方向：ABOVE 表示突破目标价触发，BELOW 表示跌破目标价触发") String direction,
            @ToolParam(description = "目标价格，例如 2000.50") String targetPrice,
            @ToolParam(description = "备注说明，可选，例如'止损位'") String note) {

        AlertDirection dir;
        try {
            dir = AlertDirection.valueOf(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "方向参数错误，请使用 ABOVE 或 BELOW";
        }

        BigDecimal price;
        try {
            price = new BigDecimal(targetPrice);
        } catch (NumberFormatException e) {
            return "价格格式不正确，请输入纯数字，例如 2000.50";
        }

        PriceAlert alert = PriceAlert.builder()
                .id(UUID.randomUUID().toString())
                .groupId(groupId)
                .targetPrice(price)
                .direction(dir)
                .note(note)
                .triggered(false)
                .build();

        alertStore.add(qqUserId, alert);

        String dirText = dir == AlertDirection.ABOVE ? "突破" : "跌破";
        return String.format("预警已设置成功✅\n方向：%s $%s\n备注：%s\nID：%s",
                dirText, price.toPlainString(),
                note != null ? note : "无",
                alert.getId().substring(0, 8));
    }

    @Tool(description = "查询指定 QQ 用户的所有 ETH 价格预警列表")
    public String listPriceAlerts(
            @ToolParam(description = "QQ 用户 ID") long qqUserId) {

        List<PriceAlert> alerts = alertStore.getByUser(qqUserId);
        if (alerts.isEmpty()) {
            return "该用户没有设置任何预警";
        }

        StringBuilder sb = new StringBuilder("ETH 预警列表：\n");
        for (PriceAlert a : alerts) {
            String dirText = a.getDirection() == AlertDirection.ABOVE ? "突破" : "跌破";
            String status = a.isTriggered() ? "冷却中" : "监控中";
            sb.append(String.format("• [%s] %s $%s  备注:%s  状态:%s\n",
                    a.getId().substring(0, 8),
                    dirText,
                    a.getTargetPrice().toPlainString(),
                    a.getNote() != null ? a.getNote() : "无",
                    status));
        }
        return sb.toString().trim();
    }

    @Tool(description = "删除指定 QQ 用户的某条 ETH 价格预警")
    public String deletePriceAlert(
            @ToolParam(description = "QQ 用户 ID") long qqUserId,
            @ToolParam(description = "预警 ID 前 8 位（从列表中获取）") String alertIdPrefix) {

        List<PriceAlert> alerts = alertStore.getByUser(qqUserId);
        Optional<PriceAlert> target = alerts.stream()
                .filter(a -> a.getId().startsWith(alertIdPrefix))
                .findFirst();

        if (target.isEmpty()) {
            return "未找到该预警，请检查 ID 是否正确";
        }
        alertStore.remove(qqUserId, target.get().getId());
        return "预警已删除✅ ID：" + alertIdPrefix;
    }
}
