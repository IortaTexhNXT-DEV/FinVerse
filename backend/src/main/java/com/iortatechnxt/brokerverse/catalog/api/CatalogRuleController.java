package com.iortatechnxt.brokerverse.catalog.api;

import com.iortatechnxt.brokerverse.catalog.api.dto.DocumentRuleRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.DocumentRuleResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.FieldRuleRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.FieldRuleResponse;
import com.iortatechnxt.brokerverse.catalog.api.dto.TsuRuleRequest;
import com.iortatechnxt.brokerverse.catalog.api.dto.TsuRuleResponse;
import com.iortatechnxt.brokerverse.catalog.service.ProductRuleService;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Minimum-field matrix, document checklist (BRNB.002/003/093) and TSU routing rules (BRNB.098). */
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogRuleController {

  private final ProductRuleService rules;
  private final TsuRoutingService tsu;

  /**
   * Creates the controller.
   *
   * @param rules field and document rules
   * @param tsu TSU routing rules
   */
  public CatalogRuleController(ProductRuleService rules, TsuRoutingService tsu) {
    this.rules = rules;
    this.tsu = tsu;
  }

  /**
   * Every minimum-field rule.
   *
   * @return rules
   */
  @GetMapping("/field-rules")
  @PreAuthorize(CatalogAccess.READ)
  public List<FieldRuleResponse> fieldRules() {
    return rules.fieldRules().stream().map(FieldRuleResponse::from).toList();
  }

  /**
   * Adds a minimum-field rule.
   *
   * @param request rule
   * @return rule
   */
  @PostMapping("/field-rules")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public FieldRuleResponse createFieldRule(@Valid @RequestBody FieldRuleRequest request) {
    return FieldRuleResponse.from(
        rules.createFieldRule(
            request.key(), request.label().trim(), request.required(), request.sortOrder()));
  }

  /**
   * Changes a minimum-field rule.
   *
   * @param id rule
   * @param request label, flag and order
   * @return rule
   */
  @PutMapping("/field-rules/{id}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public FieldRuleResponse updateFieldRule(
      @PathVariable Long id, @Valid @RequestBody FieldRuleRequest request) {
    return FieldRuleResponse.from(
        rules.updateFieldRule(id, request.label().trim(), request.required(), request.sortOrder()));
  }

  /**
   * Every document rule.
   *
   * @return rules
   */
  @GetMapping("/document-rules")
  @PreAuthorize(CatalogAccess.READ)
  public List<DocumentRuleResponse> documentRules() {
    return rules.documentRules().stream().map(DocumentRuleResponse::from).toList();
  }

  /**
   * Adds a document rule.
   *
   * @param request rule
   * @return rule
   */
  @PostMapping("/document-rules")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public DocumentRuleResponse createDocumentRule(@Valid @RequestBody DocumentRuleRequest request) {
    return DocumentRuleResponse.from(
        rules.createDocumentRule(
            request.scope(), request.scopeCode(), request.documentType(), request.required()));
  }

  /**
   * Changes the mandatory flag of a document rule.
   *
   * @param id rule
   * @param request rule
   * @return rule
   */
  @PutMapping("/document-rules/{id}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public DocumentRuleResponse updateDocumentRule(
      @PathVariable Long id, @Valid @RequestBody DocumentRuleRequest request) {
    return DocumentRuleResponse.from(rules.updateDocumentRule(id, request.required()));
  }

  /**
   * TSU routing rules in evaluation order.
   *
   * @return rules
   */
  @GetMapping("/tsu-rules")
  @PreAuthorize(CatalogAccess.READ)
  public List<TsuRuleResponse> tsuRules() {
    return tsu.rules().stream().map(TsuRuleResponse::from).toList();
  }

  /**
   * Adds a TSU routing rule.
   *
   * @param request rule
   * @return rule
   */
  @PostMapping("/tsu-rules")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public TsuRuleResponse createTsuRule(@Valid @RequestBody TsuRuleRequest request) {
    return TsuRuleResponse.from(tsu.create(request.code(), request.criteria()));
  }

  /**
   * Changes a TSU routing rule.
   *
   * @param id rule
   * @param request criteria
   * @return rule
   */
  @PutMapping("/tsu-rules/{id}")
  @PreAuthorize(CatalogAccess.MAINTAIN)
  public TsuRuleResponse updateTsuRule(
      @PathVariable Long id, @Valid @RequestBody TsuRuleRequest request) {
    return TsuRuleResponse.from(tsu.update(id, request.criteria()));
  }
}
