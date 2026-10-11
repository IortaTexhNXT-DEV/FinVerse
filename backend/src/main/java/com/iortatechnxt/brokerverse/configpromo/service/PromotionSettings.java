package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageException;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageSigner;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Settings of Configuration Promotion in this environment: its name and whether it is production
 * ({@code brokerverse.environment}), the signing key of the packages ({@code
 * brokerverse.config-promotion.signing-key}, the same key in every environment that exchanges
 * packages, from the secrets store), the platform and schema versions and the largest package.
 */
@Component
public class PromotionSettings {

  /** Change window of production imports. */
  public static final String WINDOW_PARAMETER = "CONFIG_PROMOTION_PRODUCTION_WINDOW";

  /** Permissions told about the imports. */
  public static final String NOTIFY_PARAMETER = "CONFIG_PROMOTION_NOTIFY";

  /** Pipeline apply outside production. */
  public static final String PIPELINE_PARAMETER = "CONFIG_PROMOTION_PIPELINE_APPLY";

  private static final String SIZE_PARAMETER = "CONFIG_PROMOTION_MAX_PACKAGE_MB";
  private static final int DEFAULT_MAX_MB = 50;
  private static final long MEGABYTE = 1024L * 1024L;
  private static final String PRODUCTION = "production";
  private static final String SCHEMA_VERSION_SQL =
      """
      select version from flyway_schema_history where success and version is not null
      order by cast(split_part(version, '.', 1) as integer) desc, installed_rank desc limit 1
      """;

  private final String environment;
  private final String signingKey;
  private final Environment spring;
  private final ObjectProvider<BuildProperties> build;
  private final JdbcTemplate jdbc;
  private final SystemParameterService parameters;

  /**
   * Creates the settings.
   *
   * @param environment {@code brokerverse.environment}
   * @param signingKey signing key, empty when not configured
   * @param spring Spring environment (profiles)
   * @param build build information
   * @param jdbc database (schema version)
   * @param parameters business parameters
   */
  public PromotionSettings(
      @Value("${brokerverse.environment:local}") String environment,
      @Value("${brokerverse.config-promotion.signing-key:}") String signingKey,
      Environment spring,
      ObjectProvider<BuildProperties> build,
      JdbcTemplate jdbc,
      SystemParameterService parameters) {
    this.environment = environment;
    this.signingKey = signingKey;
    this.spring = spring;
    this.build = build;
    this.jdbc = jdbc;
    this.parameters = parameters;
  }

  /**
   * Name of this environment as written in the packages.
   *
   * @return environment, upper case
   */
  public String environment() {
    return environment.toUpperCase(Locale.ROOT);
  }

  /**
   * Whether this environment is production.
   *
   * @return true for {@code brokerverse.environment=production} or the prod profile
   */
  public boolean production() {
    return PRODUCTION.equalsIgnoreCase(environment) || spring.acceptsProfiles(Profiles.of("prod"));
  }

  /**
   * Whether the signing key is configured.
   *
   * @return true when packages can be signed and verified
   */
  public boolean signingConfigured() {
    return signingKey != null && !signingKey.isBlank();
  }

  /**
   * The signer of the packages.
   *
   * @return signer
   */
  public PackageSigner signer() {
    try {
      return new PackageSigner(signingKey);
    } catch (PackageException e) {
      throw new BusinessRuleException("CONFIG_SIGNING_KEY_MISSING", e.getMessage(), e);
    }
  }

  /**
   * Version of the application.
   *
   * @return version
   */
  public String platformVersion() {
    BuildProperties props = build.getIfAvailable();
    return props != null
        ? props.getVersion()
        : Optional.ofNullable(getClass().getPackage().getImplementationVersion())
            .orElse("development");
  }

  /**
   * Schema version: the highest database migration applied.
   *
   * @return version
   */
  public String schemaVersion() {
    return jdbc.queryForObject(SCHEMA_VERSION_SQL, String.class);
  }

  /**
   * Largest unpacked package.
   *
   * @return bytes
   */
  public long maxPackageBytes() {
    return parameters.intValue(SIZE_PARAMETER, DEFAULT_MAX_MB) * MEGABYTE;
  }

  /**
   * The change window of production imports.
   *
   * @return window text, empty when none
   */
  public String productionWindow() {
    return parameters.text(WINDOW_PARAMETER, "");
  }

  /**
   * Whether the pipeline may apply its own dry run here.
   *
   * @return true outside production when the parameter allows it
   */
  public boolean pipelineApplyAllowed() {
    return !production() && Boolean.parseBoolean(parameters.text(PIPELINE_PARAMETER, "false"));
  }

  /**
   * Permissions whose holders are told about the imports.
   *
   * @return permissions
   */
  public java.util.List<String> notifiedPermissions() {
    return parameters.items(NOTIFY_PARAMETER);
  }
}
