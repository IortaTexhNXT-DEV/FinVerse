package com.iortatechnxt.brokerverse.configpromo.engine;

import com.iortatechnxt.brokerverse.common.util.Sha256;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Writes and reads the zip form of a package: {@code manifest.json}, one {@code
 * datasets/<CODE>.json} per dataset and {@code signature.json}. Reading verifies the signature, the
 * checksum and row count of every data file, and refuses anything else in the archive.
 */
public final class PackageCodec {

  /** Manifest entry. */
  public static final String MANIFEST = "manifest.json";

  /** Signature entry. */
  public static final String SIGNATURE = "signature.json";

  /** Folder of the data files. */
  public static final String DATASETS = "datasets/";

  private static final int BUFFER = 8192;
  private static final int MAX_ENTRIES = 1000;

  private PackageCodec() {}

  /**
   * The path of a dataset's file.
   *
   * @param code dataset code
   * @return path in the archive
   */
  public static String fileOf(String code) {
    return DATASETS + code + ".json";
  }

  /**
   * Writes a package.
   *
   * @param manifest manifest (with the checksums of the files)
   * @param files data file bytes by path
   * @param signer signer
   * @return zip bytes
   */
  public static byte[] write(
      PackageManifest manifest, Map<String, byte[]> files, PackageSigner signer) {
    byte[] manifestBytes = CanonicalJson.bytes(manifest);
    PackageSignature signature =
        new PackageSignature(PackageSigner.ALGORITHM, signer.keyId(), signer.sign(manifestBytes));
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      entry(zip, MANIFEST, manifestBytes);
      for (Map.Entry<String, byte[]> f : files.entrySet()) {
        entry(zip, f.getKey(), f.getValue());
      }
      entry(zip, SIGNATURE, CanonicalJson.bytes(signature));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return out.toByteArray();
  }

  private static void entry(ZipOutputStream zip, String name, byte[] content) throws IOException {
    ZipEntry e = new ZipEntry(name);
    e.setTime(0L);
    zip.putNextEntry(e);
    zip.write(content);
    zip.closeEntry();
  }

  /**
   * Reads and verifies a package.
   *
   * @param content zip bytes
   * @param signer signer of this platform
   * @param maxBytes largest total size of the unpacked files
   * @return the verified package
   */
  public static ConfigPackage read(byte[] content, PackageSigner signer, long maxBytes) {
    Map<String, byte[]> entries = unzip(content, maxBytes);
    byte[] manifestBytes = require(entries, MANIFEST);
    PackageSignature signature =
        CanonicalJson.read(require(entries, SIGNATURE), PackageSignature.class);
    check(
        PackageSigner.ALGORITHM.equals(signature.algorithm())
            && signer.verify(manifestBytes, signature.value()),
        "The package is not signed with the signing key of this platform or was changed after"
            + " its export; it is refused");
    PackageManifest manifest = CanonicalJson.read(manifestBytes, PackageManifest.class);
    check(
        PackageManifest.FORMAT.equals(manifest.format())
            && manifest.formatVersion() <= PackageManifest.FORMAT_VERSION,
        "The package format " + manifest.formatVersion() + " is not supported by this platform");
    Map<String, DatasetFile> files = new LinkedHashMap<>();
    for (ManifestDataset d : manifest.datasets()) {
      files.put(d.code(), dataset(entries, d));
    }
    check(
        entries.size() == manifest.datasets().size() + 2,
        "The package holds files that its manifest does not list");
    return new ConfigPackage(manifest, files, signature.keyId());
  }

  private static DatasetFile dataset(Map<String, byte[]> entries, ManifestDataset d) {
    byte[] bytes = require(entries, d.file());
    check(
        Sha256.hex(bytes).equals(d.sha256()),
        "The data of " + d.name() + " does not match its checksum");
    DatasetFile file = CanonicalJson.read(bytes, DatasetFile.class);
    check(
        d.code().equals(file.dataset()) && file.rows().size() == d.rows(),
        "The data of " + d.name() + " does not match the manifest");
    return file;
  }

  private static void check(boolean valid, String problem) {
    if (!valid) {
      throw new PackageException(problem);
    }
  }

  private static byte[] require(Map<String, byte[]> entries, String name) {
    byte[] bytes = entries.get(name);
    if (bytes == null) {
      throw new PackageException(
          "The package has no " + name + "; it is not a configuration package");
    }
    return bytes;
  }

  private static Map<String, byte[]> unzip(byte[] content, long maxBytes) {
    Map<String, byte[]> entries = new HashMap<>();
    long total = 0;
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
      for (ZipEntry e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
        if (!e.isDirectory()) {
          check(
              entries.size() < MAX_ENTRIES && !entries.containsKey(e.getName()),
              "The package has too many or repeated files");
          byte[] bytes = readLimited(zip, maxBytes - total);
          total += bytes.length;
          entries.put(e.getName(), bytes);
        }
      }
    } catch (IOException ex) {
      throw new PackageException("The file is not a readable configuration package", ex);
    }
    if (entries.isEmpty()) {
      throw new PackageException("The file is not a configuration package (zip)");
    }
    return entries;
  }

  private static byte[] readLimited(InputStream in, long remaining) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[BUFFER];
    long read = 0;
    for (int n = in.read(buffer); n > 0; n = in.read(buffer)) {
      read += n;
      check(read <= remaining, "The package is larger than the allowed size");
      out.write(buffer, 0, n);
    }
    return out.toByteArray();
  }
}
