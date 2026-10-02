package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.FieldRule;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRuleType;
import com.iortatechnxt.brokerverse.catalog.domain.FieldTarget;
import com.iortatechnxt.brokerverse.catalog.domain.RuleScope;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.math.BigDecimal;

/**
 * A minimum-field matrix row.
 *
 * @param id id
 * @param scope scope
 * @param scopeCode '*', line or product
 * @param target account or item
 * @param fieldKey field key
 * @param label label
 * @param required mandatory
 * @param sortOrder order
 * @param ruleType REQUIRED, LOV, RANGE or PATTERN (BRPM.004)
 * @param lovType list of values
 * @param minValue minimum
 * @param maxValue maximum
 * @param pattern regular expression
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record FieldRuleResponse(
    Long id,
    RuleScope scope,
    String scopeCode,
    FieldTarget target,
    String fieldKey,
    String label,
    boolean required,
    int sortOrder,
    FieldRuleType ruleType,
    String lovType,
    BigDecimal minValue,
    BigDecimal maxValue,
    String pattern,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param e entity
   * @return response
   */
  public static FieldRuleResponse from(FieldRule e) {
    return new FieldRuleResponse(
        e.getId(),
        e.getScope(),
        e.getScopeCode(),
        e.getTarget(),
        e.getFieldKey(),
        e.getLabel(),
        e.isRequired(),
        e.getSortOrder(),
        e.getRuleType(),
        e.getLovType(),
        e.getMinValue(),
        e.getMaxValue(),
        e.getPattern(),
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
