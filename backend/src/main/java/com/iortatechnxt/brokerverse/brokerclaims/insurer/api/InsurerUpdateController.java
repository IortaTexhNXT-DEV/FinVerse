package com.iortatechnxt.brokerverse.brokerclaims.insurer.api;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.BrokerClaimQueryService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.AdviceDraftResponse;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.AdviceRecipient;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.AdviceRequest;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.AdviceSentResponse;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.InsurerUpdateResponse;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.api.dto.InsurerDtos.UpdateRequest;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerUpdateService;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.LossAdviceService;
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

/**
 * Insurer communication of a claim (BRCLM.041; FR-CM-022/024): the insurer updates (timeline,
 * insert-only) and the loss advice e-mailed to the insurers with its drafts for review.
 */
@RestController
@RequestMapping("/api/v1/broker-claims/{claimId}")
public class InsurerUpdateController {

  private static final String RECORD = "hasAuthority('BCL_RECORD')";

  private final BrokerClaimQueryService claims;
  private final InsurerUpdateService updates;
  private final LossAdviceService advice;

  /**
   * Creates the controller.
   *
   * @param claims claims
   * @param updates insurer updates
   * @param advice loss advice
   */
  public InsurerUpdateController(
      BrokerClaimQueryService claims, InsurerUpdateService updates, LossAdviceService advice) {
    this.claims = claims;
    this.updates = updates;
    this.advice = advice;
  }

  /**
   * The insurer updates of a claim.
   *
   * @param claimId claim
   * @param companyId company
   * @return updates, newest first
   */
  @GetMapping("/updates")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public List<InsurerUpdateResponse> updates(
      @PathVariable Long claimId, @RequestParam Long companyId) {
    Claim claim = claims.require(companyId, claimId);
    return updates.ofClaim(claim.getId()).stream().map(InsurerUpdateResponse::from).toList();
  }

  /**
   * Records an insurer update (also on a closed claim).
   *
   * @param claimId claim
   * @param companyId company
   * @param request update
   * @return the update
   */
  @PostMapping("/updates")
  @PreAuthorize(RECORD)
  @ResponseStatus(HttpStatus.CREATED)
  public InsurerUpdateResponse record(
      @PathVariable Long claimId,
      @RequestParam Long companyId,
      @Valid @RequestBody UpdateRequest request) {
    Claim claim = claims.require(companyId, claimId);
    return InsurerUpdateResponse.from(updates.record(claim, request.toUpdate()));
  }

  /**
   * The loss advice of each insurer, for review before sending.
   *
   * @param claimId claim
   * @param companyId company
   * @return drafts
   */
  @GetMapping("/loss-advice")
  @PreAuthorize(RECORD)
  public List<AdviceDraftResponse> drafts(
      @PathVariable Long claimId, @RequestParam Long companyId) {
    return advice.drafts(claims.require(companyId, claimId)).stream()
        .map(AdviceDraftResponse::from)
        .toList();
  }

  /**
   * E-mails the loss advice to the chosen insurers.
   *
   * @param claimId claim
   * @param companyId company
   * @param request insurers and recipients
   * @return what was sent
   */
  @PostMapping("/loss-advice")
  @PreAuthorize(RECORD)
  public List<AdviceSentResponse> send(
      @PathVariable Long claimId,
      @RequestParam Long companyId,
      @Valid @RequestBody AdviceRequest request) {
    Claim claim = claims.require(companyId, claimId);
    List<AdviceRecipient> recipients =
        request.recipients() == null ? List.of() : request.recipients();
    return advice
        .send(claim, recipients.stream().map(AdviceRecipient::toRecipient).toList())
        .stream()
        .map(AdviceSentResponse::from)
        .toList();
  }
}
