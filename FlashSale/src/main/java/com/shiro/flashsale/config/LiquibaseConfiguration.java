package com.shiro.flashsale.config;

import javax.sql.DataSource;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquibaseConfiguration {
  @Bean
  @ConditionalOnProperty(name = "spring.liquibase.enabled", havingValue = "true")
  public SpringLiquibase liquibase(
      DataSource dataSource,
      @Value("${spring.liquibase.change-log:classpath:db/changelog/db.changelog-master.yaml}")
          String changeLog) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog(changeLog);
    return liquibase;
  }
}
