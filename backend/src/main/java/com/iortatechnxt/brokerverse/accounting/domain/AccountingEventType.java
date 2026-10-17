package com.iortatechnxt.brokerverse.accounting.domain;

import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Accounting event master (e.g. POLICY_ISSUE, CLAIM_PAYMENT). Reference data seeded by migration;
 * the event code is the natural key used by publishing modules and accounting rules.
 */
@Entity
@Table(name = "acc_event_type")
public class AccountingEventType {

  @Id
  @Column(length = 40)
  private String code;

  @Column(nullable = false, length = 120)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EventCategory category;

  @Enumerated(EnumType.STRING)
  @Column(name = "journal_type", nullable = false, length = 20)
  private JournalType journalType;

  @Column(length = 500)
  private String description;

  @Column(name = "amount_components", nullable = false, length = 500)
  private String amountComponents;

  @Column(nullable = false)
  private boolean active;

  protected AccountingEventType() {}

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public EventCategory getCategory() {
    return category;
  }

  public JournalType getJournalType() {
    return journalType;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Comma separated amount components the publishing module supplies (documentation for rule
   * designers), e.g. "GROSS_PREMIUM,DST,VAT,TOTAL_DUE".
   *
   * @return components
   */
  public String getAmountComponents() {
    return amountComponents;
  }

  public boolean isActive() {
    return active;
  }
}
