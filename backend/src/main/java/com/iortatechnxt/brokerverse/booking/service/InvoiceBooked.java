package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Domain event: an invoice was booked (BRNB.027) - an original booking, a policy year of a
 * multi-year account, a positive or negative financial endorsement or a cancellation. Contract for
 * Operations BRD-2 (OPERATIONS_DESIGN section 2.2): the {@code opsledger} feed copies every booked
 * invoice from it.
 *
 * <p>Published with Spring's event publisher <b>inside</b> the booking transaction; consumers
 * listen with {@code @TransactionalEventListener(phase = AFTER_COMMIT)} so they only see committed
 * bookings. The same data is available for replay from {@code BookingQueryService.invoice}.
 *
 * <p><b>Sign convention:</b> there is one event type. Return invoices ({@link
 * InvoiceKind#ENDORSEMENT_MINUS} and {@link InvoiceKind#CANCELLATION}) carry negative premium
 * components, commission and VAT; {@link #kind} tells the consumer which it is.
 *
 * @param invoiceNo invoice number ({@code BI-<branch>-<yyyy>-nnnnnn})
 * @param arn Account Reference Number
 * @param endorsementNo endorsement number, null for an original booking
 * @param clientCode client code (party of the premium receivable)
 * @param shares insurer codes and shares in percent
 * @param currency currency
 * @param bookingDate booking date
 * @param inceptionDate start of the period invoiced
 * @param expiryDate end of the period invoiced
 * @param riskCode BDOI risk code (product)
 * @param segment market segment
 * @param aoUsername account officer
 * @param salesUnit sales team
 * @param costCenter cost center (BRNB.108)
 * @param components premium by component (BASIC, DST, PREMIUM_TAX_OR_VAT, LGT, FST, OTHER)
 * @param commission commission
 * @param vatOnCommission VAT on the commission
 * @param wtaxRate withholding tax rate on the commission, percent
 * @param directPayment premium paid directly to the insurer (BRNB.114)
 * @param cwt2Percent the client withholds 2 % creditable tax
 * @param incentiveEligible incentive eligible (BRNB.107)
 * @param kind BOOKING, ENDORSEMENT_PLUS, ENDORSEMENT_MINUS or CANCELLATION
 * @param policyNo policy number of the year
 * @param policyYear policy year (multi-year accounts)
 * @param lineCode product line
 * @param productVersionNo package version of the account (BRPM.007), null when none
 * @param incentiveCriteria codes of the incentive criteria matched (PMADD07), empty when none
 * @param rootInvoiceNo root of the invoice family (DIS 3.27.2): the invoice itself for an original
 *     booking, else the original invoice; the same value as the ledger's {@code root_invoice_no}
 */
public record InvoiceBooked(
    String invoiceNo,
    String arn,
    String endorsementNo,
    String clientCode,
    List<Share> shares,
    String currency,
    LocalDate bookingDate,
    LocalDate inceptionDate,
    LocalDate expiryDate,
    String riskCode,
    String segment,
    String aoUsername,
    String salesUnit,
    String costCenter,
    Map<PremiumComponent, BigDecimal> components,
    BigDecimal commission,
    BigDecimal vatOnCommission,
    BigDecimal wtaxRate,
    boolean directPayment,
    boolean cwt2Percent,
    boolean incentiveEligible,
    InvoiceKind kind,
    String policyNo,
    int policyYear,
    String lineCode,
    Integer productVersionNo,
    List<String> incentiveCriteria,
    String rootInvoiceNo) {

  /** Defensive copies. */
  public InvoiceBooked {
    shares = List.copyOf(shares);
    components = Map.copyOf(components);
    incentiveCriteria = incentiveCriteria == null ? List.of() : List.copyOf(incentiveCriteria);
    rootInvoiceNo = rootInvoiceNo == null ? invoiceNo : rootInvoiceNo;
  }

  /**
   * The event without an explicit root (earlier contract): the invoice is its own root.
   *
   * @param invoiceNo invoice number
   * @param arn Account Reference Number
   * @param endorsementNo endorsement number
   * @param clientCode client code
   * @param shares insurer shares
   * @param currency currency
   * @param bookingDate booking date
   * @param inceptionDate period start
   * @param expiryDate period end
   * @param riskCode risk code
   * @param segment market segment
   * @param aoUsername account officer
   * @param salesUnit sales team
   * @param costCenter cost center
   * @param components premium by component
   * @param commission commission
   * @param vatOnCommission VAT on the commission
   * @param wtaxRate withholding tax rate
   * @param directPayment direct payment
   * @param cwt2Percent client withholds 2 %
   * @param incentiveEligible incentive eligible
   * @param kind invoice kind
   * @param policyNo policy number
   * @param policyYear policy year
   * @param lineCode product line
   * @param productVersionNo package version
   * @param incentiveCriteria incentive criteria codes
   */
  @SuppressWarnings("java:S107") // event contract
  public InvoiceBooked(
      String invoiceNo,
      String arn,
      String endorsementNo,
      String clientCode,
      List<Share> shares,
      String currency,
      LocalDate bookingDate,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      String riskCode,
      String segment,
      String aoUsername,
      String salesUnit,
      String costCenter,
      Map<PremiumComponent, BigDecimal> components,
      BigDecimal commission,
      BigDecimal vatOnCommission,
      BigDecimal wtaxRate,
      boolean directPayment,
      boolean cwt2Percent,
      boolean incentiveEligible,
      InvoiceKind kind,
      String policyNo,
      int policyYear,
      String lineCode,
      Integer productVersionNo,
      List<String> incentiveCriteria) {
    this(
        invoiceNo,
        arn,
        endorsementNo,
        clientCode,
        shares,
        currency,
        bookingDate,
        inceptionDate,
        expiryDate,
        riskCode,
        segment,
        aoUsername,
        salesUnit,
        costCenter,
        components,
        commission,
        vatOnCommission,
        wtaxRate,
        directPayment,
        cwt2Percent,
        incentiveEligible,
        kind,
        policyNo,
        policyYear,
        lineCode,
        productVersionNo,
        incentiveCriteria,
        null);
  }

  /**
   * The event of a booked invoice.
   *
   * @param invoice booked invoice
   * @return event
   */
  public static InvoiceBooked of(BookedInvoice invoice) {
    return new InvoiceBooked(
        invoice.getInvoiceNo(),
        invoice.getArn(),
        invoice.getEndorsementNo(),
        invoice.getFacts().clientCode(),
        invoice.getShares().stream().map(s -> new Share(s.insurerCode(), s.sharePct())).toList(),
        invoice.getCurrency(),
        invoice.getBookingDate(),
        invoice.getInceptionDate(),
        invoice.getExpiryDate(),
        invoice.getFacts().riskCode(),
        invoice.getFacts().marketSegment(),
        invoice.getFacts().accountOfficer(),
        invoice.getFacts().salesUnit(),
        invoice.getFacts().costCenter(),
        invoice.getPremium().asMap(),
        invoice.getCommission().commission(),
        invoice.getCommission().vatOnCommission(),
        invoice.getCommission().wtaxRate(),
        invoice.getFlags().directPayment(),
        invoice.getFlags().cwt2Percent(),
        invoice.getFlags().incentiveEligible(),
        invoice.getKind(),
        invoice.getPolicyNo(),
        invoice.getPolicyYear(),
        invoice.getFacts().lineCode(),
        invoice.getFacts().productVersionNo(),
        invoice.getFlags().incentiveCriteriaCodes(),
        invoice.getRootInvoiceNo());
  }

  /**
   * Gross premium: the sum of the components.
   *
   * @return total (negative for return invoices)
   */
  public BigDecimal grossPremium() {
    return components.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * An insurer's share.
   *
   * @param insurerCode insurer party code
   * @param sharePct share in percent
   */
  public record Share(String insurerCode, BigDecimal sharePct) {}
}
