package com.iortatechnxt.brokerverse.remittance.demo;

import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun.Scope;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRunRepository;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest.Terms;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import com.iortatechnxt.brokerverse.remittance.service.ExtractionService;
import com.iortatechnxt.brokerverse.remittance.service.HoldService;
import java.time.Clock;
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
 * Demo start-up of remittance (demo profile only, idempotent), on the invoices of the booked demo
 * accounts copied into the ledger (after {@code OpsLedgerDemoReplay}, order 90): a Marketing hold
 * request on the CGL invoice of ARN-2026-940004 waiting for approval, and a first manual extraction
 * run so the Extraction workbench shows its tags. Batches appear once payments are applied to the
 * demo invoices (cashiering), with the next extraction.
 */
@Component
@Profile("demo")
@Order(95)
public class RemittanceDemoData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(RemittanceDemoData.class);
  private static final String HELD_ARN = "ARN-2026-940004";
  private static final int HOLD_DAYS = 30;

  private final ExtractionRunRepository runs;
  private final ExtractionService extraction;
  private final HoldService holds;
  private final InvoiceLedgerQueryService ledger;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param runs extraction runs (idempotency)
   * @param extraction extraction
   * @param holds hold requests
   * @param ledger ledger reads
   * @param clock clock
   */
  public RemittanceDemoData(
      ExtractionRunRepository runs,
      ExtractionService extraction,
      HoldService holds,
      InvoiceLedgerQueryService ledger,
      Clock clock) {
    this.runs = runs;
    this.extraction = extraction;
    this.holds = holds;
    this.ledger = ledger;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    List<OpsInvoice> invoices = ledger.forArn(HELD_ARN);
    if (runs.count() == 0 && !invoices.isEmpty()) {
      seed(invoices.get(0));
    }
  }

  private void seed(OpsInvoice invoice) {
    LocalDate today = LocalDate.now(clock);
    try {
      holds.create(
          invoice.getCompanyId(),
          invoice.getInvoiceNo(),
          new Terms(
              "OTHERS",
              "Demo: client disputes the premium; hold until Marketing confirms (OQ24)",
              today.plusDays(HOLD_DAYS)),
          true,
          RequestSource.SCREEN);
    } catch (RuntimeException ex) {
      LOG.warn("Remittance demo hold skipped: {}", ex.getMessage());
    }
    ExtractionRun run =
        extraction.run(
            invoice.getCompanyId(), new Scope(ExtractionTrigger.MANUAL, null, null, null), today);
    LOG.info("Remittance demo data: extraction {} - {}", run.getRunNo(), run.getMessage());
  }
}
