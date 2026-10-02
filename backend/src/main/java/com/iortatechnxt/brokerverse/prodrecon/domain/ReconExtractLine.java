package com.iortatechnxt.brokerverse.prodrecon.domain;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One booked invoice of a production register as it was sent (Annex IV #5 Production Register,
 * PRCID.006/020): an immutable snapshot of the ledger invoice at extraction time.
 */
@Entity
@Table(name = "prc_extract_line")
public class ReconExtractLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "extract_id", nullable = false, updatable = false)
  private Long extractId;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(nullable = false, length = 30, updatable = false)
  private String arn;

  @Column(nullable = false, length = 20, updatable = false)
  private String kind;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "booking_date", nullable = false, updatable = false)
  private LocalDate bookingDate;

  @Column(name = "inception_date", nullable = false, updatable = false)
  private LocalDate inceptionDate;

  @Column(name = "expiry_date", nullable = false, updatable = false)
  private LocalDate expiryDate;

  @Column(name = "policy_no", length = 60, updatable = false)
  private String policyNo;

  @Column(name = "endorsement_no", length = 40, updatable = false)
  private String endorsementNo;

  @Column(name = "pn_nos", length = 500, updatable = false)
  private String pnNos;

  @Column(name = "assured_name", nullable = false, length = 250, updatable = false)
  private String assuredName;

  @Column(name = "client_code", nullable = false, length = 30, updatable = false)
  private String clientCode;

  @Column(name = "risk_code", length = 20, updatable = false)
  private String riskCode;

  @Column(name = "product_line", length = 30, updatable = false)
  private String productLine;

  @Column(length = 40, updatable = false)
  private String segment;

  @Column(name = "ao_username", length = 50, updatable = false)
  private String aoUsername;

  @Column(name = "sales_unit", length = 20, updatable = false)
  private String salesUnit;

  @Column(name = "branch_id", updatable = false)
  private Long branchId;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "basic_premium", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal basicPremium;

  @Column(name = "gross_commission", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal grossCommission;

  @Column(name = "gross_premium", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal grossPremium;

  @Column(name = "booked_vat", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal bookedVat;

  @Column(name = "amount_paid", nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amountPaid;

  @Column(name = "date_paid", updatable = false)
  private LocalDate datePaid;

  @Column(name = "ar_number", length = 40, updatable = false)
  private String arNumber;

  @Column(name = "remittance_status", nullable = false, length = 30, updatable = false)
  private String remittanceStatus;

  @Column(nullable = false, updatable = false)
  private boolean estimated;

  protected ReconExtractLine() {}

  /**
   * Snapshot of a ledger invoice (components loaded).
   *
   * @param extractId extract
   * @param lineNo line number
   * @param invoice ledger invoice
   * @param payment last payment of the invoice
   * @return the line
   */
  public static ReconExtractLine of(
      Long extractId, int lineNo, OpsInvoice invoice, LastPayment payment) {
    ReconExtractLine l = new ReconExtractLine();
    l.extractId = extractId;
    l.lineNo = lineNo;
    l.invoiceNo = invoice.getInvoiceNo();
    l.arn = invoice.getArn();
    l.kind = invoice.getKind().name();
    l.insurerCode = invoice.getInsurerCode();
    l.policyNo = invoice.getPolicyNo();
    l.endorsementNo = invoice.getEndorsementNo();
    l.pnNos = invoice.getPnNos();
    l.assuredName = invoice.getAssuredName();
    l.clientCode = invoice.getClientCode();
    l.branchId = invoice.getBranchId();
    l.classify(invoice);
    l.amounts(invoice);
    l.datePaid = payment.date();
    l.arNumber = payment.arNo();
    l.remittanceStatus = invoice.getRemittanceStatus().name();
    l.estimated = invoice.isEstimated();
    return l;
  }

  private void classify(OpsInvoice invoice) {
    var c = invoice.getClassification();
    this.bookingDate = c.bookingDate();
    this.inceptionDate = c.inceptionDate();
    this.expiryDate = c.expiryDate();
    this.riskCode = c.riskCode();
    this.productLine = c.productLine();
    this.segment = c.segment();
    this.aoUsername = c.aoUsername();
    this.salesUnit = c.salesUnit();
    this.currency = c.currency();
  }

  private void amounts(OpsInvoice invoice) {
    this.basicPremium = invoice.component(LedgerComponent.BASIC).due();
    this.grossCommission = invoice.getCommission();
    this.grossPremium = invoice.getGrossPremium();
    this.bookedVat = invoice.getVatOnCommission();
    BigDecimal paid = BigDecimal.ZERO;
    for (OpsInvoiceComponent c : invoice.getComponents()) {
      if (c.getComponent().isPremiumReceivable()) {
        paid = paid.add(c.netApplied());
      }
    }
    this.amountPaid = paid;
  }

  public Long getId() {
    return id;
  }

  public Long getExtractId() {
    return extractId;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getArn() {
    return arn;
  }

  public String getKind() {
    return kind;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public LocalDate getBookingDate() {
    return bookingDate;
  }

  public LocalDate getInceptionDate() {
    return inceptionDate;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public String getEndorsementNo() {
    return endorsementNo;
  }

  public String getPnNos() {
    return pnNos;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getRiskCode() {
    return riskCode;
  }

  public String getProductLine() {
    return productLine;
  }

  public String getSegment() {
    return segment;
  }

  public String getAoUsername() {
    return aoUsername;
  }

  public String getSalesUnit() {
    return salesUnit;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getBasicPremium() {
    return basicPremium;
  }

  public BigDecimal getGrossCommission() {
    return grossCommission;
  }

  public BigDecimal getGrossPremium() {
    return grossPremium;
  }

  public BigDecimal getBookedVat() {
    return bookedVat;
  }

  public BigDecimal getAmountPaid() {
    return amountPaid;
  }

  public LocalDate getDatePaid() {
    return datePaid;
  }

  public String getArNumber() {
    return arNumber;
  }

  public String getRemittanceStatus() {
    return remittanceStatus;
  }

  public boolean isEstimated() {
    return estimated;
  }

  /**
   * Last payment applied to an invoice.
   *
   * @param date value date, null when unpaid
   * @param arNo acknowledgement receipt number, may be null
   */
  public record LastPayment(LocalDate date, String arNo) {

    /** No payment. */
    public static final LastPayment NONE = new LastPayment(null, null);
  }
}
