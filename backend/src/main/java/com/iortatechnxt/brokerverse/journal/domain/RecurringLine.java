package com.iortatechnxt.brokerverse.journal.domain;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;

/**
 * Line of a recurring journal template (same fields as a manual journal line; accounts are resolved
 * when each occurrence is generated, so changes to the chart apply automatically).
 */
@Embeddable
public class RecurringLine {

  @Column(name = "account_code", nullable = false, length = 30)
  private String accountCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 6)
  private BalanceSide side;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(length = 3)
  private String currency;

  @Column(name = "exchange_rate", precision = 19, scale = 8)
  private BigDecimal exchangeRate;

  @Column(name = "branch_id")
  private Long branchId;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @Column(name = "business_line", length = 20)
  private String businessLine;

  @Column(name = "party_code", length = 30)
  private String partyCode;

  @Column(length = 60)
  private String reference;

  @Column(length = 250)
  private String narration;

  protected RecurringLine() {}

  /**
   * Creates a line.
   *
   * @param posting account, side and amount
   * @param currency line currency (null = header currency)
   * @param exchangeRate explicit rate (null = rate of the value date)
   * @param branchId line branch (null = header branch)
   * @param dimensions cost centre, business line, party, reference and narration
   */
  public RecurringLine(
      Posting posting,
      String currency,
      BigDecimal exchangeRate,
      Long branchId,
      LineDetails dimensions) {
    this.accountCode = posting.accountCode();
    this.side = posting.side();
    this.amount = posting.amount();
    this.currency = currency;
    this.exchangeRate = exchangeRate;
    this.branchId = branchId;
    this.costCenter = dimensions.costCenter();
    this.businessLine = dimensions.businessLine();
    this.partyCode = dimensions.partyCode();
    this.reference = dimensions.reference();
    this.narration = dimensions.narration();
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

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getExchangeRate() {
    return exchangeRate;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public String getBusinessLine() {
    return businessLine;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getReference() {
    return reference;
  }

  public String getNarration() {
    return narration;
  }

  /**
   * Account, side and amount of a line.
   *
   * @param accountCode GL account code
   * @param side debit or credit
   * @param amount positive amount
   */
  public record Posting(String accountCode, BalanceSide side, BigDecimal amount) {}

  /**
   * Analysis fields of a line.
   *
   * @param costCenter cost centre
   * @param businessLine line of business
   * @param partyCode sub-ledger party
   * @param reference reference
   * @param narration narration
   */
  public record LineDetails(
      String costCenter,
      String businessLine,
      String partyCode,
      String reference,
      String narration) {}
}
