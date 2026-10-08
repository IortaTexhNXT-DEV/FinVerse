package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A configuration package read and verified: manifest, data files and the key that signed it.
 *
 * @param manifest manifest
 * @param files data files by dataset code
 * @param keyId identifier of the signing key
 */
public record ConfigPackage(
    PackageManifest manifest, Map<String, DatasetFile> files, String keyId) {

  /** Unmodifiable copy keeping the manifest order. */
  public ConfigPackage {
    files = Collections.unmodifiableMap(new LinkedHashMap<>(files));
  }

  /**
   * The rows of a dataset of the package.
   *
   * @param code dataset code
   * @return rows, empty when the dataset is not in the package
   */
  public List<Map<String, Object>> rows(String code) {
    DatasetFile file = files.get(code);
    return file == null ? List.of() : file.rows();
  }

  /**
   * Whether the package holds a dataset.
   *
   * @param code dataset code
   * @return true when present
   */
  public boolean has(String code) {
    return files.containsKey(code);
  }
}
