package com.iortatechnxt.brokerverse.lov.api.dto;

import com.iortatechnxt.brokerverse.lov.domain.LovType;

/**
 * A list type.
 *
 * @param code code
 * @param name name
 * @param description description
 * @param maintainable whether business administrators maintain it
 */
public record LovTypeResponse(String code, String name, String description, boolean maintainable) {

  /**
   * Maps a type.
   *
   * @param type entity
   * @return response
   */
  public static LovTypeResponse from(LovType type) {
    return new LovTypeResponse(
        type.getCode(), type.getName(), type.getDescription(), type.isMaintainable());
  }
}
