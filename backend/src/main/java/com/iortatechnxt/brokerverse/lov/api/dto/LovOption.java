package com.iortatechnxt.brokerverse.lov.api.dto;

import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovEntries;

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

  /**
   * Maps a cached value.
   *
   * @param v cached value
   * @return option
   */
  public static LovOption from(LovEntries.Entry v) {
    return new LovOption(v.code(), v.label(), v.parentCode());
  }
}
