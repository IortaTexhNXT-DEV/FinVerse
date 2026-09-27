package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService.Decision;

/**
 * Result of an approval or rejection.
 *
 * @param request the decided request
 * @param temporaryPassword temporary password of a created user: shown once, never stored
 */
public record AccessDecisionResponse(AccessRequestResponse request, String temporaryPassword) {

  /**
   * Maps a decision.
   *
   * @param d decision
   * @return response
   */
  public static AccessDecisionResponse from(Decision d) {
    return new AccessDecisionResponse(
        AccessRequestResponse.from(d.request()), d.temporaryPassword());
  }

  @Override
  public String toString() {
    return "AccessDecisionResponse[" + request.requestNo() + ", ***]";
  }
}
