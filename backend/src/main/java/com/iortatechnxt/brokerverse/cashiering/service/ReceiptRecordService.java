package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecordRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecordRepository.RecordCriteria;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptSeriesRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordStage;
import com.iortatechnxt.brokerverse.cashiering.service.RecordValidation.Draft;
import com.iortatechnxt.brokerverse.cashiering.service.RecordValidation.Rules;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.FieldValidationException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The AR and OR creation records of BDOI's FRS (FRS.CSH.02.01, 02.02): save with the validations
 * and the number {@code CR-AR-<sequence>} / {@code CR-OR-<sequence>} (status Created), edit while
 * Created or Returned, cancel while Created (Record Cancelled, the number is kept and never given
 * again), and submit for posting (For Posting, the Approvers/Posters are notified). Every change is
 * kept in the audit trail with the values before and after.
 */
@Service
@Transactional
public class ReceiptRecordService {

  /** Audit entity of the records. */
  public static final String ENTITY = "ReceiptRecord";

  /** AR types of non-premium payments (FRS.CSH.02.01.15). */
  public static final Set<String> NON_PREMIUM = Set.of("REFUND", "OTHER_EXPENSES", "AR_INSURANCE");

  /** Permission of the Approvers/Posters. */
  public static final String POSTER = "CASH_APPROVE";

  private final ReceiptRecordRepository records;
  private final ReceiptSeriesRepository series;
  private final RecordNumbers numbers;
  private final CashieringDecisions decisions;
  private final CashieringSettings settings;
  private final RecordFacts facts;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records records
   * @param series receipt series (receipting branches)
   * @param numbers record numbers
   * @param decisions settings of BDOI's decisions
   * @param settings cashiering settings (branches, Head Office)
   * @param facts account currencies and working days
   * @param notifications notices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ReceiptRecordService(
      ReceiptRecordRepository records,
      ReceiptSeriesRepository series,
      RecordNumbers numbers,
      CashieringDecisions decisions,
      CashieringSettings settings,
      RecordFacts facts,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.records = records;
    this.series = series;
    this.numbers = numbers;
    this.decisions = decisions;
    this.settings = settings;
    this.facts = facts;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Saves a new creation record (status Created) after the validations (FRS.CSH.02.01.08.02).
   *
   * @param companyId company
   * @param draft the record as entered
   * @return the saved record with its number
   */
  public ReceiptRecord create(Long companyId, Draft draft) {
    validate(companyId, draft);
    ReceiptRecord record =
        new ReceiptRecord(
            companyId,
            numbers.next(RecordKind.CREATION, draft.kind()),
            RecordKind.CREATION,
            draft.kind());
    record.describe(
        draft.receiptType(), draft.branchId(), draft.party(), draft.tender(), draft.accounts());
    ReceiptRecord saved = records.save(record);
    audit.record(ENTITY, saved.getRecordNo(), AuditAction.CREATE, "Saved: " + summary(saved));
    return saved;
  }

  /**
   * Edits a record in status Created or Returned (FRS.CSH.02.01.13); the modification is logged
   * with the values before and after.
   *
   * @param id record
   * @param draft the record as entered
   * @return the record
   */
  public ReceiptRecord edit(Long id, Draft draft) {
    ReceiptRecord record = get(id);
    if (record.getRecordKind() != RecordKind.CREATION || record.getReceiptKind() != draft.kind()) {
      throw new BusinessRuleException(
          "RECORD_KIND_MISMATCH", record.getRecordNo() + " is not an " + draft.kind() + " record");
    }
    validate(record.getCompanyId(), draft);
    String before = summary(record);
    record.describe(
        draft.receiptType(), draft.branchId(), draft.party(), draft.tender(), draft.accounts());
    audit.record(
        ENTITY,
        record.getRecordNo(),
        AuditAction.UPDATE,
        "Edited. Before: " + before + ". After: " + summary(record));
    return record;
  }

  /**
   * Submits a record for posting (FRS.CSH.02.01.10): status For Posting, assigned to the
   * Approvers/Posters, who are notified.
   *
   * @param id record
   * @return the record
   */
  public ReceiptRecord submit(Long id) {
    ReceiptRecord record = get(id);
    record.submit(currentUser.username(), clock.instant());
    audit.record(ENTITY, record.getRecordNo(), AuditAction.SUBMIT, "Submitted for posting");
    notifications.notifyPermission(
        POSTER,
        new Notice(
            record.getRecordNo() + " for posting",
            what(record) + " of " + record.total() + " waits for posting",
            link(record),
            ENTITY,
            record.getId().toString()),
        "CASH_RECORD_FOR_POSTING");
    return record;
  }

  /**
   * Cancels a saved record (FRS.CSH.02.01.12): only in status Created; the number is kept.
   *
   * @param id record
   * @return the record
   */
  public ReceiptRecord cancelRecord(Long id) {
    ReceiptRecord record = get(id);
    record.cancelRecord();
    audit.record(ENTITY, record.getRecordNo(), AuditAction.DEACTIVATE, "Record cancelled");
    return record;
  }

  /**
   * One record.
   *
   * @param id id
   * @return record with its accounts
   */
  @Transactional(readOnly = true)
  public ReceiptRecord get(Long id) {
    ReceiptRecord record =
        records.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    record.getAccounts();
    return record;
  }

  /**
   * Records of a list, newest first (FRS.CSH.01.03.04).
   *
   * @param companyId company
   * @param kind record kind
   * @param stages statuses
   * @param criteria filters
   * @param pageable page
   * @return records
   */
  @Transactional(readOnly = true)
  public Page<ReceiptRecord> search(
      Long companyId,
      RecordKind kind,
      Collection<RecordStage> stages,
      RecordCriteria criteria,
      Pageable pageable) {
    return records.search(companyId, kind, stages, criteria, pageable);
  }

  /**
   * The creation records the current user saved on a day (FRS.CSH.02.01.16).
   *
   * @param companyId company
   * @param day business day
   * @return records, oldest first
   */
  @Transactional(readOnly = true)
  public List<ReceiptRecord> dayList(Long companyId, LocalDate day) {
    return records.dayList(
        companyId,
        RecordKind.CREATION,
        currentUser.username(),
        BusinessClock.startOf(day),
        BusinessClock.startOf(day.plusDays(1)));
  }

  /**
   * Branches a receipt kind may be issued from in the current year: active branches with a usable
   * series; OR only from Head Office (FRS.CSH.02.01.02, 02.03.04).
   *
   * @param companyId company
   * @param kind AR or OR
   * @return branches
   */
  @Transactional(readOnly = true)
  public List<Branch> receiptingBranches(Long companyId, ReceiptKind kind) {
    List<Long> usable =
        series.usableBranchIds(
            companyId, kind, RecordStatus.ACTIVE, BusinessClock.today(clock).getYear());
    return settings.branches(companyId).stream()
        .filter(b -> b.getRecordStatus() == RecordStatus.ACTIVE)
        .filter(b -> usable.contains(b.getId()))
        .filter(b -> kind == ReceiptKind.AR || b.isHeadOffice())
        .toList();
  }

  private void validate(Long companyId, Draft draft) {
    boolean nonPremium =
        draft.kind() == ReceiptKind.AR && NON_PREMIUM.contains(draft.receiptType());
    Rules rules =
        new Rules(
            decisions.remarksRequired(),
            decisions.maxPaidAmount(),
            decisions.checkDateRule()
                ? facts.workingDaysBefore(companyId, decisions.checkHoldingDays())
                : null,
            decisions.checkHoldingDays(),
            nonPremium,
            facts.currencies(companyId, draft.accounts()));
    Map<String, String> errors = RecordValidation.errors(draft, rules);
    if (draft.kind() == ReceiptKind.OR && draft.branchId() != null) {
      Long office = settings.headOffice(companyId).getId();
      if (!office.equals(draft.branchId())) {
        errors.put("branchId", "Official receipts are issued by Head Office only");
      }
    }
    if (!errors.isEmpty()) {
      throw new FieldValidationException(
          "RECORD_INVALID", String.join("; ", errors.values()), errors);
    }
  }

  /**
   * What a record is, for texts.
   *
   * @param record record
   * @return for example "AR creation record CR-AR-000012"
   */
  static String what(ReceiptRecord record) {
    String kind =
        switch (record.getRecordKind()) {
          case CREATION -> "creation";
          case CANCELLATION -> "cancellation";
          case REINSTATEMENT -> "reinstatement";
        };
    return record.getReceiptKind() + " " + kind + " record " + record.getRecordNo();
  }

  /**
   * The screen of a record.
   *
   * @param record record
   * @return link
   */
  static String link(ReceiptRecord record) {
    return "/cashiering/records/" + record.getId();
  }

  private static String summary(ReceiptRecord r) {
    var party = r.getParty();
    var tender = r.getTender();
    return String.join(
        ", ",
        "type " + r.getReceiptType(),
        "branch " + r.getBranchId(),
        "payor " + (party == null ? "" : party.payorName()),
        "amount " + (tender == null ? "" : tender.currency() + " " + tender.amount()),
        "bank account " + (tender == null ? "" : tender.bankAccount()),
        "accounts " + r.getAccounts());
  }
}
