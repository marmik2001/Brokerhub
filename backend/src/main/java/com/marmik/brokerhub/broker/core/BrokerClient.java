package com.marmik.brokerhub.broker.core;

import com.marmik.brokerhub.broker.dto.HoldingItem;
import com.marmik.brokerhub.broker.dto.PositionItem;
import java.util.List;

public interface BrokerClient {

    String getBrokerType();

    List<HoldingItem> getHoldings(String token);

    // New: fetch positions for an account/token
    List<PositionItem> getPositions(String token);
}
