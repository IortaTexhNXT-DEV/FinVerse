package com.iortatechnxt.brokerverse.remittance.demo;

import com.iortatechnxt.brokerverse.opsledger.demo.DemoUsers;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction.Terms;
import com.iortatechnxt.brokerverse.remittance.service.DeductionService;
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
 * Demo remittance deduction (ACSL 2.9.2; demo profile only, idempotent), after the remittance and
 * Disbursement storylines: the ACSL processor ({@code acsl}) records an AR insurer's refund that
 * INS-LAC confirmed in writing and submits it; it waits for the ACSL team leader ({@code acsltl})
 * to confirm it, after which the next INS-LAC batch deducts it. A failure is logged and skipped.
 */
@Component
@Profile("demo")
@Order(98)
public class DeductionDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(DeductionDemoData.class);
  private static final String ARN = "ARN-2026-940001";
  private static final String PREPARER = "acsl";
  private static final BigDecimal AMOUNT = new BigDecimal("1250.00");
  private static final int CONFIRMED_DAYS_AGO = 3;

  private final DeductionService deductions;
  private final InvoiceLedgerQueryService ledger;
  private final DemoUsers users;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param deductions deductions
   * @param ledger ledger (company of the demo invoices)
   * @param users demo sign-in
   * @param clock clock
   */
  public DeductionDemoData(
      DeductionService deductions, InvoiceLedgerQueryService ledger, DemoUsers users, Clock clock) {
    this.deductions = deductions;
    this.ledger = ledger;
    this.users = users;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    List<OpsInvoice> invoices = ledger.forArn(ARN);
    if (invoices.isEmpty()) {
      return;
    }
    Long companyId = invoices.get(0).getCompanyId();
    try {
      boolean exists =
          users
              .as(
                  PREPARER,
                  () -> deductions.search(companyId, List.of(), null, null, PageRequest.of(0, 1)))
              .hasContent();
      if (!exists) {
        seed(companyId);
      }
    } catch (RuntimeException ex) {
      LOG.warn("Remittance deduction demo skipped: {}", ex.getMessage());
    }
  }

  private void seed(Long companyId) {
    LocalDate today = LocalDate.now(clock);
    RemittanceDeduction draft =
        users.as(
            PREPARER,
            () ->
                deductions.create(
                    companyId,
                    new Terms(
                        "INS-LAC",
                        "PHP",
                        "AR_INSURER_REFUND",
                        "ARI-DEMO-0001",
                        null,
                        AMOUNT,
                        "LAC-CONF-DEMO-0001",
                        today.minusDays(CONFIRMED_DAYS_AGO),
                        "Demo: return premium already remitted; INS-LAC confirmed the refund by"
                            + " letter")));
    users.as(
        PREPARER, () -> deductions.submit(draft.getId(), "Insurer confirmation letter attached"));
    LOG.info("Remittance demo: deduction {} waiting for confirmation", draft.getDeductionNo());
  }
}
