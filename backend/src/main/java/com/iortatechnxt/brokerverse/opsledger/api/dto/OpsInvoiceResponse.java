package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.opsledger.domain.FeedSource;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceData;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * An Operations invoice with its components and shares (RMTID.026).
 *
 * @param keys identifiers
 * @param parties client, assured, payor and insurer
 * @param classification currency, dates, risk and sales stamp
 * @param grossPremium gross premium
 * @param commission commission
 * @param vatOnCommission VAT on the commission
 * @param wtaxRate withholding tax rate, percent
 * @param premiumBalance outstanding premium receivable
 * @param paymentStatus payment status
 * @param remittanceStatus remittance status
 * @param flags flags and lock
 * @param feedSource event or replay
 * @param components components with balances, in component order
 * @param shares insurer shares
 */
public record OpsInvoiceResponse(
    Keys keys,
    Parties parties,
    Classification classification,
    BigDecimal grossPremium,
    BigDecimal commission,
    BigDecimal vatOnCommission,
    BigDecimal wtaxRate,
    BigDecimal premiumBalance,
    PaymentStatus paymentStatus,
    RemittanceStatus remittanceStatus,
    FlagsResponse flags,
    FeedSource feedSource,
    List<Component> components,
    List<Share> shares) {

  /**
   * Maps an invoice (components and shares loaded).
   *
   * @param i invoice
   * @return response
   */
  public static OpsInvoiceResponse from(OpsInvoice i) {
    return new OpsInvoiceResponse(
        new Keys(
            i.getInvoiceNo(),
            i.getArn(),
            i.getAccountId(),
            i.getKind(),
            i.getEndorsementNo(),
            i.getParentInvoiceNo(),
            i.getRootInvoiceNo(),
            i.getPolicyNo(),
            i.getPolicyYear(),
            i.getPnNos()),
        new Parties(i.getClientCode(), i.getAssuredName(), i.getPayorName(), i.getInsurerCode()),
        Classification.from(i.getClassification()),
        i.getGrossPremium(),
        i.getCommission(),
        i.getVatOnCommission(),
        i.getWtaxRate(),
        i.premiumBalance(),
        i.getPaymentStatus(),
        i.getRemittanceStatus(),
        FlagsResponse.from(i),
        i.getFeedSource(),
        i.getComponents().stream()
            .sorted(Comparator.comparing(OpsInvoiceComponent::getComponent))
            .map(Component::from)
            .toList(),
        i.getShares().stream().map(Share::from).toList());
  }

  /**
   * Identifiers.
   *
   * @param invoiceNo invoice number
   * @param arn ARN
   * @param accountId account id
   * @param kind kind
   * @param endorsementNo endorsement number
   * @param parentInvoiceNo original invoice
   * @param rootInvoiceNo root invoice of the family (DIS 3.27.2)
   * @param policyNo policy number
   * @param policyYear policy year
   * @param pnNos PN numbers
   */
  public record Keys(
      String invoiceNo,
      String arn,
      Long accountId,
      InvoiceKind kind,
      String endorsementNo,
      String parentInvoiceNo,
      String rootInvoiceNo,
      String policyNo,
      int policyYear,
      String pnNos) {}

  /**
   * Parties.
   *
   * @param clientCode client
   * @param assuredName assured
   * @param payorName payor
   * @param insurerCode lead insurer
   */
  public record Parties(
      String clientCode, String assuredName, String payorName, String insurerCode) {}

  /**
   * Classification.
   *
   * @param currency currency
   * @param bookingDate booking date
   * @param inceptionDate period start
   * @param expiryDate period end
   * @param riskCode risk code
   * @param productLine product line
   * @param segment market segment
   * @param aoUsername account officer
   * @param salesUnit sales unit
   * @param costCenter cost center
   */
  public record Classification(
      String currency,
      LocalDate bookingDate,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      String riskCode,
      String productLine,
      String segment,
      String aoUsername,
      String salesUnit,
      String costCenter) {

    static Classification from(OpsInvoiceData.Classification c) {
      return new Classification(
          c.currency(),
          c.bookingDate(),
          c.inceptionDate(),
          c.expiryDate(),
          c.riskCode(),
          c.productLine(),
          c.segment(),
          c.aoUsername(),
          c.salesUnit(),
          c.costCenter());
    }
  }

  /**
   * A component with its buckets.
   *
   * @param component component
   * @param premiumReceivable part of the client's premium receivable
   * @param booked booked
   * @param applied applied payments
   * @param reversed reversed applications
   * @param remitted remitted
   * @param adjusted adjustments
   * @param writtenOff written off
   * @param balance outstanding balance
   */
  public record Component(
      LedgerComponent component,
      boolean premiumReceivable,
      BigDecimal booked,
      BigDecimal applied,
      BigDecimal reversed,
      BigDecimal remitted,
      BigDecimal adjusted,
      BigDecimal writtenOff,
      BigDecimal balance) {

    static Component from(OpsInvoiceComponent c) {
      return new Component(
          c.getComponent(),
          c.getComponent().isPremiumReceivable(),
          c.getBooked(),
          c.getApplied(),
          c.getReversed(),
          c.getRemitted(),
          c.getAdjusted(),
          c.getWrittenOff(),
          c.getBalance());
    }
  }

  /**
   * An insurer share.
   *
   * @param insurerCode insurer
   * @param sharePct share in percent
   * @param lead lead insurer
   */
  public record Share(String insurerCode, BigDecimal sharePct, boolean lead) {

    static Share from(OpsInvoiceShare s) {
      return new Share(s.insurerCode(), s.sharePct(), s.lead());
    }
  }
}
