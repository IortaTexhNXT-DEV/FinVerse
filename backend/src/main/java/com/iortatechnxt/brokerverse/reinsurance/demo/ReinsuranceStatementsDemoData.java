package com.iortatechnxt.brokerverse.reinsurance.demo;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.reinsurance.api.dto.SettlementRequest;
import com.iortatechnxt.brokerverse.reinsurance.api.dto.SoaRequest;
import com.iortatechnxt.brokerverse.reinsurance.domain.Soa;
import com.iortatechnxt.brokerverse.reinsurance.domain.Treaty;
import com.iortatechnxt.brokerverse.reinsurance.service.SoaService;
import com.iortatechnxt.brokerverse.reinsurance.service.TreatyService;
import com.iortatechnxt.brokerverse.underwriting.demo.DemoUserContext;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * DEMO PROFILE ONLY: Q1 and Q2 statements of account of every demo treaty, approved, and the Q1
 * fire quota share statement of the local reinsurer settled.
 *
 * <p>Runs after the treaty programme ({@link ReinsuranceDemoData}, {@code @Order(15)}) and the demo
 * claims ({@code @Order(20)}), so the statements carry premium as well as the reinsurers' share of
 * paid losses and outstanding reserves. Idempotent: nothing is created when the company has no
 * treaties or already has statements.
 */
@Component
@Profile("demo")
@Order(25)
public class ReinsuranceStatementsDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ReinsuranceStatementsDemoData.class);
  private static final String DEMO_COMPANY = "FVI";
  private static final String SETTLED_TREATY = "FIRE-QS-26";
  private static final String SETTLEMENT_BANK = "1111";
  private static final LocalDate SETTLEMENT_DATE = LocalDate.of(DemoTreaties.YEAR, 7, 31);
  private static final int STATEMENT_LAG_DAYS = 15;
  private static final int MONTHS_PER_QUARTER = 3;
  private static final List<Integer> QUARTERS = List.of(1, 2);

  private final OrganizationService organization;
  private final TreatyService treaties;
  private final SoaService statements;
  private final DemoUserContext users;

  /**
   * Creates the loader.
   *
   * @param organization companies
   * @param treaties treaties
   * @param statements statements of account
   * @param users demo user context
   */
  public ReinsuranceStatementsDemoData(
      OrganizationService organization,
      TreatyService treaties,
      SoaService statements,
      DemoUserContext users) {
    this.organization = organization;
    this.treaties = treaties;
    this.statements = statements;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    organization.listCompanies().stream()
        .filter(c -> DEMO_COMPANY.equals(c.getCode()))
        .findFirst()
        .map(Company::getId)
        .ifPresent(this::loadIfEmpty);
  }

  private void loadIfEmpty(Long companyId) {
    if (!treaties.list(companyId).isEmpty() && statements.list(companyId).isEmpty()) {
      LOG.info("Reinsurance demo statements created: {}", load(companyId));
    }
  }

  /**
   * Generates, approves and partly settles the statements of the demo-year treaties (no idempotency
   * check).
   *
   * @param companyId demo company
   * @return statements created
   */
  public int load(Long companyId) {
    int count = 0;
    List<Treaty> programme =
        treaties.list(companyId).stream().filter(t -> t.getUwYear() == DemoTreaties.YEAR).toList();
    for (Treaty t : programme) {
      for (int quarter : QUARTERS) {
        LocalDate quarterEnd =
            LocalDate.of(DemoTreaties.YEAR, quarter * MONTHS_PER_QUARTER, 1)
                .plusMonths(1)
                .minusDays(1);
        SoaRequest request =
            new SoaRequest(
                companyId,
                t.getCode(),
                null,
                DemoTreaties.YEAR,
                quarter,
                quarterEnd.plusDays(STATEMENT_LAG_DAYS));
        List<Soa> soas = users.runAs(ReinsuranceDemoData.MAKER, () -> statements.generate(request));
        for (Soa s : soas) {
          users.runAs(ReinsuranceDemoData.CHECKER, () -> statements.approve(s.getId()));
          count++;
        }
      }
    }
    settleOne(companyId);
    return count;
  }

  private void settleOne(Long companyId) {
    statements.list(companyId).stream()
        .filter(s -> SETTLED_TREATY.equals(s.getTreaty().getCode()))
        .filter(s -> DemoTreaties.LOCAL.equals(s.getParty().getCode()) && s.getQuarter() == 1)
        .filter(s -> s.getBalance().signum() != 0)
        .findFirst()
        .ifPresent(
            s ->
                users.runAs(
                    ReinsuranceDemoData.CHECKER,
                    () ->
                        statements.settle(
                            s.getId(), new SettlementRequest(SETTLEMENT_DATE, SETTLEMENT_BANK))));
  }
}
