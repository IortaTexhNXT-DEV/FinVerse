package com.iortatechnxt.brokerverse.configpromo.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.iortatechnxt.brokerverse.configpromo.EngineFixtures;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Writing items into the target: references are remapped from the natural key of the package to the
 * id of the same row in the target; audit columns name the maker and the checker.
 */
class RowWriterTest {

  private static final Instant AT = Instant.parse("2026-10-08T01:00:00Z");
  private static final Map<String, Object> COMPANY_FVI = Map.of("code", "FVI");

  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final CatalogueModel model = EngineFixtures.model();

  /** Target ids: company FVI is 42 here, account 1000 of FVI is 7. */
  private final IdResolver ids =
      (dataset, key) -> {
        if ("COMPANY".equals(dataset) && COMPANY_FVI.equals(key)) {
          return Optional.of(42L);
        }
        if ("ACCOUNT".equals(dataset) && ((Map<?, ?>) key).get("code").equals("1000")) {
          return Optional.of(7L);
        }
        return Optional.empty();
      };

  private RowWriter writer(String dataset) {
    return new RowWriter(
        jdbc, ids, model, model.model(dataset), new ApplyActors("admin", "checker", AT));
  }

  private static RowChange added(Map<String, Object> values, Map<String, Object> key) {
    return new RowChange(ChangeType.ADDED, "k", key, values, List.of(), null);
  }

  @Test
  void anInsertRemapsTheReferenceToTheIdOfTheTargetAndRecordsMakerAndChecker() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("company_id", COMPANY_FVI);
    values.put("code", "HO");
    values.put("name", "Head Office");
    values.put("record_status", "ACTIVE");

    writer("BRANCH").insert(added(values, Map.of("company_id", COMPANY_FVI, "code", "HO")));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
    verify(jdbc).update(sql.capture(), params.capture());
    assertThat(sql.getValue())
        .startsWith("insert into \"branch\"")
        .contains("\"company_id\"", "\"authorized_by\"", "\"created_by\"")
        .contains("cast(? as int8)");
    List<Object> p = Arrays.asList(params.getValue());
    assertThat(p).contains("42", "HO", "Head Office", "ACTIVE", "checker", "admin");
  }

  @Test
  void aSelfReferenceIsLeftEmptyOnInsertAndWrittenAfterwards() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("company_id", COMPANY_FVI);
    values.put("code", "1100");
    values.put("parent_id", Map.of("company_id", COMPANY_FVI, "code", "1000"));
    values.put("active", true);
    values.put("balance", "0.00");
    RowChange row = added(values, Map.of("company_id", COMPANY_FVI, "code", "1100"));
    RowWriter writer = writer("ACCOUNT");

    writer.insert(row);
    ArgumentCaptor<Object[]> insert = ArgumentCaptor.forClass(Object[].class);
    verify(jdbc).update(anyString(), insert.capture());
    assertThat(Arrays.asList(insert.getValue())).doesNotContain("7");

    writer.writeDeferred(new RowChange(ChangeType.ADDED, "k", row.key(), values, List.of(), 99L));
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
    verify(jdbc, org.mockito.Mockito.times(2)).update(sql.capture(), params.capture());
    assertThat(sql.getAllValues().get(1)).startsWith("update \"account\" set \"parent_id\"");
    assertThat(Arrays.asList(params.getAllValues().get(1))).containsExactly("7", 99L);
  }

  @Test
  void anUpdateKeepsTheEnvironmentColumnsAndMatchesTheTargetId() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("company_id", COMPANY_FVI);
    values.put("code", "1000");
    values.put("parent_id", null);
    values.put("active", false);
    values.put("balance", "999.00");

    writer("ACCOUNT")
        .update(
            new RowChange(
                ChangeType.CHANGED,
                "k",
                Map.of("company_id", COMPANY_FVI, "code", "1000"),
                values,
                List.of(new FieldChange("active", true, false)),
                7L));

    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
    verify(jdbc).update(sql.capture(), params.capture());
    assertThat(sql.getValue()).doesNotContain("balance").endsWith("where \"id\" = ?");
    assertThat(Arrays.asList(params.getValue()))
        .contains(Boolean.FALSE, 7L)
        .doesNotContain("999.00");
  }

  @Test
  void aReferenceThatResolvesNowhereStopsTheApply() {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("company_id", Map.of("code", "OTHER"));
    values.put("code", "HO");

    assertThatThrownBy(
            () ->
                writer("BRANCH")
                    .insert(added(values, Map.of("company_id", Map.of("code", "OTHER")))))
        .isInstanceOf(ApplyException.class)
        .hasMessageContaining("exists neither in the package nor in this environment");
  }

  @Test
  void anItemOfAReplacedCollectionIsRemovedWithItsTargetId() {
    writer("RULE_LINE")
        .delete(
            new RowChange(
                ChangeType.ONLY_IN_TARGET,
                "k",
                Map.of("rule_id", Map.of("code", "R1"), "line_no", "3"),
                Map.of(),
                List.of(),
                55L));

    verify(jdbc).update("delete from \"rule_line\" where \"id\" = ?", 55L);
  }
}
