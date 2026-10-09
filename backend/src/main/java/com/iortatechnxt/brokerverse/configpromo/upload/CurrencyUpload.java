package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** MD-02 Currencies: the currencies accepted on transactions and bank accounts. */
@Component
public class CurrencyUpload extends ConfigUploadHandler {

  static final String CODE = "Currency code";
  static final String NAME = "Name";
  static final String SYMBOL = "Symbol";
  static final String DECIMALS = "Decimal places";
  static final String ACTIVE = "Active";

  private static final String TABLE = "cur_currency";
  private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z]{3}");
  private static final int NAME_LENGTH = 60;
  private static final int SYMBOL_LENGTH = 5;
  private static final int MAX_DECIMALS = 4;

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public CurrencyUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_CURRENCY";
  }

  @Override
  public String templateId() {
    return "MD-02";
  }

  @Override
  public String title() {
    return "Currencies";
  }

  @Override
  public String screen() {
    return "Setup & Administration > Currencies & Exchange Rates";
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
    return "Head, Comptrollership (Treasury)";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(CODE, "Code of the currency", "USD").format("ISO 4217, 3 capitals"),
        BulkColumn.required(NAME, "Name of the currency", "US dollar").format("Text, up to 60"),
        BulkColumn.optional(SYMBOL, "Symbol printed on documents", "$")
            .format("Up to 5 characters"),
        new BulkColumn(DECIMALS, "Decimals of the amounts in the currency", true, Type.NUMBER, "2")
            .allowed("A whole number from 0 to 4"),
        new BulkColumn(ACTIVE, "Y when the currency may be used", true, Type.YES_NO, "Y"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    String code = row.text(CODE);
    check(errors, !CODE_FORMAT.matcher(code).matches(), CODE, "use the 3 capitals of ISO 4217");
    check(errors, row.text(NAME).length() > NAME_LENGTH, NAME, "use up to 60 characters");
    String symbol = row.text(SYMBOL);
    check(errors, symbol != null && symbol.length() > SYMBOL_LENGTH, SYMBOL, "up to 5 characters");
    whole(errors, DECIMALS, row.number(DECIMALS), 0, MAX_DECIMALS);
    if (!yes(row, ACTIVE)) {
      check(
          errors,
          db.exists("select 1 from org_company where base_currency = ?", code),
          ACTIVE,
          code + " is the base currency of a company and stays active");
      check(
          errors,
          db.exists(
              "select 1 from pay_bank_account where currency = ? and record_status = 'ACTIVE'",
              code),
          ACTIVE,
          code + " is the currency of an active bank account");
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
    db.upsert(
        TABLE,
        key(row),
        columns(
            "name", row.text(NAME),
            "symbol", row.text(SYMBOL),
            "decimal_places", row.number(DECIMALS).intValueExact(),
            "active", yes(row, ACTIVE)),
        context,
        "Currency");
    return row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows("select code, name, symbol, decimal_places, active from cur_currency order by code")
        .stream()
        .map(
            r ->
                exportRow(
                    CODE, r.get("code"),
                    NAME, r.get("name"),
                    SYMBOL, r.get("symbol"),
                    DECIMALS, r.get("decimal_places"),
                    ACTIVE, r.get("active")))
        .toList();
  }
}
