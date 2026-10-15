package com.iortatechnxt.brokerverse.opsledger.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Invoice Master List values of an invoice that belong to its account (BR-175, FR-CR-090):
 * cover and version number, the Marketing and Processing assignees, the placement status, the
 * approval by Processing and the cancellation. Read for one page of the invoice ledger at a time.
 */
@Service
@Transactional(readOnly = true)
public class InvoiceMasterFacts {

  // The latest Processing approval (validate, or direct booking of an issued policy) of the
  // account's NB_ACCOUNT case gives the Processing assignee and the approval time; while none
  // exists the case assignee is the Processing assignee.
  private static final String SQL =
      """
      select a.id, a.status, a.product_version_no, a.account_officer, a.cancelled_at,
             a.cancellation_reason, l.cover_no, l.legacy_package_version,
             c.assignee, h.actor as approved_by, h.occurred_at as approved_at
      from acc_account a
      left join acc_account_legacy l on l.account_id = a.id
      left join wf_case c
        on c.entity_type = 'Account' and c.workflow_code = 'NB_ACCOUNT'
       and c.entity_id = cast(a.id as varchar)
      left join lateral (
        select x.actor, x.occurred_at from wf_case_history x
        where x.case_id = c.id and x.action in ('validate', 'direct_booking')
        order by x.occurred_at desc, x.id desc limit 1) h on true
      where a.id in (:ids)
      """;

  private final NamedParameterJdbcTemplate jdbc;

  /**
   * Creates the service.
   *
   * @param jdbc database
   */
  public InvoiceMasterFacts(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The values of some accounts.
   *
   * @param accountIds accounts (nulls ignored)
   * @return values per account id
   */
  public Map<Long, Facts> of(Collection<Long> accountIds) {
    List<Long> ids = accountIds.stream().filter(Objects::nonNull).distinct().toList();
    Map<Long, Facts> result = new HashMap<>();
    if (ids.isEmpty()) {
      return result;
    }
    jdbc.query(
        SQL,
        new MapSqlParameterSource("ids", ids),
        (RowCallbackHandler) rs -> result.put(rs.getLong("id"), map(rs)));
    return result;
  }

  private static Facts map(ResultSet rs) throws SQLException {
    Integer version = (Integer) rs.getObject("product_version_no");
    if (version == null) {
      version = (Integer) rs.getObject("legacy_package_version");
    }
    String approvedBy = rs.getString("approved_by");
    Timestamp approvedAt = rs.getTimestamp("approved_at");
    java.sql.Date cancelled = rs.getDate("cancelled_at");
    return new Facts(
        rs.getString("cover_no"),
        version,
        rs.getString("account_officer"),
        approvedBy != null ? approvedBy : rs.getString("assignee"),
        rs.getString("status"),
        approvedAt == null ? null : approvedAt.toInstant(),
        cancelled == null ? null : cancelled.toLocalDate(),
        rs.getString("cancellation_reason"));
  }

  /**
   * Account values of an invoice.
   *
   * @param coverNo cover number (accounts taken over from the legacy system), null otherwise
   * @param versionNo package version of the account
   * @param marketing Marketing assignee (account officer)
   * @param processing Processing assignee: who approved the account, else who holds its case
   * @param placementStatus account status (placement stage)
   * @param approvedAt approval of the account by Processing
   * @param cancelledOn cancellation date of the cover, null while in force
   * @param cancellationReason reason of the cancellation
   */
  public record Facts(
      String coverNo,
      Integer versionNo,
      String marketing,
      String processing,
      String placementStatus,
      Instant approvedAt,
      LocalDate cancelledOn,
      String cancellationReason) {}
}
