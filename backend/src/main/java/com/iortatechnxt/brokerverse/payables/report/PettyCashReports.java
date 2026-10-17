package com.iortatechnxt.brokerverse.payables.report;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Read model of petty cash vouchers for FIN-PC-PENDING and FIN-PC-REIMB. */
@Component
@Transactional(readOnly = true)
public class PettyCashReports {

  private static final String VOUCHER_COLUMNS =
      """
      f.code as fund_code, f.name as fund_name, f.gl_account_code, f.imprest_amount, f.cash_balance,
      br.code as branch_code, d.document_no, d.disbursement_date, d.receipt_ref,
      d.expense_account_code, coalesce(a.name, '') as account_name, d.cost_center, d.description,
      d.payee, d.amount, d.created_by, d.created_at
      """;

  private static final String PENDING_SQL =
      "select "
          + VOUCHER_COLUMNS
          + """
          from pay_petty_cash_disbursement d
          join pay_petty_cash_fund f on f.id = d.fund_id
          join org_branch br on br.id = f.branch_id
          left join coa_account a on a.company_id = d.company_id and a.code = d.expense_account_code
          left join pay_petty_cash_reimbursement r on r.id = d.reimbursement_id
          where d.company_id = :companyId and d.status = 'APPROVED' and d.disbursement_date <= :asOf
            and (r.id is null or r.status <> 'APPROVED' or r.claim_date > :asOf)
            and (cast(:fundFrom as varchar) is null or f.code >= cast(:fundFrom as varchar))
            and (cast(:fundTo as varchar) is null or f.code <= cast(:fundTo as varchar))
          order by f.code, d.expense_account_code, d.disbursement_date, d.document_no
          """;

  private static final String REIMBURSED_SQL =
      "select "
          + VOUCHER_COLUMNS
          + """
          , r.document_no as claim_no, r.claim_date, r.journal_batch_no,
            ba.code as bank_code, ba.name as bank_name
          from pay_petty_cash_reimbursement r
          join pay_petty_cash_fund f on f.id = r.fund_id
          join org_branch br on br.id = f.branch_id
          join pay_bank_account ba on ba.id = r.bank_account_id
          join pay_petty_cash_disbursement d on d.reimbursement_id = r.id
          left join coa_account a on a.company_id = d.company_id and a.code = d.expense_account_code
          where r.company_id = :companyId and r.status = 'APPROVED'
            and r.claim_date between :from and :to
          order by f.code, r.claim_date, r.document_no, d.disbursement_date, d.document_no
          """;

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the read model.
   *
   * @param jdbc JDBC template
   */
  public PettyCashReports(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Approved vouchers not reimbursed as of a date.
   *
   * @param companyId company
   * @param asOf date
   * @param fundFrom fund code from (null = all)
   * @param fundTo fund code to (null = all)
   * @return rows (column name to value)
   */
  public List<Map<String, Object>> pending(
      Long companyId, LocalDate asOf, String fundFrom, String fundTo) {
    MapSqlParameterSource params =
        new MapSqlParameterSource(Map.of("companyId", companyId, "asOf", asOf));
    params.addValue("fundFrom", fundFrom);
    params.addValue("fundTo", fundTo);
    return jdbc.query(PENDING_SQL, params, (rs, n) -> voucher(rs));
  }

  /**
   * Vouchers reimbursed by claims approved in a period.
   *
   * @param companyId company
   * @param from claim date from
   * @param to claim date to
   * @return rows (column name to value)
   */
  public List<Map<String, Object>> reimbursed(Long companyId, LocalDate from, LocalDate to) {
    return jdbc.query(
        REIMBURSED_SQL,
        Map.of("companyId", companyId, "from", from, "to", to),
        (rs, n) -> {
          Map<String, Object> row = voucher(rs);
          row.put("claim_no", rs.getString("claim_no"));
          row.put("claim_date", rs.getObject("claim_date", LocalDate.class));
          row.put("journal_batch_no", rs.getString("journal_batch_no"));
          row.put("bank_code", rs.getString("bank_code"));
          row.put("bank_name", rs.getString("bank_name"));
          return row;
        });
  }

  private static Map<String, Object> voucher(ResultSet rs) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("fund_code", rs.getString("fund_code"));
    row.put("fund_name", rs.getString("fund_name"));
    row.put("gl_account_code", rs.getString("gl_account_code"));
    row.put("imprest_amount", rs.getBigDecimal("imprest_amount"));
    row.put("cash_balance", rs.getBigDecimal("cash_balance"));
    row.put("branch_code", rs.getString("branch_code"));
    row.put("document_no", rs.getString("document_no"));
    row.put("disbursement_date", rs.getObject("disbursement_date", LocalDate.class));
    row.put("receipt_ref", rs.getString("receipt_ref"));
    row.put("expense_account_code", rs.getString("expense_account_code"));
    row.put("account_name", rs.getString("account_name"));
    row.put("cost_center", rs.getString("cost_center"));
    row.put("description", rs.getString("description"));
    row.put("payee", rs.getString("payee"));
    row.put("amount", rs.getBigDecimal("amount"));
    row.put("created_by", rs.getString("created_by"));
    row.put("created_at", rs.getObject("created_at", OffsetDateTime.class).toLocalDate());
    return row;
  }
}
