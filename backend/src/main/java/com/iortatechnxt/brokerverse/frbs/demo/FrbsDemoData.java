package com.iortatechnxt.brokerverse.frbs.demo;

import com.iortatechnxt.brokerverse.accounting.domain.CostCenterRuleValues;
import com.iortatechnxt.brokerverse.accounting.service.CostCenterRuleService;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRun;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeBase;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeQueryService;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeRunService;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFees;
import com.iortatechnxt.brokerverse.opsledger.demo.DemoUsers;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificate.Facts;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateLine;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateLine.Kind;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.service.ReceivedCertificateService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Demo storyline of the accounting report pack and service fee (demo profile only, idempotent; V999
 * is full, design section 17), after Disbursement (order 98), each step signed in as the team
 * member who does it:
 *
 * <ul>
 *   <li>the GL team lead ({@code gltl}) adds the cost-centre rule of the service-fee accrual
 *       (account 5614 requires a cost centre, FRBS 3.1.1);
 *   <li>the GL officer ({@code glofficer}) computes the service fee of the invoices fully paid this
 *       month and submits it; the team lead approves it: every line is accrued and sent to
 *       Disbursement;
 *   <li>the Disbursement processor ({@code disb}) records an insurer's BIR 2307 received for the
 *       quarter (DIS 2.11), which feeds the SAWT.
 * </ul>
 *
 * A step that fails is logged and skipped.
 */
@Component
@Profile("demo")
@Order(98)
public class FrbsDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(FrbsDemoData.class);
  private static final String OFFICER = "glofficer";
  private static final String LEAD = "gltl";
  private static final String COST_CENTRE = "NB-CBG-M";

  private final CompanyRepository companies;
  private final CostCenterRuleService costCenters;
  private final ServiceFeeRunService runs;
  private final ServiceFeeQueryService queries;
  private final ReceivedCertificateService certificates;
  private final DemoUsers users;
  private final Clock clock;

  /**
   * Creates the runner.
   *
   * @param companies companies
   * @param costCenters cost-centre rules
   * @param runs service-fee runs
   * @param queries service-fee reads
   * @param certificates received certificates
   * @param users demo sign-in
   * @param clock clock
   */
  public FrbsDemoData(
      CompanyRepository companies,
      CostCenterRuleService costCenters,
      ServiceFeeRunService runs,
      ServiceFeeQueryService queries,
      ReceivedCertificateService certificates,
      DemoUsers users,
      Clock clock) {
    this.companies = companies;
    this.costCenters = costCenters;
    this.runs = runs;
    this.queries = queries;
    this.certificates = certificates;
    this.users = users;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    companies.findByCode("FVI").map(Company::getId).ifPresent(this::seed);
  }

  private void seed(Long companyId) {
    if (queries.search(companyId, null, null, PageRequest.of(0, 1)).getTotalElements() > 0) {
      return;
    }
    step("cost-centre rule", () -> users.run(LEAD, () -> costCentreRule(companyId)));
    step("service-fee run", () -> serviceFee(companyId));
    step("received certificate", () -> users.run("disb", () -> certificate(companyId)));
  }

  private void costCentreRule(Long companyId) {
    boolean present =
        costCenters.list(companyId).stream()
            .anyMatch(r -> ServiceFees.ACCRUAL_EVENT.equals(r.getEventType()));
    if (!present) {
      costCenters.create(
          companyId,
          new CostCenterRuleValues(
              9000,
              ServiceFees.MODULE,
              ServiceFees.ACCRUAL_EVENT,
              null,
              null,
              null,
              COST_CENTRE,
              "Service fee accrual charged to the referring sales team (demo, AQ26)",
              true));
    }
  }

  private void serviceFee(Long companyId) {
    LocalDate today = LocalDate.now(clock.withZone(ServiceFeeBase.MANILA));
    ServiceFeeRun run =
        users.as(OFFICER, () -> runs.compute(companyId, today.withDayOfMonth(1), today));
    users.run(OFFICER, () -> runs.submit(run.getId(), "Service fee of the month"));
    users.run(LEAD, () -> runs.approve(run.getId(), "Approved for payment"));
  }

  private void certificate(Long companyId) {
    LocalDate today = LocalDate.now(clock);
    TaxPeriod quarter = TaxPeriod.quarterOf(today);
    certificates.record(
        companyId,
        new Facts(
            "2307-DEMO-" + today.getYear() + "Q" + TaxPeriod.quarterNumber(today),
            "INS-MAPFRE",
            "Demo Insurer (withholding agent)",
            "000-111-222-000",
            quarter.from(),
            today,
            today,
            "DISBURSEMENT",
            null,
            "Demo certificate on commission and incentives"),
        List.of(
            new ReceivedCertificateLine(
                1,
                Kind.COMMISSION,
                "WC158",
                "Commission",
                new BigDecimal("50000.00"),
                new BigDecimal("1000.00")),
            new ReceivedCertificateLine(
                2,
                Kind.INCENTIVE,
                "WC158",
                "Incentive",
                new BigDecimal("10000.00"),
                new BigDecimal("200.00"))));
  }

  private static void step(String name, Runnable work) {
    try {
      work.run();
    } catch (RuntimeException ex) {
      LOG.warn("FRBS demo step '{}' skipped: {}", name, ex.getMessage());
    }
  }
}
