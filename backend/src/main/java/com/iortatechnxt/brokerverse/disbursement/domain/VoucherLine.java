package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.LineOrigin;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A line of the proforma entry of a voucher (DIS 2.7.6, 2.7.10, 3.30.0): side, account, sub-ledger
 * party, cost centre, line of business, amount and the event component it comes from, with its
 * origin (from the accounting rule, edited by the processor, or from the expense allocation). The
 * approver sees which lines were edited.
 */
@Entity
@Table(name = "dsb_voucher_line")
public class VoucherLine extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "voucher_id", nullable = false, updatable = false)
  private Voucher voucher;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 6)
  private BalanceSide side;

  @Column(name = "account_code", nullable = false, length = 30)
  private String accountCode;

  @Column(name = "party_code", length = 30)
  private String partyCode;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @Column(name = "business_line", length = 20)
  private String businessLine;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(length = 30)
  private String component;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 12)
  private LineOrigin origin;

  @Column(length = 250)
  private String narration;

  protected VoucherLine() {}

  /**
   * A proforma line.
   *
   * @param voucher voucher
   * @param lineNo line number
   * @param values values
   */
  VoucherLine(Voucher voucher, int lineNo, LineValues values) {
    this.voucher = voucher;
    this.lineNo = lineNo;
    this.side = values.side();
    this.accountCode = values.accountCode();
    this.partyCode = values.partyCode();
    this.costCenter = values.costCenter();
    this.businessLine = values.businessLine();
    this.amount = values.amount();
    this.component = values.component();
    this.origin = values.origin();
    this.narration = values.narration();
  }

  /**
   * The values of this line.
   *
   * @return values
   */
  public LineValues values() {
    return new LineValues(
        side,
        accountCode,
        partyCode,
        costCenter,
        businessLine,
        amount,
        component,
        origin,
        narration);
  }

  public Voucher getVoucher() {
    return voucher;
  }

  public int getLineNo() {
    return lineNo;
  }

  public BalanceSide getSide() {
    return side;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public String getBusinessLine() {
    return businessLine;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getComponent() {
    return component;
  }

  public LineOrigin getOrigin() {
    return origin;
  }

  public String getNarration() {
    return narration;
  }

  /**
   * Values of a proforma line.
   *
   * @param side debit or credit
   * @param accountCode GL account
   * @param partyCode sub-ledger party, may be null
   * @param costCenter cost centre, may be null
   * @param businessLine line of business, may be null
   * @param amount positive amount
   * @param component event component, null for an added line
   * @param origin origin
   * @param narration narration
   */
  public record LineValues(
      BalanceSide side,
      String accountCode,
      String partyCode,
      String costCenter,
      String businessLine,
      BigDecimal amount,
      String component,
      LineOrigin origin,
      String narration) {}
}
