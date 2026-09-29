package com.iortatechnxt.brokerverse.remittance.seed;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLineRepository;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun.Scope;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRunRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.service.BatchService;
import com.iortatechnxt.brokerverse.remittance.service.ExtractionService;
import com.iortatechnxt.brokerverse.remittance.service.InsurerOrUploads;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seed storyline of remittance (seed profile only, idempotent), after cashiering has paid the seed
 * invoices (order 91), each step signed in as the team member who does it:
 *
 * <ul>
 *   <li>the processor ({@code remit}) extracts the paid booking invoice of ARN-2026-940001 and
 *       submits its batch, the team leader ({@code remittl}) approves it (four eyes), Disbursement
 *       ({@code disb}) acknowledges the payment request and assigns the DV, and the insurer's OR
 *       schedule is uploaded: the batch is remitted with its OR;
 * </ul>
 *
 * A step that fails is logged and skipped. The manual extraction runs after the adjustment seed, in
 * {@link ExtractionSeedData}; the seeded hold request is made last, by {@link HoldSeedData}.
 */
@Component
@Profile("seed")
@Order(92)
public class RemittanceSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(RemittanceSeedData.class);
  private static final String REMITTED_ARN = "ARN-2026-940001";
  private static final String PROCESSOR = "remit";

  private final ExtractionRunRepository runs;
  private final ExtractionService extraction;
  private final BatchService batches;
  private final BatchLineRepository lines;
  private final DisbursementQueueService disbursements;
  private final InsurerOrUploads insurerOrs;
  private final InvoiceLedgerQueryService ledger;
  private final Clock clock;
  private final SeedUsers users;

  /**
   * Creates the loader.
   *
   * @param runs extraction runs (idempotency)
   * @param extraction extraction
   * @param batches batches
   * @param lines batch lines
   * @param disbursements Disbursement queue
   * @param insurerOrs insurer OR uploads
   * @param ledger ledger reads
   * @param clock clock
   * @param users seed sign-in
   */
  public RemittanceSeedData(
      ExtractionRunRepository runs,
      ExtractionService extraction,
      BatchService batches,
      BatchLineRepository lines,
      DisbursementQueueService disbursements,
      InsurerOrUploads insurerOrs,
      InvoiceLedgerQueryService ledger,
      Clock clock,
      SeedUsers users) {
    this.runs = runs;
    this.extraction = extraction;
    this.batches = batches;
    this.lines = lines;
    this.disbursements = disbursements;
    this.insurerOrs = insurerOrs;
    this.ledger = ledger;
    this.clock = clock;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    List<OpsInvoice> remitted = ledger.forArn(REMITTED_ARN);
    if (runs.count() > 0 || remitted.isEmpty()) {
      return;
    }
    original(REMITTED_ARN).ifPresent(i -> step("remitted batch", () -> remit(i)));
  }

  private void remit(OpsInvoice invoice) {
    String invoiceNo = invoice.getInvoiceNo();
    users.as(
        PROCESSOR,
        () ->
            extraction.run(
                invoice.getCompanyId(),
                new Scope(ExtractionTrigger.MANUAL_INVOICE, null, null, invoiceNo),
                today()));
    List<BatchLine> extracted = lines.findByInvoiceNo(invoiceNo);
    if (extracted.isEmpty()) {
      LOG.warn("Remittance seed: {} was not extracted", invoiceNo);
      return;
    }
    Long batchId = extracted.get(0).getBatch().getId();
    users.as(PROCESSOR, () -> batches.submit(batchId, "Amounts checked against the ledger"));
    RemittanceBatch approved = users.as("remittl", () -> batches.approve(batchId, "Approved"));
    DisbursementRequest request =
        disbursements.find("REMITTANCE", approved.getBatchNo()).orElseThrow();
    users.as("disb", () -> disbursements.acknowledge(request.getId()));
    users.as("disb", () -> disbursements.assignDv(request.getId(), "DV-2026-900001"));
    BatchLine line = users.as(PROCESSOR, () -> batches.get(batchId)).line(invoiceNo);
    String schedule =
        "batchNo,invoiceNo,orNo,orDate,orAmount\n"
            + String.join(
                ",",
                approved.getBatchNo(),
                invoiceNo,
                "MGIC-OR-2026-900001",
                today().toString(),
                line.getAmounts().paidAr().toPlainString())
            + "\n";
    users.as(
        PROCESSOR,
        () ->
            insurerOrs.upload(
                new FlowInFile(
                    "MGIC_OR_" + approved.getBatchNo() + ".csv",
                    schedule.getBytes(StandardCharsets.UTF_8))));
    LOG.info("Remittance seed: batch {} remitted with its insurer OR", approved.getBatchNo());
  }

  private Optional<OpsInvoice> original(String arn) {
    return ledger.forArn(arn).stream()
        .filter(i -> i.getEndorsementNo() == null && i.premiumBalance().signum() == 0)
        .findFirst();
  }

  private LocalDate today() {
    return BusinessClock.today(clock);
  }

  private static void step(String name, Runnable work) {
    try {
      work.run();
    } catch (RuntimeException ex) {
      LOG.warn("Remittance seed {} skipped: {}", name, ex.getMessage());
    }
  }
}
