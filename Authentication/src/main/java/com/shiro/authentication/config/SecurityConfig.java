package com.shiro.authentication.config;

import com.shiro.authentication.security.TokenAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, TokenAuthenticationFilter tokenFilter)
      throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/verify-otp",
                        "/api/v1/auth/login",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/actuator/health/**",
                        "/actuator/info",
                        "/error")
                    .permitAll()
                    .requestMatchers("/api/v1/auth/logout")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  FilterRegistrationBean<TokenAuthenticationFilter> disableTokenFilterAutoRegistration(
      TokenAuthenticationFilter filter) {
    FilterRegistrationBean<TokenAuthenticationFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
