package com.iortatechnxt.brokerverse.configpromo;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.configpromo.catalogue.ConfigCatalogue;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetModel;
import com.iortatechnxt.brokerverse.configpromo.engine.Deactivation;
import com.iortatechnxt.brokerverse.configpromo.engine.ForeignKey;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageWriter;
import com.iortatechnxt.brokerverse.configpromo.engine.TableSchema;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The catalogue resolved against a schema: references, dependency order, deactivation. */
class CatalogueModelTest {

  @Test
  void loadOrderPutsEveryDatasetAfterTheDatasetsItReferences() {
    CatalogueModel model = EngineFixtures.model();

    assertThat(model.errors()).isEmpty();
    List<String> order = model.loadOrder();
    assertThat(order.indexOf("COMPANY")).isLessThan(order.indexOf("BRANCH"));
    assertThat(order.indexOf("COMPANY")).isLessThan(order.indexOf("ACCOUNT"));
    assertThat(order.indexOf("RULE")).isLessThan(order.indexOf("RULE_LINE"));
    assertThat(model.loadOrder(List.of("RULE_LINE", "BRANCH", "COMPANY")))
        .containsExactly("COMPANY", "BRANCH", "RULE_LINE");
  }

  @Test
  void referencesByIdAreResolvedAndSelfReferencesAreWrittenLater() {
    DatasetModel account = EngineFixtures.model().model("ACCOUNT");

    assertThat(account.references()).containsKeys("company_id", "parent_id");
    assertThat(account.references().get("company_id").dataset()).isEqualTo("COMPANY");
    assertThat(account.dependencies()).containsExactly("COMPANY");
    assertThat(account.deferredColumns()).containsExactly("parent_id");
    assertThat(account.comparedColumns()).doesNotContain("balance", "id");
    assertThat(account.columns()).contains("balance").doesNotContain("id");
  }

  @Test
  void deactivationUsesTheRecordStatusOrTheActiveFlag() {
    CatalogueModel model = EngineFixtures.model();

    assertThat(model.model("BRANCH").deactivation())
        .contains(new Deactivation("record_status", "INACTIVE"));
    assertThat(model.model("ACCOUNT").deactivation())
        .contains(new Deactivation("active", Boolean.FALSE));
    assertThat(model.model("RULE").deactivation()).isEmpty();
  }

  @Test
  void collectionsAreExportedWithTheirParent() {
    CatalogueModel model = EngineFixtures.model();

    assertThat(model.collectionsOf("RULE")).containsExactly("RULE_LINE");
    assertThat(PackageWriter.expand(model, List.of("RULE"))).containsExactly("RULE", "RULE_LINE");
  }

  @Test
  void aReferenceToATableOutsideTheCatalogueIsAnError() {
    Map<String, TableSchema> tables = new java.util.LinkedHashMap<>(EngineFixtures.tables());
    tables.put(
        "rule",
        EngineFixtures.table(
            "rule",
            List.of(new ForeignKey(List.of("journal_id"), "journal", List.of("id"))),
            "id:int8",
            "code:varchar",
            "journal_id:int8"));

    CatalogueModel model = new CatalogueModel(EngineFixtures.catalogue(), tables);

    assertThat(model.errors())
        .anyMatch(e -> e.contains("journal") && e.contains("not configuration"));
  }

  @Test
  void datasetsThatDependOnEachOtherInACycleAreReported() {
    ConfigCatalogue catalogue =
        ConfigCatalogue.read(
            new ByteArrayInputStream(
                """
                groups: [{code: G, name: Group}]
                datasets:
                  - {code: A, name: A, group: G, module: m, table: a, key: [code]}
                  - {code: B, name: B, group: G, module: m, table: b, key: [code]}
                reasons: {}
                excluded: {}
                """
                    .getBytes(StandardCharsets.UTF_8)));
    Map<String, TableSchema> tables =
        Map.of(
            "a",
            EngineFixtures.table(
                "a",
                List.of(new ForeignKey(List.of("b_id"), "b", List.of("id"))),
                "id:int8",
                "code:varchar",
                "b_id:int8?"),
            "b",
            EngineFixtures.table(
                "b",
                List.of(new ForeignKey(List.of("a_id"), "a", List.of("id"))),
                "id:int8",
                "code:varchar",
                "a_id:int8?"));

    CatalogueModel model = new CatalogueModel(catalogue, tables);

    assertThat(model.errors()).anyMatch(e -> e.contains("cycle"));
  }

  @Test
  void aMissingTableOrColumnIsReported() {
    Map<String, TableSchema> tables = new java.util.LinkedHashMap<>(EngineFixtures.tables());
    tables.remove("param");
    tables.put("rule", EngineFixtures.table("rule", List.of(), "id:int8", "name:varchar"));

    CatalogueModel model = new CatalogueModel(EngineFixtures.catalogue(), tables);

    assertThat(model.errors())
        .anyMatch(e -> e.contains("param does not exist"))
        .anyMatch(e -> e.contains("RULE: unknown column code"));
  }
}
