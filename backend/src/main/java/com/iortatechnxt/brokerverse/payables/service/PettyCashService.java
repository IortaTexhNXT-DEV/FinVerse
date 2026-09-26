package com.iortatechnxt.brokerverse.payables.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashDisbursement;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashDisbursementRepository;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashDisbursementValues;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashFund;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashFundRepository;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashReimbursement;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashReimbursementRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Petty cash vouchers under the imprest system.
 *
 * <ul>
 *   <li>Disbursement voucher approved → {@code PETTY_CASH_EXPENSE} (Dr expense / Cr petty cash),
 *       cash in the box reduced; refused when it would overdraw the box.
 *   <li>Reimbursement claim (group of approved, unclaimed vouchers) approved → {@code
 *       PETTY_CASH_REPLENISHMENT} (Dr petty cash / Cr bank), box refilled; never above imprest.
 * </ul>
 */
@Service
@Transactional
public class PettyCashService {

  private static final String DISBURSEMENT = "PettyCashDisbursement";
  private static final String REIMBURSEMENT = "PettyCashReimbursement";

  private final PettyCashFundRepository funds;
  private final PettyCashDisbursementRepository disbursements;
  private final PettyCashReimbursementRepository reimbursements;
  private final BankAccountQueryService banks;
  private final ExpenseAccountValidator validator;
  private final AccountingEventPublisher publisher;
  private final PayablesSupport support;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param funds funds
   * @param disbursements disbursement vouchers
   * @param reimbursements reimbursement claims
   * @param banks bank accounts
   * @param validator expense line validator
   * @param publisher accounting engine
   * @param support shared helpers
   * @param audit audit trail
   * @param clock clock
   */
  public PettyCashService(
      PettyCashFundRepository funds,
      PettyCashDisbursementRepository disbursements,
      PettyCashReimbursementRepository reimbursements,
      BankAccountQueryService banks,
      ExpenseAccountValidator validator,
      AccountingEventPublisher publisher,
      PayablesSupport support,
      AuditTrailService audit,
      Clock clock) {
    this.funds = funds;
    this.disbursements = disbursements;
    this.reimbursements = reimbursements;
    this.banks = banks;
    this.validator = validator;
    this.publisher = publisher;
    this.support = support;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Lists disbursement vouchers of a fund.
   *
   * @param fundId fund
   * @param from from date
   * @param to to date
   * @return vouchers
   */
  @Transactional(readOnly = true)
  public List<PettyCashDisbursement> disbursements(Long fundId, LocalDate from, LocalDate to) {
    return disbursements.search(fundId, from, to);
  }

  /**
   * Approved vouchers not yet claimed for reimbursement.
   *
   * @param fundId fund
   * @return vouchers
   */
  @Transactional(readOnly = true)
  public List<PettyCashDisbursement> unclaimed(Long fundId) {
    return disbursements.findUnclaimed(fundId);
  }

  /**
   * Lists reimbursement claims of a fund.
   *
   * @param fundId fund
   * @return claims
   */
  @Transactional(readOnly = true)
  public List<PettyCashReimbursement> reimbursements(Long fundId) {
    return reimbursements.findByFundIdOrderByClaimDateDescIdDesc(fundId);
  }

  /**
   * Vouchers of a claim.
   *
   * @param reimbursementId claim
   * @return vouchers
   */
  @Transactional(readOnly = true)
  public List<PettyCashDisbursement> claimVouchers(Long reimbursementId) {
    return disbursements.findByReimbursementIdOrderByDisbursementDate(reimbursementId);
  }

  /**
   * Captures a disbursement voucher.
   *
   * @param fundId fund
   * @param values voucher values
   * @return voucher
   */
  public PettyCashDisbursement disburse(Long fundId, PettyCashDisbursementValues values) {
    PettyCashFund fund = fund(fundId);
    validator.validate(fund.getCompanyId(), values.expenseAccountCode(), values.costCenter());
    if (!Money.isPositive(values.amount())
        || fund.getEstablishedOn() == null
        || values.amount().compareTo(fund.getCashBalance()) > 0) {
      throw new BusinessRuleException(
          "PETTY_CASH_INSUFFICIENT",
          "Fund "
              + fund.getCode()
              + " holds "
              + fund.getCashBalance()
              + "; cannot pay "
              + values.amount());
    }
    PettyCashDisbursement voucher =
        disbursements.save(
            new PettyCashDisbursement(
                fund, support.nextNumber("PCD", fund.getBranchId(), values.date()), values));
    audit.record(
        DISBURSEMENT, voucher.getDocumentNo(), AuditAction.CREATE, "Disbursed " + values.amount());
    return voucher;
  }

  /**
   * Approves and posts a disbursement voucher.
   *
   * @param id voucher
   * @return voucher
   */
  public PettyCashDisbursement approveDisbursement(Long id) {
    PettyCashDisbursement voucher = disbursement(id);
    String checker = support.checker(voucher, voucher.getAmount());
    PettyCashFund fund = lockFund(voucher.getFundId());
    fund.disburse(voucher.getAmount());
    String batchNo =
        publisher
            .publish(
                new BusinessEvent(
                    "PETTY_CASH_EXPENSE",
                    fund.getCompanyId(),
                    fund.getBranchId(),
                    voucher.getDisbursementDate(),
                    fund.getCurrency(),
                    PayablesSupport.MODULE,
                    "PCD:" + voucher.getId(),
                    voucher.getDocumentNo(),
                    null,
                    null,
                    voucher.getCostCenter(),
                    voucher.getDescription() + " - " + voucher.getPayee(),
                    Map.of("AMOUNT", voucher.getAmount()),
                    Map.of(
                        "EXPENSE", voucher.getExpenseAccountCode(),
                        "PETTY_CASH", fund.getGlAccountCode())))
            .getBatchNo();
    voucher.approve(checker, clock.instant(), batchNo);
    audit.record(DISBURSEMENT, voucher.getDocumentNo(), AuditAction.POST, "Posted " + batchNo);
    return voucher;
  }

  /**
   * Rejects a disbursement voucher.
   *
   * @param id voucher
   * @param reason reason
   * @return voucher
   */
  public PettyCashDisbursement rejectDisbursement(Long id, String reason) {
    PettyCashDisbursement voucher = disbursement(id);
    voucher.reject(support.user(), reason);
    audit.record(DISBURSEMENT, voucher.getDocumentNo(), AuditAction.REJECT, reason);
    return voucher;
  }

  /**
   * Claims reimbursement of approved vouchers.
   *
   * @param fundId fund
   * @param date claim date
   * @param disbursementIds vouchers to claim; null or empty = every unclaimed voucher
   * @param narration narration
   * @return claim
   */
  public PettyCashReimbursement claimReimbursement(
      Long fundId, LocalDate date, List<Long> disbursementIds, String narration) {
    PettyCashFund fund = fund(fundId);
    List<PettyCashDisbursement> vouchers =
        disbursementIds == null || disbursementIds.isEmpty()
            ? disbursements.findUnclaimed(fundId)
            : disbursements.findByIdIn(disbursementIds);
    if (vouchers.isEmpty()
        || vouchers.stream().anyMatch(v -> !Objects.equals(v.getFundId(), fundId))) {
      throw new BusinessRuleException(
          "NOTHING_TO_REIMBURSE", "Select approved vouchers of fund " + fund.getCode());
    }
    BigDecimal total =
        vouchers.stream()
            .map(PettyCashDisbursement::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    PettyCashReimbursement claim =
        reimbursements.save(
            new PettyCashReimbursement(
                fund,
                support.nextNumber("PCR", fund.getBranchId(), date),
                date,
                fund.getReplenishBankAccountId(),
                Money.round(total),
                narration));
    vouchers.forEach(v -> v.claim(claim.getId()));
    audit.record(
        REIMBURSEMENT,
        claim.getDocumentNo(),
        AuditAction.CREATE,
        "Claimed " + vouchers.size() + " vouchers for " + claim.getAmount());
    return claim;
  }

  /**
   * Approves a claim: the bank refills the box.
   *
   * @param id claim
   * @return claim
   */
  public PettyCashReimbursement approveReimbursement(Long id) {
    PettyCashReimbursement claim = reimbursement(id);
    String checker = support.checker(claim, claim.getAmount());
    PettyCashFund fund = lockFund(claim.getFundId());
    fund.replenish(claim.getAmount());
    BankAccount bank = banks.requireActive(claim.getBankAccountId());
    String batchNo =
        publisher
            .publish(
                PettyCashFundService.replenishment(
                    fund,
                    bank,
                    claim.getAmount(),
                    "PCR:" + claim.getId(),
                    claim.getClaimDate(),
                    claim.getDocumentNo(),
                    "Petty cash reimbursement " + fund.getCode()))
            .getBatchNo();
    claim.approve(checker, clock.instant(), batchNo);
    audit.record(REIMBURSEMENT, claim.getDocumentNo(), AuditAction.POST, "Posted " + batchNo);
    return claim;
  }

  /**
   * Rejects a claim; its vouchers can be claimed again.
   *
   * @param id claim
   * @param reason reason
   * @return claim
   */
  public PettyCashReimbursement rejectReimbursement(Long id, String reason) {
    PettyCashReimbursement claim = reimbursement(id);
    claim.reject(support.user(), reason);
    disbursements
        .findByReimbursementIdOrderByDisbursementDate(id)
        .forEach(PettyCashDisbursement::unclaim);
    audit.record(REIMBURSEMENT, claim.getDocumentNo(), AuditAction.REJECT, reason);
    return claim;
  }

  private PettyCashFund fund(Long id) {
    return funds
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Petty cash fund", id));
  }

  private PettyCashFund lockFund(Long id) {
    return funds
        .lockById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Petty cash fund", id));
  }

  private PettyCashDisbursement disbursement(Long id) {
    return disbursements
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Petty cash voucher", id));
  }

  private PettyCashReimbursement reimbursement(Long id) {
    return reimbursements
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Reimbursement claim", id));
  }
}
