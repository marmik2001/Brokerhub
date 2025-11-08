package com.marmik.brokerhub.broker.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.marmik.brokerhub.broker.adapter.DhanAdapter;
import com.marmik.brokerhub.broker.core.BrokerClient;
import com.marmik.brokerhub.broker.dto.HoldingItem;
import com.marmik.brokerhub.broker.dto.dhan.DhanHolding;
import com.marmik.brokerhub.broker.model.PriceResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Service
public class DhanService implements BrokerClient {

    @Value("${dhan.api.base-url}")
    private String baseUrl;

    // OkHttpClient is thread-safe: reuse across requests/threads
    private final OkHttpClient client;

    // ObjectMapper is thread-safe after configuration: reuse
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final MarketDataService marketDataService;

    public DhanService(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
        this.client = new OkHttpClient.Builder()
                .callTimeout(Duration.ofSeconds(7))
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(4))
                .build();
    }

    @Override
    public String getBrokerType() {
        return "DHAN";
    }

    @Override
    public List<HoldingItem> getHoldings(String accessToken) {
        try {
            String url = baseUrl.endsWith("/") ? baseUrl + "holdings" : baseUrl + "/holdings";
            Request request = new Request.Builder()
                    .url(url)
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

                /*
                 * // Step 2: Find symbols whose data is required
                 * List<String> symbols = holdings.stream()
                 * .map(HoldingItem::getTradingSymbol)
                 * .toList();
                 * 
                 * // MarketDataService handles concurrent fetching internally
                 * List<PriceResponse> prices = marketDataService.getPrices(symbols);
                 */
                List<PriceResponse> prices = new ArrayList<>();

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
                        if (price.getLastPrice() == 0)
                            holding.setPnl(0);
                        else {
                            double pnl = (price.getLastPrice() - holding.getAveragePrice()) * holding.getQuantity();
                            holding.setPnl(pnl);
                        }
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
