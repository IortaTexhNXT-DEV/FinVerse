package com.iortatechnxt.brokerverse.attachment.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Attachment}. */
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

  /**
   * Live attachments of a record, oldest first.
   *
   * @param entityType entity type
   * @param entityId entity id
   * @return attachments
   */
  List<Attachment> findByEntityTypeAndEntityIdAndDeletedFalseOrderByCreatedAtAsc(
      String entityType, String entityId);

  /**
   * Live attachments among some ids (files linked to a record).
   *
   * @param ids ids
   * @return attachments
   */
  List<Attachment> findByIdInAndDeletedFalseOrderByCreatedAtAsc(Collection<Long> ids);
}
