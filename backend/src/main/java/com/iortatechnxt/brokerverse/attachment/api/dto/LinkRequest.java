package com.iortatechnxt.brokerverse.attachment.api.dto;

import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Records a file is linked to.
 *
 * @param records records (entity type and id)
 */
public record LinkRequest(@NotEmpty @Size(max = 50) List<@Valid LinkedRecord> records) {

  /**
   * A record.
   *
   * @param entityType entity type, e.g. Account
   * @param entityId entity id
   */
  public record LinkedRecord(@NotBlank String entityType, @NotBlank String entityId) {}

  /**
   * The records as attachment targets.
   *
   * @return targets
   */
  public List<AttachmentTarget> targets() {
    return records.stream().map(r -> new AttachmentTarget(r.entityType(), r.entityId())).toList();
  }
}
