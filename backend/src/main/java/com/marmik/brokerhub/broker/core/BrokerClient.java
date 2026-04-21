package com.marmik.brokerhub.broker.core;

import com.marmik.brokerhub.broker.dto.HoldingItem;
import com.marmik.brokerhub.broker.dto.PositionItem;
import java.util.List;

/**
 * Interface for interacting with various broker APIs.
 */
public interface BrokerClient {

    /**
     * Get the broker type identifier (e.g., KITE, DHAN).
     */
    String getBrokerType();

    /**
     * Fetch holdings for the given broker access token.
     */
    List<HoldingItem> getHoldings(String token);

    /**
     * Fetch positions for the given broker access token.
     */
    List<PositionItem> getPositions(String token);
}
