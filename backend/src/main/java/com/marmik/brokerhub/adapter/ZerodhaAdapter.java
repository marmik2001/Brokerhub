package com.marmik.brokerhub.adapter;

import com.marmik.brokerhub.dto.HoldingItem;
import com.zerodhatech.models.Holding;

public class ZerodhaAdapter {
    public static HoldingItem fromZerodha(Holding holding) {
        return HoldingItem.builder()
                .exchange(holding.exchange)
                .tradingSymbol(holding.tradingSymbol)
                .isin(holding.isin)
                .quantity(holding.quantity)
                .t1Quantity(holding.t1Quantity)
                .collateralQuantity(holding.collateralQuantity)
                .averagePrice(holding.averagePrice)
                .lastPrice(holding.lastPrice)
                .pnl(holding.pnl)
                .dayChange(holding.dayChange)
                .dayChangePercentage(holding.dayChangePercentage)
                .build();
    }
}
