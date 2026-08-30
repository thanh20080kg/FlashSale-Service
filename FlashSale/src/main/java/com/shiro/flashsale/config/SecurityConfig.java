package com.shiro.flashsale.config;

import com.shiro.flashsale.security.SecurityErrorResponder;
import com.shiro.flashsale.security.TokenAuthenticationFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      TokenAuthenticationFilter tokenFilter,
      @Qualifier("corsConfigurationSource") CorsConfigurationSource cors,
      SecurityErrorResponder errorResponder)
      throws Exception {
    return http
        // No cookies, no CSRF surface; the API is token-only.
        .csrf(AbstractHttpConfigurer::disable)
        .cors(c -> c.configurationSource(cors))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        // Nothing is kept in an HttpSession, which is what lets any instance serve any request.
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            h ->
                h.frameOptions(f -> f.deny())
                    .contentTypeOptions(Customizer.withDefaults())
                    .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true)))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/flash-sales/current")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/flash-sales/purchase")
                    .hasAuthority("PERM_SALE_PURCHASE")
                    .requestMatchers("/api/v1/me/**")
                    .authenticated()
                    .requestMatchers("/api/v1/admin/customers/**")
                    .hasAuthority("PERM_USER_MANAGE")
                    .requestMatchers("/api/v1/admin/**")
                    .hasAuthority("PERM_SALE_MANAGE")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e -> e.authenticationEntryPoint(errorResponder).accessDeniedHandler(errorResponder))
        .addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }

  /**
   * {@code @Component} filters are auto-registered with the servlet container. The token filter
   * belongs to the Spring Security chain only, so its container registration is switched off.
   */
  @Bean
  FilterRegistrationBean<TokenAuthenticationFilter> disableTokenFilterAutoRegistration(
      TokenAuthenticationFilter filter) {
    FilterRegistrationBean<TokenAuthenticationFilter> registration =
        new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
