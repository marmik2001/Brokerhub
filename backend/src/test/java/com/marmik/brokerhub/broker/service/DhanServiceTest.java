package com.marmik.brokerhub.broker.service;

import com.marmik.brokerhub.broker.dto.HoldingItem;
import com.marmik.brokerhub.broker.dto.PositionItem;
import com.marmik.brokerhub.broker.model.PriceResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for DhanService.
 *
 * Covers:
 * - Holdings enrichment from market prices.
 * - Fallback behavior when price data is missing.
 * - Empty-result behavior for upstream HTTP failures.
 * - Position last-price enrichment behavior.
 *
 * Ensures that broker data adaptation and price-enrichment constraints are not
 * broken.
 */
class DhanServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldEnrichHoldingsWhenMarketPriceAvailable() throws Exception {
        startServer("/holdings", 200,
                "[{\"exchange\":\"NSE\",\"tradingSymbol\":\"INFY\",\"isin\":\"I\",\"totalQty\":10,\"t1Qty\":0,\"collateralQty\":0,\"avgCostPrice\":100.0,\"lastPrice\":0.0}]");
        MarketDataService marketData = mock(MarketDataService.class);
        PriceResponse p = new PriceResponse();
        p.setSymbol("INFY");
        p.setLastPrice(130.0);
        p.setDayChange(5.0);
        p.setDayChangePercentage(4.0);
        when(marketData.getPrices(List.of("INFY"))).thenReturn(List.of(p));

        DhanService service = new DhanService(marketData);
        setBaseUrl(service, "http://localhost:" + server.getAddress().getPort());

        List<HoldingItem> out = service.getHoldings("token");

        assertEquals(1, out.size());
        assertEquals(130.0, out.get(0).getLastPrice());
        assertEquals(300.0, out.get(0).getPnl());
    }

    @Test
    void shouldApplyFallbackWhenMarketPriceMissingOrZero() throws Exception {
        startServer("/holdings", 200,
                "[{\"exchange\":\"NSE\",\"tradingSymbol\":\"INFY\",\"isin\":\"I\",\"totalQty\":10,\"t1Qty\":0,\"collateralQty\":0,\"avgCostPrice\":100.0,\"lastPrice\":0.0}]");
        MarketDataService marketData = mock(MarketDataService.class);
        when(marketData.getPrices(List.of("INFY"))).thenReturn(List.of());

        DhanService service = new DhanService(marketData);
        setBaseUrl(service, "http://localhost:" + server.getAddress().getPort());

        List<HoldingItem> out = service.getHoldings("token");

        assertEquals(100.0, out.get(0).getLastPrice());
        assertEquals(0.0, out.get(0).getPnl());
    }

    @Test
    void shouldReturnEmptyWhenHoldingsApiFails() throws Exception {
        startServer("/holdings", 500, "{}");
        MarketDataService marketData = mock(MarketDataService.class);
        DhanService service = new DhanService(marketData);
        setBaseUrl(service, "http://localhost:" + server.getAddress().getPort());

        assertTrue(service.getHoldings("token").isEmpty());
    }

    @Test
    void shouldEnrichPositionsLastPriceWhenAvailable() throws Exception {
        startServer("/positions", 200,
                "[{\"tradingSymbol\":\"INFY\",\"securityId\":\"1\",\"positionType\":\"LONG\",\"exchangeSegment\":\"NSE\",\"productType\":\"CNC\",\"buyAvg\":100.0,\"costPrice\":100.0,\"netQty\":5,\"realizedProfit\":0.0,\"unrealizedProfit\":0.0}]");
        MarketDataService marketData = mock(MarketDataService.class);
        PriceResponse p = new PriceResponse();
        p.setSymbol("INFY");
        p.setLastPrice(150.0);
        when(marketData.getPrices(List.of("INFY"))).thenReturn(List.of(p));

        DhanService service = new DhanService(marketData);
        setBaseUrl(service, "http://localhost:" + server.getAddress().getPort());

        List<PositionItem> out = service.getPositions("token");

        assertEquals(1, out.size());
        assertEquals(150.0, out.get(0).getLastPrice());
    }

    private void startServer(String path, int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(path, exchange -> {
            byte[] bytes = body.getBytes();
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }

    private void setBaseUrl(DhanService service, String baseUrl) throws Exception {
        Field f = DhanService.class.getDeclaredField("baseUrl");
        f.setAccessible(true);
        f.set(service, baseUrl);
    }
}
