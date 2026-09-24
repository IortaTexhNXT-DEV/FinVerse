package com.iortatechnxt.brokerverse.quotation.api;

import com.iortatechnxt.brokerverse.account.api.dto.CommentRequest;
import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationActionDtos.AcceptRequest;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationActionDtos.BatchSendRequest;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationActionDtos.BatchSendResponse;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationActionDtos.DiffResponse;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationActionDtos.PreviewResponse;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationActionDtos.SendRequest;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationActionDtos.SummaryResponse;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationActionDtos.VersionContentResponse;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationBody;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationListItem;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationResponse;
import com.iortatechnxt.brokerverse.quotation.api.dto.QuotationVersionResponse;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import com.iortatechnxt.brokerverse.quotation.service.QuotationAcceptanceService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDispatchService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDocuments;
import com.iortatechnxt.brokerverse.quotation.service.QuotationQueryService;
import com.iortatechnxt.brokerverse.quotation.service.QuotationSearch;
import com.iortatechnxt.brokerverse.quotation.service.QuotationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
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

/**
 * Package quotations (BRNB.020/021/024/042-045/063/102, MKTID.011): list, detail, versions and
 * diff, documents, create and update, live premium, and the business actions of the NB_QUOTATION
 * workflow. Generic actions (return, void, decline) are run by the workflow panel.
 */
@RestController("brokingQuotationController")
@RequestMapping("/api/v1/quotations")
public class QuotationController {

  private static final String VIEW = "hasAuthority('QUOTE_VIEW')";
  private static final String MAINTAIN = "hasAuthority('QUOTE_MAINTAIN')";
  private static final int MAX_PAGE = 200;
  private static final List<QuotationStatus> EXPIRING =
      List.of(QuotationStatus.APPROVED, QuotationStatus.SENT_TO_CLIENT);

  private final QuotationService quotations;
  private final QuotationQueryService queries;
  private final QuotationDispatchService dispatch;
  private final QuotationAcceptanceService acceptance;
  private final QuotationDocuments documents;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param quotations quotation commands
   * @param queries quotation reads
   * @param dispatch send to the client
   * @param acceptance acceptance and accounts
   * @param documents PDF and Excel
   * @param currentUser current user
   */
  public QuotationController(
      QuotationService quotations,
      QuotationQueryService queries,
      QuotationDispatchService dispatch,
      QuotationAcceptanceService acceptance,
      QuotationDocuments documents,
      CurrentUser currentUser) {
    this.quotations = quotations;
    this.queries = queries;
    this.dispatch = dispatch;
    this.acceptance = acceptance;
    this.documents = documents;
    this.currentUser = currentUser;
  }

  /**
   * Searches quotations, newest first.
   *
   * @param companyId company
   * @param criteria text and filters
   * @param status statuses
   * @param page page
   * @param size size
   * @return page of quotations
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public PageResponse<QuotationListItem> search(
      @RequestParam Long companyId,
      SearchParams criteria,
      @RequestParam(required = false) List<QuotationStatus> status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    boolean expiring = Boolean.TRUE.equals(criteria.expiring());
    List<QuotationStatus> statuses =
        expiring && (status == null || status.isEmpty()) ? EXPIRING : status;
    QuotationSearch search =
        new QuotationSearch(
            companyId,
            criteria.text(),
            statuses,
            criteria.product(),
            Boolean.TRUE.equals(criteria.mine()) ? currentUser.username() : null,
            expiring ? queries.expiringLimit() : null,
            criteria.clientId());
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    return PageResponse.of(queries.search(search, pageable), QuotationListItem::from);
  }

  /**
   * One quotation with its current content.
   *
   * @param id quotation
   * @return quotation
   */
  @GetMapping("/{id}")
  @PreAuthorize(VIEW)
  public QuotationResponse get(@PathVariable Long id) {
    return reload(id);
  }

  /**
   * The quotation of an ARN (contract for Operations Cashiering, with the direct-payment flag).
   *
   * @param arn Account Reference Number
   * @return summary
   */
  @GetMapping("/by-arn/{arn}")
  @PreAuthorize("hasAnyAuthority('QUOTE_VIEW', 'ACCOUNT_VIEW')")
  public SummaryResponse byArn(@PathVariable String arn) {
    return SummaryResponse.from(queries.getByArn(arn));
  }

  /**
   * Versions of a quotation (BRNB.020).
   *
   * @param id quotation
   * @return versions, oldest first
   */
  @GetMapping("/{id}/versions")
  @PreAuthorize(VIEW)
  public List<QuotationVersionResponse> versions(@PathVariable Long id) {
    return queries.versions(id).stream().map(QuotationVersionResponse::from).toList();
  }

  /**
   * Content of one version.
   *
   * @param id quotation
   * @param versionNo version
   * @return content
   */
  @GetMapping("/{id}/versions/{versionNo}")
  @PreAuthorize(VIEW)
  public VersionContentResponse version(@PathVariable Long id, @PathVariable int versionNo) {
    return VersionContentResponse.from(versionNo, queries.content(id, versionNo));
  }

  /**
   * Differences between two versions.
   *
   * @param id quotation
   * @param from older version
   * @param to newer version
   * @return differences
   */
  @GetMapping("/{id}/diff")
  @PreAuthorize(VIEW)
  public DiffResponse diff(@PathVariable Long id, @RequestParam int from, @RequestParam int to) {
    return DiffResponse.from(queries.diff(id, from, to));
  }

  /**
   * The quotation PDF of the current version (BRNB.043).
   *
   * @param id quotation
   * @return PDF
   */
  @GetMapping("/{id}/document.pdf")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
    return file(documents.pdfFile(queries.get(id)));
  }

  /**
   * The Excel schedule of the current version (BRNB.043).
   *
   * @param id quotation
   * @return XLSX
   */
  @GetMapping("/{id}/document.xlsx")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> xlsx(@PathVariable Long id) {
    return file(documents.xlsxFile(queries.get(id)));
  }

  private static ResponseEntity<byte[]> file(MessageFile f) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(f.mimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(f.fileName()))
        .body(f.content());
  }

  /**
   * Prices a draft without saving it (live premium of the wizard).
   *
   * @param body quotation data
   * @return priced content and TSU routing
   */
  @PostMapping("/preview")
  @PreAuthorize(MAINTAIN)
  public PreviewResponse preview(@Valid @RequestBody QuotationBody body) {
    return PreviewResponse.from(quotations.preview(body.companyId(), body.draft()));
  }

  /**
   * Creates a quotation with its number and ARN.
   *
   * @param body quotation data
   * @return the quotation
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public QuotationResponse create(@Valid @RequestBody QuotationBody body) {
    return reload(quotations.create(body.companyId(), body.draft()));
  }

  /**
   * Changes a draft quotation (a change after submission opens a new version).
   *
   * @param id quotation
   * @param body quotation data
   * @return the quotation
   */
  @PutMapping("/{id}")
  @PreAuthorize(MAINTAIN)
  public QuotationResponse update(@PathVariable Long id, @Valid @RequestBody QuotationBody body) {
    return reload(quotations.update(id, body.draft()));
  }

  /**
   * Submits the quotation for approval.
   *
   * @param id quotation
   * @param body comment
   * @return the quotation
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(MAINTAIN)
  public QuotationResponse submit(@PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    return reload(quotations.submit(id, body.text()));
  }

  /**
   * Approves the quotation (four eyes).
   *
   * @param id quotation
   * @param body comment
   * @return the quotation
   */
  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('QUOTE_APPROVE')")
  public QuotationResponse approve(@PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    return reload(quotations.approve(id, body.text()));
  }

  /**
   * Reopens an approved or sent quotation for changes (new version).
   *
   * @param id quotation
   * @param body comment
   * @return the quotation
   */
  @PostMapping("/{id}/revise")
  @PreAuthorize(MAINTAIN)
  public QuotationResponse revise(@PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    return reload(quotations.revise(id, body.text()));
  }

  /**
   * Records that the client declined.
   *
   * @param id quotation
   * @param body comment
   * @return the quotation
   */
  @PostMapping("/{id}/decline")
  @PreAuthorize(MAINTAIN)
  public QuotationResponse decline(@PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    return reload(quotations.decline(id, body.text()));
  }

  /**
   * Sends the approved quotation to the client, password protected.
   *
   * @param id quotation
   * @param body e-mail
   * @return the quotation
   */
  @PostMapping("/{id}/send")
  @PreAuthorize(MAINTAIN)
  public QuotationResponse send(@PathVariable Long id, @Valid @RequestBody SendRequest body) {
    return reload(dispatch.send(id, body.email()));
  }

  /**
   * Sends several approved quotations, one e-mail per client.
   *
   * @param body quotations and message
   * @return what was sent
   */
  @PostMapping("/batch-send")
  @PreAuthorize(MAINTAIN)
  public BatchSendResponse batchSend(@Valid @RequestBody BatchSendRequest body) {
    return BatchSendResponse.from(dispatch.sendBatch(body.ids(), body.email()));
  }

  /**
   * Records the client's acceptance (the acceptance e-mail must be attached).
   *
   * @param id quotation
   * @param body accepted groups and comment
   * @return the quotation
   */
  @PostMapping("/{id}/accept")
  @PreAuthorize(MAINTAIN)
  public QuotationResponse accept(@PathVariable Long id, @Valid @RequestBody AcceptRequest body) {
    return reload(acceptance.accept(id, body.groups(), body.comment()));
  }

  /**
   * Creates the accounts of the accepted risk groups.
   *
   * @param id quotation
   * @param body comment
   * @return the quotation with its account ARNs
   */
  @PostMapping("/{id}/create-accounts")
  @PreAuthorize("hasAuthority('QUOTE_MAINTAIN') and hasAuthority('ACCOUNT_MAINTAIN')")
  public QuotationResponse createAccounts(
      @PathVariable Long id, @Valid @RequestBody CommentRequest body) {
    return reload(acceptance.createAccounts(id, body.text()));
  }

  private QuotationResponse reload(Quotation quotation) {
    return reload(quotation.getId());
  }

  private QuotationResponse reload(Long id) {
    Quotation q = queries.get(id);
    return QuotationResponse.from(q, queries.content(q));
  }

  /**
   * Criteria of the list, bound from query parameters.
   *
   * @param text quotation number, ARN, client code or name
   * @param product product
   * @param mine only the current user's quotations
   * @param expiring only approved or sent quotations whose validity ends soon
   * @param clientId client
   */
  public record SearchParams(
      String text, String product, Boolean mine, Boolean expiring, Long clientId) {}
}
