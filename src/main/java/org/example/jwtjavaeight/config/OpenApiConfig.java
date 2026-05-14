package org.example.jwtjavaeight.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "Bearer Token";
    return new OpenAPI()
        .openapi("3.0.1")
        .info(
            new Info()
                .title("JWT 双 Token 认证 API")
                .description("基于 Spring Security + JWT 的双 Token（Access + Refresh）认证接口文档")
                .version("1.0.0")
                .contact(new Contact().name("jwt-java-eight")))
        .components(
            new Components()
                .addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
  }
}
