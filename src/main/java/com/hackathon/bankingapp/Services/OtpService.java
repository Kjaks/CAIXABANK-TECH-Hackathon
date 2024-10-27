package com.hackathon.bankingapp.Services;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class OtpService {

    private Map<String, String> otpStorage = new HashMap<>();

    public boolean verifyOtp(String identifier, String otp) {
        String storedOtp = otpStorage.get(identifier);
        return storedOtp != null && storedOtp.equals(otp);
    }

    public void storeOtp(String identifier, String otp) {
        otpStorage.put(identifier, otp);
    }
}
