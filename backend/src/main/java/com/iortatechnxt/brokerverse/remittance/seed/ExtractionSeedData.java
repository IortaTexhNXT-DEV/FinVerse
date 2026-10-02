package com.iortatechnxt.brokerverse.remittance.seed;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun.Scope;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRunRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.service.ExtractionService;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The seed's manual extraction (seed profile only, idempotent), run by the processor ({@code
 * remit}) after the adjustment seed (order 93): the paid endorsement invoice of ARN-2026-940001
 * goes into a batch waiting for review and every other invoice gets its tag, while the invoice
 * under the seeded flat cancellation stays out of it, locked by Adjustment.
 */
@Component
@Profile("seed")
@Order(94)
public class ExtractionSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ExtractionSeedData.class);
  private static final String REMITTED_ARN = "ARN-2026-940001";
  private static final String PROCESSOR = "remit";

  private final ExtractionRunRepository runs;
  private final ExtractionService extraction;
  private final InvoiceLedgerQueryService ledger;
  private final Clock clock;
  private final SeedUsers users;

  /**
   * Creates the loader.
   *
   * @param runs extraction runs (idempotency)
   * @param extraction extraction
   * @param ledger ledger reads
   * @param clock clock
   * @param users seed sign-in
   */
  public ExtractionSeedData(
      ExtractionRunRepository runs,
      ExtractionService extraction,
      InvoiceLedgerQueryService ledger,
      Clock clock,
      SeedUsers users) {
    this.runs = runs;
    this.extraction = extraction;
    this.ledger = ledger;
    this.clock = clock;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    List<OpsInvoice> remitted = ledger.forArn(REMITTED_ARN);
    boolean done =
        runs.findAll().stream().anyMatch(r -> r.getTrigger() == ExtractionTrigger.MANUAL);
    if (remitted.isEmpty() || done) {
      return;
    }
    try {
      ExtractionRun run =
          users.as(
              PROCESSOR,
              () ->
                  extraction.run(
                      remitted.get(0).getCompanyId(),
                      new Scope(ExtractionTrigger.MANUAL, null, null, null),
                      BusinessClock.today(clock)));
      LOG.info("Remittance seed extraction {} - {}", run.getRunNo(), run.getMessage());
    } catch (RuntimeException ex) {
      LOG.warn("Seed manual extraction skipped: {}", ex.getMessage());
    }
  }
}
