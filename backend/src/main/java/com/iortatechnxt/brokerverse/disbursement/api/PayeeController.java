package com.iortatechnxt.brokerverse.disbursement.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.disbursement.api.dto.PayeeDtos.AccountRequest;
import com.iortatechnxt.brokerverse.disbursement.api.dto.PayeeDtos.PayeeRequestBody;
import com.iortatechnxt.brokerverse.disbursement.api.dto.PayeeDtos.PayeeRequestResponse;
import com.iortatechnxt.brokerverse.disbursement.api.dto.PayeeDtos.PayeeResponse;
import com.iortatechnxt.brokerverse.disbursement.api.dto.PayeeDtos.PayeeSummary;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeRequestStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeStage;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.service.PayeeQueryService;
import com.iortatechnxt.brokerverse.disbursement.service.PayeeQueryService.PayeeSearch;
import com.iortatechnxt.brokerverse.disbursement.service.PayeeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * The payee master (DIS 2.2.0-2.2.8): the consolidated list (active / inactive), the payee page
 * with its bank accounts (numbers masked without {@code DISB_PAYEE_VIEW_FULL}), maintenance with
 * maker-checker (draft, submit, authorise, deactivate, reactivate, delete an unused draft) and the
 * payee maintenance requests (DIS 2.2.1). Returns run through the workflow panel.
 */
@RestController
@RequestMapping("/api/v1/disbursement")
public class PayeeController {

  private final PayeeService payees;
  private final PayeeQueryService queries;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param payees payee maintenance
   * @param queries payee reads
   * @param currentUser current user
   */
  public PayeeController(PayeeService payees, PayeeQueryService queries, CurrentUser currentUser) {
    this.payees = payees;
    this.queries = queries;
    this.currentUser = currentUser;
  }

  private boolean full() {
    return currentUser.hasAuthority(DisbursementAccess.VIEW_FULL);
  }

  private PayeeResponse page(Long id) {
    return PayeeResponse.from(queries.get(id), full());
  }

  /**
   * Payees (DIS 2.2.8).
   *
   * @param companyId company
   * @param stage stages
   * @param payeeClass class
   * @param q code or name
   * @param page page
   * @param size size
   * @return payees
   */
  @GetMapping("/payees")
  @PreAuthorize(DisbursementAccess.PAYEE_READ)
  public PageResponse<PayeeSummary> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) List<PayeeStage> stage,
      @RequestParam(required = false) String payeeClass,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    boolean full = full();
    return PageResponse.of(
        queries.search(
            new PayeeSearch(companyId, stage, payeeClass, q),
            DisbursementAccess.newestFirst(page, size)),
        p -> PayeeSummary.from(p, full));
  }

  /**
   * One payee.
   *
   * @param id payee
   * @return payee
   */
  @GetMapping("/payees/{id}")
  @PreAuthorize(DisbursementAccess.PAYEE_READ)
  public PayeeResponse get(@PathVariable Long id) {
    return page(id);
  }

  /**
   * Saves a new payee as a draft (DIS 2.2.0, 2.2.6-2.2.7).
   *
   * @param body payee
   * @return payee
   */
  @PostMapping("/payees")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(DisbursementAccess.PAYEE_MAINTAIN)
  public PayeeResponse create(@Valid @RequestBody PayeeRequestBody body) {
    if (body.companyId() == null || body.payeeCode() == null || body.payeeCode().isBlank()) {
      throw new BusinessRuleException("PAYEE_CODE", "Company and payee code are required");
    }
    Payee p =
        payees.create(
            body.companyId(),
            body.payeeCode(),
            body.details(),
            body.accountDetails(),
            PayeeSource.MANUAL);
    return page(p.getId());
  }

  /**
   * Updates a draft or amends an active payee (DIS 2.2.3).
   *
   * @param id payee
   * @param body details
   * @return payee
   */
  @PutMapping("/payees/{id}")
  @PreAuthorize(DisbursementAccess.PAYEE_MAINTAIN)
  public PayeeResponse update(@PathVariable Long id, @Valid @RequestBody PayeeRequestBody body) {
    payees.update(id, body.details());
    return page(id);
  }

  /**
   * Adds a bank account.
   *
   * @param id payee
   * @param body account
   * @return payee
   */
  @PostMapping("/payees/{id}/accounts")
  @PreAuthorize(DisbursementAccess.PAYEE_MAINTAIN)
  public PayeeResponse addAccount(@PathVariable Long id, @Valid @RequestBody AccountRequest body) {
    payees.addAccount(id, body.details());
    return page(id);
  }

  /**
   * Deactivates a bank account.
   *
   * @param id payee
   * @param accountId account
   * @return payee
   */
  @PostMapping("/payees/{id}/accounts/{accountId}/deactivate")
  @PreAuthorize(DisbursementAccess.PAYEE_MAINTAIN)
  public PayeeResponse deactivateAccount(@PathVariable Long id, @PathVariable Long accountId) {
    payees.deactivateAccount(id, accountId);
    return page(id);
  }

  /**
   * Submits a draft for authorisation.
   *
   * @param id payee
   * @return payee
   */
  @PostMapping("/payees/{id}/submit")
  @PreAuthorize(DisbursementAccess.PAYEE_MAINTAIN)
  public PayeeResponse submit(@PathVariable Long id) {
    payees.submit(id);
    return page(id);
  }

  /**
   * Authorises the pending change (four eyes).
   *
   * @param id payee
   * @return payee
   */
  @PostMapping("/payees/{id}/authorize")
  @PreAuthorize(DisbursementAccess.PAYEE_AUTHORIZE)
  public PayeeResponse authorize(@PathVariable Long id) {
    payees.authorize(id);
    return page(id);
  }

  /**
   * Requests the deactivation (DIS 2.2.4).
   *
   * @param id payee
   * @return payee
   */
  @PostMapping("/payees/{id}/deactivate")
  @PreAuthorize(DisbursementAccess.PAYEE_MAINTAIN)
  public PayeeResponse deactivate(@PathVariable Long id) {
    payees.deactivate(id);
    return page(id);
  }

  /**
   * Requests the reactivation.
   *
   * @param id payee
   * @return payee
   */
  @PostMapping("/payees/{id}/reactivate")
  @PreAuthorize(DisbursementAccess.PAYEE_MAINTAIN)
  public PayeeResponse reactivate(@PathVariable Long id) {
    payees.reactivate(id);
    return page(id);
  }

  /**
   * Deletes a draft never used (DIS 2.2.4).
   *
   * @param id payee
   */
  @DeleteMapping("/payees/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize(DisbursementAccess.PAYEE_MAINTAIN)
  public void delete(@PathVariable Long id) {
    payees.delete(id);
  }

  /**
   * Payee maintenance requests (DIS 2.2.1).
   *
   * @param companyId company
   * @param status status
   * @param page page
   * @param size size
   * @return requests
   */
  @GetMapping("/payee-requests")
  @PreAuthorize(DisbursementAccess.PAYEE_READ)
  public PageResponse<PayeeRequestResponse> requests(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "OPEN") PayeeRequestStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.requests(companyId, status, DisbursementAccess.plain(page, size)),
        PayeeRequestResponse::from);
  }

  /**
   * Closes a payee request without a payee.
   *
   * @param requestId request
   * @return request
   */
  @PostMapping("/payee-requests/{requestId}/close")
  @PreAuthorize(DisbursementAccess.PAYEE_MAINTAIN)
  public PayeeRequestResponse close(@PathVariable Long requestId) {
    return PayeeRequestResponse.from(payees.closeRequest(requestId, PayeeRequestStatus.CANCELLED));
  }
}
