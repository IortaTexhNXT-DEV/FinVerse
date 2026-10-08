package com.iortatechnxt.brokerverse.configpromo.upload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.catalog.service.IncentiveRuleParameters;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * PM-10 Incentive criteria with their products matrix (one product per line of the cell, with an
 * optional market segment). A row with the code and the effective date of a criterion updates it; a
 * later effective date adds the successor and ends the criterion in force the day before.
 */
@Component
// Criteria, value, rule parameters, products and successors of one tab.
@SuppressWarnings("PMD.GodClass")
public class IncentiveCriteriaUpload extends ConfigUploadHandler {
  private static final String RULE = "RULE";
  private static final String FIXED_AMOUNT = "FIXED_AMOUNT";
  private static final String RATE = "RATE";

  static final String CODE = "Code / Name";
  static final String TYPE = "Incentive type";
  static final String VALUE = "Value basis / Value";
  static final String PARAMETERS = "Rule parameters";
  static final String PRODUCTS = "Products matrix: risk code / segment";
  static final String EFFECTIVE = "Effective from / to";

  private static final String TABLE = "cat_incentive_criteria";
  private static final Map<String, String> BASES =
      Map.of(
          "RATE (%)",
          RATE, RATE, RATE, "FIXED AMOUNT", FIXED_AMOUNT, FIXED_AMOUNT, FIXED_AMOUNT, RULE, RULE);
  private static final Map<String, String> BASIS_LABELS =
      Map.of(RATE, "Rate (%)", FIXED_AMOUNT, "Fixed amount", RULE, "Rule");
  private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z0-9_-]{1,30}");
  private static final Pattern PAIR =
      Pattern.compile("\\s*(.+?)\\s*(?:=|:|\\s)\\s*([^\\s=:]+)\\s*");

  private final IncentiveRuleParameters ruleParameters;
  private final ObjectMapper json;

  /**
   * Creates the handler.
   *
   * @param db database support
   * @param ruleParameters parameters of the incentive rules
   * @param json JSON
   */
  public IncentiveCriteriaUpload(
      UploadSupport db, IncentiveRuleParameters ruleParameters, ObjectMapper json) {
    super(db);
    this.ruleParameters = ruleParameters;
    this.json = json;
  }

  @Override
  public String code() {
    return "CFG_INCENTIVE_CRITERIA";
  }

  @Override
  public String templateId() {
    return "PM-10";
  }

  @Override
  public String title() {
    return "Incentive criteria";
  }

  @Override
  public String screen() {
    return "Product Maintenance > Incentive Criteria";
  }

  @Override
  public String permission() {
    return "INCENTIVE_CRITERIA_MAINTAIN";
  }

  @Override
  public String approvePermission() {
    return "PRODUCT_AUTHORIZE";
  }

  @Override
  public String filledBy() {
    return "Product Owner, Marketing Business System (MBS)";
  }

  @Override
  protected Set<String> multiline() {
    return Set.of(PRODUCTS);
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(CODE, "Code; name", "CPC2; CPC2 campaign 2028")
            .format("A-Z, 0-9, _ and -; text"),
        BulkColumn.required(TYPE, "Incentive type", "OTHERS").lov("INCENTIVE_TYPE"),
        BulkColumn.required(VALUE, "Rate (%), Fixed amount or Rule; value", "Rate (%); 1.00"),
        BulkColumn.optional(
            PARAMETERS,
            "Parameter and value pairs separated by semicolons",
            "Minimum Gross Premium 5000"),
        BulkColumn.required(
                PRODUCTS, "Risk code; market segment (blank = every segment)", "MTR30; COMBANK")
            .format("One product per line"),
        BulkColumn.required(
                EFFECTIVE, "First day; last day after a semicolon, blank = open", "01-Jan-2028")
            .format("dd-MMM-yyyy; dd-MMM-yyyy"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return UploadCells.parts(row.text(CODE), 2).get(0)
        + "|"
        + UploadCells.period(row.text(EFFECTIVE)).map(d -> d.from().toString()).orElse("");
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    List<String> code = UploadCells.parts(row.text(CODE), 2);
    check(
        errors,
        code.get(0) == null || !CODE_FORMAT.matcher(code.get(0)).matches() || code.get(1) == null,
        CODE,
        "give the code (A-Z, 0-9, _ and -) and, after a semicolon, the name");
    check(
        errors,
        !db.listValue("INCENTIVE_TYPE", row.text(TYPE)),
        TYPE,
        row.text(TYPE) + " is not an incentive type");
    validateValue(row, errors);
    try {
      ruleParameters.check(row.text(TYPE), params(row));
    } catch (BusinessRuleException e) {
      errors.add(error(PARAMETERS, e.getMessage()));
    }
    validateProducts(row, errors);
    Optional<UploadCells.Period> dates = UploadCells.period(row.text(EFFECTIVE));
    check(
        errors,
        dates.isEmpty(),
        EFFECTIVE,
        "give the first day as dd-MMM-yyyy and, after a semicolon, the last day");
    check(
        errors,
        errors.isEmpty() && overlaps(context, code.get(0), dates.orElseThrow()),
        EFFECTIVE,
        "criterion " + code.get(0) + " already covers part of this period");
    return errors;
  }

  private static void validateValue(BulkRow row, List<String> errors) {
    String basis = basis(row);
    check(errors, basis == null, VALUE, "the value basis is Rate (%), Fixed amount or Rule");
    if (basis != null && !RULE.equals(basis)) {
      checkAmount(row, basis, errors);
    }
  }

  private static void checkAmount(BulkRow row, String basis, List<String> errors) {
    BigDecimal v = UploadCells.number(UploadCells.parts(row.text(VALUE), 2).get(1));
    boolean valid =
        v != null
            && v.signum() >= 0
            && !(RATE.equals(basis) && v.compareTo(BigDecimal.valueOf(100)) > 0);
    check(
        errors,
        !valid,
        VALUE,
        "give the value after a semicolon (a rate from 0 to 100, or an amount)");
  }

  private void validateProducts(BulkRow row, List<String> errors) {
    List<String> lines = products(row);
    check(errors, lines.isEmpty(), PRODUCTS, "give at least one product");
    for (String line : lines) {
      String product = UploadCells.parts(line, 2).get(0);
      check(
          errors,
          !db.exists(
              "select 1 from cat_product where code = ? and record_status = 'ACTIVE' and lifecycle_status = 'ACTIVE'",
              product),
          PRODUCTS,
          product + " is not an active product");
    }
  }

  /**
   * Another criterion with the code over the period, other than the one in force that a later row
   * succeeds.
   */
  private boolean overlaps(BulkContext context, String code, UploadCells.Period dates) {
    return db.exists(
        "select 1 from cat_incentive_criteria where company_id = ? and code = ? and record_status <> 'INACTIVE'"
            + " and effective_from <> ? and not (effective_to is null and effective_from < ?)"
            + " and (cast(? as date) is null or effective_from <= ?)"
            + " and (effective_to is null or effective_to >= ?)",
        context.companyId(),
        code,
        dates.from(),
        dates.from(),
        dates.to(),
        dates.to(),
        dates.from());
  }

  private static String basis(BulkRow row) {
    String b = UploadCells.parts(row.text(VALUE), 2).get(0);
    return b == null ? null : BASES.get(b.toUpperCase(Locale.ROOT));
  }

  private static List<String> products(BulkRow row) {
    return Arrays.stream(row.text(PRODUCTS).split("\\r?\\n|\\|"))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  /** The rule parameters as stored (a JSON object), from "label value; label value". */
  private String params(BulkRow row) {
    String cell = row.text(PARAMETERS);
    if (cell == null) {
      return null;
    }
    ObjectNode node = json.createObjectNode();
    for (String pair : cell.split(";")) {
      if (pair.isBlank()) {
        continue;
      }
      Matcher m = PAIR.matcher(pair);
      if (!m.matches()) {
        throw new BusinessRuleException(
            "INCENTIVE_RULE_PARAMS_INVALID", pair.trim() + " is not a parameter and a value");
      }
      String name = m.group(1);
      String key =
          ruleParameters.allowed(row.text(TYPE)).stream()
              .filter(p -> p.key().equalsIgnoreCase(name) || p.label().equalsIgnoreCase(name))
              .map(IncentiveRuleParameters.Parameter::key)
              .findFirst()
              .orElse(name);
      BigDecimal number = UploadCells.number(m.group(2));
      if (number != null) {
        node.put(key, number);
      } else {
        node.put(key, m.group(2));
      }
    }
    return node.toString();
  }

  private static Map<String, Object> key(BulkRow row, BulkContext context) {
    return columns(
        "company_id", context.companyId(),
        "code", UploadCells.parts(row.text(CODE), 2).get(0),
        "effective_from", UploadCells.periodOf(row.text(EFFECTIVE)).from());
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.action(TABLE, key(row, context));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    String code = UploadCells.parts(row.text(CODE), 2).get(0);
    UploadCells.Period dates = UploadCells.periodOf(row.text(EFFECTIVE));
    String basis = basis(row);
    Map<String, Object> values =
        columns(
            "name",
            UploadCells.parts(row.text(CODE), 2).get(1),
            "incentive_type",
            row.text(TYPE),
            "value_basis",
            basis,
            "value",
            RULE.equals(basis)
                ? null
                : UploadCells.number(UploadCells.parts(row.text(VALUE), 2).get(1)),
            "rule_params",
            params(row),
            "effective_to",
            dates.to());
    if (UploadSupport.ADD.equals(previewAction(row, context))) {
      Optional<Long> predecessor =
          db.id(
              "select id from cat_incentive_criteria where company_id = ? and code = ? and record_status = 'ACTIVE'"
                  + " and effective_to is null and effective_from < ?",
              context.companyId(),
              code,
              dates.from());
      predecessor.ifPresent(
          id -> {
            db.execute(
                "update cat_incentive_criteria set effective_to = ?, version = version + 1 where id = ?",
                dates.from().minusDays(1),
                id);
            values.put("successor_of", id);
          });
    }
    long id = db.upsert(TABLE, key(row, context), values, context, "Incentive criteria");
    replaceProducts(id, row);
    return code + " from " + dates.from();
  }

  private void replaceProducts(long id, BulkRow row) {
    List<String> wanted = new ArrayList<>();
    for (String line : products(row)) {
      List<String> p = UploadCells.parts(line, 2);
      wanted.add(p.get(0) + "|" + (p.get(1) == null ? "" : p.get(1)));
    }
    List<String> current =
        db
            .rows(
                "select product_code, coalesce(market_segment, '') as segment from cat_incentive_criteria_product"
                    + " where criteria_id = ? and cover_type_code is null and source_channel is null"
                    + " and insurer_code is null order by id",
                id)
            .stream()
            .map(r -> r.get("product_code") + "|" + r.get("segment"))
            .toList();
    if (current.equals(wanted)) {
      return;
    }
    db.execute("delete from cat_incentive_criteria_product where criteria_id = ?", id);
    for (String w : wanted) {
      String[] p = w.split("\\|", -1);
      db.execute(
          "insert into cat_incentive_criteria_product (criteria_id, product_code, market_segment) values (?, ?, ?)",
          id,
          p[0],
          p[1].isEmpty() ? null : p[1]);
    }
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    List<Map<String, String>> rows = new ArrayList<>();
    for (Map<String, Object> r :
        db.rows(
            "select id, code, name, incentive_type, value_basis, value, rule_params, effective_from, effective_to"
                + " from cat_incentive_criteria where company_id = ? and record_status = 'ACTIVE'"
                + " order by code, effective_from",
            companyId)) {
      List<String> products = new ArrayList<>();
      for (Map<String, Object> p :
          db.rows(
              "select product_code, market_segment from cat_incentive_criteria_product"
                  + " where criteria_id = ? order by id",
              r.get("id"))) {
        products.add(
            p.get("market_segment") == null
                ? (String) p.get("product_code")
                : p.get("product_code") + "; " + p.get("market_segment"));
      }
      String basis = (String) r.get("value_basis");
      rows.add(
          exportRow(
              CODE, r.get("code") + "; " + r.get("name"),
              TYPE, r.get("incentive_type"),
              VALUE,
                  r.get("value") == null
                      ? BASIS_LABELS.get(basis)
                      : UploadCells.join(BASIS_LABELS.get(basis), r.get("value")),
              PARAMETERS, paramsText((String) r.get("rule_params")),
              PRODUCTS, String.join("\n", products),
              EFFECTIVE, UploadCells.range(r.get("effective_from"), r.get("effective_to"))));
    }
    return rows;
  }

  private String paramsText(String params) {
    if (params == null || params.isBlank()) {
      return null;
    }
    try {
      JsonNode node = json.readTree(params);
      List<String> pairs = new ArrayList<>();
      node.properties().forEach(e -> pairs.add(e.getKey() + " " + e.getValue().asText()));
      return pairs.isEmpty() ? null : String.join("; ", pairs);
    } catch (JsonProcessingException e) {
      return params;
    }
  }
}
