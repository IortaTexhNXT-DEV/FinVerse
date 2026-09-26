package com.iortatechnxt.brokerverse.placement.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.placement.domain.BillingBatch;
import com.iortatechnxt.brokerverse.placement.domain.BillingBatchRepository;
import com.iortatechnxt.brokerverse.placement.domain.BillingItem;
import com.iortatechnxt.brokerverse.placement.domain.BillingItemStatus;
import com.iortatechnxt.brokerverse.placement.domain.BillingLine;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CLPC billing (BRNB.067): the CBG Fire accounts awaiting payment are billed on a batch whose file
 * (.xlsx or .ods) carries PN number, loan application number, booking date, borrower, originating
 * unit, premium, reference (ARN), BDOI location and amortised Y/N. The transport to CLPC is parked
 * (Q28): the file is downloaded and sent outside the system.
 */
@Service
@Transactional
public class BillingService {

  /** Audit entity type. */
  public static final String ENTITY = "BillingBatch";

  /** Market segment billed through CLPC. */
  public static final String CLPC_SEGMENT = "CBG";

  /** Product line billed through CLPC (Fire). */
  public static final String CLPC_LINE = "PROPERTY";

  /** Columns of the billing file, in order (BRNB.067). */
  public static final List<String> COLUMNS =
      List.of(
          "PN No.",
          "Loan Application No.",
          "Booking Date",
          "Borrower",
          "Originating Unit",
          "Premium",
          "Reference",
          "BDOI Location",
          "Amortised");

  private final BillingBatchRepository batches;
  private final PlacementAccounts accounts;
  private final DocumentNumberService numbers;
  private final DocumentComposer composer;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches billing batches
   * @param accounts account look-ups
   * @param numbers document numbers
   * @param composer spreadsheet rendering
   * @param audit audit trail
   * @param clock clock
   */
  public BillingService(
      BillingBatchRepository batches,
      PlacementAccounts accounts,
      DocumentNumberService numbers,
      DocumentComposer composer,
      AuditTrailService audit,
      Clock clock) {
    this.batches = batches;
    this.accounts = accounts;
    this.numbers = numbers;
    this.composer = composer;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * CBG Fire accounts awaiting payment, paid through BDOI and not yet billed (or reported unpaid).
   *
   * @param companyId company
   * @return candidate accounts, oldest first
   */
  @Transactional(readOnly = true)
  public List<Account> candidates(Long companyId) {
    List<Account> awaiting =
        accounts.all(companyId, AccountStatus.AWAITING_PAYMENT, CLPC_LINE).stream()
            .filter(a -> CLPC_SEGMENT.equals(a.getMarketSegment()) && !a.isDirectPayment())
            .toList();
    if (awaiting.isEmpty()) {
      return awaiting;
    }
    Set<Long> billed =
        batches.alreadyBilled(
            awaiting.stream().map(Account::getId).toList(), BillingItemStatus.UNPAID);
    List<Account> result = awaiting.stream().filter(a -> !billed.contains(a.getId())).toList();
    result.forEach(Account::getPnNumbers);
    return result;
  }

  /**
   * Creates a billing batch.
   *
   * @param companyId company
   * @param arns accounts to bill; empty to bill every candidate
   * @return the batch
   */
  public BillingBatch create(Long companyId, List<String> arns) {
    List<Account> candidates = candidates(companyId);
    List<Account> chosen = choose(candidates, arns);
    if (chosen.isEmpty()) {
      throw new BusinessRuleException(
          "BILLING_NOTHING_TO_BILL", "No CBG Fire account awaiting payment is left to bill");
    }
    LocalDate today = LocalDate.now(clock);
    BillingBatch batch =
        new BillingBatch(companyId, numbers.next("BILL-" + today.getYear()), today);
    chosen.forEach(a -> batch.add(lineOf(a)));
    BillingBatch saved = batches.save(batch);
    audit.record(
        ENTITY,
        saved.getBatchNo(),
        AuditAction.CREATE,
        "CLPC billing batch of "
            + saved.getItemCount()
            + " account(s), premium "
            + saved.getTotalPremium());
    return saved;
  }

  private static List<Account> choose(List<Account> candidates, List<String> arns) {
    if (arns == null || arns.isEmpty()) {
      return candidates;
    }
    Set<String> wanted = new HashSet<>(arns.stream().map(String::strip).toList());
    List<Account> chosen = new ArrayList<>();
    for (Account a : candidates) {
      if (wanted.remove(a.getArn())) {
        chosen.add(a);
      }
    }
    if (!wanted.isEmpty()) {
      throw new BusinessRuleException(
          "BILLING_NOT_CANDIDATE",
          "Not CBG Fire accounts awaiting payment, or already billed: "
              + String.join(", ", wanted));
    }
    return chosen;
  }

  private static BillingLine lineOf(Account a) {
    BigDecimal premium = Objects.requireNonNullElse(a.getPremium().grossPremium(), BigDecimal.ZERO);
    return new BillingLine(
        a.getId(),
        a.getArn(),
        a.getPnNumbers(),
        a.getLoanApplicationNo(),
        a.getPeriodFrom(),
        a.getClientName(),
        a.getSales().team(),
        premium,
        a.getSales().region(),
        a.isMultiYear());
  }

  /**
   * Batches of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  @Transactional(readOnly = true)
  public Page<BillingBatch> batches(Long companyId, Pageable pageable) {
    return batches.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }

  /**
   * One batch with its items.
   *
   * @param id batch
   * @return batch
   */
  @Transactional(readOnly = true)
  public BillingBatch get(Long id) {
    BillingBatch batch =
        batches.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    batch.getItems().forEach(BillingItem::getArn);
    return batch;
  }

  /**
   * The billing file of a batch.
   *
   * @param id batch
   * @param format XLSX or ODS
   * @return file
   */
  public BillingFile file(Long id, String format) {
    BillingBatch batch = get(id);
    SheetSpec sheet = new SheetSpec("CLPC Billing", COLUMNS, rows(batch));
    boolean ods = isOds(format);
    String kind = ods ? "ODS" : "XLSX";
    byte[] content = ods ? OdsSpreadsheetWriter.write(sheet) : composer.xlsx(sheet);
    audit.record(ENTITY, batch.getBatchNo(), AuditAction.EXPORT, "Billing file " + kind);
    return new BillingFile(batch.getBatchNo() + (ods ? ".ods" : ".xlsx"), kind, content);
  }

  private static boolean isOds(String format) {
    return switch (format == null ? "XLSX" : format) {
      case "xlsx", "XLSX" -> false;
      case "ods", "ODS" -> true;
      default ->
          throw new BusinessRuleException(
              "BILLING_FORMAT", "Choose the billing file format XLSX or ODS");
    };
  }

  private static List<List<Object>> rows(BillingBatch batch) {
    return batch.getItems().stream()
        .map(
            i ->
                Arrays.<Object>asList(
                    i.getPnNumbers(),
                    i.getLoanApplicationNo(),
                    i.getBookingDate(),
                    i.getBorrower(),
                    i.getOriginatingUnit(),
                    i.getPremium(),
                    i.getArn(),
                    i.getBdoiLocation(),
                    i.isAmortised()))
        .toList();
  }

  /**
   * A billing file.
   *
   * @param fileName file name
   * @param format XLSX or ODS
   * @param content bytes
   */
  public record BillingFile(String fileName, String format, byte[] content) {

    /** Defensive copy. */
    public BillingFile {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof BillingFile f
          && fileName.equals(f.fileName)
          && format.equals(f.format)
          && Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return Objects.hash(fileName, format, Arrays.hashCode(content));
    }

    @Override
    public String toString() {
      return fileName + " (" + content.length + " bytes)";
    }
  }
}
