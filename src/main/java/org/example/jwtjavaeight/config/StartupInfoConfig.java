package org.example.jwtjavaeight.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StartupInfoConfig implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger log = LoggerFactory.getLogger(StartupInfoConfig.class);

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    log.info("项目已启动：");
    log.info("  - Swagger UI：http://localhost:8080/swagger-ui.html");
    log.info("  - OpenAPI JSON：http://localhost:8080/v3/api-docs");
  }
}
