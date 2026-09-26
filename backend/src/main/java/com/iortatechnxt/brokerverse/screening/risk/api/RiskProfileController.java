package com.iortatechnxt.brokerverse.screening.risk.api;

import com.iortatechnxt.brokerverse.screening.risk.api.dto.ManualRiskRequest;
import com.iortatechnxt.brokerverse.screening.risk.api.dto.RiskProfileRow;
import com.iortatechnxt.brokerverse.screening.risk.service.RiskOverrideService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The client risk profile set by screening (SNSRP-302, 304; FR-SS-033, 035): the history of every
 * change (rule or manual) and "Update Risk Tag" with justification and evidence.
 */
@RestController
@RequestMapping("/api/v1/screening/clients/{clientId}/risk-profile")
public class RiskProfileController {

  private final RiskOverrideService overrides;

  /**
   * Creates the controller.
   *
   * @param overrides manual changes and history
   */
  public RiskProfileController(RiskOverrideService overrides) {
    this.overrides = overrides;
  }

  /**
   * The risk-profile history of a client, newest first.
   *
   * @param clientId client
   * @return history
   */
  @GetMapping
  @PreAuthorize("hasAuthority('SCR_VIEW')")
  public List<RiskProfileRow> history(@PathVariable Long clientId) {
    return overrides.history(clientId).stream().map(RiskProfileRow::from).toList();
  }

  /**
   * Updates the rating and tags by hand (FR-SS-035).
   *
   * @param clientId client
   * @param request rating, tags, justification and evidence
   * @return the history row
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('SCR_RISK_TAG')")
  public RiskProfileRow update(
      @PathVariable Long clientId, @RequestBody ManualRiskRequest request) {
    return RiskProfileRow.from(overrides.override(clientId, request.toChange()));
  }
}
