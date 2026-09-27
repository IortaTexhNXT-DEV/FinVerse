package com.iortatechnxt.brokerverse.claims.api;

import com.iortatechnxt.brokerverse.claims.api.dto.DecisionRequest;
import com.iortatechnxt.brokerverse.claims.api.dto.RecoveryRequest;
import com.iortatechnxt.brokerverse.claims.api.dto.RecoveryResponse;
import com.iortatechnxt.brokerverse.claims.service.RecoveryService;
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

/** REST API for salvage / subrogation recoveries: history, maker entry, checker decisions. */
@RestController
@RequestMapping("/api/v1/claims")
public class RecoveryController {

  private final RecoveryService recoveries;

  /**
   * Creates the controller.
   *
   * @param recoveries recovery service
   */
  public RecoveryController(RecoveryService recoveries) {
    this.recoveries = recoveries;
  }

  /**
   * Recoveries of a claim.
   *
   * @param id claim
   * @return recoveries
   */
  @GetMapping("/{id}/recoveries")
  @PreAuthorize(ClaimController.VIEW)
  public List<RecoveryResponse> list(@PathVariable Long id) {
    return recoveries.forClaim(id).stream().map(RecoveryResponse::from).toList();
  }

  /**
   * Records a recovery (pending approval).
   *
   * @param id claim
   * @param request type, payer, bank account and amount
   * @return pending recovery
   */
  @PostMapping("/{id}/recoveries")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(ClaimController.MAINTAIN)
  public RecoveryResponse create(
      @PathVariable Long id, @Valid @RequestBody RecoveryRequest request) {
    return RecoveryResponse.from(recoveries.create(id, request.toCommand()));
  }

  /**
   * Approves a recovery and posts it.
   *
   * @param recoveryId recovery
   * @param request accounting date
   * @return approved recovery
   */
  @PostMapping("/recoveries/{recoveryId}/approve")
  @PreAuthorize(ClaimController.AUTHORIZE)
  public RecoveryResponse approve(
      @PathVariable Long recoveryId, @RequestBody(required = false) DecisionRequest request) {
    return RecoveryResponse.from(recoveries.approve(recoveryId, DecisionRequest.dateOf(request)));
  }

  /**
   * Rejects a recovery.
   *
   * @param recoveryId recovery
   * @param request reason
   * @return rejected recovery
   */
  @PostMapping("/recoveries/{recoveryId}/reject")
  @PreAuthorize(ClaimController.AUTHORIZE)
  public RecoveryResponse reject(
      @PathVariable Long recoveryId, @Valid @RequestBody ReasonRequest request) {
    return RecoveryResponse.from(recoveries.reject(recoveryId, request.reason()));
  }
}
