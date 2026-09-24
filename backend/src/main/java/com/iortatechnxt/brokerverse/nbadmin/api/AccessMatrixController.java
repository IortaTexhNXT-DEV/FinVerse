package com.iortatechnxt.brokerverse.nbadmin.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.nbadmin.api.dto.AccessMatrixResponse;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrixService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The agreed User Access Matrix (BRD 3.3.4): read-only, exportable. */
@RestController
@RequestMapping("/api/v1/nbadmin/access-matrix")
@PreAuthorize("hasAnyAuthority('ACCESS_REQUEST', 'ACCESS_APPROVE', 'ROLE_MANAGE', 'AUDIT_VIEW')")
public class AccessMatrixController {

  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final AccessMatrixService matrix;

  /**
   * Creates the controller.
   *
   * @param matrix access matrix
   */
  public AccessMatrixController(AccessMatrixService matrix) {
    this.matrix = matrix;
  }

  /**
   * Roles against permissions.
   *
   * @return matrix
   */
  @GetMapping
  public AccessMatrixResponse matrix() {
    return AccessMatrixResponse.from(matrix.matrix());
  }

  /**
   * The matrix as an Excel file.
   *
   * @return XLSX file
   */
  @GetMapping("/export")
  public ResponseEntity<byte[]> export() {
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDispositions.attachment("user_access_matrix.xlsx"))
        .body(matrix.exportXlsx());
  }
}
