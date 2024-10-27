package com.hackathon.bankingapp.Utils;

import java.util.UUID;

public class PasswordResetToken {
    private String token;

    public PasswordResetToken() {
        this.token = UUID.randomUUID().toString();
    }

    public String getToken() {
        return token;
    }
}
