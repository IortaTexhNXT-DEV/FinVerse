package com.iortatechnxt.brokerverse.screening.matching.api.dto;

import com.iortatechnxt.brokerverse.screening.matching.domain.MatchStatus;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningQueries.MatchFilter;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Query parameters of the Matches screen (FR-SS-032 "filters by score, list type or client").
 *
 * @param companyId company
 * @param status status
 * @param listType list type
 * @param minScore lowest score
 * @param maxScore highest score
 * @param q client name or code, or entry name
 * @param uncased only matches not yet in a case
 * @param page page (default 0)
 * @param size page size (default 25)
 */
public record MatchQuery(
    @NotNull Long companyId,
    MatchStatus status,
    String listType,
    BigDecimal minScore,
    BigDecimal maxScore,
    String q,
    Boolean uncased,
    Integer page,
    Integer size) {

  private static final int DEFAULT_SIZE = 25;

  /**
   * The service filter.
   *
   * @return filter
   */
  public MatchFilter filter() {
    return new MatchFilter(status, listType, minScore, maxScore, q, Boolean.TRUE.equals(uncased));
  }

  /**
   * The page number.
   *
   * @return page, 0 by default
   */
  public int pageNumber() {
    return page == null ? 0 : Math.max(0, page);
  }

  /**
   * The page size, capped.
   *
   * @param max the largest size
   * @return size, 25 by default
   */
  public int pageSize(int max) {
    return size == null ? DEFAULT_SIZE : Math.min(Math.max(1, size), max);
  }
}
