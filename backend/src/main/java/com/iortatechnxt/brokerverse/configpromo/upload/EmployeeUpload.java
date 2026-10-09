package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.util.EmailAddresses;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** D0-05 Employees: the employees of the cost-centre rules and petty cash, on Employees. */
@Component
public class EmployeeUpload extends ConfigUploadHandler {

  static final String NUMBER = "Employee number";
  static final String NAME = "Full name";
  static final String BRANCH = "Branch code";
  static final String COST_CENTRE = "Cost centre";
  static final String POSITION = "Position";
  static final String EMAIL = "E-mail";
  static final String HIRED = "Hired on";

  private static final String TABLE = "org_employee";

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public EmployeeUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_EMPLOYEE";
  }

  @Override
  public String templateId() {
    return "D0-05";
  }

  @Override
  public String title() {
    return "Employees";
  }

  @Override
  public String screen() {
    return "Setup & Administration > Employees";
  }

  @Override
  public String permission() {
    return "EMPLOYEE_MAINTAIN";
  }

  @Override
  public String approvePermission() {
    return "MASTER_AUTHORIZE";
  }

  @Override
  public String filledBy() {
    return "Human Resources; Head, Comptrollership";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(NUMBER, "Employee number; unique", "1203344").format("HR number"),
        BulkColumn.required(NAME, "Name of the employee", "Andrea Mercado"),
        BulkColumn.required(BRANCH, "Home branch", "HO").master("branch"),
        BulkColumn.required(COST_CENTRE, "Cost centre of the employee", "FIN")
            .master("cost centre"),
        BulkColumn.optional(POSITION, "Position title", "Accounting Officer"),
        BulkColumn.optional(
            EMAIL, "For advices and statements", "andrea.mercado@company.example.ph"),
        new BulkColumn(HIRED, "Date of hire", false, Type.DATE, "2019-03-03"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(NUMBER);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    if (db.branchId(context.companyId(), row.text(BRANCH)).isEmpty()) {
      errors.add(error(BRANCH, row.text(BRANCH) + " is not a branch of the company"));
    }
    if (!db.exists(
        "select 1 from dim_value where company_id = ? and dimension_type = 'COST_CENTER' and code = ?",
        context.companyId(),
        row.text(COST_CENTRE))) {
      errors.add(
          error(COST_CENTRE, row.text(COST_CENTRE) + " is not a cost centre of the company"));
    }
    if (row.text(EMAIL) != null && !EmailAddresses.isValid(row.text(EMAIL))) {
      errors.add(error(EMAIL, row.text(EMAIL) + " is not an e-mail address"));
    }
    return errors;
  }

  private static Map<String, Object> key(BulkRow row, BulkContext context) {
    return columns("company_id", context.companyId(), "employee_no", row.text(NUMBER));
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.action(TABLE, key(row, context));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    db.upsert(
        TABLE,
        key(row, context),
        columns(
            "full_name", row.text(NAME),
            "branch_id", db.branchId(context.companyId(), row.text(BRANCH)).orElse(null),
            "cost_center", row.text(COST_CENTRE),
            "position", row.text(POSITION),
            "email", row.text(EMAIL),
            "hired_on", row.date(HIRED),
            "active", true),
        context,
        "Employee");
    return row.text(NUMBER);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select e.employee_no, e.full_name, b.code as branch_code, e.cost_center, e.position,"
                + " e.email, e.hired_on from org_employee e left join org_branch b on b.id = e.branch_id"
                + " where e.company_id = ? order by e.employee_no",
            companyId)
        .stream()
        .map(
            r ->
                exportRow(
                    NUMBER, r.get("employee_no"),
                    NAME, r.get("full_name"),
                    BRANCH, r.get("branch_code"),
                    COST_CENTRE, r.get("cost_center"),
                    POSITION, r.get("position"),
                    EMAIL, r.get("email"),
                    HIRED, r.get("hired_on")))
        .toList();
  }
}
