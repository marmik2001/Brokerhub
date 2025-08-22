package com.marmik.brokerhub.service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Holding;
import com.zerodhatech.models.User;

import jakarta.annotation.PostConstruct;

@Service
public class KiteService {

    @Value("${kite.api.key}")
    private String apiKey;

    @Value("${kite.api.secret}")
    private String apiSecret;

    @Value("${kite.api.userId}")
    private String userId;

    private KiteConnect kiteConnect;

    @PostConstruct
    public void init() {
        kiteConnect = new KiteConnect(apiKey);
        kiteConnect.setUserId(userId);
    }

    public String getLoginUrl() {
        return kiteConnect.getLoginURL();
    }

    public String generateAccessToken(String requestToken) throws KiteException, IOException {
        if (isLoggedIn()) {
            return kiteConnect.getAccessToken();
        } else {
            User user = kiteConnect.generateSession(requestToken, apiSecret);
            kiteConnect.setAccessToken(user.accessToken);
            kiteConnect.setPublicToken(user.publicToken);
            return user.accessToken;
        }
    }

    private boolean isLoggedIn() {
        try {
            kiteConnect.getProfile();
            return true;
        } catch (KiteException | IOException e) {
            return false;
        }
    }

    public List<Holding> getHoldings() {
        try {
            return kiteConnect.getHoldings();
        } catch (KiteException | IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

}
