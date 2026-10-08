package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** D0-11 Bank accounts, on Bank Accounts and Checks (Disbursement). */
@Component
public class BankAccountUpload extends ConfigUploadHandler {

  static final String CODE = "Bank account code";
  static final String NAME = "Name";
  static final String BANK = "Bank";
  static final String NUMBER = "Account number";
  static final String CURRENCY = "Currency";
  static final String ACCOUNT = "GL account";
  static final String CLEARING = "PDC clearing account";
  static final String BRANCH = "Branch code";
  static final String FORMAT = "Payment notification format";

  private static final String TABLE = "pay_bank_account";
  private static final List<String> FORMATS = List.of("FIXED_WIDTH", "CSV", "DCTF");
  private static final Pattern CODE_FORMAT = Pattern.compile("[A-Z0-9_-]{1,20}");

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public BankAccountUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_BANK_ACCOUNT";
  }

  @Override
  public String templateId() {
    return "D0-11";
  }

  @Override
  public String title() {
    return "Bank accounts";
  }

  @Override
  public String screen() {
    return "Disbursement > Bank Accounts and Checks";
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
        BulkColumn.required(CODE, "Code of the bank account", "BDO-CA-001")
            .format("Up to 20 capitals"),
        BulkColumn.required(NAME, "Name of the account", "Makati current account"),
        BulkColumn.required(BANK, "Bank name", "BDO Unibank, Inc."),
        BulkColumn.required(NUMBER, "As printed by the bank", "000123456789"),
        BulkColumn.required(CURRENCY, "Currency of the account", BulkColumn.BASE_CURRENCY_EXAMPLE),
        BulkColumn.required(ACCOUNT, "Account of the chart with a bank category", "1110.01")
            .master("account of the chart of accounts"),
        BulkColumn.optional(CLEARING, "Account of the post-dated checks", "2241")
            .master("account of the chart of accounts"),
        BulkColumn.optional(BRANCH, "Branch of the account", "HO").master("branch"),
        BulkColumn.required(FORMAT, "Format of the bank advice", "CSV")
            .codes(FORMATS.toArray(String[]::new)));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    Long company = context.companyId();
    if (!CODE_FORMAT.matcher(row.text(CODE)).matches()) {
      errors.add(error(CODE, "use up to 20 capital letters, digits, - or _"));
    }
    if (!db.currency(row.text(CURRENCY))) {
      errors.add(error(CURRENCY, row.text(CURRENCY) + " is not an active currency"));
    }
    if (!db.exists(
        "select 1 from coa_account a join coa_category c on c.id = a.category_id"
            + " where a.company_id = ? and a.code = ? and c.bank_category",
        company,
        row.text(ACCOUNT))) {
      errors.add(error(ACCOUNT, row.text(ACCOUNT) + " is not an account of a bank category"));
    }
    Optional<String> other =
        db.text(
            "select code from pay_bank_account where company_id = ? and gl_account_code = ? and code <> ?",
            company,
            row.text(ACCOUNT),
            row.text(CODE));
    other.ifPresent(o -> errors.add(error(ACCOUNT, "already the account of bank account " + o)));
    if (row.text(CLEARING) != null && !db.account(company, row.text(CLEARING))) {
      errors.add(error(CLEARING, row.text(CLEARING) + " is not an account of the company"));
    }
    if (row.text(BRANCH) != null && db.branchId(company, row.text(BRANCH)).isEmpty()) {
      errors.add(error(BRANCH, row.text(BRANCH) + " is not a branch of the company"));
    }
    oneOf(errors, FORMAT, row.text(FORMAT), FORMATS);
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
            "name",
            row.text(NAME),
            "bank_name",
            row.text(BANK),
            "account_no",
            row.text(NUMBER),
            "currency",
            row.text(CURRENCY),
            "gl_account_code",
            row.text(ACCOUNT),
            "pdc_clearing_account_code",
            row.text(CLEARING),
            "branch_id",
            row.text(BRANCH) == null
                ? null
                : db.branchId(context.companyId(), row.text(BRANCH)).orElse(null),
            "notification_format",
            row.text(FORMAT)),
        context,
        "Bank account");
    return row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select a.code, a.name, a.bank_name, a.account_no, a.currency, a.gl_account_code,"
                + " a.pdc_clearing_account_code, b.code as branch_code, a.notification_format"
                + " from pay_bank_account a left join org_branch b on b.id = a.branch_id"
                + " where a.company_id = ? order by a.code",
            companyId)
        .stream()
        .map(
            r ->
                exportRow(
                    CODE, r.get("code"),
                    NAME, r.get("name"),
                    BANK, r.get("bank_name"),
                    NUMBER, r.get("account_no"),
                    CURRENCY, r.get("currency"),
                    ACCOUNT, r.get("gl_account_code"),
                    CLEARING, r.get("pdc_clearing_account_code"),
                    BRANCH, r.get("branch_code"),
                    FORMAT, r.get("notification_format")))
        .toList();
  }
}
