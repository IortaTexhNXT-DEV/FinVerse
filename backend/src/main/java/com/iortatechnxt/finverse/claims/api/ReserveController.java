package com.iortatechnxt.finverse.claims.api;

import com.iortatechnxt.finverse.claims.api.dto.DecisionRequest;
import com.iortatechnxt.finverse.claims.api.dto.ReserveChangeResponse;
import com.iortatechnxt.finverse.claims.api.dto.ReserveRequest;
import com.iortatechnxt.finverse.claims.service.ReserveService;
import com.iortatechnxt.finverse.common.api.ReasonRequest;
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

/** REST API for claim reserve changes: history, maker request, checker approval / rejection. */
@RestController
@RequestMapping("/api/v1/claims")
public class ReserveController {

  private final ReserveService reserves;

  /**
   * Creates the controller.
   *
   * @param reserves reserve service
   */
  public ReserveController(ReserveService reserves) {
    this.reserves = reserves;
  }

  /**
   * Reserve changes of a claim.
   *
   * @param id claim
   * @return changes in sequence
   */
  @GetMapping("/{id}/reserves")
  @PreAuthorize(ClaimController.VIEW)
  public List<ReserveChangeResponse> list(@PathVariable Long id) {
    return reserves.forClaim(id).stream().map(ReserveChangeResponse::from).toList();
  }

  /**
   * Requests a new estimate (pending approval).
   *
   * @param id claim
   * @param request side, cost type, estimate and reason
   * @return pending change
   */
  @PostMapping("/{id}/reserves")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(ClaimController.MAINTAIN)
  public ReserveChangeResponse request(
      @PathVariable Long id, @Valid @RequestBody ReserveRequest request) {
    return ReserveChangeResponse.from(reserves.request(id, request.toLine(), request.reason()));
  }

  /**
   * Approves a reserve change and posts it.
   *
   * @param changeId change
   * @param request accounting date
   * @return approved change
   */
  @PostMapping("/reserves/{changeId}/approve")
  @PreAuthorize(ClaimController.AUTHORIZE)
  public ReserveChangeResponse approve(
      @PathVariable Long changeId, @RequestBody(required = false) DecisionRequest request) {
    return ReserveChangeResponse.from(reserves.approve(changeId, DecisionRequest.dateOf(request)));
  }

  /**
   * Rejects a reserve change.
   *
   * @param changeId change
   * @param request reason
   * @return rejected change
   */
  @PostMapping("/reserves/{changeId}/reject")
  @PreAuthorize(ClaimController.AUTHORIZE)
  public ReserveChangeResponse reject(
      @PathVariable Long changeId, @Valid @RequestBody ReasonRequest request) {
    return ReserveChangeResponse.from(reserves.reject(changeId, request.reason()));
  }
}
