package org.example.jwtjavaeight.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import org.springdoc.core.SwaggerUiConfigParameters;
import org.springdoc.core.SwaggerUiConfigProperties;
import org.springdoc.core.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

/**
 * springdoc-openapi-ui 1.6.15 没有 customCss/customCssUrl 字段，也没有 addCustomCssUrl API。
 * 通过覆盖默认的 SwaggerIndexTransformer，在 swagger-initializer.js 末尾追加一段 IIFE，
 * 由浏览器动态创建 <link> 标签加载本地暗黑主题 CSS。
 */
@Configuration
public class SwaggerUiConfig {

  private static final String DARK_THEME_CSS_URL =
      "https://cdn.jsdelivr.net/gh/Amoenus/SwaggerDark@master/SwaggerDark.css";

  @Bean
  public SwaggerIndexTransformer swaggerIndexTransformer(
      SwaggerUiConfigProperties swaggerUiConfigProperties,
      SwaggerUiOAuthProperties swaggerUiOAuthProperties,
      SwaggerUiConfigParameters swaggerUiConfigParameters,
      SwaggerWelcomeCommon swaggerWelcomeCommon,
      ObjectMapperProvider objectMapperProvider) {
    return new SwaggerIndexPageTransformer(
        swaggerUiConfigProperties,
        swaggerUiOAuthProperties,
        swaggerUiConfigParameters,
        swaggerWelcomeCommon,
        objectMapperProvider) {
      @Override
      @NonNull
      public Resource transform(
          @NonNull HttpServletRequest request,
          @NonNull Resource resource,
          @NonNull ResourceTransformerChain chain)
          throws IOException {
        Resource transformed = super.transform(request, resource, chain);
        if (!(transformed instanceof TransformedResource)) {
          return transformed;
        }
        byte[] bytes = ((TransformedResource) transformed).getByteArray();
        String injection =
            "\n(function(){var l=document.createElement('link');"
                + "l.rel='stylesheet';l.href='"
                + DARK_THEME_CSS_URL
                + "';document.head.appendChild(l);})();\n";
        byte[] patched =
            (new String(bytes, StandardCharsets.UTF_8) + injection).getBytes(StandardCharsets.UTF_8);
        return new TransformedResource(resource, patched);
      }
    };
  }
}
