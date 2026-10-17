package com.iortatechnxt.brokerverse.coa.api.dto;

import com.iortatechnxt.brokerverse.coa.domain.CoaNumbering;

/**
 * Numbering scheme view.
 *
 * @param id id
 * @param parentCode parent account code
 * @param separator separator
 * @param width digits
 * @param active whether codes are proposed
 * @param example code of the first child
 */
public record CoaNumberingResponse(
    Long id, String parentCode, String separator, int width, boolean active, String example) {

  /**
   * Maps an entity.
   *
   * @param n scheme
   * @return response
   */
  public static CoaNumberingResponse from(CoaNumbering n) {
    return new CoaNumberingResponse(
        n.getId(), n.getParentCode(), n.getSeparator(), n.getWidth(), n.isActive(), n.codeOf(1));
  }
}
