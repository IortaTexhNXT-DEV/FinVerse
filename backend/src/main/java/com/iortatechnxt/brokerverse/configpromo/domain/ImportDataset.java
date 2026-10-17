package com.iortatechnxt.brokerverse.configpromo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The difference and the reconciliation of one dataset of an import; the items are kept as JSON for
 * the difference viewer.
 */
@Entity
@Table(name = "cfp_import_dataset")
public class ImportDataset {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "import_id", nullable = false)
  private Long importId;

  @Column(name = "dataset_code", nullable = false, length = 60)
  private String datasetCode;

  @Column(name = "seq", nullable = false)
  private int seq;

  @Column(name = "added", nullable = false)
  private int added;

  @Column(name = "changed", nullable = false)
  private int changed;

  @Column(name = "unchanged", nullable = false)
  private int unchanged;

  @Column(name = "only_in_target", nullable = false)
  private int onlyInTarget;

  @Column(name = "blockers", nullable = false)
  private int blockers;

  @Column(name = "inserted")
  private Integer inserted;

  @Column(name = "updated")
  private Integer updated;

  @Column(name = "deactivated")
  private Integer deactivated;

  @Column(name = "removed")
  private Integer removed;

  @Column(name = "package_rows")
  private Integer packageRows;

  @Column(name = "target_rows")
  private Integer targetRows;

  @Column(name = "target_total")
  private Integer targetTotal;

  @Column(name = "package_sha256", length = 64)
  private String packageSha256;

  @Column(name = "target_sha256", length = 64)
  private String targetSha256;

  @Column(name = "reconciled")
  private Boolean reconciled;

  @Column(name = "details", nullable = false, columnDefinition = "text")
  private String details;

  protected ImportDataset() {}

  /**
   * Creates the line of a dataset.
   *
   * @param importId import
   * @param datasetCode dataset
   * @param seq load order
   * @param summary counts of the dry run
   * @param details items JSON
   */
  public ImportDataset(
      Long importId, String datasetCode, int seq, DryRunCounts summary, String details) {
    this.importId = importId;
    this.datasetCode = datasetCode;
    this.seq = seq;
    this.added = summary.added();
    this.changed = summary.changed();
    this.unchanged = summary.unchanged();
    this.onlyInTarget = summary.onlyInTarget();
    this.blockers = summary.blockers();
    this.details = details;
  }

  /**
   * Counts of a dataset in a dry run.
   *
   * @param added added
   * @param changed changed
   * @param unchanged unchanged
   * @param onlyInTarget only in this environment
   * @param blockers blockers
   */
  public record DryRunCounts(
      int added, int changed, int unchanged, int onlyInTarget, int blockers) {}

  /**
   * Records what the apply did.
   *
   * @param inserted added
   * @param updated updated
   * @param deactivated deactivated
   * @param removed removed from collections
   */
  public void applied(int inserted, int updated, int deactivated, int removed) {
    this.inserted = inserted;
    this.updated = updated;
    this.deactivated = deactivated;
    this.removed = removed;
  }

  /**
   * Records the reconciliation.
   *
   * @param packageRows rows of the package
   * @param targetRows same items in this environment
   * @param targetTotal all items in this environment
   * @param packageSha256 checksum of the package
   * @param targetSha256 checksum of this environment
   */
  public void reconciled(
      int packageRows, int targetRows, int targetTotal, String packageSha256, String targetSha256) {
    this.packageRows = packageRows;
    this.targetRows = targetRows;
    this.targetTotal = targetTotal;
    this.packageSha256 = packageSha256;
    this.targetSha256 = targetSha256;
    this.reconciled = packageRows == targetRows && packageSha256.equals(targetSha256);
  }

  public Long getId() {
    return id;
  }

  public Long getImportId() {
    return importId;
  }

  public String getDatasetCode() {
    return datasetCode;
  }

  public int getSeq() {
    return seq;
  }

  public int getAdded() {
    return added;
  }

  public int getChanged() {
    return changed;
  }

  public int getUnchanged() {
    return unchanged;
  }

  public int getOnlyInTarget() {
    return onlyInTarget;
  }

  public int getBlockers() {
    return blockers;
  }

  public Integer getInserted() {
    return inserted;
  }

  public Integer getUpdated() {
    return updated;
  }

  public Integer getDeactivated() {
    return deactivated;
  }

  public Integer getRemoved() {
    return removed;
  }

  public Integer getPackageRows() {
    return packageRows;
  }

  public Integer getTargetRows() {
    return targetRows;
  }

  public Integer getTargetTotal() {
    return targetTotal;
  }

  public String getPackageSha256() {
    return packageSha256;
  }

  public String getTargetSha256() {
    return targetSha256;
  }

  public Boolean getReconciled() {
    return reconciled;
  }

  public String getDetails() {
    return details;
  }
}
