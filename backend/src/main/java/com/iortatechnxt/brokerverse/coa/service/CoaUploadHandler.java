package com.iortatechnxt.brokerverse.coa.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.coa.api.dto.GlAccountRequest;
import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.AccountLevel;
import com.iortatechnxt.brokerverse.coa.domain.GlAccountRepository;
import com.iortatechnxt.brokerverse.coa.domain.NegativeBalancePolicy;
import com.iortatechnxt.brokerverse.coa.domain.SubLedgerType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Chart of accounts upload {@value #CODE} (FRBS 2.3.1): one row per parent or child account.
 * Parents are listed before their children; a child's parent is either an existing account or an
 * account of an earlier row of the same file. A blank account code takes the next number of the
 * parent's numbering scheme (FRBS 2.3.2). Every account is created pending authorization
 * (maker-checker), exactly as on the chart screen. Sample: {@code
 * docs/samples/coa_upload_sample.xlsx}.
 */
@Component
public class CoaUploadHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "COA_ACCOUNTS";

  static final String PARENT = "Parent Code";
  static final String ACCOUNT = "Account Code";
  static final String NAME = "Account Name";
  static final String SHORT = "Short Code";
  static final String CLASS = "Class";
  static final String LEVEL = "Level";
  static final String CATEGORY = "Category";
  static final String CONTROL = "Control Account";
  static final String SUB_LEDGER = "Sub-ledger Type";
  static final String CURRENCIES = "Currencies";
  static final String MANUAL = "Manual Posting";
  static final String COST_CENTRE = "Cost Centre Required";
  static final String REVALUATION = "Revaluation Required";
  static final String REPORT_GROUP = "Report Group";
  static final String OPENED = "Opened On";
  static final String NEGATIVE = "Negative Balance";

  private static final Pattern CODE_PATTERN = Pattern.compile("[0-9A-Z.\\-]{1,30}");
  private static final Pattern CURRENCY = Pattern.compile("[A-Z]{3}");
  private static final int REMEMBERED_JOBS = 16;
  private static final Set<String> FREE_TEXT = Set.of(NAME, REPORT_GROUP);

  private final ChartOfAccountsService chart;
  private final GlAccountRepository accounts;

  /** Codes and short codes declared by earlier rows of the files being validated, by job. */
  private final Map<String, Set<String>> declared = Collections.synchronizedMap(new RecentJobs());

  /**
   * Creates the handler.
   *
   * @param chart chart of accounts service
   * @param accounts accounts (lookups)
   */
  public CoaUploadHandler(ChartOfAccountsService chart, GlAccountRepository accounts) {
    this.chart = chart;
    this.accounts = accounts;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Chart of accounts upload";
  }

  @Override
  public String permission() {
    return "COA_UPLOAD";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.optional(PARENT, "Code of the parent account (blank for a top-level group)", ""),
        BulkColumn.optional(
            ACCOUNT, "Account code; blank = next number of the parent's scheme", "1210.07"),
        BulkColumn.required(NAME, "Account name", "Premium Receivable - Motor"),
        BulkColumn.optional(SHORT, "Unique short code used on journal lines", "PRMOTOR"),
        BulkColumn.required(
            CLASS, "ASSET, LIABILITY, EQUITY, INCOME, EXPENSE or MEMORANDUM", "ASSET"),
        BulkColumn.required(LEVEL, "GROUP, MAIN, SUB or MICRO (one tier below the parent)", "SUB"),
        BulkColumn.optional(CATEGORY, "GL category code", ""),
        new BulkColumn(CONTROL, "Control account (Y/N)", false, Type.YES_NO, "N"),
        BulkColumn.optional(SUB_LEDGER, "Controlled sub-ledger (blank = none)", "POLICYHOLDER"),
        BulkColumn.optional(CURRENCIES, "Allowed currencies, comma separated (blank = all)", "PHP"),
        new BulkColumn(MANUAL, "Manual journals allowed (Y/N, blank = Y)", false, Type.YES_NO, "Y"),
        new BulkColumn(COST_CENTRE, "Cost centre mandatory (Y/N)", false, Type.YES_NO, "N"),
        new BulkColumn(REVALUATION, "Revalued at month end (Y/N)", false, Type.YES_NO, "N"),
        BulkColumn.optional(REPORT_GROUP, "Financial statement line", "Premium Receivable"),
        new BulkColumn(OPENED, "Opening date (blank = today)", false, Type.DATE, "2026-01-01"),
        BulkColumn.optional(NEGATIVE, "ALLOW, WARN or BLOCK (blank = ALLOW)", "WARN"));
  }

  @Override
  public String instructions() {
    return "List every parent before its children. Accounts are created pending authorization.";
  }

  @Override
  public String sanitize(String header, String value) {
    String clean = BulkImportHandler.super.sanitize(header, value);
    return FREE_TEXT.contains(header) ? clean : clean.toUpperCase(Locale.ROOT);
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(ACCOUNT);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    Set<String> seen = declared.computeIfAbsent(context.jobNo(), k -> new HashSet<>());
    List<String> errors = new ArrayList<>();
    enumValue(AccountClass.class, row.text(CLASS), CLASS, errors);
    enumValue(AccountLevel.class, row.text(LEVEL), LEVEL, errors);
    enumValue(SubLedgerType.class, row.text(SUB_LEDGER), SUB_LEDGER, errors);
    enumValue(NegativeBalancePolicy.class, row.text(NEGATIVE), NEGATIVE, errors);
    currencies(row.text(CURRENCIES), errors);
    String code = row.text(ACCOUNT);
    codeAndParent(code, row.text(PARENT), context.companyId(), seen, errors);
    shortCode(row.text(SHORT), context, seen, errors);
    if (errors.isEmpty() && code != null) {
      seen.add(code);
    }
    return errors;
  }

  private void codeAndParent(
      String code, String parent, Long companyId, Set<String> seen, List<String> errors) {
    if (code != null && !CODE_PATTERN.matcher(code).matches()) {
      errors.add(ACCOUNT + " may hold digits, capital letters, '.' and '-' only");
    } else if (code != null && accounts.existsByCompanyIdAndCode(companyId, code)) {
      errors.add("Account " + code + " already exists");
    }
    if (code == null && parent == null) {
      errors.add("Enter the account code of a top-level account");
    }
    if (parent != null && !knownParent(parent, companyId, seen)) {
      errors.add("Parent " + parent + " is neither an account nor an earlier row of the file");
    }
  }

  private boolean knownParent(String parent, Long companyId, Set<String> seen) {
    return seen.contains(parent) || accounts.existsByCompanyIdAndCode(companyId, parent);
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    GlAccountRequest request =
        new GlAccountRequest(
            context.companyId(),
            row.text(ACCOUNT),
            row.text(NAME),
            row.text(SHORT),
            AccountClass.valueOf(row.text(CLASS)),
            AccountLevel.valueOf(row.text(LEVEL)),
            row.text(PARENT),
            row.text(CATEGORY),
            row.yes(CONTROL),
            row.text(SUB_LEDGER) == null ? null : SubLedgerType.valueOf(row.text(SUB_LEDGER)),
            row.text(MANUAL) == null || row.yes(MANUAL),
            row.yes(COST_CENTRE),
            false,
            row.yes(REVALUATION),
            false,
            false,
            null,
            row.text(REPORT_GROUP),
            row.date(OPENED) == null ? context.businessDate() : row.date(OPENED),
            currencySet(row.text(CURRENCIES)),
            Set.of(),
            Set.of(),
            row.text(NEGATIVE) == null
                ? NegativeBalancePolicy.ALLOW
                : NegativeBalancePolicy.valueOf(row.text(NEGATIVE)));
    return chart.create(request).getCode();
  }

  private void shortCode(String shortCode, BulkContext context, Set<String> seen, List<String> e) {
    if (shortCode == null) {
      return;
    }
    String key = "#" + shortCode.toLowerCase(Locale.ROOT);
    boolean taken =
        seen.contains(key)
            || accounts
                .findByCompanyIdAndShortNameIgnoreCase(context.companyId(), shortCode)
                .isPresent();
    if (taken) {
      e.add("Short code " + shortCode + " is already used");
    } else {
      seen.add(key);
    }
  }

  private static <E extends Enum<E>> void enumValue(
      Class<E> type, String value, String header, List<String> errors) {
    if (value == null) {
      return;
    }
    boolean known = Arrays.stream(type.getEnumConstants()).anyMatch(c -> c.name().equals(value));
    if (!known) {
      errors.add(
          header
              + " must be one of "
              + Arrays.stream(type.getEnumConstants())
                  .map(Enum::name)
                  .collect(Collectors.joining(", ")));
    }
  }

  private static void currencies(String value, List<String> errors) {
    if (value != null
        && !currencySet(value).stream().allMatch(c -> CURRENCY.matcher(c).matches())) {
      errors.add(CURRENCIES + " must be three-letter codes separated by commas");
    }
  }

  private static Set<String> currencySet(String value) {
    if (value == null || value.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(value.split("[,;\\s]+"))
        .filter(s -> !s.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }

  /** The declared codes of the most recent jobs only. */
  private static final class RecentJobs extends LinkedHashMap<String, Set<String>> {
    private static final long serialVersionUID = 1L;

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Set<String>> eldest) {
      return size() > REMEMBERED_JOBS;
    }
  }
}
