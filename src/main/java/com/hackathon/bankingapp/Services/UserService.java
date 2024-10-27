package com.hackathon.bankingapp.Services;

import com.hackathon.bankingapp.Entities.Account;
import com.hackathon.bankingapp.Entities.User;
import com.hackathon.bankingapp.Repositories.AccountRepository;
import com.hackathon.bankingapp.Repositories.UserRepository;
import com.hackathon.bankingapp.Utils.PasswordResetToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(User user, Account account) {
        user.setAccount(account);
        account.setUser(user);
    
        return userRepository.save(user);
    }
    
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getHashedPassword(),
            new ArrayList<>()
        );
    }

    public ResponseEntity<String> verifyOtp(String identifier, String otp) {
        if (otpService.verifyOtp(identifier, otp)) {
            String resetToken = new PasswordResetToken().getToken();
            return ResponseEntity.ok("{\"passwordResetToken\": \"" + resetToken + "\"}");
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid OTP");
    }

    public ResponseEntity<String> resetPassword(String identifier, String resetToken, String newPassword) {
        updatePassword(identifier, newPassword);
        return ResponseEntity.ok("Password reset successfully");
    }

    private void updatePassword(String identifier, String newPassword) {
        Optional<User> userOptional = userRepository.findByEmail(identifier);
        User user = userOptional.get();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setHashedPassword(encodedPassword);
        userRepository.save(user);
    }
}
