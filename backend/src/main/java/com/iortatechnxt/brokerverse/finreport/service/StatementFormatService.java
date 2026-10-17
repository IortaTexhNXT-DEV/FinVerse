package com.iortatechnxt.brokerverse.finreport.service;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.finreport.service.FormatLine.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides financial statement formats: company formats from {@code fin_statement_format}, and the
 * built-in {@value #STANDARD} format generated from the account report groups (one numbered
 * schedule per report group) so the MIS statements work on any chart without set-up.
 */
@Service
@Transactional(readOnly = true)
public class StatementFormatService {

  /** Code of the built-in format. */
  public static final String STANDARD = "STANDARD";

  /** Balance sheet statement type. */
  public static final String BALANCE_SHEET = "BS";

  /** Income and expense statement type. */
  public static final String INCOME_EXPENSE = "IE";

  private static final String HEADER_SQL =
      "select id, description, statement_type from fin_statement_format"
          + " where company_id = ? and code = ?";

  private static final String LINES_SQL =
      "select line_no, caption, line_type, schedule_ref, account_from, account_to, sign,"
          + " total_terms from fin_statement_format_line where format_id = ? order by line_no";

  private static final int STEP = 10;

  private final JdbcTemplate jdbc;

  /**
   * Creates the service.
   *
   * @param jdbc JDBC template
   */
  public StatementFormatService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Loads a format.
   *
   * @param companyId company
   * @param code format code ({@value #STANDARD} for the built-in format)
   * @param statementType required type ("BS" or "IE")
   * @param hierarchy chart of accounts (used by the built-in format)
   * @return format
   */
  public StatementFormat load(
      Long companyId, String code, String statementType, AccountHierarchy hierarchy) {
    if (STANDARD.equals(code)) {
      return standard(statementType, hierarchy);
    }
    List<Map<String, Object>> header = jdbc.queryForList(HEADER_SQL, companyId, code);
    if (header.isEmpty()) {
      throw new BusinessRuleException(
          "FORMAT_NOT_FOUND", "Financial statement format " + code + " does not exist");
    }
    Map<String, Object> h = header.get(0);
    if (!statementType.equals(h.get("statement_type"))) {
      throw new BusinessRuleException(
          "FORMAT_TYPE_MISMATCH",
          "Format " + code + " is not a " + statementType + " statement format");
    }
    List<FormatLine> lines =
        jdbc.query(LINES_SQL, (rs, i) -> line(rs), ((Number) h.get("id")).longValue());
    return new StatementFormat(code, (String) h.get("description"), statementType, lines);
  }

  /**
   * Builds the built-in format from the report groups of the chart.
   *
   * @param statementType "BS" or "IE"
   * @param hierarchy chart of accounts
   * @return format
   */
  public static StatementFormat standard(String statementType, AccountHierarchy hierarchy) {
    Builder b = new Builder(hierarchy);
    if (BALANCE_SHEET.equals(statementType)) {
      int assets = b.section("ASSETS", AccountClass.ASSET, 1, "TOTAL ASSETS");
      int liabilities = b.section("LIABILITIES", AccountClass.LIABILITY, -1, "TOTAL LIABILITIES");
      b.total("NET ASSETS", List.of(assets, -liabilities));
      b.heading("REPRESENTED BY");
      List<Integer> equity = b.groups(AccountClass.EQUITY, -1);
      int result = b.add(Type.RESULT, "Surplus / (Deficit) Not Yet Appropriated", null, 1, null);
      List<Integer> funds = new ArrayList<>(equity);
      funds.add(result);
      b.total("SHAREHOLDERS' FUNDS", funds);
      return new StatementFormat(STANDARD, "Standard balance sheet", BALANCE_SHEET, b.lines);
    }
    int income = b.section("INCOME", AccountClass.INCOME, -1, "TOTAL INCOME");
    int expenses = b.section("EXPENSES", AccountClass.EXPENSE, 1, "TOTAL EXPENSES");
    b.total("SURPLUS / (DEFICIT)", List.of(income, -expenses));
    return new StatementFormat(
        STANDARD, "Standard income and expense statement", INCOME_EXPENSE, b.lines);
  }

  private static FormatLine line(ResultSet rs) throws SQLException {
    String terms = rs.getString("total_terms");
    List<Integer> parsed =
        terms == null
            ? List.of()
            : Arrays.stream(terms.split(",")).map(String::trim).map(Integer::valueOf).toList();
    return new FormatLine(
        rs.getInt("line_no"),
        rs.getString("caption"),
        Type.valueOf(rs.getString("line_type")),
        rs.getString("schedule_ref"),
        rs.getString("account_from"),
        rs.getString("account_to"),
        null,
        null,
        rs.getInt("sign"),
        parsed);
  }

  /** Accumulates the lines of the built-in format. */
  private static final class Builder {
    private final AccountHierarchy hierarchy;
    private final List<FormatLine> lines = new ArrayList<>();
    private int schedule;

    Builder(AccountHierarchy hierarchy) {
      this.hierarchy = hierarchy;
    }

    int section(String heading, AccountClass cls, int sign, String totalCaption) {
      heading(heading);
      return total(totalCaption, groups(cls, sign));
    }

    void heading(String caption) {
      add(Type.HEADING, caption, null, 1, null);
    }

    int total(String caption, List<Integer> terms) {
      return add(Type.TOTAL, caption, null, 1, terms);
    }

    List<Integer> groups(AccountClass cls, int sign) {
      Map<String, String> firstCodeByGroup = new LinkedHashMap<>();
      hierarchy.all().stream()
          .filter(n -> n.postable() && n.accountClass() == cls)
          .sorted(Comparator.comparing(AccountNode::code))
          .forEach(n -> firstCodeByGroup.putIfAbsent(groupKey(n), n.code()));
      List<Integer> numbers = new ArrayList<>();
      for (String group : firstCodeByGroup.keySet()) {
        schedule++;
        String reportGroup = group.isEmpty() ? null : group;
        String caption =
            reportGroup == null ? "Other " + cls.name().toLowerCase(Locale.ROOT) : reportGroup;
        int no = lines.size() * STEP + STEP;
        lines.add(
            new FormatLine(
                no,
                caption,
                Type.ACCOUNTS,
                String.valueOf(schedule),
                null,
                null,
                cls,
                reportGroup,
                sign,
                null));
        numbers.add(no);
      }
      return numbers;
    }

    int add(Type type, String caption, String scheduleRef, int sign, List<Integer> terms) {
      int no = lines.size() * STEP + STEP;
      lines.add(
          new FormatLine(no, caption, type, scheduleRef, null, null, null, null, sign, terms));
      return no;
    }

    private static String groupKey(AccountNode n) {
      return Objects.requireNonNullElse(n.reportGroup(), "");
    }
  }
}
