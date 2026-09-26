package com.iortatechnxt.brokerverse.productmaint.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.CountsView;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.CommentBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.FormBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.ListItem;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.PrefillResponse;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.RecommendBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.RequestResponse;
import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.SearchParams;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.service.PackageDocuments;
import com.iortatechnxt.brokerverse.productmaint.service.PackageQueryService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageQueryService.CaseFacts;
import com.iortatechnxt.brokerverse.productmaint.service.PackageRequestService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageRequests;
import com.iortatechnxt.brokerverse.productmaint.service.TermsCodec;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Package requests (BRPM.008/009/011/019): the work list, the request form, submission, Marketing
 * approval, TSU recommendation and approval, the request form PDF, the pre-fill of a request from a
 * package version and the Product Maintenance home counts. Generic actions (return, void, not
 * proceeded) run through the workflow API.
 */
@RestController
@RequestMapping("/api/v1/product-maintenance")
public class PackageRequestController {

  /** Read access to the package request screens. */
  public static final String VIEW = "hasAnyAuthority('PRODUCT_VIEW', 'PKG_REPORT_VIEW')";

  private static final int MAX_PAGE = 100;

  private final PackageRequestService service;
  private final PackageQueryService queries;
  private final PackageRequests requests;
  private final PackageDocuments documents;
  private final TermsCodec codec;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param service request commands
   * @param queries request reads
   * @param requests request loader
   * @param documents request form PDF
   * @param codec terms JSON
   * @param currentUser current user
   */
  public PackageRequestController(
      PackageRequestService service,
      PackageQueryService queries,
      PackageRequests requests,
      PackageDocuments documents,
      TermsCodec codec,
      CurrentUser currentUser) {
    this.service = service;
    this.queries = queries;
    this.requests = requests;
    this.documents = documents;
    this.codec = codec;
    this.currentUser = currentUser;
  }

  /**
   * Searches package requests, newest first.
   *
   * @param params filters (company, text, stages, types, scope, mine, product, expiring within)
   * @param page page
   * @param size size
   * @return page of requests
   */
  @GetMapping("/requests")
  @PreAuthorize(VIEW)
  public PageResponse<ListItem> search(
      @ModelAttribute SearchParams params,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    if (params.companyId() == null) {
      throw new BusinessRuleException("COMPANY_REQUIRED", "Select the company");
    }
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    Page<PackageRequest> found =
        queries.search(
            params.toSearch(Boolean.TRUE.equals(params.mine()) ? currentUser.username() : null),
            pageable);
    Map<Long, CaseFacts> facts =
        queries.caseFacts(found.getContent().stream().map(PackageRequest::getId).toList());
    return PageResponse.of(found, p -> ListItem.from(p, facts.get(p.getId())));
  }

  /**
   * Creates a package request.
   *
   * @param body form
   * @return the request
   */
  @PostMapping("/requests")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('PKG_REQUEST')")
  public RequestResponse create(@Valid @RequestBody FormBody body) {
    if (body.companyId() == null) {
      throw new BusinessRuleException("COMPANY_REQUIRED", "Select the company");
    }
    return view(service.create(body.companyId(), body.toDraft()));
  }

  /**
   * One package request.
   *
   * @param id request
   * @return the request
   */
  @GetMapping("/requests/{id}")
  @PreAuthorize(VIEW)
  public RequestResponse get(@PathVariable Long id) {
    return view(requests.get(id));
  }

  /**
   * Changes a package request (draft, or TSU review).
   *
   * @param id request
   * @param body form
   * @return the request
   */
  @PutMapping("/requests/{id}")
  @PreAuthorize("hasAnyAuthority('PKG_REQUEST', 'PKG_TSU_RECOMMEND')")
  public RequestResponse update(@PathVariable Long id, @Valid @RequestBody FormBody body) {
    return view(service.update(id, body.toDraft()));
  }

  /**
   * Submits a request for Marketing approval.
   *
   * @param id request
   * @param body comment
   * @return the request
   */
  @PostMapping("/requests/{id}/submit")
  @PreAuthorize("hasAuthority('PKG_REQUEST')")
  public RequestResponse submit(@PathVariable Long id, @Valid @RequestBody CommentBody body) {
    return view(service.submit(id, body.text()));
  }

  /**
   * Marketing approval.
   *
   * @param id request
   * @param body comment
   * @return the request
   */
  @PostMapping("/requests/{id}/approve")
  @PreAuthorize("hasAuthority('PKG_REQUEST_APPROVE')")
  public RequestResponse approve(@PathVariable Long id, @Valid @RequestBody CommentBody body) {
    return view(service.approve(id, body.text()));
  }

  /**
   * TSU Team Lead recommendation.
   *
   * @param id request
   * @param body recommendation
   * @return the request
   */
  @PostMapping("/requests/{id}/recommend")
  @PreAuthorize("hasAuthority('PKG_TSU_RECOMMEND')")
  public RequestResponse recommend(@PathVariable Long id, @Valid @RequestBody RecommendBody body) {
    return view(service.recommend(id, body.text()));
  }

  /**
   * TSU Head approval (with or without negotiation, by the request's flag).
   *
   * @param id request
   * @param body comment
   * @return the request
   */
  @PostMapping("/requests/{id}/tsu-approve")
  @PreAuthorize("hasAuthority('PKG_TSU_APPROVE')")
  public RequestResponse approveTsu(@PathVariable Long id, @Valid @RequestBody CommentBody body) {
    return view(service.approveTsu(id, body.text()));
  }

  /**
   * The Package Request Form PDF.
   *
   * @param id request
   * @return PDF
   */
  @GetMapping("/requests/{id}/form.pdf")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> form(@PathVariable Long id) {
    return file(documents.requestForm(requests.get(id)));
  }

  /**
   * Terms of a package's current version to pre-fill a request.
   *
   * @param productCode product
   * @return terms
   */
  @GetMapping("/prefill")
  @PreAuthorize(VIEW)
  public PrefillResponse prefill(@RequestParam String productCode) {
    return PrefillResponse.from(service.prefill(productCode));
  }

  /**
   * Product Maintenance home counts (BRPM.019).
   *
   * @param companyId company
   * @return counts
   */
  @GetMapping("/counts")
  @PreAuthorize(VIEW)
  public CountsView counts(@RequestParam Long companyId) {
    return CountsView.from(queries.counts(companyId));
  }

  private RequestResponse view(PackageRequest p) {
    return view(p, codec);
  }

  /**
   * The response of a request with its terms.
   *
   * @param p request
   * @param codec terms JSON
   * @return response
   */
  static RequestResponse view(PackageRequest p, TermsCodec codec) {
    return RequestResponse.from(
        p,
        codec.terms(p.getRequestedTerms()),
        p.getProposedTerms() == null ? null : codec.terms(p.getProposedTerms()));
  }

  /**
   * A file download.
   *
   * @param f file
   * @return response
   */
  static ResponseEntity<byte[]> file(MessageFile f) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(f.mimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(f.fileName()))
        .body(f.content());
  }
}
