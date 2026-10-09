package com.iortatechnxt.brokerverse.configpromo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalJson;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.ChangeType;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetDiff;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetModel;
import com.iortatechnxt.brokerverse.configpromo.engine.Issue;
import com.iortatechnxt.brokerverse.configpromo.engine.RowChange;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The stored, readable form of a dry run: findings and items with their key and values in business
 * words (field labels instead of column names, the natural key of a referenced record instead of
 * its id, Yes / No for flags).
 */
public final class ImportViews {

  private static final String SEPARATOR = " / ";
  private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

  private ImportViews() {}

  /**
   * A finding.
   *
   * @param dataset dataset code, null for the package
   * @param datasetName dataset name
   * @param key item key in words, null for the dataset
   * @param message message
   */
  public record IssueView(String dataset, String datasetName, String key, String message) {}

  /**
   * Findings of a dry run.
   *
   * @param refusals reasons the package does not fit this environment
   * @param notes compatibility warnings
   * @param blockers findings that prevent the approval
   * @param warnings other findings
   */
  public record Messages(
      List<String> refusals,
      List<String> notes,
      List<IssueView> blockers,
      List<IssueView> warnings) {

    /** Defensive copies. */
    public Messages {
      refusals = refusals == null ? List.of() : List.copyOf(refusals);
      notes = notes == null ? List.of() : List.copyOf(notes);
      blockers = blockers == null ? List.of() : List.copyOf(blockers);
      warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
  }

  /**
   * A field of an item.
   *
   * @param column column
   * @param label label
   * @param from value in this environment
   * @param to value in the package
   */
  public record FieldView(String column, String label, String from, String to) {}

  /**
   * An item of the difference.
   *
   * @param type added, changed or only in this environment
   * @param key natural key in words
   * @param fields changed fields, or every field of an added item or of an item only here
   */
  public record ItemView(ChangeType type, String key, List<FieldView> fields) {

    /** Defensive copy. */
    public ItemView {
      fields = List.copyOf(fields);
    }
  }

  /**
   * The findings in words.
   *
   * @param model catalogue model
   * @param refusals refusals
   * @param notes compatibility notes
   * @param blockers blockers
   * @param warnings warnings
   * @return messages
   */
  public static Messages messages(
      CatalogueModel model,
      List<String> refusals,
      List<String> notes,
      List<Issue> blockers,
      List<Issue> warnings) {
    return new Messages(
        refusals,
        notes,
        blockers.stream().map(i -> view(model, i)).toList(),
        warnings.stream().map(i -> view(model, i)).toList());
  }

  private static IssueView view(CatalogueModel model, Issue issue) {
    String name =
        issue.dataset() == null ? null : model.catalogue().dataset(issue.dataset()).name();
    String key =
        issue.keyText() == null
            ? null
            : keyLabel(CanonicalJson.read(issue.keyText().getBytes(StandardCharsets.UTF_8), MAP));
    return new IssueView(issue.dataset(), name, key, issue.message());
  }

  /**
   * The items of a dataset difference in words.
   *
   * @param m dataset model
   * @param diff difference
   * @return items
   */
  public static List<ItemView> items(DatasetModel m, DatasetDiff diff) {
    List<ItemView> items = new ArrayList<>();
    for (RowChange row : diff.items()) {
      items.add(new ItemView(row.type(), keyLabel(row.key()), fields(m, row)));
    }
    return items;
  }

  private static List<FieldView> fields(DatasetModel m, RowChange row) {
    if (row.type() == ChangeType.CHANGED) {
      return row.fields().stream().map(f -> field(f.column(), f.from(), f.to())).toList();
    }
    List<FieldView> fields = new ArrayList<>();
    for (String column : m.comparedColumns()) {
      Object value = row.values().get(column);
      if (value != null) {
        fields.add(
            row.type() == ChangeType.ADDED
                ? field(column, null, value)
                : field(column, value, null));
      }
    }
    return fields;
  }

  private static FieldView field(String column, Object from, Object to) {
    return new FieldView(column, label(column), text(from), text(to));
  }

  /**
   * The label of a column: {@code default_commission_rate} becomes "Default Commission Rate".
   *
   * @param column column
   * @return label
   */
  public static String label(String column) {
    String label = DisplayFormat.label(column.toUpperCase(Locale.ROOT));
    return label.endsWith(" Id") ? label.substring(0, label.length() - " Id".length()) : label;
  }

  /**
   * A value in words: a referenced record by its natural key, a flag as Yes / No.
   *
   * @param value canonical value
   * @return text, null for no value
   */
  public static String text(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Map<?, ?> map) {
      return keyLabel(map);
    }
    if (value instanceof Boolean b) {
      return b ? "Yes" : "No";
    }
    return value.toString();
  }

  /**
   * A natural key in words: its values in key order, a referenced record by its own key, joined by
   * " / " (company FVI, branch HO: "FVI / HO").
   *
   * @param key natural key
   * @return words
   */
  public static String keyLabel(Map<?, ?> key) {
    List<String> parts = new ArrayList<>();
    for (Object value : key.values()) {
      if (value instanceof Map<?, ?> nested) {
        parts.add(keyLabel(nested));
      } else {
        parts.add(Objects.toString(text(value), "(none)"));
      }
    }
    return String.join(SEPARATOR, parts);
  }
}
