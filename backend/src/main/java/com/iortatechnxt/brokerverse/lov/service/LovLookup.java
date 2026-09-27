package com.iortatechnxt.brokerverse.lov.service;

import com.iortatechnxt.brokerverse.lov.domain.LovTypeRepository;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.domain.LovValueRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cached read of a whole list of values ({@link LovCaches#VALUES}), used by the pick lists, label
 * look-ups and optional-field validation of {@link LovService}.
 */
@Service
public class LovLookup {

  private final LovTypeRepository types;
  private final LovValueRepository values;

  /**
   * Creates the lookup.
   *
   * @param types list types
   * @param values list values
   */
  public LovLookup(LovTypeRepository types, LovValueRepository values) {
    this.types = types;
    this.values = values;
  }

  /**
   * Every value of a list, whatever its status.
   *
   * @param typeCode list type
   * @return snapshot (unknown lists are empty with {@code known = false})
   */
  @Cacheable(cacheNames = LovCaches.VALUES, key = "#typeCode")
  @Transactional(readOnly = true)
  public LovEntries entries(String typeCode) {
    boolean known = types.findByCode(typeCode).isPresent();
    return new LovEntries(
        typeCode,
        known,
        values.findByTypeCodeOrderBySortOrderAscLabelAsc(typeCode).stream()
            .map(LovLookup::entry)
            .toList());
  }

  private static LovEntries.Entry entry(LovValue v) {
    return new LovEntries.Entry(
        v.getCode(),
        v.getLabel(),
        v.getParentCode(),
        v.isActive(),
        v.getEffectiveFrom(),
        v.getEffectiveTo());
  }
}
