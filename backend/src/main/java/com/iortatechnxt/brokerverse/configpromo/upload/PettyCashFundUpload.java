package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * D0-13 Petty cash funds, on Petty Cash (Payables & Cash): a new fund starts with its imprest as
 * cash on hand; the cash of an existing fund is never changed by an upload.
 */
@Component
public class PettyCashFundUpload extends ConfigUploadHandler {

  static final String BRANCH = "Branch code";
  static final String CODE = "Fund code";
  static final String NAME = "Name";
  static final String CUSTODIAN = "Custodian";
  static final String ACCOUNT = "GL account";
  static final String BANK = "Replenishment bank account";
  static final String CURRENCY = "Currency";
  static final String IMPREST = "Imprest amount";

  private static final String TABLE = "pay_petty_cash_fund";

  /**
   * Creates the handler.
   *
   * @param db database support
   */
  public PettyCashFundUpload(UploadSupport db) {
    super(db);
  }

  @Override
  public String code() {
    return "CFG_PETTY_CASH_FUND";
  }

  @Override
  public String templateId() {
    return "D0-13";
  }

  @Override
  public String title() {
    return "Petty cash funds";
  }

  @Override
  public String screen() {
    return "Payables & Cash > Petty Cash";
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
        BulkColumn.required(BRANCH, "Branch of the fund", "HO").master("branch"),
        BulkColumn.required(CODE, "Code of the fund", "PCF-HO"),
        BulkColumn.required(NAME, "Name of the fund", "Head Office petty cash"),
        BulkColumn.required(CUSTODIAN, "Employee number of the custodian", "1203344")
            .master("employee"),
        BulkColumn.required(ACCOUNT, "Account of the fund", "1101.01")
            .master("account of the chart of accounts"),
        BulkColumn.required(BANK, "Bank account that replenishes the fund", "BNK-CA-001")
            .master("bank account"),
        BulkColumn.required(CURRENCY, "Currency of the fund", BulkColumn.BASE_CURRENCY_EXAMPLE),
        new BulkColumn(IMPREST, "Amount of the fund", true, Type.NUMBER, "20000"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    Long company = context.companyId();
    check(
        errors,
        db.branchId(company, row.text(BRANCH)).isEmpty(),
        BRANCH,
        row.text(BRANCH) + " is not a branch of the company");
    check(
        errors,
        custodian(row, context).isEmpty() && !sameCustodian(row, context),
        CUSTODIAN,
        row.text(CUSTODIAN) + " is not an active employee of the company");
    check(
        errors,
        !db.account(company, row.text(ACCOUNT)),
        ACCOUNT,
        row.text(ACCOUNT) + " is not an account of the company");
    check(
        errors,
        bank(row, context).isEmpty(),
        BANK,
        row.text(BANK) + " is not a bank account of the company");
    check(
        errors,
        !db.currency(row.text(CURRENCY)),
        CURRENCY,
        row.text(CURRENCY) + " is not an active currency");
    BigDecimal imprest = row.number(IMPREST);
    check(errors, imprest.signum() <= 0, IMPREST, "must be above zero");
    Optional<String> balance =
        db.text(
            "select cast(cash_balance as varchar) from pay_petty_cash_fund where company_id = ? and code = ?",
            company,
            row.text(CODE));
    if (balance.isPresent() && new BigDecimal(balance.get()).compareTo(imprest) > 0) {
      errors.add(error(IMPREST, "is below the cash on hand of the fund (" + balance.get() + ")"));
    }
    return errors;
  }

  private boolean sameCustodian(BulkRow row, BulkContext context) {
    return db.exists(
        "select 1 from pay_petty_cash_fund where company_id = ? and code = ? and custodian = ?",
        context.companyId(),
        row.text(CODE),
        row.text(CUSTODIAN));
  }

  private Optional<String> custodian(BulkRow row, BulkContext context) {
    return db.text(
        "select full_name from org_employee where company_id = ? and employee_no = ? and active",
        context.companyId(),
        row.text(CUSTODIAN));
  }

  private Optional<Long> bank(BulkRow row, BulkContext context) {
    return db.id(
        "select id from pay_bank_account where company_id = ? and code = ?",
        context.companyId(),
        row.text(BANK));
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
    boolean added = UploadSupport.ADD.equals(previewAction(row, context));
    Map<String, Object> values =
        columns(
            "branch_id", db.branchId(context.companyId(), row.text(BRANCH)).orElse(null),
            "name", row.text(NAME),
            "custodian", custodian(row, context).orElse(row.text(CUSTODIAN)),
            "gl_account_code", row.text(ACCOUNT),
            "replenish_bank_account_id", bank(row, context).orElse(null),
            "currency", row.text(CURRENCY),
            "imprest_amount", row.number(IMPREST));
    if (added) {
      values.put("cash_balance", row.number(IMPREST));
      values.put("established_on", context.businessDate());
    }
    db.upsert(TABLE, key(row, context), values, context, "Petty cash fund");
    return row.text(CODE);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    return db
        .rows(
            "select b.code as branch_code, f.code, f.name, coalesce(e.employee_no, f.custodian) as custodian,"
                + " f.gl_account_code, a.code as bank_code, f.currency, f.imprest_amount"
                + " from pay_petty_cash_fund f join org_branch b on b.id = f.branch_id"
                + " join pay_bank_account a on a.id = f.replenish_bank_account_id"
                + " left join org_employee e on e.company_id = f.company_id and e.full_name = f.custodian"
                + " where f.company_id = ? order by f.code",
            companyId)
        .stream()
        .map(
            r ->
                exportRow(
                    BRANCH, r.get("branch_code"),
                    CODE, r.get("code"),
                    NAME, r.get("name"),
                    CUSTODIAN, r.get("custodian"),
                    ACCOUNT, r.get("gl_account_code"),
                    BANK, r.get("bank_code"),
                    CURRENCY, r.get("currency"),
                    IMPREST, r.get("imprest_amount")))
        .toList();
  }
}
