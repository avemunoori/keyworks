package com.example.keyworks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Allow requests to these paths without authentication
                .requestMatchers("/", "/piano", "/js/**", "/css/**", "/web-piano/**").permitAll()
                // All other paths require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                // Specify a custom login page
                .loginPage("/login")
                // Set default success URL after login
                .defaultSuccessUrl("/piano", true)
                // Allow everyone to see the login page
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        // Disable CSRF for simplicity during development
        // In production, you should enable CSRF protection
        http.csrf(csrf -> csrf.disable());

        // Build the SecurityFilterChain
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    // Temporary in-memory user for testing
    // In production, replace with a proper UserDetailsService implementation
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();
        
        return new InMemoryUserDetailsManager(user);
    }
}