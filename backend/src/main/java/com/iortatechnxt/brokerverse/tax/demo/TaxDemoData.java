package com.iortatechnxt.brokerverse.tax.demo;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.payables.service.DemoActor;
import com.iortatechnxt.brokerverse.tax.domain.RemittanceFacts;
import com.iortatechnxt.brokerverse.tax.domain.ReturnStatus;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.TaxReturn;
import com.iortatechnxt.brokerverse.tax.service.CalendarEntry;
import com.iortatechnxt.brokerverse.tax.service.Certificate2307Service;
import com.iortatechnxt.brokerverse.tax.service.TaxCalendarService;
import com.iortatechnxt.brokerverse.tax.service.TaxReturnService;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * DEMO PROFILE ONLY: tax and statutory demo data of the demo company FVI, created through the
 * services after every other demo runner (the worksheets read their policies, invoices and
 * postings):
 *
 * <ul>
 *   <li>tax codes and ATCs, the filing calendar, tax profiles of the demo suppliers, agents,
 *       brokers and two customers, and the IC schedule mapping ({@link TaxDemoMasters});
 *   <li>every tracked return of Q1 and Q2 2026 prepared by "accountant", filed and paid by
 *       "checker" two days before its due date from bank account BDO-CA (remittance journals
 *       posted);
 *   <li>the Q3 2026 returns (July and August monthly returns and the quarterly returns) left in
 *       DRAFT, so the calendar shows due and overdue items;
 *   <li>a BIR Form 2307 batch for Q1 and for Q2.
 * </ul>
 *
 * Idempotent: each calendar entry is only brought forward to its demo state (a missing return is
 * prepared, a Q1-Q2 draft is filed and paid), and a quarter's 2307 batch is issued only when no
 * certificate exists for it.
 */
@Component
@Profile("demo")
@Order(95)
public class TaxDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(TaxDemoData.class);
  private static final String DEMO_COMPANY = "FVI";
  private static final int YEAR = 2026;
  private static final LocalDate DEMO_TODAY = LocalDate.of(YEAR, 9, 23);
  private static final LocalDate PAID_UNTIL = LocalDate.of(YEAR, 6, 30);
  private static final LocalDate DRAFT_UNTIL = LocalDate.of(YEAR, 9, 30);
  private static final int DAYS_BEFORE_DUE = 2;
  private static final String BANK = "BDO-CA";
  private static final int FIRST_QUARTER = 1;
  private static final int SECOND_QUARTER = 2;

  private final OrganizationService organization;
  private final TaxDemoMasters masters;
  private final TaxCalendarService calendar;
  private final TaxReturnService returns;
  private final Certificate2307Service certificates;
  private final DemoActor actor;

  /**
   * Creates the runner.
   *
   * @param organization companies
   * @param masters tax masters loader
   * @param calendar filing calendar
   * @param returns returns
   * @param certificates 2307 certificates
   * @param actor demo users
   */
  public TaxDemoData(
      OrganizationService organization,
      TaxDemoMasters masters,
      TaxCalendarService calendar,
      TaxReturnService returns,
      Certificate2307Service certificates,
      DemoActor actor) {
    this.organization = organization;
    this.masters = masters;
    this.calendar = calendar;
    this.returns = returns;
    this.certificates = certificates;
    this.actor = actor;
  }

  @Override
  public void run(ApplicationArguments args) {
    organization.listCompanies().stream()
        .filter(c -> DEMO_COMPANY.equals(c.getCode()))
        .findFirst()
        .map(Company::getId)
        .ifPresent(this::load);
  }

  /**
   * Loads the demo data of a company (idempotent).
   *
   * @param companyId company
   */
  public void load(Long companyId) {
    masters.ensure(companyId);
    int count = 0;
    for (CalendarEntry e : calendar.calendar(companyId, YEAR, DEMO_TODAY)) {
      if (e.tracked() && complete(companyId, e)) {
        count++;
      }
    }
    LOG.info("Tax demo data: {} returns created or completed", count);
    certify(companyId, FIRST_QUARTER);
    certify(companyId, SECOND_QUARTER);
  }

  /**
   * Brings one calendar entry to its demo state: Q1-Q2 PAID, Q3 (ended months and quarters) at
   * least DRAFT. Entries already in their state are left alone.
   */
  private boolean complete(Long companyId, CalendarEntry e) {
    Optional<ReturnStatus> target = target(e.period());
    if (target.isEmpty() || ReturnStatus.PAID.name().equals(e.returnStatus())) {
      return false;
    }
    boolean prepared = !CalendarEntry.NOT_PREPARED.equals(e.returnStatus());
    if (prepared && target.get() == ReturnStatus.DRAFT) {
      return false;
    }
    Long id =
        prepared
            ? e.returnId()
            : actor
                .as(
                    DemoActor.MAKER,
                    () -> returns.create(companyId, e.formCode(), e.period().from()))
                .getId();
    if (target.get() == ReturnStatus.PAID) {
      fileAndPay(id, e.dueDate().minusDays(DAYS_BEFORE_DUE));
    }
    return true;
  }

  /** Demo state of a period: PAID up to June, DRAFT for ended months and the quarters of Q3. */
  private static Optional<ReturnStatus> target(TaxPeriod period) {
    if (!period.to().isAfter(PAID_UNTIL)) {
      return Optional.of(ReturnStatus.PAID);
    }
    boolean ended = period.isQuarter() || period.to().isBefore(DEMO_TODAY);
    return !period.to().isAfter(DRAFT_UNTIL) && ended
        ? Optional.of(ReturnStatus.DRAFT)
        : Optional.empty();
  }

  private void fileAndPay(Long id, LocalDate day) {
    TaxReturn r = returns.get(id);
    if (r.getStatus() == ReturnStatus.DRAFT) {
      actor.as(DemoActor.CHECKER, () -> returns.file(id, day, "EFPS-" + r.getReturnNo()));
    }
    actor.as(
        DemoActor.CHECKER,
        () -> returns.pay(id, new RemittanceFacts(day, BANK, "PAY-" + r.getReturnNo())));
  }

  private void certify(Long companyId, int quarter) {
    LocalDate start = TaxPeriod.quarter(YEAR, quarter).from();
    boolean issued =
        certificates.register(companyId, YEAR).stream()
            .anyMatch(c -> c.getPeriodStart().equals(start));
    if (issued) {
      return;
    }
    try {
      actor.as(DemoActor.MAKER, () -> certificates.generate(companyId, YEAR, quarter));
    } catch (BusinessRuleException ex) {
      LOG.info("No 2307 certificates for {} Q{}: {}", YEAR, quarter, ex.getMessage());
    }
  }
}
