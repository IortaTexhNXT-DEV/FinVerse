package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.DocumentRule;
import com.iortatechnxt.brokerverse.catalog.domain.RuleScope;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;

/**
 * A document checklist row.
 *
 * @param id id
 * @param scope scope
 * @param scopeCode '*', line or product
 * @param documentType document type
 * @param required mandatory
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record DocumentRuleResponse(
    Long id,
    RuleScope scope,
    String scopeCode,
    String documentType,
    boolean required,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param e entity
   * @return response
   */
  public static DocumentRuleResponse from(DocumentRule e) {
    return new DocumentRuleResponse(
        e.getId(),
        e.getScope(),
        e.getScopeCode(),
        e.getDocumentType(),
        e.isRequired(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
