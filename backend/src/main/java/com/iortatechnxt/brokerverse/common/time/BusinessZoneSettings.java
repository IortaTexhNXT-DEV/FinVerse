package com.iortatechnxt.brokerverse.common.time;

import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Reads {@code brokerverse.business-zone} after the configuration files are read and before any
 * bean is created (registered in {@code META-INF/spring.factories}), and hands it to {@link
 * BusinessClock}. An unknown zone refuses the start.
 */
public final class BusinessZoneSettings implements EnvironmentPostProcessor, Ordered {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    BusinessClock.use(
        resolve(
            environment.getProperty(BusinessClock.ZONE_PROPERTY, BusinessClock.DEFAULT_ZONE_ID)));
  }

  @Override
  public int getOrder() {
    return ConfigDataEnvironmentPostProcessor.ORDER + 1;
  }

  static ZoneId resolve(String value) {
    String id = value == null || value.isBlank() ? BusinessClock.DEFAULT_ZONE_ID : value.trim();
    try {
      return ZoneId.of(id);
    } catch (DateTimeException e) {
      throw new IllegalStateException(
          BusinessClock.ZONE_PROPERTY + " is not a known time zone: " + id, e);
    }
  }
}
