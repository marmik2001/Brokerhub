package com.marmik.brokerhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal DTO returned by aggregate positions endpoint.
 * Contains non-sensitive, displayable fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregatedPosition {
    private String exchange;
    private String tradingSymbol;

    /** Total aggregated quantity (sum of netQtys) */
    private long quantity;

    /** Weighted average price across position items */
    private double averagePrice;

    /** Sum of PnLs (realized + unrealized) across positions */
    private double pnl;

    /** Last seen market price (preferred non-zero) */
    private double lastPrice;

    /** Optional day-change metadata (from payloads if present) */
    private double dayChange;
    private double dayChangePercentage;
}
