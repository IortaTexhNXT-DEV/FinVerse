package com.iortatechnxt.brokerverse.opsledger.api;

import com.iortatechnxt.brokerverse.opsledger.service.PolicyTransactionService;
import com.iortatechnxt.brokerverse.opsledger.service.PolicyTransactionService.PolicyTransactions;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Policy transaction history (ADJID.022/024): the original booking, endorsements, cancellations,
 * adjustments and refunds of a policy with their GL journals, for the invoice 360, the account and
 * the endorsement request pages.
 */
@RestController
@RequestMapping("/api/v1/ops")
public class PolicyTransactionController {

  private final PolicyTransactionService transactions;

  /**
   * Creates the controller.
   *
   * @param transactions policy transaction history
   */
  public PolicyTransactionController(PolicyTransactionService transactions) {
    this.transactions = transactions;
  }

  /**
   * The history of the policy an invoice belongs to.
   *
   * @param invoiceNo any invoice of the policy
   * @return transactions in order
   */
  @GetMapping("/invoices/{invoiceNo}/transactions")
  @PreAuthorize(OpsAccess.VIEW)
  public PolicyTransactions forInvoice(@PathVariable String invoiceNo) {
    return transactions.forInvoice(invoiceNo);
  }

  /**
   * The history of an account (every policy year).
   *
   * @param arn Account Reference Number
   * @return transactions in order
   */
  @GetMapping("/accounts/{arn}/transactions")
  @PreAuthorize(OpsAccess.VIEW)
  public PolicyTransactions forAccount(@PathVariable String arn) {
    return transactions.forArn(arn);
  }
}
