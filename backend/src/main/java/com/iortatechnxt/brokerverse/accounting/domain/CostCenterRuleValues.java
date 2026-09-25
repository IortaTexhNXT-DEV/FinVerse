package com.iortatechnxt.brokerverse.accounting.domain;

/**
 * Values of a cost-centre rule (FRBS 3.1.1). Blank criteria are stored as null (match all).
 *
 * @param priority evaluation order, lowest first (1 to 9999)
 * @param sourceModule publishing module criterion
 * @param eventType event type criterion
 * @param branchId branch criterion
 * @param partyCode party criterion
 * @param accountCode GL account criterion
 * @param costCenter cost centre given to the line
 * @param description description
 * @param active whether the rule is applied
 */
public record CostCenterRuleValues(
    int priority,
    String sourceModule,
    String eventType,
    Long branchId,
    String partyCode,
    String accountCode,
    String costCenter,
    String description,
    boolean active) {

  /** Blank criteria become null. */
  public CostCenterRuleValues {
    sourceModule = blankToNull(sourceModule);
    eventType = blankToNull(eventType);
    partyCode = blankToNull(partyCode);
    accountCode = blankToNull(accountCode);
    description = blankToNull(description);
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
