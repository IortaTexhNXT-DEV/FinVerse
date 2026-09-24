package com.iortatechnxt.brokerverse.receivables.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.receivables.api.dto.StatementImportRequest;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatement;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatementLine;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatementLineRepository;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatementRepository;
import com.iortatechnxt.brokerverse.receivables.service.BankStatementParser.ParsedStatement;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Imports bank statements (CSV) and lists them with their lines. */
@Service
@Transactional
public class BankStatementService {

  private static final String ENTITY = "BankStatement";
  private static final int MAX_REF = 60;

  private final BankStatementRepository statements;
  private final BankStatementLineRepository lines;
  private final BankAccountDirectory banks;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param statements statement repository
   * @param lines line repository
   * @param banks bank account directory
   * @param audit audit trail
   */
  public BankStatementService(
      BankStatementRepository statements,
      BankStatementLineRepository lines,
      BankAccountDirectory banks,
      AuditTrailService audit) {
    this.statements = statements;
    this.lines = lines;
    this.banks = banks;
    this.audit = audit;
  }

  /**
   * Imports a statement for a GL bank account.
   *
   * @param r request
   * @return statement
   */
  public BankStatement importStatement(StatementImportRequest r) {
    banks.require(r.companyId(), r.bankAccountCode(), null);
    ParsedStatement parsed = BankStatementParser.parse(r.content(), r.openingBalance());
    String ref = reference(r, parsed);
    if (statements.existsByCompanyIdAndBankAccountCodeAndStatementRef(
        r.companyId(), r.bankAccountCode(), ref)) {
      throw new DuplicateResourceException(ENTITY, ref);
    }
    BankStatement statement =
        statements.save(
            new BankStatement(
                r.companyId(), r.bankAccountCode(), ref, r.fileName(), parsed.summary()));
    lines.saveAll(parsed.lines().stream().map(l -> new BankStatementLine(statement, l)).toList());
    audit.record(
        ENTITY,
        ref,
        AuditAction.CREATE,
        "Imported " + parsed.lines().size() + " lines for " + r.bankAccountCode());
    return statement;
  }

  /**
   * Lists statements.
   *
   * @param companyId company
   * @param bankAccountCode bank account (null = all)
   * @return statements newest first
   */
  @Transactional(readOnly = true)
  public List<BankStatement> list(Long companyId, String bankAccountCode) {
    return statements.findByCompanyIdOrderByPeriodToDescIdDesc(companyId).stream()
        .filter(s -> bankAccountCode == null || s.getBankAccountCode().equals(bankAccountCode))
        .toList();
  }

  /**
   * Lists the lines of a statement.
   *
   * @param statementId statement
   * @return lines
   */
  @Transactional(readOnly = true)
  public List<BankStatementLine> lines(Long statementId) {
    statements
        .findById(statementId)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, statementId));
    return lines.findByStatementIdOrderByLineNoAsc(statementId);
  }

  private static String reference(StatementImportRequest r, ParsedStatement parsed) {
    if (r.statementRef() != null && !r.statementRef().isBlank()) {
      return r.statementRef().trim();
    }
    if (r.fileName() != null && !r.fileName().isBlank()) {
      String name = r.fileName().trim();
      return name.length() > MAX_REF ? name.substring(0, MAX_REF) : name;
    }
    return r.bankAccountCode() + "-" + parsed.summary().periodTo();
  }
}
