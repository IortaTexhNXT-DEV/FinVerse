package com.iortatechnxt.brokerverse.crm.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Onboarding action with an optional reason and comment (deactivation needs the reason).
 *
 * @param reasonCode reason code (list of values CLIENT_DEACTIVATION_REASON for deactivation)
 * @param comment comment shown in the status history
 */
public record ClientActionRequest(
    @Size(max = 40) String reasonCode, @Size(max = 500) String comment) {

  /**
   * Comment, null when blank.
   *
   * @return comment
   */
  public String cleanComment() {
    return comment == null || comment.isBlank() ? null : comment.trim();
  }
}
