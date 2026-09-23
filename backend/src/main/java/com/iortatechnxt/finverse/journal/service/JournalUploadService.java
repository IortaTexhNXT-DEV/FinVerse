package com.iortatechnxt.finverse.journal.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.finverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.finverse.journal.api.dto.JournalRequest;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.service.UploadResult.RowResult;
import com.iortatechnxt.finverse.journal.service.UploadResult.VoucherResult;
import com.iortatechnxt.finverse.journal.service.UploadResult.VoucherStatus;
import com.iortatechnxt.finverse.organization.domain.BranchRepository;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bulk upload of manual journals from CSV or XLSX (format: docs/samples/journal-upload.md).
 *
 * <p>Rows are grouped into vouchers by {@code voucher_key}. Every row and voucher is validated and
 * reported; each valid voucher is created as a DRAFT journal in its own transaction (atomic per
 * voucher). In VALIDATE mode the same checks run, including account resolution, and every
 * transaction is rolled back, so nothing is created. This class manages its own transactions.
 */
@Service
public class JournalUploadService {

  /** Largest number of data rows per file. */
  public static final int MAX_ROWS = 5000;

  /** Largest accepted file. */
  public static final int MAX_BYTES = 5 * 1024 * 1024;

  /** Source recorded on journals created by an upload; the reference is the upload number. */
  public static final String SOURCE_MODULE = "JOURNAL_UPLOAD";

  private static final String UPLOAD_PREFIX = "UPL-";

  private static final List<String> REQUIRED_HEADER =
      List.of(
          UploadLine.BRANCH_CODE, UploadLine.VALUE_DATE, UploadLine.CURRENCY, UploadLine.NARRATION);

  private final JournalEntryService entries;
  private final BranchRepository branches;
  private final Validator validator;
  private final AuditTrailService audit;
  private final DocumentNumberService numbers;
  private final Clock clock;
  private final TransactionTemplate newTransaction;

  /**
   * Creates the service.
   *
   * @param entries journal entry service
   * @param branches branch repository
   * @param validator bean validator
   * @param audit audit trail
   * @param numbers document numbers (upload references)
   * @param clock clock
   * @param transactionManager transaction manager
   */
  public JournalUploadService(
      JournalEntryService entries,
      BranchRepository branches,
      Validator validator,
      AuditTrailService audit,
      DocumentNumberService numbers,
      Clock clock,
      PlatformTransactionManager transactionManager) {
    this.entries = entries;
    this.branches = branches;
    this.validator = validator;
    this.audit = audit;
    this.numbers = numbers;
    this.clock = clock;
    this.newTransaction = new TransactionTemplate(transactionManager);
    this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Validates an upload file and, when {@code commit} is set, creates the valid vouchers. An import
   * gets an upload reference (UPL-yyyy-nnnnnn) recorded as the source of every journal it creates.
   *
   * @param companyId company
   * @param fileName file name (.csv or .xlsx)
   * @param content file bytes
   * @param commit true to create drafts, false for a dry run
   * @return validation report
   */
  public UploadResult process(Long companyId, String fileName, byte[] content, boolean commit) {
    if (content.length == 0 || content.length > MAX_BYTES) {
      throw new BusinessRuleException(
          "INVALID_UPLOAD_SIZE", "The file must contain data and be at most 5 MB");
    }
    List<List<String>> table = SpreadsheetRows.read(fileName, content, MAX_ROWS + 1);
    List<String> columns = columns(table);
    List<UploadLine> lines = new ArrayList<>();
    for (int i = 1; i < table.size(); i++) {
      lines.add(UploadLine.parse(i + 1, cells(columns, table.get(i))));
    }
    Map<String, List<UploadLine>> vouchers = new LinkedHashMap<>();
    lines.stream()
        .filter(l -> !l.voucherKey().isBlank())
        .forEach(l -> vouchers.computeIfAbsent(l.voucherKey(), k -> new ArrayList<>()).add(l));
    String uploadReference =
        commit
            ? newTransaction.execute(
                s -> numbers.next(UPLOAD_PREFIX + LocalDate.now(clock).getYear()))
            : null;
    List<VoucherResult> results = new ArrayList<>();
    vouchers.forEach(
        (key, group) -> results.add(processVoucher(companyId, key, group, uploadReference)));
    UploadResult result =
        new UploadResult(
            fileName,
            uploadReference,
            commit,
            lines.size(),
            results,
            lines.stream()
                .map(l -> new RowResult(l.rowNumber(), l.voucherKey(), l.valid(), l.errors()))
                .toList());
    if (commit) {
      newTransaction.executeWithoutResult(
          s ->
              audit.record(
                  "JournalUpload",
                  fileName,
                  AuditAction.RUN,
                  "Journal upload "
                      + uploadReference
                      + ": "
                      + result.createdVouchers()
                      + " of "
                      + results.size()
                      + " voucher(s) created as drafts"));
    }
    return result;
  }

  /** Normalized header columns; rejects files without a header or with missing columns. */
  private static List<String> columns(List<List<String>> table) {
    if (table.isEmpty()) {
      throw new BusinessRuleException("EMPTY_FILE", "The file has no header row");
    }
    List<String> columns = table.get(0).stream().map(JournalUploadService::column).toList();
    List<String> missing =
        UploadLine.REQUIRED_COLUMNS.stream().filter(c -> !columns.contains(c)).toList();
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "MISSING_COLUMNS", "Missing column(s): " + String.join(", ", missing));
    }
    return columns;
  }

  private VoucherResult processVoucher(
      Long companyId, String key, List<UploadLine> group, String uploadReference) {
    List<String> errors = new ArrayList<>();
    group.stream()
        .filter(l -> !l.valid())
        .forEach(l -> errors.add("Row " + l.rowNumber() + " has errors"));
    Map<String, String> header = mergeHeader(group, errors);
    Optional<Long> branchId = branchId(companyId, header.get(UploadLine.BRANCH_CODE), errors);
    if (group.size() < 2) {
      errors.add("A voucher needs at least two lines");
    }
    VoucherResult base =
        new VoucherResult(
            key,
            group.get(0).rowNumber(),
            group.size(),
            totalDebit(group),
            VoucherStatus.ERROR,
            null,
            null,
            errors);
    if (!errors.isEmpty() || branchId.isEmpty()) {
      return base;
    }
    JournalRequest request = toRequest(companyId, branchId.get(), header, group);
    validator
        .validate(request)
        .forEach(v -> errors.add(v.getPropertyPath() + ": " + v.getMessage()));
    try {
      RecurringLines.requireBalanced(request.currency(), request.lines());
    } catch (BusinessRuleException e) {
      errors.add(e.getMessage());
    }
    return errors.isEmpty() ? create(base, request, uploadReference) : withErrors(base, errors);
  }

  /** Creates the draft (import) or checks that it could be created and rolls back (dry run). */
  private VoucherResult create(VoucherResult base, JournalRequest request, String uploadReference) {
    boolean commit = uploadReference != null;
    try {
      JournalBatch batch =
          newTransaction.execute(
              s -> {
                JournalBatch draft = entries.createDraft(request, SOURCE_MODULE, uploadReference);
                if (!commit) {
                  s.setRollbackOnly();
                }
                return draft;
              });
      return commit && batch != null
          ? new VoucherResult(
              base.voucherKey(),
              base.firstRow(),
              base.lineCount(),
              base.totalDebit(),
              VoucherStatus.CREATED,
              batch.getId(),
              batch.getBatchNo(),
              List.of())
          : new VoucherResult(
              base.voucherKey(),
              base.firstRow(),
              base.lineCount(),
              base.totalDebit(),
              VoucherStatus.VALID,
              null,
              null,
              List.of());
    } catch (BusinessRuleException | ResourceNotFoundException e) {
      return withErrors(base, List.of(e.getMessage()));
    }
  }

  private static VoucherResult withErrors(VoucherResult base, List<String> errors) {
    return new VoucherResult(
        base.voucherKey(),
        base.firstRow(),
        base.lineCount(),
        base.totalDebit(),
        VoucherStatus.ERROR,
        null,
        null,
        errors);
  }

  /**
   * Header values of a voucher: taken from its first row that carries them; any later row that
   * repeats a header column must repeat the same value.
   */
  private static Map<String, String> mergeHeader(List<UploadLine> group, List<String> errors) {
    Map<String, String> header = new HashMap<>();
    for (String column : UploadLine.HEADER_COLUMNS) {
      String first = "";
      for (UploadLine line : group) {
        String value = line.header().getOrDefault(column, "");
        if (first.isBlank()) {
          first = value;
        } else if (!value.isBlank() && !value.equals(first)) {
          errors.add("Row " + line.rowNumber() + ": " + column + " differs from the voucher");
        }
      }
      header.put(column, first);
    }
    REQUIRED_HEADER.stream()
        .filter(c -> header.get(c).isBlank())
        .forEach(c -> errors.add(c + " is required"));
    return header;
  }

  private Optional<Long> branchId(Long companyId, String code, List<String> errors) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    Optional<Long> id =
        branches.findByCompanyIdAndCode(companyId, UploadLine.upper(code)).map(b -> b.getId());
    if (id.isEmpty()) {
      errors.add("Unknown branch " + code);
    }
    return id;
  }

  private static JournalRequest toRequest(
      Long companyId, Long branchId, Map<String, String> header, List<UploadLine> group) {
    String reference = header.get(UploadLine.REFERENCE);
    return new JournalRequest(
        companyId,
        branchId,
        UploadLine.journalType(header.get(UploadLine.JOURNAL_TYPE)),
        LocalDate.parse(header.get(UploadLine.VALUE_DATE)),
        UploadLine.upper(header.get(UploadLine.CURRENCY)),
        header.get(UploadLine.NARRATION),
        reference.isBlank() ? null : reference,
        group.stream().map(UploadLine::line).toList());
  }

  private static BigDecimal totalDebit(List<UploadLine> group) {
    return group.stream()
        .map(UploadLine::line)
        .filter(l -> l != null && l.side() == BalanceSide.DEBIT)
        .map(JournalLineRequest::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static Map<String, String> cells(List<String> columns, List<String> row) {
    Map<String, String> values = new HashMap<>();
    for (int c = 0; c < columns.size() && c < row.size(); c++) {
      values.put(columns.get(c), row.get(c));
    }
    return values;
  }

  private static String column(String header) {
    return header.strip().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
  }
}
