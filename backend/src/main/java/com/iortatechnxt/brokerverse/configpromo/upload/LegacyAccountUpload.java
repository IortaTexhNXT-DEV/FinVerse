package com.iortatechnxt.brokerverse.configpromo.upload;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * D0-09 Legacy control accounts: the account of the chart that carries each kind of migrated item
 * until it runs off; the legacy GL accounts it replaces go into the code map GL_ACCOUNT of the
 * Migration Console (target: the account of the chart).
 */
@Component
public class LegacyAccountUpload extends ConfigUploadHandler {

  static final String LEGACY = "Legacy account";
  static final String COMPONENT = "Component";
  static final String ACCOUNT = "BIBS account code";
  static final String LEGACY_GL = "Legacy GL accounts";
  static final String REMARKS = "Remarks";

  /** The legacy control accounts of the migration design. */
  static final List<String> LEGACY_ACCOUNTS =
      List.of(
          "Premium Receivable - Legacy",
          "PR 2307 - Legacy",
          "Due to Insurers - Legacy",
          "Commission Receivable - Legacy",
          "Unrealised Commission - Legacy",
          "Deferred Output VAT - Legacy",
          "Unapplied Collections - Legacy",
          "Migration Clearing");

  private static final String CODE_MAP = "GL_ACCOUNT";

  private final LegacyCodeMaps codeMaps;

  /**
   * Creates the handler.
   *
   * @param db database support
   * @param codeMaps code maps of the Migration Console
   */
  public LegacyAccountUpload(UploadSupport db, LegacyCodeMaps codeMaps) {
    super(db);
    this.codeMaps = codeMaps;
  }

  @Override
  public String code() {
    return "CFG_LEGACY_ACCOUNT";
  }

  @Override
  public String templateId() {
    return "D0-09";
  }

  @Override
  public String title() {
    return "Legacy control accounts";
  }

  @Override
  public String screen() {
    return "General Ledger > Chart of Accounts";
  }

  @Override
  public String permission() {
    return "MIG_MAPPING_EDIT";
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
        BulkColumn.required(LEGACY, "One row per account", "Migration Clearing")
            .values(LEGACY_ACCOUNTS.toArray(String[]::new)),
        BulkColumn.optional(COMPONENT, "Component the account holds", "Basic premium")
            .when("Premium Receivable - Legacy"),
        BulkColumn.required(ACCOUNT, "Account in the production chart", "1990.01")
            .master("account of the chart of accounts"),
        BulkColumn.optional(
                LEGACY_GL, "The legacy accounts it replaces (code map GL_ACCOUNT)", "1210-01")
            .format("Account codes separated by commas"),
        BulkColumn.optional(
            REMARKS, "End state after the run-off", "Nets to 0.00 after the opening"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(LEGACY) + "|" + row.text(COMPONENT);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = errors();
    oneOf(errors, LEGACY, row.text(LEGACY), LEGACY_ACCOUNTS);
    if (LEGACY_ACCOUNTS.get(0).equals(row.text(LEGACY)) && row.text(COMPONENT) == null) {
      errors.add(error(COMPONENT, "mandatory for the premium receivable, one row per component"));
    }
    if (!db.account(context.companyId(), row.text(ACCOUNT))) {
      errors.add(error(ACCOUNT, row.text(ACCOUNT) + " is not an account of the company"));
    }
    if (!LegacyCodeMaps.codes(row.text(LEGACY_GL)).isEmpty()) {
      codeMaps.blocked(CODE_MAP).ifPresent(m -> errors.add(error(LEGACY_GL, m)));
    }
    return errors;
  }

  private static String description(BulkRow row) {
    return row.text(COMPONENT) == null
        ? row.text(LEGACY)
        : row.text(LEGACY) + " (" + row.text(COMPONENT) + ")";
  }

  @Override
  public String previewAction(BulkRow row, BulkContext context) {
    List<String> codes = LegacyCodeMaps.codes(row.text(LEGACY_GL));
    boolean mapped =
        codes.stream()
            .allMatch(
                c ->
                    db.exists(
                        "select 1 from mig_code_map_entry e join mig_code_map_version v on v.id = e.version_id"
                            + " where v.set_code = ? and e.legacy_code = ? and e.target_code = ?",
                        CODE_MAP,
                        c,
                        row.text(ACCOUNT)));
    return mapped ? UploadSupport.UPDATE : UploadSupport.ADD;
  }

  @Override
  protected String apply(BulkRow row, BulkContext context) {
    codeMaps.map(
        context.companyId(),
        CODE_MAP,
        LegacyCodeMaps.codes(row.text(LEGACY_GL)),
        row.text(ACCOUNT),
        description(row),
        context.jobNo());
    return row.text(ACCOUNT);
  }

  @Override
  public List<Map<String, String>> exportRows(Long companyId) {
    Map<String, List<Map<String, Object>>> byAccount = new LinkedHashMap<>();
    db.rows(
            "select e.target_code, e.legacy_code, e.legacy_description, e.remarks"
                + " from mig_code_map_entry e join mig_code_map_version v on v.id = e.version_id"
                + " where v.set_code = ? and v.status = 'APPROVED' order by e.target_code, e.legacy_code",
            CODE_MAP)
        .forEach(
            r ->
                byAccount
                    .computeIfAbsent(
                        String.valueOf(r.get("target_code")), k -> new java.util.ArrayList<>())
                    .add(r));
    return byAccount.entrySet().stream()
        .filter(
            e -> {
              String d = String.valueOf(e.getValue().get(0).get("legacy_description"));
              return LEGACY_ACCOUNTS.stream().anyMatch(d::startsWith);
            })
        .map(
            e -> {
              String description = String.valueOf(e.getValue().get(0).get("legacy_description"));
              int open = description.indexOf(" (");
              return exportRow(
                  LEGACY,
                  open < 0 ? description : description.substring(0, open),
                  COMPONENT,
                  open < 0 ? null : description.substring(open + 2, description.length() - 1),
                  ACCOUNT,
                  e.getKey(),
                  LEGACY_GL,
                  e.getValue().stream()
                      .map(r -> String.valueOf(r.get("legacy_code")))
                      .collect(Collectors.joining(", ")));
            })
        .toList();
  }
}
