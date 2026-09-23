package com.iortatechnxt.finverse.receivables.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.receivables.api.dto.DateRequest;
import com.iortatechnxt.finverse.receivables.api.dto.DepositSlipRequest;
import com.iortatechnxt.finverse.receivables.domain.DepositSlip;
import com.iortatechnxt.finverse.receivables.domain.DepositSlipRepository;
import com.iortatechnxt.finverse.receivables.domain.DepositStatus;
import com.iortatechnxt.finverse.receivables.domain.Receipt;
import com.iortatechnxt.finverse.receivables.domain.ReceiptRepository;
import com.iortatechnxt.finverse.receivables.domain.ReceiptStatus;
import com.iortatechnxt.finverse.receivables.service.BankAccountDirectory.BankAccount;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cheques and cash received but not yet banked, and bank deposit (pay-in) slips.
 *
 * <p>The receipt journal already debits the bank account (deposits in transit appear in the bank
 * reconciliation until the bank credits them); a slip records when and with which pay-in the money
 * went to the bank, which lets the reconciliation match the bank's single credit per slip.
 */
@Service
@Transactional
public class DepositService {

  private static final String ENTITY = "DepositSlip";

  private final DepositSlipRepository slips;
  private final ReceiptRepository receipts;
  private final BankAccountDirectory banks;
  private final OrganizationService organization;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;

  /**
   * Creates the service.
   *
   * @param slips slip repository
   * @param receipts receipt repository
   * @param banks bank account directory
   * @param organization organization service
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   */
  public DepositService(
      DepositSlipRepository slips,
      ReceiptRepository receipts,
      BankAccountDirectory banks,
      OrganizationService organization,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser) {
    this.slips = slips;
    this.receipts = receipts;
    this.banks = banks;
    this.organization = organization;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
  }

  /**
   * Lists approved cash and cheque receipts not yet deposited.
   *
   * @param companyId company
   * @param bankAccountCode bank account (null = all)
   * @return receipts oldest first
   */
  @Transactional(readOnly = true)
  public List<Receipt> undeposited(Long companyId, String bankAccountCode) {
    return bankAccountCode == null || bankAccountCode.isBlank()
        ? receipts.findByCompanyIdAndStatusAndDepositStatusOrderByReceiptDateAscIdAsc(
            companyId, ReceiptStatus.APPROVED, DepositStatus.UNDEPOSITED)
        : receipts
            .findByCompanyIdAndBankAccountCodeAndStatusAndDepositStatusOrderByReceiptDateAscIdAsc(
                companyId, bankAccountCode, ReceiptStatus.APPROVED, DepositStatus.UNDEPOSITED);
  }

  /**
   * Prepares a deposit slip for receipts of one bank account and currency.
   *
   * @param r request
   * @return slip
   */
  public DepositSlip create(DepositSlipRequest r) {
    Branch branch = organization.requireActiveBranch(r.branchId());
    BankAccount bank = banks.require(r.companyId(), r.bankAccountCode(), null);
    List<Receipt> selected = receipts.findAllById(r.receiptIds());
    if (selected.size() != r.receiptIds().size()) {
      throw new ResourceNotFoundException("Receipt", r.receiptIds());
    }
    String currency = selected.get(0).getCurrency();
    DepositSlip slip =
        slips.save(
            new DepositSlip(
                r.companyId(),
                r.branchId(),
                numbers.next("DS-" + branch.getCode() + "-" + r.slipDate().getYear()),
                r.slipDate(),
                bank.code(),
                currency));
    for (Receipt receipt : selected) {
      if (!receipt.getCompanyId().equals(r.companyId())
          || !receipt.getBankAccountCode().equals(bank.code())
          || !receipt.getCurrency().equals(currency)) {
        throw new BusinessRuleException(
            "SLIP_MISMATCH",
            "Receipt " + receipt.getReceiptNo() + " is for another bank account or currency");
      }
      if (receipt.getReceiptDate().isAfter(r.slipDate())) {
        throw new BusinessRuleException(
            "SLIP_DATE", "Receipt " + receipt.getReceiptNo() + " is dated after the slip");
      }
      receipt.addToSlip(slip.getId());
      slip.add(receipt.getAmount());
    }
    audit.record(
        ENTITY,
        slip.getSlipNo(),
        AuditAction.CREATE,
        selected.size() + " receipts to " + bank.code());
    return slip;
  }

  /**
   * Confirms that a slip was deposited at the bank.
   *
   * @param id slip
   * @param request deposit date
   * @return slip
   */
  public DepositSlip confirm(Long id, DateRequest request) {
    DepositSlip slip = get(id);
    slip.deposit(request.date(), currentUser.username());
    receipts.findByDepositSlipIdOrderByIdAsc(id).forEach(r -> r.markDeposited(request.date()));
    audit.record(ENTITY, slip.getSlipNo(), AuditAction.UPDATE, "Deposited on " + request.date());
    return slip;
  }

  /**
   * Cancels a prepared slip; its receipts become undeposited again.
   *
   * @param id slip
   * @return slip
   */
  public DepositSlip cancel(Long id) {
    DepositSlip slip = get(id);
    slip.cancel();
    receipts.findByDepositSlipIdOrderByIdAsc(id).forEach(Receipt::removeFromSlip);
    audit.record(ENTITY, slip.getSlipNo(), AuditAction.DEACTIVATE, "Slip cancelled");
    return slip;
  }

  /**
   * Gets a slip.
   *
   * @param id id
   * @return slip
   */
  @Transactional(readOnly = true)
  public DepositSlip get(Long id) {
    return slips.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Lists the receipts of a slip.
   *
   * @param id slip
   * @return receipts
   */
  @Transactional(readOnly = true)
  public List<Receipt> receiptsOf(Long id) {
    return receipts.findByDepositSlipIdOrderByIdAsc(get(id).getId());
  }

  /**
   * Lists slips.
   *
   * @param companyId company
   * @return slips newest first
   */
  @Transactional(readOnly = true)
  public List<DepositSlip> list(Long companyId) {
    return slips.findByCompanyIdOrderBySlipDateDescIdDesc(companyId);
  }
}
