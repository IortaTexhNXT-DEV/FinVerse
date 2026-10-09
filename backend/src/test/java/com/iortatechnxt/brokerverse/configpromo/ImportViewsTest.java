package com.iortatechnxt.brokerverse.configpromo;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.configpromo.engine.ChangeType;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetDiff;
import com.iortatechnxt.brokerverse.configpromo.engine.FieldChange;
import com.iortatechnxt.brokerverse.configpromo.engine.RowChange;
import com.iortatechnxt.brokerverse.configpromo.service.ImportViews;
import com.iortatechnxt.brokerverse.configpromo.service.ImportViews.ItemView;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The difference in business words: labels, keys of referenced records, Yes / No. */
class ImportViewsTest {

  @Test
  void keysAndReferencesReadAsTheirCodes() {
    Map<String, Object> key = new LinkedHashMap<>();
    key.put("company_id", Map.of("code", "FVI"));
    key.put("code", "HO");

    assertThat(ImportViews.keyLabel(key)).isEqualTo("FVI / HO");
    assertThat(ImportViews.text(true)).isEqualTo("Yes");
    assertThat(ImportViews.text(null)).isNull();
    assertThat(ImportViews.label("default_commission_rate")).isEqualTo("Default Commission Rate");
    assertThat(ImportViews.label("company_id")).isEqualTo("Company");
  }

  @Test
  void addedItemsShowEveryFieldAndChangedItemsTheChangedFields() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("company_id", Map.of("code", "FVI"));
    values.put("code", "HO");
    values.put("name", "Head Office");
    values.put("record_status", "ACTIVE");
    Map<String, Object> key = Map.of("code", "HO");
    DatasetDiff diff =
        new DatasetDiff(
            "BRANCH",
            List.of(new RowChange(ChangeType.ADDED, "k1", key, values, List.of(), null)),
            List.of(
                new RowChange(
                    ChangeType.CHANGED,
                    "k2",
                    key,
                    values,
                    List.of(new FieldChange("name", "Main", "Head Office")),
                    3L)),
            0,
            List.of());

    List<ItemView> items = ImportViews.items(EngineFixtures.model().model("BRANCH"), diff);

    assertThat(items.get(0).fields())
        .extracting(ImportViews.FieldView::label)
        .contains("Company", "Code", "Name", "Record Status");
    assertThat(items.get(0).fields().get(0).to()).isEqualTo("FVI");
    assertThat(items.get(1).fields())
        .singleElement()
        .satisfies(
            f -> {
              assertThat(f.from()).isEqualTo("Main");
              assertThat(f.to()).isEqualTo("Head Office");
            });
  }
}
