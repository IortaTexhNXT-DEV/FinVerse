package com.iortatechnxt.brokerverse.screening.matching.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.screening.matching.api.dto.FalsePositiveRequest;
import com.iortatechnxt.brokerverse.screening.matching.api.dto.MatchDetail;
import com.iortatechnxt.brokerverse.screening.matching.api.dto.MatchQuery;
import com.iortatechnxt.brokerverse.screening.matching.api.dto.MatchRow;
import com.iortatechnxt.brokerverse.screening.matching.api.dto.RunDto;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningMatch;
import com.iortatechnxt.brokerverse.screening.matching.domain.ScreeningTrigger;
import com.iortatechnxt.brokerverse.screening.matching.service.MatchCaseOpener;
import com.iortatechnxt.brokerverse.screening.matching.service.MatchDecisionService;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningEngine;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningQueries;
import com.iortatechnxt.brokerverse.screening.matching.service.ScreeningResult;
import com.iortatechnxt.brokerverse.screening.watchlist.service.WatchlistDirectory;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Screening matches and runs (SNSRP-301, 304, 602; FR-SS-030 to 032): the Matches screen (search,
 * side-by-side comparison, "Open Case", "Mark False Positive"), the run log and "Screen Now" for a
 * client.
 */
@RestController
@RequestMapping("/api/v1/screening")
public class ScreeningMatchController {

  /** Guard: screening view. */
  static final String HAS_VIEW = "hasAuthority('SCR_VIEW')";

  /** Guard: investigation (match decisions). */
  static final String HAS_INVESTIGATE = "hasAuthority('SCR_INVESTIGATE')";

  private static final int MAX_PAGE = 200;

  private final ScreeningQueries queries;
  private final MatchDecisionService decisions;
  private final ScreeningEngine engine;
  private final ClientService clients;
  private final WatchlistDirectory directory;

  /**
   * Creates the controller.
   *
   * @param queries reads
   * @param decisions match decisions
   * @param engine engine (manual screening)
   * @param clients client master (read)
   * @param directory watchlist entries (read)
   */
  public ScreeningMatchController(
      ScreeningQueries queries,
      MatchDecisionService decisions,
      ScreeningEngine engine,
      ClientService clients,
      WatchlistDirectory directory) {
    this.queries = queries;
    this.decisions = decisions;
    this.engine = engine;
    this.clients = clients;
    this.directory = directory;
  }

  /**
   * Searches matches (Matches screen, FR-SS-032), highest score first.
   *
   * @param query company, filters and page
   * @return matches
   */
  @GetMapping("/matches")
  @PreAuthorize(HAS_VIEW)
  public PageResponse<MatchRow> matches(@Valid MatchQuery query) {
    return PageResponse.of(
        queries.matches(
            query.companyId(),
            query.filter(),
            PageRequest.of(
                query.pageNumber(),
                query.pageSize(MAX_PAGE),
                Sort.by(Sort.Order.desc("score"), Sort.Order.desc("id")))),
        MatchRow::from);
  }

  /**
   * A match with the client and the entry side by side.
   *
   * @param id match id
   * @return detail
   */
  @GetMapping("/matches/{id}")
  @PreAuthorize(HAS_VIEW)
  public MatchDetail match(@PathVariable Long id) {
    ScreeningMatch m = decisions.get(id);
    return MatchDetail.from(
        MatchRow.from(m),
        clients.get(m.getClientId()),
        directory.entry(m.getEntryId()).orElse(null));
  }

  /**
   * Marks a match a false positive with justification and evidence (FR-SS-032, 035).
   *
   * @param id match id
   * @param request justification, evidence, optional profile correction
   * @return the match
   */
  @PostMapping("/matches/{id}/false-positive")
  @PreAuthorize(HAS_INVESTIGATE)
  public MatchRow falsePositive(
      @PathVariable Long id, @Valid @RequestBody FalsePositiveRequest request) {
    decisions.markFalsePositive(id, request.toRequest());
    return MatchRow.from(decisions.get(id));
  }

  /**
   * Opens a case for a potential match or adds it to the client's open case (FR-SS-032).
   *
   * @param id match id
   * @return the case
   */
  @PostMapping("/matches/{id}/open-case")
  @PreAuthorize(HAS_INVESTIGATE)
  public MatchCaseOpener.OpenedCase openCase(@PathVariable Long id) {
    return decisions.openCase(id);
  }

  /**
   * The matches of a client (client Screening tab).
   *
   * @param clientId client
   * @return matches, newest first
   */
  @GetMapping("/clients/{clientId}/matches")
  @PreAuthorize(HAS_VIEW)
  public List<MatchRow> clientMatches(@PathVariable Long clientId) {
    return queries.clientMatches(clientId).stream().map(MatchRow::from).toList();
  }

  /**
   * Screens a client now against the whole list (trigger MANUAL).
   *
   * @param clientId client
   * @return the run; 204 when the client is inactive or no criteria are in force
   */
  @PostMapping("/clients/{clientId}/screen")
  @PreAuthorize(HAS_INVESTIGATE)
  public ResponseEntity<RunDto> screen(@PathVariable Long clientId) {
    return engine
        .screenClient(clientId, ScreeningTrigger.MANUAL, null)
        .map(ScreeningResult::runId)
        .map(queries::run)
        .map(RunDto::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /**
   * The run log of a company, newest first.
   *
   * @param companyId company
   * @param trigger trigger
   * @param page page
   * @param size size
   * @return runs
   */
  @GetMapping("/runs")
  @PreAuthorize(HAS_VIEW)
  public PageResponse<RunDto> runs(
      @RequestParam Long companyId,
      @RequestParam(required = false) ScreeningTrigger trigger,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return PageResponse.of(
        queries.runs(companyId, trigger, PageRequest.of(page, Math.min(size, MAX_PAGE))),
        RunDto::from);
  }

  /**
   * One run.
   *
   * @param id run id
   * @return the run
   */
  @GetMapping("/runs/{id}")
  @PreAuthorize(HAS_VIEW)
  public RunDto run(@PathVariable Long id) {
    return RunDto.from(queries.run(id));
  }

  /**
   * The matches recorded by a run.
   *
   * @param id run id
   * @return matches
   */
  @GetMapping("/runs/{id}/matches")
  @PreAuthorize(HAS_VIEW)
  public List<MatchRow> runMatches(@PathVariable Long id) {
    return queries.runMatches(id).stream().map(MatchRow::from).toList();
  }
}
