package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** D0-03 Holiday calendar: public and company holidays, on Holiday Calendar. */
@Component
public class HolidayUpload extends ConfigUploadHandler {

  static final String BRANCH = "Branch code";
  static final String DATE = "Date";
  static final String DESCRIPTION = "Description";

  private static final String TABLE = "org_holiday";

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public HolidayUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_HOLIDAY";
  }

  @Override
  public String templateId() {
    return "D0-03";
  }

  @Override
  public String title() {
    return "Holiday calendar";
  }

  @Override
  public String screen() {
    return "Setup & Administration > Holiday Calendar";
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
    return "Head, Comptrollership; Human Resources";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.optional(BRANCH, "Branch the holiday applies to; blank = every branch", "")
            .master("branch"),
        new BulkColumn(DATE, "Date of the holiday", true, Type.DATE, "2028-01-01"),
        BulkColumn.required(DESCRIPTION, "Name of the holiday", "New Year's Day"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(BRANCH) + "|" + row.text(DATE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    if (row.text(BRANCH) != null && db.branchId(context.companyId(), row.text(BRANCH)).isEmpty()) {
      errors.add(error(BRANCH, row.text(BRANCH) + " is not a branch of the company"));
    }
    return errors;
  }

  private Map<String, Object> key(BulkRow row, BulkContext context) {
    Long branch =
        row.text(BRANCH) == null
            ? null
            : db.branchId(context.companyId(), row.text(BRANCH)).orElse(null);
    return columns(
        "company_id", context.companyId(), "branch_id", branch, "holiday_date", row.date(DATE));
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
        columns("description", row.text(DESCRIPTION)),
        context,
        "Holiday");
    return row.text(DATE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select b.code as branch_code, h.holiday_date, h.description from org_holiday h"
                + " left join org_branch b on b.id = h.branch_id where h.company_id = ?"
                + " order by h.holiday_date, b.code",
            companyId)
        .stream()
        .map(
            r ->
                exportRow(
                    BRANCH, r.get("branch_code"),
                    DATE, r.get("holiday_date"),
                    DESCRIPTION, r.get("description")))
        .toList();
  }
}
