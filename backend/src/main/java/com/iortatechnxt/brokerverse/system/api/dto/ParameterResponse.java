package com.iortatechnxt.brokerverse.system.api.dto;

import com.iortatechnxt.brokerverse.system.domain.ParameterValueType;
import com.iortatechnxt.brokerverse.system.domain.SystemParameter;
import java.time.Instant;

/**
 * Business parameter view.
 *
 * @param key key
 * @param value current value
 * @param valueType data type
 * @param category category
 * @param description description
 * @param minValue lower bound (numbers)
 * @param maxValue upper bound (numbers)
 * @param updatedBy last modifier
 * @param updatedAt last modification
 * @param secondApproval whether a change waits for a second approval (security parameters)
 * @param pendingValue value waiting for approval, null for none
 * @param pendingBy user who asked for it
 * @param pendingAt when it was asked for
 */
public record ParameterResponse(
    String key,
    String value,
    ParameterValueType valueType,
    String category,
    String description,
    Integer minValue,
    Integer maxValue,
    String updatedBy,
    Instant updatedAt,
    boolean secondApproval,
    String pendingValue,
    String pendingBy,
    Instant pendingAt) {

  /**
   * Maps an entity.
   *
   * @param p parameter
   * @return response
   */
  public static ParameterResponse from(SystemParameter p) {
    return new ParameterResponse(
        p.getKey(),
        p.getValue(),
        p.getValueType(),
        p.getCategory(),
        p.getDescription(),
        p.getMinValue(),
        p.getMaxValue(),
        p.getUpdatedBy(),
        p.getUpdatedAt(),
        p.isSecurity(),
        p.getPendingValue(),
        p.getPendingBy(),
        p.getPendingAt());
  }
}
