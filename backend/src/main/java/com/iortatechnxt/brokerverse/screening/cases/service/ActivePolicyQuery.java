package com.iortatechnxt.brokerverse.screening.cases.service;

import java.time.LocalDate;

/**
 * Port: whether a client has an active policy (SNSRP-303; FR-SS-034 R3). Only clients with an
 * active policy need a KYC review or EDD; for the others a MONITOR case opens and the UCC and the
 * investigators are notified. The definition is BDOI's (SQ10); the default adapter reads the
 * client's accounts in POLICY_ISSUED or BOOKED.
 */
public interface ActivePolicyQuery {

  /**
   * Whether the client has an active policy on a date.
   *
   * @param clientId the client
   * @param asOf the date
   * @return true when active
   */
  boolean hasActivePolicy(Long clientId, LocalDate asOf);
}
