package com.iortatechnxt.brokerverse.accounting.api.dto;

import com.iortatechnxt.brokerverse.accounting.domain.CostCenterRule;
import java.time.Instant;

/**
 * Cost-centre rule view.
 *
 * @param id id
 * @param priority evaluation order
 * @param sourceModule module criterion
 * @param eventType event type criterion
 * @param branchId branch criterion
 * @param partyCode party criterion
 * @param accountCode account criterion
 * @param costCenter cost centre
 * @param description description
 * @param active whether applied
 * @param updatedBy last maintained by
 * @param updatedAt last maintained at
 */
public record CostCenterRuleResponse(
    Long id,
    int priority,
    String sourceModule,
    String eventType,
    Long branchId,
    String partyCode,
    String accountCode,
    String costCenter,
    String description,
    boolean active,
    String updatedBy,
    Instant updatedAt) {

  /**
   * Maps an entity.
   *
   * @param r rule
   * @return response
   */
  public static CostCenterRuleResponse from(CostCenterRule r) {
    return new CostCenterRuleResponse(
        r.getId(),
        r.getPriority(),
        r.getSourceModule(),
        r.getEventType(),
        r.getBranchId(),
        r.getPartyCode(),
        r.getAccountCode(),
        r.getCostCenter(),
        r.getDescription(),
        r.isActive(),
        r.getUpdatedBy() != null ? r.getUpdatedBy() : r.getCreatedBy(),
        r.getUpdatedAt() != null ? r.getUpdatedAt() : r.getCreatedAt());
  }
}
