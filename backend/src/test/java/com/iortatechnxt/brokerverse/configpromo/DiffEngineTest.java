package com.iortatechnxt.brokerverse.configpromo;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalJson;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalRow;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetDiff;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetModel;
import com.iortatechnxt.brokerverse.configpromo.engine.DiffEngine;
import com.iortatechnxt.brokerverse.configpromo.engine.FieldChange;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The difference of a dataset between a package and the target, by natural key. */
class DiffEngineTest {

  private static final Map<String, Object> FVI = Map.of("code", "FVI");
  private final CatalogueModel model = EngineFixtures.model();

  private static Map<String, Object> account(String code, boolean active, String balance) {
    Map<String, Object> v = new LinkedHashMap<>();
    v.put("company_id", FVI);
    v.put("code", code);
    v.put("parent_id", null);
    v.put("active", active);
    v.put("balance", balance);
    return v;
  }

  private CanonicalRow target(long id, Map<String, Object> values, String dataset) {
    return CanonicalRow.of(id, values, model.model(dataset));
  }

  @Test
  void itemsAreAddedChangedUnchangedOrOnlyInTheTarget() {
    DatasetModel m = model.model("ACCOUNT");
    List<Map<String, Object>> pkg =
        List.of(
            account("1000", true, "0"), account("2000", false, "0"), account("3000", true, "0"));
    List<CanonicalRow> target =
        List.of(
            target(1, account("1000", true, "0"), "ACCOUNT"),
            target(2, account("2000", true, "0"), "ACCOUNT"),
            target(4, account("4000", true, "0"), "ACCOUNT"));

    DatasetDiff full = DiffEngine.diff(m, pkg, target, true, Set.of());

    assertThat(full.unchanged()).isEqualTo(1);
    assertThat(full.changed())
        .singleElement()
        .satisfies(
            c -> {
              assertThat(c.targetId()).isEqualTo(2L);
              assertThat(c.fields()).containsExactly(new FieldChange("active", true, false));
            });
    assertThat(full.added())
        .singleElement()
        .satisfies(a -> assertThat(a.key()).containsEntry("code", "3000"));
    assertThat(full.onlyInTarget())
        .singleElement()
        .satisfies(o -> assertThat(o.targetId()).isEqualTo(4L));
    assertThat(DiffEngine.diff(m, pkg, target, false, Set.of()).onlyInTarget()).isEmpty();
  }

  @Test
  void environmentColumnsAreNotCompared() {
    DatasetModel m = model.model("ACCOUNT");

    DatasetDiff diff =
        DiffEngine.diff(
            m,
            List.of(account("1000", true, "5.00")),
            List.of(target(1, account("1000", true, "999.00"), "ACCOUNT")),
            true,
            Set.of());

    assertThat(diff.noChange()).isTrue();
    assertThat(diff.unchanged()).isEqualTo(1);
  }

  @Test
  void environmentRowsAreNeitherImportedNorListed() {
    DatasetModel m = model.model("PARAM");
    Map<String, Object> mail = Map.of("param_key", "MAIL_FROM", "param_value", "x@y.z");
    Map<String, Object> days = Map.of("param_key", "DAYS", "param_value", "30");

    DatasetDiff diff = DiffEngine.diff(m, List.of(mail, days), List.of(), true, Set.of());

    assertThat(diff.added())
        .singleElement()
        .satisfies(a -> assertThat(a.key()).containsEntry("param_key", "DAYS"));
  }

  @Test
  void aCollectionListsOnlyTheItemsOfTheParentsInThePackage() {
    DatasetModel m = model.model("RULE_LINE");
    Map<String, Object> r1 = Map.of("code", "R1");
    Map<String, Object> r2 = Map.of("code", "R2");
    List<CanonicalRow> target =
        List.of(
            target(1, Map.of("rule_id", r1, "line_no", "1", "account_code", "1000"), "RULE_LINE"),
            target(2, Map.of("rule_id", r1, "line_no", "2", "account_code", "2000"), "RULE_LINE"),
            target(3, Map.of("rule_id", r2, "line_no", "1", "account_code", "3000"), "RULE_LINE"));

    DatasetDiff diff =
        DiffEngine.diff(
            m,
            List.of(Map.of("rule_id", r1, "line_no", "1", "account_code", "1000")),
            target,
            false,
            Set.of(CanonicalJson.text(r1)));

    assertThat(diff.onlyInTarget())
        .singleElement()
        .satisfies(o -> assertThat(o.targetId()).isEqualTo(2L));
  }
}
