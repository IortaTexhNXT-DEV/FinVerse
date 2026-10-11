package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.List;

/**
 * The self-describing manifest of a configuration package.
 *
 * @param format format name
 * @param formatVersion format version
 * @param packageId unique id of the package
 * @param platformVersion version of the application that wrote it
 * @param schemaVersion database schema version (highest migration) of the source
 * @param sourceEnvironment environment that wrote it
 * @param createdBy user who exported it
 * @param createdAt time of the export (ISO-8601 UTC)
 * @param mode FULL or INCREMENTAL
 * @param includeUsers whether users are included
 * @param description purpose given by the user
 * @param datasets datasets with row counts and checksums
 */
public record PackageManifest(
    String format,
    int formatVersion,
    String packageId,
    String platformVersion,
    String schemaVersion,
    String sourceEnvironment,
    String createdBy,
    String createdAt,
    String mode,
    boolean includeUsers,
    String description,
    List<ManifestDataset> datasets) {

  /** Format name written in every manifest. */
  public static final String FORMAT = "BIBS configuration package";

  /** Format version this platform writes and reads. */
  public static final int FORMAT_VERSION = 1;

  /** Mode: the package mirrors the source (items only in the target are listed). */
  public static final String FULL = "FULL";

  /** Mode: the package adds and updates only. */
  public static final String INCREMENTAL = "INCREMENTAL";

  /** Defensive copy. */
  public PackageManifest {
    datasets = datasets == null ? List.of() : List.copyOf(datasets);
  }

  /**
   * Total number of rows.
   *
   * @return rows
   */
  public int totalRows() {
    return datasets.stream().mapToInt(ManifestDataset::rows).sum();
  }

  /**
   * A dataset of the package.
   *
   * @param code dataset code
   * @return dataset, null when absent
   */
  public ManifestDataset dataset(String code) {
    return datasets.stream().filter(d -> d.code().equals(code)).findFirst().orElse(null);
  }
}
