package com.marmik.brokerhub.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.marmik.brokerhub.dto.HoldingItem;
import com.marmik.brokerhub.adapter.DhanAdapter;
import com.marmik.brokerhub.dto.dhan.DhanHolding;
import com.marmik.brokerhub.dto.PriceResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DhanService {

    @Value("${dhan.access-token}")
    private String accessToken;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MarketDataService marketDataService;

    public DhanService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    public List<HoldingItem> getHoldings() {
        try {
            Request request = new Request.Builder()
                    .url("https://api.dhan.co/holdings")
                    .addHeader("access-token", accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null)
                    return Collections.emptyList();

                String json = response.body().string();
                List<DhanHolding> dhanHoldings = objectMapper.readValue(json,
                        new TypeReference<List<DhanHolding>>() {
                        });

                // Step 1: Convert DhanHoldings → HoldingItems
                List<HoldingItem> holdings = dhanHoldings.stream()
                        .map(DhanAdapter::fromDhan)
                        .toList();

                // Step 2: Find symbols whose data is required
                List<String> symbols = holdings.stream()
                        .map(HoldingItem::getTradingSymbol)
                        .toList();

                List<PriceResponse> prices = marketDataService.getPrices(symbols);

                // Step 3: Convert list → map for quick lookup
                Map<String, PriceResponse> priceMap = prices.stream()
                        .collect(Collectors.toMap(PriceResponse::getSymbol, p -> p));

                // Step 4: Enrich holdings
                holdings.forEach(holding -> {
                    PriceResponse price = priceMap.get(holding.getTradingSymbol());
                    if (price != null) {
                        holding.setLastPrice(price.getLastPrice());
                        holding.setDayChange(price.getDayChange());
                        holding.setDayChangePercentage(price.getDayChangePercentage());

                        // Compute PnL
                        double pnl = (price.getLastPrice() - holding.getAveragePrice()) * holding.getQuantity();
                        holding.setPnl(pnl);
                    }
                });

                return holdings;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
