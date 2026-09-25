package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.catalog.domain.FieldRule;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRuleType;
import com.iortatechnxt.brokerverse.catalog.domain.FieldTarget;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Typed checks of field rules on given values (BRPM.004): a code of a list of values, a number
 * within a range, a text matching a pattern. Used by {@link ProductRuleService}.
 */
@Component
class FieldValueRules {

  private final LovService lovs;
  private final Clock clock;

  FieldValueRules(LovService lovs, Clock clock) {
    this.lovs = lovs;
    this.clock = clock;
  }

  /**
   * Refuses a LOV rule on an unknown list of values.
   *
   * @param check rule check, may be null
   */
  void requireLovType(FieldRule.Check check) {
    boolean lov = check != null && check.type() == FieldRuleType.LOV;
    if (lov && lovs.types().stream().noneMatch(t -> t.getCode().equals(check.lovType()))) {
      throw new BusinessRuleException(
          "LOV_TYPE_UNKNOWN", "Unknown list of values " + check.lovType());
    }
  }

  /**
   * The violations of given values against typed rules.
   *
   * @param rules effective field rules of the product
   * @param values given values
   * @return field errors by path
   */
  Map<String, String> violations(List<FieldRule> rules, FieldValues values) {
    Map<String, String> errors = new LinkedHashMap<>();
    LocalDate today = LocalDate.now(clock);
    for (FieldRule rule : rules) {
      if (rule.getRuleType() == FieldRuleType.REQUIRED) {
        continue;
      }
      if (rule.getTarget() == FieldTarget.ACCOUNT) {
        check(rule, rule.getFieldKey(), values.record(), today, errors);
      } else {
        for (int i = 0; i < values.items().size(); i++) {
          check(
              rule, "items[" + i + "]." + rule.getFieldKey(), values.items().get(i), today, errors);
        }
      }
    }
    return errors;
  }

  private void check(
      FieldRule rule,
      String path,
      Map<String, String> values,
      LocalDate today,
      Map<String, String> errors) {
    String value = values.get(rule.getFieldKey());
    if (value != null) {
      violation(rule, value, today).ifPresent(message -> errors.put(path, message));
    }
  }

  private Optional<String> violation(FieldRule rule, String value, LocalDate today) {
    if (rule.getRuleType() != FieldRuleType.LOV) {
      return rule.formatViolation(value);
    }
    boolean valid =
        lovs.activeValues(rule.getLovType(), today).stream()
            .anyMatch(v -> v.getCode().equals(value));
    return valid ? Optional.empty() : Optional.of(rule.getLabel() + " is not a valid value");
  }
}
