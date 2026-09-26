package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.placement.domain.BillingBatch;
import com.iortatechnxt.brokerverse.placement.domain.BillingBatchRepository;
import com.iortatechnxt.brokerverse.placement.domain.BillingBatchStatus;
import com.iortatechnxt.brokerverse.placement.domain.BillingItem;
import com.iortatechnxt.brokerverse.placement.domain.MatchStatus;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReport;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportKind;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportLine;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportLine.MatchedAccount;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportRepository;
import com.iortatechnxt.brokerverse.placement.domain.ReportedPayment;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment report import and match review (BRNB.067/068): a CLPC report is matched to its billing
 * batch by PN or loan application number, a report of the other segments to the accounts awaiting
 * payment by ARN. Each line ends matched, unpaid, unmatched or ambiguous; the user may choose the
 * account of an ambiguous or unmatched line. Confirmation is orchestrated by {@link
 * PaymentReportConfirmation}.
 */
@Service
@Transactional
public class PaymentReportService {

  /** Audit entity type. */
  public static final String ENTITY = "PaymentReport";

  private final PaymentReportRepository reports;
  private final BillingBatchRepository batches;
  private final AccountQueryService queries;
  private final PlacementAccounts accounts;
  private final BulkFileReader reader;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param reports payment reports
   * @param batches billing batches
   * @param queries account reads
   * @param accounts account look-ups
   * @param reader XLSX / CSV / ODS reader
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PaymentReportService(
      PaymentReportRepository reports,
      BillingBatchRepository batches,
      AccountQueryService queries,
      PlacementAccounts accounts,
      BulkFileReader reader,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.reports = reports;
    this.batches = batches;
    this.queries = queries;
    this.accounts = accounts;
    this.reader = reader;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Uploads and matches a payment report.
   *
   * @param upload company, kind, batch and file
   * @return the report under review
   */
  public PaymentReport upload(ReportUpload upload) {
    BillingBatch batch = upload.kind() == PaymentReportKind.CLPC ? openBatch(upload) : null;
    List<ReportedPayment> rows =
        PaymentReportParser.read(reader.read(upload.fileName(), upload.content()), upload.kind());
    PaymentReport report =
        new PaymentReport(
            upload.companyId(),
            numbers.next("PMT-" + LocalDate.now(clock).getYear()),
            upload.kind(),
            batch == null ? null : batch.getId(),
            upload.fileName());
    for (ReportedPayment row : rows) {
      PaymentReportLine line = report.add(row);
      if (batch == null) {
        matchByArn(upload.companyId(), line);
      } else {
        matchOnBatch(batch, line);
      }
    }
    if (batch != null) {
      batch.reportReceived();
    }
    PaymentReport saved = reports.save(report);
    audit.record(
        ENTITY,
        saved.getReportNo(),
        AuditAction.CREATE,
        "Payment report "
            + upload.fileName()
            + ": "
            + saved.count(MatchStatus.MATCHED)
            + " matched, "
            + saved.count(MatchStatus.UNMATCHED)
            + " unmatched, "
            + saved.count(MatchStatus.AMBIGUOUS)
            + " ambiguous");
    return saved;
  }

  private BillingBatch openBatch(ReportUpload upload) {
    if (upload.batchId() == null) {
      throw new BusinessRuleException(
          "PAYMENT_REPORT_BATCH_REQUIRED", "Choose the CLPC billing batch the report answers");
    }
    BillingBatch batch =
        batches
            .findById(upload.batchId())
            .filter(b -> b.getCompanyId().equals(upload.companyId()))
            .orElseThrow(
                () -> new ResourceNotFoundException(BillingService.ENTITY, upload.batchId()));
    if (batch.getStatus() == BillingBatchStatus.CLOSED) {
      throw new BusinessRuleException(
          "BILLING_BATCH_CLOSED", "Billing batch " + batch.getBatchNo() + " is closed");
    }
    return batch;
  }

  private void matchOnBatch(BillingBatch batch, PaymentReportLine line) {
    List<BillingItem> items =
        batch.getItems().stream().filter(i -> i.isIdentifiedBy(line.getReference())).toList();
    Map<Long, BillingItem> byAccount =
        items.stream()
            .collect(Collectors.toMap(BillingItem::getAccountId, Function.identity(), (a, b) -> a));
    if (items.isEmpty()) {
      line.match(
          MatchStatus.UNMATCHED, null, List.of(), "Not on billing batch " + batch.getBatchNo());
    } else if (byAccount.size() > 1) {
      line.match(
          MatchStatus.AMBIGUOUS,
          null,
          byAccount.values().stream().map(BillingItem::getArn).toList(),
          "Several billed accounts share this reference");
    } else {
      matchAccount(line, queries.get(items.get(0).getAccountId()));
    }
  }

  private void matchByArn(Long companyId, PaymentReportLine line) {
    List<Account> found = queries.preBooked(companyId, line.getReference());
    List<Account> awaiting =
        found.stream().filter(a -> a.getStatus() == AccountStatus.AWAITING_PAYMENT).toList();
    if (awaiting.size() > 1) {
      line.match(
          MatchStatus.AMBIGUOUS,
          null,
          awaiting.stream().map(Account::getArn).toList(),
          "Several accounts awaiting payment have this reference");
    } else if (awaiting.size() == 1) {
      matchAccount(line, awaiting.get(0));
    } else if (found.isEmpty()) {
      line.match(MatchStatus.UNMATCHED, null, List.of(), "No live account has this reference");
    } else {
      matchAccount(line, found.get(0));
    }
  }

  private static void matchAccount(PaymentReportLine line, Account account) {
    MatchedAccount matched = new MatchedAccount(account.getId(), account.getArn());
    if (account.getStatus() != AccountStatus.AWAITING_PAYMENT) {
      line.match(
          MatchStatus.UNMATCHED,
          matched,
          List.of(),
          account.getArn() + " is " + account.getStatus() + ", not awaiting payment");
    } else if (line.isPaid()) {
      line.match(MatchStatus.MATCHED, matched, List.of(), null);
    } else {
      line.match(MatchStatus.UNPAID, matched, List.of(), "Reported unpaid");
    }
  }

  /**
   * Matches an ambiguous or unmatched line to an account chosen by the user.
   *
   * @param reportId report
   * @param lineId line
   * @param arn chosen account, awaiting payment
   * @return the report
   */
  public PaymentReport resolveLine(Long reportId, Long lineId, String arn) {
    PaymentReport report = get(reportId);
    report.requireReview();
    PaymentReportLine line =
        report.getLines().stream()
            .filter(l -> l.getId().equals(lineId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Payment report line", lineId));
    if (line.getMatchStatus() == MatchStatus.MATCHED
        || line.getMatchStatus() == MatchStatus.UNPAID) {
      throw new BusinessRuleException(
          "PAYMENT_LINE_MATCHED",
          "Row " + line.getRowNo() + " is already matched to " + line.getArn());
    }
    Account account = accounts.require(report.getCompanyId(), arn);
    if (account.getStatus() != AccountStatus.AWAITING_PAYMENT) {
      throw new BusinessRuleException(
          "ACCOUNT_NOT_AWAITING_PAYMENT",
          arn + " is " + account.getStatus() + ", not awaiting payment");
    }
    line.matchManually(
        new MatchedAccount(account.getId(), account.getArn()), currentUser.username());
    audit.record(
        ENTITY,
        report.getReportNo(),
        AuditAction.UPDATE,
        "Row " + line.getRowNo() + " (" + line.getReference() + ") matched to " + arn);
    return report;
  }

  /**
   * Discards a report under review.
   *
   * @param reportId report
   * @return the report
   */
  public PaymentReport discard(Long reportId) {
    PaymentReport report = get(reportId);
    report.discard();
    audit.record(ENTITY, report.getReportNo(), AuditAction.DEACTIVATE, "Payment report discarded");
    return report;
  }

  /**
   * Confirms the report and records the CLPC payment outcome on the billing batch (first step of
   * the confirmation; the gates are opened afterwards, one account per transaction).
   *
   * @param reportId report
   * @return ids of the matched, paid lines
   */
  public List<Long> markConfirmed(Long reportId) {
    PaymentReport report = get(reportId);
    report.confirm(currentUser.username(), clock.instant());
    if (report.getBatchId() != null) {
      BillingBatch batch =
          batches
              .findById(report.getBatchId())
              .orElseThrow(
                  () -> new ResourceNotFoundException(BillingService.ENTITY, report.getBatchId()));
      for (PaymentReportLine line : report.getLines()) {
        batch.getItems().stream()
            .filter(i -> i.getAccountId().equals(line.getAccountId()))
            .forEach(i -> i.markPayment(line.getMatchStatus() == MatchStatus.MATCHED));
      }
      batch.close();
    }
    audit.record(ENTITY, report.getReportNo(), AuditAction.AUTHORIZE, "Payment report confirmed");
    return report.getLines().stream()
        .filter(l -> l.getMatchStatus() == MatchStatus.MATCHED)
        .map(PaymentReportLine::getId)
        .toList();
  }

  /**
   * Records whether the gate opened for a line.
   *
   * @param reportId report
   * @param lineId line
   * @param applied opened
   * @param message outcome
   */
  public void markApplied(Long reportId, Long lineId, boolean applied, String message) {
    get(reportId).getLines().stream()
        .filter(l -> l.getId().equals(lineId))
        .forEach(l -> l.markApplied(applied, message));
  }

  /**
   * The confirmation of a matched, paid line (for the payment gate).
   *
   * @param reportId report
   * @param lineId line
   * @return confirmation
   */
  @Transactional(readOnly = true)
  public ConfirmedPayment confirmationOf(Long reportId, Long lineId) {
    return get(reportId).getLines().stream()
        .filter(l -> l.getId().equals(lineId))
        .findFirst()
        .map(ReportPaymentSource::confirmation)
        .orElseThrow(() -> new ResourceNotFoundException("Payment report line", lineId));
  }

  /**
   * Reports of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return reports
   */
  @Transactional(readOnly = true)
  public Page<PaymentReport> reports(Long companyId, Pageable pageable) {
    Page<PaymentReport> page = reports.findByCompanyIdOrderByIdDesc(companyId, pageable);
    page.forEach(PaymentReport::getLines);
    return page;
  }

  /**
   * One report with its lines.
   *
   * @param id report
   * @return report
   */
  @Transactional(readOnly = true)
  public PaymentReport get(Long id) {
    PaymentReport report =
        reports.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    report.getLines().forEach(PaymentReportLine::getArn);
    return report;
  }

  /**
   * An uploaded payment report.
   *
   * @param companyId company
   * @param kind CLPC or reference report
   * @param batchId billing batch answered (CLPC)
   * @param fileName file name (.xlsx, .csv or .ods)
   * @param content bytes
   */
  public record ReportUpload(
      Long companyId, PaymentReportKind kind, Long batchId, String fileName, byte[] content) {

    /** Defensive copy. */
    public ReportUpload {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ReportUpload u
          && Objects.equals(companyId, u.companyId)
          && kind == u.kind
          && Objects.equals(batchId, u.batchId)
          && Objects.equals(fileName, u.fileName)
          && Arrays.equals(content, u.content);
    }

    @Override
    public int hashCode() {
      return Objects.hash(companyId, kind, batchId, fileName, Arrays.hashCode(content));
    }

    @Override
    public String toString() {
      return "ReportUpload[" + fileName + "]";
    }
  }
}
