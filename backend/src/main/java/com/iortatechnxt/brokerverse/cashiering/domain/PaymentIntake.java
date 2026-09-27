package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A payment as it arrives from a channel (CSHID.008/009): the references it carries (invoice, ARN,
 * policy or PN number), the payor and the tender.
 *
 * @param channel channel
 * @param batchRef upload job, PDC or pick-up reference, may be null
 * @param sourceKey idempotency key within the channel
 * @param rowNo file row, may be null
 * @param reference main reference used for matching, may be null
 * @param otherRefs further references tried in order
 * @param payor payor code (may be null) and name
 * @param assuredName assured, may be null
 * @param money amount, currency and value date
 * @param tender mode and check details
 */
public record PaymentIntake(
    PaymentChannel channel,
    String batchRef,
    String sourceKey,
    Integer rowNo,
    String reference,
    List<String> otherRefs,
    Payor payor,
    String assuredName,
    Money money,
    Tender tender) {

  /** Defensive copy of the references. */
  public PaymentIntake {
    otherRefs = otherRefs == null ? List.of() : List.copyOf(otherRefs);
  }

  /**
   * Every reference to try, main first, blanks dropped.
   *
   * @return references
   */
  public List<String> references() {
    List<String> refs = new ArrayList<>();
    if (reference != null && !reference.isBlank()) {
      refs.add(reference.strip());
    }
    otherRefs.stream().filter(r -> r != null && !r.isBlank()).map(String::strip).forEach(refs::add);
    return refs;
  }

  /**
   * Payor name.
   *
   * @return name
   */
  public String payorName() {
    return payor.name();
  }

  /**
   * Amount.
   *
   * @return amount
   */
  public BigDecimal amount() {
    return money.amount();
  }

  /**
   * Currency.
   *
   * @return currency
   */
  public String currency() {
    return money.currency();
  }

  /**
   * Value date.
   *
   * @return date
   */
  public LocalDate valueDate() {
    return money.valueDate();
  }

  /**
   * Time of payment.
   *
   * @return time text, may be null
   */
  public String paidTime() {
    return tender.paidTime();
  }

  /**
   * Late deposit flag (bills payment).
   *
   * @return flag
   */
  public boolean lateDeposit() {
    return tender.lateDeposit();
  }

  /**
   * Mode of payment.
   *
   * @return mode
   */
  public PaymentMode mode() {
    return tender.mode();
  }

  /**
   * Check number.
   *
   * @return check number, may be null
   */
  public String checkNo() {
    return tender.checkNo();
  }

  /**
   * Bank of the check.
   *
   * @return bank, may be null
   */
  public String checkBank() {
    return tender.checkBank();
  }

  /**
   * Who paid.
   *
   * @param code party code, may be null
   * @param name payor name
   */
  public record Payor(String code, String name) {}

  /**
   * How much and when.
   *
   * @param amount amount received
   * @param currency currency
   * @param valueDate payment date
   */
  public record Money(BigDecimal amount, String currency, LocalDate valueDate) {}

  /**
   * Tender details.
   *
   * @param mode mode of payment
   * @param checkNo check number, may be null
   * @param checkBank bank, may be null
   * @param paidTime time of payment, may be null
   * @param lateDeposit late deposit flag
   */
  public record Tender(
      PaymentMode mode, String checkNo, String checkBank, String paidTime, boolean lateDeposit) {

    /**
     * A cash payment.
     *
     * @param mode mode
     * @return tender without check
     */
    public static Tender of(PaymentMode mode) {
      return new Tender(mode, null, null, null, false);
    }
  }
}
