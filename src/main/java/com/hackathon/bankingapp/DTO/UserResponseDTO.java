package com.hackathon.bankingapp.DTO;

public class UserResponseDTO {
    private String name;
    private String email;
    private String phoneNumber;
    private String address;
    private String accountNumber;
    private String hashedPassword;

    public UserResponseDTO(String name, String email, String phoneNumber, String address, String accountNumber, String hashedPassword) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.accountNumber = accountNumber;
        this.hashedPassword = hashedPassword;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }
}
