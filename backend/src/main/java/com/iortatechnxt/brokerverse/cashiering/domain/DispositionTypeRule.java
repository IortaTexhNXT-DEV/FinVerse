package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
}
