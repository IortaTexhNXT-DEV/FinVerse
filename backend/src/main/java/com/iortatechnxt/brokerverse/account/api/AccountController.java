package com.iortatechnxt.brokerverse.account.api;

import com.iortatechnxt.brokerverse.account.api.dto.AccountCheckResponse;
import com.iortatechnxt.brokerverse.account.api.dto.AccountRequest;
import com.iortatechnxt.brokerverse.account.api.dto.AccountResponse;
import com.iortatechnxt.brokerverse.account.api.dto.AccountSummaryResponse;
import com.iortatechnxt.brokerverse.account.api.dto.CommentRequest;
import com.iortatechnxt.brokerverse.account.api.dto.FfyCancelRequest;
import com.iortatechnxt.brokerverse.account.api.dto.FfyRequest;
import com.iortatechnxt.brokerverse.account.api.dto.PaymentArrangementRequest;
import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountSearch;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.AccountTaggingService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
 * Accounts (BRNB.050-054/102/111/113/114): search, detail, readiness check, create and update, and
 * the business actions of the NB_ACCOUNT workflow. Generic actions (return, void) are run through
 * the workflow API by the workflow panel.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

  private static final String VIEW = "hasAuthority('ACCOUNT_VIEW')";
  private static final String MAINTAIN = "hasAuthority('ACCOUNT_MAINTAIN')";
  private static final int MAX_PAGE = 200;

  private final AccountService accounts;
  private final AccountQueryService queries;
  private final AccountTaggingService tagging;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param accounts account commands
   * @param queries account reads
   * @param tagging FFY and payment arrangement
   * @param currentUser current user
   */
  public AccountController(
      AccountService accounts,
      AccountQueryService queries,
      AccountTaggingService tagging,
      CurrentUser currentUser) {
    this.accounts = accounts;
    this.queries = queries;
    this.tagging = tagging;
    this.currentUser = currentUser;
  }

  /**
   * Searches accounts (BRNB.050); voided accounts are excluded unless asked for.
   *
   * @param companyId company
   * @param criteria text and filters
   * @param status statuses
   * @param periodFrom period start on or after
   * @param periodTo period start on or before
   * @param page page
   * @param size size
   * @return page of accounts, newest first
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public PageResponse<AccountSummaryResponse> search(
      @RequestParam Long companyId,
      SearchParams criteria,
      @RequestParam(required = false) List<AccountStatus> status,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate periodFrom,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate periodTo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    String officer =
        Boolean.TRUE.equals(criteria.mine()) ? currentUser.username() : criteria.officer();
    AccountSearch search =
        new AccountSearch(
            companyId,
            criteria.text(),
            criteria.pn(),
            criteria.vehicle(),
            criteria.location(),
            criteria.product(),
            criteria.line(),
            criteria.insurer(),
            status,
            criteria.ffy(),
            criteria.directPayment(),
            periodFrom,
            periodTo,
            officer,
            Boolean.TRUE.equals(criteria.includeVoided()),
            criteria.businessType());
    PageRequest pageable =
        PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE),
            Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    return PageResponse.of(queries.search(search, pageable), AccountSummaryResponse::from);
  }

  /**
   * One account.
   *
   * @param id account
   * @return account
   */
  @GetMapping("/{id}")
  @PreAuthorize(VIEW)
  public AccountResponse get(@PathVariable Long id) {
    return AccountResponse.from(queries.get(id));
  }

  /**
   * An account by ARN.
   *
   * @param arn Account Reference Number
   * @return account
   */
  @GetMapping("/by-arn/{arn}")
  @PreAuthorize(VIEW)
  public AccountResponse byArn(@PathVariable String arn) {
    return AccountResponse.from(queries.requireByArn(arn));
  }

  /**
   * Readiness of an account: missing fields and documents, duplicates, premium, TSU.
   *
   * @param id account
   * @return check
   */
  @GetMapping("/{id}/check")
  @PreAuthorize(VIEW)
  public AccountCheckResponse check(@PathVariable Long id) {
    return AccountCheckResponse.from(queries.check(id));
  }

  /**
   * Creates a draft account with a new ARN.
   *
   * @param request account data
   * @return the account
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MAINTAIN)
  public AccountResponse create(@Valid @RequestBody AccountRequest request) {
    Account created = accounts.createDraft(NewAccount.direct(request.companyId(), request.draft()));
    return reload(created);
  }

  /**
   * Changes a draft or returned account (Marketing) or a submitted one (Processing).
   *
   * @param id account
   * @param request account data
   * @return the account
   */
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ACCOUNT_MAINTAIN', 'ACCOUNT_PROCESS')")
  public AccountResponse update(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
    return reload(accounts.update(id, request.draft()));
  }

  /**
   * Submits a draft to Processing.
   *
   * @param id account
   * @param request comment
   * @return the account
   */
  @PostMapping("/{id}/submit")
  @PreAuthorize(MAINTAIN)
  public AccountResponse submit(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return reload(accounts.submit(id, request.text()));
  }

  /**
   * Resubmits a returned account.
   *
   * @param id account
   * @param request comment
   * @return the account
   */
  @PostMapping("/{id}/resubmit")
  @PreAuthorize(MAINTAIN)
  public AccountResponse resubmit(
      @PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return reload(accounts.resubmit(id, request.text()));
  }

  /**
   * Validates a submitted account (Processing).
   *
   * @param id account
   * @param request comment
   * @return the account
   */
  @PostMapping("/{id}/validate")
  @PreAuthorize("hasAuthority('ACCOUNT_PROCESS')")
  public AccountResponse validate(
      @PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return reload(accounts.validate(id, request.text()));
  }

  /**
   * Books directly an account whose policy is already issued (BRNB.111).
   *
   * @param id account
   * @param request comment
   * @return the account
   */
  @PostMapping("/{id}/direct-booking")
  @PreAuthorize("hasAnyAuthority('ACCOUNT_PROCESS', 'PLACEMENT_MANAGE')")
  public AccountResponse directBooking(
      @PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return reload(accounts.directBooking(id, request.text()));
  }

  /**
   * Records the TSU clearance (BRNB.098).
   *
   * @param id account
   * @param request comment
   * @return the account
   */
  @PostMapping("/{id}/tsu-clearance")
  @PreAuthorize("hasAuthority('TSU_PROCESS')")
  public AccountResponse clearTsu(
      @PathVariable Long id, @Valid @RequestBody CommentRequest request) {
    return reload(accounts.clearTsu(id, request.text()));
  }

  /**
   * Tags the account Free First Year or changes its dates (BRNB.113).
   *
   * @param id account
   * @param request start
   * @return the account
   */
  @PutMapping("/{id}/ffy")
  @PreAuthorize(MAINTAIN)
  public AccountResponse tagFfy(@PathVariable Long id, @Valid @RequestBody FfyRequest request) {
    return reload(tagging.tagFreeFirstYear(id, request.start()));
  }

  /**
   * Cancels the Free First Year tag.
   *
   * @param id account
   * @param request reason
   * @return the account
   */
  @PostMapping("/{id}/ffy/cancel")
  @PreAuthorize(MAINTAIN)
  public AccountResponse cancelFfy(
      @PathVariable Long id, @Valid @RequestBody FfyCancelRequest request) {
    return reload(tagging.cancelFreeFirstYear(id, request.reasonCode(), request.comment()));
  }

  /**
   * Sets the payment arrangement (BRNB.114).
   *
   * @param id account
   * @param request arrangement
   * @return the account
   */
  @PutMapping("/{id}/payment-arrangement")
  @PreAuthorize(MAINTAIN)
  public AccountResponse paymentArrangement(
      @PathVariable Long id, @Valid @RequestBody PaymentArrangementRequest request) {
    return reload(tagging.setPaymentArrangement(id, request.arrangement()));
  }

  private AccountResponse reload(Account account) {
    return AccountResponse.from(queries.get(account.getId()));
  }

  /**
   * Text criteria of the search, bound from query parameters.
   *
   * @param text ARN, client code or name
   * @param pn promissory note number
   * @param vehicle plate, conduction sticker, engine or chassis number
   * @param location location of risk
   * @param product product
   * @param line product line
   * @param insurer insurer
   * @param ffy Free First Year tagged
   * @param directPayment direct payment
   * @param officer account officer
   * @param mine only the current user's accounts
   * @param includeVoided include voided accounts
   * @param businessType New Business or Renewal (BT0), null for both
   */
  public record SearchParams(
      String text,
      String pn,
      String vehicle,
      String location,
      String product,
      String line,
      String insurer,
      Boolean ffy,
      Boolean directPayment,
      String officer,
      Boolean mine,
      Boolean includeVoided,
      BusinessType businessType) {}
}
