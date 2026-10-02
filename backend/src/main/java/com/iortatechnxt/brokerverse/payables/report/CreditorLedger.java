package com.iortatechnxt.brokerverse.payables.report;

import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read model of creditor open items as of a date, shared by the creditors ageing, statement of
 * payables and supplier outstanding reports (one engine, spec section 2).
 *
 * <p>Balance as of a date = original amount − matches dated on or before it; the main (control)
 * account is read from the item's posted journal line of the party.
 */
@Component
@Transactional(readOnly = true)
public class CreditorLedger {

  /** Suppliers, garages and surveyors (vendor sub-ledger). */
  public static final String VENDORS = "VENDORS";

  /** Every party type that the company can owe money to. */
  public static final String ALL_CREDITORS = "ALL_CREDITORS";

  private static final Set<String> VENDOR_TYPES = Set.of("SUPPLIER", "GARAGE", "SURVEYOR");
  private static final Set<String> CREDITOR_TYPES =
      Set.of("SUPPLIER", "GARAGE", "SURVEYOR", "AGENT", "BROKER", "REINSURER", "COINSURER");

  private static final String SQL =
      """
      select i.id, i.party_code, p.name as party_name, p.party_type, p.phone, p.address,
             i.direction, i.document_type, i.document_no, i.document_date, i.due_date,
             i.currency, i.amount, i.base_amount, i.narration,
             i.amount - coalesce((select sum(m.amount) from sl_item_match m
                                  where (m.credit_item_id = i.id or m.debit_item_id = i.id)
                                    and m.match_date <= :asOf), 0) as balance,
             coalesce((select a.code from gl_ledger_entry e join coa_account a on a.id = e.account_id
                       where e.company_id = i.company_id and e.batch_no = i.journal_batch_no
                         and e.party_code = i.party_code
                       order by e.line_no limit 1), '') as main_account
      from sl_open_item i
      join pty_party p on p.id = i.party_id
      where i.company_id = :companyId
        and i.document_date <= :asOf
        and i.status <> 'WRITTEN_OFF'
        and p.party_type in (:types)
        and (cast(:branchId as bigint) is null or i.branch_id = cast(:branchId as bigint))
        and (cast(:partyFrom as varchar) is null or i.party_code >= cast(:partyFrom as varchar))
        and (cast(:partyTo as varchar) is null or i.party_code <= cast(:partyTo as varchar))
      order by i.party_code, i.due_date, i.document_no
      """;

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the read model.
   *
   * @param jdbc JDBC template
   */
  public CreditorLedger(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Party types behind a creditor type selection.
   *
   * @param selection VENDORS, ALL_CREDITORS or a party type
   * @return party types
   */
  public static Set<String> partyTypes(String selection) {
    if (selection == null || VENDORS.equals(selection)) {
      return VENDOR_TYPES;
    }
    return ALL_CREDITORS.equals(selection) ? CREDITOR_TYPES : Set.of(selection);
  }

  /**
   * Loads the open items of creditors with a non-zero balance as of a date.
   *
   * @param q query
   * @return items by party, due date and document
   */
  public List<CreditorItem> openItems(Query q) {
    return openItems(q, Set.of());
  }

  /**
   * Loads the open items of creditors as of a date, keeping some settled items (e.g. items paid by
   * a post-dated cheque that has not been presented yet).
   *
   * @param q query
   * @param alsoKeep ids of items to return even when their balance is zero
   * @return items by party, due date and document
   */
  public List<CreditorItem> openItems(Query q, Set<Long> alsoKeep) {
    MapSqlParameterSource params =
        new MapSqlParameterSource(
            Map.of("companyId", q.companyId(), "asOf", q.asOf(), "types", q.partyTypes()));
    params.addValue("branchId", q.branchId());
    params.addValue("partyFrom", q.partyFrom());
    params.addValue("partyTo", q.partyTo());
    return jdbc.query(SQL, params, (rs, n) -> map(rs)).stream()
        .filter(i -> i.balance().signum() != 0 || alsoKeep.contains(i.id()))
        .filter(i -> ReportParams.inRange(i.mainAccount(), q.mainFrom(), q.mainTo()))
        .toList();
  }

  /**
   * Payables paid by post-dated cheques issued on or before a date and not yet presented, cancelled
   * or replaced at that date (PDC columns of the statement of payables).
   *
   * @param companyId company
   * @param asOf date
   * @return pending PDC per open item id
   */
  public Map<Long, PendingPdc> pendingPdcs(Long companyId, LocalDate asOf) {
    Map<Long, PendingPdc> result = new HashMap<>();
    jdbc.query(
        PDC_SQL,
        Map.of("companyId", companyId, "asOf", asOf),
        (ResultSet rs) -> {
          PendingPdc pdc =
              new PendingPdc(
                  rs.getString("cheque_no"),
                  rs.getObject("cheque_date", LocalDate.class),
                  rs.getBigDecimal("amount"));
          result.merge(rs.getLong("open_item_id"), pdc, PendingPdc::plus);
        });
    return result;
  }

  private static final String PDC_SQL =
      """
      select a.open_item_id, p.cheque_no, p.cheque_date, a.amount
      from pay_voucher_allocation a
      join pay_pdc_issued p on p.voucher_id = a.voucher_id
      where p.company_id = :companyId and p.issue_date <= :asOf
        and (p.presented_on is null or p.presented_on > :asOf)
        and (p.cancelled_on is null or p.cancelled_on > :asOf)
        and (p.replaced_on is null or p.replaced_on > :asOf)
      """;

  /**
   * Post-dated cheque pending against an item.
   *
   * @param chequeNo cheque number(s)
   * @param chequeDate cheque date (latest)
   * @param amount amount allocated to the item
   */
  public record PendingPdc(String chequeNo, LocalDate chequeDate, BigDecimal amount) {

    PendingPdc plus(PendingPdc other) {
      return new PendingPdc(
          chequeNo + "," + other.chequeNo(),
          chequeDate.isAfter(other.chequeDate()) ? chequeDate : other.chequeDate(),
          amount.add(other.amount()));
    }
  }

  private static CreditorItem map(ResultSet rs) throws SQLException {
    BigDecimal amount = rs.getBigDecimal("amount");
    BigDecimal balance = rs.getBigDecimal("balance");
    BigDecimal base =
        amount.signum() == 0
            ? BigDecimal.ZERO
            : Money.round(
                rs.getBigDecimal("base_amount")
                    .multiply(balance)
                    .divide(amount, Money.RATE_SCALE, Money.ROUNDING));
    return new CreditorItem(
        rs.getLong("id"),
        rs.getString("party_code"),
        rs.getString("party_name"),
        rs.getString("party_type"),
        rs.getString("phone"),
        rs.getString("address"),
        rs.getString("main_account"),
        "CREDIT".equals(rs.getString("direction")),
        rs.getString("document_type"),
        rs.getString("document_no"),
        rs.getObject("document_date", LocalDate.class),
        rs.getObject("due_date", LocalDate.class),
        rs.getString("currency"),
        amount,
        balance,
        base,
        rs.getString("narration"));
  }

  /**
   * Filters of the creditor query.
   *
   * @param companyId company
   * @param asOf as-of date
   * @param partyTypes party types
   * @param branchId branch or null
   * @param partyFrom party code from or null
   * @param partyTo party code to or null
   * @param mainFrom main account from or null
   * @param mainTo main account to or null
   */
  public record Query(
      Long companyId,
      LocalDate asOf,
      Set<String> partyTypes,
      Long branchId,
      String partyFrom,
      String partyTo,
      String mainFrom,
      String mainTo) {

    /** Canonical constructor copying the types. */
    public Query {
      partyTypes = Set.copyOf(partyTypes);
    }
  }
}
