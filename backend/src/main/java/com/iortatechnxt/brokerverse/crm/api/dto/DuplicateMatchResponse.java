package com.iortatechnxt.brokerverse.crm.api.dto;

import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.service.DuplicateMatch;
import java.util.List;

/**
 * An existing client matching entered data (BRNB.032).
 *
 * @param clientId client
 * @param code client or prospect code
 * @param displayName name
 * @param status status
 * @param keys matched keys
 * @param hard whether the match blocks creation
 */
public record DuplicateMatchResponse(
    Long clientId,
    String code,
    String displayName,
    ClientStatus status,
    List<String> keys,
    boolean hard) {

  /**
   * Maps a match.
   *
   * @param m match
   * @return response
   */
  public static DuplicateMatchResponse from(DuplicateMatch m) {
    return new DuplicateMatchResponse(
        m.clientId(), m.code(), m.displayName(), m.status(), m.keys(), m.hard());
  }
}
