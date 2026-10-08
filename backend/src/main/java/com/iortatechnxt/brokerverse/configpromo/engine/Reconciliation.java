package com.iortatechnxt.brokerverse.configpromo.engine;

/**
 * Comparison of one dataset after an import: the rows and checksum of the package against the
 * same items read back from the target.
 *
 * @param code dataset code
 * @param packageRows rows in the package
 * @param targetRows items of the package found in the target
 * @param targetTotal all items of the dataset in the target
 * @param packageSha256 checksum of the package values
 * @param targetSha256 checksum of the same items in the target
 */
public record Reconciliation(
    String code,
    int packageRows,
    int targetRows,
    int targetTotal,
    String packageSha256,
    String targetSha256) {

  /**
   * Whether the target holds exactly the values of the package.
   *
   * @return true when counts and checksums agree
   */
  public boolean matched() {
    return packageRows == targetRows && packageSha256.equals(targetSha256);
  }
}
