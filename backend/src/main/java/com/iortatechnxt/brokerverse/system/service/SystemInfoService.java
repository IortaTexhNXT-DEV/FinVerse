package com.iortatechnxt.brokerverse.system.service;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Application information for administrators and the About dialog: product version and build,
 * database migration level, health and a read-only view of non-secret configuration.
 */
@Service
public class SystemInfoService {

  /** Product name shown in the About dialog. */
  public static final String PRODUCT = "iNXT BrokerVerse";

  /** Vendor. */
  public static final String VENDOR = "IortaTechNXT";

  /**
   * Whitelist of configuration keys shown to administrators. Secrets (passwords, JWT secret) are
   * deliberately absent and must never be added.
   */
  private static final List<String> VISIBLE_KEYS =
      List.of(
          "spring.application.name",
          "server.port",
          "spring.datasource.url",
          "spring.datasource.hikari.maximum-pool-size",
          "spring.flyway.locations",
          "spring.jpa.hibernate.ddl-auto",
          "spring.threads.virtual.enabled",
          "spring.servlet.multipart.max-file-size",
          "brokerverse.security.token-validity",
          "brokerverse.security.allowed-origins",
          "brokerverse.attachments.max-size",
          "brokerverse.jobs.recurring-journals-cron",
          "brokerverse.jobs.alert-checks-cron",
          "brokerverse.redis.enabled",
          "spring.data.redis.host",
          "spring.data.redis.port",
          "spring.data.redis.ssl.enabled",
          "brokerverse.kafka.enabled",
          "spring.kafka.bootstrap-servers",
          "spring.kafka.properties.security.protocol",
          "management.endpoints.web.exposure.include",
          "logging.level.root");

  private static final String LATEST_MIGRATION_SQL =
      """
      select version, description, installed_on from flyway_schema_history
      where success and version is not null order by installed_rank desc limit 1
      """;
  private static final String MIGRATION_COUNT_SQL =
      "select count(*) from flyway_schema_history where success and version is not null";
  private static final String UNKNOWN = "UNKNOWN";

  private final JdbcTemplate jdbc;
  private final Environment environment;
  private final ObjectProvider<BuildProperties> build;
  private final ObjectProvider<HealthEndpoint> health;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param jdbc JDBC access (flyway history, database version)
   * @param environment configuration
   * @param build build information (present when packaged by Maven)
   * @param health actuator health endpoint
   * @param clock clock
   */
  public SystemInfoService(
      JdbcTemplate jdbc,
      Environment environment,
      ObjectProvider<BuildProperties> build,
      ObjectProvider<HealthEndpoint> health,
      Clock clock) {
    this.jdbc = jdbc;
    this.environment = environment;
    this.build = build;
    this.health = health;
    this.clock = clock;
  }

  /**
   * Product, vendor, version and build time (About dialog).
   *
   * @return about information
   */
  public About about() {
    BuildProperties props = build.getIfAvailable();
    String version =
        props != null
            ? props.getVersion()
            : Optional.ofNullable(getClass().getPackage().getImplementationVersion())
                .orElse("development");
    return new About(PRODUCT, VENDOR, version, props == null ? null : props.getTime());
  }

  /**
   * Full application information for the administrator screen.
   *
   * @return information
   */
  public SystemInfo info() {
    Map<String, Object> latest = jdbc.queryForMap(LATEST_MIGRATION_SQL);
    Long migrations = jdbc.queryForObject(MIGRATION_COUNT_SQL, Long.class);
    String dbVersion = jdbc.queryForObject("select version()", String.class);
    HealthEndpoint endpoint = health.getIfAvailable();
    String status = endpoint == null ? UNKNOWN : endpoint.health().getStatus().getCode();
    Instant started = Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime());
    return new SystemInfo(
        about(),
        Runtime.version().toString(),
        dbVersion,
        String.valueOf(latest.get("version")),
        String.valueOf(latest.get("description")),
        migrations == null ? 0 : migrations,
        status,
        started,
        Duration.between(started, clock.instant()).toSeconds(),
        List.of(environment.getActiveProfiles()));
  }

  /**
   * Non-secret configuration values (read only).
   *
   * @return key/value pairs; unset keys are omitted
   */
  public List<ConfigEntry> configuration() {
    List<ConfigEntry> result = new ArrayList<>();
    for (String key : VISIBLE_KEYS) {
      String value = environment.getProperty(key);
      if (value != null) {
        result.add(new ConfigEntry(key, mask(key, value)));
      }
    }
    result.add(new ConfigEntry("java.version", Runtime.version().toString()));
    result.add(new ConfigEntry("user.timezone", String.valueOf(clock.getZone())));
    return result;
  }

  /** JDBC URLs may carry credentials as query parameters: never show them. */
  private static String mask(String key, String value) {
    int query = value.indexOf('?');
    return key.endsWith(".url") && query >= 0 ? value.substring(0, query) + "?…" : value;
  }

  /**
   * About information.
   *
   * @param product product name
   * @param vendor vendor
   * @param version version
   * @param buildTime build time (null when not packaged)
   */
  public record About(String product, String vendor, String version, Instant buildTime) {}

  /**
   * Application information.
   *
   * @param about product information
   * @param javaVersion Java runtime version
   * @param databaseVersion PostgreSQL version string
   * @param migrationVersion latest applied Flyway version
   * @param migrationDescription description of the latest migration
   * @param migrationsApplied number of applied versioned migrations
   * @param health overall health status
   * @param startedAt JVM start time
   * @param uptimeSeconds uptime
   * @param activeProfiles active Spring profiles
   */
  public record SystemInfo(
      About about,
      String javaVersion,
      String databaseVersion,
      String migrationVersion,
      String migrationDescription,
      long migrationsApplied,
      String health,
      Instant startedAt,
      long uptimeSeconds,
      List<String> activeProfiles) {}

  /**
   * One configuration value.
   *
   * @param key property key
   * @param value value
   */
  public record ConfigEntry(String key, String value) {}
}
