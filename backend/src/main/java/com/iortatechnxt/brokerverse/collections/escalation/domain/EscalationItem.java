package com.iortatechnxt.brokerverse.collections.escalation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** One invoice of an escalation with its outstanding and aging when escalated (BRCLXN.050 AC 1). */
@Entity
@Table(name = "clx_escalation_item")
public class EscalationItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "escalation_id", nullable = false, updatable = false)
  private Escalation escalation;

  @Column(name = "invoice_no", nullable = false, length = 40, updatable = false)
  private String invoiceNo;

  @Column(name = "policy_no", length = 60, updatable = false)
  private String policyNo;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal balance;

  @Column(name = "aging_days", nullable = false, updatable = false)
  private int agingDays;

  protected EscalationItem() {}

  EscalationItem(Escalation escalation, Facts facts) {
    this.escalation = escalation;
    this.invoiceNo = facts.invoiceNo();
    this.policyNo = facts.policyNo();
    this.balance = facts.balance();
    this.agingDays = facts.agingDays();
  }

  public Long getId() {
    return id;
  }

  public Escalation getEscalation() {
    return escalation;
  }

  public String getInvoiceNo() {
    return invoiceNo;
  }

  public String getPolicyNo() {
    return policyNo;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public int getAgingDays() {
    return agingDays;
  }

  /**
   * An escalated invoice.
   *
   * @param invoiceNo invoice
   * @param policyNo policy number, may be null
   * @param balance outstanding premium when escalated
   * @param agingDays days since booking when escalated
   */
  public record Facts(String invoiceNo, String policyNo, BigDecimal balance, int agingDays) {}
}
