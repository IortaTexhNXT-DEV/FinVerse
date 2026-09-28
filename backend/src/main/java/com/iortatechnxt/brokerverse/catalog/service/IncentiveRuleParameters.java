package com.iortatechnxt.brokerverse.catalog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * The rule parameters an incentive criterion may carry (PMADD07/08), by incentive type: each with
 * its business label and the kind of value. The criterion keeps them as one JSON object ({@code
 * {"minimumPremium": 5000}}), which the screen edits as parameter / value rows; this class checks
 * the rows in business words.
 */
@Component
public class IncentiveRuleParameters {

  private static final String INVALID = "INCENTIVE_RULE_PARAMS_INVALID";

  /** Kind of value of a parameter. */
  public enum ValueType {
    /** An amount in the company currency. */
    AMOUNT,
    /** A plain number. */
    NUMBER,
    /** A percentage. */
    PERCENT,
    /** A text. */
    TEXT
  }

  /**
   * A rule parameter.
   *
   * @param key key in the stored parameters
   * @param label business label
   * @param valueType kind of value
   * @param incentiveTypes incentive types it applies to; empty for every type
   */
  public record Parameter(
      String key, String label, ValueType valueType, Set<String> incentiveTypes) {

    /** Defensive copy. */
    public Parameter {
      incentiveTypes = Set.copyOf(incentiveTypes);
    }

    boolean appliesTo(String incentiveType) {
      return incentiveTypes.isEmpty() || incentiveTypes.contains(incentiveType);
    }
  }

  /**
   * The parameter catalogue. The minimum gross premium is read by the CPC2 incentive of the
   * remittance extraction; parameters of further incentive types are added here with their types
   * once their definitions are agreed.
   */
  private static final List<Parameter> PARAMETERS =
      List.of(new Parameter("minimumPremium", "Minimum Gross Premium", ValueType.AMOUNT, Set.of()));

  private final ObjectMapper json;

  /**
   * Creates the catalogue.
   *
   * @param json JSON (stored parameters)
   */
  public IncentiveRuleParameters(ObjectMapper json) {
    this.json = json;
  }

  /**
   * The parameters an incentive type may carry.
   *
   * @param incentiveType incentive type, null for every parameter
   * @return parameters
   */
  public List<Parameter> allowed(String incentiveType) {
    return PARAMETERS.stream()
        .filter(p -> incentiveType == null || p.appliesTo(incentiveType))
        .toList();
  }

  /**
   * Checks the parameters of a criterion: known parameters of its type, each with a value of the
   * right kind.
   *
   * @param incentiveType incentive type
   * @param params stored parameters (JSON object), null or blank for none
   */
  public void check(String incentiveType, String params) {
    if (params == null || params.isBlank()) {
      return;
    }
    JsonNode node;
    try {
      node = json.readTree(params);
    } catch (JsonProcessingException e) {
      node = null;
    }
    if (node == null || !node.isObject()) {
      throw new BusinessRuleException(
          INVALID, "Enter the rule parameters as parameter and value rows");
    }
    List<String> problems = new ArrayList<>();
    for (Map.Entry<String, JsonNode> field : node.properties()) {
      Optional<Parameter> parameter =
          allowed(incentiveType).stream().filter(p -> p.key().equals(field.getKey())).findFirst();
      if (parameter.isEmpty()) {
        problems.add(field.getKey() + " is not a parameter of this incentive type");
      } else {
        problem(parameter.get(), field.getValue()).ifPresent(problems::add);
      }
    }
    if (!problems.isEmpty()) {
      throw new BusinessRuleException(INVALID, String.join("; ", problems));
    }
  }

  private static Optional<String> problem(Parameter parameter, JsonNode value) {
    if (value == null || value.isNull() || value.asText().isBlank()) {
      return Optional.of("Enter a value for each parameter");
    }
    if (parameter.valueType() == ValueType.TEXT) {
      return Optional.empty();
    }
    if (!value.isNumber()) {
      return Optional.of(parameter.label() + " must be a number");
    }
    if (value.decimalValue().signum() < 0) {
      return Optional.of(parameter.label() + " cannot be negative");
    }
    if (parameter.valueType() == ValueType.PERCENT && value.decimalValue().intValue() > 100) {
      return Optional.of(parameter.label() + " must be between 0 and 100");
    }
    return Optional.empty();
  }
}
