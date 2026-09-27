package com.iortatechnxt.brokerverse.migration.seed;

import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Loads the SIT/UAT storyline of the data migration through the real pipeline (seed profile only,
 * idempotent; DATA_MIGRATION_DESIGN section 24): the seed extracts of {@code db/seed/migration/}
 * for the seed company, after the Operations and booking seed runners.
 */
@Component
@Profile("seed")
@Order(95)
public class LegacyMigrationSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(LegacyMigrationSeedData.class);
  private static final String COMPANY = "FVI";

  private final MigrationStoryline.Services services;
  private final JdbcTemplate jdbc;
  private final SeedUsers users;
  private final CompanyRepository companies;

  /**
   * Creates the runner.
   *
   * @param services pipeline services
   * @param jdbc JDBC
   * @param users SIT/UAT sign-in
   * @param companies companies
   */
  public LegacyMigrationSeedData(
      MigrationStoryline.Services services,
      JdbcTemplate jdbc,
      SeedUsers users,
      CompanyRepository companies) {
    this.services = services;
    this.jdbc = jdbc;
    this.users = users;
    this.companies = companies;
  }

  @Override
  public void run(ApplicationArguments args) {
    companies
        .findByCode(COMPANY)
        .ifPresent(
            company -> {
              MigrationStoryline storyline = new MigrationStoryline(services, jdbc, users::as);
              if (!storyline.loaded(company.getId())) {
                LOG.info("Migration seed storyline loaded: {}", storyline.run(company.getId()));
              }
            });
  }
}
