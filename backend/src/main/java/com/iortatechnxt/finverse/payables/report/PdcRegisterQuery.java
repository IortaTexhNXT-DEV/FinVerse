package com.iortatechnxt.finverse.payables.report;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read model of the PDC-issued register for the FIN-PDC-ISS-* reports. Status tests use the dates
 * of the status history, so the register can be reproduced as of any date. Division = branch,
 * Department = department dimension of the voucher, Bank = paying company bank account.
 */
@Component
@Transactional(readOnly = true)
public class PdcRegisterQuery {

  private static final String FILTERS =
      """
        and (cast(:divFrom as varchar) is null or br.code >= cast(:divFrom as varchar))
        and (cast(:divTo as varchar) is null or br.code <= cast(:divTo as varchar))
        and (cast(:deptFrom as varchar) is null or coalesce(p.department, '') >= cast(:deptFrom as varchar))
        and (cast(:deptTo as varchar) is null or coalesce(p.department, '') <= cast(:deptTo as varchar))
        and (cast(:bankFrom as varchar) is null or ba.code >= cast(:bankFrom as varchar))
        and (cast(:bankTo as varchar) is null or ba.code <= cast(:bankTo as varchar))
      """;

  private static final String CHEQUES_SQL =
      """
      select p.cheque_no, p.cheque_date, p.issue_date, p.currency, p.amount, p.base_amount,
             p.party_code, p.payee_name, p.status, p.department, v.voucher_no,
             ba.code as bank_code, ba.name as bank_name, br.code as division,
             coalesce((select a.code from gl_ledger_entry e join coa_account a on a.id = e.account_id
                       where e.company_id = v.company_id and e.batch_no = v.journal_batch_no
                         and e.party_code = v.party_code
                       order by e.line_no limit 1), '') as main_account
      from pay_pdc_issued p
      join pay_voucher v on v.id = p.voucher_id
      join pay_bank_account ba on ba.id = p.bank_account_id
      join org_branch br on br.id = p.branch_id
      where p.company_id = :companyId and p.issue_date between :from and :to
        and (:outstandingOnly = false or (
              (p.presented_on is null or p.presented_on > :asOf)
              and (p.cancelled_on is null or p.cancelled_on > :asOf)
              and (p.replaced_on is null or p.replaced_on > :asOf)
              and p.cheque_date <= :dueBy))
      """
          + FILTERS
          + " order by br.code, coalesce(p.department, ''), ba.code, p.cheque_date, p.cheque_no";

  private static final String CONFIRMATIONS_SQL =
      """
      select p.cheque_no, p.cheque_date, p.presented_on, p.department, br.code as division,
             ba.code as bank_code, ba.name as bank_name, jb.batch_no, jb.value_date, jb.narration,
             jl.line_no, a.code as account_code, a.name as account_name, jl.party_code,
             jl.narration as line_narration, jl.currency, jl.amount, jl.base_amount, jl.side
      from pay_pdc_issued p
      join pay_bank_account ba on ba.id = p.bank_account_id
      join org_branch br on br.id = p.branch_id
      join jnl_batch jb on jb.company_id = p.company_id and jb.batch_no = p.presentation_batch_no
      join jnl_line jl on jl.batch_id = jb.id
      join coa_account a on a.id = jl.account_id
      where p.company_id = :companyId and p.presented_on between :from and :to
      """
          + FILTERS
          + " order by br.code, coalesce(p.department, ''), ba.code, jb.batch_no, jl.line_no";

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the read model.
   *
   * @param jdbc JDBC template
   */
  public PdcRegisterQuery(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Cheques issued in a window, optionally only those outstanding as of a date.
   *
   * @param f filters
   * @return rows (column name to value)
   */
  public List<Map<String, Object>> cheques(Filter f) {
    MapSqlParameterSource params = params(f);
    params.addValue("outstandingOnly", f.outstandingOnly());
    params.addValue("asOf", f.asOf());
    params.addValue("dueBy", f.dueBy());
    return jdbc.query(CHEQUES_SQL, params, (rs, n) -> row(rs, CHEQUE_COLUMNS));
  }

  /**
   * Journal lines of the presentation (confirmation) vouchers dated in a window.
   *
   * @param f filters (from / to = presentation dates)
   * @return rows (column name to value)
   */
  public List<Map<String, Object>> confirmations(Filter f) {
    return jdbc.query(CONFIRMATIONS_SQL, params(f), (rs, n) -> row(rs, CONFIRMATION_COLUMNS));
  }

  private static final List<String> CHEQUE_COLUMNS =
      List.of(
          "cheque_no",
          "cheque_date",
          "issue_date",
          "currency",
          "amount",
          "base_amount",
          "party_code",
          "payee_name",
          "status",
          "department",
          "voucher_no",
          "bank_code",
          "bank_name",
          "division",
          "main_account");

  private static final List<String> CONFIRMATION_COLUMNS =
      List.of(
          "cheque_no",
          "cheque_date",
          "presented_on",
          "department",
          "division",
          "bank_code",
          "bank_name",
          "batch_no",
          "value_date",
          "narration",
          "line_no",
          "account_code",
          "account_name",
          "party_code",
          "line_narration",
          "currency",
          "amount",
          "base_amount",
          "side");

  private static MapSqlParameterSource params(Filter f) {
    MapSqlParameterSource params =
        new MapSqlParameterSource(
            Map.of("companyId", f.companyId(), "from", f.from(), "to", f.to()));
    params.addValue("divFrom", f.divisionFrom());
    params.addValue("divTo", f.divisionTo());
    params.addValue("deptFrom", f.departmentFrom());
    params.addValue("deptTo", f.departmentTo());
    params.addValue("bankFrom", f.bankFrom());
    params.addValue("bankTo", f.bankTo());
    return params;
  }

  private static Map<String, Object> row(ResultSet rs, List<String> columns) throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    for (String c : columns) {
      Object value = rs.getObject(c);
      row.put(c, value instanceof Date d ? d.toLocalDate() : value);
    }
    return row;
  }

  /**
   * Register filters.
   *
   * @param companyId company
   * @param from issue (or presentation) date from
   * @param to issue (or presentation) date to
   * @param outstandingOnly only cheques outstanding as of {@code asOf}
   * @param asOf status date
   * @param dueBy latest cheque date for outstanding cheques
   * @param divisionFrom branch code from
   * @param divisionTo branch code to
   * @param departmentFrom department from
   * @param departmentTo department to
   * @param bankFrom bank account code from
   * @param bankTo bank account code to
   */
  public record Filter(
      Long companyId,
      LocalDate from,
      LocalDate to,
      boolean outstandingOnly,
      LocalDate asOf,
      LocalDate dueBy,
      String divisionFrom,
      String divisionTo,
      String departmentFrom,
      String departmentTo,
      String bankFrom,
      String bankTo) {}
}
