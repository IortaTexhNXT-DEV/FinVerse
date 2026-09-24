package com.iortatechnxt.brokerverse.placement.api.dto;

import com.iortatechnxt.brokerverse.placement.domain.HoldCover;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A hold cover (BRNB.072/103).
 *
 * @param id id
 * @param arn account
 * @param insurerCode insurer
 * @param status status
 * @param startDate first day
 * @param expiryDate last day
 * @param insurerRef insurer reference
 * @param confirmedOn confirmation date
 * @param alertedOn expiry alert date
 * @param createdAt requested at
 * @param createdBy requested by
 */
public record HoldCoverResponse(
    Long id,
    String arn,
    String insurerCode,
    String status,
    LocalDate startDate,
    LocalDate expiryDate,
    String insurerRef,
    LocalDate confirmedOn,
    LocalDate alertedOn,
    Instant createdAt,
    String createdBy) {

  /**
   * Maps a hold cover.
   *
   * @param h hold cover
   * @return response
   */
  public static HoldCoverResponse from(HoldCover h) {
    return new HoldCoverResponse(
        h.getId(),
        h.getArn(),
        h.getInsurerCode(),
        h.getStatus().name(),
        h.getStartDate(),
        h.getExpiryDate(),
        h.getInsurerRef(),
        h.getConfirmedOn(),
        h.getAlertedOn(),
        h.getCreatedAt(),
        h.getCreatedBy());
  }
}
