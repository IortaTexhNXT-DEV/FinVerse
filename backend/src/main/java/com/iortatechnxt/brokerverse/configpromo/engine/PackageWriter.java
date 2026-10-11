package com.iortatechnxt.brokerverse.configpromo.engine;

import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.configpromo.catalogue.CatalogueDataset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Exports datasets of a database into a signed package. */
public final class PackageWriter {

  private PackageWriter() {}

  /**
   * Header of a package to write: everything of the manifest but the datasets.
   *
   * @param packageId unique id
   * @param platformVersion application version
   * @param schemaVersion schema version
   * @param environment source environment
   * @param createdBy user
   * @param createdAt time (ISO-8601 UTC)
   * @param mode FULL or INCREMENTAL
   * @param includeUsers whether users are included
   * @param description purpose
   */
  public record Header(
      String packageId,
      String platformVersion,
      String schemaVersion,
      String environment,
      String createdBy,
      String createdAt,
      String mode,
      boolean includeUsers,
      String description) {}

  /**
   * A written package.
   *
   * @param manifest manifest
   * @param content zip bytes
   */
  public record Written(PackageManifest manifest, byte[] content) {}

  /**
   * The datasets to export for a selection: the selected datasets and the collections of their rows
   * (lines, grants), in load order. Collections are always exported with their parent, as an import
   * replaces the collection of each parent it holds.
   *
   * @param model catalogue model
   * @param selected selected dataset codes
   * @return codes in load order
   */
  public static List<String> expand(CatalogueModel model, Collection<String> selected) {
    Set<String> all = new LinkedHashSet<>();
    Deque<String> pending = new ArrayDeque<>(selected);
    while (!pending.isEmpty()) {
      String code = pending.removeFirst();
      if (model.has(code) && all.add(code)) {
        pending.addAll(model.collectionsOf(code));
      }
    }
    return model.loadOrder(all);
  }

  /**
   * Exports datasets.
   *
   * @param reader reader of the source database
   * @param codes datasets (already expanded)
   * @param header manifest header
   * @param signer signer
   * @return package
   */
  public static Written write(
      DatasetReader reader, List<String> codes, Header header, PackageSigner signer) {
    Map<String, byte[]> files = new LinkedHashMap<>();
    List<ManifestDataset> entries = new ArrayList<>();
    for (String code : codes) {
      DatasetModel m = reader.model().model(code);
      List<Map<String, Object>> rows =
          reader.rows(code).stream().map(CanonicalRow::values).toList();
      byte[] bytes = CanonicalJson.bytes(new DatasetFile(code, m.columns(), rows));
      String path = PackageCodec.fileOf(code);
      files.put(path, bytes);
      CatalogueDataset d = m.dataset();
      entries.add(
          new ManifestDataset(
              code,
              d.name(),
              d.group(),
              d.module(),
              rows.size(),
              path,
              Sha256.hex(bytes),
              Checksums.content(rows, m.comparedColumns()),
              m.fingerprint()));
    }
    PackageManifest manifest =
        new PackageManifest(
            PackageManifest.FORMAT,
            PackageManifest.FORMAT_VERSION,
            header.packageId(),
            header.platformVersion(),
            header.schemaVersion(),
            header.environment(),
            header.createdBy(),
            header.createdAt(),
            header.mode(),
            header.includeUsers(),
            header.description(),
            entries);
    return new Written(manifest, PackageCodec.write(manifest, files, signer));
  }
}
