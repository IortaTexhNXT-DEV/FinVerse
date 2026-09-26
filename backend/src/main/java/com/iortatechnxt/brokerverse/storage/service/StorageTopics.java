package com.iortatechnxt.brokerverse.storage.service;

import com.iortatechnxt.brokerverse.events.service.IntegrationTopic;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Integration topic of the document storage: final records of a class marked "archive to ECM"
 * (DOCUMENT_STORAGE_DECISION, decision 5). The ECM adapter consumes it once BDOI IT has specified
 * the ECM interface (question DSQ02) and records the ECM reference through {@code
 * StoredFileService.recordEcmReference}.
 */
@Configuration(proxyBeanMethods = false)
public class StorageTopics {

  /** A final record is to be archived in the ECM. */
  public static final String ECM_ARCHIVE_REQUESTED = "bibs.storage.ecm-archive-requested.v1";

  /** Event type of {@link #ECM_ARCHIVE_REQUESTED}. */
  public static final String TYPE_ECM_ARCHIVE_REQUESTED = "storage.record.ecm-archive-requested";

  /**
   * ECM archive requests.
   *
   * @return topic
   */
  @Bean
  public IntegrationTopic ecmArchiveRequestedTopic() {
    return new IntegrationTopic(
        ECM_ARCHIVE_REQUESTED,
        List.of(TYPE_ECM_ARCHIVE_REQUESTED),
        "Final records to archive in the ECM (record class, owner, object, SHA-256); key stored file");
  }

  /**
   * Payload of {@link #TYPE_ECM_ARCHIVE_REQUESTED}: identifiers and object facts only (the file
   * name may carry personal data and is read by the adapter from the metadata row).
   *
   * @param storedFileId stored file id
   * @param recordClass record class
   * @param documentType document type
   * @param ownerEntityType owner entity type
   * @param ownerEntityId owner key
   * @param contentType content type
   * @param sizeBytes size
   * @param sha256 SHA-256
   * @param bucket bucket
   * @param objectKey object key
   * @param versionId object version
   * @param finalAt time the record became final
   */
  public record EcmArchiveRequested(
      Long storedFileId,
      String recordClass,
      String documentType,
      String ownerEntityType,
      String ownerEntityId,
      String contentType,
      long sizeBytes,
      String sha256,
      String bucket,
      String objectKey,
      String versionId,
      Instant finalAt) {}
}
