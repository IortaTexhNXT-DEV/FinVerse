package com.iortatechnxt.brokerverse.configpromo;

import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalJson;
import com.iortatechnxt.brokerverse.configpromo.engine.ConfigPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetFile;
import com.iortatechnxt.brokerverse.configpromo.engine.ManifestDataset;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageCodec;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageManifest;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageSigner;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Writes a package again with changed rows or manifest, signed with the given key: how the tests
 * build packages another environment would have exported.
 */
final class PackageRewriter {

  private PackageRewriter() {}

  /**
   * A package with the rows of one dataset changed.
   *
   * @param pkg verified package
   * @param code dataset
   * @param rows change of the rows
   * @param signer signer
   * @return zip bytes
   */
  static byte[] withRows(
      ConfigPackage pkg,
      String code,
      UnaryOperator<List<Map<String, Object>>> rows,
      PackageSigner signer) {
    return rewrite(pkg, code, rows, UnaryOperator.identity(), signer);
  }

  /**
   * A package with its manifest changed.
   *
   * @param pkg verified package
   * @param manifest change of the manifest
   * @param signer signer
   * @return zip bytes
   */
  static byte[] withManifest(
      ConfigPackage pkg, UnaryOperator<PackageManifest> manifest, PackageSigner signer) {
    return rewrite(pkg, null, UnaryOperator.identity(), manifest, signer);
  }

  private static byte[] rewrite(
      ConfigPackage pkg,
      String code,
      UnaryOperator<List<Map<String, Object>>> rows,
      UnaryOperator<PackageManifest> change,
      PackageSigner signer) {
    Map<String, byte[]> files = new LinkedHashMap<>();
    List<ManifestDataset> datasets = new ArrayList<>();
    for (ManifestDataset d : pkg.manifest().datasets()) {
      DatasetFile file = pkg.files().get(d.code());
      List<Map<String, Object>> content =
          d.code().equals(code) ? rows.apply(new ArrayList<>(file.rows())) : file.rows();
      byte[] bytes = CanonicalJson.bytes(new DatasetFile(d.code(), file.columns(), content));
      files.put(d.file(), bytes);
      datasets.add(
          new ManifestDataset(
              d.code(),
              d.name(),
              d.group(),
              d.module(),
              content.size(),
              d.file(),
              Sha256.hex(bytes),
              d.contentSha256(),
              d.fingerprint()));
    }
    PackageManifest m = pkg.manifest();
    PackageManifest manifest =
        change.apply(
            new PackageManifest(
                m.format(),
                m.formatVersion(),
                m.packageId(),
                m.platformVersion(),
                m.schemaVersion(),
                "SIT",
                m.createdBy(),
                m.createdAt(),
                m.mode(),
                m.includeUsers(),
                m.description(),
                datasets));
    return PackageCodec.write(manifest, files, signer);
  }
}
