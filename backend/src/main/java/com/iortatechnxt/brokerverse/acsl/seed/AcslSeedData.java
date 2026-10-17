package com.iortatechnxt.brokerverse.acsl.seed;

import com.iortatechnxt.brokerverse.acsl.domain.GlSlRun;
import com.iortatechnxt.brokerverse.acsl.service.GlSlReconciliationService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * SIT/UAT seed data of ACSL: the GL-SL reconciliation of the seed date, run by the ACSL Team Leader
 * after the other seed data posted their journals (as the daily job would in the evening), so that
 * the reconciliation screen and report show the control accounts of the seed story. Idempotent: no
 * run when the company already has one.
 */
@Component
@Profile("seed")
@Order(900)
public class AcslSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(AcslSeedData.class);
  private static final String TEAM_LEADER = "acsltl";

  private final CompanyRepository companies;
  private final GlSlReconciliationService reconciliation;
  private final SeedUsers users;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param companies companies
   * @param reconciliation GL-SL reconciliation
   * @param users seed sign-in
   * @param clock clock
   */
  public AcslSeedData(
      CompanyRepository companies,
      GlSlReconciliationService reconciliation,
      SeedUsers users,
      Clock clock) {
    this.companies = companies;
    this.reconciliation = reconciliation;
    this.users = users;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    LocalDate today = BusinessClock.today(clock);
    for (Company company : companies.findAll()) {
      if (!reconciliation.runs(company.getId()).isEmpty()) {
        continue;
      }
      try {
        GlSlRun run = users.as(TEAM_LEADER, () -> reconciliation.run(company.getId(), today));
        LOG.info(
            "Seed GL-SL reconciliation of {}: {} control accounts, {} with a difference",
            company.getCode(),
            run.getAccounts(),
            run.getDifferences());
      } catch (RuntimeException e) {
        LOG.warn("Seed GL-SL reconciliation of {} skipped: {}", company.getCode(), e.getMessage());
      }
    }
  }
}
