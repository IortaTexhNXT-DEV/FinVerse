package com.iortatechnxt.finverse.claims.api;

import com.iortatechnxt.finverse.claims.api.dto.ClaimDecisionRequest;
import com.iortatechnxt.finverse.claims.api.dto.ClaimPartyRequest;
import com.iortatechnxt.finverse.claims.api.dto.ClaimRequest;
import com.iortatechnxt.finverse.claims.api.dto.ClaimResponse;
import com.iortatechnxt.finverse.claims.api.dto.ClaimSearchParams;
import com.iortatechnxt.finverse.claims.api.dto.MovementResponse;
import com.iortatechnxt.finverse.claims.api.dto.PolicyCoverResponse;
import com.iortatechnxt.finverse.claims.domain.ClaimStatus;
import com.iortatechnxt.finverse.claims.service.ClaimLifecycleService;
import com.iortatechnxt.finverse.claims.service.ClaimQueryService;
import com.iortatechnxt.finverse.claims.service.ClaimService;
import com.iortatechnxt.finverse.common.api.PageResponse;
import com.iortatechnxt.finverse.common.api.ReasonRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for claims: search, notification (FNOL) with policy lookup, involved parties, movement
 * history and the checker decisions close, reopen, repudiate and withdraw.
 */
@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

  static final String VIEW = "hasAuthority('CLAIM_VIEW')";
  static final String MAINTAIN = "hasAuthority('CLAIM_MAINTAIN')";
  static final String AUTHORIZE = "hasAuthority('CLAIM_AUTHORIZE')";

  private static final int MAX_PAGE_SIZE = 200;

  private final ClaimService claims;
  private final ClaimLifecycleService lifecycle;
  private final ClaimQueryService query;

  /**
   * Creates the controller.
   *
   * @param claims registration and inquiry
   * @param lifecycle checker decisions
   * @param query movement history
   */
  public ClaimController(
      ClaimService claims, ClaimLifecycleService lifecycle, ClaimQueryService query) {
    this.claims = claims;
    this.lifecycle = lifecycle;
    this.query = query;
  }

  /**
   * Searches claims.
   *
   * @param params filters
   * @param page page index
   * @param size page size
   * @return page of claims, newest first
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public PageResponse<ClaimResponse> search(
      @Valid @ModelAttribute ClaimSearchParams params,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    var pageable =
        PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE), Sort.by(Sort.Direction.DESC, "id"));
    return PageResponse.of(claims.search(params.toCriteria(), pageable), ClaimResponse::summary);
  }

  /**
   * Gets a claim with its parties.
   *
   * @param id id
   * @return claim
   */
  @GetMapping("/{id}")
  @PreAuthorize(VIEW)
  public ClaimResponse get(@PathVariable Long id) {
    return ClaimResponse.detail(claims.get(id));
  }

  /**
   * Looks up a policy for a notification and checks its cover at the loss date.
   *
   * @param companyId company
   * @param policyNo policy number
   * @param lossDate date of loss, optional
   * @return policy, risks and cover
   */
  @GetMapping("/policy-cover")
  @PreAuthorize(VIEW)
  public PolicyCoverResponse policyCover(
      @RequestParam Long companyId,
      @RequestParam String policyNo,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate lossDate) {
    return PolicyCoverResponse.from(claims.policyCover(companyId, policyNo, lossDate));
  }

  /**
   * Registers a claim (first notification of loss).
   *
   * @param request notification
   * @return claim
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public ClaimResponse register(@Valid @RequestBody ClaimRequest request) {
    return ClaimResponse.detail(claims.register(request));
  }

  /**
   * Adds an involved party.
   *
   * @param id claim
   * @param request role and party
   * @return claim
   */
  @PostMapping("/{id}/parties")
  @PreAuthorize(MAINTAIN)
  public ClaimResponse addParty(
      @PathVariable Long id, @Valid @RequestBody ClaimPartyRequest request) {
    return ClaimResponse.detail(claims.addParty(id, request));
  }

  /**
   * Movement history of a claim.
   *
   * @param id claim
   * @return movements in date order
   */
  @GetMapping("/{id}/movements")
  @PreAuthorize(VIEW)
  public List<MovementResponse> movements(@PathVariable Long id) {
    return query.movements(id).stream().map(MovementResponse::from).toList();
  }

  /**
   * Closes a claim, releasing its outstanding reserve.
   *
   * @param id claim
   * @param request reason and date
   * @return claim
   */
  @PostMapping("/{id}/close")
  @PreAuthorize(AUTHORIZE)
  public ClaimResponse close(
      @PathVariable Long id, @Valid @RequestBody ClaimDecisionRequest request) {
    return ClaimResponse.detail(lifecycle.close(id, request.reason(), request.accountingDate()));
  }

  /**
   * Reopens a closed claim.
   *
   * @param id claim
   * @param request reason
   * @return claim
   */
  @PostMapping("/{id}/reopen")
  @PreAuthorize(AUTHORIZE)
  public ClaimResponse reopen(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
    return ClaimResponse.detail(lifecycle.reopen(id, request.reason()));
  }

  /**
   * Repudiates a claim.
   *
   * @param id claim
   * @param request reason and date
   * @return claim
   */
  @PostMapping("/{id}/repudiate")
  @PreAuthorize(AUTHORIZE)
  public ClaimResponse repudiate(
      @PathVariable Long id, @Valid @RequestBody ClaimDecisionRequest request) {
    return ClaimResponse.detail(
        lifecycle.decline(id, ClaimStatus.REJECTED, request.reason(), request.accountingDate()));
  }

  /**
   * Records the withdrawal of a claim by the claimant.
   *
   * @param id claim
   * @param request reason and date
   * @return claim
   */
  @PostMapping("/{id}/withdraw")
  @PreAuthorize(AUTHORIZE)
  public ClaimResponse withdraw(
      @PathVariable Long id, @Valid @RequestBody ClaimDecisionRequest request) {
    return ClaimResponse.detail(
        lifecycle.decline(id, ClaimStatus.WITHDRAWN, request.reason(), request.accountingDate()));
  }
}
