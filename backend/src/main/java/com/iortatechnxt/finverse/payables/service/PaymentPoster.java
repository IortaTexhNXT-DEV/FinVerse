package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.finverse.accounting.service.BusinessEvent;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.payables.domain.BankAccount;
import com.iortatechnxt.finverse.payables.domain.PaymentMode;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.finverse.payables.domain.VoucherAllocation;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * GL and sub-ledger side of payment vouchers.
 *
 * <p>Posting: the event of the voucher's category ({@code SUPPLIER_PAYMENT}, {@code
 * COMMISSION_PAYMENT}, {@code CLAIM_PAYMENT}, {@code RI_SETTLEMENT_PAYMENT}, {@code
 * PREMIUM_REFUND_PAYMENT}) with component AMOUNT and role {@code BANK}: Dr the party's payable
 * control account / Cr bank. For a post-dated cheque the {@code BANK} role is the bank account's
 * PDC clearing account instead (the liability moves from the party to "PDC issued" until maturity).
 * A DEBIT open item (document type PAYMENT) is recorded and matched against the paid CREDIT items.
 *
 * <p>Reversal (void of an unpresented cheque, cancellation of a PDC): the same event with a
 * negative AMOUNT (Dr bank or PDC clearing / Cr payable). In the sub-ledger the payment's matches
 * are undone ({@link OpenItemService#unmatchAll}), so the paid invoices are open again under their
 * own numbers and dates, and the payment item is neutralised by a PAYMENT_REVERSAL CREDIT item
 * matched against it (the receipts' cancellation works the same way). Ageing and statements
 * therefore show the payables exactly as before the payment.
 */
@Component
public class PaymentPoster {

  /** Open item document type of payments. */
  public static final String PAYMENT_DOCUMENT = "PAYMENT";

  /** Open item document type neutralising a voided or cancelled payment. */
  public static final String REVERSAL_DOCUMENT = "PAYMENT_REVERSAL";

  private static final String AMOUNT = "AMOUNT";
  private static final String BANK = "BANK";
  private static final String REF = "PV:";
  private static final String VOID_SUFFIX = ":VOID";
  private static final int NARRATION_MAX = 250;

  private final AccountingEventPublisher publisher;
  private final OpenItemService openItems;
  private final PayablesSupport support;

  /**
   * Creates the poster.
   *
   * @param publisher accounting engine
   * @param openItems sub-ledger
   * @param support shared helpers
   */
  public PaymentPoster(
      AccountingEventPublisher publisher, OpenItemService openItems, PayablesSupport support) {
    this.publisher = publisher;
    this.openItems = openItems;
    this.support = support;
  }

  /**
   * GL account credited by the payment: the bank, or the PDC clearing account for PDCs.
   *
   * @param voucher voucher
   * @param bank bank account
   * @return account code
   */
  public static String creditAccount(PaymentVoucher voucher, BankAccount bank) {
    if (voucher.getPaymentMode() != PaymentMode.PDC) {
      return bank.getGlAccountCode();
    }
    if (bank.getPdcClearingAccountCode() == null) {
      throw new BusinessRuleException(
          "NO_PDC_CLEARING_ACCOUNT",
          "Bank account " + bank.getCode() + " has no PDC clearing account");
    }
    return bank.getPdcClearingAccountCode();
  }

  /**
   * Posts an approved voucher, records its DEBIT item and matches the paid items.
   *
   * @param voucher voucher being approved
   * @param bank bank account
   * @return posting facts (journal, payment item, base amount)
   */
  public PostedPayment post(PaymentVoucher voucher, BankAccount bank) {
    String batchNo =
        publisher
            .publish(
                event(
                    voucher,
                    bank,
                    voucher.getAmount(),
                    REF + voucher.getId(),
                    voucher.getNarration(),
                    voucher.getVoucherDate()))
            .getBatchNo();
    BigDecimal base =
        support.toBase(
            voucher.getCompanyId(),
            voucher.getCurrency(),
            voucher.getAmount(),
            voucher.getVoucherDate());
    OpenItem payment =
        openItems.record(
            new OpenItemValues(
                voucher.getCompanyId(),
                voucher.getBranchId(),
                voucher.getPartyId(),
                voucher.getPartyCode(),
                ItemDirection.DEBIT,
                PAYMENT_DOCUMENT,
                voucher.getVoucherNo(),
                voucher.getVoucherDate(),
                voucher.getVoucherDate(),
                voucher.getCurrency(),
                voucher.getAmount(),
                base,
                PayablesSupport.MODULE,
                REF + voucher.getId(),
                batchNo,
                "Payment " + voucher.getVoucherNo()));
    for (VoucherAllocation a : voucher.getAllocations()) {
      openItems.match(payment.getId(), a.getOpenItemId(), a.getAmount(), voucher.getVoucherDate());
    }
    return new PostedPayment(batchNo, payment.getId(), base);
  }

  /**
   * Reverses an approved voucher: posts the reversal, re-opens the paid items and neutralises the
   * payment item.
   *
   * @param voucher approved voucher
   * @param bank bank account
   * @param date reversal date (not before the voucher date)
   * @param reason reason
   * @return reversal journal batch number
   */
  public String reverse(PaymentVoucher voucher, BankAccount bank, LocalDate date, String reason) {
    if (date.isBefore(voucher.getVoucherDate())) {
      throw new BusinessRuleException("INVALID_DATE", "Reversal precedes the payment date");
    }
    String narration = "Reversal of " + voucher.getVoucherNo() + ": " + reason;
    String batchNo =
        publisher
            .publish(
                event(
                    voucher,
                    bank,
                    voucher.getAmount().negate(),
                    REF + voucher.getId() + VOID_SUFFIX,
                    narration,
                    date))
            .getBatchNo();
    OpenItem payment = openItems.get(voucher.getOpenItemId());
    openItems.unmatchAll(payment.getId(), narration);
    OpenItem reversal = openItems.record(reversalItem(voucher, payment, date, batchNo, narration));
    openItems.match(payment.getId(), reversal.getId(), payment.getAmount(), date);
    return batchNo;
  }

  private static OpenItemValues reversalItem(
      PaymentVoucher voucher, OpenItem payment, LocalDate date, String batchNo, String narration) {
    return new OpenItemValues(
        payment.getCompanyId(),
        payment.getBranchId(),
        payment.getPartyId(),
        payment.getPartyCode(),
        ItemDirection.CREDIT,
        REVERSAL_DOCUMENT,
        voucher.getVoucherNo() + "-VOID",
        date,
        date,
        payment.getCurrency(),
        payment.getAmount(),
        payment.getBaseAmount(),
        PayablesSupport.MODULE,
        REF + voucher.getId() + VOID_SUFFIX,
        batchNo,
        narration.length() > NARRATION_MAX ? narration.substring(0, NARRATION_MAX) : narration);
  }

  private static BusinessEvent event(
      PaymentVoucher v,
      BankAccount bank,
      BigDecimal amount,
      String sourceReference,
      String narration,
      LocalDate date) {
    String text =
        narration == null || narration.isBlank() ? "Payment to " + v.getPayeeName() : narration;
    return new BusinessEvent(
        v.getCategory().eventType(),
        v.getCompanyId(),
        v.getBranchId(),
        date,
        v.getCurrency(),
        PayablesSupport.MODULE,
        sourceReference,
        v.getVoucherNo(),
        v.getPartyCode(),
        null,
        null,
        text.length() > NARRATION_MAX ? text.substring(0, NARRATION_MAX) : text,
        Map.of(AMOUNT, amount),
        Map.of(BANK, creditAccount(v, bank)));
  }

  /**
   * Facts of a posted payment.
   *
   * @param batchNo journal batch
   * @param openItemId DEBIT open item
   * @param baseAmount amount in base currency
   */
  public record PostedPayment(String batchNo, Long openItemId, BigDecimal baseAmount) {}
}
