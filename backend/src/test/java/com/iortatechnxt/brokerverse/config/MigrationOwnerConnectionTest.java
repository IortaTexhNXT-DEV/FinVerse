package com.iortatechnxt.brokerverse.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;

class MigrationOwnerConnectionTest {

  @Test
  void withoutASchemaOwnerTheMigrationsKeepTheApplicationConnection() {
    FluentConfiguration configuration = new FluentConfiguration();

    new MigrationOwnerConnection(new MockEnvironment()).customize(configuration);

    assertThat(configuration.getDataSource()).isNull();
  }

  @Test
  void theSchemaOwnerConnectsWithTheTlsSettingsOfThePool() {
    MockEnvironment env =
        new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/bibs")
            .withProperty("spring.datasource.hikari.data-source-properties.sslmode", "verify-full")
            .withProperty(
                "spring.datasource.hikari.data-source-properties.sslrootcert",
                "/etc/bibs/rds-ca/global-bundle.pem")
            .withProperty("spring.flyway.user", "bibs_owner")
            .withProperty("spring.flyway.password", "owner-secret");
    FluentConfiguration configuration = new FluentConfiguration();

    new MigrationOwnerConnection(env).customize(configuration);

    assertThat(configuration.getDataSource())
        .isInstanceOfSatisfying(
            DriverManagerDataSource.class,
            ds -> {
              assertThat(ds.getUrl()).isEqualTo("jdbc:postgresql://db:5432/bibs");
              assertThat(ds.getUsername()).isEqualTo("bibs_owner");
              assertThat(ds.getPassword()).isEqualTo("owner-secret");
              assertThat(ds.getConnectionProperties())
                  .containsEntry("sslmode", "verify-full")
                  .containsEntry("sslrootcert", "/etc/bibs/rds-ca/global-bundle.pem");
            });
  }

  @Test
  void aMigrationUrlTakesPrecedenceOverTheApplicationUrl() {
    MockEnvironment env =
        new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:postgresql://db:5432/bibs")
            .withProperty("spring.flyway.url", "jdbc:postgresql://db-admin:5432/bibs")
            .withProperty("spring.flyway.user", "bibs_owner");
    FluentConfiguration configuration = new FluentConfiguration();

    new MigrationOwnerConnection(env).customize(configuration);

    assertThat(((DriverManagerDataSource) configuration.getDataSource()).getUrl())
        .isEqualTo("jdbc:postgresql://db-admin:5432/bibs");
  }
}
