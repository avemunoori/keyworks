package com.example.keyworks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Allow requests to these paths without authentication
                .requestMatchers("/", "/piano", "/js/**", "/css/**").permitAll()
                // All other paths require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                // Specify a custom login page
                .loginPage("/login")
                // Allow everyone to see the login page (i.e., no need to be logged in for it)
                .permitAll()
            )
            .logout(logout -> logout
                // Make the logout endpoint available for all
                .permitAll()
            );

        // Build the SecurityFilterChain
        return http.build();
    }
}
