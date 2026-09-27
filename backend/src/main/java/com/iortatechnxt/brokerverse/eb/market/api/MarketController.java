package com.iortatechnxt.brokerverse.eb.market.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.eb.document.api.EbUploads;
import com.iortatechnxt.brokerverse.eb.domain.EbInsurerRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbProposal;
import com.iortatechnxt.brokerverse.eb.market.api.dto.MarketDtos.CloseRequest;
import com.iortatechnxt.brokerverse.eb.market.api.dto.MarketDtos.InsurersRequest;
import com.iortatechnxt.brokerverse.eb.market.api.dto.MarketDtos.ProposalRequest;
import com.iortatechnxt.brokerverse.eb.market.api.dto.MarketDtos.ProposalResponse;
import com.iortatechnxt.brokerverse.eb.market.api.dto.MarketDtos.ReasonRequest;
import com.iortatechnxt.brokerverse.eb.market.api.dto.MarketDtos.RequestResponse;
import com.iortatechnxt.brokerverse.eb.market.api.dto.MarketDtos.RevisionRequestBody;
import com.iortatechnxt.brokerverse.eb.market.api.dto.MarketDtos.RevisionResponse;
import com.iortatechnxt.brokerverse.eb.market.api.dto.MarketDtos.TorItemsRequest;
import com.iortatechnxt.brokerverse.eb.market.api.dto.MarketDtos.TorResponse;
import com.iortatechnxt.brokerverse.eb.market.service.InsurerRequestService;
import com.iortatechnxt.brokerverse.eb.market.service.ProposalInput;
import com.iortatechnxt.brokerverse.eb.market.service.ProposalService;
import com.iortatechnxt.brokerverse.eb.market.service.RevisionService;
import com.iortatechnxt.brokerverse.eb.market.service.TorService;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * The market steps of a cycle (FR-EB-035, 040, 044, 045): the TOR, the insurer requests, the
 * proposals and the revision requests.
 */
@RestController
@RequestMapping("/api/v1/eb")
@Transactional
public class MarketController {

  private static final String VIEW = "hasAuthority('EB_VIEW')";
  private static final String MARKET = "hasAuthority('EB_MARKET')";

  private final TorService tors;
  private final InsurerRequestService requests;
  private final ProposalService proposals;
  private final RevisionService revisions;
  private final EbParties parties;
  private final ObjectMapper json;

  /**
   * Creates the controller.
   *
   * @param tors TOR versions
   * @param requests insurer requests
   * @param proposals proposals
   * @param revisions revision requests
   * @param parties insurer names
   * @param json JSON field of the proposal form
   */
  public MarketController(
      TorService tors,
      InsurerRequestService requests,
      ProposalService proposals,
      RevisionService revisions,
      EbParties parties,
      ObjectMapper json) {
    this.tors = tors;
    this.requests = requests;
    this.proposals = proposals;
    this.revisions = revisions;
    this.parties = parties;
    this.json = json;
  }

  /**
   * The TOR versions of a cycle.
   *
   * @param cycleId cycle
   * @param companyId company
   * @return versions, latest first
   */
  @GetMapping("/cycles/{cycleId}/tor")
  @PreAuthorize(VIEW)
  public List<TorResponse> tor(@PathVariable Long cycleId, @RequestParam Long companyId) {
    return tors.ofCycle(companyId, cycleId).stream().map(TorResponse::from).toList();
  }

  /**
   * Replaces the items of the draft TOR.
   *
   * @param cycleId cycle
   * @param companyId company
   * @param request items
   * @return the draft
   */
  @PutMapping("/cycles/{cycleId}/tor")
  @PreAuthorize(MARKET)
  public TorResponse saveTor(
      @PathVariable Long cycleId, @RequestParam Long companyId, @RequestBody TorItemsRequest request) {
    return TorResponse.from(tors.saveItems(companyId, cycleId, request.items()));
  }

  /**
   * Releases the draft TOR.
   *
   * @param cycleId cycle
   * @param companyId company
   * @return the released version
   */
  @PostMapping("/cycles/{cycleId}/tor/release")
  @PreAuthorize(MARKET)
  public TorResponse releaseTor(@PathVariable Long cycleId, @RequestParam Long companyId) {
    return TorResponse.from(tors.release(companyId, cycleId));
  }

  /**
   * The insurer requests of a cycle.
   *
   * @param cycleId cycle
   * @param companyId company
   * @return requests
   */
  @GetMapping("/cycles/{cycleId}/requests")
  @PreAuthorize(VIEW)
  public List<RequestResponse> requests(@PathVariable Long cycleId, @RequestParam Long companyId) {
    return requests.ofCycle(companyId, cycleId).stream().map(r -> map(companyId, r)).toList();
  }

  /**
   * Sends the released TOR to insurers.
   *
   * @param cycleId cycle
   * @param companyId company
   * @param request insurers
   * @return the requests sent
   */
  @PostMapping("/cycles/{cycleId}/requests")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MARKET)
  public List<RequestResponse> send(
      @PathVariable Long cycleId, @RequestParam Long companyId, @RequestBody InsurersRequest request) {
    return requests.send(companyId, cycleId, request.insurerCodes()).stream()
        .map(r -> map(companyId, r))
        .toList();
  }

  /**
   * Closes a request without a proposal.
   *
   * @param requestId request
   * @param companyId company
   * @param request declined flag and reason
   * @return the request
   */
  @PostMapping("/requests/{requestId}/close")
  @PreAuthorize(MARKET)
  public RequestResponse close(
      @PathVariable Long requestId, @RequestParam Long companyId, @RequestBody CloseRequest request) {
    return map(companyId, requests.close(companyId, requestId, request.declined(), request.reason()));
  }

  /**
   * The proposals of a cycle.
   *
   * @param cycleId cycle
   * @param companyId company
   * @return proposals
   */
  @GetMapping("/cycles/{cycleId}/proposals")
  @PreAuthorize(VIEW)
  public List<ProposalResponse> proposals(@PathVariable Long cycleId, @RequestParam Long companyId) {
    return proposals.ofCycle(companyId, cycleId).stream().map(p -> map(companyId, p)).toList();
  }

  /**
   * Records a proposal entered from the insurer's e-mail.
   *
   * @param cycleId cycle
   * @param companyId company
   * @param proposal the proposal as JSON
   * @param file the insurer's document
   * @return the proposal
   */
  @PostMapping(value = "/cycles/{cycleId}/proposals", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MARKET)
  public ProposalResponse record(
      @PathVariable Long cycleId,
      @RequestParam Long companyId,
      @RequestParam String proposal,
      @RequestParam(required = false) MultipartFile file) {
    ProposalRequest body = EbUploads.read(json, proposal, ProposalRequest.class);
    return map(
        companyId,
        proposals.record(
            companyId,
            cycleId,
            new ProposalInput(
                body.insurerCode(),
                body.receivedOn(),
                body.validUntil(),
                body.currency(),
                body.terms(),
                body.exclusions(),
                body.lines(),
                body.items(),
                body.factors(),
                EbUploads.file(file))));
  }

  /**
   * Validates a proposal.
   *
   * @param proposalId proposal
   * @param companyId company
   * @return the proposal
   */
  @PostMapping("/proposals/{proposalId}/validate")
  @PreAuthorize(MARKET)
  public ProposalResponse validate(@PathVariable Long proposalId, @RequestParam Long companyId) {
    return map(companyId, proposals.validate(companyId, proposalId));
  }

  /**
   * Rejects a proposal.
   *
   * @param proposalId proposal
   * @param companyId company
   * @param request reason
   * @return the proposal
   */
  @PostMapping("/proposals/{proposalId}/reject")
  @PreAuthorize(MARKET)
  public ProposalResponse reject(
      @PathVariable Long proposalId, @RequestParam Long companyId, @RequestBody ReasonRequest request) {
    return map(companyId, proposals.reject(companyId, proposalId, request.reason()));
  }

  /**
   * The revision requests of a cycle.
   *
   * @param cycleId cycle
   * @param companyId company
   * @return revisions
   */
  @GetMapping("/cycles/{cycleId}/revisions")
  @PreAuthorize(VIEW)
  public List<RevisionResponse> revisions(@PathVariable Long cycleId, @RequestParam Long companyId) {
    return revisions.ofCycle(companyId, cycleId).stream().map(RevisionResponse::from).toList();
  }

  /**
   * Relays the client's changes to insurers.
   *
   * @param cycleId cycle
   * @param companyId company
   * @param request changes and insurers
   * @return the revision
   */
  @PostMapping("/cycles/{cycleId}/revisions")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MARKET)
  public RevisionResponse revise(
      @PathVariable Long cycleId, @RequestParam Long companyId, @RequestBody RevisionRequestBody request) {
    return RevisionResponse.from(
        revisions.request(
            companyId,
            cycleId,
            new RevisionService.RevisionInput(
                request.description(), request.changes(), request.insurerCodes())));
  }

  private RequestResponse map(Long companyId, EbInsurerRequest r) {
    return RequestResponse.from(r, parties.insurerName(companyId, r.getInsurerCode()));
  }

  private ProposalResponse map(Long companyId, EbProposal p) {
    return ProposalResponse.from(p, parties.insurerName(companyId, p.getInsurerCode()));
  }
}
