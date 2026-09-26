package com.iortatechnxt.brokerverse.crm.domain;

/**
 * KYC profile of a client per BDO standard (BRNB.030; the full BDO field set is parked, Q16).
 *
 * @param nationality nationality (list of values NATIONALITY), individuals
 * @param civilStatus civil status (list of values CIVIL_STATUS), individuals
 * @param occupation occupation of an individual or nature of business of a corporate client
 * @param sourceOfFunds source of funds (list of values SOURCE_OF_FUNDS)
 * @param riskRating KYC risk rating (list of values KYC_RISK_RATING); drives the review cycle
 */
public record ClientProfile(
    String nationality,
    String civilStatus,
    String occupation,
    String sourceOfFunds,
    String riskRating) {

  /** No profile data (prospects created with minimum data). */
  public static final ClientProfile EMPTY = new ClientProfile(null, null, null, null, null);
}
