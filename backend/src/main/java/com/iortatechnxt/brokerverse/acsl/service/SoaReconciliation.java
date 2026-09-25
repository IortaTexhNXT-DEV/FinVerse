package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.BookFigures;
import com.iortatechnxt.brokerverse.acsl.domain.ReconBucket;
import com.iortatechnxt.brokerverse.acsl.domain.ReconResult;
import com.iortatechnxt.brokerverse.acsl.domain.ReconResultRepository;
import com.iortatechnxt.brokerverse.acsl.domain.ReconRun;
import com.iortatechnxt.brokerverse.acsl.domain.ReconRunRepository;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLine;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLineRepository;
import com.iortatechnxt.brokerverse.acsl.domain.SoaUpload;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Automatic reconciliation of an insurer SOA against the books (ACSL 2.13.0-2.14.1): every loaded
 * line is matched to the invoice ledger by the invoice number and put in one bucket (Outstanding,
 * For remittance, Remitted, Cancelled, Direct billed, Not found) with BDOI's figures (premium,
 * outstanding, for remittance, remitted with batch and date, 2307 with batch and date, the
 * cancellation of the family) and the variances against the insurer's premium and balance.
 */
@Service
@Transactional
public class SoaReconciliation {

  private final SoaLineRepository lines;
  private final ReconRunRepository runs;
  private final ReconResultRepository results;
  private final InvoiceLedgerQueryService ledger;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param lines SOA lines
   * @param runs runs
   * @param results results
   * @param ledger Operations invoice ledger
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public SoaReconciliation(
      SoaLineRepository lines,
      ReconRunRepository runs,
      ReconResultRepository results,
      InvoiceLedgerQueryService ledger,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.lines = lines;
    this.runs = runs;
    this.results = results;
    this.ledger = ledger;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Reconciles an upload's loaded lines (a new run; earlier runs are kept).
   *
   * @param upload upload
   * @return the run
   */
  public ReconRun run(SoaUpload upload) {
    int runNo = (int) runs.countByUploadId(upload.getId()) + 1;
    ReconRun run =
        runs.save(new ReconRun(upload.getId(), runNo, clock.instant(), currentUser.username()));
    Map<ReconBucket, Integer> counts = new EnumMap<>(ReconBucket.class);
    int variances = 0;
    for (SoaLine line : lines.findByUploadIdAndStatusOrderByRowNo(upload.getId(), SoaLine.LOADED)) {
      ReconResult result = results.save(reconcile(run.getId(), line));
      counts.merge(result.getBucket(), 1, Integer::sum);
      if (result.hasVariance()) {
        variances++;
      }
    }
    run.counted(counts, variances);
    upload.reconciled(run.getId());
    audit.record(
        Acsl.SOA_ENTITY,
        upload.getUploadNo(),
        AuditAction.RUN,
        "Reconciliation " + runNo + ": " + counts);
    return run;
  }

  private ReconResult reconcile(Long runId, SoaLine line) {
    Optional<OpsInvoice> found =
        line.getInvoiceNo() == null ? Optional.empty() : ledger.find(line.getInvoiceNo());
    if (found.isEmpty()) {
      return new ReconResult(runId, line, ReconBucket.NOT_FOUND, null, BookFigures.NONE);
    }
    OpsInvoice invoice = found.get();
    BookFigures book = figures(invoice);
    return new ReconResult(runId, line, bucket(invoice, book), invoice.getRootInvoiceNo(), book);
  }

  private BookFigures figures(OpsInvoice invoice) {
    List<OpsInvoiceMovement> movements = ledger.movements(invoice.getInvoiceNo());
    Optional<OpsInvoiceMovement> remittance = latest(movements, MovementType.REMITTED);
    Optional<OpsInvoiceMovement> cwt = latest(movements, MovementType.CWT_RECLASS);
    Optional<OpsInvoiceComponent> dtip = component(invoice, LedgerComponent.DTIP);
    return new BookFigures(
        invoice.getGrossPremium(),
        invoice.premiumBalance(),
        dtip.map(OpsInvoiceComponent::getBalance).orElse(BigDecimal.ZERO),
        dtip.map(OpsInvoiceComponent::getRemitted).orElse(BigDecimal.ZERO),
        remittance.map(OpsInvoiceMovement::getBatchNo).orElse(null),
        remittance.map(OpsInvoiceMovement::getValueDate).orElse(null),
        component(invoice, LedgerComponent.PR2307)
            .map(OpsInvoiceComponent::getBalance)
            .orElse(null),
        cwt.map(OpsInvoiceMovement::getBatchNo).orElse(null),
        cwt.map(OpsInvoiceMovement::getValueDate).orElse(null),
        cancellationOf(invoice),
        invoice.isDpFlag());
  }

  private static ReconBucket bucket(OpsInvoice invoice, BookFigures book) {
    if (invoice.isCancelled()) {
      return ReconBucket.CANCELLED;
    }
    if (book.directBilled()) {
      return ReconBucket.DIRECT_BILLED;
    }
    if (book.remitted().signum() > 0 && book.forRemittance().signum() <= 0) {
      return ReconBucket.REMITTED;
    }
    return book.outstanding().signum() > 0 ? ReconBucket.OUTSTANDING : ReconBucket.FOR_REMITTANCE;
  }

  private String cancellationOf(OpsInvoice invoice) {
    return ledger.family(invoice.getInvoiceNo()).stream()
        .filter(i -> i.getKind() == InvoiceKind.CANCELLATION)
        .map(OpsInvoice::getInvoiceNo)
        .filter(no -> !no.equals(invoice.getInvoiceNo()) || invoice.isCancelled())
        .findFirst()
        .orElse(null);
  }

  private static Optional<OpsInvoiceMovement> latest(
      List<OpsInvoiceMovement> movements, MovementType type) {
    return movements.stream()
        .filter(m -> m.getMovementType() == type)
        .max(Comparator.comparing(OpsInvoiceMovement::getValueDate));
  }

  private static Optional<OpsInvoiceComponent> component(
      OpsInvoice invoice, LedgerComponent component) {
    return invoice.getComponents().stream().filter(c -> c.getComponent() == component).findFirst();
  }
}
