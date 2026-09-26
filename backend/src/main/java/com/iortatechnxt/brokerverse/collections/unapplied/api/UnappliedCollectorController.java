package com.iortatechnxt.brokerverse.collections.unapplied.api;

import com.iortatechnxt.brokerverse.collections.unapplied.api.dto.UnappliedCollectorDtos.DisposeRequest;
import com.iortatechnxt.brokerverse.collections.unapplied.api.dto.UnappliedCollectorDtos.DispositionResponse;
import com.iortatechnxt.brokerverse.collections.unapplied.api.dto.UnappliedCollectorDtos.EventResponse;
import com.iortatechnxt.brokerverse.collections.unapplied.api.dto.UnappliedCollectorDtos.FileResponse;
import com.iortatechnxt.brokerverse.collections.unapplied.api.dto.UnappliedCollectorDtos.RequestResponse;
import com.iortatechnxt.brokerverse.collections.unapplied.api.dto.UnappliedCollectorDtos.RuleResponse;
import com.iortatechnxt.brokerverse.collections.unapplied.api.dto.UnappliedCollectorDtos.RulesResponse;
import com.iortatechnxt.brokerverse.collections.unapplied.api.dto.UnappliedCollectorDtos.UnappliedDetailResponse;
import com.iortatechnxt.brokerverse.collections.unapplied.api.dto.UnappliedCollectorDtos.UnappliedRowResponse;
import com.iortatechnxt.brokerverse.collections.unapplied.api.dto.UnappliedQuery;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.ApplicationRequest;
import com.iortatechnxt.brokerverse.collections.unapplied.service.ApplicationFileService;
import com.iortatechnxt.brokerverse.collections.unapplied.service.ApplicationRequestService;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedDispositionService;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedRules;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedWorklistService;
import com.iortatechnxt.brokerverse.collections.worklist.api.ClxAccess;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedView;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The collector view of unapplied payments (BRCLXN.030-048): the list as of today with the
 * collector filters, an item with its dispositions, requests and history, recording a disposition
 * (with the Cashiering request it implies), the status view of the requests, the disposition rules
 * of the form and a manual run of the "For Application To Invoice" file.
 */
@RestController
@RequestMapping("/api/v1/collections/unapplied")
public class UnappliedCollectorController {

  private static final String VIEW = "hasAnyAuthority('CLX_VIEW', 'CLX_UNAPPLIED_WORK')";
  private static final String WORK = "hasAuthority('CLX_UNAPPLIED_WORK')";

  private final UnappliedWorklistService worklist;
  private final UnappliedDispositionService dispositions;
  private final ApplicationRequestService requests;
  private final ApplicationFileService files;
  private final UnappliedRules rules;

  /**
   * Creates the controller.
   *
   * @param worklist collector list
   * @param dispositions collector dispositions
   * @param requests requests to Cashiering
   * @param files application file
   * @param rules disposition rules
   */
  public UnappliedCollectorController(
      UnappliedWorklistService worklist,
      UnappliedDispositionService dispositions,
      ApplicationRequestService requests,
      ApplicationFileService files,
      UnappliedRules rules) {
    this.worklist = worklist;
    this.dispositions = dispositions;
    this.requests = requests;
    this.files = files;
    this.rules = rules;
  }

  /**
   * Open unapplied payments as of today (BRCLXN.034-036).
   *
   * @param companyId company
   * @param query filters
   * @param page page
   * @param size size
   * @return rows, latest payment first
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public PageResponse<UnappliedRowResponse> list(
      @RequestParam Long companyId,
      @ModelAttribute UnappliedQuery query,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        worklist.list(companyId, query.toFilter(), ClxAccess.page(page, size, Sort.unsorted())),
        UnappliedRowResponse::from);
  }

  /**
   * The disposition values of the form and the invoice number pattern (BRCLXN.037/047).
   *
   * @return rules
   */
  @GetMapping("/disposition-rules")
  @PreAuthorize(VIEW)
  public RulesResponse dispositionRules() {
    return new RulesResponse(
        rules.active().stream().map(RuleResponse::from).toList(), rules.invoicePattern());
  }

  /**
   * Requests sent to Cashiering (status view).
   *
   * @param companyId company
   * @param status statuses (default every status)
   * @param q item, invoice, Cashiering reference or requester
   * @param page page
   * @param size size
   * @return requests, newest first
   */
  @GetMapping("/requests")
  @PreAuthorize(VIEW)
  public PageResponse<RequestResponse> requests(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<ApplicationRequest.Status> status,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        requests.list(
            companyId,
            status == null || status.isEmpty()
                ? Arrays.asList(ApplicationRequest.Status.values())
                : status,
            q,
            ClxAccess.page(page, size, Sort.unsorted())),
        RequestResponse::from);
  }

  /**
   * Asks Cashiering again where a request stands.
   *
   * @param id request
   * @return the request
   */
  @PostMapping("/requests/{id}/refresh")
  @PreAuthorize(WORK)
  public RequestResponse refresh(@PathVariable Long id) {
    return RequestResponse.from(requests.refresh(id));
  }

  /**
   * Writes the "For Application To Invoice" file of the requests up to a day now (BRCLXN.041).
   *
   * @param companyId company
   * @param day last day of the requests, default yesterday
   * @return the file
   */
  @PostMapping("/application-file")
  @PreAuthorize("hasAnyAuthority('CLX_SETUP', 'CLX_EXPORT')")
  public FileResponse applicationFile(
      @RequestParam Long companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate day) {
    LocalDate upTo = day == null ? files.previousDay() : day;
    return files
        .publish(companyId, upTo)
        .map(f -> new FileResponse(f.path(), "File written to " + f.path()))
        .orElse(new FileResponse(null, "No application request to list up to " + upTo));
  }

  /**
   * One unapplied payment with its collector dispositions and requests.
   *
   * @param ref Cashiering reference
   * @param companyId company
   * @return item
   */
  @GetMapping("/{ref}")
  @PreAuthorize(VIEW)
  public UnappliedDetailResponse get(@PathVariable String ref, @RequestParam Long companyId) {
    UnappliedView item = dispositions.item(companyId, ref);
    return new UnappliedDetailResponse(
        UnappliedRowResponse.from(worklist.row(companyId, item)),
        dispositions.of(companyId, ref).stream().map(DispositionResponse::from).toList(),
        requests.ofItem(companyId, ref).stream().map(RequestResponse::from).toList());
  }

  /**
   * The history of an unapplied payment (BRCLXN.040).
   *
   * @param ref Cashiering reference
   * @param companyId company
   * @return events, oldest first
   */
  @GetMapping("/{ref}/history")
  @PreAuthorize(VIEW)
  public List<EventResponse> history(@PathVariable String ref, @RequestParam Long companyId) {
    return dispositions.history(companyId, ref).stream().map(EventResponse::from).toList();
  }

  /**
   * Records a collector disposition (BRCLXN.030-033, 047/048).
   *
   * @param ref Cashiering reference
   * @param request disposition
   * @return the disposition
   */
  @PostMapping("/{ref}/dispositions")
  @PreAuthorize(WORK)
  @ResponseStatus(HttpStatus.CREATED)
  public DispositionResponse dispose(
      @PathVariable String ref, @Valid @RequestBody DisposeRequest request) {
    return DispositionResponse.from(
        dispositions.dispose(request.companyId(), ref, request.toInput()));
  }
}
