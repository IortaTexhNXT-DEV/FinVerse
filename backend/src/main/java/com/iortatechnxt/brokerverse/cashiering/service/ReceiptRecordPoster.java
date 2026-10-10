package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecordRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordStage;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.TenderType;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Posting and return of BDOI's records by the Approver/Poster (FRS.CSH.02.05, 03.01.08, 04.01.09):
 * one or several records For Posting are posted together, each on its own, so that a record that
 * fails (for example in a depleted series) stays For Posting with its reason while the others are
 * posted; check payments are posted before cash payments (setting {@code CASH_CHECK_BEFORE_CASH}).
 * The creator of a record can neither post nor return it.
 */
@Service
public class ReceiptRecordPoster {

  /** BDOI's message of a depleted series (FRS.CSH.02.05.09.02). */
  public static final String DEPLETED =
      "Receipt Number series for this Branch is depleted. Contact your Administrator.";

  private final ReceiptRecordRepository records;
  private final RecordIssuer issuer;
  private final RecordReversals reversals;
  private final CashieringDecisions decisions;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final TransactionTemplate tx;

  /**
   * Creates the poster.
   *
   * @param records records
   * @param issuer issuance of the AR / OR of a creation record
   * @param reversals posting of cancellation and reinstatement records
   * @param decisions settings
   * @param notifications notices
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   * @param txManager transactions (one per record)
   */
  public ReceiptRecordPoster(
      ReceiptRecordRepository records,
      RecordIssuer issuer,
      RecordReversals reversals,
      CashieringDecisions decisions,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock,
      PlatformTransactionManager txManager) {
    this.records = records;
    this.issuer = issuer;
    this.reversals = reversals;
    this.decisions = decisions;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
  }

  /**
   * Posts the records selected (FRS.CSH.02.05.04 to 02.05.06), each in its own transaction.
   *
   * @param ids records For Posting
   * @return the outcome of each record, in posting order
   */
  public List<Outcome> post(Collection<Long> ids) {
    List<ReceiptRecord> selected = new ArrayList<>(records.findAllById(ids));
    if (selected.size() != ids.size()) {
      throw new ResourceNotFoundException(ReceiptRecordService.ENTITY, ids);
    }
    selected.sort(order());
    List<Outcome> outcomes = new ArrayList<>();
    for (ReceiptRecord record : selected) {
      outcomes.add(postOne(record.getId()));
    }
    return outcomes;
  }

  private Comparator<ReceiptRecord> order() {
    Comparator<ReceiptRecord> byId = Comparator.comparing(ReceiptRecord::getId);
    if (!decisions.checkBeforeCash()) {
      return byId;
    }
    return Comparator.comparing((ReceiptRecord r) -> !isCheck(r)).thenComparing(byId);
  }

  private static boolean isCheck(ReceiptRecord r) {
    return r.getTender() != null && r.getTender().tenderType() == TenderType.CHECK;
  }

  private Outcome postOne(Long id) {
    try {
      return required(tx.execute(s -> postInTransaction(id)));
    } catch (BusinessRuleException ex) {
      return notPosted(
          id, "RECEIPT_SERIES_DEPLETED".equals(ex.getCode()) ? DEPLETED : ex.getMessage());
    } catch (ResourceNotFoundException | IllegalStateException ex) {
      return notPosted(id, ex.getMessage());
    }
  }

  private Outcome notPosted(Long id, String message) {
    String no = required(tx.execute(s -> failed(id, message)));
    return new Outcome(id, no, false, message, null);
  }

  private Outcome postInTransaction(Long id) {
    ReceiptRecord record = forPosting(id);
    String poster = currentUser.username();
    if (CurrentUser.sameUser(poster, record.getCreatedBy())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "The requester cannot approve " + record.getRecordNo());
    }
    String receiptNo =
        record.getRecordKind() == RecordKind.CREATION
            ? issuer.issue(record, poster, clock.instant())
            : reversals.post(record, poster, clock.instant());
    audit.record(
        ReceiptRecordService.ENTITY,
        record.getRecordNo(),
        AuditAction.POST,
        record.label() + ": " + record.getReceiptKind() + " " + receiptNo);
    notifications.notifyUser(
        record.getCreatedBy(),
        new Notice(
            record.getRecordNo() + " posted",
            record.label() + ": " + record.getReceiptKind() + " " + receiptNo,
            ReceiptRecordService.link(record),
            ReceiptRecordService.ENTITY,
            id.toString()),
        "CASH_RECORD_DECIDED");
    return new Outcome(id, record.getRecordNo(), true, record.label(), receiptNo);
  }

  private String failed(Long id, String message) {
    ReceiptRecord record = forPosting(id);
    record.postingFailed(message);
    return record.getRecordNo();
  }

  /**
   * Returns a record to its creator with a reason (FRS.CSH.02.05.07); the creator is notified.
   *
   * @param id record For Posting
   * @param reason reason of the return
   * @return the record
   */
  public ReceiptRecord returnToCreator(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("RECORD_RETURN_REASON", "Enter the reason for the return");
    }
    return required(
        tx.execute(
            s -> {
              ReceiptRecord record = forPosting(id);
              if (CurrentUser.sameUser(currentUser.username(), record.getCreatedBy())) {
                throw new BusinessRuleException(
                    "MAKER_CHECKER_VIOLATION",
                    "The requester cannot return " + record.getRecordNo());
              }
              record.returnToCreator(reason.strip());
              audit.record(
                  ReceiptRecordService.ENTITY,
                  record.getRecordNo(),
                  AuditAction.REJECT,
                  "Returned: " + reason.strip());
              notifications.notifyUser(
                  record.getCreatedBy(),
                  new Notice(
                      record.getRecordNo() + " returned",
                      "Reason: " + reason.strip(),
                      ReceiptRecordService.link(record),
                      ReceiptRecordService.ENTITY,
                      id.toString()),
                  "CASH_RECORD_DECIDED");
              return record;
            }));
  }

  private ReceiptRecord forPosting(Long id) {
    ReceiptRecord record =
        records
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ReceiptRecordService.ENTITY, id));
    if (record.getStage() != RecordStage.FOR_POSTING) {
      throw new BusinessRuleException(
          "RECORD_NOT_FOR_POSTING", record.getRecordNo() + " is " + record.label());
    }
    return record;
  }

  private static <T> T required(T value) {
    if (value == null) {
      throw new IllegalStateException("The posting transaction returned nothing");
    }
    return value;
  }

  /**
   * The outcome of one record of a posting.
   *
   * @param id record
   * @param recordNo record number
   * @param posted whether it was posted
   * @param message status reached or reason of the failure
   * @param receiptNo AR / OR number issued, cancelled or reinstated; null when not posted
   */
  public record Outcome(
      Long id, String recordNo, boolean posted, String message, String receiptNo) {}
}
