package com.iortatechnxt.brokerverse.common.storage;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHold;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus;
import software.amazon.awssdk.services.s3.model.PutObjectLegalHoldRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.Tag;
import software.amazon.awssdk.services.s3.model.TaggingDirective;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Amazon S3 adapter of {@link FileStore} (AWS SDK v2), used in DEV, SIT, UAT, pre-production,
 * production and DR.
 *
 * <ul>
 *   <li>Every write is encrypted with SSE-KMS under the customer-managed key of its bucket class
 *       ({@code brokerverse.storage.kms-keys.*}, BDOI-owned keys); the bucket policy denies
 *       unencrypted puts and non-TLS access.
 *   <li>Every put carries its SHA-256 checksum, which S3 verifies on receipt.
 *   <li>Download links force {@code Content-Disposition: attachment} and {@code Cache-Control:
 *       no-store}.
 *   <li>Credentials come from the default chain: on EKS the IAM role of the service account.
 * </ul>
 */
public class S3FileStore implements FileStore {

  private static final int NOT_FOUND = 404;
  private static final String NO_STORE = "no-store";
  private static final String HOST = "host";

  private final S3Client s3;
  private final S3Presigner presigner;
  private final StorageProperties properties;

  /**
   * Creates the adapter on existing clients.
   *
   * @param s3 S3 client
   * @param presigner presigner
   * @param properties storage settings (bucket names and KMS keys)
   */
  public S3FileStore(S3Client s3, S3Presigner presigner, StorageProperties properties) {
    this.s3 = s3;
    this.presigner = presigner;
    this.properties = properties;
  }

  /**
   * Creates the adapter with clients for the configured region and optional endpoint.
   *
   * @param properties storage settings
   * @param credentials credentials provider
   * @return adapter
   */
  public static S3FileStore create(
      StorageProperties properties, AwsCredentialsProvider credentials) {
    Region region = Region.of(properties.region());
    S3ClientBuilder client =
        S3Client.builder()
            .region(region)
            .credentialsProvider(credentials)
            .httpClient(UrlConnectionHttpClient.create())
            .forcePathStyle(properties.pathStyleAccess());
    S3Presigner.Builder presigner =
        S3Presigner.builder()
            .region(region)
            .credentialsProvider(credentials)
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(properties.pathStyleAccess())
                    .build());
    if (properties.endpoint() != null) {
      client.endpointOverride(properties.endpoint());
      presigner.endpointOverride(properties.endpoint());
    }
    return new S3FileStore(client.build(), presigner.build(), properties);
  }

  @Override
  public String provider() {
    return StorageProperties.S3;
  }

  @Override
  public StoredObject put(ObjectRef ref, byte[] content, String contentType, String sha256) {
    PutObjectRequest.Builder request =
        PutObjectRequest.builder()
            .bucket(bucket(ref.bucket()))
            .key(ref.key())
            .contentType(contentType)
            .contentLength((long) content.length)
            .checksumSHA256(base64Sha256(sha256));
    encrypt(ref.bucket(), request);
    try {
      String version = s3.putObject(request.build(), RequestBody.fromBytes(content)).versionId();
      return new StoredObject(ref, version);
    } catch (S3Exception e) {
      throw new FileStoreException("The object could not be written", e);
    }
  }

  @Override
  public byte[] get(ObjectRef ref) {
    try {
      return s3.getObjectAsBytes(
              GetObjectRequest.builder().bucket(bucket(ref.bucket())).key(ref.key()).build())
          .asByteArray();
    } catch (NoSuchKeyException e) {
      throw new FileStoreException("Object not found", e);
    } catch (S3Exception e) {
      throw new FileStoreException("The object could not be read", e);
    }
  }

  @Override
  public PresignedLink presignedGet(
      ObjectRef ref, Duration ttl, String fileName, String contentType) {
    GetObjectRequest get =
        GetObjectRequest.builder()
            .bucket(bucket(ref.bucket()))
            .key(ref.key())
            .responseContentDisposition(ContentDispositions.attachment(fileName))
            .responseCacheControl(NO_STORE)
            .responseContentType(contentType)
            .build();
    PresignedGetObjectRequest signed =
        presigner.presignGetObject(
            GetObjectPresignRequest.builder().signatureDuration(ttl).getObjectRequest(get).build());
    return link(signed.url(), "GET", signed.expiration(), Map.of());
  }

  @Override
  public PresignedLink presignedPut(
      ObjectRef ref, Duration ttl, String contentType, String sha256) {
    PutObjectRequest.Builder put =
        PutObjectRequest.builder()
            .bucket(bucket(ref.bucket()))
            .key(ref.key())
            .contentType(contentType)
            .checksumSHA256(base64Sha256(sha256));
    encrypt(ref.bucket(), put);
    PresignedPutObjectRequest signed =
        presigner.presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(put.build())
                .build());
    return link(signed.url(), "PUT", signed.expiration(), headers(signed.signedHeaders()));
  }

  @Override
  public void delete(ObjectRef ref) {
    try {
      s3.deleteObject(b -> b.bucket(bucket(ref.bucket())).key(ref.key()));
    } catch (S3Exception e) {
      throw new FileStoreException("The object could not be deleted", e);
    }
  }

  @Override
  public StoredObject copy(ObjectRef from, ObjectRef to) {
    CopyObjectRequest.Builder request =
        CopyObjectRequest.builder()
            .sourceBucket(bucket(from.bucket()))
            .sourceKey(from.key())
            .destinationBucket(bucket(to.bucket()))
            .destinationKey(to.key())
            .taggingDirective(TaggingDirective.COPY);
    String kms = properties.kmsKeys().of(to.bucket());
    if (kms != null) {
      request
          .serverSideEncryption(ServerSideEncryption.AWS_KMS)
          .ssekmsKeyId(kms)
          .bucketKeyEnabled(true);
    }
    try {
      return new StoredObject(to, s3.copyObject(request.build()).versionId());
    } catch (S3Exception e) {
      throw new FileStoreException("The object could not be copied", e);
    }
  }

  @Override
  public boolean exists(ObjectRef ref) {
    return head(ref).isPresent();
  }

  @Override
  public Optional<ObjectMetadata> metadata(ObjectRef ref) {
    return head(ref)
        .map(
            head ->
                new ObjectMetadata(
                    head.contentLength() == null ? 0 : head.contentLength(),
                    head.contentType(),
                    head.lastModified(),
                    head.versionId(),
                    tags(ref),
                    head.objectLockLegalHoldStatus() == ObjectLockLegalHoldStatus.ON));
  }

  @Override
  public void list(BucketClass bucket, String prefix, Consumer<ObjectSummary> action) {
    s3.listObjectsV2Paginator(b -> b.bucket(bucket(bucket)).prefix(prefix))
        .contents()
        .forEach(
            o -> {
              if (ObjectRef.isValidKey(o.key())) {
                action.accept(
                    new ObjectSummary(new ObjectRef(bucket, o.key()), o.size(), o.lastModified()));
              }
            });
  }

  @Override
  public void legalHold(ObjectRef ref, boolean on) {
    try {
      s3.putObjectLegalHold(
          PutObjectLegalHoldRequest.builder()
              .bucket(bucket(ref.bucket()))
              .key(ref.key())
              .legalHold(
                  ObjectLockLegalHold.builder()
                      .status(on ? ObjectLockLegalHoldStatus.ON : ObjectLockLegalHoldStatus.OFF)
                      .build())
              .build());
    } catch (S3Exception e) {
      throw new FileStoreException("The legal hold could not be changed", e);
    }
  }

  private Optional<HeadObjectResponse> head(ObjectRef ref) {
    try {
      return Optional.of(
          s3.headObject(
              HeadObjectRequest.builder().bucket(bucket(ref.bucket())).key(ref.key()).build()));
    } catch (NoSuchKeyException e) {
      return Optional.empty();
    } catch (S3Exception e) {
      if (e.statusCode() == NOT_FOUND) {
        return Optional.empty();
      }
      throw new FileStoreException("The object metadata could not be read", e);
    }
  }

  private Map<String, String> tags(ObjectRef ref) {
    List<Tag> tags =
        s3.getObjectTagging(
                GetObjectTaggingRequest.builder()
                    .bucket(bucket(ref.bucket()))
                    .key(ref.key())
                    .build())
            .tagSet();
    return tags.stream().collect(Collectors.toMap(Tag::key, Tag::value, (a, b) -> b));
  }

  private void encrypt(BucketClass bucket, PutObjectRequest.Builder request) {
    String kms = properties.kmsKeys().of(bucket);
    if (kms != null) {
      request
          .serverSideEncryption(ServerSideEncryption.AWS_KMS)
          .ssekmsKeyId(kms)
          .bucketKeyEnabled(true);
    }
  }

  private String bucket(BucketClass bucket) {
    String name = properties.buckets().of(bucket);
    if (name == null) {
      throw new FileStoreException("No bucket is configured for " + bucket);
    }
    return name;
  }

  private static String base64Sha256(String hex) {
    return Base64.getEncoder().encodeToString(HexFormat.of().parseHex(hex));
  }

  private static Map<String, String> headers(Map<String, List<String>> signed) {
    Map<String, String> headers = new LinkedHashMap<>();
    signed.forEach(
        (name, values) -> {
          if (String.CASE_INSENSITIVE_ORDER.compare(HOST, name) != 0) {
            headers.put(name, String.join(",", values));
          }
        });
    return headers;
  }

  private static PresignedLink link(
      URL url, String method, Instant expires, Map<String, String> headers) {
    try {
      return new PresignedLink(url.toURI(), method, expires, headers);
    } catch (URISyntaxException e) {
      throw new FileStoreException("The presigned link is not a valid URI", e);
    }
  }
}
