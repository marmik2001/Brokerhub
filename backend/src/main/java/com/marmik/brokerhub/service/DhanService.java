package com.marmik.brokerhub.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.marmik.brokerhub.dto.HoldingItem;
import com.marmik.brokerhub.adapter.DhanAdapter;
import com.marmik.brokerhub.dto.dhan.DhanHolding;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DhanService {

    @Value("${dhan.access-token}")
    private String accessToken;

    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                return dhanHoldings.stream()
                        .map(DhanAdapter::fromDhan)
                        .toList();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
