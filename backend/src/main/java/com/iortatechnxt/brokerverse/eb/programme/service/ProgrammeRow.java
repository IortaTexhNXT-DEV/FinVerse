package com.iortatechnxt.brokerverse.eb.programme.service;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A row of the Programmes work list.
 *
 * @param id programme
 * @param programmeNo programme number
 * @param clientId client
 * @param clientCode client code
 * @param clientName client name
 * @param name programme name
 * @param teamCode team
 * @param funding employer or voluntary
 * @param accountOfficer AO user name
 * @param status programme status
 * @param renewalEligible flagged for renewal
 * @param lines benefit line codes, comma separated
 * @param nextExpiry next expiry of an active line, null when none
 * @param cycle latest cycle, null when none
 */
public record ProgrammeRow(
    Long id,
    String programmeNo,
    Long clientId,
    String clientCode,
    String clientName,
    String name,
    String teamCode,
    String funding,
    String accountOfficer,
    String status,
    boolean renewalEligible,
    String lines,
    LocalDate nextExpiry,
    CycleRef cycle) {

  /**
   * The latest cycle of a programme.
   *
   * @param id cycle
   * @param cycleNo cycle number
   * @param businessType NEW_BUSINESS or RENEWAL
   * @param stage stage
   * @param policyYear policy year
   * @param raSentAt renewal advice sent, null when not sent
   */
  public record CycleRef(
      Long id,
      String cycleNo,
      String businessType,
      String stage,
      int policyYear,
      Instant raSentAt) {}
}
