package com.iortatechnxt.brokerverse.config;

import java.util.Map;
import java.util.Properties;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

/**
 * Connection of the database migrations when they run as the schema owner (docs/operations
 * DEPLOYMENT.md "Database roles").
 *
 * <p>With {@code spring.flyway.user} set (environment {@code SPRING_FLYWAY_USER} and {@code
 * SPRING_FLYWAY_PASSWORD}), Flyway connects as the owner of the schema objects while the
 * application pool connects as the least-privilege runtime login ({@code BROKERVERSE_DB_USER}).
 * Spring Boot would build that migration connection from the URL and credentials only; this
 * customizer adds the driver properties of the application pool ({@code
 * spring.datasource.hikari.data-source-properties}: {@code sslmode}, {@code sslrootcert}...), so
 * the migration connection is verified by TLS exactly like the application's. Settings in the JDBC
 * URL still take precedence, as for the pool. Without {@code spring.flyway.user} (development,
 * tests, single-user stacks) nothing changes.
 */
@Component
public class MigrationOwnerConnection implements FlywayConfigurationCustomizer {

  static final String OWNER_PROPERTY = "spring.flyway.user";
  private static final String POOL_DRIVER_PROPERTIES =
      "spring.datasource.hikari.data-source-properties";

  private final Environment environment;

  /**
   * Creates the customizer.
   *
   * @param environment resolved configuration
   */
  public MigrationOwnerConnection(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void customize(FluentConfiguration configuration) {
    String owner = environment.getProperty(OWNER_PROPERTY, "");
    if (owner.isBlank()) {
      return;
    }
    DriverManagerDataSource connection = new DriverManagerDataSource();
    connection.setUrl(
        environment.getProperty(
            "spring.flyway.url", environment.getProperty("spring.datasource.url", "")));
    Properties driverProperties = new Properties();
    driverProperties.putAll(
        Binder.get(environment)
            .bind(POOL_DRIVER_PROPERTIES, Bindable.mapOf(String.class, String.class))
            .orElse(Map.of()));
    connection.setConnectionProperties(driverProperties);
    connection.setUsername(owner);
    connection.setPassword(environment.getProperty("spring.flyway.password", ""));
    configuration.dataSource(connection);
  }
}
