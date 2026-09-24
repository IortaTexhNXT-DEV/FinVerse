package com.iortatechnxt.brokerverse.placement.api.dto;

import com.iortatechnxt.brokerverse.placement.domain.InsurerReturn;
import java.time.Instant;

/**
 * A placement returned by the insurer and its resolution (BRNB.034).
 *
 * @param id id
 * @param arn account
 * @param insurerCode insurer
 * @param slipNo slip returned
 * @param reasonCode reason
 * @param remarks insurer remarks
 * @param resolution how it was resolved (resubmit, return, cancel_placement)
 * @param resolvedAt resolved at
 * @param createdAt recorded at
 * @param createdBy recorded by
 */
public record InsurerReturnResponse(
    Long id,
    String arn,
    String insurerCode,
    String slipNo,
    String reasonCode,
    String remarks,
    String resolution,
    Instant resolvedAt,
    Instant createdAt,
    String createdBy) {

  /**
   * Maps a return.
   *
   * @param r return
   * @return response
   */
  public static InsurerReturnResponse from(InsurerReturn r) {
    return new InsurerReturnResponse(
        r.getId(),
        r.getArn(),
        r.getInsurerCode(),
        r.getSlipNo(),
        r.getReasonCode(),
        r.getRemarks(),
        r.getResolution(),
        r.getResolvedAt(),
        r.getCreatedAt(),
        r.getCreatedBy());
  }
}
