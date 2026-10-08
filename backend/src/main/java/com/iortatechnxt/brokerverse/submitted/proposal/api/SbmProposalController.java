package com.iortatechnxt.brokerverse.submitted.proposal.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNominatedRate;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposal;
import com.iortatechnxt.brokerverse.submitted.domain.SbmProposalBatch;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.MasterlistController;
import com.iortatechnxt.brokerverse.submitted.proposal.service.NominatedRateService;
import com.iortatechnxt.brokerverse.submitted.proposal.service.ProposalDocument;
import com.iortatechnxt.brokerverse.submitted.proposal.service.SbmProposalService;
import com.iortatechnxt.brokerverse.submitted.proposal.service.SbmProposalService.Line;
import com.iortatechnxt.brokerverse.submitted.proposal.service.SbmProposalService.Proposed;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Renewal proposals and nominated rates of Submitted Policies (FR-SP-066, 067): preview and
 * generate proposals in a batch, release or return them, assign the preferred insurer, download a
 * proposal, and maintain the nominated rates with maker and checker.
 */
@RestController
@RequestMapping("/api/v1/submitted/proposals")
public class SbmProposalController {

  private static final String PROPOSE = "hasAuthority('SBM_PROPOSAL')";

  private final SbmProposalService proposals;
  private final NominatedRateService rates;
  private final ProposalDocument documents;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param proposals proposals
   * @param rates nominated rates
   * @param documents proposal PDF
   * @param downloads file download answers
   */
  public SbmProposalController(
      SbmProposalService proposals,
      NominatedRateService rates,
      ProposalDocument documents,
      FileDownloads downloads) {
    this.proposals = proposals;
    this.rates = rates;
    this.documents = documents;
    this.downloads = downloads;
  }

  /**
   * The proposed insurer, rate and premium of the selected records.
   *
   * @param request company and records
   * @return one line per record
   */
  @PostMapping("/preview")
  @PreAuthorize(PROPOSE)
  public List<Proposed> preview(@RequestBody PreviewRequest request) {
    return proposals.preview(request.companyId(), request.policyIds());
  }

  /**
   * Generates the proposals in a new batch.
   *
   * @param request company and lines
   * @return the batch with its proposals
   */
  @PostMapping
  @PreAuthorize(PROPOSE)
  @Transactional
  public BatchView generate(@RequestBody GenerateRequest request) {
    SbmProposalBatch batch = proposals.generate(request.companyId(), request.lines());
    return view(batch);
  }

  /**
   * The batches of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  @GetMapping("/batches")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public PageResponse<BatchView> batches(@RequestParam Long companyId, Pageable pageable) {
    return PageResponse.of(proposals.batches(companyId, pageable), this::view);
  }

  /**
   * Assigns a preferred insurer to proposals For Review.
   *
   * @param request proposals and insurer
   * @return the proposals
   */
  @PostMapping("/assign-insurer")
  @PreAuthorize(PROPOSE)
  @Transactional
  public List<ProposalView> assignInsurer(@RequestBody AssignRequest request) {
    return proposals.assignInsurer(request.proposalIds(), request.insurerCode()).stream()
        .map(this::view)
        .toList();
  }

  /**
   * Releases a proposal.
   *
   * @param id proposal
   * @return proposal
   */
  @PostMapping("/{id}/release")
  @PreAuthorize(PROPOSE)
  @Transactional
  public ProposalView release(@PathVariable Long id) {
    return view(proposals.release(id));
  }

  /**
   * Returns a proposal with a reason.
   *
   * @param id proposal
   * @param request reason
   * @return proposal
   */
  @PostMapping("/{id}/return")
  @PreAuthorize(PROPOSE)
  @Transactional
  public ProposalView returnProposal(@PathVariable Long id, @RequestBody ReasonRequest request) {
    return view(proposals.returnProposal(id, request.reason()));
  }

  /**
   * The PDF of a proposal.
   *
   * @param id proposal
   * @param request request (download audit and links)
   * @return PDF
   */
  @GetMapping("/{id}/document")
  @PreAuthorize(MasterlistController.VIEW)
  public ResponseEntity<byte[]> document(@PathVariable Long id, HttpServletRequest request) {
    return downloads.respond(documents.pdf(id), request);
  }

  /**
   * The nominated rates of a company.
   *
   * @param companyId company
   * @return rates
   */
  @GetMapping("/rates")
  @PreAuthorize(MasterlistController.VIEW)
  public List<RateView> rates(@RequestParam Long companyId) {
    return rates.list(companyId).stream().map(RateView::of).toList();
  }

  /**
   * Adds a nominated rate.
   *
   * @param companyId company
   * @param row values
   * @return rate
   */
  @PostMapping("/rates")
  @PreAuthorize(PROPOSE)
  public RateView addRate(@RequestParam Long companyId, @RequestBody SbmNominatedRate.Row row) {
    return RateView.of(rates.create(companyId, row));
  }

  /**
   * Changes a nominated rate.
   *
   * @param companyId company
   * @param id rate
   * @param row values
   * @return rate
   */
  @PutMapping("/rates/{id}")
  @PreAuthorize(PROPOSE)
  public RateView changeRate(
      @RequestParam Long companyId, @PathVariable Long id, @RequestBody SbmNominatedRate.Row row) {
    return RateView.of(rates.update(companyId, id, row));
  }

  /**
   * Authorizes or deactivates a nominated rate.
   *
   * @param companyId company
   * @param id rate
   * @param action authorize or deactivate
   * @return rate
   */
  @PostMapping("/rates/{id}/{action}")
  @PreAuthorize(PROPOSE)
  public RateView rateAction(
      @RequestParam Long companyId, @PathVariable Long id, @PathVariable String action) {
    return RateView.of(
        "authorize".equals(action)
            ? rates.authorize(companyId, id)
            : rates.deactivate(companyId, id));
  }

  private BatchView view(SbmProposalBatch batch) {
    return new BatchView(
        batch.getId(),
        batch.getBatchNo(),
        batch.getCreatedBy(),
        batch.getCreatedAt(),
        proposals.ofBatch(batch.getId()).stream().map(this::view).toList());
  }

  private ProposalView view(SbmProposal p) {
    SbmPolicy policy = proposals.policy(p.getCompanyId(), p.getPolicyId());
    return new ProposalView(
        p.getId(),
        p.getPolicyId(),
        policy.getSbmNo(),
        policy.getAssured().assuredName(),
        p.getVersionNo(),
        p.getDefaultInsurer(),
        p.getInsurerCode(),
        p.getNominatedRate(),
        p.getAppliedRate(),
        p.getRateReason(),
        p.getSumInsured(),
        p.getPremium(),
        p.getStatus().name(),
        p.getReturnReason(),
        p.getCreatedBy(),
        p.getReleasedBy());
  }

  /**
   * Records to preview.
   *
   * @param companyId company
   * @param policyIds records
   */
  public record PreviewRequest(Long companyId, List<Long> policyIds) {}

  /**
   * Proposals to generate.
   *
   * @param companyId company
   * @param lines records with the insurer and rate to apply
   */
  public record GenerateRequest(Long companyId, List<Line> lines) {}

  /**
   * An insurer for proposals.
   *
   * @param proposalIds proposals
   * @param insurerCode insurer
   */
  public record AssignRequest(List<Long> proposalIds, String insurerCode) {}

  /**
   * A reason.
   *
   * @param reason reason
   */
  public record ReasonRequest(String reason) {}

  /**
   * A batch with its proposals.
   *
   * @param id id
   * @param batchNo SBP-yyyy-nnnnnn
   * @param createdBy maker
   * @param createdAt time
   * @param proposals proposals
   */
  public record BatchView(
      Long id, String batchNo, String createdBy, Instant createdAt, List<ProposalView> proposals) {}

  /**
   * A proposal.
   *
   * @param id id
   * @param policyId record
   * @param sbmNo masterlist number
   * @param assuredName assured
   * @param versionNo version
   * @param defaultInsurer insurer of the rules
   * @param insurerCode insurer chosen
   * @param nominatedRate nominated rate
   * @param appliedRate rate applied
   * @param rateReason reason of an edited rate
   * @param sumInsured sum insured
   * @param premium premium
   * @param status FOR_REVIEW, RELEASED, RETURNED or SUPERSEDED
   * @param returnReason reason of a return
   * @param maker maker
   * @param releasedBy Team Lead who released it
   */
  public record ProposalView(
      Long id,
      Long policyId,
      String sbmNo,
      String assuredName,
      int versionNo,
      String defaultInsurer,
      String insurerCode,
      BigDecimal nominatedRate,
      BigDecimal appliedRate,
      String rateReason,
      BigDecimal sumInsured,
      BigDecimal premium,
      String status,
      String returnReason,
      String maker,
      String releasedBy) {}

  /**
   * A nominated rate.
   *
   * @param id id
   * @param segment segment
   * @param vehicleType vehicle classification, null for every classification
   * @param insurerCode insurer
   * @param rate rate in percent
   * @param effectiveFrom first day
   * @param effectiveTo last day
   * @param recordStatus maker-checker status
   * @param maker maker
   */
  public record RateView(
      Long id,
      String segment,
      String vehicleType,
      String insurerCode,
      BigDecimal rate,
      LocalDate effectiveFrom,
      LocalDate effectiveTo,
      String recordStatus,
      String maker) {

    static RateView of(SbmNominatedRate r) {
      return new RateView(
          r.getId(),
          r.getSegment(),
          r.getVehicleType(),
          r.getInsurerCode(),
          r.getRate(),
          r.getEffectiveFrom(),
          r.getEffectiveTo(),
          r.getRecordStatus().name(),
          r.getMaker());
    }
  }
}
