package com.xiaonaol.mcp.binance.bean;

import com.xiaonaol.mcp.binance.model.PriceAlert;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class AlertStore {
    // key: QQ号, value: 该用户的预警列表
    private final Map<Long, List<PriceAlert>> userAlerts = new ConcurrentHashMap<>();

    public void add(Long userId, PriceAlert alert) {
        userAlerts.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(alert);
    }

    public void remove(Long userId, String alertId) {
        List<PriceAlert> alerts = userAlerts.get(userId);
        if (alerts != null) {
            alerts.removeIf(a -> a.getId().equals(alertId));
        }
    }

    public List<PriceAlert> getByUser(Long userId) {
        return userAlerts.getOrDefault(userId, Collections.emptyList());
    }

    // 检查时需要遍历所有用户的所有预警
    public Map<Long, List<PriceAlert>> getAll() {
        return userAlerts;
    }

    public void update(Long userId, PriceAlert alert) {
        // 找到对应用户的对应预警，替换
        List<PriceAlert> alerts = userAlerts.get(userId);
        if (alerts != null) {
            alerts.replaceAll(a -> a.getId().equals(alert.getId()) ? alert : a);
        }
    }
}
