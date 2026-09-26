package com.iortatechnxt.brokerverse.screening.cases.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.CaseDetail;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.CaseDocumentDto;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.CaseEventDto;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.CaseQuery;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.CaseRequests;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.CaseRow;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.ReviewDto;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.TilesDto;
import com.iortatechnxt.brokerverse.screening.cases.api.dto.VoteDto;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocument.DocumentMeta;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseActions;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseDocumentService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseMatchService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseQueries;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseReviewService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseTimeline;
import com.iortatechnxt.brokerverse.screening.matching.api.dto.MatchRow;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Screening cases: list, case page and its tabs (SNSRP-401-405, 501, 601; FR-SS-040 to 045, 050,
 * 052): the case list by tab and filters, the Screening Home tiles, a case with the user's actions,
 * its timeline, matches, review, documents and committee votes, the client's cases, saving the
 * review draft and uploading documents.
 */
@RestController
@RequestMapping("/api/v1/screening")
public class CaseController {

  /** Guard: screening view. */
  static final String HAS_VIEW = "hasAuthority('SCR_VIEW')";

  /** Guard: investigation. */
  static final String HAS_INVESTIGATE = "hasAuthority('SCR_INVESTIGATE')";

  private static final int MAX_PAGE = 200;

  private final CaseQueries queries;
  private final CaseActions actions;
  private final CaseTimeline timeline;
  private final CaseReviewService reviews;
  private final CaseDocumentService documents;
  private final CaseMatchService matches;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param queries case reads
   * @param actions the user's actions
   * @param timeline case timeline
   * @param reviews reviews
   * @param documents documents
   * @param matches case matches
   * @param clock clock
   */
  public CaseController(
      CaseQueries queries,
      CaseActions actions,
      CaseTimeline timeline,
      CaseReviewService reviews,
      CaseDocumentService documents,
      CaseMatchService matches,
      Clock clock) {
    this.queries = queries;
    this.actions = actions;
    this.timeline = timeline;
    this.reviews = reviews;
    this.documents = documents;
    this.matches = matches;
    this.clock = clock;
  }

  /**
   * The case list (FR-SS-041, 042), newest first.
   *
   * @param query tab, text, filters and page
   * @return cases
   */
  @GetMapping("/cases")
  @PreAuthorize(HAS_VIEW)
  public PageResponse<CaseRow> cases(@Valid CaseQuery query) {
    var now = clock.instant();
    return PageResponse.of(
        queries.search(
            query.search(),
            PageRequest.of(
                query.pageNumber(), query.pageSize(MAX_PAGE), Sort.by(Sort.Order.desc("id")))),
        c -> CaseRow.from(c, now));
  }

  /**
   * The Screening Home tiles (FR-SS-045).
   *
   * @param companyId company
   * @return tiles
   */
  @GetMapping("/cases/tiles")
  @PreAuthorize(HAS_VIEW)
  public TilesDto tiles(@RequestParam Long companyId) {
    return TilesDto.from(queries.tiles(companyId));
  }

  /**
   * A case with the actions of the current user.
   *
   * @param id the case
   * @return the case
   */
  @GetMapping("/cases/{id}")
  @PreAuthorize(HAS_VIEW)
  public CaseDetail get(@PathVariable Long id) {
    ScreeningCase c = queries.get(id);
    return CaseDetail.from(c, clock.instant(), actions.of(c));
  }

  /**
   * The timeline of a case (FR-SS-040).
   *
   * @param id the case
   * @return events, oldest first
   */
  @GetMapping("/cases/{id}/timeline")
  @PreAuthorize(HAS_VIEW)
  public List<CaseEventDto> timeline(@PathVariable Long id) {
    queries.get(id);
    return timeline.of(id).stream().map(CaseEventDto::from).toList();
  }

  /**
   * The matches of a case.
   *
   * @param id the case
   * @return matches
   */
  @GetMapping("/cases/{id}/matches")
  @PreAuthorize(HAS_VIEW)
  public List<MatchRow> matches(@PathVariable Long id) {
    return matches.of(queries.get(id)).stream().map(MatchRow::from).toList();
  }

  /**
   * The committee votes of a case (FR-SS-064 Decisions tab).
   *
   * @param id the case
   * @return votes
   */
  @GetMapping("/cases/{id}/votes")
  @PreAuthorize(HAS_VIEW)
  public List<VoteDto> votes(@PathVariable Long id) {
    queries.get(id);
    return queries.votes(id).stream().map(VoteDto::from).toList();
  }

  /**
   * The review of a case (FR-SS-050); 204 when the case has no review.
   *
   * @param id the case
   * @return the review
   */
  @GetMapping("/cases/{id}/review")
  @PreAuthorize(HAS_VIEW)
  public ResponseEntity<ReviewDto> review(@PathVariable Long id) {
    return reviews
        .form(queries.get(id))
        .map(ReviewDto::from)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.noContent().build());
  }

  /**
   * Saves the review as a draft (FR-SS-050).
   *
   * @param id the case
   * @param request raw values by field code
   * @return the review
   */
  @PutMapping("/cases/{id}/review")
  @PreAuthorize(HAS_INVESTIGATE)
  public ReviewDto saveReview(@PathVariable Long id, @RequestBody CaseRequests.Review request) {
    return ReviewDto.from(reviews.save(queries.get(id), request.values()));
  }

  /**
   * The documents of a case (FR-SS-052).
   *
   * @param id the case
   * @return documents
   */
  @GetMapping("/cases/{id}/documents")
  @PreAuthorize(HAS_VIEW)
  public List<CaseDocumentDto> documents(@PathVariable Long id) {
    queries.get(id);
    return documents.of(id).stream().map(CaseDocumentDto::from).toList();
  }

  /**
   * Uploads a document with its metadata (FR-SS-052).
   *
   * @param id the case
   * @param formType form type
   * @param documentType document type
   * @param dateReceived date received
   * @param source source
   * @param file the file
   * @return the document
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/cases/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('SCR_INVESTIGATE','SCR_COMPLIANCE_REVIEW','SCR_CASE_APPROVE')")
  public CaseDocumentDto upload(
      @PathVariable Long id,
      @RequestParam(required = false) String formType,
      @RequestParam(required = false) String documentType,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dateReceived,
      @RequestParam(required = false) String source,
      @RequestParam MultipartFile file)
      throws IOException {
    return CaseDocumentDto.from(
        documents.upload(
            queries.get(id),
            new DocumentMeta(formType, documentType, dateReceived, source),
            file.getOriginalFilename(),
            file.getBytes()));
  }

  /**
   * The cases of a client (client Screening tab).
   *
   * @param clientId the client
   * @return cases, newest first
   */
  @GetMapping("/clients/{clientId}/cases")
  @PreAuthorize(HAS_VIEW)
  public List<CaseRow> clientCases(@PathVariable Long clientId) {
    var now = clock.instant();
    return queries.ofClient(clientId).stream().map(c -> CaseRow.from(c, now)).toList();
  }
}
