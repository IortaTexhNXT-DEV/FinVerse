package com.iortatechnxt.brokerverse.submitted.review.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovalMatrix;
import com.iortatechnxt.brokerverse.submitted.domain.SbmDocStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaaf;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafLinkRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafReviewRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTor;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTorRepository;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.MasterlistController;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.review.api.dto.ReviewDtos.Approval;
import com.iortatechnxt.brokerverse.submitted.review.api.dto.ReviewDtos.IaafRequest;
import com.iortatechnxt.brokerverse.submitted.review.api.dto.ReviewDtos.IaafView;
import com.iortatechnxt.brokerverse.submitted.review.api.dto.ReviewDtos.LinkView;
import com.iortatechnxt.brokerverse.submitted.review.api.dto.ReviewDtos.PolicyFacts;
import com.iortatechnxt.brokerverse.submitted.review.api.dto.ReviewDtos.ReturnRequest;
import com.iortatechnxt.brokerverse.submitted.review.api.dto.ReviewDtos.ReviewRequest;
import com.iortatechnxt.brokerverse.submitted.review.api.dto.ReviewDtos.ReviewView;
import com.iortatechnxt.brokerverse.submitted.review.api.dto.ReviewDtos.TorRequest;
import com.iortatechnxt.brokerverse.submitted.review.api.dto.ReviewDtos.TorView;
import com.iortatechnxt.brokerverse.submitted.review.service.DocumentApprovals;
import com.iortatechnxt.brokerverse.submitted.review.service.IaafService;
import com.iortatechnxt.brokerverse.submitted.review.service.TorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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
 * Policy Reviews / IAAF and TOR (FRS FR-SP-040, 041, 051-053): the reviews of a record, its IAAF
 * with approvals and sending, the TORs with approvals, and the download that releases a TOR to its
 * Account Officer.
 */
@RestController
@RequestMapping("/api/v1/submitted")
public class ReviewController {

  private static final String PREPARE_IAAF = "hasAuthority('IAAF_PREPARE')";
  private static final String APPROVE_IAAF = "hasAuthority('IAAF_APPROVE')";
  private static final String PREPARE_TOR = "hasAuthority('TOR_PREPARE')";
  private static final String APPROVE_TOR = "hasAuthority('TOR_APPROVE')";

  private final IaafService iaafService;
  private final TorService torService;
  private final DocumentApprovals approvals;
  private final SbmIaafRepository iaafs;
  private final SbmTorRepository tors;
  private final SbmIaafReviewRepository reviews;
  private final SbmIaafLinkRepository links;
  private final SbmPolicyRepository policies;
  private final MasterlistService masterlist;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param iaafService IAAF
   * @param torService TOR
   * @param approvals matrix approvals
   * @param iaafs IAAFs
   * @param tors TORs
   * @param reviews reviews
   * @param links IAAF links
   * @param policies masterlist
   * @param masterlist masterlist (scope)
   * @param downloads file answers
   */
  @SuppressWarnings("java:S107") // reads of the review screens
  public ReviewController(
      IaafService iaafService,
      TorService torService,
      DocumentApprovals approvals,
      SbmIaafRepository iaafs,
      SbmTorRepository tors,
      SbmIaafReviewRepository reviews,
      SbmIaafLinkRepository links,
      SbmPolicyRepository policies,
      MasterlistService masterlist,
      FileDownloads downloads) {
    this.iaafService = iaafService;
    this.torService = torService;
    this.approvals = approvals;
    this.iaafs = iaafs;
    this.tors = tors;
    this.reviews = reviews;
    this.links = links;
    this.policies = policies;
    this.masterlist = masterlist;
    this.downloads = downloads;
  }

  /**
   * The reviews of a record.
   *
   * @param policyId record
   * @return reviews, oldest first
   */
  @GetMapping("/policies/{policyId}/reviews")
  @PreAuthorize(MasterlistController.VIEW)
  public List<ReviewView> reviews(@PathVariable Long policyId) {
    masterlist.get(policyId);
    return reviews.findByPolicyIdOrderByReviewNoAsc(policyId).stream()
        .map(ReviewView::from)
        .toList();
  }

  /**
   * Records a review.
   *
   * @param policyId record
   * @param r review
   * @return the review
   */
  @PostMapping("/policies/{policyId}/reviews")
  @PreAuthorize(PREPARE_IAAF)
  @Transactional
  public ReviewView review(@PathVariable Long policyId, @Valid @RequestBody ReviewRequest r) {
    return ReviewView.from(iaafService.review(policyId, r.content()));
  }

  /**
   * The IAAF of a record.
   *
   * @param policyId record
   * @return the IAAF, empty when none
   */
  @GetMapping("/policies/{policyId}/iaaf")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public List<IaafView> iaafOf(@PathVariable Long policyId) {
    masterlist.get(policyId);
    return iaafs.findByPolicyId(policyId).map(this::view).stream().toList();
  }

  /**
   * Generates the IAAF of a record.
   *
   * @param policyId record
   * @param r related policies
   * @return the IAAF
   */
  @PostMapping("/policies/{policyId}/iaaf")
  @PreAuthorize(PREPARE_IAAF)
  @Transactional
  public IaafView generate(@PathVariable Long policyId, @RequestBody IaafRequest r) {
    return view(iaafService.generate(policyId, r.related()));
  }

  /**
   * IAAFs of a status tab.
   *
   * @param companyId company
   * @param status statuses
   * @param pageable page
   * @return IAAFs
   */
  @GetMapping("/iaaf")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public PageResponse<IaafView> iaafs(
      @RequestParam Long companyId, @RequestParam List<SbmDocStatus> status, Pageable pageable) {
    return PageResponse.of(
        iaafs.findByCompanyIdAndStatusInOrderByIdDesc(companyId, status, pageable), this::view);
  }

  /**
   * An IAAF.
   *
   * @param id IAAF
   * @return IAAF
   */
  @GetMapping("/iaaf/{id}")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public IaafView iaaf(@PathVariable Long id) {
    return view(iaafService.get(id));
  }

  /**
   * Submits an IAAF.
   *
   * @param id IAAF
   * @return IAAF
   */
  @PostMapping("/iaaf/{id}/submit")
  @PreAuthorize(PREPARE_IAAF)
  @Transactional
  public IaafView submitIaaf(@PathVariable Long id) {
    return view(iaafService.submit(id));
  }

  /**
   * Approves the current level of an IAAF.
   *
   * @param id IAAF
   * @return IAAF
   */
  @PostMapping("/iaaf/{id}/approve")
  @PreAuthorize(APPROVE_IAAF)
  @Transactional
  public IaafView approveIaaf(@PathVariable Long id) {
    return view(iaafService.approve(id));
  }

  /**
   * Returns an IAAF.
   *
   * @param id IAAF
   * @param r reason
   * @return IAAF
   */
  @PostMapping("/iaaf/{id}/return")
  @PreAuthorize(APPROVE_IAAF)
  @Transactional
  public IaafView returnIaaf(@PathVariable Long id, @RequestBody ReturnRequest r) {
    return view(iaafService.returned(id, r.reasonCode(), r.comment()));
  }

  /**
   * Sends an approved IAAF to the bank counterpart.
   *
   * @param id IAAF
   * @return IAAF
   */
  @PostMapping("/iaaf/{id}/issue")
  @PreAuthorize(PREPARE_IAAF)
  @Transactional
  public IaafView issue(@PathVariable Long id) {
    return view(iaafService.issue(id));
  }

  /**
   * Cancels an IAAF.
   *
   * @param id IAAF
   * @param r reason
   * @return IAAF
   */
  @PostMapping("/iaaf/{id}/cancel")
  @PreAuthorize(PREPARE_IAAF)
  @Transactional
  public IaafView cancelIaaf(@PathVariable Long id, @RequestBody ReturnRequest r) {
    return view(iaafService.cancel(id, r.reasonCode()));
  }

  /**
   * The TORs of a record.
   *
   * @param policyId record
   * @return TORs, newest first
   */
  @GetMapping("/policies/{policyId}/tors")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public List<TorView> torsOf(@PathVariable Long policyId) {
    masterlist.get(policyId);
    return tors.findByPolicyIdOrderByIdDesc(policyId).stream().map(this::view).toList();
  }

  /**
   * The breached limits a new TOR would list.
   *
   * @param policyId record
   * @return breaches, one per line
   */
  @GetMapping("/policies/{policyId}/breaches")
  @PreAuthorize(MasterlistController.VIEW)
  public Map<String, String> breaches(@PathVariable Long policyId) {
    masterlist.get(policyId);
    return Map.of("breaches", torService.breaches(policyId));
  }

  /**
   * Generates a TOR.
   *
   * @param r record, proposed terms and AO
   * @return the TOR
   */
  @PostMapping("/tors")
  @PreAuthorize(PREPARE_TOR)
  @Transactional
  public TorView generateTor(@Valid @RequestBody TorRequest r) {
    return view(torService.generate(r.policyId(), r.proposedTerms(), r.aoUsername()));
  }

  /**
   * TORs of a status tab.
   *
   * @param companyId company
   * @param status statuses
   * @param pageable page
   * @return TORs
   */
  @GetMapping("/tors")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public PageResponse<TorView> tors(
      @RequestParam Long companyId, @RequestParam List<SbmDocStatus> status, Pageable pageable) {
    return PageResponse.of(
        tors.findByCompanyIdAndStatusInOrderByIdDesc(companyId, status, pageable), this::view);
  }

  /**
   * A TOR.
   *
   * @param id TOR
   * @return TOR
   */
  @GetMapping("/tors/{id}")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public TorView tor(@PathVariable Long id) {
    return view(torService.get(id));
  }

  /**
   * Changes a draft or returned TOR.
   *
   * @param id TOR
   * @param r proposed terms and AO
   * @return TOR
   */
  @PutMapping("/tors/{id}")
  @PreAuthorize(PREPARE_TOR)
  @Transactional
  public TorView changeTor(@PathVariable Long id, @Valid @RequestBody TorRequest r) {
    return view(torService.change(id, r.proposedTerms(), r.aoUsername()));
  }

  /**
   * Submits a TOR.
   *
   * @param id TOR
   * @return TOR
   */
  @PostMapping("/tors/{id}/submit")
  @PreAuthorize(PREPARE_TOR)
  @Transactional
  public TorView submitTor(@PathVariable Long id) {
    return view(torService.submit(id));
  }

  /**
   * Approves the current level of a TOR.
   *
   * @param id TOR
   * @return TOR
   */
  @PostMapping("/tors/{id}/approve")
  @PreAuthorize(APPROVE_TOR)
  @Transactional
  public TorView approveTor(@PathVariable Long id) {
    return view(torService.approve(id));
  }

  /**
   * Returns a TOR.
   *
   * @param id TOR
   * @param r reason
   * @return TOR
   */
  @PostMapping("/tors/{id}/return")
  @PreAuthorize(APPROVE_TOR)
  @Transactional
  public TorView returnTor(@PathVariable Long id, @RequestBody ReturnRequest r) {
    return view(torService.returned(id, r.reasonCode(), r.comment()));
  }

  /**
   * Cancels a TOR.
   *
   * @param id TOR
   * @param r reason
   * @return TOR
   */
  @PostMapping("/tors/{id}/cancel")
  @PreAuthorize(PREPARE_TOR)
  @Transactional
  public TorView cancelTor(@PathVariable Long id, @RequestBody ReturnRequest r) {
    return view(torService.cancel(id, r.reasonCode()));
  }

  /**
   * Downloads the signed TOR; the Account Officer's first download releases it.
   *
   * @param id TOR
   * @param request request (download mode)
   * @return the file or a redirect to its link
   */
  @GetMapping("/tors/{id}/pdf")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional
  public ResponseEntity<byte[]> torPdf(@PathVariable Long id, HttpServletRequest request) {
    return downloads.respond(torService.download(id), request);
  }

  private IaafView view(SbmIaaf i) {
    SbmPolicy p = policies.findById(i.getPolicyId()).orElseThrow();
    return IaafView.from(
        i,
        facts(p),
        Approval.of(
            i,
            levels(() -> iaafService.levels(i, p)),
            approvals.signaturesOf(DocumentApprovals.IAAF, i.getId())),
        reviews.findByPolicyIdOrderByReviewNoAsc(p.getId()).stream().map(ReviewView::from).toList(),
        links.findByIaafIdOrderByIdAsc(i.getId()).stream()
            .map(l -> LinkView.from(l, sbmNo(l.getRelatedPolicyId())))
            .toList());
  }

  private TorView view(SbmTor t) {
    SbmPolicy p = t.getPolicyId() == null ? null : policies.findById(t.getPolicyId()).orElse(null);
    return TorView.from(
        t,
        p == null ? new PolicyFacts(null, null, null, null) : facts(p),
        Approval.of(
            t,
            p == null ? List.of() : levels(() -> torService.levels(t, p)),
            approvals.signaturesOf(DocumentApprovals.TOR, t.getId())));
  }

  private List<SbmApprovalMatrix> levels(Supplier<List<SbmApprovalMatrix>> read) {
    try {
      return read.get();
    } catch (BusinessRuleException e) {
      return List.of();
    }
  }

  private String sbmNo(Long policyId) {
    return policies.findById(policyId).map(SbmPolicy::getSbmNo).orElse(null);
  }

  private static PolicyFacts facts(SbmPolicy p) {
    return new PolicyFacts(
        p.getSbmNo(), p.getAssured().assuredName(), p.getSegment(), p.getTerms().sumInsured());
  }
}
