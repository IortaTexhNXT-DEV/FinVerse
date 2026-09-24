package com.iortatechnxt.brokerverse.nbadmin.api;

import com.iortatechnxt.brokerverse.nbadmin.api.dto.RetentionEligibleResponse;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.RetentionRuleRequest;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.RetentionRuleResponse;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionReviewJob;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionService;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.system.service.JobRunService;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Data retention (BRNB.106): rules, eligible counts and drill-down, manual review run. */
@RestController
@RequestMapping("/api/v1/nbadmin/retention")
public class RetentionController {

  private static final String MAINTAIN = "hasAuthority('MASTER_MAINTAIN')";
  private static final int MAX_LIMIT = 500;

  private final RetentionService retention;
  private final RetentionReviewJob job;
  private final JobRunService jobRuns;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param retention retention service
   * @param job review job
   * @param jobRuns job run recorder
   * @param clock clock
   */
  public RetentionController(
      RetentionService retention, RetentionReviewJob job, JobRunService jobRuns, Clock clock) {
    this.retention = retention;
    this.job = job;
    this.jobRuns = jobRuns;
    this.clock = clock;
  }

  /**
   * Rules with their latest eligible counts.
   *
   * @return rules
   */
  @GetMapping("/rules")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public List<RetentionRuleResponse> rules() {
    return retention.rules().stream().map(RetentionRuleResponse::from).toList();
  }

  /**
   * Changes a rule.
   *
   * @param id rule
   * @param request new terms
   * @return the rule
   */
  @PutMapping("/rules/{id}")
  @PreAuthorize(MAINTAIN)
  public RetentionRuleResponse update(
      @PathVariable Long id, @Valid @RequestBody RetentionRuleRequest request) {
    retention.update(id, request.terms());
    return retention.rules().stream()
        .filter(s -> s.rule().getId().equals(id))
        .map(RetentionRuleResponse::from)
        .findFirst()
        .orElseThrow();
  }

  /**
   * Records currently eligible under a rule.
   *
   * @param id rule
   * @param limit maximum number of records
   * @return records
   */
  @GetMapping("/rules/{id}/eligible")
  @PreAuthorize("hasAuthority('MASTER_VIEW')")
  public RetentionEligibleResponse eligible(
      @PathVariable Long id, @RequestParam(defaultValue = "100") int limit) {
    return RetentionEligibleResponse.from(
        retention.eligible(id, Math.max(1, Math.min(limit, MAX_LIMIT))));
  }

  /**
   * Runs the retention review now (recorded in the job monitor).
   *
   * @return rules with the new counts
   */
  @PostMapping("/review")
  @PreAuthorize(MAINTAIN)
  public List<RetentionRuleResponse> review() {
    LocalDate today = LocalDate.now(clock);
    jobRuns.execute(job.name(), JobTrigger.MANUAL, () -> job.execute(today));
    return rules();
  }
}
