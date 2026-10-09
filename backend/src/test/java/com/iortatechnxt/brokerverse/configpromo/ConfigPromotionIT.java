package com.iortatechnxt.brokerverse.configpromo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportDataset;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportStatus;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImport;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.ConfigPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.ImportOptions;
import com.iortatechnxt.brokerverse.configpromo.engine.ManifestDataset;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageManifest;
import com.iortatechnxt.brokerverse.configpromo.service.ExportService;
import com.iortatechnxt.brokerverse.configpromo.service.ExportService.ExportCommand;
import com.iortatechnxt.brokerverse.configpromo.service.ImportApplier;
import com.iortatechnxt.brokerverse.configpromo.service.ImportService;
import com.iortatechnxt.brokerverse.configpromo.service.ImportService.UploadCommand;
import com.iortatechnxt.brokerverse.configpromo.service.PackageStore;
import com.iortatechnxt.brokerverse.configpromo.service.PromotionSettings;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The promotion lifecycle in the application: export, upload with signature and compatibility
 * checks, dry run, maker-checker approval, apply with reconciliation and snapshot, rollback, and
 * the refusal of a deactivation of a product that transactions use.
 */
@IntegrationTest
class ConfigPromotionIT {

  private static final String ADMIN = "admin";
  private static final String APPROVER = "cfgapprover";
  private static final String VALIDITY = "QUOTATION_VALIDITY_DAYS";
  private static final String NEW_PARAMETER = "CFP_IT_REMINDER_DAYS";

  @Autowired private ExportService exports;
  @Autowired private ImportService imports;
  @Autowired private ImportApplier applier;
  @Autowired private PackageStore store;
  @Autowired private PromotionSettings settings;
  @Autowired private AsUser asUser;
  @Autowired private JdbcTemplate jdbc;

  private ConfigPackage export(List<String> datasets) {
    PromotionPackage pkg =
        asUser.run(ADMIN, () -> exports.export(new ExportCommand(datasets, false, null, "test")));
    return asUser.run(ADMIN, () -> store.open(pkg));
  }

  private PromotionImport upload(byte[] content, ImportOptions options) {
    return asUser.run(
        ADMIN,
        () ->
            imports.upload(
                new UploadCommand(
                    "package.zip", content, options, "CR-1", "Promotion test", false)));
  }

  private static ImportOptions all() {
    return new ImportOptions(Set.of(), Set.of(), false);
  }

  /** Approves, retrying while a scheduled job of the test context holds its lock. */
  private PromotionImport approve(Long id) throws InterruptedException {
    for (int attempt = 0; ; attempt++) {
      try {
        return asUser.run(APPROVER, () -> applier.approve(id, "Checked"));
      } catch (BusinessRuleException e) {
        if (!"CONFIG_IMPORT_JOB_RUNNING".equals(e.getCode()) || attempt > 20) {
          throw e;
        }
        Thread.sleep(500);
      }
    }
  }

  private String parameter(String key) {
    List<String> v =
        jdbc.queryForList(
            "select param_value from sys_parameter where param_key = ?", String.class, key);
    return v.isEmpty() ? null : v.get(0);
  }

  @Test
  void aChangedPackageIsAppliedAfterASecondUsersApprovalAndRolledBackFromItsSnapshot()
      throws InterruptedException {
    String before = parameter(VALIDITY);
    ConfigPackage pkg = export(List.of("SYS_PARAMETER"));
    byte[] changed =
        PackageRewriter.withRows(
            pkg,
            "SYS_PARAMETER",
            rows -> {
              List<Map<String, Object>> out = new ArrayList<>();
              for (Map<String, Object> r : rows) {
                Map<String, Object> copy = new HashMap<>(r);
                if (VALIDITY.equals(r.get("param_key"))) {
                  copy.put("param_value", "45");
                  Map<String, Object> added = new HashMap<>(r);
                  added.put("param_key", NEW_PARAMETER);
                  added.put("description", "Days before the reminder of the promotion test");
                  out.add(added);
                }
                out.add(copy);
              }
              return out;
            },
            settings.signer());

    PromotionImport checked = upload(changed, all());
    assertThat(checked.getStatus()).isEqualTo(ImportStatus.CHECKED);
    assertThat(checked.getChangedCount()).isEqualTo(1);
    assertThat(checked.getAddedCount()).isEqualTo(1);
    assertThat(checked.getBlockerCount()).isZero();
    assertThat(parameter(VALIDITY)).as("the dry run changes nothing").isEqualTo(before);

    asUser.run(ADMIN, () -> imports.submit(checked.getId()));
    assertThatThrownBy(() -> asUser.run(ADMIN, () -> applier.approve(checked.getId(), "self")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cannot be approved by the user who prepared it");

    PromotionImport applied = approve(checked.getId());
    assertThat(applied.getStatus()).as(applied.getErrorMessage()).isEqualTo(ImportStatus.APPLIED);
    assertThat(applied.getSnapshotPackageId()).isNotNull();
    assertThat(parameter(VALIDITY)).isEqualTo("45");
    assertThat(parameter(NEW_PARAMETER)).isEqualTo(before);
    List<ImportDataset> lines = asUser.run(ADMIN, () -> imports.datasets(applied.getId()));
    assertThat(lines)
        .singleElement()
        .satisfies(
            l -> {
              assertThat(l.getReconciled()).isTrue();
              assertThat(l.getPackageSha256()).isEqualTo(l.getTargetSha256());
              assertThat(l.getInserted()).isEqualTo(1);
              assertThat(l.getUpdated()).isEqualTo(1);
            });
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_log where entity_type = 'ConfigImport' and entity_id = ?",
                Long.class,
                applied.getImportNo()))
        .isGreaterThanOrEqualTo(4);

    PromotionImport rollback = asUser.run(ADMIN, () -> imports.rollback(applied.getId()));
    assertThat(rollback.getChangedCount()).isEqualTo(1);
    assertThat(rollback.getOnlyInTargetCount()).isEqualTo(1);
    asUser.run(ADMIN, () -> imports.submit(rollback.getId()));
    assertThat(approve(rollback.getId()).getStatus()).isEqualTo(ImportStatus.APPLIED);
    assertThat(parameter(VALIDITY)).isEqualTo(before);

    jdbc.update("delete from sys_parameter where param_key = ?", NEW_PARAMETER);
  }

  @Test
  void reImportingAnUnchangedPackageChangesNothing() {
    ConfigPackage pkg = export(List.of("LOV_TYPE", "CAT_PRODUCT_LINE", "SEC_ROLE"));

    PromotionImport imp =
        upload(PackageRewriter.withRows(pkg, "X", r -> r, settings.signer()), all());

    assertThat(imp.getAddedCount() + imp.getChangedCount() + imp.getOnlyInTargetCount()).isZero();
    assertThat(imp.getUnchangedCount()).isEqualTo(pkg.manifest().totalRows());
  }

  @Test
  void aPackageChangedAfterItsExportIsRefused() {
    ConfigPackage pkg = export(List.of("CUR_CURRENCY"));
    byte[] foreign =
        PackageRewriter.withManifest(
            pkg,
            m -> m,
            new com.iortatechnxt.brokerverse.configpromo.engine.PackageSigner(
                "a-key-that-is-not-the-platform-key"));

    assertThatThrownBy(() -> upload(foreign, all()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("not signed with the signing key");
  }

  @Test
  void aPackageOfAnotherSchemaIsRefused() {
    ConfigPackage pkg = export(List.of("CUR_CURRENCY"));
    byte[] other =
        PackageRewriter.withManifest(
            pkg,
            m ->
                new PackageManifest(
                    m.format(),
                    m.formatVersion(),
                    m.packageId(),
                    m.platformVersion(),
                    "9999",
                    m.sourceEnvironment(),
                    m.createdBy(),
                    m.createdAt(),
                    m.mode(),
                    m.includeUsers(),
                    m.description(),
                    m.datasets().stream()
                        .map(
                            d ->
                                new ManifestDataset(
                                    d.code(),
                                    d.name(),
                                    d.group(),
                                    d.module(),
                                    d.rows(),
                                    d.file(),
                                    d.sha256(),
                                    d.contentSha256(),
                                    d.fingerprint() + ",iso_numeric:int4"))
                        .toList()),
            settings.signer());

    PromotionImport imp = upload(other, all());

    assertThat(imp.isCompatible()).isFalse();
    assertThat(imp.getMessages()).contains("other fields here than in the source environment");
    assertThatThrownBy(() -> asUser.run(ADMIN, () -> imports.submit(imp.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("blockers");
  }

  @Test
  void aProductThatTransactionsUseIsNotDeactivated() {
    String used = jdbc.queryForObject("select min(product_code) from acc_account", String.class);
    ConfigPackage pkg = export(List.of("CAT_PRODUCT"));
    byte[] without =
        PackageRewriter.withRows(
            pkg,
            "CAT_PRODUCT",
            rows -> rows.stream().filter(r -> !used.equals(r.get("code"))).toList(),
            settings.signer());

    PromotionImport kept = upload(without, all());
    PromotionImport deactivate =
        upload(without, new ImportOptions(Set.of(), Set.of("CAT_PRODUCT"), false));

    assertThat(kept.getOnlyInTargetCount()).isEqualTo(1);
    assertThat(kept.getBlockerCount()).isZero();
    assertThat(deactivate.getBlockerCount()).isEqualTo(1);
    assertThat(deactivate.getMessages()).contains("it cannot be deactivated", used);
    assertThatThrownBy(() -> asUser.run(ADMIN, () -> imports.submit(deactivate.getId())))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void onlyThePreparerChangesAnImportAndProductionNeedsAChangeReference() {
    ConfigPackage pkg = export(List.of("CUR_CURRENCY"));
    PromotionImport imp =
        upload(PackageRewriter.withRows(pkg, "X", r -> r, settings.signer()), all());

    Supplier<PromotionImport> cancel = () -> imports.cancel(imp.getId());
    assertThatThrownBy(() -> asUser.run(APPROVER, cancel))
        .hasMessageContaining("Only the user who prepared the import");
    assertThat(asUser.run(ADMIN, cancel).getStatus()).isEqualTo(ImportStatus.CANCELLED);
    assertThat(settings.production()).isFalse();
  }
}
