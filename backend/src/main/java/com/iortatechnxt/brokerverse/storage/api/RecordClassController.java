package com.iortatechnxt.brokerverse.storage.api;

import com.iortatechnxt.brokerverse.storage.api.dto.RecordClassRequest;
import com.iortatechnxt.brokerverse.storage.api.dto.RecordClassResponse;
import com.iortatechnxt.brokerverse.storage.service.RecordClassService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The parameterised record classes of stored files (retention, legal hold, archive to ECM). Changes
 * are limited to the DOA approvers ({@code FILE_LEGAL_HOLD_APPROVE}) because they decide which
 * records are held.
 */
@RestController
@RequestMapping("/api/v1/files/record-classes")
public class RecordClassController {

  private final RecordClassService classes;

  /**
   * Creates the controller.
   *
   * @param classes record classes
   */
  public RecordClassController(RecordClassService classes) {
    this.classes = classes;
  }

  /**
   * All record classes.
   *
   * @return classes
   */
  @GetMapping
  @PreAuthorize(
      "hasAnyAuthority('FILE_LEGAL_HOLD_REQUEST', 'FILE_LEGAL_HOLD_APPROVE', 'AUDIT_VIEW',"
          + " 'SYSTEM_PARAMETER_MANAGE')")
  public List<RecordClassResponse> list() {
    return classes.list().stream().map(RecordClassResponse::from).toList();
  }

  /**
   * Changes a record class; audited.
   *
   * @param code code
   * @param body new settings
   * @return class
   */
  @PutMapping("/{code}")
  @PreAuthorize("hasAuthority('FILE_LEGAL_HOLD_APPROVE')")
  public RecordClassResponse change(
      @PathVariable String code, @Valid @RequestBody RecordClassRequest body) {
    return RecordClassResponse.from(classes.change(code, body.toSettings()));
  }
}
