package com.iortatechnxt.brokerverse.renewal.report;

import com.iortatechnxt.brokerverse.nbreport.service.SqlArgs;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalScope;
import com.iortatechnxt.brokerverse.report.core.CodeSet;
import com.iortatechnxt.brokerverse.report.core.ParameterSpec;
import com.iortatechnxt.brokerverse.report.core.ParameterType;
import com.iortatechnxt.brokerverse.report.core.ReportMetadata;
import com.iortatechnxt.brokerverse.report.core.ReportParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Shared parameters, filters and scope of the Renewal reports (FRS section "Reports", report
 * criteria BRD 1.009.2): company, expiry range and the multi-select criteria (each "all", a list,
 * or "all except" a list) over the renewal candidates {@code c}, restricted to the user's scope.
 */
@Component
public class RenewalReportSupport {

  static final String COMPANY = "companyId";
  static final String FROM = "expiryFrom";
  static final String TO = "expiryTo";

  /** Filters on the renewal {@code c}: expiry range, criteria and the user's scope. */
  public static final String FILTERS =
      " and c.company_id = :company"
          + " and (cast(:from as date) is null or c.expiry_date >= :from)"
          + " and (cast(:to as date) is null or c.expiry_date <= :to)"
          + " and (cast(:unit_all as boolean) or ((coalesce(c.owner_unit, '') = any(cast(:unit as varchar[])))"
          + " <> cast(:unit_ex as boolean)))"
          + " and (cast(:head_all as boolean) or ((coalesce(c.unit_head, '') = any(cast(:head as varchar[])))"
          + " <> cast(:head_ex as boolean)))"
          + " and (cast(:risk_all as boolean) or ((coalesce(c.product_code, '') = any(cast(:risk as varchar[])))"
          + " <> cast(:risk_ex as boolean)))"
          + " and (cast(:insurer_all as boolean) or ((coalesce(c.insurer_code, '')"
          + " = any(cast(:insurer as varchar[]))) <> cast(:insurer_ex as boolean)))"
          + " and (cast(:segment_all as boolean) or ((coalesce(c.segment, '') = any(cast(:segment as varchar[])))"
          + " <> cast(:segment_ex as boolean)))"
          + " and (cast(:origin_all as boolean) or ((coalesce(c.business_origin, '')"
          + " = any(cast(:origin as varchar[]))) <> cast(:origin_ex as boolean)))"
          + " and (cast(:officer_all as boolean) or ((coalesce(c.account_officer, '')"
          + " = any(cast(:officer as varchar[]))) <> cast(:officer_ex as boolean)))"
          + " and (cast(:branch_all as boolean) or ((coalesce(c.branch_code, '') = any(cast(:branch as varchar[])))"
          + " <> cast(:branch_ex as boolean)))"
          + " and (cast(:stage_all as boolean) or ((c.stage = any(cast(:stage as varchar[])))"
          + " <> cast(:stage_ex as boolean)))"
          + " and (cast(:scope_all as boolean) or c.owner_unit = any(cast(:scope_units as varchar[]))"
          + " or c.assigned_ao = :scope_user or exists (select 1 from rnw_assignment sa"
          + " where sa.candidate_id = c.id and sa.role = 'AO' and sa.username = :scope_user))";

  private static final List<String[]> CODE_SETS =
      List.of(
          new String[] {"unit", "Unit", "renewal.unit"},
          new String[] {"head", "Unit Head", "renewal.unitHead"},
          new String[] {"risk", "Risk Code", "renewal.riskCode"},
          new String[] {"insurer", "Insurance Company", "renewal.insurer"},
          new String[] {"segment", "Customer Segment", "renewal.segment"},
          new String[] {"origin", "Business Origin", "renewal.origin"},
          new String[] {"officer", "Bank Officer", "renewal.officer"},
          new String[] {"branch", "Bank Branch", "renewal.branch"},
          new String[] {"stage", "Renewal Status", "renewal.stage"});

  private final RenewalScope scope;

  /**
   * Creates the support.
   *
   * @param scope data scope of the user
   */
  public RenewalReportSupport(RenewalScope scope) {
    this.scope = scope;
  }

  /**
   * Metadata with the company, the expiry range, the criteria and further parameters.
   *
   * @param code report code
   * @param title title
   * @param description purpose
   * @param extra further parameters
   * @return metadata
   */
  static ReportMetadata metadata(
      String code, String title, String description, ParameterSpec... extra) {
    List<ParameterSpec> params = new ArrayList<>();
    params.add(ParameterSpec.required(COMPANY, "Company", ParameterType.COMPANY));
    params.add(ParameterSpec.optional(FROM, "Expiry From", ParameterType.DATE));
    params.add(ParameterSpec.optional(TO, "Expiry To", ParameterType.DATE));
    params.addAll(List.of(extra));
    for (String[] set : CODE_SETS) {
      params.add(ParameterSpec.codeSet(set[0], set[1], set[2]));
    }
    return ReportMetadata.renewal(code, title, description, params);
  }

  /**
   * Bind parameters of the filters and the scope.
   *
   * @param p report parameters
   * @return arguments
   */
  SqlArgs args(ReportParameters p) {
    long companyId = p.longValue(COMPANY);
    SqlArgs args =
        SqlArgs.company(companyId)
            .with("from", p.optionalDate(FROM).orElse(null))
            .with("to", p.optionalDate(TO).orElse(null));
    for (String[] set : CODE_SETS) {
      CodeSet codes = p.codeSet(set[0]);
      args.with(set[0] + "_all", codes.isAll())
          .with(set[0], codes.array())
          .with(set[0] + "_ex", codes.exclude());
    }
    return scoped(args, companyId);
  }

  /**
   * Arguments of the filters with every criterion "all" and the user's scope (Renewal Home).
   *
   * @param companyId company
   * @return arguments
   */
  public SqlArgs allOf(long companyId) {
    SqlArgs args = SqlArgs.company(companyId).with("from", null).with("to", null);
    for (String[] set : CODE_SETS) {
      args.with(set[0] + "_all", true)
          .with(set[0], CodeSet.ALL.array())
          .with(set[0] + "_ex", false);
    }
    return scoped(args, companyId);
  }

  private SqlArgs scoped(SqlArgs args, long companyId) {
    RenewalScope.Scope s = scope.current(companyId);
    return args.with("scope_all", s.kind() == RenewalScope.Kind.ALL)
        .with(
            "scope_units",
            s.units().isEmpty() ? new String[] {""} : s.units().toArray(String[]::new))
        .with("scope_user", s.kind() == RenewalScope.Kind.ASSIGNED ? s.username() : "");
  }

  /**
   * Replaces the stage, bucket and disposition codes of a row by their names.
   *
   * @param row row
   * @return the row
   */
  static Map<String, Object> labels(Map<String, Object> row) {
    Object stage = row.get("stage");
    if (stage != null) {
      row.put("stage", RenewalStage.valueOf(stage.toString()).label());
    }
    Object bucket = row.get("bucket");
    if (bucket != null) {
      row.put("bucket", Bucket.valueOf(bucket.toString()).label());
    }
    Object disposition = row.get("disposition");
    if (disposition != null) {
      row.put("disposition", RenewalDisposition.valueOf(disposition.toString()).label());
    }
    return row;
  }
}
