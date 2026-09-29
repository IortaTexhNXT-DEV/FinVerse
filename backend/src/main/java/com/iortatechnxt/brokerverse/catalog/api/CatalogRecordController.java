package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.CatalogRecordResponse;
import com.iortatechnxt.brokerverse.catalog.service.CatalogKind;
import com.iortatechnxt.brokerverse.catalog.service.CatalogRecords;
import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Maker-checker actions on any catalog record: authorize and deactivate. */
@RestController
@RequestMapping("/api/v1/catalog/records")
public class CatalogRecordController {

  private final CatalogRecords records;

  /**
   * Creates the controller.
   *
   * @param records catalog records
   */
  public CatalogRecordController(CatalogRecords records) {
    this.records = records;
  }

  /**
   * Authorizes a new or changed record (a user other than its maker).
   *
   * @param kind kind
   * @param id id
   * @return record
   */
  @PostMapping("/{kind}/{id}/authorize")
  @PreAuthorize(CatalogAccess.AUTHORIZE)
  public CatalogRecordResponse authorize(@PathVariable CatalogKind kind, @PathVariable Long id) {
    return CatalogRecordResponse.from(kind, records.authorize(kind, id));
  }

  /**
   * Deactivates a record; the reason is kept in the audit trail of the record.
   *
   * @param kind kind
   * @param id id
   * @param request reason (optional body)
   * @return record
   */
  @PostMapping("/{kind}/{id}/deactivate")
  @PreAuthorize(CatalogAccess.DEACTIVATE)
  public CatalogRecordResponse deactivate(
      @PathVariable CatalogKind kind,
      @PathVariable Long id,
      @RequestBody(required = false) @Valid ReasonRequest request) {
    return CatalogRecordResponse.from(
        kind, records.deactivate(kind, id, request == null ? null : request.reason()));
  }
}
