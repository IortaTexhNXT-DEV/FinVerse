package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.service.RetentionService.DrillDown;
import java.time.LocalDate;
import java.util.List;

/**
 * Records currently eligible under a retention rule.
 *
 * @param ruleId rule
 * @param recordType record type
 * @param providerAvailable whether a module provides the record type yet
 * @param cutoff last activity date counted
 * @param records eligible records (limited)
 */
public record RetentionEligibleResponse(
    Long ruleId,
    String recordType,
    boolean providerAvailable,
    LocalDate cutoff,
    List<Candidate> records) {

  /**
   * Maps a drill-down.
   *
   * @param d drill-down
   * @return response
   */
  public static RetentionEligibleResponse from(DrillDown d) {
    return new RetentionEligibleResponse(
        d.rule().getId(),
        d.rule().getRecordType(),
        d.providerAvailable(),
        d.cutoff(),
        d.records().stream()
            .map(
                c ->
                    new Candidate(
                        c.reference(), c.description(), c.status(), c.lastActivity(), c.link()))
            .toList());
  }

  /**
   * An eligible record.
   *
   * @param reference reference
   * @param description description
   * @param status status
   * @param lastActivity last change date
   * @param link frontend route
   */
  public record Candidate(
      String reference, String description, String status, LocalDate lastActivity, String link) {}
}
