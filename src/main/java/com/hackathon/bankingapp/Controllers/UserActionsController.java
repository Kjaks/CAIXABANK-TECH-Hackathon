package com.hackathon.bankingapp.Controllers;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.hackathon.bankingapp.Services.UserActionsService;

@RestController
@RequestMapping("/api/user-actions")
public class UserActionsController {

    @Autowired
    UserActionsService userActionsService;

    @PostMapping("/subscribe")
    public ResponseEntity<String> createSubscription(@RequestBody Map<String, String> request, @AuthenticationPrincipal UserDetails userDetails) {
    String pin = request.get("pin");
    BigDecimal amount = new BigDecimal(request.get("amount"));
    int intervalSeconds = Integer.parseInt(request.get("intervalSeconds"));
    String email = userDetails.getUsername();

    String responseMessage = userActionsService.createSubscription(email, pin, amount, intervalSeconds);
    return ResponseEntity.ok(responseMessage);
    }

    @PostMapping("/enable-auto-invest")
    public ResponseEntity<String> checkMarketAndTrade(@RequestBody Map<String, String> request,@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        String pin = request.get("pin");

        // Llama al método checkMarketAndTrade en el servicio
        userActionsService.enableAutoInvest(email ,pin);
        
        return ResponseEntity.ok("Market checked and trades executed if applicable.");
    }

}
