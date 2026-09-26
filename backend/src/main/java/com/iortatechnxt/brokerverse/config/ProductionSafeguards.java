package com.iortatechnxt.brokerverse.config;

import com.iortatechnxt.brokerverse.common.runtime.RuntimeRole;
import com.iortatechnxt.brokerverse.common.runtime.Workload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Start-up safeguards of a production deployment, checked after the configuration files are read
 * and before any bean is created (registered in {@code META-INF/spring.factories}).
 *
 * <ul>
 *   <li><b>Seed data.</b> The start is refused when the {@code seed} profile (SIT, UAT and training
 *       data) is active together with the {@code prod} profile or with {@code
 *       brokerverse.environment=production}.
 *   <li><b>Secrets.</b> A production start (profile {@code prod} or {@code
 *       brokerverse.environment=production}) is refused when a required secret is missing: the
 *       database URL, user and password; the JWT signing key (at least 32 characters and not a
 *       development value); the SMTP host and credentials when mail delivery is on; the Redis
 *       password when Redis is on; the Kafka SASL protocol and credentials when Kafka is on.
 *   <li><b>Encryption in transit.</b> A production start is refused when a connection could run in
 *       plaintext: PostgreSQL without {@code sslmode=verify-full} (or {@code verify-ca}), Redis
 *       without TLS, Kafka without {@code SASL_SSL}, the HTTP listener without TLS ({@code
 *       server.ssl.enabled}).
 *   <li><b>Integration tokens.</b> A production instance serving {@code /integration/**} (runtime
 *       role {@code integration} or {@code all}) needs the API gateway's key set, issuer and
 *       audience.
 * </ul>
 *
 * <p>Every problem is listed in one message, so an operator fixes the deployment in one pass.
 */
public final class ProductionSafeguards implements EnvironmentPostProcessor, Ordered {

  /** Profile that loads the seed data (db/seed migrations and the seed start-up runners). */
  public static final String SEED_PROFILE = "seed";

  /** Profile of a production deployment. */
  public static final String PROD_PROFILE = "prod";

  /**
   * Property naming the kind of environment ({@code local}, {@code sit}, {@code production}...).
   */
  public static final String ENVIRONMENT_PROPERTY = "brokerverse.environment";

  /** Value of {@link #ENVIRONMENT_PROPERTY} for production. */
  public static final String PRODUCTION = "production";

  private static final int MIN_SECRET_LENGTH = 32;
  private static final Pattern PRODUCTION_KIND =
      Pattern.compile(PRODUCTION, Pattern.CASE_INSENSITIVE);
  private static final String SASL_SSL = "SASL_SSL";
  private static final String SERVER_BUNDLE = "server";
  private static final Set<String> VERIFYING_SSL_MODES = Set.of("verify-full", "verify-ca");
  private static final Pattern URL_SSL_MODE =
      Pattern.compile("[?&]sslmode=([^&]*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern DEVELOPMENT_VALUE =
      Pattern.compile("change-me|local-|test-secret|seed-profile", Pattern.CASE_INSENSITIVE);

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    check(environment);
  }

  @Override
  public int getOrder() {
    return ConfigDataEnvironmentPostProcessor.ORDER + 1;
  }

  /**
   * Refuses the start when {@link #problems(Environment)} finds anything.
   *
   * @param environment resolved environment
   * @throws IllegalStateException listing every problem found
   */
  public static void check(Environment environment) {
    List<String> problems = problems(environment);
    if (!problems.isEmpty()) {
      throw new IllegalStateException(
          "BIBS refuses to start: " + String.join("; ", problems) + ".");
    }
  }

  /**
   * Lists the safeguard violations of an environment.
   *
   * @param environment resolved environment
   * @return the problems, empty when the start may go on
   */
  public static List<String> problems(Environment environment) {
    List<String> problems = new ArrayList<>();
    boolean production = isProduction(environment);
    if (production && environment.acceptsProfiles(Profiles.of(SEED_PROFILE))) {
      problems.add(
          "the seed profile (SIT/UAT seed data) is active in production; remove 'seed' from"
              + " SPRING_PROFILES_ACTIVE");
    }
    if (production) {
      requireSecrets(environment, problems);
      requireEncryptionInTransit(environment, problems);
      requireIntegrationTokens(environment, problems);
    }
    return problems;
  }

  /**
   * Tells whether the environment is a production one.
   *
   * @param environment resolved environment
   * @return true for the {@code prod} profile or {@code brokerverse.environment=production}
   */
  public static boolean isProduction(Environment environment) {
    String kind = environment.getProperty(ENVIRONMENT_PROPERTY, "");
    return environment.acceptsProfiles(Profiles.of(PROD_PROFILE))
        || PRODUCTION_KIND.matcher(kind.trim()).matches();
  }

  private static void requireSecrets(Environment env, List<String> problems) {
    require(env, "spring.datasource.url", "BROKERVERSE_DB_URL", problems);
    require(env, "spring.datasource.username", "BROKERVERSE_DB_USER", problems);
    require(env, "spring.datasource.password", "BROKERVERSE_DB_PASSWORD", problems);
    String jwt = env.getProperty("brokerverse.security.jwt-secret", "");
    if (jwt.isBlank()) {
      problems.add("BROKERVERSE_JWT_SECRET (JWT signing key) is not set");
    } else if (jwt.length() < MIN_SECRET_LENGTH || isDevelopmentValue(jwt)) {
      problems.add(
          "BROKERVERSE_JWT_SECRET must be a random value of at least "
              + MIN_SECRET_LENGTH
              + " characters, not a development value");
    }
    if (enabled(env, "brokerverse.mail.enabled", false)) {
      require(env, "spring.mail.host", "MAIL_HOST", problems);
      if (enabled(env, "spring.mail.properties.mail.smtp.auth", true)) {
        require(env, "spring.mail.username", "MAIL_USERNAME", problems);
        require(env, "spring.mail.password", "MAIL_PASSWORD", problems);
      }
    }
    if (enabled(env, "brokerverse.redis.enabled", true)) {
      require(env, "spring.data.redis.password", "BROKERVERSE_REDIS_PASSWORD", problems);
    }
    if (enabled(env, "brokerverse.kafka.enabled", true)) {
      require(
          env,
          "spring.kafka.properties.sasl.jaas.config",
          "BROKERVERSE_KAFKA_SASL_JAAS_CONFIG",
          problems);
    }
  }

  private static void requireEncryptionInTransit(Environment env, List<String> problems) {
    String sslMode = databaseSslMode(env);
    if (!VERIFYING_SSL_MODES.contains(sslMode)) {
      problems.add(
          "the database connection must use TLS with sslmode=verify-full (BROKERVERSE_DB_SSL_MODE),"
              + " not "
              + sslMode);
    }
    if (enabled(env, "brokerverse.redis.enabled", true)
        && !enabled(env, "spring.data.redis.ssl.enabled", false)
        && env.getProperty("spring.data.redis.ssl.bundle", "").isBlank()) {
      problems.add(
          "BROKERVERSE_REDIS_TLS must be true in production (ElastiCache in-transit encryption)");
    }
    if (enabled(env, "brokerverse.kafka.enabled", true)
        && !SASL_SSL.equalsIgnoreCase(
            env.getProperty("spring.kafka.properties.security.protocol", "").trim())) {
      problems.add("BROKERVERSE_KAFKA_SECURITY_PROTOCOL must be SASL_SSL in production");
    }
    if (!enabled(env, "server.ssl.enabled", false)) {
      problems.add(
          "BROKERVERSE_SERVER_SSL_ENABLED must be true in production (HTTPS from the load balancer)");
    } else if (SERVER_BUNDLE.equals(env.getProperty("server.ssl.bundle", "").trim())) {
      require(
          env,
          "spring.ssl.bundle.pem.server.keystore.certificate",
          "BROKERVERSE_SERVER_SSL_CERTIFICATE",
          problems);
      require(
          env,
          "spring.ssl.bundle.pem.server.keystore.private-key",
          "BROKERVERSE_SERVER_SSL_PRIVATE_KEY",
          problems);
    }
  }

  /**
   * The effective PostgreSQL {@code sslmode}: the one of the JDBC URL, else the driver property,
   * else the driver default {@code prefer}.
   *
   * @param env environment
   * @return lower-case ssl mode
   */
  static String databaseSslMode(Environment env) {
    Matcher inUrl = URL_SSL_MODE.matcher(env.getProperty("spring.datasource.url", ""));
    String mode =
        inUrl.find()
            ? inUrl.group(1)
            : env.getProperty("spring.datasource.hikari.data-source-properties.sslmode", "");
    return mode.isBlank() ? "prefer" : mode.trim().toLowerCase(Locale.ROOT);
  }

  private static void requireIntegrationTokens(Environment env, List<String> problems) {
    RuntimeRole role;
    try {
      role = RuntimeRole.of(env);
    } catch (IllegalStateException ex) {
      problems.add(ex.getMessage());
      return;
    }
    if (role.runs(Workload.INTEGRATION)) {
      require(
          env,
          "brokerverse.integration.security.jwk-set-uri",
          "BROKERVERSE_INTEGRATION_JWK_SET_URI",
          problems);
      require(
          env,
          "brokerverse.integration.security.issuer",
          "BROKERVERSE_INTEGRATION_ISSUER",
          problems);
      require(
          env,
          "brokerverse.integration.security.audiences",
          "BROKERVERSE_INTEGRATION_AUDIENCES",
          problems);
    }
  }

  private static void require(
      Environment env, String property, String variable, List<String> problems) {
    if (env.getProperty(property, "").isBlank()) {
      problems.add(variable + " is not set");
    }
  }

  private static boolean enabled(Environment env, String property, boolean fallback) {
    return Boolean.parseBoolean(env.getProperty(property, String.valueOf(fallback)).trim());
  }

  private static boolean isDevelopmentValue(String secret) {
    return DEVELOPMENT_VALUE.matcher(secret).find();
  }
}
