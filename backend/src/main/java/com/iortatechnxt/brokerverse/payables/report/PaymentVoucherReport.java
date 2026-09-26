package com.iortatechnxt.brokerverse.payables.report;

import com.iortatechnxt.brokerverse.common.util.AmountInWords;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportCategory;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import com.iortatechnxt.brokerverse.report.gl.GlReportSupport;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * FIN-AP-VOUCHER – Payment Voucher print (layout of the Src FGL001 voucher): one block per payment
 * voucher with payee, cheque and bank, its accounting entries (Dr payable / Cr bank or PDC
 * clearing), total, amount in words and the Entered / Approved signature lines. Unposted vouchers
 * show the payables being paid instead of journal lines.
 */
@Component
public class PaymentVoucherReport implements ReportDefinition {

  private static final String VOUCHER_NO = "voucherNo";
  private static final String STATUS = "status";
  private static final String ALL = "ALL";
  private static final String NARRATION = "narration";

  private static final String SQL =
      """
      select v.voucher_no, v.voucher_date, v.payee_name, v.party_code, v.payment_mode, v.cheque_no,
             v.cheque_date, v.currency, v.amount, v.status, v.narration, v.created_by, v.created_at,
             v.approved_by, v.approved_at, ba.code as bank_code, ba.name as bank_name,
             jl.line_no, a.code as account_code, a.name as account_name, jl.party_code as line_party,
             jl.cost_center, jl.narration as line_narration, jl.side, jl.amount as line_amount
      from pay_voucher v
      join pay_bank_account ba on ba.id = v.bank_account_id
      left join jnl_batch jb on jb.company_id = v.company_id and jb.batch_no = v.journal_batch_no
      left join jnl_line jl on jl.batch_id = jb.id
      left join coa_account a on a.id = jl.account_id
      where v.company_id = :companyId and v.voucher_date between :from and :to
        and (cast(:voucherNo as varchar) is null or v.voucher_no = cast(:voucherNo as varchar))
        and (cast(:status as varchar) is null or v.status = cast(:status as varchar))
      order by v.voucher_date, v.voucher_no, jl.line_no
      """;

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the report.
   *
   * @param jdbc JDBC template
   */
  public PaymentVoucherReport(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ReportMetadata metadata() {
    return new ReportMetadata(
            "FIN-AP-VOUCHER",
            "Payment Voucher",
            ReportCategory.RECEIVABLES_PAYABLES,
            "Printable payment vouchers with entries, amount in words and signatures (FGL001 layout)",
            List.of(
                GlReportSupport.companyParam(),
                GlReportSupport.fromParam().withDefault("YEAR_START"),
                GlReportSupport.toParam(),
                ParameterSpec.optional(VOUCHER_NO, "Voucher No", ParameterType.TEXT),
                ParameterSpec.select(
                    STATUS,
                    "Status",
                    List.of(ALL, "DRAFT", "PENDING_APPROVAL", "APPROVED", "VOIDED", "CANCELLED"),
                    ALL)),
            Permission.REPORT_VIEW)
        .asDocument(); // the printed voucher form: Word and PDF
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("companyId", p.longValue(GlReportSupport.COMPANY));
    params.addValue("from", p.date(GlReportSupport.FROM));
    params.addValue("to", p.date(GlReportSupport.TO));
    params.addValue(VOUCHER_NO, p.optionalText(VOUCHER_NO).orElse(null));
    String status = p.text(STATUS);
    params.addValue(STATUS, ALL.equals(status) ? null : status);
    List<Map<String, Object>> rows = new ArrayList<>();
    Map<String, String> footers = new LinkedHashMap<>();
    jdbc.query(
        SQL,
        params,
        (ResultSet rs) -> {
          rows.add(line(rs));
          footers.putIfAbsent(rs.getString("voucher_no"), footer(rs));
        });
    TabularReportBuilder builder =
        TabularReportBuilder.of(p)
            .columns(
                ReportColumn.text("account", "Main A/c"),
                ReportColumn.text("accountName", "Main A/c Description"),
                ReportColumn.text("subAccount", "Sub A/c"),
                ReportColumn.text("costCenter", "Dept"),
                ReportColumn.text(NARRATION, "Narration"),
                ReportColumn.amount("debit", "Amount DR"),
                ReportColumn.amount("credit", "Amount CR"))
            .groupBy("voucher", "Voucher")
            .presorted()
            .withoutGrandTotal()
            .rows(rows);
    footers.values().forEach(builder::note);
    return builder.build();
  }

  private static Map<String, Object> line(ResultSet rs) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(
        "voucher",
        rs.getString("voucher_no")
            + " | "
            + rs.getDate("voucher_date").toLocalDate()
            + " | Pay to "
            + rs.getString("payee_name")
            + " ("
            + rs.getString("party_code")
            + ") | "
            + rs.getString("payment_mode")
            + (rs.getString("cheque_no") == null ? "" : " No. " + rs.getString("cheque_no"))
            + " dated "
            + rs.getDate("cheque_date").toLocalDate()
            + " | "
            + rs.getString("bank_code")
            + " | Status "
            + rs.getString("status"));
    if (rs.getString("account_code") == null) {
      row.put(NARRATION, "Not yet posted - " + rs.getString(NARRATION));
      row.put("debit", rs.getBigDecimal("amount"));
      return row;
    }
    BigDecimal amount = rs.getBigDecimal("line_amount");
    row.put("account", rs.getString("account_code"));
    row.put("accountName", rs.getString("account_name"));
    row.put("subAccount", rs.getString("line_party"));
    row.put("costCenter", rs.getString("cost_center"));
    row.put(NARRATION, rs.getString("line_narration"));
    row.put("DEBIT".equals(rs.getString("side")) ? "debit" : "credit", amount);
    return row;
  }

  private static String footer(ResultSet rs) throws SQLException {
    return rs.getString("voucher_no")
        + ": "
        + AmountInWords.spell(rs.getBigDecimal("amount"), rs.getString("currency"))
            .toUpperCase(Locale.ROOT)
        + " | Total "
        + rs.getBigDecimal("amount").toPlainString()
        + " | Entered by "
        + rs.getString("created_by")
        + " on "
        + rs.getTimestamp("created_at").toLocalDateTime().toLocalDate()
        + " | Approved by "
        + (rs.getString("approved_by") == null ? "________" : rs.getString("approved_by"))
        + (rs.getTimestamp("approved_at") == null
            ? ""
            : " on " + rs.getTimestamp("approved_at").toLocalDateTime().toLocalDate())
        + " | Received by ________";
  }
}
