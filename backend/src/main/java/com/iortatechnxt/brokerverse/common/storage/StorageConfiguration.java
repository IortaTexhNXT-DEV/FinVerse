package com.iortatechnxt.brokerverse.common.storage;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

/**
 * Selects the {@link FileStore} adapter by {@code brokerverse.storage.provider}: {@code s3} or
 * {@code local}.
 *
 * <p>Start-up checks: the local store is refused when {@code brokerverse.environment} is {@code
 * production}; the S3 store needs the bucket and the KMS key of the documents, reports and inbound
 * bucket classes (the migration bucket is used by the migration role only).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {

  private static final String PRODUCTION = "production";
  private static final List<BucketClass> REQUIRED =
      List.of(BucketClass.DOCUMENTS, BucketClass.REPORTS, BucketClass.INBOUND);

  /**
   * The file store of the configured provider.
   *
   * @param properties storage settings
   * @param environment {@code brokerverse.environment}
   * @param clock clock
   * @return file store
   */
  @Bean
  public FileStore fileStore(
      StorageProperties properties,
      @Value("${brokerverse.environment:local}") String environment,
      Clock clock) {
    return switch (properties.provider()) {
      case StorageProperties.S3 -> {
        requireS3Settings(properties);
        yield S3FileStore.create(properties, DefaultCredentialsProvider.builder().build());
      }
      case StorageProperties.LOCAL -> {
        if (String.CASE_INSENSITIVE_ORDER.compare(PRODUCTION, environment) == 0) {
          throw new IllegalStateException(
              "brokerverse.storage.provider=local is not allowed in production: set s3");
        }
        yield new LocalFileStore(properties, clock);
      }
      default ->
          throw new IllegalStateException(
              "Unknown brokerverse.storage.provider '" + properties.provider() + "' (s3 or local)");
    };
  }

  /**
   * Refuses an S3 configuration without the bucket and KMS key of a required bucket class.
   *
   * @param properties storage settings
   */
  static void requireS3Settings(StorageProperties properties) {
    List<String> missing = new ArrayList<>();
    for (BucketClass bucket : REQUIRED) {
      String name = bucket.name().toLowerCase(Locale.ROOT);
      if (properties.buckets().of(bucket) == null) {
        missing.add("brokerverse.storage.buckets." + name);
      }
      if (properties.kmsKeys().of(bucket) == null) {
        missing.add("brokerverse.storage.kms-keys." + name);
      }
    }
    if (!missing.isEmpty()) {
      throw new IllegalStateException("S3 storage settings missing: " + String.join(", ", missing));
    }
  }
}
