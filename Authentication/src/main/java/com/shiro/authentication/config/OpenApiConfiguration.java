package com.shiro.authentication.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
  @Bean
  OpenAPI authenticationOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Authentication API")
                .version("v1")
                .description("User registration, OTP verification, login and logout APIs."));
  }
}
