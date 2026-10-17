package com.iortatechnxt.brokerverse.configpromo;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.configpromo.catalogue.ConfigCatalogue;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.Analysis;
import com.iortatechnxt.brokerverse.configpromo.engine.ApplyActors;
import com.iortatechnxt.brokerverse.configpromo.engine.ApplyEngine;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.ConfigPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetDiff;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetReader;
import com.iortatechnxt.brokerverse.configpromo.engine.ImportAnalyzer;
import com.iortatechnxt.brokerverse.configpromo.engine.ImportOptions;
import com.iortatechnxt.brokerverse.configpromo.engine.ManifestDataset;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageWriter;
import com.iortatechnxt.brokerverse.configpromo.engine.Reconciler;
import com.iortatechnxt.brokerverse.configpromo.engine.Reconciliation;
import com.iortatechnxt.brokerverse.configpromo.engine.SchemaReader;
import com.iortatechnxt.brokerverse.configpromo.service.ExportService;
import com.iortatechnxt.brokerverse.configpromo.service.ExportService.ExportCommand;
import com.iortatechnxt.brokerverse.configpromo.service.PackageStore;
import com.iortatechnxt.brokerverse.configpromo.service.PromotionSettings;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Promotion into another environment: the configuration of the seeded test database is exported,
 * then imported into a fresh database of the same server that has only the migrations (no seed
 * data). Afterwards the configuration of both is equal dataset by dataset (checksums), the fresh
 * database holds no transactions, references point to the rows with the same natural keys, and a
 * second import of the same package changes nothing.
 */
@IntegrationTest
class ConfigPromotionFreshDatabaseIT {

  @Autowired private ExportService exports;
  @Autowired private PackageStore store;
  @Autowired private PromotionSettings settings;
  @Autowired private AsUser asUser;
  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbc;

  @Test
  void theConfigurationOfTheSeededDatabaseArrivesWholeInAFreshDatabaseWithoutTransactions()
      throws SQLException {
    PromotionPackage exported =
        asUser.run(
            "admin", () -> exports.export(new ExportCommand(List.of(), false, null, "fresh")));
    ConfigPackage pkg = asUser.run("admin", () -> store.open(exported));
    String database = "cfp_fresh_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    jdbc.execute("create database " + database);
    try (SingleConnectionDataSource fresh = freshDataSource(database)) {
      Flyway.configure()
          .dataSource(fresh)
          .locations("classpath:db/migration")
          .outOfOrder(true)
          .placeholders(Map.of("runtime_role", "brokerverse_runtime"))
          .load()
          .migrate();
      JdbcTemplate target = new JdbcTemplate(fresh);
      CatalogueModel model = new CatalogueModel(ConfigCatalogue.load(), SchemaReader.read(target));
      TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(fresh));

      List<Reconciliation> reconciliation =
          tx.execute(
              s -> {
                DatasetReader reader = new DatasetReader(target, model);
                ImportAnalyzer analyzer = new ImportAnalyzer(target, reader, u -> true);
                ImportOptions options = new ImportOptions(Set.of(), Set.of(), false);
                Analysis analysis = analyzer.analyze(pkg, options);
                assertThat(analysis.blockers()).isEmpty();
                ApplyEngine.apply(
                    target,
                    reader,
                    analysis.diffs(),
                    Set.of(),
                    new ApplyActors("admin", "cfgapprover", Instant.now()));
                return Reconciler.reconcile(reader, pkg, analyzer.datasets(pkg, options));
              });

      assertThat(reconciliation).hasSize(pkg.manifest().datasets().size());
      assertThat(reconciliation.stream().filter(r -> !r.matched()).toList()).isEmpty();
      assertSameConfiguration(pkg, target, model);
      assertNoTransactions(target);
      assertReferencesFollowNaturalKeys(target);
      List<DatasetDiff> again =
          tx.execute(
              s ->
                  new ImportAnalyzer(target, new DatasetReader(target, model), u -> true)
                      .analyze(pkg, new ImportOptions(Set.of(), Set.of(), false))
                      .diffs());
      assertThat(again).allMatch(DatasetDiff::noChange);
    } finally {
      jdbc.execute("drop database if exists " + database);
    }
  }

  private void assertSameConfiguration(
      ConfigPackage pkg, JdbcTemplate target, CatalogueModel model) {
    List<String> codes = pkg.manifest().datasets().stream().map(ManifestDataset::code).toList();
    PackageWriter.Written copy =
        PackageWriter.write(
            new DatasetReader(target, model),
            codes,
            new PackageWriter.Header(
                UUID.randomUUID().toString(),
                "test",
                "test",
                "FRESH",
                "admin",
                Instant.now().toString(),
                "FULL",
                false,
                "copy"),
            settings.signer());
    for (ManifestDataset source : pkg.manifest().datasets()) {
      ManifestDataset copied = copy.manifest().dataset(source.code());
      assertThat(copied.contentSha256()).as(source.code()).isEqualTo(source.contentSha256());
    }
  }

  private static void assertNoTransactions(JdbcTemplate target) {
    for (String table :
        List.of(
            "acc_account",
            "crm_client",
            "jnl_batch",
            "gl_ledger_entry",
            "bkg_invoice",
            "csh_receipt",
            "quo_quotation",
            "sbm_policy")) {
      assertThat(target.queryForObject("select count(*) from " + table, Long.class))
          .as(table)
          .isZero();
    }
  }

  private void assertReferencesFollowNaturalKeys(JdbcTemplate target) {
    String sql =
        """
        select c.code || '/' || b.code from org_branch b join org_company c on c.id = b.company_id
        order by 1
        """;
    assertThat(target.queryForList(sql, String.class))
        .isEqualTo(jdbc.queryForList(sql, String.class));
    String accounts =
        """
        select c.code || '/' || a.code || '<' || coalesce(p.code, '-')
        from coa_account a join org_company c on c.id = a.company_id
        left join coa_account p on p.id = a.parent_id order by 1
        """;
    assertThat(target.queryForList(accounts, String.class))
        .isEqualTo(jdbc.queryForList(accounts, String.class));
  }

  private SingleConnectionDataSource freshDataSource(String database) throws SQLException {
    try (Connection c = dataSource.getConnection()) {
      String url = c.getMetaData().getURL();
      String user = c.getMetaData().getUserName();
      int slash = url.indexOf('/', "jdbc:postgresql://".length());
      int query = url.indexOf('?', slash);
      String freshUrl =
          url.substring(0, slash + 1) + database + (query < 0 ? "" : url.substring(query));
      SingleConnectionDataSource ds = new SingleConnectionDataSource(freshUrl, user, "", true);
      ds.setAutoCommit(true);
      return ds;
    }
  }
}
