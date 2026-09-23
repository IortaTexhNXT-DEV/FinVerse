package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.finverse.accounting.service.BusinessEvent;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
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
 * negative AMOUNT (Dr bank or PDC clearing / Cr payable). The sub-ledger is append-only, so the
 * matches of the original payment are kept and each paid item is <em>reinstated</em> as a new
 * CREDIT open item with the original document type, number and dates for the amount that had been
 * paid; ageing and statements therefore show the payable again exactly as before the payment.
 */
@Component
public class PaymentPoster {

  /** Open item document type of payments. */
  public static final String PAYMENT_DOCUMENT = "PAYMENT";

  private static final String AMOUNT = "AMOUNT";
  private static final String BANK = "BANK";
  private static final String REF = "PV:";
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
   * Reverses an approved voucher and reinstates the paid items.
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
                    REF + voucher.getId() + ":VOID",
                    narration,
                    date))
            .getBatchNo();
    for (VoucherAllocation a : voucher.getAllocations()) {
      reinstate(voucher, a, batchNo, date);
    }
    return batchNo;
  }

  private void reinstate(
      PaymentVoucher voucher, VoucherAllocation a, String batchNo, LocalDate date) {
    OpenItem original = openItems.get(a.getOpenItemId());
    BigDecimal base =
        Money.round(
            original
                .getBaseAmount()
                .multiply(a.getAmount())
                .divide(original.getAmount(), Money.RATE_SCALE, Money.ROUNDING));
    openItems.record(
        new OpenItemValues(
            original.getCompanyId(),
            original.getBranchId(),
            original.getPartyId(),
            original.getPartyCode(),
            ItemDirection.CREDIT,
            original.getDocumentType(),
            original.getDocumentNo(),
            original.getDocumentDate(),
            original.getDueDate(),
            original.getCurrency(),
            a.getAmount(),
            base,
            PayablesSupport.MODULE,
            REF + voucher.getId() + ":REINSTATE:" + original.getId(),
            batchNo,
            "Reinstated on " + date + " after reversal of " + voucher.getVoucherNo()));
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
