package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.configpromo.engine.Analysis;
import com.iortatechnxt.brokerverse.configpromo.engine.ApplyActors;
import com.iortatechnxt.brokerverse.configpromo.engine.ApplyEngine;
import com.iortatechnxt.brokerverse.configpromo.engine.ApplyException;
import com.iortatechnxt.brokerverse.configpromo.engine.Compatibility;
import com.iortatechnxt.brokerverse.configpromo.engine.ConfigPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetReader;
import com.iortatechnxt.brokerverse.configpromo.engine.ImportAnalyzer;
import com.iortatechnxt.brokerverse.configpromo.engine.ImportOptions;
import com.iortatechnxt.brokerverse.configpromo.engine.Issue;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs the dry run of an import: the compatibility check, the analysis of every dataset and a trial
 * apply in a transaction that is always rolled back, so constraints of the database are found
 * before the approval. Nothing is changed.
 */
@Component
public class DryRunner {

  private final CatalogueService catalogue;
  private final PromotionSettings settings;
  private final JdbcTemplate jdbc;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the runner.
   *
   * @param catalogue catalogue
   * @param settings settings (versions)
   * @param jdbc database
   * @param txManager transaction manager
   * @param clock clock
   */
  public DryRunner(
      CatalogueService catalogue,
      PromotionSettings settings,
      JdbcTemplate jdbc,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.catalogue = catalogue;
    this.settings = settings;
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(txManager);
    this.clock = clock;
  }

  /**
   * Result of a dry run.
   *
   * @param compatibility fit of the package
   * @param analysis difference and findings (null when the package does not fit)
   * @param codes datasets of the import in load order
   */
  public record Outcome(Compatibility compatibility, Analysis analysis, List<String> codes) {}

  /**
   * Runs a dry run.
   *
   * @param pkg verified package
   * @param options choices
   * @param maker user preparing the import
   * @return outcome
   */
  public Outcome run(ConfigPackage pkg, ImportOptions options, String maker) {
    Compatibility compatibility =
        Compatibility.check(
            pkg.manifest(),
            catalogue.model(),
            settings.schemaVersion(),
            settings.platformVersion());
    if (!compatibility.compatible()) {
      return new Outcome(compatibility, null, List.of());
    }
    return tx.execute(
        status -> {
          status.setRollbackOnly();
          DatasetReader reader = catalogue.reader();
          ImportAnalyzer analyzer = new ImportAnalyzer(jdbc, reader, this::userExists);
          Analysis analysis = analyzer.analyze(pkg, options);
          List<String> codes = analyzer.datasets(pkg, options);
          if (!analysis.blockers().isEmpty()) {
            return new Outcome(compatibility, analysis, codes);
          }
          try {
            ApplyEngine.apply(
                jdbc,
                reader,
                analysis.diffs(),
                options.deactivate(),
                new ApplyActors(maker, maker, clock.instant()));
          } catch (ApplyException e) {
            List<Issue> blockers = new ArrayList<>(analysis.blockers());
            blockers.add(new Issue(null, null, "The trial apply failed: " + e.getMessage()));
            analysis = new Analysis(analysis.diffs(), blockers, analysis.warnings());
          }
          return new Outcome(compatibility, analysis, codes);
        });
  }

  private boolean userExists(String username) {
    Long n =
        jdbc.queryForObject(
            "select count(*) from sec_user where lower(username) = lower(?)", Long.class, username);
    return n != null && n > 0;
  }
}
