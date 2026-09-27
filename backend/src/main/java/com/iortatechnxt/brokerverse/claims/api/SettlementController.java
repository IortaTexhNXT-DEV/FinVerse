package com.iortatechnxt.brokerverse.claims.api;

import com.iortatechnxt.brokerverse.claims.api.dto.DecisionRequest;
import com.iortatechnxt.brokerverse.claims.api.dto.SettlementRequest;
import com.iortatechnxt.brokerverse.claims.api.dto.SettlementResponse;
import com.iortatechnxt.brokerverse.claims.service.SettlementService;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import jakarta.validation.Valid;
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

/** REST API for claim settlements: history, maker entry, checker approval / rejection. */
@RestController
@RequestMapping("/api/v1/claims")
public class SettlementController {

  private final SettlementService settlements;

  /**
   * Creates the controller.
   *
   * @param settlements settlement service
   */
  public SettlementController(SettlementService settlements) {
    this.settlements = settlements;
  }

  /**
   * Settlements of a claim.
   *
   * @param id claim
   * @return settlements
   */
  @GetMapping("/{id}/settlements")
  @PreAuthorize(ClaimController.VIEW)
  public List<SettlementResponse> list(@PathVariable Long id) {
    return settlements.forClaim(id).stream().map(SettlementResponse::from).toList();
  }

  /**
   * Enters a settlement (pending approval).
   *
   * @param id claim
   * @param request payee, cost type, type and amounts
   * @return pending settlement
   */
  @PostMapping("/{id}/settlements")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(ClaimController.MAINTAIN)
  public SettlementResponse create(
      @PathVariable Long id, @Valid @RequestBody SettlementRequest request) {
    return SettlementResponse.from(settlements.create(id, request.toCommand()));
  }

  /**
   * Approves a settlement: posts it and records the payee's payable.
   *
   * @param settlementId settlement
   * @param request accounting date
   * @return approved settlement
   */
  @PostMapping("/settlements/{settlementId}/approve")
  @PreAuthorize(ClaimController.AUTHORIZE)
  public SettlementResponse approve(
      @PathVariable Long settlementId, @RequestBody(required = false) DecisionRequest request) {
    return SettlementResponse.from(
        settlements.approve(settlementId, DecisionRequest.dateOf(request)));
  }

  /**
   * Rejects a settlement.
   *
   * @param settlementId settlement
   * @param request reason
   * @return rejected settlement
   */
  @PostMapping("/settlements/{settlementId}/reject")
  @PreAuthorize(ClaimController.AUTHORIZE)
  public SettlementResponse reject(
      @PathVariable Long settlementId, @Valid @RequestBody ReasonRequest request) {
    return SettlementResponse.from(settlements.reject(settlementId, request.reason()));
  }
}
