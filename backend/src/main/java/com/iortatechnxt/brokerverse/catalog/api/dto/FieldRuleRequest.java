package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.FieldRule;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRule.RuleKey;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRuleType;
import com.iortatechnxt.brokerverse.catalog.domain.FieldTarget;
import com.iortatechnxt.brokerverse.catalog.domain.RuleScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * New or changed minimum-field rule.
 *
 * @param scope scope (ignored on update)
 * @param scopeCode '*', line or product (ignored on update)
 * @param target account or item (ignored on update)
 * @param fieldKey field key, alternatives separated by |
 * @param label label
 * @param required mandatory
 * @param sortOrder order
 * @param ruleType what a given value is checked against (BRPM.004), REQUIRED when empty
 * @param lovType list of values (LOV rules)
 * @param minValue minimum (RANGE rules)
 * @param maxValue maximum (RANGE rules)
 * @param pattern regular expression (PATTERN rules)
 */
public record FieldRuleRequest(
    @NotNull RuleScope scope,
    @NotBlank @Size(max = 30) String scopeCode,
    @NotNull FieldTarget target,
    @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9|]+") String fieldKey,
    @NotBlank @Size(max = 120) String label,
    boolean required,
    @PositiveOrZero int sortOrder,
    FieldRuleType ruleType,
    @Size(max = 40) String lovType,
    BigDecimal minValue,
    BigDecimal maxValue,
    @Size(max = 200) String pattern) {

  /**
   * What the rule checks.
   *
   * @return check
   */
  public FieldRule.Check check() {
    return new FieldRule.Check(ruleType, lovType, minValue, maxValue, pattern);
  }

  /**
   * Identity of the rule.
   *
   * @return key
   */
  public RuleKey key() {
    return new RuleKey(scope, scopeCode, target, fieldKey);
  }
}
