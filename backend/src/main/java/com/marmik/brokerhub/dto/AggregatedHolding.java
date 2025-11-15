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

    private long quantity; // aggregated sum
    private double averagePrice; // weighted average price
    private double pnl; // aggregated PnL
    private double lastPrice; // most recent non-zero last price (if available)
    private double dayChange; // day change matching lastPrice
    private double dayChangePercentage;
}
