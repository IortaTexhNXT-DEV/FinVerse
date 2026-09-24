package com.iortatechnxt.brokerverse.lov.api.dto;

import com.iortatechnxt.brokerverse.lov.domain.LovValue;

/**
 * A pick-list option.
 *
 * @param code code
 * @param label label
 * @param parentCode parent value (dependent lists)
 */
public record LovOption(String code, String label, String parentCode) {

  /**
   * Maps a value.
   *
   * @param v entity
   * @return option
   */
  public static LovOption from(LovValue v) {
    return new LovOption(v.getCode(), v.getLabel(), v.getParentCode());
  }
}
