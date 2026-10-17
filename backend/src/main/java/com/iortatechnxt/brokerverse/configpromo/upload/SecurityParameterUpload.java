package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * UA-05 Parameters of sign-in and user access: the value wanted replaces the value of the parameter
 * when the upload is approved by an approver of the security parameters. The value as delivered
 * must match the current value, so a value changed since the workbook was filled is not overwritten
 * unnoticed.
 */
@Component
public class SecurityParameterUpload extends ConfigUploadHandler {

  static final String PARAMETER = "Parameter";
  static final String DELIVERED = "Value as delivered";
  static final String WANTED = "Value wanted";
  static final String REASON = "Reason";
  static final String APPROVED_BY = "Approved by";

  static final List<String> PARAMETERS =
      List.of(
          "LOGIN_MAX_FAILED_ATTEMPTS",
          "SESSION_IDLE_WARNING_MINUTES",
          "SESSION_TIMEOUT_MINUTES",
          "SESSION_EXPIRY_WARNING_MINUTES",
          "AUTH_MODE",
          "USER_ID_PATTERN",
          "USER_ID_FORMAT_TEXT",
          "PASSWORD_HISTORY_COUNT",
          "PASSWORD_MAX_AGE_DAYS",
          "PASSWORD_MIN_AGE_DAYS",
          "UAM_WORKING_HOURS",
          "UAM_ANY_APPROVER",
          "UAM_ROLE_APPLY_ON_APPROVAL",
          "UAM_EXTERNAL_USERS",
          "UAM_DORMANT_DAYS",
          "UAM_DORMANT_NOTICE_DAYS",
          "JOB_FAILURE_RECIPIENTS");

  private final ListParameterRows lists;

  /**
   * Creates the handler.
   *
   * @param db database support
   * @param lists lists and parameters
   */
  public SecurityParameterUpload(UploadSupport db, ListParameterRows lists) {
    super(db);
    this.lists = lists;
  }

  @Override
  public String code() {
    return "CFG_SECURITY_PARAMETER";
  }

  @Override
  public String templateId() {
    return "UA-05";
  }

  @Override
  public String title() {
    return "Sign-in and user access parameters";
  }

  @Override
  public String screen() {
    return "System Administration > System Parameters";
  }

  @Override
  public String permission() {
    return "SYSTEM_PARAMETER_MANAGE";
  }

  @Override
  public String approvePermission() {
    return "SECURITY_PARAMETER_APPROVE";
  }

  @Override
  public String filledBy() {
    return "Information Security; IT";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(PARAMETER, "One row per parameter", "PASSWORD_MAX_AGE_DAYS")
            .codes(PARAMETERS.toArray(String[]::new)),
        BulkColumn.optional(
            DELIVERED, "The value proposed with the system; blank = no value", "90"),
        BulkColumn.optional(WANTED, "The value wanted; blank = no value", "90")
            .format("Same type and range"),
        BulkColumn.optional(REASON, "Policy reference or reason", "Password policy"),
        BulkColumn.required(
            APPROVED_BY, "Information Security or IT approver of the value", "Ramon Dizon"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(PARAMETER);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    String key = row.text(PARAMETER);
    oneOf(errors, PARAMETER, key, PARAMETERS);
    if (!errors.isEmpty()) {
      return errors;
    }
    Optional<String> current = lists.parameterValue(key);
    if (current.isEmpty()) {
      errors.add(error(PARAMETER, key + " is not a parameter of this environment"));
      return errors;
    }
    String delivered = row.text(DELIVERED) == null ? "" : row.text(DELIVERED);
    String wanted = row.text(WANTED) == null ? "" : row.text(WANTED);
    if (!current.get().equals(delivered) && !current.get().equals(wanted)) {
      errors.add(
          error(
              DELIVERED,
              "the parameter is now " + current.get() + "; check the value wanted against it"));
    }
    lists.checkParameter(key, row.text(WANTED)).ifPresent(e -> errors.add(error(WANTED, e)));
    return errors;
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return UploadSupport.UPDATE;
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    boolean changed = lists.saveParameter(row.text(PARAMETER), row.text(WANTED));
    return row.text(PARAMETER) + (changed ? " = " + row.text(WANTED) : " unchanged");
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    List<Map<String, String>> rows = new ArrayList<>();
    for (String key : PARAMETERS) {
      for (Map<String, Object> r :
          db.rows(
              "select p.param_value, coalesce(u.full_name, p.updated_by, p.created_by) as approver"
                  + " from sys_parameter p left join sec_user u on u.username = coalesce(p.updated_by, p.created_by)"
                  + " where p.param_key = ?",
              key)) {
        rows.add(
            exportRow(
                PARAMETER, key,
                DELIVERED, r.get("param_value"),
                WANTED, r.get("param_value"),
                APPROVED_BY, r.get("approver")));
      }
    }
    return rows;
  }
}
