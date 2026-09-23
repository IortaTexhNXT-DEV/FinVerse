package com.iortatechnxt.finverse.budget.domain;

import com.iortatechnxt.finverse.common.util.Money;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.hibernate.annotations.BatchSize;

/**
 * Budget line: one GL account (optionally per cost centre) with twelve monthly amounts in natural
 * sign (income and expense budgets are both positive).
 */
@Entity
@Table(name = "bud_budget_line")
public class BudgetLine {

  /** Number of monthly periods in a budget year. */
  public static final int MONTHS = 12;

  private static final int BATCH = 100;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "budget_id", nullable = false)
  private Budget budget;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "account_code", nullable = false, length = 30)
  private String accountCode;

  @Column(name = "cost_center", length = 20)
  private String costCenter;

  @ElementCollection
  @BatchSize(size = BATCH)
  @CollectionTable(name = "bud_budget_amount", joinColumns = @JoinColumn(name = "line_id"))
  @MapKeyColumn(name = "period_no")
  @Column(name = "amount", nullable = false, precision = 19, scale = 2)
  private final Map<Integer, BigDecimal> amounts = new TreeMap<>();

  protected BudgetLine() {}

  BudgetLine(Budget budget, BudgetLineValues values) {
    this.budget = budget;
    this.accountId = values.accountId();
    this.accountCode = values.accountCode();
    this.costCenter = values.costCenter();
    setAmounts(values.months());
  }

  /**
   * Replaces the monthly amounts (same account and cost centre).
   *
   * @param values new values
   */
  void update(BudgetLineValues values) {
    setAmounts(values.months());
  }

  /**
   * Identity of a line within a budget.
   *
   * @param accountCode account
   * @param costCenter cost centre or null
   * @return key
   */
  static String key(String accountCode, String costCenter) {
    return accountCode + "|" + (costCenter == null ? "" : costCenter);
  }

  String key() {
    return key(accountCode, costCenter);
  }

  private void setAmounts(List<BigDecimal> months) {
    for (int i = 0; i < MONTHS; i++) {
      amounts.put(i + 1, Money.round(months.get(i)));
    }
  }

  /**
   * Monthly amounts, period 1 first.
   *
   * @return twelve amounts
   */
  public List<BigDecimal> months() {
    List<BigDecimal> out = new ArrayList<>(MONTHS);
    for (int i = 1; i <= MONTHS; i++) {
      out.add(amount(i));
    }
    return out;
  }

  /**
   * Amount of one period.
   *
   * @param periodNo 1..12
   * @return amount
   */
  public BigDecimal amount(int periodNo) {
    return amounts.getOrDefault(periodNo, Money.zero());
  }

  /**
   * Annual total.
   *
   * @return sum of the twelve months
   */
  public BigDecimal annual() {
    return upTo(MONTHS);
  }

  /**
   * Year-to-date total up to and including a period.
   *
   * @param periodNo last period included
   * @return sum
   */
  public BigDecimal upTo(int periodNo) {
    BigDecimal sum = Money.zero();
    for (int i = 1; i <= periodNo; i++) {
      sum = sum.add(amount(i));
    }
    return sum;
  }

  /**
   * Values of this line (used to copy lines into a new version).
   *
   * @return values
   */
  public BudgetLineValues values() {
    return new BudgetLineValues(accountId, accountCode, costCenter, months());
  }

  public Long getId() {
    return id;
  }

  public Budget getBudget() {
    return budget;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public String getCostCenter() {
    return costCenter;
  }
}
