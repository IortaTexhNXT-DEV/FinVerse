package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionRun;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionService.RuleStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A retention rule with its latest review result.
 *
 * @param id id
 * @param recordType record type
 * @param statuses comma separated statuses
 * @param yearsOnline years online
 * @param yearsArchive years in archive
 * @param action REVIEW or ARCHIVE
 * @param active active flag
 * @param description description
 * @param providerAvailable whether a module provides the record type yet
 * @param lastRunAt time of the latest review
 * @param lastCutoff last activity date counted by the latest review
 * @param lastEligibleCount eligible records at the latest review
 */
public record RetentionRuleResponse(
    Long id,
    String recordType,
    String statuses,
    int yearsOnline,
    int yearsArchive,
    RetentionAction action,
    boolean active,
    String description,
    boolean providerAvailable,
    Instant lastRunAt,
    LocalDate lastCutoff,
    Long lastEligibleCount) {

  /**
   * Maps a rule and its status.
   *
   * @param s rule status
   * @return response
   */
  public static RetentionRuleResponse from(RuleStatus s) {
    RetentionRule r = s.rule();
    RetentionRun run = s.latestRun();
    return new RetentionRuleResponse(
        r.getId(),
        r.getRecordType(),
        r.getStatuses(),
        r.getYearsOnline(),
        r.getYearsArchive(),
        r.getAction(),
        r.isActive(),
        r.getDescription(),
        s.providerAvailable(),
        run == null ? null : run.getRunAt(),
        run == null ? null : run.getCutoffDate(),
        run == null ? null : run.getEligibleCount());
  }
}
