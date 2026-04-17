package com.marmik.brokerhub.broker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marmik.brokerhub.broker.model.PriceResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Service
public class MarketDataService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${marketdata.base-url}")
    private String baseUrl;

    public MarketDataService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public List<PriceResponse> getPrices(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        String joinedSymbols = String.join(",", symbols);

        try {
            JsonNode root = restClient.get()
                    .uri(baseUrl + "/prices?symbols={symbols}", joinedSymbols)
                    .retrieve()
                    .body(JsonNode.class);

            if (root != null && root.has("results")) {
                return objectMapper.readerForListOf(PriceResponse.class).readValue(root.get("results"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
}
