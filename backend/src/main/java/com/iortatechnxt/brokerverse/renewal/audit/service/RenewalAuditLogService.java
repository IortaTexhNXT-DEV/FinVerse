package com.iortatechnxt.brokerverse.renewal.audit.service;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.nbreport.service.NbReportJdbc;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.report.RenewalReportSupport;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit Logs of the Renewal module (BDOI Renewal FRS FRRN.043): every recorded activity on the
 * renewal accounts of the user's scope - the audit trail of the actions, the status changes of the
 * workflow, the dispositions and the classifications with their old and new values - filtered by
 * date range, action type, reference and user, and exported as CSV or Excel named {@code Audit
 * Logs_MMDDYYYY}. The entries are read only: no screen changes or deletes them.
 */
@Service
@Transactional(readOnly = true)
public class RenewalAuditLogService {

  /** Action types of the filter. */
  public static final List<String> ACTION_TYPES =
      List.of("Create", "Update Details", "Update Status", "Approve", "Send", "Delete");

  private static final String SCOPED =
      " from rnw_candidate c where c.company_id = :company" + RenewalReportSupport.FILTERS;

  private static final String SQL =
      "select * from ("
          + "select a.occurred_at, c.renewal_ref as ref, coalesce(c.assured_name, c.client_name) as client,"
          + " case a.action when 'CREATE' then 'Create' when 'DEACTIVATE' then 'Delete'"
          + " when 'AUTHORIZE' then 'Approve' when 'POST' then 'Approve' when 'SUBMIT' then 'Update Status'"
          + " else 'Update Details' end as action_type, a.summary as description,"
          + " null as old_value, null as new_value, a.username as performed_by, null as remarks"
          + " from audit_log a join rnw_candidate c on a.entity_type = 'RenewalCandidate'"
          + " and a.entity_id = c.renewal_ref where c.id in (select c.id"
          + SCOPED
          + ") union all select h.occurred_at, c.renewal_ref, coalesce(c.assured_name, c.client_name),"
          + " 'Update Status', coalesce(t.label, h.action),"
          + " (select s.name from wf_stage s where s.workflow_code = 'RNW_CASE' and s.stage_code = h.from_stage),"
          + " (select s.name from wf_stage s where s.workflow_code = 'RNW_CASE' and s.stage_code = h.to_stage),"
          + " h.actor, coalesce(h.comment, h.reason_code)"
          + " from wf_case w join wf_case_history h on h.case_id = w.id"
          + " join rnw_candidate c on w.entity_type = 'RenewalCandidate' and w.entity_id = cast(c.id as varchar)"
          + " left join wf_transition t on t.workflow_code = 'RNW_CASE' and t.from_stage = h.from_stage"
          + " and t.action = h.action where c.id in (select c.id"
          + SCOPED
          + ") union all select d.created_at, c.renewal_ref, coalesce(c.assured_name, c.client_name),"
          + " 'Update Details', 'Disposition (' || lower(d.source) || ')',"
          + " (select p.code from rnw_disposition p where p.superseded_by = d.id limit 1), d.code,"
          + " d.created_by, coalesce(d.remarks, d.reason_code)"
          + " from rnw_disposition d join rnw_candidate c on c.id = d.candidate_id where c.id in (select c.id"
          + SCOPED
          + ") union all select b.created_at, c.renewal_ref, coalesce(c.assured_name, c.client_name),"
          + " 'Update Details', 'Classification', b.from_bucket, b.to_bucket, b.created_by,"
          + " 'Rules version ' || coalesce(cast(b.rule_set_version as varchar), '-')"
          + " from rnw_bucket_history b join rnw_candidate c on c.id = b.candidate_id where c.id in (select c.id"
          + SCOPED
          + ")) x where (cast(:dfrom as timestamptz) is null or x.occurred_at >= :dfrom)"
          + " and (cast(:dto as timestamptz) is null or x.occurred_at < :dto)"
          + " and (cast(:action as varchar) is null or x.action_type = :action)"
          + " and (cast(:ref as varchar) is null or x.ref = :ref)"
          + " and (cast(:usr as varchar) is null or x.performed_by = :usr)"
          + " order by x.occurred_at desc limit 5000";

  private static final List<String> HEADERS =
      List.of(
          "Timestamp",
          "Module",
          "Reference Number",
          "Client / Assured's Name",
          "Action Type",
          "Description",
          "Old Value",
          "New Value",
          "Performed By",
          "Remarks");

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final NbReportJdbc jdbc;
  private final RenewalReportSupport support;
  private final DocumentComposer composer;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param jdbc SQL
   * @param support data scope
   * @param composer spreadsheet writer
   * @param clock clock
   */
  public RenewalAuditLogService(
      NbReportJdbc jdbc, RenewalReportSupport support, DocumentComposer composer, Clock clock) {
    this.jdbc = jdbc;
    this.support = support;
    this.composer = composer;
    this.clock = clock;
  }

  /**
   * The audit log entries of the filters, latest first.
   *
   * @param companyId company
   * @param q filters
   * @return entries
   */
  public List<Entry> entries(Long companyId, Query q) {
    Map<String, Object> args =
        support
            .allOf(companyId)
            .with(
                "dfrom", q.from() == null ? null : Timestamp.from(BusinessClock.startOf(q.from())))
            .with(
                "dto",
                q.to() == null ? null : Timestamp.from(BusinessClock.startOf(q.to().plusDays(1))))
            .with("action", blank(q.actionType()))
            .with("ref", blank(q.ref()))
            .with("usr", blank(q.user()))
            .map();
    return jdbc.rows(SQL, args).stream().map(RenewalAuditLogService::entry).toList();
  }

  /**
   * The entries of the filters as a file named {@code Audit Logs_MMDDYYYY}.
   *
   * @param companyId company
   * @param q filters
   * @param csv true for CSV, false for Excel
   * @return file
   */
  public MessageFile export(Long companyId, Query q, boolean csv) {
    String name =
        "Audit Logs_" + BusinessClock.today(clock).format(DateTimeFormatter.ofPattern("MMddyyyy"));
    List<List<Object>> rows = new ArrayList<>();
    for (Entry e : entries(companyId, q)) {
      rows.add(
          List.of(
              e.at() == null ? "" : e.at().toString(),
              "Renewal",
              e.ref(),
              nz(e.client()),
              e.actionType(),
              nz(e.description()),
              nz(e.oldValue()),
              nz(e.newValue()),
              nz(e.performedBy()),
              nz(e.remarks())));
    }
    if (csv) {
      String text =
          rows.stream()
              .map(r -> r.stream().map(v -> quote(v.toString())).collect(Collectors.joining(",")))
              .collect(Collectors.joining("\r\n", String.join(",", HEADERS) + "\r\n", "\r\n"));
      return new MessageFile(name + ".csv", "text/csv", text.getBytes(StandardCharsets.UTF_8));
    }
    return new MessageFile(
        name + ".xlsx", XLSX, composer.xlsx(new SheetSpec("Audit Logs", HEADERS, rows)));
  }

  private static Entry entry(Map<String, Object> r) {
    Object at = r.get("occurred_at");
    Instant when = at instanceof Timestamp t ? t.toInstant() : null;
    String description = (String) r.get("description");
    return new Entry(
        when,
        "Renewal",
        (String) r.get("ref"),
        (String) r.get("client"),
        (String) r.get("action_type"),
        description,
        label(description, (String) r.get("old_value")),
        label(description, (String) r.get("new_value")),
        (String) r.get("performed_by"),
        (String) r.get("remarks"));
  }

  /** The name of a disposition or classification code; other values as they are. */
  private static String label(String description, String value) {
    if (value == null || description == null) {
      return value;
    }
    if (description.startsWith("Disposition")) {
      return RenewalDisposition.valueOf(value).label();
    }
    return "Classification".equals(description) ? Bucket.valueOf(value).label() : value;
  }

  private static String blank(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }

  private static String nz(String s) {
    return s == null ? "" : s;
  }

  private static String quote(String v) {
    return "\"" + v.replace("\"", "\"\"") + "\"";
  }

  /**
   * Filters of the audit logs.
   *
   * @param from first date
   * @param to last date
   * @param actionType action type
   * @param ref reference number
   * @param user user name
   */
  public record Query(LocalDate from, LocalDate to, String actionType, String ref, String user) {}

  /**
   * An audit log entry.
   *
   * @param at timestamp
   * @param module module
   * @param ref reference number
   * @param client client or assured's name
   * @param actionType action type
   * @param description description
   * @param oldValue old value
   * @param newValue new value
   * @param performedBy user
   * @param remarks remarks
   */
  public record Entry(
      Instant at,
      String module,
      String ref,
      String client,
      String actionType,
      String description,
      String oldValue,
      String newValue,
      String performedBy,
      String remarks) {}
}
