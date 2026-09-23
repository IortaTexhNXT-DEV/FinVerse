package com.iortatechnxt.finverse.journal.domain;

import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.coa.domain.GlAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * One debit or credit line of a journal batch.
 *
 * <p>{@code amount} is in the line currency; {@code baseAmount} is the company base currency
 * equivalent at {@code exchangeRate}. Amounts are always positive; {@code side} gives direction.
 */
@Entity
@Table(name = "jnl_line")
public class JournalLine {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "batch_id", nullable = false)
  private JournalBatch batch;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "account_id", nullable = false)
  private GlAccount account;

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 6)
  private BalanceSide side;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 8)
  private BigDecimal exchangeRate;

  @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal baseAmount;

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

  protected JournalLine() {}

  /**
   * Creates a line.
   *
   * @param spec line values
   */
  public JournalLine(JournalLineSpec spec) {
    this.account = spec.account();
    this.branchId = spec.branchId();
    this.side = spec.side();
    this.currency = spec.currency();
    this.amount = spec.amount();
    this.exchangeRate = spec.exchangeRate();
    this.baseAmount = spec.baseAmount();
    this.costCenter = spec.costCenter();
    this.businessLine = spec.businessLine();
    this.partyCode = spec.partyCode();
    this.reference = spec.reference();
    this.narration = spec.narration();
  }

  void attach(JournalBatch owner, int number) {
    this.batch = owner;
    this.lineNo = number;
  }

  /**
   * Adjusts the base amount by a rounding difference (see {@link JournalBatch}).
   *
   * @param delta amount to add
   */
  void adjustBaseAmount(BigDecimal delta) {
    this.baseAmount = this.baseAmount.add(delta);
  }

  /**
   * Returns a copy of this line's values with the side swapped (for reversals).
   *
   * @return reversed spec
   */
  public JournalLineSpec reversedSpec() {
    return toSpec(side.opposite());
  }

  /**
   * Returns a copy of this line's values (for copy-transaction).
   *
   * @return spec
   */
  public JournalLineSpec toSpec() {
    return toSpec(side);
  }

  private JournalLineSpec toSpec(BalanceSide newSide) {
    return new JournalLineSpec(
        account,
        branchId,
        newSide,
        currency,
        amount,
        exchangeRate,
        baseAmount,
        costCenter,
        businessLine,
        partyCode,
        reference,
        narration);
  }

  public Long getId() {
    return id;
  }

  public JournalBatch getBatch() {
    return batch;
  }

  public int getLineNo() {
    return lineNo;
  }

  public GlAccount getAccount() {
    return account;
  }

  public Long getBranchId() {
    return branchId;
  }

  public BalanceSide getSide() {
    return side;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BigDecimal getExchangeRate() {
    return exchangeRate;
  }

  public BigDecimal getBaseAmount() {
    return baseAmount;
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
}
