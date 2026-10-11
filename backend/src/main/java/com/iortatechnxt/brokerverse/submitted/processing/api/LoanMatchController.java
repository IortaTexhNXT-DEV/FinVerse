package com.iortatechnxt.brokerverse.submitted.processing.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoanMatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoanMatchRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRun;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.MasterlistController;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService;
import com.iortatechnxt.brokerverse.submitted.service.SbmScopeService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Loan file results of a processing run (FR-SP-035): per record, the loan file and key that
 * matched, or the reason it is unmatched, with the time stamp, within the user's scope.
 */
@RestController
@RequestMapping("/api/v1/submitted/runs")
public class LoanMatchController {

  private final SbmProcessingService processing;
  private final SbmLoanMatchRepository matches;
  private final SbmPolicyRepository policies;
  private final SbmScopeService scope;

  /**
   * Creates the controller.
   *
   * @param processing processing runs
   * @param matches matching log
   * @param policies masterlist
   * @param scope data scope
   */
  public LoanMatchController(
      SbmProcessingService processing,
      SbmLoanMatchRepository matches,
      SbmPolicyRepository policies,
      SbmScopeService scope) {
    this.processing = processing;
    this.matches = matches;
    this.policies = policies;
    this.scope = scope;
  }

  /**
   * The loan file results of a run, optionally of one outcome (UNMATCHED for review).
   *
   * @param id run
   * @param outcome MATCHED or UNMATCHED, may be empty
   * @param pageable page
   * @return results
   */
  @GetMapping("/{id}/loan-matches")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public PageResponse<LoanMatchView> loanMatches(
      @PathVariable Long id,
      @RequestParam(required = false) SbmLoanMatch.Outcome outcome,
      Pageable pageable) {
    SbmRun run = processing.require(id);
    Page<SbmLoanMatch> page =
        outcome == null
            ? matches.findByRunIdOrderByIdAsc(id, pageable)
            : matches.findByRunIdAndOutcomeOrderByIdAsc(id, outcome, pageable);
    Map<Long, SbmPolicy> byId =
        policies
            .findAllById(
                page.getContent().stream().map(SbmLoanMatch::getPolicyId).distinct().toList())
            .stream()
            .filter(scope.current(run.getCompanyId())::allows)
            .collect(Collectors.toMap(SbmPolicy::getId, Function.identity()));
    return PageResponse.of(page, m -> LoanMatchView.of(m, byId.get(m.getPolicyId())));
  }

  /**
   * The matching of one record.
   *
   * @param id log row
   * @param policyId record
   * @param sbmNo record number, null outside the user's scope
   * @param assuredName assured, null outside the user's scope
   * @param outcome MATCHED or UNMATCHED
   * @param keyUsed PN or LOAN_APPLICATION
   * @param loanReport loan report of the file
   * @param fileDate date of the file
   * @param uploadNo upload of the file
   * @param reason why unmatched
   * @param matchedAt time stamp
   */
  public record LoanMatchView(
      Long id,
      Long policyId,
      String sbmNo,
      String assuredName,
      String outcome,
      String keyUsed,
      String loanReport,
      LocalDate fileDate,
      String uploadNo,
      String reason,
      Instant matchedAt) {

    static LoanMatchView of(SbmLoanMatch m, SbmPolicy p) {
      return new LoanMatchView(
          m.getId(),
          m.getPolicyId(),
          p == null ? null : p.getSbmNo(),
          p == null ? null : p.getAssured().assuredName(),
          m.getOutcome().name(),
          m.getKeyUsed() == null ? null : m.getKeyUsed().name(),
          m.getLoanReport(),
          m.getSnapshotDate(),
          m.getBulkJobNo(),
          m.getReason(),
          m.getMatchedAt());
    }
  }
}
