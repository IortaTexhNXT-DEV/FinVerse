package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequestRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.service.DisbursementForms.FormFacts;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.domain.BankAccountRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads what the forms print besides the voucher: the request number, the paying bank account and
 * the payee bank account (DIS 2.7.1, 2.7.7-2.7.9).
 */
@Component
@Transactional(readOnly = true)
public class FormFactsReader {

  private final IntakeRequestRepository requests;
  private final BankAccountRepository banks;
  private final PayeeRepository payees;

  /**
   * Creates the reader.
   *
   * @param requests payment requests
   * @param banks bank accounts
   * @param payees payees
   */
  public FormFactsReader(
      IntakeRequestRepository requests, BankAccountRepository banks, PayeeRepository payees) {
    this.requests = requests;
    this.banks = banks;
    this.payees = payees;
  }

  /**
   * The facts of a voucher.
   *
   * @param v voucher
   * @return facts; missing records are left null
   */
  public FormFacts of(Voucher v) {
    String requestNo =
        requests.findById(v.getRequestId()).map(IntakeRequest::getRequestNo).orElse(null);
    BankAccount bank =
        v.getBankAccountId() == null ? null : banks.findById(v.getBankAccountId()).orElse(null);
    return new FormFacts(requestNo, bank, payeeAccount(v));
  }

  /**
   * The payee bank account of a voucher.
   *
   * @param v voucher
   * @return account, null when none
   */
  public PayeeAccount payeeAccount(Voucher v) {
    if (v.getPayeeAccountId() == null) {
      return null;
    }
    return payees
        .findWithAccountsById(v.getPayeeId())
        .flatMap(
            p ->
                p.getAccounts().stream()
                    .filter(a -> a.getId().equals(v.getPayeeAccountId()))
                    .findFirst())
        .orElse(null);
  }
}
