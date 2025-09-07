package com.marmik.brokerhub.broker.dto.dhan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DhanHolding {
    public String exchange;
    public String tradingSymbol;
    public String isin;
    public int totalQty;
    public int t1Qty;
    public int collateralQty;
    public double avgCostPrice;
    public double lastPrice;
}
