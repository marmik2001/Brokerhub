package com.marmik.brokerhub.broker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldingItem {
    private String exchange;
    private String tradingSymbol;
    private String isin;

    private int quantity; // Zerodha: quantity, Dhan: totalQty
    private int t1Quantity; // Zerodha: t1Quantity, Dhan: t1Qty
    private String collateralQuantity; // Zerodha: collateralQuantity(String), Dhan: collateralQty (int)
    private double averagePrice; // Zerodha: averagePrice, Dhan: avgCostPrice

    // Fields -> Only in Zerodha, added to dhan using market-data-service
    private double lastPrice;
    private double pnl;
    private double dayChange;
    private double dayChangePercentage;
}
