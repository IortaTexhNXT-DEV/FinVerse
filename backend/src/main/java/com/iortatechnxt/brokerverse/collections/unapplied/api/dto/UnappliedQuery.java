package com.iortatechnxt.brokerverse.collections.unapplied.api.dto;

import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedWorklistService.CollectorFilter;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Query parameters of the collector list of unapplied payments (BRCLXN.034-036): text, matched
 * client, marketing unit, Cashiering tab, payment dates, age, market segment and collector
 * disposition ({@code NONE} = none yet).
 *
 * @param q reference, payor, transaction, check, reference or invoice
 * @param clientCode matched client
 * @param salesUnit marketing unit
 * @param tab Cashiering tab
 * @param paidFrom payment date from
 * @param paidTo payment date to
 * @param ageMin minimum age in days
 * @param ageMax maximum age in days
 * @param segment market segment
 * @param disposition collector disposition
 */
public record UnappliedQuery(
    String q,
    String clientCode,
    String salesUnit,
    String tab,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidTo,
    Integer ageMin,
    Integer ageMax,
    String segment,
    String disposition) {

  /**
   * The service filter, blank values ignored.
   *
   * @return filter
   */
  public CollectorFilter toFilter() {
    return new CollectorFilter(
        blankToNull(q),
        blankToNull(clientCode),
        blankToNull(salesUnit),
        blankToNull(tab),
        paidFrom,
        paidTo,
        ageMin,
        ageMax,
        blankToNull(segment),
        blankToNull(disposition));
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
