package com.iortatechnxt.brokerverse.collections.common.domain;

import com.iortatechnxt.brokerverse.collections.common.domain.ClxEnums.InvoiceCategory;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parts of a collection item copied from the Operations ledger at each refresh (COLLECTIONS_DESIGN
 * 4.1, BRCLXN.001-015): keys and parties, classification, figures and the balance per component.
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class ItemParts {

  private ItemParts() {}

  /**
   * Keys and parties of the invoice.
   *
   * @param arn Account Reference Number
   * @param rootInvoiceNo original invoice of the family
   * @param invoiceKind booking, endorsement plus / minus or cancellation
   * @param policyNo policy number
   * @param policyYear policy year of a multi-year account
   * @param clientCode client
   * @param assuredName assured
   * @param insurerCode lead insurer
   */
  @Embeddable
  public record Parties(
      @Column(nullable = false, length = 30) String arn,
      @Column(name = "root_invoice_no", length = 40) String rootInvoiceNo,
      @Column(name = "invoice_kind", nullable = false, length = 20) String invoiceKind,
      @Column(name = "policy_no", length = 60) String policyNo,
      @Column(name = "policy_year", nullable = false) int policyYear,
      @Column(name = "client_code", nullable = false, length = 30) String clientCode,
      @Column(name = "assured_name", nullable = false, length = 250) String assuredName,
      @Column(name = "insurer_code", nullable = false, length = 30) String insurerCode) {}

  /**
   * Classification of the invoice (filters of BRCLXN.011/012).
   *
   * @param branchId invoicing branch
   * @param segment market segment
   * @param salesUnit sales unit (team)
   * @param unitHeadUsername Unit Head resolved from the sales organisation (CQ05)
   * @param aoUsername account officer
   * @param productLine product line
   * @param currency currency
   * @param bookingDate booking date
   * @param inceptionDate inception
   * @param expiryDate expiry
   * @param dpFlag direct payment
   * @param cwtFlag client withholds 2% CWT
   * @param invoiceCategory regular or direct bill
   */
  @Embeddable
  public record Classification(
      @Column(name = "branch_id") Long branchId,
      @Column(length = 40) String segment,
      @Column(name = "sales_unit", length = 20) String salesUnit,
      @Column(name = "unit_head_username", length = 50) String unitHeadUsername,
      @Column(name = "ao_username", length = 50) String aoUsername,
      @Column(name = "product_line", length = 30) String productLine,
      @Column(nullable = false, length = 3) String currency,
      @Column(name = "booking_date", nullable = false) LocalDate bookingDate,
      @Column(name = "inception_date", nullable = false) LocalDate inceptionDate,
      @Column(name = "expiry_date", nullable = false) LocalDate expiryDate,
      @Column(name = "dp_flag", nullable = false) boolean dpFlag,
      @Column(name = "cwt_flag", nullable = false) boolean cwtFlag,
      @Enumerated(EnumType.STRING) @Column(name = "invoice_category", nullable = false, length = 12)
          InvoiceCategory invoiceCategory) {}

  /**
   * Figures at the last refresh.
   *
   * @param grossPremium gross premium of the invoice
   * @param netOutstanding outstanding premium receivable (the six PR components)
   * @param outstandingPr2307 PR covered by the client's 2307 still open
   * @param agingDays days since the aging basis date
   * @param agingBracket bracket of {@code CLX_AGING_BRACKETS}
   * @param paymentStatus payment status of the ledger
   */
  @Embeddable
  public record Figures(
      @Column(name = "gross_premium", nullable = false, precision = 19, scale = 2)
          BigDecimal grossPremium,
      @Column(name = "net_outstanding", nullable = false, precision = 19, scale = 2)
          BigDecimal netOutstanding,
      @Column(name = "outstanding_pr2307", nullable = false, precision = 19, scale = 2)
          BigDecimal outstandingPr2307,
      @Column(name = "aging_days", nullable = false) int agingDays,
      @Column(name = "aging_bracket", length = 20) String agingBracket,
      @Enumerated(EnumType.STRING) @Column(name = "payment_status", nullable = false, length = 20)
          PaymentStatus paymentStatus) {

    /**
     * Total still to collect: premium receivable and PR2307.
     *
     * @return net outstanding plus PR2307
     */
    public BigDecimal total() {
      return netOutstanding.add(outstandingPr2307);
    }
  }

  /**
   * Balance snapshot of one component (BRCLXN.046, table {@code clx_item_balance}).
   *
   * @param component ledger component
   * @param booked booked
   * @param adjusted endorsements, corrections and reclasses
   * @param applied payments applied net of reversals
   * @param writtenOff write-offs, minimal balances and DP reversals
   * @param outstanding balance
   */
  @Embeddable
  public record Balance(
      @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) LedgerComponent component,
      @Column(nullable = false, precision = 19, scale = 2) BigDecimal booked,
      @Column(nullable = false, precision = 19, scale = 2) BigDecimal adjusted,
      @Column(nullable = false, precision = 19, scale = 2) BigDecimal applied,
      @Column(name = "written_off", nullable = false, precision = 19, scale = 2)
          BigDecimal writtenOff,
      @Column(nullable = false, precision = 19, scale = 2) BigDecimal outstanding) {}
}
