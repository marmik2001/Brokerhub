package com.marmik.brokerhub.adapter;

import com.marmik.brokerhub.dto.HoldingItem;
import com.marmik.brokerhub.dto.dhan.DhanHolding;

public class DhanAdapter {
    public static HoldingItem fromDhan(DhanHolding holding) {
        return HoldingItem.builder()
                .exchange(holding.exchange)
                .tradingSymbol(holding.tradingSymbol)
                .isin(holding.isin)
                .quantity(holding.totalQty)
                .t1Quantity(holding.t1Qty)
                .collateralQuantity(String.valueOf(holding.collateralQty))
                .averagePrice(holding.avgCostPrice)
                .lastPrice(holding.lastPrice)
                .build();
    }
}
