package com.xiaonaol.mcp.binance.controller;

import com.xiaonaol.mcp.binance.bean.AlertStore;
import com.xiaonaol.mcp.binance.model.PriceAlert;
import com.xiaonaol.mcp.binance.model.PriceAlertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertStore alertStore;

    // 查看某个用户的预警
    // GET /api/alerts?userId=123456
    @GetMapping
    public List<PriceAlert> list(@RequestParam Long userId) {
        return alertStore.getByUser(userId);
    }

    // 添加预警（管理用，正常走群指令）
    // POST /api/alerts?userId=123456
    // body: {"targetPrice": 1900, "direction": "BELOW", "groupId": 789, "note": "止损"}
    @PostMapping
    public PriceAlert add(@RequestParam Long userId, @RequestBody PriceAlertRequest req) {
        PriceAlert alert = PriceAlert.builder()
                .id(UUID.randomUUID().toString())
                .targetPrice(req.getTargetPrice())
                .direction(req.getDirection())
                .groupId(req.getGroupId())
                .note(req.getNote())
                .triggered(false)
                .build();
        alertStore.add(userId, alert);
        return alert;
    }

    // 删除预警
    // DELETE /api/alerts/{alertId}?userId=123456
    @DeleteMapping("/{alertId}")
    public void delete(@RequestParam Long userId, @PathVariable String alertId) {
        alertStore.remove(userId, alertId);
    }

    // 重置冷却
    // POST /api/alerts/{alertId}/reset?userId=123456
    @PostMapping("/{alertId}/reset")
    public void reset(@RequestParam Long userId, @PathVariable String alertId) {
        alertStore.getByUser(userId).stream()
                .filter(a -> a.getId().equals(alertId))
                .findFirst()
                .ifPresent(a -> {
                    a.setTriggered(false);
                    a.setLastTriggeredAt(null);
                    alertStore.update(userId, a);
                });
    }

    // 查看所有用户的预警（管理员用）
    // GET /api/alerts/all
    @GetMapping("/all")
    public Map<Long, List<PriceAlert>> listAll() {
        return alertStore.getAll();
    }
}
