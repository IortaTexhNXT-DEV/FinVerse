package com.iortatechnxt.brokerverse.coa.api.dto;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.GlCategory;

/**
 * GL category view.
 *
 * @param id id
 * @param code code
 * @param name name
 * @param accountClass class
 * @param bankCategory bank flag
 */
public record GlCategoryResponse(
    Long id, String code, String name, AccountClass accountClass, boolean bankCategory) {

  /**
   * Maps an entity.
   *
   * @param c category
   * @return response
   */
  public static GlCategoryResponse from(GlCategory c) {
    return new GlCategoryResponse(
        c.getId(), c.getCode(), c.getName(), c.getAccountClass(), c.isBankCategory());
  }
}
