package com.marmik.brokerhub.broker.adapter;

import com.marmik.brokerhub.broker.dto.HoldingItem;
import com.marmik.brokerhub.broker.dto.PositionItem;
import com.marmik.brokerhub.broker.dto.dhan.DhanHolding;
import com.marmik.brokerhub.broker.dto.dhan.DhanPosition;

/**
 * Adapter that converts Dhan DTOs into unified backend DTOs (HoldingItem /
 * PositionItem).
 */
public class DhanAdapter {

    /**
     * Map DhanHolding -> HoldingItem (existing behaviour).
     */
    public static HoldingItem fromDhan(DhanHolding holding) {
        if (holding == null)
            return null;

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

    /**
     * Map DhanPosition -> PositionItem (minimal stock-only model).
     *
     * Note:
     * - averagePrice prefers buyAvg; fallback to costPrice.
     * - DhanPosition DTO currently does not expose lastPrice. We fallback to
     * costPrice or averagePrice.
     */
    public static PositionItem fromDhanPosition(DhanPosition p) {
        if (p == null)
            return null;

        double avgPrice = (p.buyAvg > 0) ? p.buyAvg : p.costPrice;
        // DhanPosition doesn't currently provide lastPrice in the DTO. Use sensible
        // fallback.
        double lastPrice = p.costPrice > 0 ? p.costPrice : avgPrice;

        double unrealized = p.unrealizedProfit;
        double realized = p.realizedProfit;
        double totalPnl = (unrealized) + (realized);

        return PositionItem.builder()
                .exchange(p.exchangeSegment)
                .tradingSymbol(p.tradingSymbol)
                .securityId(p.securityId)
                .quantity(p.netQty)
                .averagePrice(avgPrice)
                .lastPrice(lastPrice)
                .unrealizedProfit(unrealized)
                .realizedProfit(realized)
                .totalPnl(totalPnl)
                .positionType(p.positionType)
                .productType(p.productType)
                .build();
    }
}
