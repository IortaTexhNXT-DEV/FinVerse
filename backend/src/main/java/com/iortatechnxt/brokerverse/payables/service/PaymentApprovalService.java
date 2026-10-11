package com.iortatechnxt.brokerverse.payables.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.domain.PaymentMode;
import com.iortatechnxt.brokerverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.brokerverse.payables.domain.VoucherPosting;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checker side of payment vouchers: approval (cheque leaf allocation, GL posting, sub-ledger
 * matching, PDC registration), confirmation of cheque presentation and void of unpresented cheques.
 * See {@link PaymentPoster} for the accounting.
 */
@Service
@Transactional
public class PaymentApprovalService {

  private static final String ENTITY = "PaymentVoucher";

  private final PaymentVoucherService vouchers;
  private final BankAccountQueryService banks;
  private final BankAccountService bankService;
  private final PartyService parties;
  private final PayableItemGuard guard;
  private final PaymentPoster poster;
  private final IssuedPdcService pdcs;
  private final PayablesSupport support;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param vouchers voucher service
   * @param banks bank accounts
   * @param bankService cheque allocation
   * @param parties party master
   * @param guard payable checks
   * @param poster posting
   * @param pdcs PDC register
   * @param support shared helpers
   * @param audit audit trail
   * @param clock clock
   */
  public PaymentApprovalService(
      PaymentVoucherService vouchers,
      BankAccountQueryService banks,
      BankAccountService bankService,
      PartyService parties,
      PayableItemGuard guard,
      PaymentPoster poster,
      IssuedPdcService pdcs,
      PayablesSupport support,
      AuditTrailService audit,
      Clock clock) {
    this.vouchers = vouchers;
    this.banks = banks;
    this.bankService = bankService;
    this.parties = parties;
    this.guard = guard;
    this.poster = poster;
    this.pdcs = pdcs;
    this.support = support;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Approves, numbers and posts a voucher.
   *
   * @param id voucher
   * @return approved voucher
   */
  public PaymentVoucher approve(Long id) {
    PaymentVoucher voucher = vouchers.get(id);
    String checker =
        support.checker(
            voucher,
            support.toBase(
                voucher.getCompanyId(),
                voucher.getCurrency(),
                voucher.getAmount(),
                voucher.getVoucherDate()));
    BankAccount bank = banks.requireActive(voucher.getBankAccountId());
    Party party = parties.get(voucher.getPartyId());
    if (!party.isActive() || !voucher.getCategory().accepts(party.getPartyType())) {
      throw new BusinessRuleException(
          "INVALID_PAYEE", "Party " + party.getCode() + " cannot receive this payment");
    }
    guard.revalidate(party.getId(), voucher.getCurrency(), voucher.getAllocations());
    String chequeNo =
        voucher.getPaymentMode().usesCheque() ? bankService.nextChequeNo(bank.getId()) : null;
    PaymentPoster.PostedPayment posted = poster.post(voucher, bank);
    voucher.approve(
        checker,
        clock.instant(),
        new VoucherPosting(chequeNo, posted.batchNo(), posted.openItemId(), posted.baseAmount()));
    if (voucher.getPaymentMode() == PaymentMode.PDC) {
      pdcs.register(voucher);
    }
    audit.record(
        ENTITY,
        voucher.getVoucherNo(),
        AuditAction.POST,
        "Approved and posted "
            + posted.batchNo()
            + (chequeNo == null ? "" : ", cheque " + chequeNo));
    return voucher;
  }

  /**
   * Confirms that a cheque was presented (it can no longer be voided).
   *
   * @param id voucher
   * @param date presentation date
   * @return voucher
   */
  public PaymentVoucher markPresented(Long id, LocalDate date) {
    PaymentVoucher voucher = vouchers.get(id);
    voucher.markPresented(date);
    audit.record(ENTITY, voucher.getVoucherNo(), AuditAction.UPDATE, "Cheque presented " + date);
    return voucher;
  }

  /**
   * Voids an unpresented cheque: reverses the posting and re-opens the paid payables.
   *
   * @param id voucher
   * @param date void date
   * @param reason reason
   * @return voided voucher
   */
  public PaymentVoucher voidCheque(Long id, LocalDate date, String reason) {
    PaymentVoucher voucher = vouchers.get(id);
    if (voucher.getPaymentMode() != PaymentMode.CHEQUE) {
      throw new BusinessRuleException(
          "NOT_VOIDABLE",
          voucher.getPaymentMode() == PaymentMode.PDC
              ? "Cancel post-dated cheques from the PDC register"
              : "Bank transfers cannot be voided; record a refund instead");
    }
    support.checker(voucher, voucher.getBaseAmount());
    String batchNo = poster.reverse(voucher, banks.get(voucher.getBankAccountId()), date, reason);
    voucher.markVoided(date, batchNo, reason);
    audit.record(ENTITY, voucher.getVoucherNo(), AuditAction.REVERSE, "Voided: " + reason);
    return voucher;
  }
}
