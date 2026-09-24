package com.iortatechnxt.brokerverse.consolidation.api;

import com.iortatechnxt.brokerverse.consolidation.api.dto.IntercompanyTransactionRequest;
import com.iortatechnxt.brokerverse.consolidation.api.dto.IntercompanyTransactionResponse;
import com.iortatechnxt.brokerverse.consolidation.api.dto.ReconciliationLineResponse;
import com.iortatechnxt.brokerverse.consolidation.api.dto.RelationshipRequest;
import com.iortatechnxt.brokerverse.consolidation.api.dto.RelationshipResponse;
import com.iortatechnxt.brokerverse.consolidation.service.IntercompanyReconciliationService;
import com.iortatechnxt.brokerverse.consolidation.service.IntercompanyService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Inter-company relationships, mirror-journal transactions and due-to/due-from reconciliation. */
@RestController
@RequestMapping("/api/v1/intercompany")
public class IntercompanyController {

  private static final String RUN = "hasAuthority('CONSOLIDATION_RUN')";
  private static final String VIEW = "hasAnyAuthority('CONSOLIDATION_RUN','REPORT_FINANCIAL')";

  private final IntercompanyService service;
  private final IntercompanyReconciliationService reconciliation;

  /**
   * Creates the controller.
   *
   * @param service inter-company service
   * @param reconciliation reconciliation service
   */
  public IntercompanyController(
      IntercompanyService service, IntercompanyReconciliationService reconciliation) {
    this.service = service;
    this.reconciliation = reconciliation;
  }

  /**
   * Lists relationships.
   *
   * @param companyId optional company
   * @return relationships
   */
  @GetMapping("/relationships")
  @PreAuthorize(VIEW)
  public List<RelationshipResponse> relationships(@RequestParam(required = false) Long companyId) {
    return service.relationships(companyId).stream().map(RelationshipResponse::from).toList();
  }

  /**
   * Creates a relationship.
   *
   * @param request request
   * @return relationship
   */
  @PostMapping("/relationships")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(RUN)
  public RelationshipResponse create(@Valid @RequestBody RelationshipRequest request) {
    return RelationshipResponse.from(service.createRelationship(request));
  }

  /**
   * Activates or deactivates a relationship.
   *
   * @param id id
   * @param active new state
   * @return relationship
   */
  @PostMapping("/relationships/{id}/active")
  @PreAuthorize(RUN)
  public RelationshipResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
    return RelationshipResponse.from(service.setActive(id, active));
  }

  /**
   * Lists the transactions of a company.
   *
   * @param companyId company
   * @return transactions
   */
  @GetMapping("/transactions")
  @PreAuthorize(VIEW)
  public List<IntercompanyTransactionResponse> transactions(@RequestParam Long companyId) {
    return service.transactions(companyId).stream()
        .map(IntercompanyTransactionResponse::from)
        .toList();
  }

  /**
   * Posts an inter-company transaction (two mirror journals).
   *
   * @param request request
   * @return transaction
   */
  @PostMapping("/transactions")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(RUN)
  public IntercompanyTransactionResponse post(
      @Valid @RequestBody IntercompanyTransactionRequest request) {
    return IntercompanyTransactionResponse.from(service.post(request));
  }

  /**
   * Due-to / due-from reconciliation.
   *
   * @param companyId optional company
   * @param asOf as-of date
   * @return lines
   */
  @GetMapping("/reconciliation")
  @PreAuthorize(VIEW)
  public List<ReconciliationLineResponse> reconcile(
      @RequestParam(required = false) Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
    return reconciliation.reconcile(companyId, asOf).stream()
        .map(ReconciliationLineResponse::from)
        .toList();
  }
}
