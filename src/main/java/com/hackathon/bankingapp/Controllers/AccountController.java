package com.hackathon.bankingapp.Controllers;

import com.hackathon.bankingapp.Services.AccountService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import com.hackathon.bankingapp.Entities.Transaction;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;


    @PostMapping("/create-pin")
    public ResponseEntity<String> createPin(@RequestBody Map<String, String> request, @AuthenticationPrincipal UserDetails userDetails) {
        String pin = request.get("pin");
        String password = request.get("password");

        String email = userDetails.getUsername();

        String responseMessage = accountService.createPin(email, pin, password);

        return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/update-pin")
    public ResponseEntity<String> updatePin(@RequestBody Map<String, String> request, @AuthenticationPrincipal UserDetails userDetails) {
    String oldPin = request.get("oldPin");
    String newPin = request.get("newPin");
    String password = request.get("password");

    String email = userDetails.getUsername();

    String responseMessage = accountService.updatePin(email, oldPin, newPin, password);

    return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestBody Map<String, String> request, @AuthenticationPrincipal UserDetails userDetails) {
        String pin = request.get("pin");
        String amountStr = request.get("amount");
        BigDecimal amount = new BigDecimal(amountStr);

        String email = userDetails.getUsername();

        String responseMessage = accountService.deposit(email, pin, amount);
        if (responseMessage.equals("Cash deposited successfully")) {
            return ResponseEntity.ok(responseMessage);
        } else if (responseMessage.equals("Incorrect PIN")) {
            return ResponseEntity.status(403).body("Invalid PIN");
        }
        return ResponseEntity.status(500).body("An error occurred");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> withdraw(@RequestBody Map<String, String> request, @AuthenticationPrincipal UserDetails userDetails) {
        String pin = request.get("pin");
        String amountStr = request.get("amount");
        BigDecimal amount = new BigDecimal(amountStr);
        String email = userDetails.getUsername();

        String responseMessage = accountService.withdraw(email, pin, amount);
        if (responseMessage.equals("Cash withdrawn successfully")) {
            return ResponseEntity.ok(responseMessage);
        } else if (responseMessage.equals("Incorrect PIN")) {
            return ResponseEntity.status(403).body("Invalid PIN");
        } else if (responseMessage.equals("Insufficient balance")) {
            return ResponseEntity.status(403).body("Insufficient balance");
        }
        return ResponseEntity.status(500).body("An error occurred");
    }

    @PostMapping("/fund-transfer")
    public ResponseEntity<String> transfer(@RequestBody Map<String, String> request, @AuthenticationPrincipal UserDetails userDetails) {
        String pin = request.get("pin");
        String amountStr = request.get("amount");
        BigDecimal amount = new BigDecimal(amountStr);
        String targetAccountNumber = request.get("targetAccountNumber");
        String sourceAccountEmail = userDetails.getUsername();

        String responseMessage = accountService.transferFunds(sourceAccountEmail, pin, amount, targetAccountNumber);
        if (responseMessage.equals("Fund transferred successfully")) {
            return ResponseEntity.ok(responseMessage);
        } else if (responseMessage.equals("Incorrect PIN")) {
            return ResponseEntity.status(403).body("Invalid PIN");
        } else if (responseMessage.equals("Insufficient balance")) {
            return ResponseEntity.status(403).body("Insufficient balance");
        }
        return ResponseEntity.status(500).body("An error occurred");
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactionHistory(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        List<Transaction> transactionHistory = accountService.getTransactionHistory(email);
        return ResponseEntity.ok(transactionHistory);
    }

    @PostMapping("/buy-asset")
    public ResponseEntity<String> buyAsset(@RequestBody Map<String, String> request, @AuthenticationPrincipal UserDetails userDetails) {
        String pin = request.get("pin");
        String assetSymbol = request.get("assetSymbol");
        BigDecimal amount = new BigDecimal(request.get("amount"));
        String email = userDetails.getUsername();
        
        String responseMessage = accountService.buyAsset(email, assetSymbol, amount, pin);
    
        if ("Asset purchase successful.".equals(responseMessage)) {
            return ResponseEntity.ok(responseMessage);
        } else if ("Incorrect PIN".equals(responseMessage)) {
            return ResponseEntity.status(403).body("Invalid PIN");
        }
        return ResponseEntity.status(500).body("An error occurred");
    }
    

    @PostMapping("/sell-asset")
    public ResponseEntity<String> sellAsset(@RequestBody Map<String, String> request, @AuthenticationPrincipal UserDetails userDetails) {
        String pin = request.get("pin");
        String assetSymbol = request.get("assetSymbol");
        BigDecimal quantity = new BigDecimal(request.get("quantity"));
        String email = userDetails.getUsername();

        String responseMessage = accountService.sellAsset(email, assetSymbol, quantity, pin);
        if (responseMessage.equals("Asset sale successful.")) {
            return ResponseEntity.ok(responseMessage);
        } else if (responseMessage.equals("Incorrect PIN")) {
            return ResponseEntity.status(403).body("Invalid PIN");
        }
        return ResponseEntity.status(500).body("An error occurred");
    }

    @GetMapping("/networth")
    public ResponseEntity<BigDecimal> getNetWorth(@AuthenticationPrincipal UserDetails userDetails) {
    String email = userDetails.getUsername();
    try {
        BigDecimal netWorth = accountService.calculateNetWorth(email);
        return ResponseEntity.ok(netWorth);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(null);
    } catch (Exception e) {
        return ResponseEntity.internalServerError().body(null);
    }
    }

}
