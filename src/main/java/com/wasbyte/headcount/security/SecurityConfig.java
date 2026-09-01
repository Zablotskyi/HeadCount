package com.wasbyte.headcount.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                                "/login", "/login.html", "/register", "/register.html",
                                "/api/csrf", "/error", "/favicon.ico", "/favicon.png",
                                "/css/**", "/js/**", "/images/**",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/registration").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/registration/organization-units").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/admin.html", "/admin/**").hasRole("ADMIN")
                        .requestMatchers("/security/**").hasAnyRole(
                                "SECURITY_OFFICER", "SECURITY_MANAGER", "ADMIN")
                        .requestMatchers("/headcount/manage/**").hasAnyRole(
                                "COUNTRY_MANAGER", "REGIONAL_MANAGER", "SUPPORT_MANAGER",
                                "PROGRAM_MANAGER", "DEPARTMENT_MANAGER", "UNIT_MANAGER",
                                "SECURITY_OFFICER", "SECURITY_MANAGER", "ADMIN")
                        .requestMatchers("/headcount/**", "/profile/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .failureUrl("/login?error")
                        .defaultSuccessUrl("/index.html", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                )
                .build();
    }
}
