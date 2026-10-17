package com.iortatechnxt.brokerverse.migration.mapping.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * A version of an extract layout (DATA_MIGRATION_DESIGN section 5.1; FR-DM-010): the columns of one
 * file of a data object, its key and the rule of the hash total of its control file. A file whose
 * header does not match the FROZEN version in force is rejected. The layouts drive the data
 * requirements workbook and the load templates exported from the console.
 */
@Entity
@Table(name = "mig_layout")
public class Layout extends BaseEntity {

  /** Status of a layout version. */
  public enum Status {
    /** Being prepared. */
    DRAFT,
    /** In force (the latest frozen version of the code). */
    FROZEN,
    /** Replaced. */
    RETIRED
  }

  /** How the hash total of the control file is computed. */
  public enum HashRule {
    /** Count of distinct values of the hash columns. */
    DISTINCT_COUNT,
    /** Count of rows. */
    ROW_COUNT,
    /** Sum of the numeric part of the hash column. */
    NUMERIC_SUM
  }

  @Column(nullable = false, length = 10, updatable = false)
  private String code;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Column(name = "object_code", nullable = false, length = 10, updatable = false)
  private String objectCode;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(name = "key_columns", nullable = false, length = 200)
  private String keyColumns;

  @Enumerated(EnumType.STRING)
  @Column(name = "hash_rule", nullable = false, length = 20)
  private HashRule hashRule;

  @Column(name = "hash_columns", length = 200)
  private String hashColumns;

  @Column(name = "amount_columns", length = 300)
  private String amountColumns;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.DRAFT;

  @Column(name = "frozen_by", length = 50)
  private String frozenBy;

  @Column(name = "frozen_at")
  private Instant frozenAt;

  protected Layout() {}

  /**
   * A new DRAFT layout version.
   *
   * @param code layout code
   * @param versionNo version
   * @param objectCode data object
   * @param title title
   * @param keyColumns key columns, comma separated
   */
  public Layout(String code, int versionNo, String objectCode, String title, String keyColumns) {
    this.code = code;
    this.versionNo = versionNo;
    this.objectCode = objectCode;
    this.title = title;
    this.keyColumns = keyColumns;
    this.hashRule = HashRule.ROW_COUNT;
  }

  /**
   * Sets the control-file rules of the layout.
   *
   * @param rule hash rule
   * @param hashCols hash columns, comma separated
   * @param amounts amount columns with control totals, comma separated
   */
  public void controls(HashRule rule, String hashCols, String amounts) {
    requireDraft();
    this.hashRule = rule;
    this.hashColumns = hashCols;
    this.amountColumns = amounts;
  }

  /**
   * Freezes the version; it becomes the version in force.
   *
   * @param user Data Steward
   * @param when time
   */
  public void freeze(String user, Instant when) {
    requireDraft();
    this.status = Status.FROZEN;
    this.frozenBy = user;
    this.frozenAt = when;
  }

  /** A later version was frozen. */
  public void retire() {
    this.status = Status.RETIRED;
  }

  /** Refuses a change unless the version is a draft. */
  public void requireDraft() {
    if (status != Status.DRAFT) {
      throw new BusinessRuleException(
          "MIG_LAYOUT_FROZEN", "Layout " + code + " version " + versionNo + " is frozen");
    }
  }

  public List<String> keys() {
    return split(keyColumns);
  }

  public List<String> hashCols() {
    List<String> cols = split(hashColumns);
    return cols.isEmpty() ? keys() : cols;
  }

  public List<String> amounts() {
    return split(amountColumns);
  }

  private static List<String> split(String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }
    return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
  }

  public String getCode() {
    return code;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public String getObjectCode() {
    return objectCode;
  }

  public String getTitle() {
    return title;
  }

  public String getKeyColumns() {
    return keyColumns;
  }

  public HashRule getHashRule() {
    return hashRule;
  }

  public String getHashColumns() {
    return hashColumns;
  }

  public String getAmountColumns() {
    return amountColumns;
  }

  public Status getStatus() {
    return status;
  }

  public String getFrozenBy() {
    return frozenBy;
  }

  public Instant getFrozenAt() {
    return frozenAt;
  }
}
