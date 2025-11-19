package com.marmik.brokerhub.broker.dto.dhan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DhanPosition {

    public String dhanClientId;
    public String tradingSymbol;
    public String securityId;
    public String positionType;
    public String exchangeSegment;
    public String productType;

    public double buyAvg;
    public double costPrice;
    public int buyQty;

    public double sellAvg;
    public int sellQty;

    public int netQty;

    public double realizedProfit;
    public double unrealizedProfit;

    public double rbiReferenceRate;
    public int multiplier;

    public int carryForwardBuyQty;
    public int carryForwardSellQty;
    public double carryForwardBuyValue;
    public double carryForwardSellValue;

    public int dayBuyQty;
    public int daySellQty;
    public double dayBuyValue;
    public double daySellValue;

    public String drvExpiryDate;
    public String drvOptionType;
    public double drvStrikePrice;

    public boolean crossCurrency;
}