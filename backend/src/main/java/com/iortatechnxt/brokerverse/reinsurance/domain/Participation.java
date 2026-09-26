package com.iortatechnxt.brokerverse.reinsurance.domain;

import java.math.BigDecimal;

/**
 * Who takes a share of a risk: the company (retention), a treaty participant, a facultative
 * participant, or the facultative requirement not yet placed (no party).
 *
 * @param layer layer
 * @param treatyId treaty (quota share or surplus), else null
 * @param placementId facultative placement (FAC layer), else null
 * @param partyId reinsurer, null for the retention and unplaced facultative
 * @param partyCode reinsurer code, null when no party
 * @param commissionPct ceding commission %
 */
public record Participation(
    RiLayer layer,
    Long treatyId,
    Long placementId,
    Long partyId,
    String partyCode,
    BigDecimal commissionPct) {

  /**
   * The company's own retention.
   *
   * @return participation
   */
  public static Participation retention() {
    return new Participation(RiLayer.RETENTION, null, null, null, null, BigDecimal.ZERO);
  }

  /**
   * Whether a reinsurer takes this share (and premium is ceded and accounted for).
   *
   * @return true when a party is set
   */
  public boolean isCeded() {
    return partyId != null;
  }

  /**
   * Key of the treaty or placement ("T12", "F5"), used in posting references.
   *
   * @return key, null for the retention
   */
  public String contractKey() {
    if (treatyId != null) {
      return "T" + treatyId;
    }
    return placementId == null ? null : "F" + placementId;
  }
}
