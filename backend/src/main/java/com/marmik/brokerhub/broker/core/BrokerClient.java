package com.marmik.brokerhub.broker.core;

import com.marmik.brokerhub.broker.dto.HoldingItem;
import java.util.List;

public interface BrokerClient {

    String getBrokerType();

    List<HoldingItem> getHoldings(String token);

}
