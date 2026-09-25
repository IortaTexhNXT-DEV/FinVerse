package com.iortatechnxt.brokerverse.collections.worklist.api;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.common.service.ChangeRecorder;
import com.iortatechnxt.brokerverse.collections.common.service.CollectionItems;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AccountDtos.AccountResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AccountDtos.AssignmentResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AccountDtos.FieldChangeResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AccountDtos.LockResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AccountDtos.PaymentsResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.AccountDtos.PolicyResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.ItemResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.WorklistDtos.ClientViewResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.WorklistDtos.GroupTotalResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.WorklistDtos.RefreshResponse;
import com.iortatechnxt.brokerverse.collections.worklist.api.dto.WorklistQuery;
import com.iortatechnxt.brokerverse.collections.worklist.service.AccountViewService;
import com.iortatechnxt.brokerverse.collections.worklist.service.AssignmentService;
import com.iortatechnxt.brokerverse.collections.worklist.service.EditLockService;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistQueryService;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistQueryService.GroupBy;
import com.iortatechnxt.brokerverse.collections.worklist.service.WorklistRefreshService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The PR worklist and the collection account (BRCLXN.001-015, 043, 046, 052-057): the filtered
 * worklist with totals per client or account, the account page (header and live breakdown,
 * payments, policy and co-insurance, assignments, field history), the client view, the edit lock
 * and a refresh on demand.
 */
@RestController
@RequestMapping("/api/v1/collections")
public class WorklistController {

  private static final Map<String, String> SORTS =
      Map.of(
          "agingDays", "figures.agingDays",
          "netOutstanding", "figures.netOutstanding",
          "bookingDate", "classification.bookingDate",
          "invoiceNo", "invoiceNo",
          "assuredName", "parties.assuredName",
          "handler", "currentHandler");

  private final WorklistQueryService worklist;
  private final AccountViewService accounts;
  private final EditLockService locks;
  private final AssignmentService assignments;
  private final CollectionItems items;
  private final ChangeRecorder changes;
  private final WorklistRefreshService refresh;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param worklist worklist reads
   * @param accounts account page
   * @param locks edit lock
   * @param assignments assignment history and handlers
   * @param items items
   * @param changes field history
   * @param refresh refresh
   * @param currentUser signed-in user
   * @param clock clock
   */
  public WorklistController(
      WorklistQueryService worklist,
      AccountViewService accounts,
      EditLockService locks,
      AssignmentService assignments,
      CollectionItems items,
      ChangeRecorder changes,
      WorklistRefreshService refresh,
      CurrentUser currentUser,
      Clock clock) {
    this.worklist = worklist;
    this.accounts = accounts;
    this.locks = locks;
    this.assignments = assignments;
    this.items = items;
    this.changes = changes;
    this.refresh = refresh;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The worklist (BRCLXN.001-012).
   *
   * @param companyId company
   * @param query filters
   * @param sort sort key (agingDays, netOutstanding, bookingDate, invoiceNo, assuredName, handler)
   * @param direction asc or desc
   * @param page page
   * @param size size
   * @return items
   */
  @GetMapping("/worklist")
  @PreAuthorize(ClxAccess.VIEW)
  public PageResponse<ItemResponse> worklist(
      @RequestParam Long companyId,
      @ModelAttribute WorklistQuery query,
      @RequestParam(defaultValue = "agingDays") String sort,
      @RequestParam(defaultValue = "desc") String direction,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Sort order =
        Sort.by(
                Sort.Direction.fromOptionalString(direction).orElse(Sort.Direction.DESC),
                SORTS.getOrDefault(sort, SORTS.get("agingDays")))
            .and(Sort.by("id"));
    return PageResponse.of(
        worklist.search(
            companyId, query.toFilter(currentUser.username()), ClxAccess.page(page, size, order)),
        ItemResponse::from);
  }

  /**
   * Totals per client or account for the filters (BRCLXN.003).
   *
   * @param companyId company
   * @param query filters
   * @param groupBy CLIENT or ARN
   * @return groups, largest first
   */
  @GetMapping("/worklist/totals")
  @PreAuthorize(ClxAccess.VIEW)
  public List<GroupTotalResponse> totals(
      @RequestParam Long companyId,
      @ModelAttribute WorklistQuery query,
      @RequestParam(defaultValue = "CLIENT") GroupBy groupBy) {
    return worklist.totals(companyId, query.toFilter(currentUser.username()), groupBy).stream()
        .map(GroupTotalResponse::from)
        .toList();
  }

  /**
   * The account header, live breakdown and edit lock.
   *
   * @param invoiceNo invoice
   * @return account
   */
  @GetMapping("/items/{invoiceNo}")
  @PreAuthorize(ClxAccess.VIEW)
  public AccountResponse account(@PathVariable String invoiceNo) {
    return AccountResponse.from(accounts.account(invoiceNo));
  }

  /**
   * Payments of the account (BRCLXN.054).
   *
   * @param invoiceNo invoice
   * @return payments
   */
  @GetMapping("/items/{invoiceNo}/payments")
  @PreAuthorize(ClxAccess.VIEW)
  public PaymentsResponse payments(@PathVariable String invoiceNo) {
    return PaymentsResponse.from(accounts.payments(invoiceNo));
  }

  /**
   * Policy, invoice family and co-insurance (BRCLXN.056).
   *
   * @param invoiceNo invoice
   * @return policy view
   */
  @GetMapping("/items/{invoiceNo}/policy")
  @PreAuthorize(ClxAccess.VIEW)
  public PolicyResponse policy(@PathVariable String invoiceNo) {
    return PolicyResponse.from(accounts.policy(invoiceNo));
  }

  /**
   * Assignment history of the account (BRCLXN.052).
   *
   * @param invoiceNo invoice
   * @return assignments, newest first
   */
  @GetMapping("/items/{invoiceNo}/assignments")
  @PreAuthorize(ClxAccess.VIEW)
  public List<AssignmentResponse> assignments(@PathVariable String invoiceNo) {
    return assignments.history(items.require(invoiceNo).getId()).stream()
        .map(AssignmentResponse::from)
        .toList();
  }

  /**
   * Field changes of the account (BRCLXN.043).
   *
   * @param invoiceNo invoice
   * @param page page
   * @param size size
   * @return changes, newest first
   */
  @GetMapping("/items/{invoiceNo}/history")
  @PreAuthorize(ClxAccess.VIEW)
  public PageResponse<FieldChangeResponse> history(
      @PathVariable String invoiceNo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    CollectionItem item = items.require(invoiceNo);
    return PageResponse.of(
        changes.ofItem(item.getId(), ClxAccess.page(page, size, Sort.unsorted())),
        FieldChangeResponse::from);
  }

  /**
   * Takes or refreshes the edit lock ("&lt;user&gt; is editing").
   *
   * @param invoiceNo invoice
   * @return lock
   */
  @PostMapping("/items/{invoiceNo}/lock")
  @PreAuthorize(ClxAccess.WORK)
  public LockResponse lock(@PathVariable String invoiceNo) {
    return LockResponse.from(locks.acquire(invoiceNo));
  }

  /**
   * Releases the edit lock.
   *
   * @param invoiceNo invoice
   * @return lock
   */
  @DeleteMapping("/items/{invoiceNo}/lock")
  @PreAuthorize(ClxAccess.WORK)
  public LockResponse unlock(@PathVariable String invoiceNo) {
    return LockResponse.from(locks.release(invoiceNo));
  }

  /**
   * The client view: every account of a client with totals (BRCLXN.003).
   *
   * @param companyId company
   * @param clientCode client
   * @return accounts and totals
   */
  @GetMapping("/clients/{clientCode}")
  @PreAuthorize(ClxAccess.VIEW)
  public ClientViewResponse client(@RequestParam Long companyId, @PathVariable String clientCode) {
    return ClientViewResponse.from(clientCode, items.ofClient(companyId, clientCode));
  }

  /**
   * Users who may handle collection accounts (pick lists).
   *
   * @return user names
   */
  @GetMapping("/handlers")
  @PreAuthorize(ClxAccess.HANDLERS)
  public List<String> handlers() {
    return assignments.handlers();
  }

  /**
   * Refreshes one account from the ledger now (BRCLXN.015).
   *
   * @param invoiceNo invoice
   * @return the account
   */
  @PostMapping("/items/{invoiceNo}/refresh")
  @PreAuthorize(ClxAccess.WORK)
  public ItemResponse refreshOne(@PathVariable String invoiceNo) {
    return refresh
        .refreshInvoice(invoiceNo)
        .map(ItemResponse::from)
        .orElseThrow(() -> new ResourceNotFoundException(CollectionItems.ENTITY, invoiceNo));
  }

  /**
   * Refreshes the worklist of a company now (Collections Setup; the job runs nightly).
   *
   * @param companyId company
   * @return counts
   */
  @PostMapping("/refresh")
  @PreAuthorize(ClxAccess.SETUP)
  public RefreshResponse refresh(@RequestParam Long companyId) {
    return RefreshResponse.from(refresh.refreshAll(companyId, LocalDate.now(clock)));
  }
}
