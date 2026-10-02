package com.iortatechnxt.brokerverse.placement.api.dto;

import com.iortatechnxt.brokerverse.placement.service.PlacementSlipService.Readiness;
import com.iortatechnxt.brokerverse.placement.service.SlipPrerequisites.Unmet;
import java.util.List;

/**
 * Whether an account may be put on a placement slip, and why not (BRNB.069).
 *
 * @param arn Account Reference Number
 * @param clientName client
 * @param insurerCode insurer
 * @param insurerBranch insurer branch
 * @param status account status
 * @param ready all prerequisites met
 * @param unmet unmet prerequisites
 */
public record ReadinessResponse(
    String arn,
    String clientName,
    String insurerCode,
    String insurerBranch,
    String status,
    boolean ready,
    List<Unmet> unmet) {

  /**
   * Maps a readiness.
   *
   * @param r readiness
   * @return response
   */
  public static ReadinessResponse from(Readiness r) {
    return new ReadinessResponse(
        r.account().getArn(),
        r.account().getClientName(),
        r.account().getInsurerCode(),
        r.account().getInsurerBranch(),
        r.account().getStatus().name(),
        r.unmet().isEmpty(),
        r.unmet());
  }
}
