package com.hackathon.bankingapp.Controllers;

import com.hackathon.bankingapp.Services.AccountService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/market")
public class MarketController {

    @Autowired
    private AccountService accountService;
    
    @GetMapping("/prices")
    public ResponseEntity<Map<String, BigDecimal>> getMarketPrices() {
        Map<String, BigDecimal> marketPrices = accountService.fetchAllMarketPrices();
        return ResponseEntity.ok(marketPrices);
    }

    @GetMapping("/prices/{assetSymbol}")
    public ResponseEntity<BigDecimal> getMarketPrice(@PathVariable String assetSymbol) {
        BigDecimal assetPrice = accountService.fetchMarketPrice(assetSymbol);
        if (assetPrice != null) {
            return ResponseEntity.ok(assetPrice);
        } else {
            return ResponseEntity.notFound().build();
        }

    }
}
