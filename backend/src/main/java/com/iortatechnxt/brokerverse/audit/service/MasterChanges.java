package com.iortatechnxt.brokerverse.audit.service;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The field changes of a master record as the master change log keeps them: the business fields
 * whose value changed, with their value before and after. Technical fields (version, creation and
 * change stamps, the authorization stamps) are left out, and so is the record status while a record
 * moves between pending and active (the checker columns of the log say that); a deactivation or a
 * reactivation is kept.
 */
public final class MasterChanges {

  /** Longest value kept; longer values are cut. */
  static final int MAX_VALUE = 2000;

  /** Field of the record status. */
  static final String STATUS_FIELD = "recordStatus";

  /** Fields never logged. */
  static final Set<String> TECHNICAL_FIELDS =
      Set.of(
          "version",
          "createdAt",
          "createdBy",
          "updatedAt",
          "updatedBy",
          "authorizedBy",
          "authorizedAt",
          "makerOnAuthorization");

  private MasterChanges() {}

  /**
   * One changed field.
   *
   * @param field property name
   * @param oldValue value before, null when empty
   * @param newValue value after, null when empty
   */
  public record FieldChange(String field, String oldValue, String newValue) {}

  /**
   * The logged changes between two states of a record.
   *
   * @param names property names
   * @param loggable whether each property is a simple value that can be logged
   * @param oldState values before
   * @param newState values after
   * @return changed fields in property order
   */
  public static List<FieldChange> diff(
      String[] names, boolean[] loggable, Object[] oldState, Object[] newState) {
    List<FieldChange> changes = new ArrayList<>();
    for (int i = 0; i < names.length; i++) {
      String name = names[i];
      if (!loggable[i] || TECHNICAL_FIELDS.contains(name)) {
        continue;
      }
      String before = text(oldState[i]);
      String after = text(newState[i]);
      if (Objects.equals(before, after) || isApprovalFlow(name, before, after)) {
        continue;
      }
      changes.add(new FieldChange(name, before, after));
    }
    return changes;
  }

  /**
   * Whether a change of state authorizes the record: its status turns active from pending.
   *
   * @param names property names
   * @param oldState values before
   * @param newState values after
   * @return true for an authorization
   */
  public static boolean authorizes(String[] names, Object[] oldState, Object[] newState) {
    for (int i = 0; i < names.length; i++) {
      if (STATUS_FIELD.equals(names[i])) {
        return oldState[i] == RecordStatus.PENDING_AUTHORIZATION
            && newState[i] == RecordStatus.ACTIVE;
      }
    }
    return false;
  }

  private static boolean isApprovalFlow(String name, String before, String after) {
    if (!STATUS_FIELD.equals(name)) {
      return false;
    }
    Set<String> flow =
        Set.of(RecordStatus.PENDING_AUTHORIZATION.name(), RecordStatus.ACTIVE.name());
    return (before == null || flow.contains(before)) && flow.contains(after);
  }

  /**
   * The text of a value as the log keeps it.
   *
   * @param value value
   * @return text, null for an empty value
   */
  static String text(Object value) {
    if (value == null) {
      return null;
    }
    String text =
        switch (value) {
          case Enum<?> e -> e.name();
          case Instant i -> i.toString();
          case LocalDate d -> d.toString();
          case BigDecimal b -> b.stripTrailingZeros().toPlainString();
          default -> String.valueOf(value);
        };
    if (text.isEmpty()) {
      return null;
    }
    return text.length() > MAX_VALUE ? text.substring(0, MAX_VALUE) : text;
  }
}
