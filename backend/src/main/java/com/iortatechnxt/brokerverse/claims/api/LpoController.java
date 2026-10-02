package com.iortatechnxt.brokerverse.claims.api;

import com.iortatechnxt.brokerverse.claims.api.dto.LpoRequest;
import com.iortatechnxt.brokerverse.claims.api.dto.LpoResponse;
import com.iortatechnxt.brokerverse.claims.service.LpoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST API for local purchase orders (motor garage repairs). */
@RestController
@RequestMapping("/api/v1/claims")
public class LpoController {

  private final LpoService lpos;

  /**
   * Creates the controller.
   *
   * @param lpos LPO service
   */
  public LpoController(LpoService lpos) {
    this.lpos = lpos;
  }

  /**
   * LPO register of a company.
   *
   * @param companyId company
   * @return LPOs, newest first
   */
  @GetMapping("/lpos")
  @PreAuthorize(ClaimController.VIEW)
  public List<LpoResponse> register(@RequestParam Long companyId) {
    return lpos.forCompany(companyId).stream().map(LpoResponse::from).toList();
  }

  /**
   * LPOs of a claim.
   *
   * @param id claim
   * @return LPOs
   */
  @GetMapping("/{id}/lpos")
  @PreAuthorize(ClaimController.VIEW)
  public List<LpoResponse> list(@PathVariable Long id) {
    return lpos.forClaim(id).stream().map(LpoResponse::from).toList();
  }

  /**
   * Issues an LPO to a garage.
   *
   * @param id motor claim
   * @param request garage, cover, amounts
   * @return LPO
   */
  @PostMapping("/{id}/lpos")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(ClaimController.MAINTAIN)
  public LpoResponse issue(@PathVariable Long id, @Valid @RequestBody LpoRequest request) {
    return LpoResponse.from(lpos.issue(id, request.toCommand()));
  }

  /**
   * Cancels an LPO.
   *
   * @param lpoId LPO
   * @param request reason
   * @return cancelled LPO
   */
  @PostMapping("/lpos/{lpoId}/cancel")
  @PreAuthorize(ClaimController.MAINTAIN)
  public LpoResponse cancel(@PathVariable Long lpoId, @Valid @RequestBody ReasonRequest request) {
    return LpoResponse.from(lpos.cancel(lpoId, request.reason()));
  }
}
