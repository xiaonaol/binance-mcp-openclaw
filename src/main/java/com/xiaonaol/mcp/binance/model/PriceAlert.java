package com.xiaonaol.mcp.binance.model;

import com.xiaonaol.mcp.binance.enums.AlertDirection;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PriceAlert {
    private String id;
    private Long groupId;           // 在哪个群设置的，触发时就在哪个群通知
    private BigDecimal targetPrice;
    private AlertDirection direction;
    private boolean triggered;
    private LocalDateTime lastTriggeredAt;
    private String note;
}
