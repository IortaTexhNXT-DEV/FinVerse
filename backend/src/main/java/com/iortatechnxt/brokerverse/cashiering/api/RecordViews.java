package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.api.dto.RecordDtos.RecordResponse;
import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.CashBankAccountRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordReason;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** A record with the names and labels the screens show (names not logins, labels not codes). */
@Component
@Transactional(readOnly = true)
public class RecordViews {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final UserDirectory users;
  private final LovService lovs;
  private final BranchRepository branches;
  private final CashBankAccountRepository bankAccounts;
  private final Clock clock;
  private final AuditTrailService audit;
  private final DocumentComposer composer;

  /**
   * Creates the views.
   *
   * @param users display names
   * @param lovs list labels
   * @param branches branch names
   * @param bankAccounts bank account names
   * @param clock clock
   * @param audit audit trail of the records
   * @param composer Excel files
   */
  public RecordViews(
      UserDirectory users,
      LovService lovs,
      BranchRepository branches,
      CashBankAccountRepository bankAccounts,
      Clock clock,
      AuditTrailService audit,
      DocumentComposer composer) {
    this.audit = audit;
    this.composer = composer;
    this.users = users;
    this.lovs = lovs;
    this.branches = branches;
    this.bankAccounts = bankAccounts;
    this.clock = clock;
  }

  /**
   * The business day.
   *
   * @return today
   */
  LocalDate today() {
    return BusinessClock.today(clock);
  }

  /**
   * A record for the screens.
   *
   * @param r record
   * @return response
   */
  public RecordResponse of(ReceiptRecord r) {
    String bank = r.getTender() == null ? null : r.getTender().bankAccount();
    return new RecordResponse(
        r.getId(),
        r.getRecordNo(),
        r.getRecordKind().name(),
        r.getReceiptKind().name(),
        r.getReceiptType(),
        typeLabel(r),
        r.getStage().name(),
        r.label(),
        r.isEditable(),
        r.getBranchId(),
        r.getBranchId() == null
            ? null
            : branches.findById(r.getBranchId()).map(Branch::getName).orElse(null),
        r.getParty(),
        r.getTender(),
        bank == null
            ? null
            : bankAccounts
                .findByCompanyIdAndCode(r.getCompanyId(), bank)
                .map(CashBankAccount::getName)
                .orElse(bank),
        r.getReason(),
        reasonLabel(r),
        r.getReceiptId(),
        r.getReceiptNo(),
        r.getAccounts(),
        r.getAccounts().stream().map(RecordAccount::reference).collect(Collectors.joining(", ")),
        r.total(),
        r.getReturnReason(),
        users.displayName(r.getCreatedBy()),
        r.getCreatedBy(),
        r.getCreatedAt(),
        r.getSubmittedAt(),
        users.displayName(r.getPostedBy()),
        r.getPostedAt(),
        r.getPostingResult());
  }

  private String typeLabel(ReceiptRecord r) {
    if (r.getReceiptType() == null) {
      return null;
    }
    String lov = r.getReceiptKind() == ReceiptKind.OR ? "OR_TYPE" : "AR_CLASS";
    return lovs.label(lov, r.getReceiptType());
  }

  private String reasonLabel(ReceiptRecord r) {
    RecordReason reason = r.getReason();
    if (reason == null || reason.reasonCode() == null) {
      return null;
    }
    String lov =
        r.getRecordKind() == RecordKind.CANCELLATION
            ? "RECEIPT_CANCEL_REASON"
            : "REINSTATEMENT_REASON";
    return lovs.label(lov, reason.reasonCode());
  }

  /**
   * The audit trail of a record with the names of the users.
   *
   * @param r record
   * @return entries, newest first
   */
  public List<HistoryEntry> history(ReceiptRecord r) {
    return audit.history(ReceiptRecordService.ENTITY, List.of(r.getRecordNo())).stream()
        .map(
            a ->
                new HistoryEntry(
                    a.getOccurredAt(),
                    users.displayName(a.getUsername()),
                    a.getAction().name(),
                    a.getSummary()))
        .toList();
  }

  /**
   * A list of records as an Excel file named after the list and the date of extraction.
   *
   * @param kind record kind
   * @param rows records
   * @return file
   */
  public ResponseEntity<byte[]> excel(RecordKind kind, List<ReceiptRecord> rows) {
    List<String> headers =
        List.of(
            numberHeader(kind),
            "Receipt Type",
            "Record Type",
            "AR/OR Number",
            "Amount",
            "Account Number / Invoice Number",
            "Client Name",
            "Insurer Name",
            "Receipting Branch",
            "Created by",
            "Date Created",
            "Record Status");
    List<List<Object>> lines = new ArrayList<>();
    for (ReceiptRecord r : rows) {
      lines.add(excelRow(r));
    }
    String list = listName(kind);
    String name = list + "_" + today().format(DateTimeFormatter.ofPattern("MMddyyyy")) + ".xlsx";
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(XLSX))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(name).build().toString())
        .body(composer.xlsx(new SheetSpec(list, headers, lines)));
  }

  private List<Object> excelRow(ReceiptRecord r) {
    RecordResponse v = of(r);
    String client =
        v.party() == null ? null : firstOf(v.party().clientName(), v.party().payorName());
    String insurer = v.party() == null ? null : v.party().insurerName();
    return List.of(
        v.recordNo(),
        v.receiptKind(),
        nz(v.receiptTypeLabel()),
        nz(v.receiptNo()),
        v.total(),
        v.accountsText(),
        nz(client),
        nz(insurer),
        nz(v.branchName()),
        nz(v.createdBy()),
        DisplayFormat.date(BusinessClock.dateOf(r.getCreatedAt())),
        v.statusLabel());
  }

  private static String numberHeader(RecordKind kind) {
    return switch (kind) {
      case CREATION -> "Creation Record Number";
      case CANCELLATION -> "Cancellation Record Number";
      case REINSTATEMENT -> "Reinstatement Record Number";
    };
  }

  private static String listName(RecordKind kind) {
    return switch (kind) {
      case CREATION -> "AR-OR for Issuance Posting";
      case CANCELLATION -> "Cancellation for Posting";
      case REINSTATEMENT -> "Reinstatement for Posting";
    };
  }

  private static String nz(String value) {
    return value == null ? "" : value;
  }

  private static String firstOf(String a, String b) {
    return a != null ? a : b;
  }

  /**
   * One activity on a record.
   *
   * @param at date and time
   * @param user user's name
   * @param action activity
   * @param summary what was done, with the values before and after
   */
  public record HistoryEntry(Instant at, String user, String action, String summary) {}
}
