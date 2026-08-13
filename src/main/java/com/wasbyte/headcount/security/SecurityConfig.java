package com.wasbyte.headcount.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login", "/error", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/security/**").hasAnyRole(
                                "SECURITY_OFFICER", "SECURITY_MANAGER", "ADMIN")
                        .requestMatchers("/headcount/manage/**").hasAnyRole(
                                "COUNTRY_MANAGER", "REGIONAL_MANAGER", "SUPPORT_MANAGER",
                                "PROGRAM_MANAGER", "DEPARTMENT_MANAGER", "UNIT_MANAGER",
                                "SECURITY_OFFICER", "SECURITY_MANAGER", "ADMIN")
                        .requestMatchers("/headcount/**", "/profile/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form.permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                )
                .build();
    }
}
