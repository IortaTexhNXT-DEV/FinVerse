package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** MD-01 Business partners (agents, brokers, reinsurers, suppliers...), on Business Partners. */
@Component
public class BusinessPartnerUpload extends ConfigUploadHandler {

  static final String CODE = "Party code";
  static final String TYPE = "Party type";
  static final String NAME = "Name";
  static final String TIN = "TIN / tax id";
  static final String ADDRESS = "Address";
  static final String EMAIL = "E-mail";
  static final String PHONE = "Phone";
  static final String CURRENCY = "Default currency";
  static final String BANK = "Bank name";
  static final String ACCOUNT = "Bank account no.";
  static final String LICENCE = "Licence no.";
  static final String CREDIT_DAYS = "Credit days";
  static final String COMMISSION = "Commission rate %";
  static final String WITHHOLDING = "Withholding tax rate %";

  private static final String TABLE = "pty_party";
  private static final List<String> TYPES =
      List.of(
          "AGENT",
          "BROKER",
          "REINSURER",
          "RI_BROKER",
          "COINSURER",
          "SUPPLIER",
          "GARAGE",
          "SURVEYOR",
          "BANK",
          "EMPLOYEE",
          "GOVERNMENT",
          "OTHER_PAYEE");
  private static final List<String> INTERMEDIARIES = List.of("AGENT", "BROKER", "RI_BROKER");
  private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z0-9.-]{1,30}");
  private static final Pattern MAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public BusinessPartnerUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_BUSINESS_PARTNER";
  }

  @Override
  public String templateId() {
    return "MD-01";
  }

  @Override
  public String title() {
    return "Business partners";
  }

  @Override
  public String screen() {
    return "Setup & Administration > Business Partners";
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
    return "Head, Comptrollership; owners of the partner relationships";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(CODE, "Code of the partner, unique", "RI-0001")
            .format("Up to 30; capitals, digits, - and ."),
        BulkColumn.required(TYPE, "Kind of partner", "REINSURER")
            .codes(TYPES.toArray(String[]::new)),
        BulkColumn.required(NAME, "Registered name", "Asia Pacific Reinsurance Corp."),
        BulkColumn.optional(TIN, "Tax identification number", "123-456-789-000")
            .format("000-000-000-000"),
        BulkColumn.optional(ADDRESS, "Registered address", "Makati City"),
        BulkColumn.optional(EMAIL, "E-mail for advices and statements", "treaty@apre.example.ph"),
        BulkColumn.optional(PHONE, "Phone number", "+63 2 8888 0000"),
        BulkColumn.required(
            CURRENCY, "Currency of the partner's transactions", BulkColumn.BASE_CURRENCY_EXAMPLE),
        BulkColumn.optional(BANK, "Bank of the partner's account", "BDO Unibank"),
        BulkColumn.optional(ACCOUNT, "Account number for payments", "001234567890"),
        BulkColumn.optional(LICENCE, "Insurance Commission licence number", "IC-RB-2026-015")
            .when("agent, broker, reinsurance broker"),
        new BulkColumn(CREDIT_DAYS, "Credit days of the partner's items", false, Type.NUMBER, "30"),
        new BulkColumn(COMMISSION, "Commission rate of an intermediary", false, Type.NUMBER, ""),
        new BulkColumn(
            WITHHOLDING, "Withholding tax rate applied to the partner", false, Type.NUMBER, "2"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    if (!CODE_FORMAT.matcher(row.text(CODE)).matches()) {
      errors.add(error(CODE, "use up to 30 capital letters, digits, - or ."));
    }
    oneOf(errors, TYPE, row.text(TYPE), TYPES);
    if (INTERMEDIARIES.contains(row.text(TYPE)) && row.text(LICENCE) == null) {
      errors.add(error(LICENCE, "mandatory for an agent, a broker or a reinsurance broker"));
    }
    if (row.text(EMAIL) != null && !MAIL.matcher(row.text(EMAIL)).matches()) {
      errors.add(error(EMAIL, row.text(EMAIL) + " is not an e-mail address"));
    }
    if (!db.currency(row.text(CURRENCY))) {
      errors.add(error(CURRENCY, row.text(CURRENCY) + " is not an active currency"));
    }
    BigDecimal days = row.number(CREDIT_DAYS);
    if (days != null
        && (days.stripTrailingZeros().scale() > 0
            || days.intValue() < 0
            || days.intValue() > 365)) {
      errors.add(error(CREDIT_DAYS, "enter a whole number from 0 to 365"));
    }
    for (String h : List.of(COMMISSION, WITHHOLDING)) {
      BigDecimal rate = row.number(h);
      if (rate != null && (rate.signum() < 0 || rate.compareTo(HUNDRED) > 0)) {
        errors.add(error(h, "enter a rate from 0 to 100"));
      }
    }
    if (db.exists(
        "select 1 from pty_party where company_id = ? and code = ? and party_type <> ?",
        context.companyId(),
        row.text(CODE),
        row.text(TYPE))) {
      errors.add(error(TYPE, "the partner " + row.text(CODE) + " exists with another type"));
    }
    return errors;
  }

  private static Map<String, Object> key(BulkRow row, BulkContext context) {
    return columns("company_id", context.companyId(), "code", row.text(CODE));
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
        columns(
            "party_type", row.text(TYPE),
            "name", row.text(NAME),
            "tax_id", row.text(TIN),
            "address", row.text(ADDRESS),
            "email", row.text(EMAIL),
            "phone", row.text(PHONE),
            "default_currency", row.text(CURRENCY),
            "bank_name", row.text(BANK),
            "bank_account_no", row.text(ACCOUNT),
            "licence_no", row.text(LICENCE),
            "credit_days", row.number(CREDIT_DAYS) == null ? 0 : row.number(CREDIT_DAYS).intValue(),
            "commission_rate", row.number(COMMISSION),
            "withholding_tax_rate", row.number(WITHHOLDING)),
        context,
        "Business partner");
    return row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select code, party_type, name, tax_id, address, email, phone, default_currency, bank_name,"
                + " bank_account_no, licence_no, credit_days, commission_rate, withholding_tax_rate"
                + " from pty_party where company_id = ? and party_type = any (?) and record_status = 'ACTIVE'"
                + " order by code",
            companyId,
            TYPES.toArray(String[]::new))
        .stream()
        .map(
            r ->
                exportRow(
                    CODE, r.get("code"),
                    TYPE, r.get("party_type"),
                    NAME, r.get("name"),
                    TIN, r.get("tax_id"),
                    ADDRESS, r.get("address"),
                    EMAIL, r.get("email"),
                    PHONE, r.get("phone"),
                    CURRENCY, r.get("default_currency"),
                    BANK, r.get("bank_name"),
                    ACCOUNT, r.get("bank_account_no"),
                    LICENCE, r.get("licence_no"),
                    CREDIT_DAYS, r.get("credit_days"),
                    COMMISSION, r.get("commission_rate"),
                    WITHHOLDING, r.get("withholding_tax_rate")))
        .toList();
  }
}
