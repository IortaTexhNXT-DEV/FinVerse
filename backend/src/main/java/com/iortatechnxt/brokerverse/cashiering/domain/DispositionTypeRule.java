package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * What a disposition type of LOV {@code DISPOSITION_TYPE} does and whether it needs the team
 * leader's approval (CSHID.024, OQ15). Types added to the LOV need a rule row.
 */
@Entity
@Table(name = "csh_disposition_type_rule")
public class DispositionTypeRule {

  @Id
  @Column(name = "type_code", length = 40)
  private String typeCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private DispositionAction action;

  @Column(name = "requires_approval", nullable = false)
  private boolean requiresApproval;

  @Column(nullable = false, length = 250)
  private String description;

  @Column(name = "income_event", length = 40)
  private String incomeEvent;

  @Column(name = "or_type", length = 40)
  private String orType;

  @Column(name = "vat_rate", precision = 9, scale = 6)
  private BigDecimal vatRate;

  protected DispositionTypeRule() {}

  public String getTypeCode() {
    return typeCode;
  }

  public DispositionAction getAction() {
    return action;
  }

  public boolean isRequiresApproval() {
    return requiresApproval;
  }

  public String getDescription() {
    return description;
  }

  /**
   * Accounting event of an income type (action INCOME), e.g. {@code SBM_HANDLING_FEE}.
   *
   * @return event type, null for the other actions
   */
  public String getIncomeEvent() {
    return incomeEvent;
  }

  /**
   * Official receipt type issued for an income type (LOV {@code OR_TYPE}).
   *
   * @return OR type, null for the other actions
   */
  public String getOrType() {
    return orType;
  }

  /**
   * Output VAT rate included in the amount of an income type (0.12 for 12 %).
   *
   * @return rate, null when the income carries no VAT
   */
  public BigDecimal getVatRate() {
    return vatRate;
  }
}
