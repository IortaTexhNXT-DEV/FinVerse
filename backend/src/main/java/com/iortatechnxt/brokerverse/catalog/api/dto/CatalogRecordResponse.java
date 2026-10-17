package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.CatalogRecord;
import com.iortatechnxt.brokerverse.catalog.service.CatalogKind;
import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;

/**
 * Outcome of a maker-checker action on a catalog record.
 *
 * @param kind kind
 * @param id id
 * @param reference business reference
 * @param description description
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 * @param authorizedBy checker
 */
public record CatalogRecordResponse(
    CatalogKind kind,
    Long id,
    String reference,
    String description,
    RecordStatus recordStatus,
    String maker,
    String authorizedBy) {

  /**
   * Maps a record.
   *
   * @param kind kind
   * @param e record
   * @return response
   */
  public static CatalogRecordResponse from(CatalogKind kind, AuthorizableEntity e) {
    String reference = String.valueOf(e.getId());
    String description = null;
    if (e instanceof CatalogRecord r) {
      reference = r.catalogReference();
      description = r.catalogDescription();
    }
    return new CatalogRecordResponse(
        kind,
        e.getId(),
        reference,
        description,
        e.getRecordStatus(),
        e.getMaker(),
        e.getAuthorizedBy());
  }
}
