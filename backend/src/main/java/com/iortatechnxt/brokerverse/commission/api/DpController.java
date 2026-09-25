package com.iortatechnxt.brokerverse.commission.api;

import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.AnswersRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.BillingResponse;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.CollectRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.CommentRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.DpItemResponse;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.DpListResponse;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.ExcludeRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.IdsRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.PrepareRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.ReinstateRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.SendRequest;
import com.iortatechnxt.brokerverse.commission.api.dto.DpDtos.SubmissionResponse;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.DpTag;
import com.iortatechnxt.brokerverse.commission.domain.CommissionEnums.Sanitation;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.service.DpBillingSender;
import com.iortatechnxt.brokerverse.commission.service.DpBillingService;
import com.iortatechnxt.brokerverse.commission.service.DpCollectionService;
import com.iortatechnxt.brokerverse.commission.service.DpFeedbackService;
import com.iortatechnxt.brokerverse.commission.service.DpFeedbackService.Answer;
import com.iortatechnxt.brokerverse.commission.service.DpIntakeService;
import com.iortatechnxt.brokerverse.commission.service.DpItemFilter;
import com.iortatechnxt.brokerverse.commission.service.DpListService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Direct payment commission (CMRID.001-013, MKTID.012): DP list intake and submission tracker,
 * validation and sanitation list with confirmation and exclusion, billings per insurer with
 * sending, insurer answers, collection with the commission OR and the premium receivable reversal
 * and reinstatement.
 */
@RestController
@RequestMapping("/api/v1/commission/dp")
public class DpController {

  private static final int ITEM_PAGE = 50;

  private final DpListService lists;
  private final DpIntakeService intake;
  private final DpBillingService billings;
  private final DpBillingSender sender;
  private final DpFeedbackService feedback;
  private final DpCollectionService collection;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param lists DP lists
   * @param intake DP accounts
   * @param billings billings
   * @param sender billing sending
   * @param feedback insurer answers
   * @param collection collection and PR reversal
   * @param clock clock
   */
  public DpController(
      DpListService lists,
      DpIntakeService intake,
      DpBillingService billings,
      DpBillingSender sender,
      DpFeedbackService feedback,
      DpCollectionService collection,
      Clock clock) {
    this.lists = lists;
    this.intake = intake;
    this.billings = billings;
    this.sender = sender;
    this.feedback = feedback;
    this.collection = collection;
    this.clock = clock;
  }

  /**
   * DP lists received in a period (CMRID.001).
   *
   * @param companyId company
   * @param from first submission date
   * @param to last submission date
   * @param page page
   * @param size size
   * @return lists
   */
  @GetMapping("/lists")
  @PreAuthorize(CommissionAccess.READ)
  public PageResponse<DpListResponse> lists(
      @RequestParam Long companyId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    LocalDate today = LocalDate.now(clock);
    return PageResponse.of(
        lists.lists(
            companyId,
            from == null ? today.minusYears(1) : from,
            to == null ? today : to,
            CommissionAccess.page(page, size)),
        DpListResponse::from);
  }

  /**
   * The submission tracker per branch (CMRID.001).
   *
   * @param companyId company
   * @param from first submission date
   * @param to last submission date
   * @return one row per branch
   */
  @GetMapping("/submissions")
  @PreAuthorize(CommissionAccess.READ)
  public List<SubmissionResponse> submissions(
      @RequestParam Long companyId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
    return lists.tracker(companyId, from, to).stream().map(SubmissionResponse::from).toList();
  }

  /**
   * Uploads a DP list named {@code <Branch>_DP_<yyyyMMdd>} (CMRID.001).
   *
   * @param companyId company
   * @param file file
   * @return the list
   * @throws IOException when the upload cannot be read
   */
  @PostMapping(value = "/lists", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize(CommissionAccess.PROCESS)
  public DpListResponse upload(@RequestParam Long companyId, @RequestParam MultipartFile file)
      throws IOException {
    return DpListResponse.from(
        lists.upload(companyId, file.getOriginalFilename(), file.getBytes()));
  }

  /**
   * Takes in the lists waiting in the Collection system (OQ38).
   *
   * @param companyId company
   * @return the lists created
   */
  @PostMapping("/lists/pull")
  @PreAuthorize(CommissionAccess.PROCESS)
  public List<DpListResponse> pull(@RequestParam Long companyId) {
    return lists.pull(companyId).stream().map(DpListResponse::from).toList();
  }

  /**
   * DP accounts (validation and sanitation list, CMRID.008/013).
   *
   * @param companyId company
   * @param tag tags
   * @param sanitation sanitation
   * @param insurer insurer
   * @param listId list
   * @param q search text
   * @param page page and size
   * @return accounts
   */
  @GetMapping("/items")
  @PreAuthorize(CommissionAccess.READ)
  public PageResponse<DpItemResponse> items(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<DpTag> tag,
      @RequestParam(required = false) Sanitation sanitation,
      @RequestParam(required = false) String insurer,
      @RequestParam(required = false) Long listId,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page) {
    return PageResponse.of(
        intake.items(
            companyId,
            new DpItemFilter(tag, sanitation, insurer, listId, null, q),
            PageRequest.of(Math.max(page, 0), ITEM_PAGE, Sort.by("id"))),
        DpItemResponse::from);
  }

  /**
   * Confirms accounts as fully paid to the insurer (CMRID.013).
   *
   * @param request accounts
   * @return accounts
   */
  @PostMapping("/items/confirm")
  @PreAuthorize(CommissionAccess.PROCESS)
  public List<DpItemResponse> confirm(@Valid @RequestBody IdsRequest request) {
    return intake.confirm(request.ids()).stream().map(DpItemResponse::from).toList();
  }

  /**
   * Excludes accounts by hand.
   *
   * @param request accounts and reason
   * @return accounts
   */
  @PostMapping("/items/exclude")
  @PreAuthorize(CommissionAccess.PROCESS)
  public List<DpItemResponse> exclude(@Valid @RequestBody ExcludeRequest request) {
    return intake.exclude(request.ids(), request.reason()).stream()
        .map(DpItemResponse::from)
        .toList();
  }

  /**
   * Validates an account again.
   *
   * @param id account
   * @return account
   */
  @PostMapping("/items/{id}/revalidate")
  @PreAuthorize(CommissionAccess.PROCESS)
  public DpItemResponse revalidate(@PathVariable Long id) {
    return DpItemResponse.from(intake.revalidate(id));
  }

  /**
   * Reverses again the premium receivable of a reinstated account (MKTID.012).
   *
   * @param id account
   * @return account
   */
  @PostMapping("/items/{id}/reverse")
  @PreAuthorize(CommissionAccess.PROCESS)
  public DpItemResponse reverse(@PathVariable Long id) {
    return DpItemResponse.from(collection.reverse(id));
  }

  /**
   * Reinstates the premium receivable of an account (CSHID.004 b).
   *
   * @param id account
   * @param request reason
   * @return account
   */
  @PostMapping("/items/{id}/reinstate")
  @PreAuthorize(CommissionAccess.PROCESS)
  public DpItemResponse reinstate(
      @PathVariable Long id, @Valid @RequestBody ReinstateRequest request) {
    return DpItemResponse.from(collection.reinstate(id, request.reasonCode(), request.comment()));
  }

  /**
   * Sorts the accounts for billing by insurer into billings (CMRID.009).
   *
   * @param request company and accounts
   * @return billings
   */
  @PostMapping("/billings")
  @PreAuthorize(CommissionAccess.PROCESS)
  public List<BillingResponse> prepare(@Valid @RequestBody PrepareRequest request) {
    return billings.prepare(request.companyId(), request.ids()).stream().map(this::view).toList();
  }

  /**
   * Billings of a company.
   *
   * @param companyId company
   * @param stage stage
   * @param insurer insurer
   * @param page page
   * @param size size
   * @return billings
   */
  @GetMapping("/billings")
  @PreAuthorize(CommissionAccess.READ)
  public PageResponse<BillingResponse> billings(
      @RequestParam Long companyId,
      @RequestParam(required = false) String stage,
      @RequestParam(required = false) String insurer,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        billings.search(companyId, blank(stage), blank(insurer), CommissionAccess.page(page, size)),
        this::view);
  }

  /**
   * A billing.
   *
   * @param id billing
   * @return billing
   */
  @GetMapping("/billings/{id}")
  @PreAuthorize(CommissionAccess.READ)
  public BillingResponse billing(@PathVariable Long id) {
    return view(billings.require(id));
  }

  /**
   * Accounts of a billing.
   *
   * @param id billing
   * @return accounts
   */
  @GetMapping("/billings/{id}/items")
  @PreAuthorize(CommissionAccess.READ)
  public List<DpItemResponse> billingItems(@PathVariable Long id) {
    return billings.itemsOf(id).stream().map(DpItemResponse::from).toList();
  }

  /**
   * Sends a billing to its insurer (CMRID.009/011/012).
   *
   * @param id billing
   * @param request recipients
   * @return billing
   */
  @PostMapping("/billings/{id}/send")
  @PreAuthorize(CommissionAccess.PROCESS)
  public BillingResponse send(@PathVariable Long id, @Valid @RequestBody SendRequest request) {
    return view(sender.send(id, request.to(), request.cc()));
  }

  /**
   * Cancels a billing not yet sent.
   *
   * @param id billing
   * @param request comment
   * @return billing
   */
  @PostMapping("/billings/{id}/cancel")
  @PreAuthorize(CommissionAccess.PROCESS)
  public BillingResponse cancel(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return view(billings.cancel(id, request.comment()));
  }

  /**
   * Records the insurer's answers (CMRID.009).
   *
   * @param id billing
   * @param request answers
   * @return billing
   */
  @PostMapping("/billings/{id}/answers")
  @PreAuthorize(CommissionAccess.PROCESS)
  public BillingResponse answer(@PathVariable Long id, @Valid @RequestBody AnswersRequest request) {
    return view(
        feedback.answer(
            id,
            request.answers().stream()
                .map(a -> new Answer(a.invoiceNo(), a.approved(), a.reason(), a.comment()))
                .toList()));
  }

  /**
   * Collects the commission of a billing and reverses the premium receivable (CMRID.010,
   * MKTID.012).
   *
   * @param id billing
   * @param request date, bank and certificate
   * @return billing
   */
  @PostMapping("/billings/{id}/collect")
  @PreAuthorize(CommissionAccess.PROCESS)
  public BillingResponse collect(
      @PathVariable Long id, @Valid @RequestBody CollectRequest request) {
    return view(
        collection.collect(
            id,
            new DpCollectionService.CollectRequest(
                request.receiptDate(), request.bankAccount(), request.certificateRef())));
  }

  /**
   * Counts of the accounts per tag of a company.
   *
   * @param companyId company
   * @return tag to count
   */
  @GetMapping("/items/counts")
  @PreAuthorize(CommissionAccess.READ)
  public Map<DpTag, Long> counts(@RequestParam Long companyId) {
    return intake.countsByTag(companyId);
  }

  private BillingResponse view(DpBilling b) {
    return BillingResponse.from(b, LocalDate.now(clock));
  }

  private static String blank(String v) {
    return v == null || v.isBlank() ? null : v.strip();
  }
}
