package com.iortatechnxt.brokerverse.common.storage;

import com.iortatechnxt.brokerverse.common.util.Sha256;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * File-system adapter of {@link FileStore} for developer machines, automated tests and seed stacks
 * ({@code brokerverse.storage.provider=local}; refused in production).
 *
 * <p>Objects live under {@code <root>/<bucket-class>/objects/<key>}, their metadata (content type,
 * SHA-256, version, tags, legal hold) under {@code <root>/<bucket-class>/meta/<key>.properties}.
 * Every stored object receives the scan tag of {@code brokerverse.storage.local.scan-status}
 * (default {@code NO_THREATS_FOUND}): the local store marks files clean. Presigned links are
 * application URLs ({@value #CONTENT_PATH}) carrying an HMAC-signed token with the object, the
 * method and the expiry.
 */
@SuppressWarnings(
    "PMD.GodClass") // one adapter: the ten operations of the port plus the local links
public class LocalFileStore implements FileStore {

  /** Path of the endpoint that serves local presigned links. */
  public static final String CONTENT_PATH = "/api/v1/files/local-content";

  private static final String GET = "GET";
  private static final String PUT = "PUT";
  private static final String TAG_PREFIX = "tag.";
  private static final String CONTENT_TYPE = "contentType";
  private static final String SHA256 = "sha256";
  private static final String VERSION = "versionId";
  private static final String LEGAL_HOLD = "legalHold";
  private static final String META_SUFFIX = ".properties";

  private final Path root;
  private final LocalLinkSigner signer;
  private final String scanTag;
  private final String scanStatus;
  private final Clock clock;

  /**
   * Creates the store.
   *
   * @param properties storage settings
   * @param clock clock (link expiry)
   */
  public LocalFileStore(StorageProperties properties, Clock clock) {
    this.root = properties.local().root().toAbsolutePath().normalize();
    this.signer = new LocalLinkSigner(properties.local().linkSecret(), clock);
    this.scanTag = properties.scanTag();
    this.scanStatus = properties.local().scanStatus();
    this.clock = clock;
  }

  @Override
  public String provider() {
    return StorageProperties.LOCAL;
  }

  @Override
  public StoredObject put(ObjectRef ref, byte[] content, String contentType, String sha256) {
    if (String.CASE_INSENSITIVE_ORDER.compare(Sha256.hex(content), sha256) != 0) {
      throw new FileStoreException("The content does not match its SHA-256");
    }
    Optional<Properties> existing = readMeta(ref);
    if (existing.isPresent() && isHeld(existing.get())) {
      throw new FileStoreException("The object is under legal hold");
    }
    Properties meta = new Properties();
    meta.setProperty(CONTENT_TYPE, contentType);
    meta.setProperty(SHA256, sha256.toLowerCase(Locale.ROOT));
    meta.setProperty(TAG_PREFIX + scanTag, scanStatus);
    return write(ref, content, meta);
  }

  @Override
  public byte[] get(ObjectRef ref) {
    Path file = objectPath(ref);
    if (!Files.isRegularFile(file)) {
      throw new FileStoreException("Object not found");
    }
    try {
      return Files.readAllBytes(file);
    } catch (IOException e) {
      throw new FileStoreException("The object could not be read", e);
    }
  }

  @Override
  public PresignedLink presignedGet(
      ObjectRef ref, Duration ttl, String fileName, String contentType) {
    Instant expires = clock.instant().plus(ttl);
    String token = signer.sign(GET, ref, expires, fileName, contentType);
    return new PresignedLink(URI.create(CONTENT_PATH + "?token=" + token), GET, expires, Map.of());
  }

  @Override
  public PresignedLink presignedPut(
      ObjectRef ref, Duration ttl, String contentType, String sha256) {
    Instant expires = clock.instant().plus(ttl);
    String token = signer.sign(PUT, ref, expires, sha256, contentType);
    return new PresignedLink(
        URI.create(CONTENT_PATH + "?token=" + token),
        PUT,
        expires,
        Map.of("Content-Type", contentType));
  }

  @Override
  public void delete(ObjectRef ref) {
    Optional<Properties> meta = readMeta(ref);
    if (meta.isPresent() && isHeld(meta.get())) {
      throw new FileStoreException("The object is under legal hold");
    }
    try {
      Files.deleteIfExists(objectPath(ref));
      Files.deleteIfExists(metaPath(ref));
    } catch (IOException e) {
      throw new FileStoreException("The object could not be deleted", e);
    }
  }

  @Override
  public StoredObject copy(ObjectRef from, ObjectRef to) {
    byte[] content = get(from);
    Properties meta = readMeta(from).orElseGet(Properties::new);
    meta.remove(LEGAL_HOLD);
    return write(to, content, meta);
  }

  @Override
  public boolean exists(ObjectRef ref) {
    return Files.isRegularFile(objectPath(ref));
  }

  @Override
  public Optional<ObjectMetadata> metadata(ObjectRef ref) {
    Path file = objectPath(ref);
    if (!Files.isRegularFile(file)) {
      return Optional.empty();
    }
    Properties meta = readMeta(ref).orElseGet(Properties::new);
    Map<String, String> tags = new HashMap<>();
    for (String name : meta.stringPropertyNames()) {
      if (name.startsWith(TAG_PREFIX)) {
        tags.put(name.substring(TAG_PREFIX.length()), meta.getProperty(name));
      }
    }
    try {
      return Optional.of(
          new ObjectMetadata(
              Files.size(file),
              meta.getProperty(CONTENT_TYPE),
              Files.getLastModifiedTime(file).toInstant(),
              meta.getProperty(VERSION),
              tags,
              isHeld(meta)));
    } catch (IOException e) {
      throw new FileStoreException("The object metadata could not be read", e);
    }
  }

  @Override
  public void list(BucketClass bucket, String prefix, Consumer<ObjectSummary> action) {
    Path objects = bucketPath(bucket).resolve("objects");
    if (!Files.isDirectory(objects)) {
      return;
    }
    try (Stream<Path> files = Files.walk(objects)) {
      files
          .filter(Files::isRegularFile)
          .forEach(
              f -> {
                String key = objects.relativize(f).toString().replace('\\', '/');
                if (key.startsWith(prefix) && ObjectRef.isValidKey(key)) {
                  action.accept(summary(new ObjectRef(bucket, key), f));
                }
              });
    } catch (IOException e) {
      throw new FileStoreException("The bucket could not be listed", e);
    }
  }

  @Override
  public void legalHold(ObjectRef ref, boolean on) {
    Properties meta = readMeta(ref).orElseThrow(() -> new FileStoreException("Object not found"));
    meta.setProperty(LEGAL_HOLD, Boolean.toString(on));
    writeMeta(ref, meta);
  }

  /**
   * Sets the malware scan result of an object, as the scanning service does in S3 (seed data and
   * tests of the quarantine path).
   *
   * @param ref object
   * @param status scan result, e.g. {@code THREATS_FOUND}; null removes the tag (scan pending)
   */
  public void markScanResult(ObjectRef ref, String status) {
    Properties meta = readMeta(ref).orElseThrow(() -> new FileStoreException("Object not found"));
    if (status == null) {
      meta.remove(TAG_PREFIX + scanTag);
    } else {
      meta.setProperty(TAG_PREFIX + scanTag, status);
    }
    writeMeta(ref, meta);
  }

  /**
   * Reads the object a GET link points to, after checking its signature and expiry.
   *
   * @param token token of the link
   * @return content with the file name and type of the link
   * @throws FileStoreException when the token is invalid or expired
   */
  public LinkedContent readLinked(String token) {
    LocalLinkSigner.Signed signed = signer.verify(token, GET);
    return new LinkedContent(get(signed.ref()), signed.extra(), signed.contentType());
  }

  /**
   * Stores the bytes sent to a PUT link, after checking its signature, expiry and the SHA-256.
   *
   * @param token token of the link
   * @param content uploaded bytes
   * @return the stored object
   */
  public StoredObject writeLinked(String token, byte[] content) {
    LocalLinkSigner.Signed signed = signer.verify(token, PUT);
    return put(signed.ref(), content, signed.contentType(), signed.extra());
  }

  private StoredObject write(ObjectRef ref, byte[] content, Properties meta) {
    Path file = objectPath(ref);
    String version = UUID.randomUUID().toString();
    meta.setProperty(VERSION, version);
    try {
      Path folder = parentOf(file);
      Files.createDirectories(folder);
      Path temp = Files.createTempFile(folder, ".upload", ".tmp");
      Files.write(temp, content);
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (IOException e) {
      throw new FileStoreException("The object could not be written", e);
    }
    writeMeta(ref, meta);
    return new StoredObject(ref, version);
  }

  private static Path parentOf(Path file) {
    Path parent = file.getParent();
    if (parent == null) {
      throw new FileStoreException("Invalid object key");
    }
    return parent;
  }

  private static boolean isHeld(Properties meta) {
    return Boolean.parseBoolean(meta.getProperty(LEGAL_HOLD, "false"));
  }

  private Optional<Properties> readMeta(ObjectRef ref) {
    Path file = metaPath(ref);
    if (!Files.isRegularFile(file)) {
      return Optional.empty();
    }
    Properties meta = new Properties();
    try (InputStream in = Files.newInputStream(file)) {
      meta.load(in);
      return Optional.of(meta);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void writeMeta(ObjectRef ref, Properties meta) {
    Path file = metaPath(ref);
    try {
      Files.createDirectories(parentOf(file));
      try (OutputStream out = Files.newOutputStream(file)) {
        meta.store(out, null);
      }
    } catch (IOException e) {
      throw new FileStoreException("The object metadata could not be written", e);
    }
  }

  private static ObjectSummary summary(ObjectRef ref, Path file) {
    try {
      return new ObjectSummary(ref, Files.size(file), Files.getLastModifiedTime(file).toInstant());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private Path bucketPath(BucketClass bucket) {
    return root.resolve(bucket.name().toLowerCase(Locale.ROOT));
  }

  private Path objectPath(ObjectRef ref) {
    return inside(bucketPath(ref.bucket()).resolve("objects").resolve(ref.key()));
  }

  private Path metaPath(ObjectRef ref) {
    return inside(bucketPath(ref.bucket()).resolve("meta").resolve(ref.key() + META_SUFFIX));
  }

  private Path inside(Path path) {
    Path normalized = path.normalize();
    if (!normalized.startsWith(root)) {
      throw new FileStoreException("Invalid object key");
    }
    return normalized;
  }

  /**
   * Content served through a local GET link.
   *
   * @param content bytes
   * @param fileName file name of the link
   * @param contentType content type of the link
   */
  public record LinkedContent(byte[] content, String fileName, String contentType) {}
}
