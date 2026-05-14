package org.example.jwtjavaeight.config;

import java.util.Arrays;
import java.util.Collections;
import org.example.jwtjavaeight.security.JwtAuthenticationFilter;
import org.example.jwtjavaeight.security.JwtAuthenticationProvider;
import org.example.jwtjavaeight.security.JwtLoginFilter;
import org.example.jwtjavaeight.security.handler.LoginFailureHandler;
import org.example.jwtjavaeight.security.handler.LoginSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 核心配置类
 * 配置 JWT 认证、CORS、会话管理、权限控制等安全策略
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final SecurityExceptionHandler securityExceptionHandler;
  private final UserDetailsService userDetailsService;
  private final LoginSuccessHandler loginSuccessHandler;
  private final LoginFailureHandler loginFailureHandler;
  private final AuthenticationConfiguration authenticationConfiguration;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      SecurityExceptionHandler securityExceptionHandler,
      UserDetailsService userDetailsService,
      LoginSuccessHandler loginSuccessHandler,
      LoginFailureHandler loginFailureHandler,
      AuthenticationConfiguration authenticationConfiguration) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.securityExceptionHandler = securityExceptionHandler;
    this.userDetailsService = userDetailsService;
    this.loginSuccessHandler = loginSuccessHandler;
    this.loginFailureHandler = loginFailureHandler;
    this.authenticationConfiguration = authenticationConfiguration;
  }

  /**
   * 配置安全过滤器链
   * 禁用 CSRF、启用 CORS、设置无状态会话、配置权限规则、注册 JWT 过滤器
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    AuthenticationManager authenticationManager =
        authenticationConfiguration.getAuthenticationManager();

    // 创建登录过滤器
    JwtLoginFilter loginFilter = new JwtLoginFilter(
        authenticationManager,
        loginSuccessHandler,
        loginFailureHandler
    );

    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeRequests(
                auth ->
                auth
                    // 认证接口白名单
                    .antMatchers("/api/v1/auth/**")
                    .permitAll()
                    // 开发工具接口白名单（仅非生产环境）
                    .antMatchers("/api/doc/**")
                    .permitAll()
                    // Swagger文档接口白名单
                    .antMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**")
                    .permitAll()
                    // 其他接口需要认证
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(securityExceptionHandler)
                    .accessDeniedHandler(securityExceptionHandler))
        .authenticationProvider(
            new JwtAuthenticationProvider(userDetailsService, passwordEncoder()))
        // 注意顺序：先添加登录过滤器，再添加JWT认证过滤器
        .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * CORS 跨域配置
   * 允许所有来源、常用 HTTP 方法、携带凭证
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(Collections.singletonList("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /**
   * 密码编码器，使用 BCrypt 强哈希算法
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
