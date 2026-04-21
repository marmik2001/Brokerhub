package com.marmik.brokerhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal DTO returned by aggregate holdings endpoint.
 * Contains non-sensitive, displayable fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregatedHolding {
    private String exchange;
    private String tradingSymbol;
    private String isin;

    /** Aggregated sum of quantity across brokers */
    private long quantity;
    
    /** Weighted average price */
    private double averagePrice;
    
    /** Total aggregated PnL */
    private double pnl;
    
    /** Most recent non-zero last price (if available) */
    private double lastPrice;
    
    /** Day change matching the last price */
    private double dayChange;
    
    private double dayChangePercentage;
}
