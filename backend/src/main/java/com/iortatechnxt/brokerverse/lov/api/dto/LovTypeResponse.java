package com.iortatechnxt.brokerverse.lov.api.dto;

import com.iortatechnxt.brokerverse.lov.domain.LovType;

/**
 * A list type.
 *
 * @param code code
 * @param name name
 * @param description description
 * @param maintainable whether business administrators maintain it
 * @param ownerPermission permission that also maintains the values (null = LOV_MANAGE only)
 */
public record LovTypeResponse(
    String code, String name, String description, boolean maintainable, String ownerPermission) {

  /**
   * Maps a type.
   *
   * @param type entity
   * @return response
   */
  public static LovTypeResponse from(LovType type) {
    return new LovTypeResponse(
        type.getCode(),
        type.getName(),
        type.getDescription(),
        type.isMaintainable(),
        type.getOwnerPermission());
  }
}
