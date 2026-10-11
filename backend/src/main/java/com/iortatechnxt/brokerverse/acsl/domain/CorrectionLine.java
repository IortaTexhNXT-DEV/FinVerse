package com.iortatechnxt.brokerverse.acsl.domain;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * One line of a correction entry (ACSL 2.9.0-2.9.1): the account, side and amount with the party,
 * invoice and component it corrects, and the original journal line it reverses when it does.
 */
@Entity
@Table(name = "acsl_correction_line")
public class CorrectionLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Column(name = "account_code", nullable = false, length = 30)
  private String accountCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 6)
  private BalanceSide side;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "party_code", length = 30)
  private String partyCode;

  @Column(name = "invoice_no", length = 40)
  private String invoiceNo;

  @Column(length = 20)
  private String component;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @Column(name = "business_line", length = 20)
  private String businessLine;

  @Column(length = 250)
  private String narration;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private LineOrigin origin;

  @Column(name = "original_batch_no", length = 40)
  private String originalBatchNo;

  @Column(name = "original_line_no")
  private Integer originalLineNo;

  protected CorrectionLine() {}

  /**
   * Creates a line.
   *
   * @param lineNo line number
   * @param v values
   */
  public CorrectionLine(int lineNo, CorrectionLineValues v) {
    this.lineNo = lineNo;
    this.accountCode = v.accountCode();
    this.side = v.side();
    this.amount = v.amount();
    this.partyCode = v.partyCode();
    this.invoiceNo = v.invoiceNo();
    this.component = v.component();
    this.costCenter = v.costCenter();
    this.businessLine = v.businessLine();
    this.narration = v.narration();
    this.origin = v.origin();
    this.originalBatchNo = v.originalBatchNo();
    this.originalLineNo = v.originalLineNo();
  }

  /**
   * The line's values (to copy or re-validate it).
   *
   * @return values
   */
  public CorrectionLineValues values() {
    return new CorrectionLineValues(
        accountCode,
        side,
        amount,
        partyCode,
        invoiceNo,
        component,
        costCenter,
        businessLine,
        narration,
        origin,
        originalBatchNo,
        originalLineNo);
  }

  /**
   * The amount signed by side: positive for a debit.
   *
   * @return signed amount
   */
  public BigDecimal debitAmount() {
    return side == BalanceSide.DEBIT ? amount : amount.negate();
  }

  public Long getId() {
    return id;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public BalanceSide getSide() {
    return side;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getComponent() {
    return component;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public String getBusinessLine() {
    return businessLine;
  }

  public String getNarration() {
    return narration;
  }

  public LineOrigin getOrigin() {
    return origin;
  }

  public String getOriginalBatchNo() {
    return originalBatchNo;
  }

  public Integer getOriginalLineNo() {
    return originalLineNo;
  }
}
