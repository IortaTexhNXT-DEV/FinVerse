package com.iortatechnxt.brokerverse.configpromo.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Whether a package fits the target: a dataset the target does not know, or whose columns differ,
 * refuses the import; another schema or platform version with the same columns is a warning.
 *
 * @param refusals reasons the package is refused
 * @param warnings warnings
 */
public record Compatibility(List<String> refusals, List<String> warnings) {

  /** Defensive copies. */
  public Compatibility {
    refusals = List.copyOf(refusals);
    warnings = List.copyOf(warnings);
  }

  /**
   * Checks a package against a target.
   *
   * @param manifest manifest of the package
   * @param target catalogue model of the target
   * @param schemaVersion schema version of the target
   * @param platformVersion platform version of the target
   * @return result
   */
  public static Compatibility check(
      PackageManifest manifest, CatalogueModel target, String schemaVersion, String platformVersion) {
    List<String> refusals = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    for (ManifestDataset d : manifest.datasets()) {
      if (!target.has(d.code())) {
        refusals.add(d.name() + " is not a configuration dataset of this platform version");
      } else if (!target.model(d.code()).fingerprint().equals(d.fingerprint())) {
        refusals.add(
            d.name()
                + " has other fields here than in the source environment; promote between"
                + " environments on the same schema version");
      }
    }
    if (!Objects.equals(manifest.schemaVersion(), schemaVersion)) {
      warnings.add(
          "The package comes from schema version "
              + manifest.schemaVersion()
              + "; this environment is on "
              + schemaVersion);
    }
    if (!Objects.equals(manifest.platformVersion(), platformVersion)) {
      warnings.add(
          "The package comes from platform version "
              + manifest.platformVersion()
              + "; this environment runs "
              + platformVersion);
    }
    return new Compatibility(refusals, warnings);
  }

  /**
   * Whether the package may be imported.
   *
   * @return true when nothing refuses it
   */
  public boolean compatible() {
    return refusals.isEmpty();
  }
}
