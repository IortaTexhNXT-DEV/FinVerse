package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The settings that carry the points BDOI decides in Appendix R of the Operations Cashiering FRS
 * v3.2. Each setting keeps both readings selectable; the default is BDOI's FRS unless the BRD reads
 * otherwise.
 */
@Component
@Transactional(readOnly = true)
public class CashieringDecisions {

  /** Receipt kinds with the posting step (C1). */
  public static final String POSTING_STEP = "CASH_POSTING_STEP_RECEIPTS";

  /** Scope of Reinstate (C2). */
  public static final String REINSTATEMENT_SCOPE = "CASH_REINSTATEMENT_SCOPE";

  /** Record number format (C3). */
  public static final String RECORD_NO_FORMAT = "CASH_RECORD_NO_FORMAT";

  /** Check date rule at receipting (C7). */
  public static final String CHECK_DATE_RULE = "CASH_CHECK_DATE_RULE";

  /** Remarks required (C25). */
  public static final String REMARKS_REQUIRED = "CASH_AR_REMARKS_REQUIRED";

  /** When the OR of an insurer settlement is issued (C12). */
  public static final String SETTLEMENT_OR_TRIGGER = "CASH_SETTLEMENT_OR_TRIGGER";

  private static final String YES = "Y";
  private static final String ON = "ON";
  private static final int DEFAULT_HOLDING_DAYS = 4;
  private static final String DEFAULT_MAX = "1000000000.00";

  private final SystemParameterService parameters;

  /**
   * Creates the settings.
   *
   * @param parameters business parameters
   */
  public CashieringDecisions(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  /**
   * Whether a receipt kind is issued only when a second user posts its creation record (C1).
   *
   * @param kind AR or OR
   * @return true with the posting step
   */
  public boolean postingStep(ReceiptKind kind) {
    return parameters.items(POSTING_STEP).stream()
        .map(s -> s.strip().toUpperCase(Locale.ROOT))
        .anyMatch(kind.name()::equals);
  }

  /**
   * Refuses the direct issuance of a receipt kind that has the posting step (C1): it is created as
   * a creation record and issued when a second user posts it.
   *
   * @param kind AR or OR
   */
  public void requireDirectIssue(ReceiptKind kind) {
    if (postingStep(kind)) {
      throw new BusinessRuleException(
          "RECEIPT_POSTING_STEP",
          "An "
              + kind
              + " is issued when its creation record is posted: save it with Create "
              + kind
              + " and submit it for posting");
    }
  }

  /**
   * Whether Reinstate reverses the payment of an issued receipt (C2, BDOI's reading).
   *
   * @return true for ISSUED or BOTH
   */
  public boolean reinstatesIssuedReceipts() {
    return !"CANCELLED".equals(scope());
  }

  /**
   * Whether Reinstate restores a cancelled receipt (C2, the BRD's reading kept).
   *
   * @return true for CANCELLED or BOTH
   */
  public boolean reinstatesCancelledReceipts() {
    return !"ISSUED".equals(scope());
  }

  private String scope() {
    return text(REINSTATEMENT_SCOPE, "BOTH");
  }

  /**
   * Whether cancellations follow BDOI's accounting (C9): the payment of a remitted AR becomes an AR
   * Insurer Refund and the payment of a cancelled OR a Commission Receivable payment.
   *
   * @return true for AR_INSURER_REFUND (the default)
   */
  public boolean bdoiCancellation() {
    return "AR_INSURER_REFUND".equals(text("CASH_REMITTED_CANCELLATION", "AR_INSURER_REFUND"));
  }

  /**
   * The record number format (C3).
   *
   * @return format with the tokens {PREFIX}, {TYPE}, {YEAR} and {SEQ}
   */
  public String recordNumberFormat() {
    String value = parameters.text(RECORD_NO_FORMAT, "").strip();
    return value.isEmpty() ? "{PREFIX}-{TYPE}-{SEQ}" : value;
  }

  /**
   * Whether a check dated within the holding period is refused at receipting (C7).
   *
   * @return true when the rule is on
   */
  public boolean checkDateRule() {
    return ON.equals(text(CHECK_DATE_RULE, ON));
  }

  /**
   * Working days of the check holding period (C7).
   *
   * @return days, default 4
   */
  public int checkHoldingDays() {
    return parameters.intValue("CASH_CHECK_HOLDING_DAYS", DEFAULT_HOLDING_DAYS);
  }

  /**
   * Whether remarks are required on a creation record (C25).
   *
   * @return true when required
   */
  public boolean remarksRequired() {
    return YES.equals(text(REMARKS_REQUIRED, YES));
  }

  /**
   * Highest paid amount of a creation record.
   *
   * @return amount, default 1,000,000,000.00
   */
  public BigDecimal maxPaidAmount() {
    return new BigDecimal(text("CASH_MAX_PAID_AMOUNT", DEFAULT_MAX));
  }

  /**
   * Whether check payments are posted before cash payments in one posting run (C6).
   *
   * @return true by default
   */
  public boolean checkBeforeCash() {
    return YES.equals(text("CASH_CHECK_BEFORE_CASH", YES));
  }

  /**
   * Whether the OR of an insurer settlement sent to Disbursement waits for Disbursement's approval
   * (C12).
   *
   * @return true for DISBURSEMENT_APPROVAL (the default), false for REMITTANCE_APPROVAL
   */
  public boolean orAtDisbursementApproval() {
    return !"REMITTANCE_APPROVAL".equals(text(SETTLEMENT_OR_TRIGGER, "DISBURSEMENT_APPROVAL"));
  }

  /**
   * A text setting, upper case, the fallback when blank.
   *
   * @param key key
   * @param fallback value when missing or blank
   * @return value
   */
  String text(String key, String fallback) {
    String value = parameters.text(key, "").strip().toUpperCase(Locale.ROOT);
    return value.isEmpty() ? fallback : value;
  }
}
