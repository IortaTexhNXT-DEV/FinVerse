package com.iortatechnxt.brokerverse.remittance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun.Scope;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTagRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceAmounts;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RunStatus;
import com.iortatechnxt.brokerverse.remittance.service.BatchService;
import com.iortatechnxt.brokerverse.remittance.service.ExtractionService;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceExtractionJob;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Extraction, exclusion and restore, submission, four-eyes approval with posting, ledger movements
 * and the Disbursement queue, DV assignment and return (RMTID.001-019/029/036/040).
 */
@IntegrationTest
class RemittanceIT {

  private static final String REMIT = "remit";
  private static final String REMITTL = "remittl";

  @Autowired private RemittanceFixtures fx;
  @Autowired private ExtractionService extraction;
  @Autowired private RemittanceExtractionJob job;
  @Autowired private BatchService batches;
  @Autowired private InvoiceLedgerQueryService ledger;
  @Autowired private DisbursementQueueService queue;
  @Autowired private InvoiceTagRepository tags;
  @Autowired private AsUser as;
  @Autowired private TransactionTemplate tx;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void theScheduledExtractionTagsInvoicesAndBatchesTheEligibleOnes() {
    OpsInvoice paid = fx.paidInvoice();
    OpsInvoice holding = fx.invoice();
    fx.payInFull(holding, LocalDate.now());
    OpsInvoice unpaid = fx.invoice();

    job.execute(LocalDate.now());

    BatchLine line = fx.lineOf(paid.getInvoiceNo());
    assertThat(line).isNotNull();
    RemittanceBatch batch = fx.batchOf(paid.getInvoiceNo());
    assertThat(batch.getRemittanceType()).isEqualTo(RemittanceType.NORMAL_PHP);
    assertThat(batch.getStage()).isEqualTo(BatchStage.REVIEW_IN_PROCESS);
    assertThat(batch.getBatchNo()).startsWith("RMB-INS-MGIC-");
    assertThat(batch.getExtractFileId()).isNotNull();
    RemittanceAmounts a = line.getAmounts();
    assertThat(a.paidAr()).isEqualByComparingTo(paid.getGrossPremium());
    assertThat(a.commission()).isEqualByComparingTo(paid.getCommission());
    assertThat(a.commissionVat()).isEqualByComparingTo(paid.getVatOnCommission());
    assertThat(a.netDue())
        .isEqualByComparingTo(
            a.paidAr().add(a.wtax()).subtract(a.commission()).subtract(a.commissionVat()));
    OpsInvoice extracted = fx.reload(paid.getInvoiceNo());
    assertThat(extracted.getRemittanceStatus()).isEqualTo(RemittanceStatus.REVIEW_IN_PROCESS);
    assertThat(extracted.getLockOwner()).isEqualTo("REMITTANCE");

    assertThat(tags.findFirstByInvoiceNoOrderByIdDesc(holding.getInvoiceNo()))
        .hasValueSatisfying(
            t -> {
              assertThat(t.getTag()).isEqualTo(ExtractionTag.UNEXTRACTED_DUE);
              assertThat(t.getReasons()).contains("CHECK_HOLDING");
            });
    assertThat(fx.lineOf(holding.getInvoiceNo())).isNull();
    assertThat(fx.lineOf(unpaid.getInvoiceNo())).isNull();
    assertThat(fx.reload(unpaid.getInvoiceNo()).getLockOwner()).isNull();
  }

  @Test
  void aManualInvoiceExtractionRefusesInvoicesThatCannotBeRemitted() {
    OpsInvoice unpaid = fx.invoice();
    ExtractionRun run = fx.extract(unpaid.getInvoiceNo());
    assertThat(run.getStatus()).isEqualTo(RunStatus.SUCCEEDED);
    assertThat(run.getNotDueCount()).isEqualTo(1);
    assertThat(run.getBatchCount()).isZero();
    assertThat(tags.findFirstByInvoiceNoOrderByIdDesc(unpaid.getInvoiceNo()))
        .hasValueSatisfying(
            t -> assertThat(t.getTag()).isEqualTo(ExtractionTag.UNEXTRACTED_NOT_DUE));

    OpsInvoice paid = fx.paidInvoice();
    fx.extract(paid.getInvoiceNo());
    assertThatThrownBy(() -> fx.extract(paid.getInvoiceNo()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("REVIEW_IN_PROCESS");
    assertThatThrownBy(
            () ->
                as.run(
                    REMIT,
                    () ->
                        extraction.run(
                            fx.company(),
                            new Scope(
                                ExtractionTrigger.MANUAL_INVOICE,
                                "INS-LAC",
                                null,
                                fx.paidInvoice().getInvoiceNo()),
                            LocalDate.now())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("INS-LAC");
  }

  @Test
  void excludedInvoicesAreGivenBackAndCanBeRestoredBeforeSubmission() {
    OpsInvoice paid = fx.paidInvoice();
    fx.extract(paid.getInvoiceNo());
    RemittanceBatch batch = fx.batchOf(paid.getInvoiceNo());
    Long id = batch.getId();

    RemittanceBatch excluded =
        as.run(
            REMIT,
            () -> batches.exclude(id, List.of(paid.getInvoiceNo()), "OTHERS", "Client disputes"));
    assertThat(excluded.getLineCount()).isZero();
    assertThat(excluded.getTotals().paidAr()).isZero();
    BatchLine line = excluded.line(paid.getInvoiceNo());
    assertThat(line.isExcluded()).isTrue();
    assertThat(line.getExcludedBy()).isEqualTo(REMIT);
    assertThat(line.getAmounts().paidAr()).isEqualByComparingTo(paid.getGrossPremium());
    OpsInvoice released = fx.reload(paid.getInvoiceNo());
    assertThat(released.getRemittanceStatus()).isEqualTo(RemittanceStatus.UNPROCESSED);
    assertThat(released.getLockOwner()).isNull();
    assertThatThrownBy(() -> as.run(REMIT, () -> batches.submit(id, null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("no invoice left");
    assertThatThrownBy(
            () ->
                as.run(
                    REMIT, () -> batches.exclude(id, List.of(paid.getInvoiceNo()), "NOPE", null)))
        .isInstanceOf(RuntimeException.class);

    RemittanceBatch restored = as.run(REMIT, () -> batches.restore(id, paid.getInvoiceNo()));
    assertThat(restored.getLineCount()).isEqualTo(1);
    assertThat(restored.line(paid.getInvoiceNo()).getRestoredBy()).isEqualTo(REMIT);
    assertThat(fx.reload(paid.getInvoiceNo()).getLockOwner()).isEqualTo("REMITTANCE");
    assertThat(as.run(REMIT, () -> batches.preview(id)).problems()).isEmpty();
  }

  @Test
  void anApprovedBatchIsPostedRemittedAndPushedToDisbursement() {
    OpsInvoice paid = fx.paidInvoice();
    fx.extract(paid.getInvoiceNo());
    Long id = fx.batchOf(paid.getInvoiceNo()).getId();
    as.run(REMIT, () -> batches.submit(id, "Ready"));
    assertThatThrownBy(() -> as.run(REMIT, () -> batches.submit(id, "Again")))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> as.run(REMIT, () -> batches.approve(id, null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("another user");

    RemittanceBatch approved = as.run(REMITTL, () -> batches.approve(id, "OK"));
    assertThat(approved.getStage()).isEqualTo(BatchStage.APPROVED);
    assertThat(approved.getApprovedBy()).isEqualTo(REMITTL);
    assertThat(approved.getDisbursementRequestNo()).startsWith("DSQ-");
    assertThat(approved.getCommissionOrStatus()).isEqualTo("ISSUED");
    BatchLine line = approved.line(paid.getInvoiceNo());
    assertThat(line.getJournalBatchNo()).isNotBlank();

    OpsInvoice remitted = fx.reload(paid.getInvoiceNo());
    assertThat(remitted.getRemittanceStatus()).isEqualTo(RemittanceStatus.APPROVED);
    assertThat(remitted.component(LedgerComponent.DTIP).getBalance()).isZero();
    assertThat(remitted.component(LedgerComponent.COMMISSION).getBalance()).isZero();
    assertThat(ledger.movements(paid.getInvoiceNo()))
        .filteredOn(m -> m.getMovementType() == MovementType.REMITTED)
        .extracting(m -> m.getBatchNo())
        .containsOnly(approved.getBatchNo());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from acc_event_log where source_reference = ?",
                Integer.class,
                "RMB:" + approved.getBatchNo() + ":" + paid.getInvoiceNo()))
        .isEqualTo(1);
    DisbursementRequest request = queue.find("REMITTANCE", approved.getBatchNo()).orElseThrow();
    assertThat(request.getRequestType()).isEqualTo(DisbursementRequest.Type.REMITTANCE);
    assertThat(request.getAmount()).isEqualByComparingTo(approved.getTotals().payable());
    assertThat(request.getPayeeCode()).isEqualTo("INS-MGIC");

    as.run(
        "disb",
        () -> tx.execute(s -> queue.assignDv(request.getId(), "DV-" + BookingFixtures.token())));
    RemittanceBatch done = fx.batchOf(paid.getInvoiceNo());
    assertThat(done.getStage()).isEqualTo(BatchStage.FULLY_REMITTED);
    assertThat(done.getDvNo()).startsWith("DV-");
    OpsInvoice full = fx.reload(paid.getInvoiceNo());
    assertThat(full.getRemittanceStatus()).isEqualTo(RemittanceStatus.FULLY_REMITTED);
    assertThat(full.getLockOwner()).isNull();
  }

  @Test
  void aPartlyPaidInvoiceIsPartiallyRemittedAndExtractedAgainLater() {
    OpsInvoice invoice = fx.invoice();
    BigDecimal half =
        invoice
            .component(LedgerComponent.BASIC)
            .getBooked()
            .divide(BigDecimal.TWO, 2, java.math.RoundingMode.HALF_UP);
    fx.payBasic(invoice, half, RemittanceFixtures.PAID_ON);
    RemittanceBatch approved = fx.approvedBatch(fx.reload(invoice.getInvoiceNo()));
    BatchLine line = approved.line(invoice.getInvoiceNo());
    assertThat(line.getAmounts().paidAr()).isEqualByComparingTo(half);
    assertThat(line.getAmounts().commission()).isPositive().isLessThan(invoice.getCommission());

    DisbursementRequest request = queue.find("REMITTANCE", approved.getBatchNo()).orElseThrow();
    as.run(
        "disb",
        () -> tx.execute(s -> queue.assignDv(request.getId(), "DV-P-" + BookingFixtures.token())));
    assertThat(fx.batchOf(invoice.getInvoiceNo()).getStage())
        .isEqualTo(BatchStage.PARTIALLY_REMITTED);
    OpsInvoice partial = fx.reload(invoice.getInvoiceNo());
    assertThat(partial.getRemittanceStatus()).isEqualTo(RemittanceStatus.PARTIALLY_REMITTED);

    fx.payInFull(partial, RemittanceFixtures.PAID_ON);
    fx.extract(invoice.getInvoiceNo());
    BatchLine second = fx.lineOf(invoice.getInvoiceNo());
    assertThat(second.getBatch().getId()).isNotEqualTo(approved.getId());
    assertThat(second.getAmounts().paidAr().add(half))
        .isEqualByComparingTo(invoice.getGrossPremium());
    assertThat(second.getAmounts().commission().add(line.getAmounts().commission()))
        .isEqualByComparingTo(invoice.getCommission());
  }

  @Test
  void aReturnedBatchGivesItsInvoicesBackTaggedReturned() {
    OpsInvoice paid = fx.paidInvoice();
    fx.extract(paid.getInvoiceNo());
    Long id = fx.batchOf(paid.getInvoiceNo()).getId();
    assertThatThrownBy(() -> as.run(REMIT, () -> batches.returnBatch(id, "NOT_A_REASON", null)))
        .isInstanceOf(RuntimeException.class);
    RemittanceBatch returned = as.run(REMIT, () -> batches.returnBatch(id, "OTHERS", "Wrong type"));
    assertThat(returned.getStage()).isEqualTo(BatchStage.RETURNED);
    assertThat(returned.getReturnReason()).startsWith("OTHERS");
    assertThat(tags.findFirstByInvoiceNoOrderByIdDesc(paid.getInvoiceNo()))
        .hasValueSatisfying(t -> assertThat(t.getTag()).isEqualTo(ExtractionTag.RETURNED));
    OpsInvoice back = fx.reload(paid.getInvoiceNo());
    assertThat(back.getRemittanceStatus()).isEqualTo(RemittanceStatus.UNPROCESSED);
    assertThat(back.getLockOwner()).isNull();

    fx.extract(paid.getInvoiceNo());
    assertThat(fx.batchOf(paid.getInvoiceNo()).getId()).isNotEqualTo(id);
  }

  @Test
  void aBatchIsReassignedToAnotherProcessor() {
    OpsInvoice paid = fx.paidInvoice();
    fx.extract(paid.getInvoiceNo());
    Long id = fx.batchOf(paid.getInvoiceNo()).getId();
    assertThat(as.run(REMITTL, () -> batches.assign(id, REMITTL)).getProcessor())
        .isEqualTo(REMITTL);
    assertThatThrownBy(() -> as.run(REMITTL, () -> batches.assign(id, "ao")))
        .isInstanceOf(BusinessRuleException.class);
  }
}
