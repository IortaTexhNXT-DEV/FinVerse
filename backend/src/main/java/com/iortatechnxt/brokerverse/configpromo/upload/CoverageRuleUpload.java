package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Component;

/**
 * PM-02 Coverages and rules: the coverages and perils of a line (Coverages &amp; Clauses), the
 * field rules and the mandatory documents of the lines and products (rule tabs of Products). A
 * field key starting with "item." is a rule on the insured items, any other a rule on the account.
 */
@Component
// Coverages, field rules and document rules of one tab, each with its scope and checks.
@SuppressWarnings("PMD.GodClass")
public class CoverageRuleUpload extends ConfigUploadHandler {
  private static final String REQUIRED_COLUMN = "required";
  private static final String SCOPE_CODE = "scope_code";
  private static final String PATTERN = "PATTERN";
  private static final String RANGE = "RANGE";
  private static final String LOV = "LOV";
  private static final String REQUIRED = "REQUIRED";

  static final String RECORD = "Record";
  static final String SCOPE = "Line code / Risk code";
  static final String CODE = "Code or field key";
  static final String NAME = "Name or label";
  static final String KIND = "Kind or check";
  static final String BASIC = "Basic";
  static final String PARAMETERS = "Parameters";
  static final String DOCUMENT = "Document type";
  static final String MANDATORY = "Mandatory";

  static final String COVERAGE = "COVERAGE";
  static final String FIELD_RULE = "FIELD_RULE";
  static final String DOCUMENT_RULE = "DOCUMENT_RULE";

  private static final List<String> RECORDS = List.of(COVERAGE, FIELD_RULE, DOCUMENT_RULE);
  private static final Map<String, String> CHECKS =
      Map.of(
          "PRESENCE",
          REQUIRED,
          REQUIRED,
          REQUIRED,
          "LIST OF VALUES",
          LOV,
          LOV,
          LOV,
          "NUMBER RANGE",
          RANGE,
          RANGE,
          RANGE,
          "FORMAT",
          PATTERN,
          PATTERN,
          PATTERN);
  private static final Map<String, String> CHECK_LABELS =
      Map.of(REQUIRED, "Presence", LOV, "List of values", RANGE, "Number range", PATTERN, "Format");
  private static final Pattern COVERAGE_CODE = Pattern.compile("[A-Z0-9_]{1,30}");
  private static final String ITEM_PREFIX = "item.";
  private static final String ALL = "*";

  /** A scope of a rule: every product, a line or a product. */
  private record Scope(String scope, String code) {}

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public CoverageRuleUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_COVERAGE_RULE";
  }

  @Override
  public String templateId() {
    return "PM-02";
  }

  @Override
  public String title() {
    return "Coverages, perils and field rules";
  }

  @Override
  public String screen() {
    return "Product Maintenance > Coverages & Clauses";
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
        BulkColumn.required(RECORD, "Coverage, field rule or document rule", COVERAGE)
            .codes(RECORDS.toArray(String[]::new)),
        BulkColumn.required(SCOPE, "Line or product the row applies to; * = every product", "MOTOR")
            .format("Line or risk codes separated by commas"),
        BulkColumn.required(CODE, "Coverage code, or field key of the rule", "OD_THEFT"),
        BulkColumn.required(
            NAME, "Name of the coverage or label of the field", "Own damage and theft"),
        BulkColumn.required(KIND, "Coverage kind, or check of the field rule", "Coverage")
            .allowed(
                "Section, Coverage, Peril, Extension; Presence, List of values, Number range, Format"),
        new BulkColumn(BASIC, "Y when the coverage is basic", false, Type.YES_NO, "Y")
            .when(COVERAGE),
        BulkColumn.optional(PARAMETERS, "List code; minimum and maximum; pattern", "")
            .when("List, range or format rules"),
        BulkColumn.optional(DOCUMENT, "Document type of a document rule", "")
            .when(DOCUMENT_RULE)
            .lov("DOCUMENT_TYPE"),
        new BulkColumn(
            MANDATORY, "Y when the field or document is mandatory", true, Type.YES_NO, "Y"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(RECORD)
        + "|"
        + row.text(SCOPE)
        + "|"
        + row.text(CODE)
        + "|"
        + row.text(DOCUMENT);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    String record = row.text(RECORD);
    oneOf(errors, RECORD, record, RECORDS);
    if (!errors.isEmpty()) {
      return errors;
    }
    if (COVERAGE.equals(record)) {
      validateCoverage(row, errors);
      return errors;
    }
    for (String code : codes(row)) {
      if (scope(code).isEmpty()) {
        errors.add(error(SCOPE, code + " is neither a line nor a product"));
      }
    }
    if (FIELD_RULE.equals(record)) {
      validateFieldRule(row, errors);
    } else if (row.text(DOCUMENT) == null || !db.listValue("DOCUMENT_TYPE", row.text(DOCUMENT))) {
      errors.add(error(DOCUMENT, "give a document type of the list Document type"));
    }
    return errors;
  }

  private void validateCoverage(BulkRow row, List<String> errors) {
    for (String line : codes(row)) {
      if (!db.exists("select 1 from cat_product_line where code = ?", line)) {
        errors.add(error(SCOPE, line + " is not a line; a coverage belongs to a line"));
      }
    }
    if (!COVERAGE_CODE.matcher(row.text(CODE)).matches()) {
      errors.add(error(CODE, "use up to 30 capital letters, digits or _"));
    }
    if (coverageKind(row).isEmpty()) {
      errors.add(error(KIND, row.text(KIND) + " is not a coverage kind"));
    }
  }

  private void validateFieldRule(BulkRow row, List<String> errors) {
    String type = ruleType(row);
    String parameters = row.text(PARAMETERS);
    if (type == null) {
      errors.add(error(KIND, "use Presence, List of values, Number range or Format"));
    } else if (LOV.equals(type)) {
      check(
          errors,
          parameters == null || !db.exists("select 1 from lov_type where code = ?", parameters),
          PARAMETERS,
          "give the code of an existing list");
    } else if (RANGE.equals(type)) {
      check(
          errors,
          range(parameters).isEmpty(),
          PARAMETERS,
          "give the minimum and the maximum separated by ;");
    } else if (PATTERN.equals(type)) {
      check(
          errors,
          !isPattern(parameters),
          PARAMETERS,
          "give the format of the field as a valid pattern");
    }
  }

  private static boolean isPattern(String text) {
    if (text == null) {
      return false;
    }
    try {
      Pattern.compile(text);
      return true;
    } catch (PatternSyntaxException e) {
      return false;
    }
  }

  private List<String> codes(BulkRow row) {
    return Arrays.stream(row.text(SCOPE).split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  private Optional<Scope> scope(String code) {
    if (ALL.equals(code)) {
      return Optional.of(new Scope("ALL", ALL));
    }
    if (db.exists("select 1 from cat_product_line where code = ?", code)) {
      return Optional.of(new Scope("LINE", code));
    }
    if (db.exists("select 1 from cat_product where code = ?", code)) {
      return Optional.of(new Scope("PRODUCT", code));
    }
    return Optional.empty();
  }

  private Optional<String> coverageKind(BulkRow row) {
    return db.text(
        "select code from lov_value where type_code = 'COVERAGE_KIND' and (code = upper(?) or lower(label) = lower(?))",
        row.text(KIND),
        row.text(KIND));
  }

  private static String ruleType(BulkRow row) {
    return CHECKS.get(row.text(KIND).toUpperCase(Locale.ROOT));
  }

  /** The bounds of a number range rule; either may be open. */
  private record Bounds(BigDecimal min, BigDecimal max) {}

  private static final Bounds NO_BOUNDS = new Bounds(null, null);

  private static Optional<Bounds> range(String parameters) {
    List<String> parts = parameters == null ? List.of() : List.of(parameters.split(";", -1));
    if (parts.size() != 2) {
      return Optional.empty();
    }
    BigDecimal min = UploadCells.number(parts.get(0));
    BigDecimal max = UploadCells.number(parts.get(1));
    boolean read = readable(parts.get(0), min) && readable(parts.get(1), max);
    return read && ordered(min, max) ? Optional.of(new Bounds(min, max)) : Optional.empty();
  }

  /** A bound is blank or a number. */
  private static boolean readable(String text, BigDecimal value) {
    return value != null || text.isBlank();
  }

  /** At least one bound, and the minimum not above the maximum. */
  private static boolean ordered(BigDecimal min, BigDecimal max) {
    if (min == null || max == null) {
      return min != null || max != null;
    }
    return min.compareTo(max) <= 0;
  }

  private static String target(BulkRow row) {
    return row.text(CODE).startsWith(ITEM_PREFIX) ? "ITEM" : "ACCOUNT";
  }

  private static String fieldKey(BulkRow row) {
    String key = row.text(CODE);
    return key.startsWith(ITEM_PREFIX) ? key.substring(ITEM_PREFIX.length()) : key;
  }

  private List<Map<String, Object>> keys(BulkRow row) {
    List<Map<String, Object>> keys = new ArrayList<>();
    for (String code : codes(row)) {
      if (COVERAGE.equals(row.text(RECORD))) {
        keys.add(columns("line_code", code, "code", row.text(CODE)));
        continue;
      }
      Scope s = scope(code).orElseThrow();
      keys.add(
          FIELD_RULE.equals(row.text(RECORD))
              ? columns(
                  "scope",
                  s.scope(),
                  SCOPE_CODE,
                  s.code(),
                  "target",
                  target(row),
                  "field_key",
                  fieldKey(row))
              : columns(
                  "scope", s.scope(), SCOPE_CODE, s.code(), "document_type", row.text(DOCUMENT)));
    }
    return keys;
  }

  private static String table(BulkRow row) {
    return switch (row.text(RECORD)) {
      case COVERAGE -> "cat_coverage";
      case FIELD_RULE -> "cat_field_rule";
      default -> "cat_document_rule";
    };
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return keys(row).stream().anyMatch(k -> UploadSupport.UPDATE.equals(db.action(table(row), k)))
        ? UploadSupport.UPDATE
        : UploadSupport.ADD;
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    for (Map<String, Object> key : keys(row)) {
      db.upsert(table(row), key, values(row), context, label(row.text(RECORD)));
    }
    return row.text(SCOPE) + " " + row.text(CODE);
  }

  private Map<String, Object> values(BulkRow row) {
    boolean mandatory = row.yes(MANDATORY);
    return switch (row.text(RECORD)) {
      case COVERAGE ->
          columns(
              "name",
              row.text(NAME),
              "kind",
              coverageKind(row).orElseThrow(),
              "basic",
              row.yes(BASIC));
      case FIELD_RULE -> {
        String check = ruleType(row);
        Bounds range =
            RANGE.equals(check) ? range(row.text(PARAMETERS)).orElse(NO_BOUNDS) : NO_BOUNDS;
        yield columns(
            "label",
            row.text(NAME),
            REQUIRED_COLUMN,
            mandatory,
            "rule_type",
            check,
            "lov_type",
            LOV.equals(check) ? row.text(PARAMETERS) : null,
            "min_value",
            range.min(),
            "max_value",
            range.max(),
            "pattern",
            PATTERN.equals(check) ? row.text(PARAMETERS) : null);
      }
      default -> columns(REQUIRED_COLUMN, mandatory);
    };
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    List<Map<String, String>> rows = new ArrayList<>();
    for (Map<String, Object> r :
        db.rows(
            "select c.line_code, c.code, c.name, coalesce(k.label, c.kind) as kind, c.basic from cat_coverage c"
                + " left join lov_value k on k.type_code = 'COVERAGE_KIND' and k.code = c.kind"
                + " where c.record_status = 'ACTIVE' order by c.line_code, c.sort_order, c.code")) {
      rows.add(
          exportRow(
              RECORD, COVERAGE,
              SCOPE, r.get("line_code"),
              CODE, r.get("code"),
              NAME, r.get("name"),
              KIND, r.get("kind"),
              BASIC, r.get("basic"),
              MANDATORY, r.get("basic")));
    }
    for (Map<String, Object> r :
        db.rows(
            "select scope_code, target, field_key, label, rule_type, lov_type, min_value, max_value, pattern,"
                + " required from cat_field_rule where record_status = 'ACTIVE'"
                + " order by scope, scope_code, target, sort_order, field_key")) {
      String type = (String) r.get("rule_type");
      rows.add(
          exportRow(
              RECORD, FIELD_RULE,
              SCOPE, r.get(SCOPE_CODE),
              CODE, ("ITEM".equals(r.get("target")) ? ITEM_PREFIX : "") + r.get("field_key"),
              NAME, r.get("label"),
              KIND, CHECK_LABELS.getOrDefault(type, type),
              PARAMETERS, parameters(type, r),
              MANDATORY, r.get(REQUIRED_COLUMN)));
    }
    for (Map<String, Object> r :
        db.rows(
            "select d.scope_code, d.document_type, coalesce(v.label, d.document_type) as label, d.required"
                + " from cat_document_rule d left join lov_value v on v.type_code = 'DOCUMENT_TYPE'"
                + " and v.code = d.document_type where d.record_status = 'ACTIVE'"
                + " order by d.scope, d.scope_code, d.document_type")) {
      rows.add(
          exportRow(
              RECORD, DOCUMENT_RULE,
              SCOPE, r.get(SCOPE_CODE),
              CODE, r.get("document_type"),
              NAME, r.get("label"),
              KIND, "Presence",
              DOCUMENT, r.get("document_type"),
              MANDATORY, r.get(REQUIRED_COLUMN)));
    }
    return rows;
  }

  private static String parameters(String type, Map<String, Object> r) {
    return switch (type) {
      case LOV -> (String) r.get("lov_type");
      case PATTERN -> (String) r.get("pattern");
      case RANGE -> plain(r.get("min_value")) + ";" + plain(r.get("max_value"));
      default -> null;
    };
  }

  private static String plain(Object value) {
    return value instanceof BigDecimal n ? n.stripTrailingZeros().toPlainString() : "";
  }
}
