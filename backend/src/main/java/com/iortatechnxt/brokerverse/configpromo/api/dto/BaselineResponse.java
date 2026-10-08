package com.iortatechnxt.brokerverse.configpromo.api.dto;

import com.iortatechnxt.brokerverse.configpromo.domain.ConfigBaseline;
import java.time.Instant;

/**
 * A configuration baseline.
 *
 * @param id id
 * @param name name
 * @param packageId package
 * @param packageNo package number
 * @param environment environment
 * @param remarks remarks
 * @param active whether in force
 * @param createdBy user who marked it
 * @param createdAt time
 */
public record BaselineResponse(
    Long id,
    String name,
    Long packageId,
    String packageNo,
    String environment,
    String remarks,
    boolean active,
    String createdBy,
    Instant createdAt) {

  /**
   * Maps a baseline.
   *
   * @param b baseline
   * @param packageNo number of its package
   * @return response
   */
  public static BaselineResponse from(ConfigBaseline b, String packageNo) {
    return new BaselineResponse(
        b.getId(),
        b.getName(),
        b.getPackageId(),
        packageNo,
        b.getEnvironment(),
        b.getRemarks(),
        b.isActive(),
        b.getCreatedBy(),
        b.getCreatedAt());
  }
}
