package com.marmik.brokerhub.broker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marmik.brokerhub.broker.model.PriceResponse;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class MarketDataService {

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${marketdata.base-url}")
    private String baseUrl;

    public List<PriceResponse> getPrices(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Collections.emptyList();
        }

        // Build URL with comma-separated symbols
        String joinedSymbols = String.join(",", symbols);
        HttpUrl url = HttpUrl.parse(baseUrl + "/prices")
                .newBuilder()
                .addQueryParameter("symbols", joinedSymbols)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return Collections.emptyList();
            }

            String json = response.body().string();

            JsonNode root = objectMapper.readTree(json).get("results");
            return objectMapper.readerForListOf(PriceResponse.class).readValue(root);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
