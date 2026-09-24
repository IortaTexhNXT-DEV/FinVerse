package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DropContent;
import com.iortatechnxt.brokerverse.remittance.domain.EodRequest;
import com.iortatechnxt.brokerverse.remittance.domain.EodRequestRepository;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun.Scope;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRunRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTrigger;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RunStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Remittance extraction (RMTID.001/003/004/005): runs a logged extraction for a company, scheduled
 * or manual, per insurer and type or for one invoice. The run record, the work and the outcome are
 * committed separately, so a failing run is still logged, raises {@code REMIT_EXTRACTION_FAILED}
 * and notifies the processors; a successful run stores one extract file per batch in the folder of
 * its remittance type (FileDropPort, shared drive parked OQ17), processes the end-of-day requests
 * and notifies {@code REMIT_EXTRACTION_DONE} (RMTID.034).
 */
@Service
public class ExtractionService {

  /** Permission of the processors (notifications). */
  public static final String PROCESSORS = "REMIT_PROCESS";

  private static final String FAILED_ALERT = "REMIT_EXTRACTION_FAILED";
  private static final String ENTITY = "RemittanceExtractionRun";
  private static final String LINK = "/remittance/extraction";

  private final ExtractionRunRepository runs;
  private final ExtractionWork work;
  private final RemittanceBatchRepository batches;
  private final EodRequestRepository eod;
  private final InvoiceLedgerQueryService ledger;
  private final BatchDocuments documents;
  private final RemittanceSettings settings;
  private final FileDropPort files;
  private final DocumentNumberService numbers;
  private final NotificationService notifications;
  private final AlertService alerts;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param runs runs
   * @param work run work
   * @param batches batches
   * @param eod end-of-day requests
   * @param ledger ledger reads
   * @param documents extract files
   * @param settings parameters
   * @param files shared-drive port
   * @param numbers run numbers
   * @param notifications notifications
   * @param alerts alerts
   * @param audit audit trail
   * @param currentUser current user
   * @param txManager transactions
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public ExtractionService(
      ExtractionRunRepository runs,
      ExtractionWork work,
      RemittanceBatchRepository batches,
      EodRequestRepository eod,
      InvoiceLedgerQueryService ledger,
      BatchDocuments documents,
      RemittanceSettings settings,
      FileDropPort files,
      DocumentNumberService numbers,
      NotificationService notifications,
      AlertService alerts,
      AuditTrailService audit,
      CurrentUser currentUser,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.runs = runs;
    this.work = work;
    this.batches = batches;
    this.eod = eod;
    this.ledger = ledger;
    this.documents = documents;
    this.settings = settings;
    this.files = files;
    this.numbers = numbers;
    this.notifications = notifications;
    this.alerts = alerts;
    this.audit = audit;
    this.currentUser = currentUser;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
  }

  /**
   * Runs an extraction outside any transaction (job, manual run).
   *
   * @param companyId company
   * @param scope trigger, insurer, type or invoice
   * @param businessDate business date
   * @return the finished run
   */
  public ExtractionRun run(Long companyId, Scope scope, LocalDate businessDate) {
    if (scope.invoiceNo() != null) {
      requireExtractable(scope);
    }
    Long runId =
        required(tx.execute(s -> runs.save(newRun(companyId, scope, businessDate)).getId()));
    try {
      ExtractionWork.Outcome outcome =
          required(tx.execute(s -> work.perform(runs.findById(runId).orElseThrow(), null)));
      return required(tx.execute(s -> succeeded(runId, outcome)));
    } catch (BusinessRuleException
        | ResourceNotFoundException
        | IllegalStateException
        | DataAccessException ex) {
      String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
      return required(tx.execute(s -> failed(runId, message)));
    }
  }

  /**
   * Extracts the invoice of an approved special remittance request into its own SPECIAL batch
   * inside the caller's transaction (MKTID.009); refused when the invoice is not eligible.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param requestNo special remittance request
   * @param businessDate business date
   * @return the special batch
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public RemittanceBatch special(
      Long companyId, String invoiceNo, String requestNo, LocalDate businessDate) {
    ExtractionRun run =
        runs.save(
            newRun(
                companyId,
                new Scope(ExtractionTrigger.SPECIAL, null, null, invoiceNo),
                businessDate));
    ExtractionWork.Outcome outcome = work.perform(run, requestNo);
    if (outcome.batches().isEmpty()) {
      throw new BusinessRuleException(
          "SPECIAL_REMIT_NOT_ELIGIBLE",
          "Invoice " + invoiceNo + " cannot be remitted now: " + outcome.tagged().get(invoiceNo));
    }
    run.finish(RunStatus.SUCCEEDED, run.summary(), clock.instant());
    audit.record(ENTITY, run.getRunNo(), AuditAction.RUN, run.summary());
    return outcome.batches().get(0);
  }

  private ExtractionRun newRun(Long companyId, Scope scope, LocalDate businessDate) {
    return new ExtractionRun(
        companyId,
        numbers.next("REX-" + businessDate.getYear()),
        scope,
        businessDate,
        clock.instant());
  }

  private void requireExtractable(Scope scope) {
    OpsInvoice invoice = ledger.require(scope.invoiceNo());
    if (scope.insurerCode() != null && !scope.insurerCode().equals(invoice.getInsurerCode())) {
      throw new BusinessRuleException(
          "REMIT_INVOICE_INSURER",
          "Invoice " + scope.invoiceNo() + " is not an invoice of " + scope.insurerCode());
    }
    if (!ExtractionWork.isExaminable(invoice)) {
      throw new BusinessRuleException(
          "REMIT_INVOICE_NOT_EXTRACTABLE",
          "Invoice "
              + scope.invoiceNo()
              + " cannot be extracted: remittance status "
              + invoice.getRemittanceStatus()
              + (invoice.isDpFlag() ? " (direct payment)" : ""));
    }
  }

  private ExtractionRun succeeded(Long runId, ExtractionWork.Outcome outcome) {
    ExtractionRun run = runs.findById(runId).orElseThrow();
    for (RemittanceBatch created : outcome.batches()) {
      storeExtract(run, batches.findWithLinesById(created.getId()).orElseThrow());
    }
    int requests = processRequests(run, outcome.tagged());
    String summary =
        run.summary() + (requests == 0 ? "" : "; " + requests + " end-of-day request(s) processed");
    run.finish(RunStatus.SUCCEEDED, summary, clock.instant());
    audit.record(ENTITY, run.getRunNo(), AuditAction.RUN, summary);
    notifications.notifyPermission(
        PROCESSORS,
        new Notice(
            "Remittance extraction " + run.getRunNo(), summary, LINK, ENTITY, run.getRunNo()),
        "REMIT_EXTRACTION_DONE");
    return run;
  }

  private void storeExtract(ExtractionRun run, RemittanceBatch batch) {
    FileDropPort.DroppedFile dropped =
        files.drop(
            batch.getCompanyId(),
            new ExtractFile.Location(
                "REMITTANCE/" + batch.getRemittanceType().name(),
                settings.extractFileName(
                    batch.getBatchNo(),
                    batch.getInsurerCode(),
                    batch.getRemittanceType().name(),
                    run.getBusinessDate().toString())),
            new DropContent(BatchDocuments.XLSX, documents.scheduleXlsx(batch)),
            new ExtractFile.Origin(RemittanceSettings.MODULE, batch.getBatchNo()));
    batch.extractFile(dropped.id());
  }

  private int processRequests(ExtractionRun run, Map<String, ExtractionTag> tagged) {
    if (run.getTrigger() != ExtractionTrigger.SCHEDULED) {
      return 0;
    }
    int processed = 0;
    for (EodRequest r : eod.findByCompanyIdAndRunNoIsNullOrderByIdAsc(run.getCompanyId())) {
      r.processed(
          run.getRunNo(),
          tagged.getOrDefault(r.getInvoiceNo(), ExtractionTag.UNEXTRACTED_NOT_DUE),
          clock.instant());
      processed++;
    }
    return processed;
  }

  private ExtractionRun failed(Long runId, String message) {
    ExtractionRun run = runs.findById(runId).orElseThrow();
    run.finish(RunStatus.FAILED, message, clock.instant());
    audit.record(ENTITY, run.getRunNo(), AuditAction.RUN, "Failed: " + message);
    alerts.raise(
        FAILED_ALERT,
        new AlertFacts(
            run.getCompanyId(),
            null,
            ENTITY,
            run.getRunNo(),
            "Remittance extraction " + run.getRunNo() + " failed: " + message,
            null,
            FAILED_ALERT + ":" + run.getRunNo()));
    notifications.notifyPermission(
        PROCESSORS,
        new Notice(
            "Remittance extraction " + run.getRunNo() + " failed",
            message,
            LINK,
            ENTITY,
            run.getRunNo()),
        "REMIT_EXTRACTION_DONE");
    return run;
  }

  /**
   * Queues an invoice looked up for remittance for the end-of-day extraction (RMTID.005): once per
   * invoice and day.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @return the request
   */
  @Transactional
  public EodRequest queueForEndOfDay(Long companyId, String invoiceNo) {
    OpsInvoice invoice = ledger.require(invoiceNo);
    LocalDate today = LocalDate.now(clock);
    return eod.findByInvoiceNoAndRequestedOn(invoice.getInvoiceNo(), today)
        .orElseGet(
            () -> {
              EodRequest saved =
                  eod.save(new EodRequest(companyId, invoiceNo, today, currentUser.username()));
              audit.record(
                  "RemittanceEodRequest", invoiceNo, AuditAction.CREATE, "Queued for end of day");
              return saved;
            });
  }

  private static <T> T required(T value) {
    if (value == null) {
      throw new IllegalStateException("The extraction step returned nothing");
    }
    return value;
  }
}
