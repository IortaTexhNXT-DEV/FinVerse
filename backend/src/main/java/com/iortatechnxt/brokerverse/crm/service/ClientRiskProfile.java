package com.iortatechnxt.brokerverse.crm.service;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * A client's risk profile after a change by {@link ClientRiskService} (SNSRP-302, 304).
 *
 * @param clientId client
 * @param clientCode client or prospect code
 * @param previousRating risk rating before the change
 * @param riskRating risk rating after the change
 * @param activeTags active tags after the change
 * @param tagsAdded tags added by this change
 * @param tagsRemoved tags ended by this change
 * @param kycReviewDue next periodic KYC review date
 */
public record ClientRiskProfile(
    Long clientId,
    String clientCode,
    String previousRating,
    String riskRating,
    Set<String> activeTags,
    Set<String> tagsAdded,
    Set<String> tagsRemoved,
    LocalDate kycReviewDue) {

  /** Defensive copies. */
  public ClientRiskProfile {
    activeTags = Set.copyOf(activeTags);
    tagsAdded = Set.copyOf(tagsAdded);
    tagsRemoved = Set.copyOf(tagsRemoved);
  }

  /**
   * Whether the change altered anything.
   *
   * @return true when the rating or a tag changed
   */
  public boolean changed() {
    return !Objects.equals(previousRating, riskRating)
        || !tagsAdded.isEmpty()
        || !tagsRemoved.isEmpty();
  }
}
