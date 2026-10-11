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
 * MD-03 Exchange rates: the rates of each foreign currency per rate type and date, in pesos for one
 * unit of the currency. The book rates come from the closing rates on the same screen and are not
 * uploaded.
 */
@Component
public class ExchangeRateUpload extends ConfigUploadHandler {

  static final String CURRENCY = "Currency code";
  static final String TYPE = "Rate type";
  static final String DATE = "Effective date";
  static final String RATE = "Rate";

  private static final String TABLE = "cur_exchange_rate";
  private static final List<String> TYPES = List.of("SPOT", "CLOSING", "AVERAGE", "BUDGET");
  private static final BigDecimal MAX_RATE = BigDecimal.valueOf(1_000_000);
  private static final int MAX_SCALE = 8;

  /** Days of rates in the export of the current data (daily rates of every currency). */
  private static final int EXPORT_DAYS = 90;

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public ExchangeRateUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_EXCHANGE_RATE";
  }

  @Override
  public String templateId() {
    return "MD-03";
  }

  @Override
  public String title() {
    return "Exchange rates";
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
        BulkColumn.required(
                CURRENCY,
                "Currency of the rate (not the base currency)",
                CurrencyUpload.EXAMPLE_CODE)
            .master("currency of MD-02 Currencies"),
        BulkColumn.required(TYPE, "Kind of rate", "SPOT").codes(TYPES.toArray(String[]::new)),
        new BulkColumn(DATE, "Date from which the rate applies", true, Type.DATE, "2028-01-03"),
        new BulkColumn(RATE, "Pesos for one unit of the currency", true, Type.NUMBER, "58.125")
            .allowed("A number from 0 to 1000000"));
  }

  @Override
  public List<String> rules() {
    return List.of(
        "The layout is the tab " + templateId() + " of the master data and configuration workbook.",
        "One rate per currency, rate type and date: a row for a date that already has a rate"
            + " updates it; uploading the same file again changes nothing.",
        "The current data downloads with the rates of the last 90 days and the rates dated"
            + " later; older rates stay on the screen.",
        "The rows are applied only after a second user approves the upload.");
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(CURRENCY) + "|" + row.text(TYPE) + "|" + row.text(DATE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    String currency = row.text(CURRENCY);
    if (!db.currency(currency)) {
      errors.add(error(CURRENCY, currency + " is not an active currency"));
    } else if (db.exists(
        "select 1 from org_company where id = ? and base_currency = ?",
        context.companyId(),
        currency)) {
      errors.add(error(CURRENCY, currency + " is the base currency; it has no exchange rate"));
    }
    oneOf(errors, TYPE, row.text(TYPE), TYPES);
    BigDecimal rate = row.number(RATE);
    check(
        errors,
        rate.signum() <= 0 || rate.compareTo(MAX_RATE) > 0,
        RATE,
        "enter a rate above 0 and up to 1000000");
    check(
        errors, rate.stripTrailingZeros().scale() > MAX_SCALE, RATE, "use up to 8 decimal places");
    return errors;
  }

  private static Map<String, Object> key(BulkRow row) {
    return columns(
        "currency_code", row.text(CURRENCY),
        "rate_type", row.text(TYPE),
        "effective_date", row.date(DATE));
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    return db.action(TABLE, key(row));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    db.upsert(TABLE, key(row), columns("rate", row.number(RATE)), context, "ExchangeRate");
    return row.text(CURRENCY) + " " + label(row.text(TYPE)) + " " + row.text(DATE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select currency_code, rate_type, effective_date, rate from cur_exchange_rate"
                + " where rate_type = any (?) and effective_date >= current_date - ?"
                + " order by currency_code, rate_type, effective_date",
            TYPES.toArray(String[]::new),
            EXPORT_DAYS)
        .stream()
        .map(
            r ->
                exportRow(
                    CURRENCY, r.get("currency_code"),
                    TYPE, r.get("rate_type"),
                    DATE, r.get("effective_date"),
                    RATE, r.get("rate")))
        .toList();
  }
}
