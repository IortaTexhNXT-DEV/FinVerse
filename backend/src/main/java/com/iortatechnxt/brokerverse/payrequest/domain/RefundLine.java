package com.iortatechnxt.brokerverse.payrequest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * One account of a Refund Request Form (Appendix D, MKT 1.10.0): AR number, client, assured,
 * amount, reason, branch / unit, categories and account or check name. A live line blocks another
 * refund of the same AR number (MKT 2.23.0); the line stops being live when its request is
 * cancelled.
 */
@Entity
@Table(name = "prq_request_line")
public class RefundLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Column(name = "ar_no", nullable = false, length = 40)
  private String arNo;

  @Column(name = "client_code", nullable = false, length = 30)
  private String clientCode;

  @Column(name = "assured_name", nullable = false, length = 250)
  private String assuredName;

  @Column(name = "invoice_no", length = 40)
  private String invoiceNo;

  @Column(name = "root_invoice_no", length = 40)
  private String rootInvoiceNo;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "reason_code", nullable = false, length = 40)
  private String reasonCode;

  @Column(name = "branch_unit", length = 60)
  private String branchUnit;

  @Column(name = "category_a", length = 40)
  private String categoryA;

  @Column(name = "category_b", length = 40)
  private String categoryB;

  @Column(name = "account_name", length = 250)
  private String accountName;

  @Column(name = "cancelled_policy", nullable = false)
  private boolean cancelledPolicy;

  @Column(nullable = false)
  private boolean live = true;

  protected RefundLine() {}

  /**
   * Creates a line.
   *
   * @param lineNo line number
   * @param values the form values
   * @param rootInvoiceNo root invoice of the invoice family (ACSL 2.16.0), may be null
   * @param cancelledPolicy whether the refund concerns a cancelled policy (MKT 1.11.0)
   */
  public RefundLine(
      int lineNo, RefundLineValues values, String rootInvoiceNo, boolean cancelledPolicy) {
    this.lineNo = lineNo;
    this.arNo = values.arNo().strip();
    this.clientCode = values.clientCode().strip();
    this.assuredName = values.assuredName();
    this.invoiceNo =
        values.invoiceNo() == null || values.invoiceNo().isBlank()
            ? null
            : values.invoiceNo().strip();
    this.rootInvoiceNo = rootInvoiceNo;
    this.amount = values.amount().setScale(2);
    this.reasonCode = values.reasonCode();
    this.branchUnit = values.branchUnit();
    this.categoryA = values.categoryA();
    this.categoryB = values.categoryB();
    this.accountName = values.accountName();
    this.cancelledPolicy = cancelledPolicy;
  }

  /** The line no longer blocks its AR number (request cancelled). */
  public void release() {
    this.live = false;
  }

  public Long getId() {
    return id;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getArNo() {
    return arNo;
  }

  public String getClientCode() {
    return clientCode;
  }

  public String getAssuredName() {
    return assuredName;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getRootInvoiceNo() {
    return rootInvoiceNo;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public String getBranchUnit() {
    return branchUnit;
  }

  public String getCategoryA() {
    return categoryA;
  }

  public String getCategoryB() {
    return categoryB;
  }

  public String getAccountName() {
    return accountName;
  }

  public boolean isCancelledPolicy() {
    return cancelledPolicy;
  }

  public boolean isLive() {
    return live;
  }
}
