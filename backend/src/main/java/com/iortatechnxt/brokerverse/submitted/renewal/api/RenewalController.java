package com.iortatechnxt.brokerverse.submitted.renewal.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetter;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLetterRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPrintBatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPrintBatchRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewal;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewalRepository;
import com.iortatechnxt.brokerverse.submitted.masterlist.api.MasterlistController;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.renewal.api.dto.RenewalDtos.DispatchView;
import com.iortatechnxt.brokerverse.submitted.renewal.api.dto.RenewalDtos.LetterView;
import com.iortatechnxt.brokerverse.submitted.renewal.api.dto.RenewalDtos.PrintBatchView;
import com.iortatechnxt.brokerverse.submitted.renewal.api.dto.RenewalDtos.ReassignRequest;
import com.iortatechnxt.brokerverse.submitted.renewal.api.dto.RenewalDtos.RenewalRow;
import com.iortatechnxt.brokerverse.submitted.renewal.api.dto.RenewalDtos.ScanView;
import com.iortatechnxt.brokerverse.submitted.renewal.service.ExpiryScanService;
import com.iortatechnxt.brokerverse.submitted.renewal.service.HandOffService;
import com.iortatechnxt.brokerverse.submitted.renewal.service.LetterService;
import com.iortatechnxt.brokerverse.submitted.renewal.service.RenewalFollowService;
import com.iortatechnxt.brokerverse.submitted.service.SbmScopeService;
import jakarta.persistence.criteria.Subquery;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Renewal Work List, letters and print batches (FR-SP-060 to 066): the records handed to Renewal
 * within the user's scope, Renew with BDOI, the re-assignment of the insurer, the expiry scan and
 * the letter dispatch started by hand, the letters of a record and the print batches.
 */
@RestController
@RequestMapping("/api/v1/submitted")
public class RenewalController {

  private static final String PROCESS = "hasAuthority('SBM_PROCESS')";
  private static final String RENEW = "hasAnyAuthority('SBM_PROCESS','SBM_MAINTAIN')";
  private static final String LETTERS = "hasAuthority('SBM_LETTER_SEND')";

  private final SbmRenewalRepository renewals;
  private final SbmPolicyRepository policies;
  private final SbmLetterRepository letters;
  private final SbmPrintBatchRepository batches;
  private final SbmScopeService scopes;
  private final MasterlistService masterlist;
  private final Services services;
  private final FileDownloads downloads;
  private final Clock clock;

  /**
   * The renewal services behind the controller.
   *
   * @param handOff hand-off
   * @param follow follow-up and re-assignment
   * @param scan expiry scan
   * @param letterService letters
   */
  public record Services(
      HandOffService handOff,
      RenewalFollowService follow,
      ExpiryScanService scan,
      LetterService letterService) {}

  /**
   * Creates the controller.
   *
   * @param renewals hand-offs
   * @param policies masterlist
   * @param letters letters
   * @param batches print batches
   * @param scopes scope
   * @param masterlist masterlist (scope of one record)
   * @param handOff hand-off
   * @param follow follow-up
   * @param scan expiry scan
   * @param letterService letters
   * @param downloads file answers
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // reads and actions of the work list
  public RenewalController(
      SbmRenewalRepository renewals,
      SbmPolicyRepository policies,
      SbmLetterRepository letters,
      SbmPrintBatchRepository batches,
      SbmScopeService scopes,
      MasterlistService masterlist,
      HandOffService handOff,
      RenewalFollowService follow,
      ExpiryScanService scan,
      LetterService letterService,
      FileDownloads downloads,
      Clock clock) {
    this.renewals = renewals;
    this.policies = policies;
    this.letters = letters;
    this.batches = batches;
    this.scopes = scopes;
    this.masterlist = masterlist;
    this.services = new Services(handOff, follow, scan, letterService);
    this.downloads = downloads;
    this.clock = clock;
  }

  /**
   * The renewal work list within the user's scope.
   *
   * @param companyId company
   * @param status hand-off statuses, all when empty
   * @param pageable page
   * @return rows
   */
  @GetMapping("/renewals")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public PageResponse<RenewalRow> workList(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<String> status,
      Pageable pageable) {
    Specification<SbmPolicy> handedOff =
        (root, query, cb) -> {
          Subquery<Long> sub = query.subquery(Long.class);
          var r = sub.from(SbmRenewal.class);
          var where = cb.equal(r.get("companyId"), companyId);
          if (status != null && !status.isEmpty()) {
            where = cb.and(where, r.get("handoffStatus").in(status));
          }
          sub.select(r.get("policyId")).where(where);
          return root.get("id").in(sub);
        };
    Specification<SbmPolicy> spec = scopes.current(companyId).specification().and(handedOff);
    return PageResponse.of(policies.findAll(spec, pageable), this::row);
  }

  /**
   * The hand-off of a record.
   *
   * @param policyId record
   * @return the hand-off, empty when none
   */
  @GetMapping("/policies/{policyId}/renewal")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public List<RenewalRow> renewalOf(@PathVariable Long policyId) {
    SbmPolicy p = masterlist.get(policyId);
    return renewals.findByPolicyId(policyId).map(r -> RenewalRow.from(r, p)).stream().toList();
  }

  /**
   * Renew with BDOI.
   *
   * @param policyId record For Renewal
   * @return the hand-off
   */
  @PostMapping("/policies/{policyId}/renew")
  @PreAuthorize(RENEW)
  @Transactional
  public RenewalRow renew(@PathVariable Long policyId) {
    SbmRenewal r = services.handOff().renewWithBdoi(policyId);
    return RenewalRow.from(r, masterlist.get(policyId));
  }

  /**
   * Re-assigns the insurer while the hold cover request is open.
   *
   * @param policyId record
   * @param request insurer and reason
   * @return the hand-off
   */
  @PostMapping("/policies/{policyId}/reassign")
  @PreAuthorize(PROCESS)
  @Transactional
  public RenewalRow reassign(
      @PathVariable Long policyId, @Valid @RequestBody ReassignRequest request) {
    SbmRenewal r =
        services.follow().reassign(policyId, request.insurerCode(), request.reasonCode());
    return RenewalRow.from(r, masterlist.get(policyId));
  }

  /**
   * Runs the expiry scan of a company now.
   *
   * @param companyId company
   * @return counts
   */
  @PostMapping("/renewals/scan")
  @PreAuthorize(PROCESS)
  public ScanView scan(@RequestParam Long companyId) {
    ExpiryScanService.Scan s = services.scan().scan(companyId, BusinessClock.today(clock));
    return new ScanView(s.handedOff(), s.noInsurer(), s.replayed());
  }

  /**
   * The letters of a record.
   *
   * @param policyId record
   * @return letters, newest first
   */
  @GetMapping("/policies/{policyId}/letters")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public List<LetterView> lettersOf(@PathVariable Long policyId) {
    SbmPolicy p = masterlist.get(policyId);
    return letters.findByPolicyIdOrderByIdDesc(policyId).stream()
        .map(l -> LetterView.from(l, p.getSbmNo()))
        .toList();
  }

  /**
   * Letters of a company by status.
   *
   * @param companyId company
   * @param status statuses
   * @param pageable page
   * @return letters
   */
  @GetMapping("/letters")
  @PreAuthorize(LETTERS)
  @Transactional(readOnly = true)
  public PageResponse<LetterView> letters(
      @RequestParam Long companyId, @RequestParam List<String> status, Pageable pageable) {
    return PageResponse.of(
        letters.findByCompanyIdAndStatusInOrderByIdDesc(companyId, status, pageable),
        l -> LetterView.from(l, sbmNo(l.getPolicyId())));
  }

  /**
   * Sends a refused letter again.
   *
   * @param id letter
   * @return the letter
   */
  @PostMapping("/letters/{id}/resend")
  @PreAuthorize(LETTERS)
  @Transactional
  public LetterView resend(@PathVariable Long id) {
    SbmLetter l = services.letterService().resend(id);
    return LetterView.from(l, sbmNo(l.getPolicyId()));
  }

  /**
   * Downloads a letter.
   *
   * @param id letter
   * @param request request
   * @return the PDF
   */
  @GetMapping("/letters/{id}/pdf")
  @PreAuthorize(MasterlistController.VIEW)
  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> letterPdf(@PathVariable Long id, HttpServletRequest request) {
    SbmLetter l =
        letters.findById(id).orElseThrow(() -> new ResourceNotFoundException("Letter", id));
    masterlist.get(l.getPolicyId());
    if (l.getStoredFileId() == null) {
      throw new ResourceNotFoundException("Letter file", id);
    }
    return downloads.respond(FileDownload.stored(l.getStoredFileId()), request);
  }

  /**
   * Sends the due letters and hands the print batches over now.
   *
   * @param companyId company
   * @return counts
   */
  @PostMapping("/letters/dispatch")
  @PreAuthorize(LETTERS)
  public DispatchView dispatch(@RequestParam Long companyId) {
    LetterService.Dispatch d =
        services.letterService().dispatch(companyId, BusinessClock.today(clock));
    return new DispatchView(d.letters(), d.printBatches());
  }

  /**
   * The print batches of a company.
   *
   * @param companyId company
   * @param pageable page
   * @return batches, newest first
   */
  @GetMapping("/print-batches")
  @PreAuthorize(LETTERS)
  public PageResponse<PrintBatchView> printBatches(
      @RequestParam Long companyId, Pageable pageable) {
    return PageResponse.of(
        batches.findByCompanyIdOrderByIdDesc(companyId, pageable), PrintBatchView::from);
  }

  /**
   * Downloads the merged PDF or the control list of a print batch.
   *
   * @param id batch
   * @param part MERGED or CONTROL
   * @param request request
   * @return the file
   */
  @GetMapping("/print-batches/{id}/{part}")
  @PreAuthorize(LETTERS)
  public ResponseEntity<byte[]> printBatchFile(
      @PathVariable Long id, @PathVariable String part, HttpServletRequest request) {
    SbmPrintBatch b =
        batches.findById(id).orElseThrow(() -> new ResourceNotFoundException("Print batch", id));
    Long file = "control".equalsIgnoreCase(part) ? b.getControlFileId() : b.getMergedFileId();
    if (file == null) {
      throw new ResourceNotFoundException("Print batch file", id);
    }
    return downloads.respond(FileDownload.stored(file), request);
  }

  private RenewalRow row(SbmPolicy p) {
    return renewals.findByPolicyId(p.getId()).map(r -> RenewalRow.from(r, p)).orElseThrow();
  }

  private String sbmNo(Long policyId) {
    return policies.findById(policyId).map(SbmPolicy::getSbmNo).orElse(null);
  }
}
