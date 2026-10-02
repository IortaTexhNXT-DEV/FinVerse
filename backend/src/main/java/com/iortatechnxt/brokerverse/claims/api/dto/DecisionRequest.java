package com.iortatechnxt.brokerverse.claims.api.dto;

import java.time.LocalDate;

/**
 * Checker approval options.
 *
 * @param accountingDate accounting date of the posting; default today
 */
public record DecisionRequest(LocalDate accountingDate) {

  /**
   * Accounting date of an optional request.
   *
   * @param request request, may be null
   * @return date or null
   */
  public static LocalDate dateOf(DecisionRequest request) {
    return request == null ? null : request.accountingDate();
  }
}
