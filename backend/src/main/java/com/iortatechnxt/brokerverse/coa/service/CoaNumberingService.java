package com.iortatechnxt.brokerverse.coa.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.coa.domain.CoaNumbering;
import com.iortatechnxt.brokerverse.coa.domain.CoaNumberingRepository;
import com.iortatechnxt.brokerverse.coa.domain.GlAccountRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * System-generated account numbers (FRBS 2.3.2): a numbering scheme per parent account proposes the
 * next free child code; the child stays linked to its parent and rolls up to it.
 */
@Service
@Transactional
public class CoaNumberingService {

  private static final String ENTITY = "CoaNumbering";
  private static final Set<String> SEPARATORS = Set.of("", ".", "-");
  private static final int MAX_WIDTH = 6;

  private final CoaNumberingRepository schemes;
  private final GlAccountRepository accounts;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param schemes numbering schemes
   * @param accounts accounts
   * @param audit audit trail
   */
  public CoaNumberingService(
      CoaNumberingRepository schemes, GlAccountRepository accounts, AuditTrailService audit) {
    this.schemes = schemes;
    this.accounts = accounts;
    this.audit = audit;
  }

  /**
   * The schemes of a company.
   *
   * @param companyId company
   * @return schemes by parent code
   */
  @Transactional(readOnly = true)
  public List<CoaNumbering> list(Long companyId) {
    return schemes.findByCompanyIdOrderByParentCode(companyId);
  }

  /**
   * Creates or changes the scheme of a parent account.
   *
   * @param companyId company
   * @param parentCode parent account (must exist)
   * @param separator "" / "." / "-"
   * @param width 1 to 6 digits
   * @param active whether numbers are proposed
   * @return scheme
   */
  public CoaNumbering save(
      Long companyId, String parentCode, String separator, int width, boolean active) {
    String sep = separator == null ? "" : separator;
    if (!SEPARATORS.contains(sep) || width < 1 || width > MAX_WIDTH) {
      throw new BusinessRuleException(
          "INVALID_NUMBERING", "Separator must be none, '.' or '-' and width 1 to 6 digits");
    }
    if (!accounts.existsByCompanyIdAndCode(companyId, parentCode)) {
      throw new ResourceNotFoundException("GL account", parentCode);
    }
    CoaNumbering scheme =
        schemes
            .findByCompanyIdAndParentCode(companyId, parentCode)
            .orElseGet(() -> new CoaNumbering(companyId, parentCode, sep, width));
    scheme.change(sep, width, active);
    CoaNumbering saved = schemes.save(scheme);
    audit.record(
        ENTITY,
        parentCode,
        AuditAction.UPDATE,
        "Numbering under " + parentCode + ": '" + sep + "' + " + width + " digits");
    return saved;
  }

  /**
   * The code of a new account entered without one: the next free child code of its parent.
   *
   * @param companyId company
   * @param parentCode parent account code (required)
   * @return proposed code
   */
  @Transactional(readOnly = true)
  public String proposedCode(Long companyId, String parentCode) {
    if (parentCode == null || parentCode.isBlank()) {
      throw new BusinessRuleException(
          "ACCOUNT_CODE_REQUIRED", "Enter the account code of a top-level account");
    }
    return nextCode(companyId, parentCode.trim());
  }

  /**
   * The next free child code under a parent.
   *
   * @param companyId company
   * @param parentCode parent account code
   * @return proposed code
   */
  @Transactional(readOnly = true)
  public String nextCode(Long companyId, String parentCode) {
    CoaNumbering scheme =
        schemes
            .findByCompanyIdAndParentCode(companyId, parentCode)
            .filter(CoaNumbering::isActive)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "NO_NUMBERING",
                        "Enter the account code: no numbering scheme under " + parentCode));
    int next =
        accounts.codesStartingWith(companyId, parentCode + scheme.getSeparator()).stream()
                .mapToInt(scheme::sequenceOf)
                .max()
                .orElse(0)
            + 1;
    String code = scheme.codeOf(next);
    if (code.length() - parentCode.length() - scheme.getSeparator().length() > scheme.getWidth()) {
      throw new BusinessRuleException(
          "NUMBERING_EXHAUSTED",
          "No free " + scheme.getWidth() + "-digit number under " + parentCode);
    }
    return code;
  }
}
