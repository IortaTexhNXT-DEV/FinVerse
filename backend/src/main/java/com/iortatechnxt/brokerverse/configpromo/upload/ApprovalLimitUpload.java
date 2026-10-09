package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * D0-15 Approval limits and matrices: the levels of the Submitted Policies approval matrix (IAAF
 * and terms of reference by segment and sum insured band) and the approval thresholds of Employee
 * Benefits (sum insured or annual premium by benefit line). The approval routes of Sanction
 * Screening belong to a configuration version of Compliance Setup and the disbursement approvals
 * follow the authorisation limit of the user (UA-03), so those rows are refused with where to set
 * them.
 */
@Component
public class ApprovalLimitUpload extends ConfigUploadHandler {
  private static final String TSI = "TSI";
  private static final String TOR = "TOR";
  private static final String IAAF = "IAAF";

  static final String MODULE = "Module";
  static final String DOCUMENT = "Document or transaction";
  static final String SEGMENT = "Segment or case type";
  static final String FROM = "From amount";
  static final String TO = "To amount";
  static final String LEVEL = "Approval level";
  static final String ROLE = "Approver role";
  static final String APPROVER = "Named approver";
  static final String TITLE = "Signatory title";

  static final String SUBMITTED = "Submitted Policies";
  static final String BENEFITS = "Employee Benefits";
  static final String SCREENING = "Sanction Screening";
  static final String DISBURSEMENT = "Disbursement";

  private static final String SBM_TABLE = "sbm_approval_matrix";
  private static final int MAX_LEVEL = 9;
  private static final String EB_TABLE = "eb_threshold_rule";
  private static final List<String> MODULES = List.of(SUBMITTED, BENEFITS, SCREENING, DISBURSEMENT);
  private static final Map<String, String> SBM_DOCUMENTS =
      Map.of(IAAF, IAAF, "INSURER APPROVAL", IAAF, TOR, TOR, "TERMS OF REFERENCE", TOR);
  private static final Map<String, String> EB_MEASURES =
      Map.of(
          TSI,
          TSI,
          "SUM INSURED",
          TSI,
          "TOTAL SUM INSURED",
          TSI,
          "ANNUAL_PREMIUM",
          "ANNUAL_PREMIUM",
          "ANNUAL PREMIUM",
          "ANNUAL_PREMIUM");

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public ApprovalLimitUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_APPROVAL_LIMIT";
  }

  @Override
  public String templateId() {
    return "D0-15";
  }

  @Override
  public String title() {
    return "Approval limits and matrices";
  }

  @Override
  public String screen() {
    return "Submitted Policies Setup and EB Setup > Approval Matrix";
  }

  @Override
  public String permission() {
    return "MASTER_MAINTAIN";
  }

  @Override
  public String approvePermission() {
    return "MASTER_AUTHORIZE";
  }

  @Override
  public String filledBy() {
    return "Owners of the modules; Head, Comptrollership for the amounts";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(MODULE, "Module of the approval", SUBMITTED)
            .values(MODULES.toArray(String[]::new)),
        BulkColumn.required(DOCUMENT, "What is approved", "Terms of reference")
            .allowed(
                "IAAF or Terms of reference; for Employee Benefits: Sum insured or Annual premium"),
        BulkColumn.optional(SEGMENT, "Segment or benefit line of the row", "CBG"),
        new BulkColumn(
            FROM, "Lower bound (sum insured, premium or amount)", false, Type.NUMBER, "0"),
        new BulkColumn(TO, "Upper bound; blank = no limit", false, Type.NUMBER, "10000000"),
        new BulkColumn(LEVEL, "Order of the approval", true, Type.NUMBER, "1")
            .allowed("A whole number from 1 to 9"),
        BulkColumn.required(ROLE, "Permission held by the approvers of the level", "SBM_PROCESS")
            .allowed("A permission of the role matrix (UA-02)"),
        BulkColumn.optional(APPROVER, "User ID, only when one person approves", ""),
        BulkColumn.optional(TITLE, "Title printed on the document", "Unit Head, Combank"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return String.join(
        "|",
        String.valueOf(row.text(MODULE)),
        String.valueOf(row.text(DOCUMENT)),
        String.valueOf(row.text(SEGMENT)),
        String.valueOf(row.text(LEVEL)),
        String.valueOf(row.text(FROM)));
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    String module = row.text(MODULE);
    oneOf(errors, MODULE, module, MODULES);
    check(
        errors,
        SCREENING.equals(module),
        MODULE,
        "the approval routes of Sanction Screening are set in a draft configuration version on Compliance Setup");
    check(
        errors,
        DISBURSEMENT.equals(module),
        MODULE,
        "disbursements follow the authorisation limit of the user (UA-03)");
    if (!errors.isEmpty()) {
      return errors;
    }
    whole(errors, LEVEL, row.number(LEVEL), 1, MAX_LEVEL);
    check(
        errors,
        Arrays.stream(Permission.values()).noneMatch(p -> p.name().equals(row.text(ROLE))),
        ROLE,
        row.text(ROLE) + " is not a permission; give the permission the approvers hold");
    String approver = row.text(APPROVER);
    check(
        errors,
        approver != null && !db.exists("select 1 from sec_user where username = ?", approver),
        APPROVER,
        approver + " is not a user");
    if (SUBMITTED.equals(module)) {
      validateSubmitted(row, errors);
    } else {
      validateBenefits(row, errors);
    }
    return errors;
  }

  private void validateSubmitted(BulkRow row, List<String> errors) {
    if (document(row) == null) {
      errors.add(error(DOCUMENT, "use IAAF or Terms of reference"));
    }
    if (row.text(SEGMENT) != null && !db.listValue("SBM_SEGMENT", row.text(SEGMENT))) {
      errors.add(error(SEGMENT, row.text(SEGMENT) + " is not a segment of Submitted Policies"));
    }
    BigDecimal from = row.number(FROM) == null ? BigDecimal.ZERO : row.number(FROM);
    if (from.signum() < 0) {
      errors.add(error(FROM, "cannot be negative"));
    }
    if (row.number(TO) != null && row.number(TO).compareTo(from) < 0) {
      errors.add(error(TO, "is below the from amount"));
    }
    if (row.text(TITLE) == null) {
      errors.add(error(TITLE, "mandatory for the documents of Submitted Policies"));
    }
  }

  private void validateBenefits(BulkRow row, List<String> errors) {
    if (measure(row) == null) {
      errors.add(error(DOCUMENT, "use Sum insured or Annual premium"));
    }
    if (row.text(SEGMENT) != null && !db.listValue("EB_BENEFIT_LINE", row.text(SEGMENT))) {
      errors.add(error(SEGMENT, row.text(SEGMENT) + " is not a benefit line"));
    }
    if (row.number(FROM) == null || row.number(FROM).signum() <= 0) {
      errors.add(error(FROM, "the threshold above which the level approves; must be above zero"));
    }
    if (row.text(TO) != null) {
      errors.add(error(TO, "leave blank: a threshold of Employee Benefits has no upper bound"));
    }
  }

  private static String document(BulkRow row) {
    return SBM_DOCUMENTS.get(row.text(DOCUMENT).toUpperCase(Locale.ROOT));
  }

  private static String measure(BulkRow row) {
    return EB_MEASURES.get(row.text(DOCUMENT).toUpperCase(Locale.ROOT));
  }

  private static Map<String, Object> sbmKey(BulkRow row, BulkContext context) {
    return columns(
        "company_id", context.companyId(),
        "document", document(row),
        "segment", row.text(SEGMENT),
        "level", row.number(LEVEL).intValue(),
        "tsi_from", row.number(FROM) == null ? BigDecimal.ZERO : row.number(FROM));
  }

  private static Map<String, Object> ebKey(BulkRow row, BulkContext context) {
    return columns(
        "company_id", context.companyId(),
        "benefit_line", row.text(SEGMENT),
        "measure", measure(row),
        "approval_level", row.number(LEVEL).intValue());
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return SUBMITTED.equals(row.text(MODULE))
        ? db.action(SBM_TABLE, sbmKey(row, context))
        : db.action(EB_TABLE, ebKey(row, context));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    if (SUBMITTED.equals(row.text(MODULE))) {
      db.upsert(
          SBM_TABLE,
          sbmKey(row, context),
          columns(
              "tsi_to", row.number(TO),
              "permission", row.text(ROLE),
              "approver_username", row.text(APPROVER),
              "signatory_title", row.text(TITLE)),
          context,
          "Submitted policies approval matrix");
      return document(row) + " level " + row.text(LEVEL);
    }
    String currency =
        db.text("select base_currency from org_company where id = ?", context.companyId())
            .orElse(null);
    Map<String, Object> values =
        columns(
            "amount", row.number(FROM),
            "currency", currency,
            "approver_permission", row.text(ROLE),
            "description", row.text(TITLE));
    if (UploadSupport.ADD.equals(db.action(EB_TABLE, ebKey(row, context)))) {
      values.put("effective_from", context.businessDate());
    }
    db.upsert(EB_TABLE, ebKey(row, context), values, context, "EB approval threshold");
    return measure(row) + " level " + row.text(LEVEL);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    List<Map<String, String>> rows = new ArrayList<>();
    for (Map<String, Object> r :
        db.rows(
            "select document, segment, tsi_from, tsi_to, level, permission, approver_username,"
                + " signatory_title from sbm_approval_matrix where company_id = ?"
                + " and record_status = 'ACTIVE' order by document, segment nulls first, level, tsi_from",
            companyId)) {
      rows.add(
          exportRow(
              MODULE, SUBMITTED,
              DOCUMENT, TOR.equals(r.get("document")) ? "Terms of reference" : IAAF,
              SEGMENT, r.get("segment"),
              FROM, r.get("tsi_from"),
              TO, r.get("tsi_to"),
              LEVEL, r.get("level"),
              ROLE, r.get("permission"),
              APPROVER, r.get("approver_username"),
              TITLE, r.get("signatory_title")));
    }
    for (Map<String, Object> r :
        db.rows(
            "select benefit_line, measure, amount, approval_level, approver_permission, description"
                + " from eb_threshold_rule where company_id = ? and record_status = 'ACTIVE'"
                + " and effective_to is null order by measure, benefit_line nulls first, approval_level",
            companyId)) {
      rows.add(
          exportRow(
              MODULE, BENEFITS,
              DOCUMENT, TSI.equals(r.get("measure")) ? "Sum insured" : "Annual premium",
              SEGMENT, r.get("benefit_line"),
              FROM, r.get("amount"),
              LEVEL, r.get("approval_level"),
              ROLE, r.get("approver_permission"),
              TITLE, r.get("description")));
    }
    return rows;
  }
}
