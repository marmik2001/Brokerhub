package com.marmik.brokerhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregatedPosition {
    private String exchange;
    private String tradingSymbol;

    // total aggregated quantity (sum of netQtys)
    private long quantity;

    // weighted average price across position items
    private double averagePrice;

    // sum of pnls (realized + unrealized) across positions
    private double pnl;

    // last seen price (preferred non-zero)
    private double lastPrice;

    // optional day-change metadata (from payloads if present)
    private double dayChange;
    private double dayChangePercentage;
}
