package com.hackathon.bankingapp.Controllers;

import com.hackathon.bankingapp.Services.EmailService;
import com.hackathon.bankingapp.Services.OtpService;
import com.hackathon.bankingapp.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpService OtpService;


    @PostMapping("password-reset/send-otp")
    public ResponseEntity<String> sendOtp(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String otp = generateOtp();
        
        OtpService.storeOtp(identifier, otp);
        
        emailService.sendEmail(identifier, "OTP: " + otp);
        
        return ResponseEntity.ok("OTP sent successfully to: " + identifier);
    }

    private String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }

    @PostMapping("/password-reset/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String otp = request.get("otp");
        return userService.verifyOtp(identifier, otp);
    }

    @PostMapping("/password-reset")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String resetToken = request.get("resetToken");
        String newPassword = request.get("newPassword");
        return userService.resetPassword(identifier, resetToken, newPassword);
    }
    
}

