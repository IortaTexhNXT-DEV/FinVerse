package com.iortatechnxt.finverse.receivables.service;

import com.iortatechnxt.finverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.finverse.accounting.service.BusinessEvent;
import com.iortatechnxt.finverse.receivables.domain.PayerType;
import com.iortatechnxt.finverse.receivables.domain.Receipt;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Accounts for receipts through the accounting engine (the module never picks GL accounts, except
 * the bank and income accounts passed as {@code @BANK} / {@code @INCOME} roles).
 *
 * <ul>
 *   <li>Policyholder / intermediary: PREMIUM_RECEIPT for the applied part (Dr bank, Cr premium
 *       receivable) and PREMIUM_DEPOSIT for the unapplied part (Dr bank, Cr premium deposits).
 *       Applying money on account later posts UNAPPLIED_APPLICATION (Dr premium deposits, Cr
 *       premium receivable).
 *   <li>Reinsurer: RI_SETTLEMENT_RECEIPT for the whole amount.
 *   <li>Other payer: MISC_RECEIPT (Dr bank, Cr the chosen income account).
 * </ul>
 *
 * Cancellations and bounced cheques publish the same events with negative amounts (posted on the
 * opposite side), with their own idempotency keys.
 */
@Component
public class ReceiptAccounting {

  /** Source module of receivables postings and open items. */
  public static final String MODULE = "RECEIVABLES";

  private static final String AMOUNT = "AMOUNT";
  private static final String PREMIUM_RECEIPT = "PREMIUM_RECEIPT";
  private static final String PREMIUM_DEPOSIT = "PREMIUM_DEPOSIT";
  private static final String APPLIED = ":APPLIED";
  private static final String UNAPPLIED = ":UNAPPLIED";
  private static final String REVERSAL = ":REV";
  private static final int MAX_NARRATION = 240;

  private final AccountingEventPublisher publisher;

  /**
   * Creates the component.
   *
   * @param publisher accounting engine
   */
  public ReceiptAccounting(AccountingEventPublisher publisher) {
    this.publisher = publisher;
  }

  /**
   * Source reference of a receipt (idempotency key prefix and open item source).
   *
   * @param r receipt
   * @return key
   */
  public static String key(Receipt r) {
    return "RCPT:" + r.getId();
  }

  /**
   * Posts an approved receipt.
   *
   * @param r receipt
   * @param applied part applied to debit notes
   * @return batch number of the first journal posted
   */
  public String postApproval(Receipt r, BigDecimal applied) {
    return post(r, applied, r.getAmount().subtract(applied), r.getReceiptDate(), "");
  }

  /**
   * Reverses the postings of a receipt (cancellation or bounced cheque), using its current applied
   * / unapplied split so later applications are reversed too.
   *
   * @param r receipt
   * @param date reversal date
   * @return batch number of the first reversal journal
   */
  public String postReversal(Receipt r, LocalDate date) {
    return post(r, r.getAppliedAmount().negate(), r.unapplied().negate(), date, REVERSAL);
  }

  /**
   * Posts the application of money held on account (premium deposit) to debit notes.
   *
   * @param r receipt
   * @param value amount applied
   * @param date application date
   * @param sequence application number (idempotency)
   */
  public void postApplication(Receipt r, BigDecimal value, LocalDate date, int sequence) {
    publish(r, "UNAPPLIED_APPLICATION", value, date, key(r) + ":APPLY:" + sequence, Map.of());
  }

  private String post(
      Receipt r, BigDecimal applied, BigDecimal unapplied, LocalDate date, String suffix) {
    String base = key(r);
    PayerType type = r.getPayerType();
    if (type.usesPremiumDeposit()) {
      String first = null;
      if (applied.signum() != 0) {
        first = publish(r, PREMIUM_RECEIPT, applied, date, base + APPLIED + suffix, Map.of());
      }
      if (unapplied.signum() != 0) {
        String batch =
            publish(r, PREMIUM_DEPOSIT, unapplied, date, base + UNAPPLIED + suffix, Map.of());
        first = first == null ? batch : first;
      }
      return first;
    }
    BigDecimal total = applied.add(unapplied);
    if (type == PayerType.REINSURER) {
      return publish(r, "RI_SETTLEMENT_RECEIPT", total, date, base + suffix, Map.of());
    }
    return publish(
        r, "MISC_RECEIPT", total, date, base + suffix, Map.of("INCOME", r.getIncomeAccountCode()));
  }

  private String publish(
      Receipt r,
      String eventType,
      BigDecimal value,
      LocalDate date,
      String sourceReference,
      Map<String, String> roles) {
    Map<String, String> accounts = new HashMap<>(roles);
    accounts.put("BANK", r.getBankAccountCode());
    return publisher
        .publish(
            new BusinessEvent(
                eventType,
                r.getCompanyId(),
                r.getBranchId(),
                date,
                r.getCurrency(),
                MODULE,
                sourceReference,
                r.getReceiptNo(),
                r.getPayerType().hasParty() ? r.getPartyCode() : null,
                null,
                null,
                narration(r),
                Map.of(AMOUNT, value),
                accounts))
        .getBatchNo();
  }

  /**
   * Narration printed on the ledger: receipt number, payer and instrument (the cheque number lets
   * the bank reconciliation match on reference).
   *
   * @param r receipt
   * @return narration
   */
  static String narration(Receipt r) {
    StringBuilder sb = new StringBuilder("Receipt ").append(r.getReceiptNo());
    if (r.getInstrumentNo() != null) {
      sb.append(' ').append(r.getMode().name()).append(' ').append(r.getInstrumentNo());
    }
    sb.append(" - ").append(r.getPayerName());
    return sb.length() > MAX_NARRATION ? sb.substring(0, MAX_NARRATION) : sb.toString();
  }
}
