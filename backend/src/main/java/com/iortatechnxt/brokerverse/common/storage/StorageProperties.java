package com.iortatechnxt.brokerverse.common.storage;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Document storage settings ({@code brokerverse.storage.*}, docs/operations/CONFIGURATION.md).
 *
 * @param provider {@code s3} or {@code local} (default {@code local}; refused in production)
 * @param region AWS region of the buckets (BDOI: ap-southeast-1)
 * @param endpoint S3-compatible endpoint (only for tests against a local S3 service); empty for AWS
 * @param pathStyleAccess path-style addressing (S3-compatible services)
 * @param buckets bucket name per bucket class
 * @param kmsKeys ARN of the customer-managed KMS key per bucket class (SSE-KMS)
 * @param local settings of the local file-system store
 * @param maxUploadSize largest file accepted through the application (default 25 MB)
 * @param allowedExtensions file types accepted for upload (extension, confirmed by the signature)
 * @param linkTtl validity of presigned links when the parameter FILE_LINK_TTL_SECONDS is missing
 * @param orphanAge age after which an object without a metadata row is deleted (default 24 hours)
 * @param deletedGrace time between the soft delete of a file and the removal of its object
 * @param scanTag object tag that carries the malware scan result
 * @param jobs schedules of the storage jobs (Spring cron, UTC; {@code -} = manual only)
 */
@ConfigurationProperties(prefix = "brokerverse.storage")
public record StorageProperties(
    String provider,
    String region,
    URI endpoint,
    boolean pathStyleAccess,
    PerBucket buckets,
    PerBucket kmsKeys,
    Local local,
    DataSize maxUploadSize,
    List<String> allowedExtensions,
    Duration linkTtl,
    Duration orphanAge,
    Duration deletedGrace,
    String scanTag,
    Jobs jobs) {

  /** Provider name of {@link S3FileStore}. */
  public static final String S3 = "s3";

  /** Provider name of {@link LocalFileStore}. */
  public static final String LOCAL = "local";

  private static final long DEFAULT_MAX_MB = 25;
  private static final long DEFAULT_LINK_SECONDS = 300;
  private static final long DEFAULT_ORPHAN_HOURS = 24;
  private static final long DEFAULT_GRACE_DAYS = 30;
  private static final PerBucket NONE = new PerBucket(null, null, null, null);
  private static final List<String> DEFAULT_EXTENSIONS =
      List.of(
          "pdf", "png", "jpg", "jpeg", "xlsx", "xls", "docx", "doc", "csv", "txt", "zip", "ods",
          "odt", "msg", "eml");

  /** Applies the defaults. */
  public StorageProperties {
    provider = text(provider, LOCAL).toLowerCase(Locale.ROOT);
    region = text(region, "ap-southeast-1");
    endpoint = endpoint == null || endpoint.toString().isBlank() ? null : endpoint;
    buckets = orElse(buckets, NONE);
    kmsKeys = orElse(kmsKeys, NONE);
    local = orElse(local, new Local(null, null, null));
    maxUploadSize = orElse(maxUploadSize, DataSize.ofMegabytes(DEFAULT_MAX_MB));
    allowedExtensions =
        allowedExtensions == null || allowedExtensions.isEmpty()
            ? DEFAULT_EXTENSIONS
            : List.copyOf(allowedExtensions);
    linkTtl = orElse(linkTtl, Duration.ofSeconds(DEFAULT_LINK_SECONDS));
    orphanAge = orElse(orphanAge, Duration.ofHours(DEFAULT_ORPHAN_HOURS));
    deletedGrace = orElse(deletedGrace, Duration.ofDays(DEFAULT_GRACE_DAYS));
    scanTag = text(scanTag, "GuardDutyMalwareScanStatus");
    jobs = orElse(jobs, new Jobs(null, null, null, null));
  }

  private static <T> T orElse(T value, T fallback) {
    return value == null ? fallback : value;
  }

  private static String text(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  /**
   * One value per bucket class.
   *
   * @param documents documents bucket
   * @param reports reports bucket
   * @param inbound inbound bucket
   * @param migration migration bucket
   */
  public record PerBucket(String documents, String reports, String inbound, String migration) {

    /**
     * The value of a bucket class.
     *
     * @param bucket bucket class
     * @return value, null when not configured
     */
    public String of(BucketClass bucket) {
      String value =
          switch (bucket) {
            case DOCUMENTS -> documents;
            case REPORTS -> reports;
            case INBOUND -> inbound;
            case MIGRATION -> migration;
          };
      return value == null || value.isBlank() ? null : value;
    }
  }

  /**
   * Local file-system store.
   *
   * @param root folder of the objects (default {@code <tmp>/brokerverse-files})
   * @param linkSecret key that signs local links (random per start when empty)
   * @param scanStatus scan result tag given to every stored object (default NO_THREATS_FOUND: the
   *     local store marks files clean)
   */
  public record Local(Path root, String linkSecret, String scanStatus) {

    /** Applies the defaults. */
    public Local {
      root =
          root == null ? Path.of(System.getProperty("java.io.tmpdir"), "brokerverse-files") : root;
      scanStatus = text(scanStatus, "NO_THREATS_FOUND");
    }
  }

  /**
   * Schedules of the storage jobs.
   *
   * @param scanResultsCron {@code FILE_SCAN_RESULTS} (default every 5 minutes)
   * @param orphanCron {@code FILE_ORPHAN_RECONCILIATION} (default daily 02:10 UTC)
   * @param retentionCron {@code FILE_RETENTION} (default daily 02:40 UTC)
   * @param ecmArchiveCron {@code FILE_ECM_ARCHIVE} (default every 15 minutes)
   */
  public record Jobs(
      String scanResultsCron, String orphanCron, String retentionCron, String ecmArchiveCron) {

    /** Applies the defaults. */
    public Jobs {
      scanResultsCron = text(scanResultsCron, "0 */5 * * * *");
      orphanCron = text(orphanCron, "0 10 2 * * *");
      retentionCron = text(retentionCron, "0 40 2 * * *");
      ecmArchiveCron = text(ecmArchiveCron, "0 */15 * * * *");
    }
  }
}
