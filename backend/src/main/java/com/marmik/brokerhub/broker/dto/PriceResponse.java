package com.marmik.brokerhub.broker.dto;

import lombok.Data;

@Data
public class PriceResponse {
    private String symbol;
    private double lastPrice;
    private double dayChange;
    private double dayChangePercentage;
}
