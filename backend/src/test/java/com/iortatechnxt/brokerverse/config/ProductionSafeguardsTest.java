package com.iortatechnxt.brokerverse.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSafeguardsTest {

  private static final String KEY = "Zq8v3N0kP1rT7yW2bX5cD9fG4hJ6mL0sQ";

  private static MockEnvironment completeProduction() {
    return new MockEnvironment()
        .withProperty("brokerverse.environment", "production")
        .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/bibs")
        .withProperty("spring.datasource.username", "bibs")
        .withProperty("spring.datasource.password", "from-the-vault")
        .withProperty("brokerverse.security.jwt-secret", KEY)
        .withProperty("brokerverse.mail.enabled", "true")
        .withProperty("spring.mail.host", "smtp.bdo.example")
        .withProperty("spring.mail.username", "bibs")
        .withProperty("spring.mail.password", "from-the-vault")
        .withProperty("spring.data.redis.password", "auth-token")
        .withProperty("spring.kafka.properties.security.protocol", "SASL_SSL")
        .withProperty("spring.kafka.properties.sasl.jaas.config", "ScramLoginModule required;")
        .withProperty("spring.datasource.hikari.data-source-properties.sslmode", "verify-full")
        .withProperty("spring.data.redis.ssl.enabled", "true")
        .withProperty("server.ssl.enabled", "true")
        .withProperty("server.ssl.bundle", "server")
        .withProperty(
            "spring.ssl.bundle.pem.server.keystore.certificate", "file:/etc/bibs/tls/tls.crt")
        .withProperty(
            "spring.ssl.bundle.pem.server.keystore.private-key", "file:/etc/bibs/tls/tls.key")
        .withProperty("brokerverse.runtime.role", "web");
  }

  @Test
  void aCompleteProductionConfigurationStarts() {
    assertThatCode(() -> ProductionSafeguards.check(completeProduction()))
        .doesNotThrowAnyException();
  }

  @Test
  void seedDataIsRefusedWithTheProdProfile() {
    MockEnvironment env = new MockEnvironment();
    env.setActiveProfiles("prod", "seed");

    assertThat(ProductionSafeguards.problems(env))
        .anySatisfy(p -> assertThat(p).contains("seed profile"));
  }

  @Test
  void seedDataIsRefusedInAProductionEnvironment() {
    MockEnvironment env = completeProduction();
    env.setActiveProfiles("seed");

    assertThatThrownBy(() -> ProductionSafeguards.check(env))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("seed profile");
  }

  @Test
  void seedDataIsAllowedOutsideProduction() {
    MockEnvironment env = new MockEnvironment().withProperty("brokerverse.environment", "uat");
    env.setActiveProfiles("seed");

    assertThat(ProductionSafeguards.problems(env)).isEmpty();
    assertThat(ProductionSafeguards.isProduction(env)).isFalse();
  }

  @Test
  void everyMissingSecretIsListed() {
    MockEnvironment env = new MockEnvironment().withProperty("brokerverse.mail.enabled", "true");
    env.setActiveProfiles("prod");

    assertThat(ProductionSafeguards.problems(env))
        .contains(
            "BROKERVERSE_DB_URL is not set",
            "BROKERVERSE_DB_USER is not set",
            "BROKERVERSE_DB_PASSWORD is not set",
            "BROKERVERSE_JWT_SECRET (JWT signing key) is not set",
            "MAIL_HOST is not set",
            "MAIL_USERNAME is not set",
            "MAIL_PASSWORD is not set",
            "BROKERVERSE_REDIS_PASSWORD is not set",
            "BROKERVERSE_KAFKA_SASL_JAAS_CONFIG is not set")
        .anySatisfy(p -> assertThat(p).contains("SASL_SSL"));
  }

  @Test
  void aDevelopmentOrShortSigningKeyIsRefused() {
    MockEnvironment dev =
        completeProduction()
            .withProperty(
                "brokerverse.security.jwt-secret",
                "seed-profile-local-signing-key-0123456789abcdef");
    MockEnvironment shortKey =
        completeProduction().withProperty("brokerverse.security.jwt-secret", "short");

    assertThat(ProductionSafeguards.problems(dev)).singleElement().asString().contains("random");
    assertThat(ProductionSafeguards.problems(shortKey))
        .singleElement()
        .asString()
        .contains("at least 32");
  }

  @Test
  void disabledMailRedisAndKafkaNeedNoCredentials() {
    MockEnvironment env =
        new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/bibs")
            .withProperty("spring.datasource.username", "bibs")
            .withProperty("spring.datasource.password", "from-the-vault")
            .withProperty("brokerverse.security.jwt-secret", KEY)
            .withProperty("brokerverse.redis.enabled", "false")
            .withProperty("brokerverse.kafka.enabled", "false")
            .withProperty("spring.datasource.hikari.data-source-properties.sslmode", "verify-full")
            .withProperty("server.ssl.enabled", "true")
            .withProperty("brokerverse.runtime.role", "jobs");
    env.setActiveProfiles("prod");

    assertThat(ProductionSafeguards.problems(env)).isEmpty();
  }

  @Test
  void plaintextTransportsAreRefusedInProduction() {
    MockEnvironment env =
        completeProduction()
            .withProperty("spring.datasource.hikari.data-source-properties.sslmode", "prefer")
            .withProperty("spring.data.redis.ssl.enabled", "false")
            .withProperty("spring.kafka.properties.security.protocol", "SASL_PLAINTEXT")
            .withProperty("server.ssl.enabled", "false");

    assertThat(ProductionSafeguards.problems(env))
        .hasSize(4)
        .anySatisfy(p -> assertThat(p).contains("sslmode=verify-full").endsWith("prefer"))
        .anySatisfy(p -> assertThat(p).startsWith("BROKERVERSE_REDIS_TLS"))
        .anySatisfy(p -> assertThat(p).startsWith("BROKERVERSE_KAFKA_SECURITY_PROTOCOL"))
        .anySatisfy(p -> assertThat(p).startsWith("BROKERVERSE_SERVER_SSL_ENABLED"));
  }

  @Test
  void httpsNeedsTheMountedCertificateAndKey() {
    MockEnvironment env =
        completeProduction()
            .withProperty("spring.ssl.bundle.pem.server.keystore.certificate", "")
            .withProperty("spring.ssl.bundle.pem.server.keystore.private-key", " ");

    assertThat(ProductionSafeguards.problems(env))
        .containsExactly(
            "BROKERVERSE_SERVER_SSL_CERTIFICATE is not set",
            "BROKERVERSE_SERVER_SSL_PRIVATE_KEY is not set");
  }

  @Test
  void theSslModeOfTheJdbcUrlTakesPrecedence() {
    MockEnvironment disabledInUrl =
        completeProduction()
            .withProperty(
                "spring.datasource.url",
                "jdbc:postgresql://db:5432/bibs?ApplicationName=x&sslmode=disable");
    MockEnvironment verifiedInUrl =
        completeProduction()
            .withProperty("spring.datasource.hikari.data-source-properties.sslmode", "")
            .withProperty(
                "spring.datasource.url", "jdbc:postgresql://db:5432/bibs?sslmode=VERIFY-CA");
    MockEnvironment nothing =
        completeProduction()
            .withProperty("spring.datasource.hikari.data-source-properties.sslmode", "");

    assertThat(ProductionSafeguards.databaseSslMode(disabledInUrl)).isEqualTo("disable");
    assertThat(ProductionSafeguards.problems(disabledInUrl))
        .singleElement()
        .asString()
        .endsWith("disable");
    assertThat(ProductionSafeguards.problems(verifiedInUrl)).isEmpty();
    assertThat(ProductionSafeguards.databaseSslMode(nothing)).isEqualTo("prefer");
  }

  @Test
  void anInstanceServingIntegrationApisNeedsTheGatewayTokenSettings() {
    MockEnvironment integration =
        completeProduction().withProperty("brokerverse.runtime.role", "integration");
    MockEnvironment configured =
        completeProduction()
            .withProperty("brokerverse.runtime.role", "INTEGRATION")
            .withProperty(
                "brokerverse.integration.security.jwk-set-uri", "https://gateway.bdo.example/jwks")
            .withProperty("brokerverse.integration.security.issuer", "https://gateway.bdo.example")
            .withProperty("brokerverse.integration.security.audiences", "bibs");

    assertThat(ProductionSafeguards.problems(integration))
        .containsExactly(
            "BROKERVERSE_INTEGRATION_JWK_SET_URI is not set",
            "BROKERVERSE_INTEGRATION_ISSUER is not set",
            "BROKERVERSE_INTEGRATION_AUDIENCES is not set");
    assertThat(ProductionSafeguards.problems(configured)).isEmpty();
  }

  @Test
  void anUnknownRuntimeRoleIsReported() {
    MockEnvironment env = completeProduction().withProperty("brokerverse.runtime.role", "batch");

    assertThat(ProductionSafeguards.problems(env))
        .singleElement()
        .asString()
        .contains("web, jobs, integration or all");
  }

  @Test
  void transportChecksDoNotApplyOutsideProduction() {
    MockEnvironment env =
        new MockEnvironment()
            .withProperty("brokerverse.environment", "sit")
            .withProperty("spring.datasource.hikari.data-source-properties.sslmode", "disable")
            .withProperty("spring.kafka.properties.security.protocol", "PLAINTEXT");

    assertThat(ProductionSafeguards.problems(env)).isEmpty();
  }
}
