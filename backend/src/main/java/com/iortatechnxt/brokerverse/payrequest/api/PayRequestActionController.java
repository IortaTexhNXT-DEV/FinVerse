package com.iortatechnxt.brokerverse.payrequest.api;

import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestInputs.AssignInput;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestInputs.CommentInput;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestInputs.LiquidationAccountInput;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestInputs.LiquidationInput;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestInputs.ValidationResultInput;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestViews.AccountView;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestViews.LiquidationView;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestViews.RequestView;
import com.iortatechnxt.brokerverse.payrequest.api.dto.PayRequestViews.ValidationView;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.service.LiquidationService;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestWorkflowService;
import com.iortatechnxt.brokerverse.payrequest.service.RefundValidationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Actions on requests (MKT 1.9.0-1.16.3, 1.11.0, 2.24.0): assign, submit, endorse, approve, enter a
 * handed-over validation result; the liquidation of a cash advance (Appendix D, AQ18) and the
 * accounts of its event roles. Returns and cancellations are generic workflow actions.
 */
@RestController
@RequestMapping("/api/v1/payment-requests")
public class PayRequestActionController {

  private static final String REQUEST = "/requests/{id}";
  private static final String LIQUIDATION = REQUEST + "/liquidation";

  private final PayRequestWorkflowService workflow;
  private final RefundValidationService validations;
  private final LiquidationService liquidations;

  /**
   * Creates the controller.
   *
   * @param workflow business steps
   * @param validations validation tasks
   * @param liquidations liquidations
   */
  public PayRequestActionController(
      PayRequestWorkflowService workflow,
      RefundValidationService validations,
      LiquidationService liquidations) {
    this.workflow = workflow;
    this.validations = validations;
    this.liquidations = liquidations;
  }

  /**
   * Assigns a refund to a preparer (MKT 1.9.0).
   *
   * @param id request
   * @param input preparer and comment
   * @return the request
   */
  @PostMapping(REQUEST + "/assign")
  @PreAuthorize(PayRequestAccess.ASSIGN)
  public RequestView assign(@PathVariable Long id, @Valid @RequestBody AssignInput input) {
    return view(workflow.assign(id, input.username().strip(), input.comment()));
  }

  /**
   * Submits a request for validation or review (MKT 1.11.0, 1.14.0).
   *
   * @param id request
   * @param input comment
   * @return the request
   */
  @PostMapping(REQUEST + "/submit")
  @PreAuthorize(PayRequestAccess.CREATE)
  public RequestView submit(@PathVariable Long id, @Valid @RequestBody CommentInput input) {
    return view(workflow.submit(id, input.comment()));
  }

  /**
   * Endorses a request for approval (MKT 1.15.0).
   *
   * @param id request
   * @param input comment
   * @return the request
   */
  @PostMapping(REQUEST + "/endorse")
  @PreAuthorize(PayRequestAccess.REVIEW)
  public RequestView endorse(@PathVariable Long id, @Valid @RequestBody CommentInput input) {
    return view(workflow.endorse(id, input.comment()));
  }

  /**
   * Approves a request: Marketing, then HR for a cash advance (MKT 1.16.0-1.16.3, 2.24.0).
   *
   * @param id request
   * @param input comment
   * @return the request
   */
  @PostMapping(REQUEST + "/approve")
  @PreAuthorize(PayRequestAccess.APPROVE)
  public RequestView approve(@PathVariable Long id, @Valid @RequestBody CommentInput input) {
    return view(workflow.approve(id, input.comment()));
  }

  /**
   * Enters the result of a handed-over validation (MKT 1.11.0).
   *
   * @param id request
   * @param validationId task
   * @param input result
   * @return the task
   */
  @PostMapping(REQUEST + "/validations/{validationId}/result")
  @PreAuthorize(PayRequestAccess.ASSIGN)
  public ValidationView recordValidation(
      @PathVariable Long id,
      @PathVariable Long validationId,
      @Valid @RequestBody ValidationResultInput input) {
    return ValidationView.from(
        validations.record(id, validationId, input.confirmed(), input.newArNo(), input.remarks()));
  }

  /**
   * Starts or changes the liquidation of a disbursed cash advance.
   *
   * @param id cash-advance request
   * @param input form
   * @return the liquidation
   */
  @PutMapping(LIQUIDATION)
  @PreAuthorize(PayRequestAccess.CREATE)
  public LiquidationView saveLiquidation(
      @PathVariable Long id, @Valid @RequestBody LiquidationInput input) {
    return LiquidationView.from(liquidations.save(id, input.draft()));
  }

  /**
   * Submits the liquidation for checking.
   *
   * @param id cash-advance request
   * @return the liquidation
   */
  @PostMapping(LIQUIDATION + "/submit")
  @PreAuthorize(PayRequestAccess.CREATE)
  public LiquidationView submitLiquidation(@PathVariable Long id) {
    return LiquidationView.from(liquidations.submit(id));
  }

  /**
   * Returns the liquidation to the employee.
   *
   * @param id cash-advance request
   * @param input reason
   * @return the liquidation
   */
  @PostMapping(LIQUIDATION + "/return")
  @PreAuthorize(PayRequestAccess.REVIEW)
  public LiquidationView returnLiquidation(
      @PathVariable Long id, @Valid @RequestBody CommentInput input) {
    return LiquidationView.from(liquidations.sendBack(id, input.comment()));
  }

  /**
   * Checks and posts the liquidation (event PRQ_CA_LIQUIDATION).
   *
   * @param id cash-advance request
   * @return the liquidation
   */
  @PostMapping(LIQUIDATION + "/post")
  @PreAuthorize(PayRequestAccess.REVIEW)
  public LiquidationView postLiquidation(@PathVariable Long id) {
    return LiquidationView.from(liquidations.post(id));
  }

  /**
   * Accounts of the liquidation event roles.
   *
   * @param companyId company
   * @return accounts by role
   */
  @GetMapping("/liquidation-accounts")
  @PreAuthorize(PayRequestAccess.VIEW)
  public List<AccountView> accounts(@RequestParam Long companyId) {
    return liquidations.accounts(companyId).stream().map(AccountView::from).toList();
  }

  /**
   * Sets the account of a liquidation event role (Comptrollership, AQ02).
   *
   * @param companyId company
   * @param input role and account
   * @return the configuration
   */
  @PutMapping("/liquidation-accounts")
  @PreAuthorize(PayRequestAccess.ACCOUNTS)
  public AccountView assignAccount(
      @RequestParam Long companyId, @Valid @RequestBody LiquidationAccountInput input) {
    return AccountView.from(liquidations.assign(companyId, input.role(), input.accountCode()));
  }

  private RequestView view(PaymentRequest r) {
    return RequestView.from(r, liquidations.of(r.getId()).orElse(null));
  }
}
