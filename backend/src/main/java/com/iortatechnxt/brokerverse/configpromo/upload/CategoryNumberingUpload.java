package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * D0-07 Account categories and numbering, on Chart of Accounts: a CATEGORY row is a GL category, a
 * NUMBERING row the numbering scheme of the children of a parent account.
 */
@Component
public class CategoryNumberingUpload extends ConfigUploadHandler {

  static final String ROW = "Row";
  static final String CATEGORY = "Category code";
  static final String NAME = "Category name";
  static final String ACCOUNT_CLASS = "Account class";
  static final String BANK = "Bank category";
  static final String PARENT = "Parent account";
  static final String SEPARATOR = "Separator";
  static final String WIDTH = "Width";

  private static final String KIND_CATEGORY = "CATEGORY";
  private static final String KIND_NUMBERING = "NUMBERING";
  private static final List<String> CLASSES =
      List.of("ASSET", "LIABILITY", "EQUITY", "INCOME", "EXPENSE", "MEMORANDUM");
  private static final Pattern CATEGORY_FORMAT = Pattern.compile("[A-Z0-9_]{1,20}");
  private static final int MAX_WIDTH = 6;

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public CategoryNumberingUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_COA_CATEGORY";
  }

  @Override
  public String templateId() {
    return "D0-07";
  }

  @Override
  public String title() {
    return "Account categories and numbering";
  }

  @Override
  public String screen() {
    return "General Ledger > Chart of Accounts";
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
        BulkColumn.required(ROW, "What the row defines", KIND_CATEGORY)
            .codes(KIND_CATEGORY, KIND_NUMBERING),
        BulkColumn.optional(CATEGORY, "GL category", "CASH_IN_BANK").when(KIND_CATEGORY),
        BulkColumn.optional(NAME, "Name of the category", "Cash in bank").when(KIND_CATEGORY),
        BulkColumn.optional(ACCOUNT_CLASS, "Class of the accounts of the category", "ASSET")
            .codes(CLASSES.toArray(String[]::new))
            .when(KIND_CATEGORY),
        new BulkColumn(BANK, "Y for the accounts of bank accounts", false, Type.YES_NO, "Y")
            .when(KIND_CATEGORY),
        BulkColumn.optional(PARENT, "Parent whose children are numbered", "1110")
            .master("account of the chart of accounts")
            .when(KIND_NUMBERING),
        BulkColumn.optional(SEPARATOR, "Between the parent code and the running number", ".")
            .values(".", "-")
            .when(KIND_NUMBERING),
        new BulkColumn(WIDTH, "Digits of the running number", false, Type.NUMBER, "2")
            .allowed("A whole number from 1 to 6")
            .when(KIND_NUMBERING));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return KIND_CATEGORY.equals(row.text(ROW))
        ? "C|" + row.text(CATEGORY)
        : "N|" + row.text(PARENT);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    String kind = row.text(ROW);
    oneOf(errors, ROW, kind, List.of(KIND_CATEGORY, KIND_NUMBERING));
    if (KIND_CATEGORY.equals(kind)) {
      required(errors, row, List.of(CATEGORY, NAME, ACCOUNT_CLASS), KIND_CATEGORY);
      if (row.text(CATEGORY) != null && !CATEGORY_FORMAT.matcher(row.text(CATEGORY)).matches()) {
        errors.add(error(CATEGORY, "use up to 20 capital letters, digits or _"));
      }
      oneOf(errors, ACCOUNT_CLASS, row.text(ACCOUNT_CLASS), CLASSES);
    } else if (KIND_NUMBERING.equals(kind)) {
      required(errors, row, List.of(PARENT, WIDTH), KIND_NUMBERING);
      if (row.text(PARENT) != null && !db.account(context.companyId(), row.text(PARENT))) {
        errors.add(error(PARENT, row.text(PARENT) + " is not an account of the company"));
      }
      CompanyUpload.whole(errors, row, WIDTH, 1, MAX_WIDTH);
    }
    return errors;
  }

  private static void required(
      List<String> errors, BulkRow row, List<String> headers, String kind) {
    for (String h : headers) {
      if (row.text(h) == null) {
        errors.add(error(h, "mandatory on a " + kind + " row"));
      }
    }
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return KIND_CATEGORY.equals(row.text(ROW))
        ? db.action("coa_category", columns("code", row.text(CATEGORY)))
        : db.action("coa_numbering", numberingKey(row, context));
  }

  private static Map<String, Object> numberingKey(BulkRow row, BulkContext context) {
    return columns("company_id", context.companyId(), "parent_code", row.text(PARENT));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    if (KIND_CATEGORY.equals(row.text(ROW))) {
      db.upsert(
          "coa_category",
          columns("code", row.text(CATEGORY)),
          columns(
              "name", row.text(NAME),
              "account_class", row.text(ACCOUNT_CLASS),
              "bank_category", yes(row, BANK)),
          context,
          "Account category");
      return row.text(CATEGORY);
    }
    db.upsert(
        "coa_numbering",
        numberingKey(row, context),
        columns(
            "separator",
            row.text(SEPARATOR) == null ? "" : row.text(SEPARATOR),
            "width",
            row.number(WIDTH),
            "active",
            true),
        context,
        "Account numbering");
    return row.text(PARENT);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    List<Map<String, String>> rows = new ArrayList<>();
    db.rows("select code, name, account_class, bank_category from coa_category order by code")
        .forEach(
            r ->
                rows.add(
                    exportRow(
                        ROW, KIND_CATEGORY,
                        CATEGORY, r.get("code"),
                        NAME, r.get("name"),
                        ACCOUNT_CLASS, r.get("account_class"),
                        BANK, r.get("bank_category"))));
    db.rows(
            "select parent_code, separator, width from coa_numbering where company_id = ?"
                + " order by parent_code",
            companyId)
        .forEach(
            r ->
                rows.add(
                    exportRow(
                        ROW, KIND_NUMBERING,
                        PARENT, r.get("parent_code"),
                        SEPARATOR, r.get("separator"),
                        WIDTH, r.get("width"))));
    return rows;
  }
}
