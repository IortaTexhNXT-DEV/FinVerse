package com.iortatechnxt.finverse.accounting.domain;

import com.iortatechnxt.finverse.common.domain.AuthorizableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Accounting rule: how an event type is journalised, optionally specialised by line of business and
 * currency and valid for a date range.
 *
 * <p>Selection: among active rules of the company and event type whose conditions match the event,
 * the most specific (most non-blank conditions) wins; ties are broken by the lowest priority value.
 * Rules are changed under maker-checker control.
 */
@Entity
@Table(name = "acc_rule")
public class AccountingRule extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "event_type", nullable = false, length = 40)
  private String eventType;

  @Column(nullable = false, length = 120)
  private String name;

  @Column(name = "business_line", length = 20)
  private String businessLine;

  @Column(length = 3)
  private String currency;

  @Column(nullable = false)
  private int priority = 100;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(name = "effective_to")
  private LocalDate effectiveTo;

  @OneToMany(
      mappedBy = "rule",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @OrderBy("lineNo")
  private final List<AccountingRuleLine> lines = new ArrayList<>();

  protected AccountingRule() {}

  /**
   * Creates a rule.
   *
   * @param companyId company
   * @param eventType event type code
   * @param name descriptive name
   * @param effectiveFrom first effective date
   */
  public AccountingRule(Long companyId, String eventType, String name, LocalDate effectiveFrom) {
    this.companyId = companyId;
    this.eventType = eventType;
    this.name = name;
    this.effectiveFrom = effectiveFrom;
  }

  /**
   * Replaces the rule lines.
   *
   * @param newLines lines in order
   */
  public void replaceLines(List<AccountingRuleLine> newLines) {
    lines.clear();
    int n = 1;
    for (AccountingRuleLine line : newLines) {
      line.attach(this, n++);
      lines.add(line);
    }
  }

  /**
   * Whether the rule applies to an event.
   *
   * @param eventBusinessLine event line of business (may be null)
   * @param eventCurrency event currency
   * @param date event value date
   * @return true when every condition matches
   */
  public boolean matches(String eventBusinessLine, String eventCurrency, LocalDate date) {
    boolean lobOk = businessLine == null || businessLine.equals(eventBusinessLine);
    boolean ccyOk = currency == null || currency.equals(eventCurrency);
    boolean dateOk =
        !date.isBefore(effectiveFrom) && (effectiveTo == null || !date.isAfter(effectiveTo));
    return isActive() && lobOk && ccyOk && dateOk;
  }

  /**
   * Number of specific conditions (used to pick the most specific rule).
   *
   * @return specificity score
   */
  public int specificity() {
    return (businessLine == null ? 0 : 1) + (currency == null ? 0 : 1);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getBusinessLine() {
    return businessLine;
  }

  public void setBusinessLine(String businessLine) {
    this.businessLine = businessLine;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public void setEffectiveFrom(LocalDate effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  public LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  public void setEffectiveTo(LocalDate effectiveTo) {
    this.effectiveTo = effectiveTo;
  }

  public List<AccountingRuleLine> getLines() {
    return List.copyOf(lines);
  }
}
