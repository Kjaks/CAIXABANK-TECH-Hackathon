package com.hackathon.bankingapp.Controllers;

import com.hackathon.bankingapp.Entities.User;
import com.hackathon.bankingapp.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/dashboard")
public class DashBoardController {

    @Autowired
    private UserService userService;

    @GetMapping("/user")
    public ResponseEntity<User> getUserInfo(@AuthenticationPrincipal UserDetails userDetails) {
        System.out.println("Llamando a getUserInfo para el usuario: " + userDetails.getUsername());
        User user = userService.getUserByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping("/account")
    public ResponseEntity<Map<String, Object>> getAccountInfo(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByEmail(userDetails.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> accountInfo = new HashMap<>();
        accountInfo.put("accountNumber", user.getAccount().getAccountNumber().toString());
        accountInfo.put("balance", user.getAccount().getBalance()); 

        return ResponseEntity.ok(accountInfo);
    }
    
}


