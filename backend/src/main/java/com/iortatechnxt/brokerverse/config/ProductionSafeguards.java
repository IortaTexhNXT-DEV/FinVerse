package com.iortatechnxt.brokerverse.config;

import com.iortatechnxt.brokerverse.common.office.BrandAssets;
import com.iortatechnxt.brokerverse.common.runtime.RuntimeRole;
import com.iortatechnxt.brokerverse.common.runtime.Workload;
import com.iortatechnxt.brokerverse.common.util.AsciiCase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

/**
 * Start-up safeguards of a deployment, checked after the configuration files are read and before
 * any bean is created (registered in {@code META-INF/spring.factories}).
 *
 * <ul>
 *   <li><b>Seed data.</b> The start is refused when the {@code seed} profile (SIT, UAT and training
 *       data) is active together with the {@code prod} profile or with {@code
 *       brokerverse.environment=production}.
 *   <li><b>Secrets (every environment except local).</b> A start with {@code
 *       brokerverse.environment} other than {@code local} (SIT, UAT, training, pre-production,
 *       production) or with the {@code prod} profile is refused when a required secret is missing
 *       or weak: the database URL, user and password; the JWT signing key (at least 32 characters
 *       and not a development value); the key of the second-factor secrets (32 random bytes,
 *       Base64); the SMTP host and credentials and the sender address when mail delivery is on; the
 *       Valkey password when Valkey is on; the Kafka SASL credentials when Kafka is on; the client
 *       secret of the single sign-on provider when one is configured. The allowed web origins and
 *       the password reset page must be set and must not point to the developer's machine, and a
 *       masking key, when set, must not be a development value.
 *   <li><b>Database roles.</b> A production start is refused unless the migrations run as the
 *       schema owner ({@code SPRING_FLYWAY_USER} and {@code SPRING_FLYWAY_PASSWORD}) and the
 *       application connects as a different, least-privilege runtime login ({@code
 *       BROKERVERSE_DB_USER}); see DEPLOYMENT.md "Database roles".
 *   <li><b>Encryption in transit.</b> A production start is refused when a connection could run in
 *       plaintext: PostgreSQL without {@code sslmode=verify-full} (or {@code verify-ca}), Valkey
 *       without TLS, Kafka without {@code SASL_SSL}, the HTTP listener without TLS ({@code
 *       server.ssl.enabled}).
 *   <li><b>Integration tokens.</b> A production instance serving {@code /integration/**} (runtime
 *       role {@code integration} or {@code all}) needs the API gateway's key set, issuer and
 *       audience.
 * </ul>
 *
 * <p>Every problem is listed in one message, so an operator fixes the deployment in one pass. The
 * API documentation (springdoc) is switched off outside local and the {@code dev} profile.
 */
@SuppressWarnings("PMD.GodClass") // every start-up check in one place, listed in one message
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

  /** Kafka protocol required in production (Kafka reads the value case-insensitively). */
  private static final Pattern ENCRYPTED_KAFKA_PROTOCOL =
      Pattern.compile("SASL_SSL", Pattern.CASE_INSENSITIVE);

  private static final String SERVER_BUNDLE = "server";
  private static final Set<String> VERIFYING_SSL_MODES = Set.of("verify-full", "verify-ca");
  private static final Pattern URL_SSL_MODE =
      Pattern.compile("[?&]sslmode=([^&]*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern DEVELOPMENT_VALUE =
      Pattern.compile(
          "change-me|local-|test-secret|test-.*-key|seed-profile", Pattern.CASE_INSENSITIVE);
  private static final Pattern LOCAL_ADDRESS =
      Pattern.compile("//(localhost|127\\.0\\.0\\.1|\\[::1])([:/]|$)", Pattern.CASE_INSENSITIVE);

  /** Value of {@link #ENVIRONMENT_PROPERTY} of a developer's machine (the default). */
  public static final String LOCAL = "local";

  /** Bytes of the key of the second-factor secrets (AES-256). */
  public static final int MFA_KEY_BYTES = 32;

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    check(environment);
    disableApiDocsOutsideLocal(environment);
  }

  /**
   * Switches the API documentation (springdoc: {@code /v3/api-docs}, {@code /swagger-ui}) off in
   * every environment except local and the {@code dev} profile.
   *
   * @param environment environment
   */
  static void disableApiDocsOutsideLocal(ConfigurableEnvironment environment) {
    if (isLocal(environment) || environment.acceptsProfiles(Profiles.of("dev"))) {
      return;
    }
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "brokerverseApiDocsOff",
                Map.of(
                    "springdoc.api-docs.enabled",
                    "false",
                    "springdoc.swagger-ui.enabled",
                    "false")));
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
          BrandAssets.SYSTEM_NAME + " refuses to start: " + String.join("; ", problems) + ".");
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
    if (production
        && Boolean.parseBoolean(environment.getProperty("brokerverse.identity.simulator", ""))) {
      problems.add(
          "the Enterprise SSO simulator is on in production; set BROKERVERSE_IDENTITY_SIMULATOR to"
              + " false");
    }
    if (!isLocal(environment)) {
      requireSecrets(environment, problems);
      requireSharedEnvironmentSettings(environment, problems);
    }
    if (production) {
      requireSeparateDatabaseRoles(environment, problems);
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

  /**
   * Tells whether the environment is a developer's machine: {@code brokerverse.environment} is
   * {@code local} (or not set) and the {@code prod} profile is not active. Every other environment
   * gets the secret checks.
   *
   * @param environment resolved environment
   * @return true for a local environment
   */
  public static boolean isLocal(Environment environment) {
    String kind = environment.getProperty(ENVIRONMENT_PROPERTY, "").trim();
    return !isProduction(environment)
        && (kind.isEmpty() || AsciiCase.equalsIgnoreCase(LOCAL, kind));
  }

  private static void requireSharedEnvironmentSettings(Environment env, List<String> problems) {
    requireWebAddress(
        env,
        "brokerverse.security.allowed-origins",
        "BROKERVERSE_ALLOWED_ORIGINS (address of the web client)",
        "BROKERVERSE_ALLOWED_ORIGINS",
        problems);
    requireWebAddress(
        env,
        "brokerverse.security.password-reset-url",
        "BROKERVERSE_PASSWORD_RESET_URL (page of the password reset link)",
        "BROKERVERSE_PASSWORD_RESET_URL",
        problems);
    if (enabled(env, "brokerverse.mail.enabled", false)) {
      require(env, "brokerverse.mail.from-address", "BROKERVERSE_MAIL_FROM", problems);
    }
    requireKeys(env, problems);
    requireSingleSignOnSecrets(env, problems);
  }

  private static void requireWebAddress(
      Environment env, String property, String described, String variable, List<String> problems) {
    String value = env.getProperty(property, "");
    if (value.isBlank()) {
      problems.add(described + " is not set");
    } else if (LOCAL_ADDRESS.matcher(value).find()) {
      problems.add(variable + " must name the web client, not localhost");
    }
  }

  private static void requireKeys(Environment env, List<String> problems) {
    String mfaKey = env.getProperty("brokerverse.security.mfa.encryption-key", "");
    if (mfaKey.isBlank()) {
      problems.add("BROKERVERSE_MFA_ENCRYPTION_KEY (key of the second-factor secrets) is not set");
    } else if (!isRandomKey(mfaKey)) {
      problems.add(
          "BROKERVERSE_MFA_ENCRYPTION_KEY must be "
              + MFA_KEY_BYTES
              + " random bytes in Base64, not a development value");
    }
    String maskingKey = env.getProperty("brokerverse.migration.masking-key", "");
    if (!maskingKey.isBlank()
        && (maskingKey.length() < MIN_SECRET_LENGTH || isDevelopmentValue(maskingKey))) {
      problems.add(
          "BROKERVERSE_MIGRATION_MASKING_KEY must be a random value of at least "
              + MIN_SECRET_LENGTH
              + " characters, not a development value");
    }
  }

  private static void requireSingleSignOnSecrets(Environment env, List<String> problems) {
    if (!env.getProperty("brokerverse.security.sso.oidc.issuer", "").isBlank()) {
      require(
          env, "brokerverse.security.sso.oidc.client-id", "BROKERVERSE_OIDC_CLIENT_ID", problems);
      require(
          env,
          "brokerverse.security.sso.oidc.client-secret",
          "BROKERVERSE_OIDC_CLIENT_SECRET",
          problems);
    }
    if (!env.getProperty("brokerverse.security.sso.saml.idp-entity-id", "").isBlank()) {
      require(
          env,
          "brokerverse.security.sso.saml.idp-certificate",
          "BROKERVERSE_SAML_IDP_CERTIFICATE",
          problems);
    }
  }

  /**
   * Whether a key is {@value #MFA_KEY_BYTES} bytes in Base64 and not a development value.
   *
   * @param key Base64 key
   * @return true for a usable key
   */
  static boolean isRandomKey(String key) {
    if (isDevelopmentValue(key)) {
      return false;
    }
    try {
      byte[] bytes = Base64.getDecoder().decode(key.trim());
      return bytes.length == MFA_KEY_BYTES
          && !isDevelopmentValue(new String(bytes, StandardCharsets.ISO_8859_1));
    } catch (IllegalArgumentException ex) {
      return false;
    }
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
      require(env, "spring.data.redis.password", "BROKERVERSE_VALKEY_PASSWORD", problems);
    }
    if (enabled(env, "brokerverse.kafka.enabled", true)) {
      require(
          env,
          "spring.kafka.properties.sasl.jaas.config",
          "BROKERVERSE_KAFKA_SASL_JAAS_CONFIG",
          problems);
    }
  }

  private static void requireSeparateDatabaseRoles(Environment env, List<String> problems) {
    if (enabled(env, "spring.flyway.enabled", true)) {
      requireMigrationOwner(env, problems);
    }
  }

  private static void requireMigrationOwner(Environment env, List<String> problems) {
    String owner = env.getProperty(MigrationOwnerConnection.OWNER_PROPERTY, "").trim();
    if (owner.isEmpty()) {
      problems.add(
          "SPRING_FLYWAY_USER (schema owner running the migrations) is not set; the application"
              + " must connect as a least-privilege runtime login");
    } else {
      if (String.CASE_INSENSITIVE_ORDER.compare(
              owner, env.getProperty("spring.datasource.username", "").trim())
          == 0) {
        problems.add(
            "SPRING_FLYWAY_USER and BROKERVERSE_DB_USER name the same login; the application must"
                + " connect as a least-privilege runtime login, not as the schema owner");
      }
      require(env, "spring.flyway.password", "SPRING_FLYWAY_PASSWORD", problems);
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
          "BROKERVERSE_VALKEY_TLS must be true in production (ElastiCache in-transit encryption)");
    }
    if (enabled(env, "brokerverse.kafka.enabled", true)
        && !ENCRYPTED_KAFKA_PROTOCOL
            .matcher(env.getProperty("spring.kafka.properties.security.protocol", "").trim())
            .matches()) {
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
