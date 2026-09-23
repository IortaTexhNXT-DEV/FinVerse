package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.tax.domain.TaxCode;
import com.iortatechnxt.finverse.tax.domain.TaxCodeRepository;
import com.iortatechnxt.finverse.tax.domain.TaxType;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maintenance of tax codes and ATCs (maker-checker, audited). */
@Service
@Transactional
public class TaxCodeService {

  /** Audit entity name. */
  public static final String ENTITY = "TaxCode";

  private final TaxCodeRepository codes;
  private final TaxMasterSupport support;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param codes repository
   * @param support validations
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public TaxCodeService(
      TaxCodeRepository codes,
      TaxMasterSupport support,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.codes = codes;
    this.support = support;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists the codes of a company.
   *
   * @param companyId company
   * @return codes ordered by type and code
   */
  @Transactional(readOnly = true)
  public List<TaxCode> list(Long companyId) {
    return codes.findByCompanyIdOrderByTaxTypeAscCodeAsc(companyId);
  }

  /**
   * Gets a code.
   *
   * @param id id
   * @return code
   */
  @Transactional(readOnly = true)
  public TaxCode get(Long id) {
    return codes.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Creates a code (pending authorization).
   *
   * @param c values
   * @return code
   */
  public TaxCode create(TaxCodeCommand c) {
    if (codes.existsByCompanyIdAndCode(c.companyId(), c.code())) {
      throw new DuplicateResourceException(ENTITY, c.code());
    }
    TaxCode code = new TaxCode(c.companyId(), c.code(), c.taxType());
    apply(code, c);
    TaxCode saved = codes.save(code);
    audit.record(ENTITY, saved.getCode(), AuditAction.CREATE, "Created tax code " + c.name());
    return saved;
  }

  /**
   * Updates a code; it returns to pending authorization.
   *
   * @param id id
   * @param c values (code and company are immutable)
   * @return code
   */
  public TaxCode update(Long id, TaxCodeCommand c) {
    TaxCode code = get(id);
    code.setTaxType(c.taxType());
    apply(code, c);
    code.markModified();
    audit.record(ENTITY, code.getCode(), AuditAction.UPDATE, "Updated tax code");
    return code;
  }

  /**
   * Authorizes a code (checker).
   *
   * @param id id
   * @return code
   */
  public TaxCode authorize(Long id) {
    TaxCode code = get(id);
    code.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, code.getCode(), AuditAction.AUTHORIZE, "Authorized tax code");
    return code;
  }

  private void apply(TaxCode code, TaxCodeCommand c) {
    String atc = TaxMasterSupport.blankToNull(c.atc());
    if (c.taxType() == TaxType.EWT && atc == null) {
      throw new BusinessRuleException(
          "ATC_REQUIRED", "A withholding tax code needs its alphanumeric tax code (ATC)");
    }
    if (c.effectiveTo() != null && c.effectiveTo().isBefore(c.effectiveFrom())) {
      throw new BusinessRuleException(
          "INVALID_EFFECTIVE_DATES", "Effective to precedes effective from");
    }
    support.requirePostable(c.companyId(), c.glAccountCode(), "Tax");
    code.setName(c.name());
    code.setAtc(atc);
    code.setPayeeClass(c.payeeClass());
    code.setRate(c.rate());
    code.setGlAccountCode(c.glAccountCode());
    code.setIncomeNature(TaxMasterSupport.blankToNull(c.incomeNature()));
    code.setEffectiveFrom(c.effectiveFrom());
    code.setEffectiveTo(c.effectiveTo());
  }
}
