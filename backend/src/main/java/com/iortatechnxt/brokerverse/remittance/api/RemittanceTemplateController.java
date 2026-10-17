package com.iortatechnxt.brokerverse.remittance.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceTemplates;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The guided Excel templates of the remittance uploads (insurer ORs, holds, specials). */
@RestController
@RequestMapping("/api/v1/remittance/templates")
public class RemittanceTemplateController {

  private static final MediaType XLSX =
      MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  private final RemittanceTemplates templates;

  /**
   * Creates the controller.
   *
   * @param templates templates
   */
  public RemittanceTemplateController(RemittanceTemplates templates) {
    this.templates = templates;
  }

  /**
   * The insurer OR schedule template.
   *
   * @return xlsx
   */
  @GetMapping("/insurer-or")
  @PreAuthorize(RemittanceAccess.OR_UPLOAD)
  public ResponseEntity<byte[]> insurerOr() {
    return xlsx("insurer-or-template.xlsx", templates.insurerOr());
  }

  /**
   * The hold file template.
   *
   * @return xlsx
   */
  @GetMapping("/holds")
  @PreAuthorize(RemittanceAccess.HOLD_REQUEST)
  public ResponseEntity<byte[]> holds() {
    return xlsx("hold-template.xlsx", templates.holds());
  }

  /**
   * The special remittance file template.
   *
   * @return xlsx
   */
  @GetMapping("/special")
  @PreAuthorize(RemittanceAccess.SPECIAL_REQUEST)
  public ResponseEntity<byte[]> specials() {
    return xlsx("special-remittance-template.xlsx", templates.specials());
  }

  private static ResponseEntity<byte[]> xlsx(String name, byte[] body) {
    return ResponseEntity.ok()
        .contentType(XLSX)
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(name))
        .body(body);
  }
}
