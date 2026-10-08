package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** PM-03 Clause library: warranties, clauses, exclusions and deductible wordings. */
@Component
public class ClauseUpload extends ConfigUploadHandler {

  static final String CODE = "Clause code";
  static final String KIND = "Kind";
  static final String LINE = "Line code";
  static final String TITLE = "Title";
  static final String WORDING = "Wording";
  static final String EFFECTIVE = "Effective from / to";

  private static final String TABLE = "cat_clause";
  private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z0-9_]{1,30}");

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public ClauseUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_CLAUSE";
  }

  @Override
  public String templateId() {
    return "PM-03";
  }

  @Override
  public String title() {
    return "Clause library";
  }

  @Override
  public String screen() {
    return "Product Maintenance > Coverages & Clauses > Clause Library";
  }

  @Override
  public String permission() {
    return "PRODUCT_MAINTAIN";
  }

  @Override
  public String approvePermission() {
    return "PRODUCT_AUTHORIZE";
  }

  @Override
  public String filledBy() {
    return "Product Owner, Product Maintenance";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(CODE, "Code of the clause", "FLEET_REPAIR").format("A-Z, 0-9 and _"),
        BulkColumn.required(KIND, "Kind of the clause", "CLAUSE").lov("CLAUSE_KIND"),
        BulkColumn.optional(LINE, "Line of the clause; blank = every line", "MOTOR").master("line"),
        BulkColumn.required(TITLE, "Title of the clause", "Accredited repair shops for fleets"),
        BulkColumn.required(
            WORDING, "Full text", "Repairs are made at the accredited shops of the insurer."),
        BulkColumn.required(
                EFFECTIVE, "First day; last day after a semicolon, blank = open", "01-Jan-2027")
            .format("dd-MMM-yyyy; dd-MMM-yyyy"));
  }

  @Override
  protected Set<String> multiline() {
    return Set.of(WORDING);
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    if (!CODE_FORMAT.matcher(row.text(CODE)).matches()) {
      errors.add(error(CODE, "use up to 30 capital letters, digits or _"));
    }
    if (!db.listValue("CLAUSE_KIND", row.text(KIND))) {
      errors.add(error(KIND, row.text(KIND) + " is not a clause kind"));
    }
    if (row.text(LINE) != null
        && !db.exists("select 1 from cat_product_line where code = ?", row.text(LINE))) {
      errors.add(error(LINE, row.text(LINE) + " is not a line"));
    }
    if (UploadCells.dates(row.text(EFFECTIVE)) == null) {
      errors.add(
          error(
              EFFECTIVE, "give the first day as dd-MMM-yyyy and, after a semicolon, the last day"));
    }
    return errors;
  }

  private static Map<String, Object> key(BulkRow row) {
    return columns("code", row.text(CODE));
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.action(TABLE, key(row));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    LocalDate[] dates = UploadCells.dates(row.text(EFFECTIVE));
    db.upsert(
        TABLE,
        key(row),
        columns(
            "kind", row.text(KIND),
            "line_code", row.text(LINE),
            "title", row.text(TITLE),
            "wording", row.text(WORDING),
            "effective_from", dates[0],
            "effective_to", dates[1]),
        context,
        "Clause");
    return row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select code, kind, line_code, title, wording, effective_from, effective_to from cat_clause"
                + " where record_status = 'ACTIVE' order by code")
        .stream()
        .map(
            r ->
                exportRow(
                    CODE, r.get("code"),
                    KIND, r.get("kind"),
                    LINE, r.get("line_code"),
                    TITLE, r.get("title"),
                    WORDING, r.get("wording"),
                    EFFECTIVE, UploadCells.range(r.get("effective_from"), r.get("effective_to"))))
        .toList();
  }
}
