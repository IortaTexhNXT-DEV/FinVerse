package com.iortatechnxt.brokerverse.docgen.api;

import com.iortatechnxt.brokerverse.docgen.api.dto.DocTemplateRequest;
import com.iortatechnxt.brokerverse.docgen.api.dto.DocTemplateResponse;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Document templates: versions and new versions (Broking Setup). */
@RestController
@RequestMapping("/api/v1/doc-templates")
public class DocTemplateController {

  private final DocTemplateService templates;

  /**
   * Creates the controller.
   *
   * @param templates template service
   */
  public DocTemplateController(DocTemplateService templates) {
    this.templates = templates;
  }

  /**
   * Every version of every template.
   *
   * @return versions
   */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('MASTER_VIEW', 'LOV_MANAGE')")
  public List<DocTemplateResponse> all() {
    return templates.all().stream().map(DocTemplateResponse::from).toList();
  }

  /**
   * Adds a version.
   *
   * @param code template
   * @param request text and effective date
   * @return new version
   */
  @PostMapping("/{code}/versions")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyAuthority('MASTER_MAINTAIN', 'LOV_MANAGE')")
  public DocTemplateResponse newVersion(
      @PathVariable String code, @Valid @RequestBody DocTemplateRequest request) {
    return DocTemplateResponse.from(
        templates.newVersion(code, request.title(), request.body(), request.effectiveFrom()));
  }
}
