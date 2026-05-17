package org.example.jwtjavaeight.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Collections;
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
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        .path("/api/v1/auth/login", createLoginPathItem());
  }

  private PathItem createLoginPathItem() {
    return new PathItem()
        .post(
            new Operation()
                .summary("用户登录")
                .description("用户登录接口，由 JwtLoginFilter 处理，返回 Access Token 和 Refresh Token")
                .tags(Collections.singletonList("认证管理"))
                .security(Collections.emptyList()) // 登录接口不需要认证
                .requestBody(
                    new RequestBody()
                        .description("登录请求参数")
                        .required(true)
                        .content(
                            new Content()
                                .addMediaType(
                                    "application/json",
                                    new MediaType()
                                        .schema(
                                            new Schema<>()
                                                .$ref("#/components/schemas/LoginRequest"))
                                        .example(
                                            "{\n"
                                                + "  \"username\": \"admin\",\n"
                                                + "  \"password\": \"123456\",\n"
                                                + "  \"remember\": false\n"
                                                + "}"))))
                .responses(
                    new ApiResponses()
                        .addApiResponse(
                            "200",
                            new ApiResponse()
                                .description("登录成功")
                                .content(
                                    new Content()
                                        .addMediaType(
                                            "application/json",
                                            new MediaType()
                                                .schema(
                                                    new Schema<>()
                                                        .example(
                                                            "{\n"
                                                                + "  \"code\": 200,\n"
                                                                + "  \"message\": \"success\",\n"
                                                                + "  \"data\": {\n"
                                                                + "    \"accessToken\": \"eyJhbGciOiJIUzUxMiJ9...\",\n"
                                                                + "    \"refreshToken\": \"eyJhbGciOiJIUzUxMiJ9...\",\n"
                                                                + "    \"tokenPrefix\": \"Bearer\"\n"
                                                                + "  },\n"
                                                                + "  \"timestamp\": 1715644800000\n"
                                                                + "}")))))
                        .addApiResponse(
                            "401",
                            new ApiResponse()
                                .description("登录失败（用户名或密码错误、账户锁定等）")
                                .content(
                                    new Content()
                                        .addMediaType(
                                            "application/json",
                                            new MediaType()
                                                .schema(
                                                    new Schema<>()
                                                        .example(
                                                            "{\n"
                                                                + "  \"code\": 401,\n"
                                                                + "  \"message\": \"用户名或密码错误\",\n"
                                                                + "  \"data\": {\n"
                                                                + "    \"remainingAttempts\": 4,\n"
                                                                + "    \"lockRemainingSeconds\": null\n"
                                                                + "  },\n"
                                                                + "  \"timestamp\": 1715644800000\n"
                                                                + "}")))))));
  }
}
