package com.iortatechnxt.finverse.investment.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.domain.GlAccountRepository;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.investment.api.dto.PortfolioRequest;
import com.iortatechnxt.finverse.investment.domain.InvestmentPortfolio;
import com.iortatechnxt.finverse.investment.domain.InvestmentPortfolioRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Investment portfolio maintenance (maker-checker) and GL account validation. */
@Service
@Transactional
public class PortfolioService {

  private static final String PORTFOLIO = "InvestmentPortfolio";

  private final InvestmentPortfolioRepository portfolios;
  private final GlAccountRepository accounts;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param portfolios repository
   * @param accounts chart of accounts
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public PortfolioService(
      InvestmentPortfolioRepository portfolios,
      GlAccountRepository accounts,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.portfolios = portfolios;
    this.accounts = accounts;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists portfolios.
   *
   * @param companyId company
   * @return portfolios
   */
  @Transactional(readOnly = true)
  public List<InvestmentPortfolio> list(Long companyId) {
    return portfolios.findByCompanyIdOrderByCode(companyId);
  }

  /**
   * Gets a portfolio.
   *
   * @param id id
   * @return portfolio
   */
  @Transactional(readOnly = true)
  public InvestmentPortfolio get(Long id) {
    return portfolios.findById(id).orElseThrow(() -> new ResourceNotFoundException(PORTFOLIO, id));
  }

  /**
   * Creates a portfolio (pending authorization).
   *
   * @param r request
   * @return portfolio
   */
  public InvestmentPortfolio create(PortfolioRequest r) {
    if (portfolios.existsByCompanyIdAndCode(r.companyId(), r.code())) {
      throw new DuplicateResourceException(PORTFOLIO, r.code());
    }
    InvestmentPortfolio p = new InvestmentPortfolio(r.companyId(), r.code(), r.classification());
    apply(p, r);
    InvestmentPortfolio saved = portfolios.save(p);
    audit.record(PORTFOLIO, saved.getCode(), AuditAction.CREATE, "Created portfolio " + r.name());
    return saved;
  }

  /**
   * Updates a portfolio; it returns to pending authorization.
   *
   * @param id id
   * @param r request
   * @return portfolio
   */
  public InvestmentPortfolio update(Long id, PortfolioRequest r) {
    InvestmentPortfolio p = get(id);
    apply(p, r);
    p.markModified();
    audit.record(PORTFOLIO, p.getCode(), AuditAction.UPDATE, "Updated portfolio");
    return p;
  }

  /**
   * Authorizes a portfolio (checker).
   *
   * @param id id
   * @return portfolio
   */
  public InvestmentPortfolio authorize(Long id) {
    InvestmentPortfolio p = get(id);
    p.authorize(currentUser.username(), clock.instant());
    audit.record(PORTFOLIO, p.getCode(), AuditAction.AUTHORIZE, "Authorized portfolio");
    return p;
  }

  /**
   * Fails unless the account exists and accepts postings.
   *
   * @param companyId company
   * @param code account code
   */
  public void requirePostable(Long companyId, String code) {
    boolean postable =
        accounts.findByCompanyIdAndCode(companyId, code).map(GlAccount::isPostable).orElse(false);
    if (!postable) {
      throw new BusinessRuleException(
          "INVALID_ACCOUNT", "GL account " + code + " does not exist or is not postable");
    }
  }

  private void apply(InvestmentPortfolio p, PortfolioRequest r) {
    boolean fairValued = p.getClassification().isFairValued();
    if (fairValued && (r.fairValueAccount() == null || r.fairValueAccount().isBlank())) {
      throw new BusinessRuleException(
          "FAIR_VALUE_ACCOUNT_REQUIRED", "FVOCI and FVPL portfolios need a fair value account");
    }
    requirePostable(r.companyId(), r.investmentAccount());
    requirePostable(r.companyId(), r.accruedInterestAccount());
    requirePostable(r.companyId(), r.interestIncomeAccount());
    requirePostable(r.companyId(), r.realizedGainAccount());
    if (fairValued) {
      requirePostable(r.companyId(), r.fairValueAccount());
    }
    p.setName(r.name());
    p.setInvestmentAccount(r.investmentAccount());
    p.setAccruedInterestAccount(r.accruedInterestAccount());
    p.setInterestIncomeAccount(r.interestIncomeAccount());
    p.setRealizedGainAccount(r.realizedGainAccount());
    p.setFairValueAccount(fairValued ? r.fairValueAccount() : null);
  }
}
