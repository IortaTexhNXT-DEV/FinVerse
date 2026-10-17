package com.iortatechnxt.brokerverse.configpromo.api.dto;

import com.iortatechnxt.brokerverse.configpromo.catalogue.CatalogueDataset;
import com.iortatechnxt.brokerverse.configpromo.domain.ImportDataset;

/**
 * One dataset of an import: dry run, apply and reconciliation.
 *
 * @param code dataset
 * @param name name
 * @param group group
 * @param seq load order
 * @param added added
 * @param changed changed
 * @param unchanged unchanged
 * @param onlyInTarget only in this environment
 * @param blockers blockers
 * @param collection whether its items only here are removed with the collection of their parent
 * @param inserted added by the apply
 * @param updated updated by the apply
 * @param deactivated deactivated by the apply
 * @param removed removed from collections by the apply
 * @param packageRows rows of the package
 * @param targetRows same items in this environment after the apply
 * @param targetTotal all items in this environment after the apply
 * @param packageSha256 checksum of the package values
 * @param targetSha256 checksum of the same items here
 * @param reconciled whether counts and checksums agree
 */
public record ImportDatasetResponse(
    String code,
    String name,
    String group,
    int seq,
    int added,
    int changed,
    int unchanged,
    int onlyInTarget,
    int blockers,
    boolean collection,
    Integer inserted,
    Integer updated,
    Integer deactivated,
    Integer removed,
    Integer packageRows,
    Integer targetRows,
    Integer targetTotal,
    String packageSha256,
    String targetSha256,
    Boolean reconciled) {

  /**
   * Maps a line.
   *
   * @param l line
   * @param d catalogue dataset
   * @return response
   */
  public static ImportDatasetResponse from(ImportDataset l, CatalogueDataset d) {
    return new ImportDatasetResponse(
        l.getDatasetCode(),
        d == null ? l.getDatasetCode() : d.name(),
        d == null ? null : d.group(),
        l.getSeq(),
        l.getAdded(),
        l.getChanged(),
        l.getUnchanged(),
        l.getOnlyInTarget(),
        l.getBlockers(),
        d != null && d.collection(),
        l.getInserted(),
        l.getUpdated(),
        l.getDeactivated(),
        l.getRemoved(),
        l.getPackageRows(),
        l.getTargetRows(),
        l.getTargetTotal(),
        l.getPackageSha256(),
        l.getTargetSha256(),
        l.getReconciled());
  }
}
