package com.marmik.brokerhub.broker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionItem {

    /* Identity */
    private String exchange; // e.g. NSE_EQ
    private String tradingSymbol; // e.g. WAAREEENER
    private String securityId; // from Dhan payload
    private String isin; // optional (may be null)

    /* Quantity & Price */
    private int quantity; // netQty
    private double averagePrice; // buyAvg or costPrice
    private double lastPrice; // market price (from payload)

    /* P&L */
    private double unrealizedProfit;
    private double realizedProfit;
    private double totalPnl; // unrealized + realized

    /* Metadata */
    private String positionType; // LONG / SHORT
    private String productType; // CNC / MIS
}
