package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * D0-08 Accounting rules, on Accounting Rules: one row per line of a rule; the rule is the event,
 * its name and its first posting date, the line its order in the entry.
 */
@Component
public class AccountingRuleUpload extends ConfigUploadHandler {

  static final String EVENT = "Event type";
  static final String RULE = "Rule name";
  static final String LINE_OF_BUSINESS = "Business line";
  static final String CURRENCY = "Currency";
  static final String FROM = "Effective from";
  static final String LINE = "Line";
  static final String SIDE = "Debit or credit";
  static final String ACCOUNT = "Account code";
  static final String COMPONENT = "Amount component";
  static final String NARRATION = "Narration";

  private static final int MAX_LINE = 999;

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public AccountingRuleUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_ACCOUNTING_RULE";
  }

  @Override
  public String templateId() {
    return "D0-08";
  }

  @Override
  public String title() {
    return "Accounting rules";
  }

  @Override
  public String screen() {
    return "Accounting Engine > Accounting Rules";
  }

  @Override
  public String permission() {
    return "ACCOUNTING_RULE_MANAGE";
  }

  @Override
  public String approvePermission() {
    return "ACCOUNTING_RULE_MANAGE";
  }

  @Override
  public String filledBy() {
    return "Head, Comptrollership";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(EVENT, "Event that the rule posts", "PREMIUM_BOOKED")
            .master("accounting event"),
        BulkColumn.required(RULE, "Name of the rule", "Premium booked - non-life"),
        BulkColumn.optional(LINE_OF_BUSINESS, "Line the rule applies to; blank = every line", "")
            .master("business line"),
        BulkColumn.optional(CURRENCY, "Currency the rule applies to; blank = every currency", ""),
        new BulkColumn(FROM, "First posting date of the rule", true, Type.DATE, "2028-01-01"),
        new BulkColumn(LINE, "Order of the line in the entry", true, Type.NUMBER, "1")
            .allowed("A whole number from 1 to 999"),
        BulkColumn.required(SIDE, "Side of the line", "DEBIT").codes("DEBIT", "CREDIT"),
        BulkColumn.required(ACCOUNT, "Account posted", "1210.07")
            .master("account of the chart of accounts"),
        BulkColumn.required(COMPONENT, "Amount the line posts", "GROSS_PREMIUM")
            .allowed("A component of the event"),
        BulkColumn.optional(NARRATION, "Text of the journal line", "Premium receivable"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(EVENT) + "|" + row.text(RULE) + "|" + row.text(FROM) + "|" + row.text(LINE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    Optional<String> components =
        db.text(
            "select amount_components from acc_event_type where code = ? and active",
            row.text(EVENT));
    if (components.isEmpty()) {
      errors.add(error(EVENT, row.text(EVENT) + " is not an active accounting event"));
    } else if (!Arrays.asList(components.get().split(",")).contains(row.text(COMPONENT))) {
      errors.add(
          error(
              COMPONENT,
              row.text(COMPONENT)
                  + " is not an amount of "
                  + row.text(EVENT)
                  + "; use one of "
                  + components.get().replace(",", ", ")));
    }
    oneOf(errors, SIDE, row.text(SIDE), List.of("DEBIT", "CREDIT"));
    String account = row.text(ACCOUNT);
    boolean role =
        account.startsWith("@")
            && account.length() > 1
            && account.substring(1).matches("[A-Z0-9_]+");
    if (!role && !db.account(context.companyId(), account)) {
      errors.add(
          error(
              ACCOUNT,
              account + " is not an account of the company nor an account role such as @BANK"));
    }
    if (row.text(LINE_OF_BUSINESS) != null
        && !db.exists(
            "select 1 from dim_value where company_id = ? and dimension_type = 'BUSINESS_LINE' and code = ?",
            context.companyId(),
            row.text(LINE_OF_BUSINESS))) {
      errors.add(error(LINE_OF_BUSINESS, row.text(LINE_OF_BUSINESS) + " is not a business line"));
    }
    if (row.text(CURRENCY) != null && !db.currency(row.text(CURRENCY))) {
      errors.add(error(CURRENCY, row.text(CURRENCY) + " is not an active currency"));
    }
    CompanyUpload.whole(errors, row, LINE, 1, MAX_LINE);
    return errors;
  }

  private static Map<String, Object> ruleKey(BulkRow row, BulkContext context) {
    return columns(
        "company_id", context.companyId(),
        "event_type", row.text(EVENT),
        "name", row.text(RULE),
        "effective_from", row.date(FROM));
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    Optional<Long> rule = db.find("acc_rule", ruleKey(row, context));
    return rule.isEmpty()
        ? UploadSupport.ADD
        : db.action("acc_rule_line", columns("rule_id", rule.get(), "line_no", row.number(LINE)));
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    long rule =
        db.upsert(
            "acc_rule",
            ruleKey(row, context),
            columns("business_line", row.text(LINE_OF_BUSINESS), "currency", row.text(CURRENCY)),
            context,
            "Accounting rule");
    db.upsert(
        "acc_rule_line",
        columns("rule_id", rule, "line_no", row.number(LINE)),
        columns(
            "side", row.text(SIDE),
            "account_code", row.text(ACCOUNT),
            "amount_component", row.text(COMPONENT),
            "narration", row.text(NARRATION)),
        context,
        "Accounting rule line");
    return row.text(RULE) + " line " + row.text(LINE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select r.event_type, r.name, r.business_line, r.currency, r.effective_from, l.line_no,"
                + " l.side, l.account_code, l.amount_component, l.narration from acc_rule r"
                + " join acc_rule_line l on l.rule_id = r.id where r.company_id = ?"
                + " order by r.event_type, r.name, r.effective_from, l.line_no",
            companyId)
        .stream()
        .map(
            r ->
                exportRow(
                    EVENT, r.get("event_type"),
                    RULE, r.get("name"),
                    LINE_OF_BUSINESS, r.get("business_line"),
                    CURRENCY, r.get("currency"),
                    FROM, r.get("effective_from"),
                    LINE, r.get("line_no"),
                    SIDE, r.get("side"),
                    ACCOUNT, r.get("account_code"),
                    COMPONENT, r.get("amount_component"),
                    NARRATION, r.get("narration")))
        .toList();
  }
}
