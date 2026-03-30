package com.xiaonaol.mcp.binance.model;

import com.xiaonaol.mcp.binance.enums.AlertDirection;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PriceAlertRequest {
    private String id;
    private Long groupId;           // 在哪个群设置的，触发时就在哪个群通知
    private BigDecimal targetPrice;
    private AlertDirection direction;
    private boolean triggered;
    private String note;
}
