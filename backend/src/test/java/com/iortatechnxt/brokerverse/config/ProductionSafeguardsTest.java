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
        .withProperty("spring.kafka.properties.sasl.jaas.config", "ScramLoginModule required;");
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
            .withProperty("brokerverse.kafka.enabled", "false");
    env.setActiveProfiles("prod");

    assertThat(ProductionSafeguards.problems(env)).isEmpty();
  }
}
