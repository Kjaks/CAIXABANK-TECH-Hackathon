package com.hackathon.bankingapp.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.hackathon.bankingapp.Security.JwtRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtRequestFilter JwtRequestFilter;

    public SecurityConfig(
        JwtRequestFilter JwtRequestFilter
    ) {
        this.JwtRequestFilter = JwtRequestFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "api/users/register", "api/users/login",
                    "/api/auth/password-reset/send-otp", "/api/auth/password-reset/verify-otp", "/api/auth/password-reset",
                    "/market/prices", "/market/prices/{symbol}").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(JwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}