package com.iortatechnxt.brokerverse.screening.matching.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.screening.matching.domain.MatchSearch;
import com.iortatechnxt.brokerverse.screening.matching.domain.MatchStatus;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatch;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatchRepository;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRun;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningRunRepository;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningTrigger;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of screening runs and matches (SNSRP-301, 602; FR-SS-030 run log, FR-SS-031 / 032 Matches
 * screen, client Screening tab).
 */
@Service
@Transactional(readOnly = true)
public class ScreeningQueries {

  private final ScreeningMatchRepository matches;
  private final ScreeningRunRepository runs;

  /**
   * Creates the queries.
   *
   * @param matches matches
   * @param runs runs
   */
  public ScreeningQueries(ScreeningMatchRepository matches, ScreeningRunRepository runs) {
    this.matches = matches;
    this.runs = runs;
  }

  /**
   * Searches the matches of a company.
   *
   * @param companyId company
   * @param filter filters
   * @param pageable page (sorted by the caller)
   * @return matches
   */
  public Page<ScreeningMatch> matches(Long companyId, MatchFilter filter, Pageable pageable) {
    String text = filter.text() == null ? "" : filter.text().trim().toLowerCase(Locale.ROOT);
    return matches.search(
        new MatchSearch(
            companyId,
            filter.status(),
            blankToNull(filter.listType()),
            filter.minScore(),
            filter.maxScore(),
            "%" + text + "%",
            filter.uncased()),
        pageable);
  }

  /**
   * The matches of a client, newest first.
   *
   * @param clientId client
   * @return matches
   */
  public List<ScreeningMatch> clientMatches(Long clientId) {
    return matches.findByClientIdOrderByIdDesc(clientId);
  }

  /**
   * The potential matches of a company not yet in a case (Screening Home).
   *
   * @param companyId company
   * @return count
   */
  public long openPotentialMatches(Long companyId) {
    return matches.countByCompanyIdAndStatusAndCaseIdIsNull(companyId, MatchStatus.POTENTIAL);
  }

  /**
   * The run log of a company.
   *
   * @param companyId company
   * @param trigger trigger, {@code null} for all
   * @param pageable page
   * @return runs, newest first
   */
  public Page<ScreeningRun> runs(Long companyId, ScreeningTrigger trigger, Pageable pageable) {
    return runs.search(companyId, trigger, pageable);
  }

  /**
   * One run.
   *
   * @param id run id
   * @return the run
   */
  public ScreeningRun run(Long id) {
    return runs.findById(id).orElseThrow(() -> new ResourceNotFoundException("ScreeningRun", id));
  }

  /**
   * The matches recorded by a run.
   *
   * @param runId run
   * @return matches
   */
  public List<ScreeningMatch> runMatches(Long runId) {
    return matches.findByRunIdOrderByIdAsc(runId);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  /**
   * Filters of the Matches screen.
   *
   * @param status status, {@code null} for all
   * @param listType list type, {@code null} for all
   * @param minScore lowest score, {@code null} for no bound
   * @param maxScore highest score, {@code null} for no bound
   * @param text client name or code, or entry name (contains)
   * @param uncased true for matches not yet in a case only
   */
  public record MatchFilter(
      MatchStatus status,
      String listType,
      BigDecimal minScore,
      BigDecimal maxScore,
      String text,
      boolean uncased) {}
}
