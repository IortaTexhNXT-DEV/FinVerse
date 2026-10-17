package com.iortatechnxt.brokerverse.common.storage;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Port to the object store that keeps file content (DOCUMENT_STORAGE_DECISION section 3, option C).
 * Modules never hold file bytes in database columns: they store through this port and keep the
 * metadata in {@code stored_file} ({@code storage.service.StoredFileService}).
 *
 * <p>Adapters: {@link S3FileStore} (Amazon S3, SSE-KMS, every shared environment) and {@link
 * LocalFileStore} (file system, developer machines and automated tests), selected by {@code
 * brokerverse.storage.provider}.
 */
public interface FileStore {

  /**
   * Name of the adapter ({@code s3} or {@code local}).
   *
   * @return provider name
   */
  String provider();

  /**
   * Writes an object. The store verifies the SHA-256 on receipt where it can (S3 checksum).
   *
   * @param ref where to write
   * @param content bytes
   * @param contentType content type
   * @param sha256 SHA-256 of the bytes (hex)
   * @return the stored object and its version
   */
  StoredObject put(ObjectRef ref, byte[] content, String contentType, String sha256);

  /**
   * Reads an object.
   *
   * @param ref object
   * @return bytes
   * @throws FileStoreException when the object does not exist
   */
  byte[] get(ObjectRef ref);

  /**
   * A short-lived download link. The response carries {@code Content-Disposition: attachment} with
   * the file name and {@code Cache-Control: no-store}.
   *
   * @param ref object
   * @param ttl validity
   * @param fileName name to save the file under
   * @param contentType content type of the response
   * @return link
   */
  PresignedLink presignedGet(ObjectRef ref, Duration ttl, String fileName, String contentType);

  /**
   * A short-lived upload link (large inbound files). The client must send the returned headers.
   *
   * @param ref where the client uploads
   * @param ttl validity
   * @param contentType content type the client must declare
   * @param sha256 SHA-256 the uploaded bytes must have (hex)
   * @return link
   */
  PresignedLink presignedPut(ObjectRef ref, Duration ttl, String contentType, String sha256);

  /**
   * Deletes an object (in a versioned bucket: the current version becomes non-current and the
   * lifecycle rule expires it).
   *
   * @param ref object
   */
  void delete(ObjectRef ref);

  /**
   * Copies an object with its tags (the copy is encrypted with the key of the target bucket class).
   *
   * @param from source
   * @param to target
   * @return the copy
   */
  StoredObject copy(ObjectRef from, ObjectRef to);

  /**
   * Whether an object exists.
   *
   * @param ref object
   * @return true when present
   */
  boolean exists(ObjectRef ref);

  /**
   * Size, type, version, tags and legal hold of an object.
   *
   * @param ref object
   * @return metadata, empty when the object does not exist
   */
  Optional<ObjectMetadata> metadata(ObjectRef ref);

  /**
   * Lists the objects of a bucket class under a prefix.
   *
   * @param bucket bucket class
   * @param prefix key prefix ({@code ""} for all)
   * @param action called for each object
   */
  void list(BucketClass bucket, String prefix, Consumer<ObjectSummary> action);

  /**
   * Places or releases an Object Lock legal hold (governance of the documents bucket). A held
   * object cannot be deleted until the hold is released.
   *
   * @param ref object
   * @param on true to place, false to release
   */
  void legalHold(ObjectRef ref, boolean on);
}
