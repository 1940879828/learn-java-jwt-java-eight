package org.example.jwtjavaeight;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class JwtJavaEightApplication {

  public static void main(String[] args) {
    ConfigurableApplicationContext context =
        SpringApplication.run(JwtJavaEightApplication.class, args);

    try {
      BuildProperties build = context.getBean(BuildProperties.class);
      String time =
          build
              .getTime()
              .atZone(ZoneId.systemDefault())
              .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      System.out.println("========================================");
      System.out.println("  Build Time: " + time);
      System.out.println("========================================");
    } catch (Exception e) {
      System.out.println("========================================");
      System.out.println("  Running in Development Mode");
      System.out.println("========================================");
    }
  }
}
