package com.iortatechnxt.brokerverse.receivables.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.bulk.service.BulkFileReader;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile;
import com.iortatechnxt.brokerverse.bulk.service.ParsedFile.RawRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.receivables.api.dto.AutoMatchRequest;
import com.iortatechnxt.brokerverse.receivables.api.dto.StatementImportRequest;
import com.iortatechnxt.brokerverse.receivables.domain.BankMatch;
import com.iortatechnxt.brokerverse.receivables.domain.BankStatement;
import com.iortatechnxt.brokerverse.receivables.domain.MatchRule;
import com.iortatechnxt.brokerverse.receivables.domain.MatchRuleRepository;
import com.iortatechnxt.brokerverse.receivables.domain.StatementLayout;
import com.iortatechnxt.brokerverse.receivables.domain.StatementLayoutRepository;
import com.iortatechnxt.brokerverse.receivables.domain.StatementLayoutValues;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spreadsheet bank statements and matching rules (FRBS 3.3.1 / 3.3.2; AQ08): the column mapping of
 * each bank account turns an .xlsx / .ods (or CSV) export of the bank into the standard statement,
 * which is imported and immediately auto-matched; the rule CHECK_NO_AND_AMOUNT of the bank account
 * makes the matching start with cheque number and amount.
 */
@Service
@Transactional
public class StatementFileImportService {

  private static final String ENTITY = "StatementLayout";
  private static final String HEADER = "date,description,reference,debit,credit,balance";
  private static final String ROW = "Row ";

  private final StatementLayoutRepository layouts;
  private final MatchRuleRepository rules;
  private final BulkFileReader reader;
  private final BankStatementService statements;
  private final BankMatchingService matching;
  private final BankAccountDirectory banks;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param layouts statement layouts
   * @param rules matching rules
   * @param reader spreadsheet reader
   * @param statements statement import
   * @param matching auto matching
   * @param banks bank accounts
   * @param audit audit trail
   */
  public StatementFileImportService(
      StatementLayoutRepository layouts,
      MatchRuleRepository rules,
      BulkFileReader reader,
      BankStatementService statements,
      BankMatchingService matching,
      BankAccountDirectory banks,
      AuditTrailService audit) {
    this.layouts = layouts;
    this.rules = rules;
    this.reader = reader;
    this.statements = statements;
    this.matching = matching;
    this.banks = banks;
    this.audit = audit;
  }

  /**
   * Layouts of a company.
   *
   * @param companyId company
   * @return layouts
   */
  @Transactional(readOnly = true)
  public List<StatementLayout> layouts(Long companyId) {
    return layouts.findByCompanyIdOrderByBankAccountCode(companyId);
  }

  /**
   * Creates or changes the layout of a bank account.
   *
   * @param companyId company
   * @param bankAccountCode GL bank account
   * @param values mapping
   * @return layout
   */
  public StatementLayout saveLayout(
      Long companyId, String bankAccountCode, StatementLayoutValues values) {
    banks.require(companyId, bankAccountCode, null);
    if (values.amountColumn() == null
        && (values.debitColumn() == null || values.creditColumn() == null)) {
      throw new BusinessRuleException(
          "LAYOUT_AMOUNTS", "Map either a signed amount column or both debit and credit columns");
    }
    try {
      DateTimeFormatter.ofPattern(values.datePattern());
    } catch (IllegalArgumentException ex) {
      throw new BusinessRuleException(
          "LAYOUT_DATE_PATTERN", "Invalid date pattern " + values.datePattern(), ex);
    }
    StatementLayout layout =
        layouts
            .findByCompanyIdAndBankAccountCode(companyId, bankAccountCode)
            .orElseGet(() -> new StatementLayout(companyId, bankAccountCode, values));
    layout.change(values);
    StatementLayout saved = layouts.save(layout);
    audit.record(ENTITY, bankAccountCode, AuditAction.UPDATE, "Statement layout " + values.name());
    return saved;
  }

  /**
   * Whether cheque number and amount are matched first for a bank account.
   *
   * @param companyId company
   * @param bankAccountCode bank account
   * @return true when CHECK_NO_AND_AMOUNT is active
   */
  @Transactional(readOnly = true)
  public boolean chequeNumberFirst(Long companyId, String bankAccountCode) {
    return rules
        .findByCompanyIdAndBankAccountCodeAndRuleCode(
            companyId, bankAccountCode, MatchRule.CHECK_NO_AND_AMOUNT)
        .filter(MatchRule::isActive)
        .isPresent();
  }

  /**
   * Switches the CHECK_NO_AND_AMOUNT rule of a bank account on or off (the standard rules always
   * run after it).
   *
   * @param companyId company
   * @param bankAccountCode bank account
   * @param on whether cheque number and amount are matched first
   */
  public void setChequeNumberFirst(Long companyId, String bankAccountCode, boolean on) {
    banks.require(companyId, bankAccountCode, null);
    MatchRule rule =
        rules
            .findByCompanyIdAndBankAccountCodeAndRuleCode(
                companyId, bankAccountCode, MatchRule.CHECK_NO_AND_AMOUNT)
            .orElseGet(
                () ->
                    rules.save(
                        new MatchRule(
                            companyId, bankAccountCode, MatchRule.CHECK_NO_AND_AMOUNT, 1)));
    rule.setActive(on);
    audit.record(
        ENTITY,
        bankAccountCode,
        AuditAction.UPDATE,
        "Cheque number and amount matching " + (on ? "on" : "off"));
  }

  /**
   * Imports a spreadsheet statement with the layout of its bank account, then auto-matches.
   *
   * @param request company, bank account, file and options
   * @return statement and matches created
   */
  public Result importFile(FileImport request) {
    StatementLayoutValues layout =
        layouts
            .findByCompanyIdAndBankAccountCode(request.companyId(), request.bankAccountCode())
            .map(StatementLayout::values)
            .orElseGet(StatementFileImportService::standardLayout);
    ParsedFile file = reader.read(request.fileName(), request.content());
    String csv = toCsv(file, layout);
    BankStatement statement =
        statements.importStatement(
            new StatementImportRequest(
                request.companyId(),
                request.bankAccountCode(),
                request.statementRef(),
                request.fileName(),
                csv,
                request.openingBalance()));
    List<BankMatch> matches =
        matching.autoMatch(
            new AutoMatchRequest(
                request.companyId(), request.bankAccountCode(), statement.getPeriodTo(), null));
    return new Result(statement, matches.size());
  }

  private static StatementLayoutValues standardLayout() {
    return new StatementLayoutValues(
        "Standard", "date", "description", "reference", "debit", "credit", null, "balance", null);
  }

  private static String toCsv(ParsedFile file, StatementLayoutValues layout) {
    Map<String, String> headers =
        file.headers().stream()
            .collect(Collectors.toMap(h -> h.trim().toLowerCase(Locale.ROOT), h -> h, (a, b) -> a));
    DateTimeFormatter pattern = DateTimeFormatter.ofPattern(layout.datePattern(), Locale.ENGLISH);
    StringBuilder csv = new StringBuilder(HEADER).append('\n');
    for (RawRow row : file.rows()) {
      Cells cells = new Cells(row, headers);
      String[] amounts = amounts(cells, layout, row.rowNo());
      csv.append(
              String.join(
                  ",",
                  date(cells.get(layout.dateColumn()), pattern, row.rowNo()),
                  quote(cells.get(layout.descriptionColumn())),
                  quote(cells.get(layout.referenceColumn())),
                  amounts[0],
                  amounts[1],
                  cells.get(layout.balanceColumn()).replace(",", "")))
          .append('\n');
    }
    return csv.toString();
  }

  private static String[] amounts(Cells cells, StatementLayoutValues layout, int rowNo) {
    if (layout.amountColumn() == null) {
      return new String[] {
        cells.get(layout.debitColumn()).replace(",", ""),
        cells.get(layout.creditColumn()).replace(",", "")
      };
    }
    String text = cells.get(layout.amountColumn()).replace(",", "");
    try {
      BigDecimal amount = new BigDecimal(text);
      return amount.signum() < 0
          ? new String[] {amount.negate().toPlainString(), ""}
          : new String[] {"", amount.toPlainString()};
    } catch (NumberFormatException ex) {
      throw new BusinessRuleException(
          "INVALID_STATEMENT", ROW + rowNo + ": invalid amount '" + text + "'", ex);
    }
  }

  private static String date(String text, DateTimeFormatter pattern, int rowNo) {
    LocalDate parsed = parseOrNull(text, pattern);
    if (parsed == null) {
      parsed = parseOrNull(text, DateTimeFormatter.ISO_LOCAL_DATE);
    }
    if (parsed == null) {
      throw new BusinessRuleException(
          "INVALID_STATEMENT", ROW + rowNo + ": invalid date '" + text + "'");
    }
    return parsed.toString();
  }

  private static LocalDate parseOrNull(String text, DateTimeFormatter pattern) {
    try {
      return LocalDate.parse(text, pattern);
    } catch (DateTimeParseException ex) {
      return null;
    }
  }

  private static String quote(String text) {
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }

  /** Cells of a row looked up by mapped column name (case-insensitive), blank when absent. */
  private record Cells(RawRow row, Map<String, String> headers) {
    String get(String column) {
      return Optional.ofNullable(column)
          .map(c -> headers.get(c.trim().toLowerCase(Locale.ROOT)))
          .map(h -> row.values().get(h))
          .map(String::trim)
          .orElse("");
    }
  }

  /**
   * A spreadsheet statement to import.
   *
   * @param companyId company
   * @param bankAccountCode GL bank account
   * @param fileName file name (decides the format)
   * @param content bytes
   * @param statementRef statement reference, null for the file name
   * @param openingBalance balance before the first line, null to derive it
   */
  public record FileImport(
      Long companyId,
      String bankAccountCode,
      String fileName,
      byte[] content,
      String statementRef,
      BigDecimal openingBalance) {

    /** Defensive copy. */
    public FileImport {
      content = content.clone();
    }

    @Override
    public byte[] content() {
      return content.clone();
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof FileImport f
          && Objects.equals(companyId, f.companyId)
          && Objects.equals(bankAccountCode, f.bankAccountCode)
          && Objects.equals(fileName, f.fileName)
          && Arrays.equals(content, f.content);
    }

    @Override
    public int hashCode() {
      return Objects.hash(companyId, bankAccountCode, fileName, Arrays.hashCode(content));
    }

    @Override
    public String toString() {
      return "FileImport[" + bankAccountCode + ", " + fileName + ", " + content.length + " bytes]";
    }
  }

  /**
   * Outcome of an import.
   *
   * @param statement imported statement
   * @param matched matches created by the auto matching
   */
  public record Result(BankStatement statement, int matched) {}
}
