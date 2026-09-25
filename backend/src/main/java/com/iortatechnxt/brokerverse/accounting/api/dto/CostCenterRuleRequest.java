package com.iortatechnxt.brokerverse.accounting.api.dto;

import com.iortatechnxt.brokerverse.accounting.domain.CostCenterRuleValues;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Cost-centre rule (FRBS 3.1.1). Blank criteria match everything.
 *
 * @param companyId company (ignored on update)
 * @param priority evaluation order, lowest first
 * @param sourceModule publishing module criterion
 * @param eventType event type criterion
 * @param branchId branch criterion
 * @param partyCode party criterion
 * @param accountCode GL account criterion
 * @param costCenter cost centre given to the line
 * @param description description
 * @param active whether the rule is applied
 */
public record CostCenterRuleRequest(
    @NotNull Long companyId,
    @Min(1) @Max(9999) int priority,
    @Size(max = 30) String sourceModule,
    @Size(max = 40) String eventType,
    Long branchId,
    @Size(max = 30) String partyCode,
    @Size(max = 30) String accountCode,
    @NotBlank @Size(max = 20) String costCenter,
    @Size(max = 200) String description,
    boolean active) {

  /**
   * The rule values.
   *
   * @return values
   */
  public CostCenterRuleValues values() {
    return new CostCenterRuleValues(
        priority,
        sourceModule,
        eventType,
        branchId,
        partyCode,
        accountCode,
        costCenter,
        description,
        active);
  }
}
