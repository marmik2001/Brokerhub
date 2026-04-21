package com.marmik.brokerhub.broker.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.marmik.brokerhub.broker.adapter.DhanAdapter;
import com.marmik.brokerhub.broker.core.BrokerClient;
import lombok.extern.slf4j.Slf4j;
import com.marmik.brokerhub.broker.dto.HoldingItem;
import com.marmik.brokerhub.broker.dto.PositionItem;
import com.marmik.brokerhub.broker.dto.dhan.DhanHolding;
import com.marmik.brokerhub.broker.dto.dhan.DhanPosition;
import com.marmik.brokerhub.broker.model.PriceResponse;

/**
 * BrokerClient implementation for Dhan broker.
 */
@Service
@Slf4j
public class DhanService implements BrokerClient {

    @Value("${dhan.api.base-url}")
    private String baseUrl;

    private final RestClient restClient;
    private final MarketDataService marketDataService;

    public DhanService(RestClient.Builder restClientBuilder, MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String getBrokerType() {
        return "DHAN";
    }

    @Override
    public List<HoldingItem> getHoldings(String accessToken) {
        try {
            String url = baseUrl.endsWith("/") ? baseUrl + "holdings" : baseUrl + "/holdings";
            
            List<DhanHolding> dhanHoldings = restClient.get()
                    .uri(url)
                    .header("access-token", accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DhanHolding>>() {});

            if (dhanHoldings == null) return Collections.emptyList();

            List<HoldingItem> holdings = dhanHoldings.stream()
                    .map(DhanAdapter::fromDhan)
                    .toList();

            List<String> symbols = holdings.stream()
                    .map(HoldingItem::getTradingSymbol)
                    .toList();

            List<PriceResponse> prices = marketDataService.getPrices(symbols);

            Map<String, PriceResponse> priceMap = prices.stream()
                    .collect(Collectors.toMap(PriceResponse::getSymbol, p -> p));

            holdings.forEach(holding -> {
                PriceResponse price = priceMap.get(holding.getTradingSymbol());

                if (price != null && price.getLastPrice() != 0) {
                    holding.setLastPrice(price.getLastPrice());
                    holding.setDayChange(price.getDayChange());
                    holding.setDayChangePercentage(price.getDayChangePercentage());

                    double pnl = (price.getLastPrice() - holding.getAveragePrice()) * holding.getQuantity();
                    holding.setPnl(pnl);
                } else {
                    double avg = holding.getAveragePrice();
                    holding.setLastPrice(avg);
                    holding.setDayChange(0);
                    holding.setDayChangePercentage(0);
                    holding.setPnl(0);
                }
            });

            return holdings;
        } catch (Exception e) {
            log.error("Failed to fetch holdings from Dhan", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<PositionItem> getPositions(String accessToken) {
        try {
            String url = baseUrl.endsWith("/") ? baseUrl + "positions" : baseUrl + "/positions";
            
            List<DhanPosition> dhanPositions = restClient.get()
                    .uri(url)
                    .header("access-token", accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<DhanPosition>>() {});

            if (dhanPositions == null) return Collections.emptyList();

            List<PositionItem> positions = dhanPositions.stream()
                    .map(DhanAdapter::fromDhanPosition)
                    .toList();

            List<String> symbols = positions.stream()
                    .map(PositionItem::getTradingSymbol)
                    .toList();

            List<PriceResponse> prices = marketDataService.getPrices(symbols);
            Map<String, PriceResponse> priceMap = prices.stream()
                    .collect(Collectors.toMap(PriceResponse::getSymbol, p -> p));

            positions.forEach(position -> {
                PriceResponse price = priceMap.get(position.getTradingSymbol());
                if (price != null && price.getLastPrice() != 0) {
                    position.setLastPrice(price.getLastPrice());
                }
            });

            return positions;
        } catch (Exception e) {
            log.error("Failed to fetch positions from Dhan", e);
            return Collections.emptyList();
        }
    }
}
