package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.reserves.domain.IbnrMethod;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameter;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameterRepository;
import com.iortatechnxt.finverse.reserves.domain.ReserveParameterTerms;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserve parameters per line of business, effective dated, under maker-checker control. The set in
 * force at a valuation date is, per line, the authorized record with the latest effective date on
 * or before it.
 */
@Service
@Transactional
public class ReserveParameterService {

  /** Audit entity name. */
  public static final String ENTITY = "ReserveParameter";

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final ReserveParameterRepository repository;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param repository parameter repository
   * @param audit audit trail
   * @param currentUser current user (checker)
   * @param clock clock
   */
  public ReserveParameterService(
      ReserveParameterRepository repository,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.repository = repository;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists a company's parameter sets.
   *
   * @param companyId company
   * @return parameter sets
   */
  @Transactional(readOnly = true)
  public List<ReserveParameter> list(Long companyId) {
    return repository.findByCompanyIdOrderByBusinessLineAscEffectiveFromDesc(companyId);
  }

  /**
   * Gets a parameter set.
   *
   * @param id id
   * @return parameter set
   */
  @Transactional(readOnly = true)
  public ReserveParameter get(Long id) {
    return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Parameters in force at a valuation date.
   *
   * @param companyId company
   * @param date valuation date
   * @return parameters by line of business
   */
  @Transactional(readOnly = true)
  public ReserveParameterSet inForce(Long companyId, LocalDate date) {
    Map<String, ReserveParameterTerms> byLine = new LinkedHashMap<>();
    for (ReserveParameter p : list(companyId)) {
      if (p.isActive() && !p.getEffectiveFrom().isAfter(date)) {
        byLine.putIfAbsent(p.getBusinessLine(), p.terms());
      }
    }
    return new ReserveParameterSet(byLine);
  }

  /**
   * Creates a parameter set (pending authorization).
   *
   * @param companyId company
   * @param businessLine line of business
   * @param effectiveFrom effective date
   * @param terms values
   * @return created record
   */
  public ReserveParameter create(
      Long companyId, String businessLine, LocalDate effectiveFrom, ReserveParameterTerms terms) {
    validate(terms);
    if (repository.existsByCompanyIdAndBusinessLineAndEffectiveFrom(
        companyId, businessLine, effectiveFrom)) {
      throw new DuplicateResourceException(ENTITY, businessLine + " from " + effectiveFrom);
    }
    ReserveParameter saved =
        repository.save(new ReserveParameter(companyId, businessLine, effectiveFrom, terms));
    audit.record(ENTITY, label(saved), AuditAction.CREATE, "Reserve parameters created");
    return saved;
  }

  /**
   * Changes a parameter set that is still pending authorization.
   *
   * @param id id
   * @param terms values
   * @return record
   */
  public ReserveParameter update(Long id, ReserveParameterTerms terms) {
    validate(terms);
    ReserveParameter p = get(id);
    p.update(terms);
    audit.record(ENTITY, label(p), AuditAction.UPDATE, "Reserve parameters changed");
    return p;
  }

  /**
   * Authorizes a parameter set (checker).
   *
   * @param id id
   * @return record
   */
  public ReserveParameter authorize(Long id) {
    ReserveParameter p = get(id);
    p.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, label(p), AuditAction.AUTHORIZE, "Reserve parameters authorized");
    return p;
  }

  /**
   * Deactivates a parameter set: later valuations fall back to the previous effective record.
   *
   * @param id id
   * @return record
   */
  public ReserveParameter deactivate(Long id) {
    ReserveParameter p = get(id);
    p.deactivate();
    audit.record(ENTITY, label(p), AuditAction.DEACTIVATE, "Reserve parameters deactivated");
    return p;
  }

  private static void validate(ReserveParameterTerms t) {
    if (t.ibnrMethod() == IbnrMethod.RATE && t.ibnrRate().compareTo(HUNDRED) > 0) {
      throw new BusinessRuleException("INVALID_IBNR_RATE", "IBNR rate cannot exceed 100 %");
    }
    if (t.ibnrMethod() == IbnrMethod.CHAIN_LADDER
        && (t.triangleBasis() == null || t.developmentPeriod() == null)) {
      throw new BusinessRuleException(
          "CHAIN_LADDER_SETUP", "Chain-ladder needs a triangle basis and a development period");
    }
  }

  private static String label(ReserveParameter p) {
    return p.getBusinessLine() + "@" + p.getEffectiveFrom();
  }
}
