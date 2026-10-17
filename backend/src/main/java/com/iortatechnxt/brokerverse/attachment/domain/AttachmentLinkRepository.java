package com.iortatechnxt.brokerverse.attachment.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Links of files to further records. */
public interface AttachmentLinkRepository extends JpaRepository<AttachmentLink, Long> {

  /**
   * Links of a record.
   *
   * @param entityType entity type
   * @param entityId entity id
   * @return links
   */
  List<AttachmentLink> findByEntityTypeAndEntityId(String entityType, String entityId);

  /**
   * Links of a file.
   *
   * @param attachmentId file
   * @return links
   */
  List<AttachmentLink> findByAttachmentId(Long attachmentId);

  /**
   * Whether a file is already linked to a record.
   *
   * @param attachmentId file
   * @param entityType entity type
   * @param entityId entity id
   * @return true when linked
   */
  boolean existsByAttachmentIdAndEntityTypeAndEntityId(
      Long attachmentId, String entityType, String entityId);
}
