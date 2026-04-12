package com.marmik.brokerhub.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marmik.brokerhub.broker.dto.HoldingItem;

@Service
public class BrokerHoldingsCacheService {

    private static final Logger log = LoggerFactory.getLogger(BrokerHoldingsCacheService.class);
    private static final TypeReference<List<HoldingItem>> HOLDINGS_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${broker.holdings-cache.ttl-seconds:43200}")
    private long holdingsCacheTtlSeconds;

    public BrokerHoldingsCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<List<HoldingItem>> getCachedHoldings(String broker, UUID credentialId) {
        String key = cacheKey(broker, credentialId);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, HOLDINGS_LIST_TYPE));
        } catch (Exception ex) {
            log.debug("Failed reading holdings cache for key {}", key, ex);
            return Optional.empty();
        }
    }

    public void cacheHoldings(String broker, UUID credentialId, List<HoldingItem> holdings) {
        String key = cacheKey(broker, credentialId);
        try {
            String json = objectMapper.writeValueAsString(holdings);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(holdingsCacheTtlSeconds));
        } catch (Exception ex) {
            log.debug("Failed writing holdings cache for key {}", key, ex);
        }
    }

    private String cacheKey(String broker, UUID credentialId) {
        String safeBroker = (broker == null || broker.isBlank()) ? "unknown" : broker.toUpperCase();
        String safeCredId = credentialId == null ? "unknown" : credentialId.toString();
        return "bh:holdings:" + safeBroker + ":" + safeCredId;
    }
}
