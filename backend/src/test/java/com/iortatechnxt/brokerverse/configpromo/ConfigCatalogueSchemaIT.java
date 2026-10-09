package com.iortatechnxt.brokerverse.configpromo;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.configpromo.catalogue.CatalogueDataset;
import com.iortatechnxt.brokerverse.configpromo.catalogue.ConfigCatalogue;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.ColumnInfo;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetModel;
import com.iortatechnxt.brokerverse.configpromo.service.CatalogueService;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The configuration catalogue fits the migrated database: every table is either a configuration
 * dataset or excluded with its reason (a new table must be classified), every dataset resolves (key
 * columns exist, references point to configuration, no dependency cycle), and no dataset carries a
 * database id that is not remapped by natural key.
 */
@IntegrationTest
class ConfigCatalogueSchemaIT {

  @Autowired private CatalogueService catalogue;

  @Test
  void everyTableIsADatasetOrExcludedWithItsReason() {
    CatalogueModel model = catalogue.model();
    ConfigCatalogue cat = model.catalogue();
    Set<String> unclassified = new TreeSet<>(model.tables().keySet());
    unclassified.removeAll(cat.excludedTables().keySet());
    cat.datasets().forEach(d -> unclassified.remove(d.table()));

    assertThat(unclassified).as("tables neither in the catalogue nor excluded").isEmpty();
    Set<String> stale = new TreeSet<>(cat.excludedTables().keySet());
    stale.removeAll(model.tables().keySet());
    assertThat(stale).as("excluded tables that no longer exist").isEmpty();
  }

  @Test
  void everyDatasetResolvesAgainstTheSchema() {
    CatalogueModel model = catalogue.model();

    assertThat(model.errors()).isEmpty();
    assertThat(model.loadOrder()).hasSize(model.catalogue().datasets().size());
  }

  @Test
  void noDatabaseIdLeavesTheEnvironment() {
    CatalogueModel model = catalogue.model();
    List<String> raw = new ArrayList<>();
    for (CatalogueDataset d : model.catalogue().datasets()) {
      DatasetModel m = model.model(d.code());
      for (String column : m.columns()) {
        ColumnInfo info = m.table().column(column);
        boolean idLike = column.endsWith("_id") && info.type().startsWith("int");
        if (idLike && !m.references().containsKey(column)) {
          raw.add(d.code() + "." + column);
        }
      }
    }
    assertThat(raw).as("id columns exported without remapping").isEmpty();
  }
}
