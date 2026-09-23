package com.iortatechnxt.finverse.accounting.api.dto;

import com.iortatechnxt.finverse.accounting.domain.AccountingRule;
import com.iortatechnxt.finverse.common.domain.RecordStatus;
import java.time.LocalDate;
import java.util.List;

/**
 * Accounting rule view.
 *
 * @param id id
 * @param companyId company
 * @param eventType event type
 * @param name name
 * @param businessLine line-of-business condition
 * @param currency currency condition
 * @param priority priority
 * @param effectiveFrom effective from
 * @param effectiveTo effective to
 * @param lines lines
 * @param recordStatus maker-checker status
 * @param createdBy maker
 * @param authorizedBy checker
 */
public record RuleResponse(
    Long id,
    Long companyId,
    String eventType,
    String name,
    String businessLine,
    String currency,
    int priority,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    List<RuleLineDto> lines,
    RecordStatus recordStatus,
    String createdBy,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param r rule
   * @return response
   */
  public static RuleResponse from(AccountingRule r) {
    return new RuleResponse(
        r.getId(),
        r.getCompanyId(),
        r.getEventType(),
        r.getName(),
        r.getBusinessLine(),
        r.getCurrency(),
        r.getPriority(),
        r.getEffectiveFrom(),
        r.getEffectiveTo(),
        r.getLines().stream().map(RuleLineDto::from).toList(),
        r.getRecordStatus(),
        r.getCreatedBy(),
        r.getAuthorizedBy());
  }
}
