package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * PM-09 Lists and parameters of Product Maintenance: a row of a list adds or updates the value
 * (order and active in one cell, "50; Y"); a row of a parameter sets its value.
 */
@Component
public class ListParameterUpload extends ConfigUploadHandler {

  static final String LIST = "List or parameter";
  static final String CODE = "Value code";
  static final String LABEL = "Label or value";
  static final String ORDER = "Order / Active";
  static final String FROM = "Effective from";

  static final List<String> LISTS =
      List.of(
          "PKG_REQUEST_REASON",
          "PKG_RESPONSE_OUTCOME",
          "PKG_ADVISORY_GROUP",
          "PKG_NOT_PROCEEDED_REASON",
          "PKG_REQUEST_TYPE",
          "INCENTIVE_TYPE",
          "COVERAGE_KIND",
          "CLAUSE_KIND",
          "DOCUMENT_TYPE",
          "MARKET_SEGMENT");
  static final List<String> PARAMETERS =
      List.of(
          "PKG_REQUEST_PREFIX",
          "PKG_QS_PREFIX",
          "PKG_QS_REPLY_DAYS",
          "PACKAGE_EXPIRY_NOTICE_DAYS",
          "PACKAGE_RENEWAL_AUTODRAFT",
          "PKG_SLA_MKT_APPROVAL",
          "PKG_SLA_TSU_REVIEW",
          "PKG_SLA_TSU_APPROVAL",
          "PKG_SLA_NEGOTIATION",
          "PKG_SLA_MANCOM",
          "PKG_SLA_MBS_SETUP",
          "PACKAGE_EXPIRY_REMINDER_DAYS",
          "PKG_ADVISORY_GROUPS");

  private static final int MAX_CODE = 40;

  private final ListParameterRows lists;

  /**
   * Creates the handler.
   *
   * @param db database support
   * @param lists lists and parameters
   */
  public ListParameterUpload(UploadSupport db, ListParameterRows lists) {
    super(db);
    this.lists = lists;
  }

  @Override
  public String code() {
    return "CFG_PM_LIST_PARAMETER";
  }

  @Override
  public String templateId() {
    return "PM-09";
  }

  @Override
  public String title() {
    return "Product Maintenance lists and parameters";
  }

  @Override
  public String screen() {
    return "Setup & Administration > Lists of Values";
  }

  @Override
  public String permission() {
    return "LOV_MANAGE";
  }

  @Override
  public String approvePermission() {
    return "MASTER_AUTHORIZE";
  }

  @Override
  public String filledBy() {
    return "Product Owner, Marketing Business System (MBS)";
  }

  @Override
  public List<BulkColumn> columns() {
    List<String> all = new ArrayList<>(LISTS);
    all.addAll(PARAMETERS);
    return List.of(
        BulkColumn.required(LIST, "List or parameter", "PKG_REQUEST_REASON")
            .codes(all.toArray(String[]::new)),
        BulkColumn.optional(CODE, "Code of the value of a list", "LOSS_EXPERIENCE")
            .when("list")
            .format("Up to 40 capitals"),
        BulkColumn.required(
            LABEL, "Label of the value, or value of the parameter", "Loss experience"),
        BulkColumn.optional(ORDER, "Order; active Y or N", "50; Y").when("list"),
        BulkColumn.optional(FROM, "First day of use of the value", "")
            .when("list")
            .format("dd-MMM-yyyy"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(LIST) + "|" + row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    String list = row.text(LIST);
    if (PARAMETERS.contains(list)) {
      lists.checkParameter(list, row.text(LABEL)).ifPresent(e -> errors.add(error(LABEL, e)));
      return errors;
    }
    if (!LISTS.contains(list) || !lists.isList(list)) {
      errors.add(error(LIST, list + " is not a list or parameter of Product Maintenance"));
      return errors;
    }
    lists.checkCode(row.text(CODE), MAX_CODE).ifPresent(e -> errors.add(error(CODE, e)));
    checkOrder(row, errors);
    check(
        errors,
        row.text(FROM) != null && UploadCells.date(row.text(FROM)) == null,
        FROM,
        "give the date as dd-MMM-yyyy");
    return errors;
  }

  private static void checkOrder(BulkRow row, List<String> errors) {
    List<String> order = UploadCells.parts(row.text(ORDER), 2);
    BigDecimal sort = UploadCells.number(order.get(0));
    check(
        errors,
        order.get(0) != null && (sort == null || sort.stripTrailingZeros().scale() > 0),
        ORDER,
        "the order is a whole number");
    check(
        errors,
        order.get(1) != null && UploadCells.yes(order.get(1)).isEmpty(),
        ORDER,
        "active is Y or N");
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return PARAMETERS.contains(row.text(LIST))
        ? UploadSupport.UPDATE
        : lists.valueAction(row.text(LIST), row.text(CODE));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    if (PARAMETERS.contains(row.text(LIST))) {
      boolean changed = lists.saveParameter(row.text(LIST), row.text(LABEL));
      return row.text(LIST) + (changed ? "" : " unchanged");
    }
    List<String> order = UploadCells.parts(row.text(ORDER), 2);
    BigDecimal sort = UploadCells.number(order.get(0));
    LocalDate from = UploadCells.date(row.text(FROM));
    lists.saveValue(
        row.text(LIST),
        row.text(CODE),
        row.text(LABEL),
        sort == null ? null : sort.intValue(),
        UploadCells.yes(order.get(1)).orElse(Boolean.TRUE),
        from,
        context);
    return row.text(LIST) + " " + row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    List<Map<String, String>> rows = new ArrayList<>();
    for (Map<String, Object> r : lists.values(LISTS)) {
      rows.add(
          exportRow(
              LIST, r.get("type_code"),
              CODE, r.get("code"),
              LABEL, r.get("label"),
              ORDER, UploadCells.join(r.get("sort_order"), "ACTIVE".equals(r.get("record_status"))),
              FROM, UploadCells.range(r.get("effective_from"), null)));
    }
    for (String key : PARAMETERS) {
      lists.parameterValue(key).ifPresent(v -> rows.add(exportRow(LIST, key, LABEL, v)));
    }
    return rows;
  }
}
