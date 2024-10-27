package com.hackathon.bankingapp.Controllers;

import com.hackathon.bankingapp.DTO.RegisterUserDTO;
import com.hackathon.bankingapp.DTO.UserResponseDTO;
import com.hackathon.bankingapp.Entities.Account;
import com.hackathon.bankingapp.Entities.User;
import com.hackathon.bankingapp.Services.TokenRevocationService;
import com.hackathon.bankingapp.Services.UserService;
import com.hackathon.bankingapp.Utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

import java.util.HashMap;
import java.security.SecureRandom;
import java.util.Map;

@RestController
@RequestMapping("api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterUserDTO user) {
        if (user.getName() == null || user.getName().isEmpty() ||
            user.getEmail() == null || user.getEmail().isEmpty() ||
            user.getPhoneNumber() == null || user.getPhoneNumber().isEmpty() ||
            user.getPassword() == null || user.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body("No empty fields allowed");
        }
    
        if (!user.getEmail().matches("^[\\w-\\.]+@[\\w-]+\\.[a-zA-Z]{2,4}$")) {
            return ResponseEntity.badRequest().body("Invalid email format");
        }
    
        if (userService.getUserByEmail(user.getEmail()).isPresent() ||
            userService.getUserByPhoneNumber(user.getPhoneNumber()).isPresent()) {
            return ResponseEntity.badRequest().body("Email or phone number already exists");
        }

        // Validación de la contraseña
        String password = user.getPassword();
        String passwordError = validatePassword(password);
        if (passwordError != null) {
            return ResponseEntity.badRequest().body(passwordError);
        }
    
        String hashedPassword = passwordEncoder.encode(user.getPassword());
    
        User newUser = new User();
        newUser.setName(user.getName());
        newUser.setEmail(user.getEmail());
        newUser.setPhoneNumber(user.getPhoneNumber());
        newUser.setAddress(user.getAddress());
        newUser.setHashedPassword(hashedPassword);
    
        Account account = new Account();
        account.setAccountNumber(generateRandomAccountNumber(6)); 
        account.setBalance(BigDecimal.ZERO);
        
        newUser.setAccount(account); 
    
        User savedUser = userService.createUser(newUser, account);
    
        UserResponseDTO responseDTO = new UserResponseDTO(
            savedUser.getName(),
            savedUser.getEmail(),
            savedUser.getPhoneNumber(),
            savedUser.getAddress(),
            account.getAccountNumber(),
            savedUser.getHashedPassword() 
        );
    
        return ResponseEntity.ok(responseDTO);
    }

    private String validatePassword(String password) {
        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }
    
        if (password.length() > 128) {
            return "Password must be less than 128 characters long";
        }
    
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter";
        }
    
        if (!password.matches(".*\\d.*")) {
            return "Password must contain at least one digit and one special character";
        }

        if (!password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            return "Password must contain at least one special character";
        }
    
        if (password.contains(" ")) {
            return "Password cannot contain whitespace";
        }
    
        return null;
    }
    

    public String generateRandomAccountNumber(int length) {
        String characters = "0123456789abcdefghijklmnopqrstuvwxyz";
        SecureRandom random = new SecureRandom();
        StringBuilder accountNumber = new StringBuilder(length);
    
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            accountNumber.append(characters.charAt(index));
        }
    
        return accountNumber.toString();
        }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials) {
        String identifier = credentials.get("identifier");
        String password = credentials.get("password");

        User user = userService.getUserByEmail(identifier).orElse(null);

        if (user == null) {
            return ResponseEntity.badRequest().body("User not found for the given identifier: " + identifier);
        }

        if (!passwordEncoder.matches(password, user.getHashedPassword())) {
            return ResponseEntity.status(401).body("Bad credentials");
        }

        String token = jwtUtil.generateToken(identifier);
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logout")
    public ResponseEntity<String> logoutUser(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7); 
            tokenRevocationService.revokeToken(token);
        }
    
        return ResponseEntity.ok("Logged out successfully");
    }
    
}

