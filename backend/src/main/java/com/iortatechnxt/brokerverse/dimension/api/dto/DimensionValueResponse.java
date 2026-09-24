package com.iortatechnxt.brokerverse.dimension.api.dto;

import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionValue;

/**
 * Dimension value view.
 *
 * @param id id
 * @param companyId company
 * @param type type
 * @param code code
 * @param name name
 * @param active active flag
 */
public record DimensionValueResponse(
    Long id, Long companyId, DimensionType type, String code, String name, boolean active) {

  /**
   * Maps an entity.
   *
   * @param v value
   * @return response
   */
  public static DimensionValueResponse from(DimensionValue v) {
    return new DimensionValueResponse(
        v.getId(), v.getCompanyId(), v.getType(), v.getCode(), v.getName(), v.isActive());
  }
}
