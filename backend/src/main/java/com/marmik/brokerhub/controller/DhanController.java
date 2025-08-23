package com.marmik.brokerhub.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marmik.brokerhub.dto.HoldingItem;
import com.marmik.brokerhub.service.DhanService;

@RestController
@RequestMapping("/api/dhan")
public class DhanController {

    @Autowired
    private DhanService dhanService;

    @GetMapping("/holdings")
    public ResponseEntity<List<HoldingItem>> getHoldings() {
        List<HoldingItem> holdings = dhanService.getHoldings();
        return ResponseEntity.ok(holdings);
    }
}
