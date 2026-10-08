package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** D0-04 Cost centres and business lines: the values of the dimensions, on Dimensions. */
@Component
public class DimensionUpload extends ConfigUploadHandler {

  static final String DIMENSION = "Dimension";
  static final String CODE = "Code";
  static final String NAME = "Name";
  static final String ACTIVE = "Active";

  private static final String TABLE = "dim_value";
  private static final List<String> DIMENSIONS =
      List.of("COST_CENTER", "BUSINESS_LINE", "DEPARTMENT");
  private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z0-9_-]{1,20}");

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public DimensionUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_DIMENSION";
  }

  @Override
  public String templateId() {
    return "D0-04";
  }

  @Override
  public String title() {
    return "Cost centres and business lines";
  }

  @Override
  public String screen() {
    return "Setup & Administration > Dimensions";
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
    return "Head, Comptrollership";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(DIMENSION, "Dimension of the value", "COST_CENTER")
            .codes(DIMENSIONS.toArray(String[]::new)),
        BulkColumn.required(CODE, "Code of the value", "FIN").format("Up to 20 capitals or digits"),
        BulkColumn.required(NAME, "Name shown on screens and reports", "Finance and Accounting"),
        new BulkColumn(ACTIVE, "N keeps the value for history only", true, Type.YES_NO, "Y"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(DIMENSION) + "|" + row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    oneOf(errors, DIMENSION, row.text(DIMENSION), DIMENSIONS);
    if (!CODE_FORMAT.matcher(row.text(CODE)).matches()) {
      errors.add(error(CODE, "use up to 20 capital letters, digits, - or _"));
    }
    return errors;
  }

  private static Map<String, Object> key(BulkRow row, BulkContext context) {
    return columns(
        "company_id",
        context.companyId(),
        "dimension_type",
        row.text(DIMENSION),
        "code",
        row.text(CODE));
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
        columns("name", row.text(NAME), "active", yes(row, ACTIVE)),
        context,
        "Dimension value");
    return row.text(DIMENSION) + " " + row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select dimension_type, code, name, active from dim_value where company_id = ?"
                + " order by dimension_type, code",
            companyId)
        .stream()
        .map(
            r ->
                exportRow(
                    DIMENSION, r.get("dimension_type"),
                    CODE, r.get("code"),
                    NAME, r.get("name"),
                    ACTIVE, r.get("active")))
        .toList();
  }
}
