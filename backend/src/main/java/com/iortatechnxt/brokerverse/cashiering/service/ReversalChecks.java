package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptActionRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecordRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordStage;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.ReinstatementType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordReason;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The validations of a cancellation or reinstatement record (FRS.CSH.03.01.06, 04.01.07) with the
 * messages of Appendix F: ARs only or ORs only, a reason (with its text for Others), the reasons of
 * checks for a check payment (setting {@code CASH_CHECK_CANCEL_REASONS}, Appendix R, C21), no other
 * record waiting for the receipt, no disposition in process, the type and accounts of a
 * reinstatement and the Unit Head of a premium reinstatement (CSHID.005).
 */
@Component
@Transactional(readOnly = true)
public class ReversalChecks {

  /** Reasons of a cancellation. */
  static final String CANCEL_REASON = "RECEIPT_CANCEL_REASON";

  /** Reasons of a reinstatement. */
  static final String REINSTATE_REASON = "REINSTATEMENT_REASON";

  private static final Set<RecordStage> OPEN =
      Set.of(RecordStage.CREATED, RecordStage.FOR_POSTING, RecordStage.RETURNED);
  private static final Set<RecordKind> REVERSALS =
      Set.of(RecordKind.CANCELLATION, RecordKind.REINSTATEMENT);
  private static final List<String> OPEN_ACTION_STAGES = List.of("REQUESTED", "FOR_APPROVAL");
  private static final int REMARKS_LENGTH = 100;

  private final ReceiptRecordRepository records;
  private final CashReceiptRepository receipts;
  private final ReceiptActionRepository actions;
  private final ReceiptReversalService reversal;
  private final SystemParameterService parameters;
  private final LovService lovs;
  private final Clock clock;

  /**
   * Creates the checks.
   *
   * @param records records (waiting records of a receipt)
   * @param receipts receipts
   * @param actions cancellations and reinstatements of the earlier flow (waiting requests)
   * @param reversal dispositions in process
   * @param parameters business parameters (reasons allowed for checks)
   * @param lovs reason lists
   * @param clock clock
   */
  public ReversalChecks(
      ReceiptRecordRepository records,
      CashReceiptRepository receipts,
      ReceiptActionRepository actions,
      ReceiptReversalService reversal,
      SystemParameterService parameters,
      LovService lovs,
      Clock clock) {
    this.records = records;
    this.receipts = receipts;
    this.actions = actions;
    this.reversal = reversal;
    this.parameters = parameters;
    this.lovs = lovs;
    this.clock = clock;
  }

  List<Receipt> selection(List<Long> receiptIds) {
    if (receiptIds == null || receiptIds.isEmpty()) {
      throw new BusinessRuleException("RECORD_NO_RECEIPT", "Select at least one AR or OR number");
    }
    List<Receipt> selected = receipts.findByIdInOrderByIdAsc(receiptIds);
    long kinds = selected.stream().map(Receipt::getKind).distinct().count();
    if (kinds > 1) {
      throw new BusinessRuleException(
          "RECORD_MIXED_RECEIPTS", "Select AR numbers only or OR numbers only");
    }
    return selected;
  }

  void checkCancellation(Receipt receipt, RecordReason reason) {
    if (receipt.getStatus() == ReceiptStatus.CANCELLED) {
      throw new BusinessRuleException(
          "RECEIPT_ALREADY_CANCELLED",
          "Receipt " + receipt.getReceiptNo() + " is already cancelled");
    }
    requireNothingWaiting(receipt);
    requireReason(CANCEL_REASON, reason);
    requireCheckReason(receipt, reason);
    reversal.requireCancellable(receipt);
  }

  void checkReinstatement(Receipt receipt, RecordReason reason, List<String> accounts) {
    if (receipt.getStatus() == ReceiptStatus.CANCELLED) {
      throw new BusinessRuleException(
          "RECEIPT_NOT_ISSUED",
          "Receipt "
              + receipt.getReceiptNo()
              + " is cancelled; only an issued receipt is reinstated here");
    }
    requireNothingWaiting(receipt);
    checkReinstatementDetails(receipt, reason, accounts);
  }

  void checkReinstatementDetails(Receipt receipt, RecordReason reason, List<String> accounts) {
    requireReason(REINSTATE_REASON, reason);
    if (reason.reinstatementType() == null) {
      throw new BusinessRuleException(
          "RECORD_REINSTATEMENT_TYPE", "Select the type of reinstatement");
    }
    if (reason.reinstatementType() == ReinstatementType.PARTIAL) {
      requirePartial(receipt, accounts);
    }
    requireFields(receipt, reason);
  }

  private static void requireFields(Receipt receipt, RecordReason reason) {
    if (reason.reasonText() != null && reason.reasonText().length() > REMARKS_LENGTH) {
      throw new BusinessRuleException(
          "RECORD_REMARKS_LENGTH", "Remarks can have at most " + REMARKS_LENGTH + " characters");
    }
    if (receipt.getKind() == ReceiptKind.AR && blank(reason.unitHead())) {
      throw new BusinessRuleException("RECORD_UNIT_HEAD", "Enter the Unit Head");
    }
  }

  private static void requirePartial(Receipt receipt, List<String> accounts) {
    if (receipt.getKind() == ReceiptKind.OR) {
      throw new BusinessRuleException(
          "RECORD_PARTIAL_OR", "Partial reinstatement is not available for an OR");
    }
    if (accounts == null || accounts.isEmpty()) {
      throw new BusinessRuleException(
          "RECORD_PARTIAL_ACCOUNTS", "Select at least one account for a partial reinstatement");
    }
  }

  private void requireNothingWaiting(Receipt receipt) {
    boolean waiting =
        records.existsByReceiptIdAndRecordKindInAndStageIn(receipt.getId(), REVERSALS, OPEN)
            || actions.existsByReceiptIdAndStageIn(receipt.getId(), OPEN_ACTION_STAGES);
    if (waiting) {
      throw new BusinessRuleException(
          "RECORD_WAITING",
          "Receipt "
              + receipt.getReceiptNo()
              + " already has a cancellation or reinstatement record waiting");
    }
  }

  void requireReason(String lov, RecordReason reason) {
    if (reason == null || blank(reason.reasonCode())) {
      throw new BusinessRuleException("RECEIPT_REASON_REQUIRED", "Select a reason");
    }
    lovs.requireValid(lov, reason.reasonCode(), BusinessClock.today(clock));
    if (reason.reasonCode().endsWith("_OTHERS") && blank(reason.reasonText())) {
      throw new BusinessRuleException(
          "RECEIPT_REASON_TEXT_REQUIRED", "Specify the reason when 'Others' is selected");
    }
  }

  /** C21: a check payment is cancelled only for the reasons of checks. */
  void requireCheckReason(Receipt receipt, RecordReason reason) {
    List<String> allowed = parameters.items("CASH_CHECK_CANCEL_REASONS");
    if (receipt.getMode() == PaymentMode.CHECK
        && !allowed.isEmpty()
        && !allowed.contains(reason.reasonCode())) {
      throw new BusinessRuleException(
          "RECORD_CHECK_REASON",
          "The reason "
              + lovs.label(CANCEL_REASON, reason.reasonCode())
              + " is not allowed for a check payment");
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
