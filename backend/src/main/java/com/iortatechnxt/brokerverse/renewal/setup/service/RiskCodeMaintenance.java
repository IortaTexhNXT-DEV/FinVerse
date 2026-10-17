package com.iortatechnxt.brokerverse.renewal.setup.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCode;
import com.iortatechnxt.brokerverse.renewal.domain.NonRenewableRiskCodeRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Risk Code Maintenance (FRRN.038): every risk code with its description, product line and
 * Renewable or Non-Renewable indicator, effective from today or a later date, a duplicate refused;
 * a change takes effect once another user authorizes it, and the log keeps the previous and the new
 * values. Accounts on a Non-Renewable risk code are Not for Renewal at their generation.
 */
@Service
public class RiskCodeMaintenance {

  /** Message of a duplicate risk code. */
  public static final String DUPLICATE =
      "Duplicate entry detected. The risk code already exists in the list.";

  /** Message of an incomplete entry. */
  public static final String INCOMPLETE =
      "Invalid or Incomplete input. One or more required fields are missing or contain invalid"
          + " data. Please review your input and ensure all mandatory fields are correctly filled"
          + " out.";

  private static final String ENTITY = RenewalSetupService.ENTITY_RISK_CODE;
  private static final String NON_RENEWABLE = "Non-Renewable Risk Code";

  private final NonRenewableRiskCodeRepository codes;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the maintenance.
   *
   * @param codes risk codes
   * @param audit audit trail
   * @param clock clock
   */
  public RiskCodeMaintenance(
      NonRenewableRiskCodeRepository codes, AuditTrailService audit, Clock clock) {
    this.codes = codes;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The maintained risk codes, filtered.
   *
   * @param companyId company
   * @param search risk code fragment
   * @param renewable indicator filter, null for both
   * @return codes
   */
  @Transactional(readOnly = true)
  public List<NonRenewableRiskCode> list(Long companyId, String search, Boolean renewable) {
    String text = search == null ? "" : search.strip();
    return codes.findByCompanyIdOrderByRiskCodeAscIdAsc(companyId).stream()
        .filter(c -> text.isEmpty() || c.getRiskCode().contains(text))
        .filter(c -> renewable == null || c.isRenewable() == renewable)
        .toList();
  }

  /**
   * Adds a risk code to the list.
   *
   * @param companyId company
   * @param entry the entry
   * @return the code, waiting for authorization
   */
  @Transactional
  public NonRenewableRiskCode create(Long companyId, Entry entry) {
    validate(entry, null);
    requireUnique(companyId, entry.riskCode().strip(), null);
    NonRenewableRiskCode code = codes.save(new NonRenewableRiskCode(companyId, data(entry)));
    code.classify(entry.renewable(), entry.description().strip(), blank(entry.remarks()));
    audit.record(ENTITY, code.getId(), AuditAction.CREATE, describe(code));
    return code;
  }

  /**
   * Updates a risk code; it must be authorized again.
   *
   * @param companyId company
   * @param id code
   * @param entry the entry
   * @return the code
   */
  @Transactional
  public NonRenewableRiskCode update(Long companyId, Long id, Entry entry) {
    NonRenewableRiskCode code =
        codes
            .findById(id)
            .filter(c -> c.getCompanyId().equals(companyId))
            .orElseThrow(() -> new BusinessRuleException("RNW_RISK_CODE_NOT_FOUND", INCOMPLETE));
    validate(entry, code.getEffectiveFrom());
    requireUnique(companyId, entry.riskCode().strip(), id);
    String before = describe(code);
    code.update(data(entry));
    code.classify(entry.renewable(), entry.description().strip(), blank(entry.remarks()));
    audit.record(ENTITY, id, AuditAction.UPDATE, before + " -> " + describe(code));
    return code;
  }

  private void validate(Entry e, LocalDate previous) {
    if (e == null
        || blank(e.riskCode()) == null
        || blank(e.description()) == null
        || e.effectiveDate() == null) {
      throw new BusinessRuleException("RNW_RISK_CODE_INCOMPLETE", INCOMPLETE);
    }
    boolean changed = previous == null || !previous.equals(e.effectiveDate());
    if (changed && e.effectiveDate().isBefore(BusinessClock.today(clock))) {
      throw new BusinessRuleException(
          "RNW_RISK_CODE_RETROACTIVE",
          "The effective date must be today or a later date; retroactive dates are not allowed");
    }
  }

  private void requireUnique(Long companyId, String riskCode, Long self) {
    boolean duplicate =
        codes.findByCompanyIdAndRiskCode(companyId, riskCode).stream()
            .anyMatch(c -> !Objects.equals(c.getId(), self) && c.getEffectiveTo() == null);
    if (duplicate) {
      throw new BusinessRuleException("RNW_RISK_CODE_DUPLICATE", DUPLICATE);
    }
  }

  private static NonRenewableRiskCode.Data data(Entry e) {
    return new NonRenewableRiskCode.Data(
        e.riskCode().strip(),
        blank(e.lineCode()),
        e.renewable() ? "Renewable" : NON_RENEWABLE,
        e.effectiveDate(),
        null);
  }

  private static String describe(NonRenewableRiskCode c) {
    return "Risk code "
        + c.getRiskCode()
        + " ("
        + Objects.toString(c.getDescription(), "")
        + "): "
        + (c.isRenewable() ? "Renewable" : "Non-Renewable")
        + " from "
        + c.getEffectiveFrom();
  }

  private static String blank(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  /**
   * An entry of the list.
   *
   * @param riskCode risk code
   * @param description description
   * @param lineCode product line, null for any
   * @param renewable Renewable or Non-Renewable
   * @param effectiveDate effective date, today or later
   * @param remarks remarks
   */
  public record Entry(
      String riskCode,
      String description,
      String lineCode,
      boolean renewable,
      LocalDate effectiveDate,
      String remarks) {}
}
