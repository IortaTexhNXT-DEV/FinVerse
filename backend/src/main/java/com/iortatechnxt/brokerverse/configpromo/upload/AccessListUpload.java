package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * UA-06 Lists of values of User Access Maintenance (business units, user levels, deactivation
 * reasons).
 */
@Component
public class AccessListUpload extends ConfigUploadHandler {

  static final String LIST = "List";
  static final String CODE = "Code";
  static final String LABEL = "Label";
  static final String ORDER = "Sort order";
  static final String FROM = "Effective from";

  static final List<String> LISTS =
      List.of("UAM_BUSINESS_UNIT", "UAM_USER_LEVEL", "UAM_DEACTIVATION_REASON");

  private final ListParameterRows lists;

  /**
   * Creates the handler.
   *
   * @param db database support
   * @param lists lists and parameters
   */
  public AccessListUpload(UploadSupport db, ListParameterRows lists) {
    super(db);
    this.lists = lists;
  }

  @Override
  public String code() {
    return "CFG_ACCESS_LIST";
  }

  @Override
  public String templateId() {
    return "UA-06";
  }

  @Override
  public String title() {
    return "User access lists of values";
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
    return "Business Administration (User Access)";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(LIST, "The list", "UAM_BUSINESS_UNIT")
            .codes(LISTS.toArray(String[]::new)),
        BulkColumn.required(CODE, "Code of the value", "COMBANK_MKT")
            .format("Up to 30 capitals, digits or _"),
        BulkColumn.required(LABEL, "Label shown on screens", "Combank Marketing")
            .format("Up to 120 characters"),
        new BulkColumn(ORDER, "Order in the drop-down", false, Type.NUMBER, "10"),
        new BulkColumn(FROM, "Date the value may be used", true, Type.DATE, "2027-01-04"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(LIST) + "|" + row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    oneOf(errors, LIST, row.text(LIST), LISTS);
    if (errors.isEmpty() && !lists.isList(row.text(LIST))) {
      errors.add(error(LIST, row.text(LIST) + " is not a list of this environment"));
    }
    lists.checkCode(row.text(CODE), 30).ifPresent(e -> errors.add(error(CODE, e)));
    if (row.text(LABEL).length() > 120) {
      errors.add(error(LABEL, "use up to 120 characters"));
    }
    BigDecimal order = row.number(ORDER);
    if (order != null && order.stripTrailingZeros().scale() > 0) {
      errors.add(error(ORDER, "enter a whole number"));
    }
    return errors;
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return lists.valueAction(row.text(LIST), row.text(CODE));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    BigDecimal order = row.number(ORDER);
    lists.saveValue(
        row.text(LIST),
        row.text(CODE),
        row.text(LABEL),
        order == null ? null : order.intValue(),
        true,
        row.date(FROM),
        context);
    return row.text(LIST) + " " + row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return lists.values(LISTS).stream()
        .filter(r -> "ACTIVE".equals(r.get("record_status")))
        .map(
            r ->
                exportRow(
                    LIST, r.get("type_code"),
                    CODE, r.get("code"),
                    LABEL, r.get("label"),
                    ORDER, r.get("sort_order"),
                    FROM, r.get("effective_from")))
        .toList();
  }
}
