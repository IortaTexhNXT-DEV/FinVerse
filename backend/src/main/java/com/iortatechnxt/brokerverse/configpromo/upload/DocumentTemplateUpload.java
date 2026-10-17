package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * PM-08 Document templates of the packages: a row whose title, text or effective date differs from
 * the latest version of the template adds a new version effective from the date; a row equal to the
 * latest version changes nothing.
 */
@Component
public class DocumentTemplateUpload extends ConfigUploadHandler {
  private static final String TITLE_FIELD = "title";
  private static final String REFERENCE = "reference";

  static final String TEMPLATE = "Template";
  static final String TITLE = "Title";
  static final String TEXT = "Text";
  static final String FIELDS = "Merge fields used";
  static final String FROM = "Effective from";

  private static final Map<String, Set<String>> MERGE_FIELDS = mergeFields();
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.]+)\\s*}}");
  private static final String LATEST =
      "select title, body, effective_from from doc_template where code = ? order by version_no desc limit 1";

  private final DocTemplateService templates;

  /**
   * Creates the handler.
   *
   * @param db database support
   * @param templates document templates
   */
  public DocumentTemplateUpload(UploadSupport db, DocTemplateService templates) {
    super(db);
    this.templates = templates;
  }

  private static Map<String, Set<String>> mergeFields() {
    Map<String, Set<String>> m = new LinkedHashMap<>();
    Set<String> advisory =
        Set.of("productCode", "productName", "versionNo", "effectiveFrom", REFERENCE);
    m.put("PKG_REQUEST_FORM", Set.of(REFERENCE, "requestType", TITLE_FIELD));
    m.put("PKG_QUOTATION_SLIP", Set.of(REFERENCE, "roundNo", "replyBy"));
    m.put("PKG_COMPARATIVE", Set.of(REFERENCE, "roundNo", "variant"));
    m.put("PKG_SLIP", Set.of(REFERENCE, TITLE_FIELD));
    m.put("PKG_ADVISORY", advisory);
    m.put("PKG_RENEWAL_ADVISORY", advisory);
    return m;
  }

  @Override
  public String code() {
    return "CFG_DOCUMENT_TEMPLATE";
  }

  @Override
  public String templateId() {
    return "PM-08";
  }

  @Override
  public String title() {
    return "Package document templates";
  }

  @Override
  public String screen() {
    return "New Business > Document Templates";
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
    return "Product Owner, Product Maintenance";
  }

  @Override
  protected Set<String> multiline() {
    return Set.of(TEXT);
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(TEMPLATE, "Package document template", "PKG_ADVISORY")
            .codes(MERGE_FIELDS.keySet().toArray(String[]::new)),
        BulkColumn.required(TITLE, "Title of the document", "Package advisory"),
        BulkColumn.required(
            TEXT,
            "Text with merge fields in double braces",
            "This is to advise that the package {{productCode}} - {{productName}} is available."),
        BulkColumn.optional(
            FIELDS, "Merge fields of the text, for reference", "productCode, productName"),
        new BulkColumn(FROM, "First day of use of the version", true, Type.DATE, "2028-01-01"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(TEMPLATE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    Set<String> allowed = MERGE_FIELDS.get(row.text(TEMPLATE));
    if (allowed == null) {
      oneOf(errors, TEMPLATE, row.text(TEMPLATE), List.copyOf(MERGE_FIELDS.keySet()));
      return errors;
    }
    if (!db.exists("select 1 from doc_template where code = ?", row.text(TEMPLATE))) {
      errors.add(error(TEMPLATE, row.text(TEMPLATE) + " has no version yet in this environment"));
    }
    Set<String> unknown = new TreeSet<>();
    Matcher m = PLACEHOLDER.matcher(row.text(TEXT));
    while (m.find()) {
      if (!allowed.contains(m.group(1))) {
        unknown.add(m.group(1));
      }
    }
    if (!unknown.isEmpty()) {
      errors.add(
          error(
              TEXT,
              "unknown merge fields "
                  + String.join(", ", unknown)
                  + "; use "
                  + String.join(", ", new TreeSet<>(allowed))));
    }
    return errors;
  }

  private boolean same(BulkRow row) {
    return db.rows(LATEST, row.text(TEMPLATE)).stream()
        .anyMatch(
            r ->
                Objects.equals(r.get(TITLE_FIELD), row.text(TITLE))
                    && Objects.equals(r.get("body"), row.text(TEXT))
                    && Objects.equals(
                        String.valueOf(r.get("effective_from")), String.valueOf(row.date(FROM))));
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return same(row) ? UploadSupport.UPDATE : UploadSupport.ADD;
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    if (same(row)) {
      return row.text(TEMPLATE) + " unchanged";
    }
    return row.text(TEMPLATE)
        + " v"
        + templates
            .newVersion(row.text(TEMPLATE), row.text(TITLE), row.text(TEXT), row.date(FROM))
            .getVersionNo();
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    List<Map<String, String>> rows = new ArrayList<>();
    for (String code : MERGE_FIELDS.keySet()) {
      for (Map<String, Object> r : db.rows(LATEST, code)) {
        Set<String> used = new TreeSet<>();
        Matcher m = PLACEHOLDER.matcher(String.valueOf(r.get("body")));
        while (m.find()) {
          used.add(m.group(1));
        }
        rows.add(
            exportRow(
                TEMPLATE, code,
                TITLE, r.get(TITLE_FIELD),
                TEXT, r.get("body"),
                FIELDS, used.isEmpty() ? null : String.join(", ", used),
                FROM, r.get("effective_from")));
      }
    }
    return rows;
  }
}
