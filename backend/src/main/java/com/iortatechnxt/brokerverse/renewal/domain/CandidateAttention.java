package com.iortatechnxt.brokerverse.renewal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDate;

/**
 * The attention flag of a renewal in the centralised listing (BRRN.036; FR-RN-102): ageing, overdue
 * or high risk, with the rule that set it and the day it was first set. Escalation is supported by
 * visibility only: no alert follows the flag.
 */
@Embeddable
public class CandidateAttention {

  @Enumerated(EnumType.STRING)
  @Column(name = "attention_flag", length = 20)
  private AttentionFlag flag;

  @Column(name = "attention_rule", length = 200)
  private String rule;

  @Column(name = "attention_since")
  private LocalDate since;

  /**
   * Sets or clears the flag; the first day is kept while the flag stays the same.
   *
   * @param newFlag flag, null to clear
   * @param newRule rule that set it
   * @param today business date
   */
  public void set(AttentionFlag newFlag, String newRule, LocalDate today) {
    if (newFlag == null) {
      this.flag = null;
      this.rule = null;
      this.since = null;
      return;
    }
    if (newFlag != flag) {
      this.since = today;
    }
    this.flag = newFlag;
    this.rule = newRule;
  }

  public AttentionFlag getFlag() {
    return flag;
  }

  public String getRule() {
    return rule;
  }

  public LocalDate getSince() {
    return since;
  }
}
