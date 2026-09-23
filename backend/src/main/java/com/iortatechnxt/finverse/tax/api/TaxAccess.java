package com.iortatechnxt.finverse.tax.api;

import com.iortatechnxt.finverse.common.api.ContentDispositions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Permission expressions of the tax API and the file response helper.
 *
 * <ul>
 *   <li>Read (worksheets, calendar, returns, certificates, IC schedules): {@code TAX_VIEW}.
 *   <li>Maintain masters, prepare / file / pay returns, issue certificates: {@code TAX_MANAGE}.
 *   <li>Authorize tax masters and IC mappings (checker): {@code MASTER_AUTHORIZE}.
 * </ul>
 */
final class TaxAccess {

  static final String VIEW = "hasAuthority('TAX_VIEW')";
  static final String MANAGE = "hasAuthority('TAX_MANAGE')";
  static final String AUTHORIZE = "hasAuthority('MASTER_AUTHORIZE')";

  private TaxAccess() {}

  /**
   * A file download response.
   *
   * @param fileName file name
   * @param contentType MIME type
   * @param content bytes
   * @return response
   */
  static ResponseEntity<byte[]> file(String fileName, String contentType, byte[] content) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(fileName))
        .body(content);
  }
}
