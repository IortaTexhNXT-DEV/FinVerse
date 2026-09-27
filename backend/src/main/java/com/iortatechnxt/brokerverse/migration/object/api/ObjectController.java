package com.iortatechnxt.brokerverse.migration.object.api;

import com.iortatechnxt.brokerverse.common.api.ReasonRequest;
import com.iortatechnxt.brokerverse.migration.mapping.service.TemplateExport;
import com.iortatechnxt.brokerverse.migration.object.api.dto.ObjectDtos.CommentRequest;
import com.iortatechnxt.brokerverse.migration.object.api.dto.ObjectDtos.DecisionResponse;
import com.iortatechnxt.brokerverse.migration.object.api.dto.ObjectDtos.ObjectRequest;
import com.iortatechnxt.brokerverse.migration.object.api.dto.ObjectDtos.ObjectResponse;
import com.iortatechnxt.brokerverse.migration.object.api.dto.ObjectDtos.SubmitRequest;
import com.iortatechnxt.brokerverse.migration.object.service.DecisionService;
import com.iortatechnxt.brokerverse.migration.object.service.ObjectRegisterService;
import com.iortatechnxt.brokerverse.storage.api.FileDownloads;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The data object register and its decisions (FR-DM-001, FR-DM-002; screen Data Objects). */
@RestController
@RequestMapping("/api/v1/migration")
public class ObjectController {

  private static final String VIEW = "hasAuthority('MIG_VIEW')";
  private static final String MANAGE = "hasAuthority('MIG_OBJECT_MANAGE')";
  private static final String APPROVE = "hasAuthority('MIG_DECISION_APPROVE')";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final ObjectRegisterService register;
  private final DecisionService decisions;
  private final TemplateExport templates;
  private final FileDownloads downloads;

  /**
   * Creates the controller.
   *
   * @param register register
   * @param decisions decisions
   * @param templates load templates
   * @param downloads file answers
   */
  public ObjectController(
      ObjectRegisterService register,
      DecisionService decisions,
      TemplateExport templates,
      FileDownloads downloads) {
    this.register = register;
    this.decisions = decisions;
    this.templates = templates;
    this.downloads = downloads;
  }

  /**
   * The register.
   *
   * @return objects in load order
   */
  @GetMapping("/objects")
  @PreAuthorize(VIEW)
  public List<ObjectResponse> list() {
    return register.list().stream().map(ObjectResponse::from).toList();
  }

  /**
   * An object.
   *
   * @param code object
   * @return object
   */
  @GetMapping("/objects/{code}")
  @PreAuthorize(VIEW)
  public ObjectResponse get(@PathVariable String code) {
    return ObjectResponse.from(register.get(code));
  }

  /**
   * Adds an object.
   *
   * @param request object
   * @return object
   */
  @PostMapping("/objects")
  @PreAuthorize(MANAGE)
  public ObjectResponse create(@Valid @RequestBody ObjectRequest request) {
    return ObjectResponse.from(register.create(request.code(), request.toData()));
  }

  /**
   * Changes an object.
   *
   * @param code object
   * @param request object
   * @return object
   */
  @PutMapping("/objects/{code}")
  @PreAuthorize(MANAGE)
  public ObjectResponse update(
      @PathVariable String code, @Valid @RequestBody ObjectRequest request) {
    return ObjectResponse.from(register.update(code, request.toData()));
  }

  /**
   * Submits the proposed class to the business owner.
   *
   * @param code object
   * @param companyId company
   * @param request submission
   * @return decision
   */
  @PostMapping("/objects/{code}/decision")
  @PreAuthorize(MANAGE)
  public DecisionResponse submit(
      @PathVariable String code, @RequestParam Long companyId, @RequestBody SubmitRequest request) {
    return DecisionResponse.from(decisions.submit(companyId, code, request.conditionMet()));
  }

  /**
   * The decisions of an object.
   *
   * @param code object
   * @return decisions, newest first
   */
  @GetMapping("/objects/{code}/decisions")
  @PreAuthorize(VIEW)
  public List<DecisionResponse> history(@PathVariable String code) {
    return decisions.history(code).stream().map(DecisionResponse::from).toList();
  }

  /**
   * Decisions waiting for a business owner.
   *
   * @return decisions
   */
  @GetMapping("/decisions/pending")
  @PreAuthorize(VIEW)
  public List<DecisionResponse> pending() {
    return decisions.pending().stream().map(DecisionResponse::from).toList();
  }

  /**
   * Approves a decision (G1).
   *
   * @param decisionNo decision
   * @param request comment
   * @return decision
   */
  @PostMapping("/decisions/{decisionNo}/approve")
  @PreAuthorize(APPROVE)
  public DecisionResponse approve(
      @PathVariable String decisionNo, @RequestBody CommentRequest request) {
    return DecisionResponse.from(decisions.approve(decisionNo, request.comment()));
  }

  /**
   * Returns a decision.
   *
   * @param decisionNo decision
   * @param request reason
   * @return decision
   */
  @PostMapping("/decisions/{decisionNo}/return")
  @PreAuthorize(APPROVE)
  public DecisionResponse returnDecision(
      @PathVariable String decisionNo, @Valid @RequestBody ReasonRequest request) {
    return DecisionResponse.from(decisions.returnDecision(decisionNo, request.reason()));
  }

  /**
   * The load template workbook of an object (its layouts in force).
   *
   * @param code object
   * @param request HTTP request
   * @return XLSX
   */
  @GetMapping("/objects/{code}/template")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> template(@PathVariable String code, HttpServletRequest request) {
    register.get(code);
    return downloads.respond(
        FileDownload.inline(code + "_load_template.xlsx", XLSX, templates.objectWorkbook(code)),
        request);
  }
}
