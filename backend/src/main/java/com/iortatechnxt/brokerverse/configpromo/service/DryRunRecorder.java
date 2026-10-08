package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.configpromo.domain.ImportDataset;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportDataset.DryRunCounts;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportDatasetRepository;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImport;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImport.CheckResult;
import com.iortatechnxt.brokerverse.configpromo.domain.PromotionImport.Counts;
import com.iortatechnxt.brokerverse.configpromo.engine.Analysis;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalJson;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetDiff;
import com.iortatechnxt.brokerverse.configpromo.engine.ImportOptions;
import com.iortatechnxt.brokerverse.configpromo.engine.Issue;
import com.iortatechnxt.brokerverse.configpromo.service.ImportViews.Messages;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/** Keeps the result of a dry run on the import: summary, findings and the items per dataset. */
@Component
public class DryRunRecorder {

  private final ImportDatasetRepository lines;
  private final CatalogueService catalogue;

  /**
   * Creates the recorder.
   *
   * @param lines dataset lines
   * @param catalogue catalogue
   */
  public DryRunRecorder(ImportDatasetRepository lines, CatalogueService catalogue) {
    this.lines = lines;
    this.catalogue = catalogue;
  }

  /**
   * Records a dry run (in the caller's transaction).
   *
   * @param imp import
   * @param outcome dry run
   * @param options choices
   * @return the import
   */
  public PromotionImport record(
      PromotionImport imp, DryRunner.Outcome outcome, ImportOptions options) {
    CatalogueModel model = catalogue.model();
    lines.deleteByImportId(imp.getId());
    lines.flush();
    Analysis analysis = outcome.analysis();
    List<Issue> blockers = analysis == null ? List.of() : analysis.blockers();
    List<Issue> warnings = analysis == null ? List.of() : analysis.warnings();
    Messages messages =
        ImportViews.messages(
            model,
            outcome.compatibility().refusals(),
            outcome.compatibility().warnings(),
            blockers,
            warnings);
    int[] totals = new int[4];
    if (analysis != null) {
      int seq = 0;
      for (DatasetDiff diff : analysis.diffs()) {
        seq++;
        totals[0] += diff.added().size();
        totals[1] += diff.changed().size();
        totals[2] += diff.unchanged();
        totals[3] += diff.onlyInTarget().size();
        long datasetBlockers =
            blockers.stream().filter(b -> diff.code().equals(b.dataset())).count();
        lines.save(
            new ImportDataset(
                imp.getId(),
                diff.code(),
                seq,
                new DryRunCounts(
                    diff.added().size(),
                    diff.changed().size(),
                    diff.unchanged(),
                    diff.onlyInTarget().size(),
                    (int) datasetBlockers),
                CanonicalJson.text(ImportViews.items(model.model(diff.code()), diff))));
      }
    }
    imp.checked(
        new CheckResult(
            CanonicalJson.text(options),
            outcome.compatibility().compatible(),
            CanonicalJson.text(messages),
            messages.refusals().size() + blockers.size(),
            messages.notes().size() + warnings.size(),
            new Counts(totals[0], totals[1], totals[2], totals[3])));
    return imp;
  }

  /**
   * The options of an import.
   *
   * @param imp import
   * @return options
   */
  public ImportOptions options(PromotionImport imp) {
    return CanonicalJson.read(
        imp.getOptions().getBytes(StandardCharsets.UTF_8), ImportOptions.class);
  }

  /**
   * The findings of an import.
   *
   * @param imp import
   * @return findings
   */
  public Messages messages(PromotionImport imp) {
    return CanonicalJson.read(imp.getMessages().getBytes(StandardCharsets.UTF_8), Messages.class);
  }
}
