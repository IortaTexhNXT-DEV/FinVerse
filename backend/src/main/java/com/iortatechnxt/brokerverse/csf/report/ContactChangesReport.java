package com.iortatechnxt.brokerverse.csf.report;

import com.iortatechnxt.brokerverse.csf.service.CsfCodes;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportColumn;
import com.iortatechnxt.brokerverse.report.core.ReportDefinition;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import com.iortatechnxt.brokerverse.report.core.ReportResult;
import com.iortatechnxt.brokerverse.report.core.TabularReportBuilder;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Contact Changes ({@value #CODE}; FR-CSF-041, FRS section 6.1.1): one row per changed field with
 * the values before and after, the verification of the caller, the channel, the agent, the reason
 * and the legacy sync status, sorted by change time; refusals and referrals are included with the
 * value asked for.
 */
@Component
public class ContactChangesReport implements ReportDefinition {

  /** Report code. */
  public static final String CODE = "CSF-CONTACT-CHANGES";

  private static final String SQL =
      "select c.change_no, "
          + CsfReportSupport.time("c.applied_at")
          + " as applied_at, c.client_code || ' ' || c.client_name as client, f.field,"
          + " f.old_value, f.new_value, c.status, v.result as verification, v.matches, v.required,"
          + " c.channel, coalesce(u.full_name, c.agent) as agent, c.reason_code, c.sync_status"
          + " from csf_contact_change c"
          + " left join csf_contact_change_field f on f.change_id = c.id"
          + " left join csf_verification v on v.id = c.verification_id"
          + " left join sec_user u on lower(u.username) = lower(c.agent)"
          + " where c.company_id = :company and "
          + CsfReportSupport.day("c.applied_at")
          + " between :from and :to"
          + " and (:agent = '' or lower(c.agent) = :agent)"
          + " and (:client = '' or upper(c.client_code) = :client)"
          + " order by c.applied_at, c.id, f.field_index";

  private static final Map<String, String> FIELDS =
      Map.of(
          "EMAIL", "E-mail",
          "MOBILE", "Mobile",
          "PHONE", "Phone",
          "ADDRESS_LINE", "Address line",
          "CITY", "City",
          "PROVINCE", "Province",
          "POSTAL_CODE", "Postal code");

  private final CsfReportSupport support;

  /**
   * Creates the report.
   *
   * @param support parameters, SQL and labels
   */
  public ContactChangesReport(CsfReportSupport support) {
    this.support = support;
  }

  @Override
  public ReportMetadata metadata() {
    return ReportMetadata.customerService(
        CODE,
        "Contact Changes",
        "Contact changes with old and new values, verification, agent, reason and sync status",
        CsfReportSupport.parameters(
            ParameterSpec.optional(CsfReportSupport.CLIENT, "Client code", ParameterType.TEXT)));
  }

  @Override
  public ReportResult generate(ReportParameters p) {
    List<Map<String, Object>> rows =
        support.rows(SQL, p, Map.of()).stream().map(this::labelled).toList();
    return TabularReportBuilder.of(p)
        .columns(
            ReportColumn.text("change_no", "Change No."),
            ReportColumn.text("applied_at", "Date / Time"),
            ReportColumn.text("client", "Client"),
            ReportColumn.text("field", "Field"),
            ReportColumn.text("old_value", "Old Value"),
            ReportColumn.text("new_value", "New Value"),
            ReportColumn.text("status", "Status"),
            ReportColumn.text("verification", "Verification"),
            ReportColumn.text("channel", "Channel"),
            ReportColumn.text("agent", "Agent"),
            ReportColumn.text("reason_code", "Reason"),
            ReportColumn.text("sync_status", "Sync Status"))
        .rows(rows)
        .presorted()
        .withoutGrandTotal()
        .build();
  }

  private Map<String, Object> labelled(Map<String, Object> row) {
    Object field = row.get("field");
    if (field != null) {
      String code = field.toString();
      row.put("field", FIELDS.containsKey(code) ? FIELDS.get(code) : labelOf(code));
    }
    Object result = row.get("verification");
    row.put(
        "verification",
        result == null
            ? null
            : CsfReportSupport.words(result)
                + " ("
                + row.get("matches")
                + " of "
                + row.get("required")
                + " required)");
    support.label(row, "channel", CsfCodes.LOV_CHANNEL);
    support.label(row, "reason_code", CsfCodes.LOV_CHANGE_REASON);
    row.put("status", CsfReportSupport.words(row.get("status")));
    row.put("sync_status", CsfReportSupport.words(row.get("sync_status")));
    return row;
  }

  private String labelOf(String code) {
    return support.labelOf(CsfCodes.LOV_REFERRAL_FIELD, code);
  }
}
