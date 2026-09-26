package com.iortatechnxt.brokerverse.common.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.util.Sha256;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * The S3 adapter against an in-process S3 REST service ({@link S3MockServer}), through the real AWS
 * SDK v2 client and presigner. A container runtime for an S3-compatible container (MinIO) is not
 * available on the build hosts, so the adapter's requests (SSE-KMS and checksum headers, copy,
 * tagging, legal hold, listing) and the presigned links are verified against this service; the
 * bucket policy, Object Lock and GuardDuty are verified in the DEV account by BDOI IT.
 */
class S3FileStoreTest {

  private static final String DOCS_KEY =
      "arn:aws:kms:ap-southeast-1:111122223333:key/documents-key";
  private static final String INBOUND_KEY =
      "arn:aws:kms:ap-southeast-1:111122223333:key/inbound-key";
  private static final byte[] PDF = "%PDF-1.7 s3".getBytes(StandardCharsets.US_ASCII);

  private static S3MockServer s3;
  private static S3FileStore store;

  @BeforeAll
  static void start() throws Exception {
    s3 = new S3MockServer();
    store =
        S3FileStore.create(
            properties(s3.endpoint().toString()),
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test-key", "test-secret")));
  }

  @AfterAll
  static void stop() {
    s3.close();
  }

  private static StorageProperties properties(String endpoint) {
    return new StorageProperties(
        "s3",
        "ap-southeast-1",
        endpoint == null ? null : java.net.URI.create(endpoint),
        true,
        new StorageProperties.PerBucket(
            "bibs-sit-documents", "bibs-sit-reports", "bibs-sit-inbound", null),
        new StorageProperties.PerBucket(DOCS_KEY, "arn:reports", INBOUND_KEY, null),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static ObjectRef docs(String key) {
    return new ObjectRef(BucketClass.DOCUMENTS, key);
  }

  @Test
  void putIsEncryptedWithTheBucketKeyAndCarriesItsChecksum() {
    ObjectRef ref = docs("bdoi/policy/2026/09/put-1");
    StoredObject stored = store.put(ref, PDF, "application/pdf", Sha256.hex(PDF));
    assertThat(stored.versionId()).isNotBlank();
    assertThat(store.provider()).isEqualTo("s3");
    Map<String, String> headers = s3.lastWriteHeaders();
    assertThat(headers)
        .containsEntry("x-amz-server-side-encryption", "aws:kms")
        .containsEntry("x-amz-server-side-encryption-aws-kms-key-id", DOCS_KEY)
        .containsEntry("x-amz-server-side-encryption-bucket-key-enabled", "true");
    String expected = Base64.getEncoder().encodeToString(HexFormat.of().parseHex(Sha256.hex(PDF)));
    assertThat(headers.values()).contains(expected);
    assertThat(s3.content("bibs-sit-documents/" + ref.key())).isEqualTo(PDF);
    assertThat(store.get(ref)).isEqualTo(PDF);
  }

  @Test
  void metadataTagsLegalHoldCopyListAndDelete() {
    ObjectRef ref = docs("bdoi/claim/2026/09/meta-1");
    store.put(ref, PDF, "application/pdf", Sha256.hex(PDF));
    s3.tag("bibs-sit-documents/" + ref.key(), "GuardDutyMalwareScanStatus", "NO_THREATS_FOUND");
    ObjectMetadata meta = store.metadata(ref).orElseThrow();
    assertThat(meta.size()).isEqualTo(PDF.length);
    assertThat(meta.contentType()).isEqualTo("application/pdf");
    assertThat(meta.tag("GuardDutyMalwareScanStatus")).contains("NO_THREATS_FOUND");
    assertThat(meta.legalHold()).isFalse();
    assertThat(meta.lastModified()).isNotNull();

    store.legalHold(ref, true);
    assertThat(store.metadata(ref).orElseThrow().legalHold()).isTrue();
    store.legalHold(ref, false);
    assertThat(store.metadata(ref).orElseThrow().legalHold()).isFalse();

    ObjectRef copy = ref.withPrefix(ObjectKeys.QUARANTINE_PREFIX);
    assertThat(store.copy(ref, copy).versionId()).isNotBlank();
    assertThat(s3.lastWriteHeaders())
        .containsEntry("x-amz-server-side-encryption-aws-kms-key-id", DOCS_KEY)
        .containsEntry("x-amz-tagging-directive", "COPY");
    assertThat(store.metadata(copy).orElseThrow().tag("GuardDutyMalwareScanStatus"))
        .contains("NO_THREATS_FOUND");

    List<String> keys = new ArrayList<>();
    store.list(BucketClass.DOCUMENTS, "bdoi/claim/", o -> keys.add(o.ref().key()));
    assertThat(keys).contains(ref.key()).doesNotContain(copy.key());

    store.delete(ref);
    assertThat(store.exists(ref)).isFalse();
    assertThat(store.metadata(ref)).isEmpty();
    assertThatThrownBy(() -> store.get(ref))
        .isInstanceOf(FileStoreException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void presignedGetForcesAttachmentNoStoreAndExpiry() {
    ObjectRef ref = docs("bdoi/claim/2026/09/link-1");
    PresignedLink link =
        store.presignedGet(ref, Duration.ofSeconds(300), "Claim letter.pdf", "application/pdf");
    String url = URLDecoder.decode(link.url().toString(), StandardCharsets.UTF_8);
    assertThat(link.method()).isEqualTo("GET");
    assertThat(url)
        .startsWith(s3.endpoint() + "/bibs-sit-documents/" + ref.key())
        .contains("X-Amz-Expires=300")
        .contains("response-cache-control=no-store")
        .contains("response-content-disposition=attachment; filename=\"Claim letter.pdf\"")
        .contains("response-content-type=application/pdf")
        .contains("X-Amz-Signature=");
    assertThat(link.expiresAt()).isAfter(Clock.systemUTC().instant().plusSeconds(290));
  }

  @Test
  void presignedPutSignsEncryptionAndChecksumHeaders() {
    ObjectRef ref = new ObjectRef(BucketClass.INBOUND, "incoming/bdoi/bank-file/2026/09/put-link");
    PresignedLink link =
        store.presignedPut(ref, Duration.ofMinutes(5), "text/csv", Sha256.hex(PDF));
    assertThat(link.method()).isEqualTo("PUT");
    assertThat(link.url().toString()).contains("/bibs-sit-inbound/").contains("X-Amz-Expires=300");
    assertThat(link.headers())
        .containsEntry("x-amz-server-side-encryption", "aws:kms")
        .containsEntry("x-amz-server-side-encryption-aws-kms-key-id", INBOUND_KEY)
        .containsKey("x-amz-checksum-sha256")
        .doesNotContainKey("host");
  }

  @Test
  void unconfiguredBucketsAndSettingsAreRefused() {
    ObjectRef migration = new ObjectRef(BucketClass.MIGRATION, "x/y/2026/09/z");
    assertThatThrownBy(() -> store.exists(migration))
        .isInstanceOf(FileStoreException.class)
        .hasMessageContaining("MIGRATION");
    StorageConfiguration.requireS3Settings(properties(null));
    StorageProperties empty =
        new StorageProperties(
            "s3", null, null, false, null, null, null, null, null, null, null, null, null, null);
    assertThatThrownBy(() -> StorageConfiguration.requireS3Settings(empty))
        .hasMessageContaining("brokerverse.storage.buckets.documents")
        .hasMessageContaining("brokerverse.storage.kms-keys.inbound");
  }

  @Test
  void theLocalStoreIsRefusedInProduction(@org.junit.jupiter.api.io.TempDir Path root) {
    StorageConfiguration configuration = new StorageConfiguration();
    StorageProperties local =
        new StorageProperties(
            "local",
            null,
            null,
            false,
            null,
            null,
            new StorageProperties.Local(root, null, null),
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    assertThatThrownBy(() -> configuration.fileStore(local, "production", Clock.systemUTC()))
        .hasMessageContaining("not allowed in production");
    assertThat(configuration.fileStore(local, "sit", Clock.systemUTC()))
        .isInstanceOf(LocalFileStore.class);
    StorageProperties unknown =
        new StorageProperties(
            "ftp", null, null, false, null, null, null, null, null, null, null, null, null, null);
    assertThatThrownBy(() -> configuration.fileStore(unknown, "sit", Clock.systemUTC()))
        .hasMessageContaining("Unknown");
    assertThat(
            configuration.fileStore(
                properties(s3.endpoint().toString()), "production", Clock.systemUTC()))
        .isInstanceOf(S3FileStore.class);
  }

  @Test
  void defaultsApply() {
    StorageProperties p =
        new StorageProperties(
            null, " ", null, false, null, null, null, null, null, null, null, null, null, null);
    assertThat(p.provider()).isEqualTo("local");
    assertThat(p.region()).isEqualTo("ap-southeast-1");
    assertThat(p.maxUploadSize().toMegabytes()).isEqualTo(25);
    assertThat(p.linkTtl()).isEqualTo(Duration.ofSeconds(300));
    assertThat(p.orphanAge()).isEqualTo(Duration.ofHours(24));
    assertThat(p.scanTag()).isEqualTo("GuardDutyMalwareScanStatus");
    assertThat(p.jobs().orphanCron()).isEqualTo("0 10 2 * * *");
    assertThat(p.local().scanStatus()).isEqualTo("NO_THREATS_FOUND");
    assertThat(p.allowedExtensions()).contains("pdf", "xlsx", "csv");
    assertThat(p.buckets().of(BucketClass.DOCUMENTS)).isNull();
  }
}
