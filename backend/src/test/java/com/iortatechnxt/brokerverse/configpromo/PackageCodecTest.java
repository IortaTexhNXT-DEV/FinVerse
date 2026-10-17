package com.iortatechnxt.brokerverse.configpromo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.util.Sha256;
import com.iortatechnxt.brokerverse.configpromo.engine.CanonicalJson;
import com.iortatechnxt.brokerverse.configpromo.engine.ConfigPackage;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetFile;
import com.iortatechnxt.brokerverse.configpromo.engine.ManifestDataset;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageCodec;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageException;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageManifest;
import com.iortatechnxt.brokerverse.configpromo.engine.PackageSigner;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

/** The signed zip form of a package: round trip, and refusal of anything changed or foreign. */
class PackageCodecTest {

  private static final PackageSigner SIGNER =
      new PackageSigner("unit-test-key-0123456789abcdef-01");
  private static final long MAX = 10_000_000L;

  private static byte[] pkg(PackageSigner signer) {
    DatasetFile file =
        new DatasetFile(
            "COMPANY", List.of("code", "name"), List.of(Map.of("code", "FVI", "name", "Head")));
    byte[] bytes = CanonicalJson.bytes(file);
    PackageManifest manifest =
        new PackageManifest(
            PackageManifest.FORMAT,
            PackageManifest.FORMAT_VERSION,
            "11111111-2222-3333-4444-555555555555",
            "1.0",
            "2502",
            "SIT",
            "admin",
            "2026-10-08T00:00:00Z",
            PackageManifest.FULL,
            false,
            "test",
            List.of(
                new ManifestDataset(
                    "COMPANY",
                    "Companies",
                    "G",
                    "org",
                    1,
                    PackageCodec.fileOf("COMPANY"),
                    Sha256.hex(bytes),
                    "x",
                    "code:varchar,name:varchar")));
    return PackageCodec.write(manifest, Map.of(PackageCodec.fileOf("COMPANY"), bytes), signer);
  }

  @Test
  void aPackageReadsBackWithItsManifestAndRows() {
    ConfigPackage read = PackageCodec.read(pkg(SIGNER), SIGNER, MAX);

    assertThat(read.manifest().sourceEnvironment()).isEqualTo("SIT");
    assertThat(read.manifest().totalRows()).isEqualTo(1);
    assertThat(read.rows("COMPANY"))
        .singleElement()
        .satisfies(r -> assertThat(r).containsEntry("code", "FVI"));
    assertThat(read.keyId()).isEqualTo(SIGNER.keyId());
    assertThat(read.has("BRANCH")).isFalse();
  }

  @Test
  void theSameContentGivesTheSameBytes() {
    assertThat(pkg(SIGNER)).isEqualTo(pkg(SIGNER));
  }

  @Test
  void aPackageSignedWithAnotherKeyIsRefused() {
    byte[] foreign = pkg(new PackageSigner("another-key-of-another-platform-xyz"));

    assertThatThrownBy(() -> PackageCodec.read(foreign, SIGNER, MAX))
        .isInstanceOf(PackageException.class)
        .hasMessageContaining("not signed with the signing key");
  }

  @Test
  void aChangedDataFileIsRefused() throws IOException {
    byte[] tampered =
        replace(
            pkg(SIGNER),
            PackageCodec.fileOf("COMPANY"),
            "{\"dataset\":\"COMPANY\",\"columns\":[],\"rows\":[{}]}");

    assertThatThrownBy(() -> PackageCodec.read(tampered, SIGNER, MAX))
        .isInstanceOf(PackageException.class)
        .hasMessageContaining("does not match its checksum");
  }

  @Test
  void aChangedManifestIsRefused() throws IOException {
    byte[] original = pkg(SIGNER);
    String manifest =
        new String(
            entries(original).get(PackageCodec.MANIFEST), java.nio.charset.StandardCharsets.UTF_8);
    byte[] tampered = replace(original, PackageCodec.MANIFEST, manifest.replace("SIT", "PROD"));

    assertThatThrownBy(() -> PackageCodec.read(tampered, SIGNER, MAX))
        .hasMessageContaining("not signed with the signing key");
  }

  @Test
  void anExtraFileIsRefused() throws IOException {
    Map<String, byte[]> entries = entries(pkg(SIGNER));
    entries.put("datasets/EXTRA.json", "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    assertThatThrownBy(() -> PackageCodec.read(zip(entries), SIGNER, MAX))
        .hasMessageContaining("does not list");
  }

  @Test
  void aPackageLargerThanAllowedOrNotAZipIsRefused() {
    assertThatThrownBy(() -> PackageCodec.read(pkg(SIGNER), SIGNER, 10))
        .hasMessageContaining("larger than the allowed size");
    assertThatThrownBy(
            () ->
                PackageCodec.read(
                    "not a zip".getBytes(java.nio.charset.StandardCharsets.UTF_8), SIGNER, MAX))
        .isInstanceOf(PackageException.class);
  }

  @Test
  void aShortSigningKeyIsRefused() {
    assertThatThrownBy(() -> new PackageSigner("short"))
        .isInstanceOf(PackageException.class)
        .hasMessageContaining("signing key");
  }

  private static Map<String, byte[]> entries(byte[] zip) throws IOException {
    Map<String, byte[]> entries = new LinkedHashMap<>();
    try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
      ZipEntry e;
      while ((e = in.getNextEntry()) != null) {
        entries.put(e.getName(), in.readAllBytes());
      }
    }
    return entries;
  }

  private static byte[] zip(Map<String, byte[]> entries) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      for (Map.Entry<String, byte[]> e : entries.entrySet()) {
        zip.putNextEntry(new ZipEntry(e.getKey()));
        zip.write(e.getValue());
        zip.closeEntry();
      }
    }
    return out.toByteArray();
  }

  private static byte[] replace(byte[] zip, String name, String content) throws IOException {
    Map<String, byte[]> entries = entries(zip);
    entries.put(name, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return zip(entries);
  }
}
