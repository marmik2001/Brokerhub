package com.marmik.brokerhub.broker.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.marmik.brokerhub.broker.dto.HoldingItem;
import com.marmik.brokerhub.broker.service.KiteService;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;

@RestController
@RequestMapping("/api/kite")
public class KiteController {

    @Autowired
    private KiteService kiteService;

    @GetMapping("/login-url")
    public String getLoginUrl() {
        return kiteService.getLoginUrl();
    }

    @PostMapping("/generate-token")
    public ResponseEntity<String> generateToken(@RequestParam String requestToken) throws Exception, KiteException {
        String accessToken = kiteService.generateAccessToken(requestToken);
        return ResponseEntity.ok("Access token generated successfully: " + accessToken);
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<HoldingItem>> getHoldings() {
        return ResponseEntity.ok(kiteService.getHoldings());
    }
}
