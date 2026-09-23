package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.finverse.accounting.service.BusinessEvent;
import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.payables.domain.BankAccount;
import com.iortatechnxt.finverse.payables.domain.PettyCashFund;
import com.iortatechnxt.finverse.payables.domain.PettyCashFundRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Petty cash fund master (maker-checker) and establishment of the imprest.
 *
 * <p>Establishment posts {@code PETTY_CASH_REPLENISHMENT} for the imprest amount (Dr petty cash /
 * Cr bank): the box starts full.
 */
@Service
@Transactional
public class PettyCashFundService {

  /** Event type used to fund and replenish a box. */
  public static final String REPLENISHMENT_EVENT = "PETTY_CASH_REPLENISHMENT";

  private static final String ENTITY = "PettyCashFund";

  private final PettyCashFundRepository funds;
  private final BankAccountQueryService banks;
  private final ChartOfAccountsService chart;
  private final AccountingEventPublisher publisher;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param funds repository
   * @param banks bank accounts
   * @param chart chart of accounts
   * @param publisher accounting engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PettyCashFundService(
      PettyCashFundRepository funds,
      BankAccountQueryService banks,
      ChartOfAccountsService chart,
      AccountingEventPublisher publisher,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.funds = funds;
    this.banks = banks;
    this.chart = chart;
    this.publisher = publisher;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists the funds of a company.
   *
   * @param companyId company
   * @return funds
   */
  @Transactional(readOnly = true)
  public List<PettyCashFund> list(Long companyId) {
    return funds.findByCompanyIdOrderByCode(companyId);
  }

  /**
   * Gets a fund.
   *
   * @param id id
   * @return fund
   */
  @Transactional(readOnly = true)
  public PettyCashFund get(Long id) {
    return funds
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Petty cash fund", id));
  }

  /**
   * Creates a fund (pending authorization).
   *
   * @param c values
   * @return fund
   */
  public PettyCashFund create(FundCommand c) {
    if (funds.findByCompanyIdAndCode(c.companyId(), c.code()).isPresent()) {
      throw new DuplicateResourceException("Petty cash fund", c.code());
    }
    BankAccount bank = banks.requireActive(c.replenishBankAccountId());
    PettyCashFund fund =
        new PettyCashFund(c.companyId(), c.branchId(), c.code(), bank.getCurrency());
    apply(fund, c);
    PettyCashFund saved = funds.save(fund);
    audit.record(ENTITY, saved.getCode(), AuditAction.CREATE, "Created fund " + c.name());
    return saved;
  }

  /**
   * Updates a fund; it returns to pending authorization.
   *
   * @param id id
   * @param c values
   * @return fund
   */
  public PettyCashFund update(Long id, FundCommand c) {
    PettyCashFund fund = get(id);
    banks.requireActive(c.replenishBankAccountId());
    fund.setBranchId(c.branchId());
    apply(fund, c);
    fund.markModified();
    audit.record(ENTITY, fund.getCode(), AuditAction.UPDATE, "Updated fund");
    return fund;
  }

  /**
   * Authorizes a fund.
   *
   * @param id id
   * @return fund
   */
  public PettyCashFund authorize(Long id) {
    PettyCashFund fund = get(id);
    fund.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, fund.getCode(), AuditAction.AUTHORIZE, "Authorized fund");
    return fund;
  }

  /**
   * Establishes an authorized fund: draws the imprest from the replenishment bank account.
   *
   * @param id fund
   * @param date establishment date
   * @return fund
   */
  public PettyCashFund establish(Long id, LocalDate date) {
    PettyCashFund fund =
        funds.lockById(id).orElseThrow(() -> new ResourceNotFoundException("Petty cash fund", id));
    fund.establish(date);
    BankAccount bank = banks.requireActive(fund.getReplenishBankAccountId());
    String batchNo =
        publisher
            .publish(
                replenishment(
                    fund,
                    bank,
                    fund.getImprestAmount(),
                    "PCF:" + fund.getId() + ":ESTABLISH",
                    date,
                    fund.getCode(),
                    "Establishment of petty cash fund " + fund.getName()))
            .getBatchNo();
    audit.record(ENTITY, fund.getCode(), AuditAction.POST, "Established with " + batchNo);
    return fund;
  }

  /**
   * Builds the replenishment event of a fund (Dr petty cash / Cr bank).
   *
   * @param fund fund
   * @param bank paying bank account
   * @param amount amount
   * @param sourceReference idempotency key
   * @param date value date
   * @param reference business reference
   * @param narration narration
   * @return event
   */
  static BusinessEvent replenishment(
      PettyCashFund fund,
      BankAccount bank,
      BigDecimal amount,
      String sourceReference,
      LocalDate date,
      String reference,
      String narration) {
    return new BusinessEvent(
        REPLENISHMENT_EVENT,
        fund.getCompanyId(),
        fund.getBranchId(),
        date,
        fund.getCurrency(),
        PayablesSupport.MODULE,
        sourceReference,
        reference,
        null,
        null,
        null,
        narration,
        Map.of("AMOUNT", amount),
        Map.of("BANK", bank.getGlAccountCode(), "PETTY_CASH", fund.getGlAccountCode()));
  }

  private void apply(PettyCashFund fund, FundCommand c) {
    GlAccount account = chart.getByCode(c.companyId(), c.glAccountCode());
    if (!account.isPostable() || account.isControlAccount()) {
      throw new BusinessRuleException(
          "INVALID_PETTY_CASH_ACCOUNT", "Account " + c.glAccountCode() + " cannot hold petty cash");
    }
    fund.setName(c.name());
    fund.setCustodian(c.custodian());
    fund.setGlAccountCode(c.glAccountCode());
    fund.setReplenishBankAccountId(c.replenishBankAccountId());
    fund.changeImprest(c.imprestAmount());
  }
}
