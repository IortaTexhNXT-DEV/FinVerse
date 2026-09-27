package com.iortatechnxt.brokerverse.renewal.candidate.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateDtos.CandidateRow;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateRecordDtos.AssignmentView;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateRecordDtos.BucketChangeView;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateRecordDtos.CandidateDetail;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateRecordDtos.CheckResultView;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateRecordDtos.CheckRunView;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateRecordDtos.ChecksView;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateRecordDtos.DispositionView;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateRecordDtos.EndorsementView;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateRecordDtos.HistoryViewDto;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateRecordDtos.Lifecycle;
import com.iortatechnxt.brokerverse.renewal.candidate.api.dto.CandidateRecordDtos.OverrideView;
import com.iortatechnxt.brokerverse.renewal.candidate.service.AccountHistoryService;
import com.iortatechnxt.brokerverse.renewal.candidate.service.AccountHistoryService.AccountHistory;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateDocuments;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateQueryService;
import com.iortatechnxt.brokerverse.renewal.check.service.BlockingChecks;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.rules.service.InitiationService;
import com.iortatechnxt.brokerverse.renewal.rules.service.InitiationService.Outcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalScope.Scope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Renewal lists and record page (FR-RN-011-013, 040, 041, 045, 062): the Expiry List, My
 * Dispositions, the Team Leader Review Queue, the Processing Worklist and the Letters lists share
 * this endpoint with their tab; the record page reads its tabs here.
 */
@RestController
@RequestMapping("/api/v1/renewal/candidates")
public class CandidateController {

  /** View permission of the renewal screens. */
  public static final String VIEW = "hasAuthority('RNW_VIEW')";

  private static final String EXPORT = "hasAuthority('RNW_EXPORT')";
  private static final String INITIATE = "hasAnyAuthority('RNW_EXTRACT','RNW_ASSIGN')";

  private final CandidateQueryService queries;
  private final CandidateRowMapper rows;
  private final AccountHistoryService history;
  private final BlockingChecks blocking;
  private final CandidateDocuments documents;
  private final InitiationService initiation;

  /**
   * Creates the controller.
   *
   * @param queries renewal reads
   * @param rows row mapping
   * @param history account history
   * @param blocking blocking checks
   * @param documents downloads
   * @param initiation initiation
   */
  public CandidateController(
      CandidateQueryService queries,
      CandidateRowMapper rows,
      AccountHistoryService history,
      BlockingChecks blocking,
      CandidateDocuments documents,
      InitiationService initiation) {
    this.queries = queries;
    this.rows = rows;
    this.history = history;
    this.blocking = blocking;
    this.documents = documents;
    this.initiation = initiation;
  }

  /**
   * A chunk of a renewal list.
   *
   * @param params criteria
   * @param page chunk number
   * @param size chunk size (at most 1000)
   * @return rows with the total count
   */
  @GetMapping
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public PageResponse<CandidateRow> list(
      @ModelAttribute CandidateListParams params,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "200") int size) {
    Scope scope = queries.scope(params.companyId());
    Page<RenewalCandidate> chunk = queries.list(params.filter(), page, size);
    List<CandidateRow> mapped = rows.rows(chunk.getContent(), scope);
    return new PageResponse<>(
        mapped,
        chunk.getNumber(),
        chunk.getSize(),
        chunk.getTotalElements(),
        chunk.getTotalPages());
  }

  /**
   * The number of renewals of a list (tab counters).
   *
   * @param params criteria
   * @return count
   */
  @GetMapping("/count")
  @PreAuthorize(VIEW)
  public long count(@ModelAttribute CandidateListParams params) {
    return queries.count(params.filter());
  }

  /**
   * A list as a spreadsheet.
   *
   * @param params criteria
   * @return spreadsheet
   */
  @GetMapping("/export.xlsx")
  @PreAuthorize(EXPORT)
  public ResponseEntity<byte[]> export(@ModelAttribute CandidateListParams params) {
    return file(documents.export(params.filter()));
  }

  /**
   * The record page header and lifecycle.
   *
   * @param companyId company
   * @param ref renewal reference
   * @return detail
   */
  @GetMapping("/{ref}")
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public CandidateDetail get(@RequestParam Long companyId, @PathVariable String ref) {
    RenewalCandidate c = queries.get(companyId, ref);
    return new CandidateDetail(
        rows.row(c, queries.scope(companyId)),
        Lifecycle.of(c),
        blocking.failing(c).stream().map(CheckResult::getMessage).toList(),
        history.viewedByCurrentUser(c));
  }

  /**
   * The Checks and Bucket tab.
   *
   * @param companyId company
   * @param ref renewal reference
   * @return checks, runs, bucket changes and endorsements
   */
  @GetMapping("/{ref}/checks")
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public ChecksView checks(@RequestParam Long companyId, @PathVariable String ref) {
    RenewalCandidate c = queries.get(companyId, ref);
    return new ChecksView(
        queries.latestResults(c).stream().map(CheckResultView::of).toList(),
        queries.runs(c).stream().map(CheckRunView::of).toList(),
        queries.buckets(c).stream().map(BucketChangeView::of).toList(),
        queries.endorsements(c).stream().map(EndorsementView::of).toList());
  }

  /**
   * The History tab.
   *
   * @param companyId company
   * @param ref renewal reference
   * @return dispositions, assignments and overrides
   */
  @GetMapping("/{ref}/history")
  @PreAuthorize(VIEW)
  @Transactional(readOnly = true)
  public HistoryViewDto history(@RequestParam Long companyId, @PathVariable String ref) {
    RenewalCandidate c = queries.get(companyId, ref);
    return new HistoryViewDto(
        queries.dispositions(c).stream().map(DispositionView::of).toList(),
        queries.assignments(c).stream().map(AssignmentView::of).toList(),
        queries.overrides(c).stream().map(OverrideView::of).toList());
  }

  /**
   * Opens the account history (recorded; required before a disposition, FR-RN-004).
   *
   * @param companyId company
   * @param ref renewal reference
   * @return account history
   */
  @PostMapping("/{ref}/account-history")
  @PreAuthorize(VIEW)
  public AccountHistory accountHistory(@RequestParam Long companyId, @PathVariable String ref) {
    return history.open(companyId, ref);
  }

  /**
   * The record details as PDF.
   *
   * @param companyId company
   * @param ref renewal reference
   * @return PDF
   */
  @GetMapping("/{ref}/details.pdf")
  @PreAuthorize(EXPORT)
  public ResponseEntity<byte[]> details(@RequestParam Long companyId, @PathVariable String ref) {
    return file(documents.details(companyId, ref));
  }

  /**
   * Initiates extracted renewals (FR-RN-003): checks, bucket, matrix proposal and routing.
   *
   * @param request renewals
   * @return initiated references and refusals by reference
   */
  @PostMapping("/initiate")
  @PreAuthorize(INITIATE)
  public Outcome initiate(@Valid @RequestBody InitiateRequest request) {
    return initiation.initiate(request.companyId(), request.renewalRefs());
  }

  /**
   * Renewals to initiate.
   *
   * @param companyId company
   * @param renewalRefs renewal references
   */
  public record InitiateRequest(@NotNull Long companyId, @NotEmpty List<String> renewalRefs) {}

  static ResponseEntity<byte[]> file(MessageFile f) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(f.mimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(f.fileName()))
        .body(f.content());
  }
}
