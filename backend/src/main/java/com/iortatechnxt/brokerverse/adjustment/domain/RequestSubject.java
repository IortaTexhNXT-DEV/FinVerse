package com.iortatechnxt.brokerverse.adjustment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * The booked invoice an endorsement request is raised on, copied from the Operations ledger when
 * the request is created (ADJID.001/020: the ARN and the invoice number stay on the request).
 *
 * @param invoiceNo invoice number
 * @param arn Account Reference Number
 * @param policyNo insurer policy number, may be null (kept in line with the ledger: the policy
 *     number is often issued after the request is raised)
 * @param clientCode client code
 * @param assuredName assured name
 * @param insurerCode lead insurer
 * @param currency currency
 * @param segment market segment
 * @param aoUsername requesting Account Officer
 * @param productLine product line (risk type)
 * @param accountId account (placement and policy), may be null for a legacy invoice
 * @param productCode product of the account, may be null for a legacy invoice
 */
@Embeddable
public record RequestSubject(
    @Column(name = "invoice_no", nullable = false, length = 40, updatable = false) String invoiceNo,
    @Column(nullable = false, length = 30, updatable = false) String arn,
    @Column(name = "policy_no", length = 60) String policyNo,
    @Column(name = "client_code", nullable = false, length = 30, updatable = false)
        String clientCode,
    @Column(name = "assured_name", nullable = false, length = 250, updatable = false)
        String assuredName,
    @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
        String insurerCode,
    @Column(nullable = false, length = 3, updatable = false) String currency,
    @Column(length = 40, updatable = false) String segment,
    @Column(name = "ao_username", length = 50, updatable = false) String aoUsername,
    @Column(name = "product_line", length = 30, updatable = false) String productLine,
    @Column(name = "account_id", updatable = false) Long accountId,
    @Column(name = "product_code", length = 30, updatable = false) String productCode) {

  /**
   * The same subject with the policy number the ledger holds now.
   *
   * @param policy policy number
   * @return subject
   */
  public RequestSubject withPolicyNo(String policy) {
    return new RequestSubject(
        invoiceNo,
        arn,
        policy,
        clientCode,
        assuredName,
        insurerCode,
        currency,
        segment,
        aoUsername,
        productLine,
        accountId,
        productCode);
  }
}
