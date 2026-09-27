package com.iortatechnxt.brokerverse.collections.bulk.api;

import com.iortatechnxt.brokerverse.collections.bulk.api.dto.BulkDtos.EscalateRequest;
import com.iortatechnxt.brokerverse.collections.bulk.api.dto.BulkDtos.PromisesRequest;
import com.iortatechnxt.brokerverse.collections.bulk.service.CollectionsBulkActions;
import com.iortatechnxt.brokerverse.collections.bulk.service.ItemResult;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationService.ManualEscalation;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService.PromiseInput;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bulk actions on selected collection accounts (BRCLXN.050/051): manual escalation of one or
 * several invoices ({@code CLX_ESCALATE}) and one promise to pay recorded on several invoices
 * ({@code CLX_BULK_UPDATE}). Each invoice gets its own outcome. The upload variant is the bulk
 * handler {@code CLX_BULK_UPDATE} on Bulk Processing.
 */
@RestController
@RequestMapping("/api/v1/collections/bulk")
public class CollectionsBulkController {

  private final CollectionsBulkActions actions;

  /**
   * Creates the controller.
   *
   * @param actions bulk actions
   */
  public CollectionsBulkController(CollectionsBulkActions actions) {
    this.actions = actions;
  }

  /**
   * Escalates invoices: one escalation per account (BRCLXN.050).
   *
   * @param request invoices, target, reason and remarks
   * @return outcome per invoice
   */
  @PostMapping("/escalate")
  @PreAuthorize("hasAuthority('CLX_ESCALATE')")
  public List<ItemResult> escalate(@Valid @RequestBody EscalateRequest request) {
    return actions.escalate(
        new ManualEscalation(
            request.companyId(),
            request.invoiceNos(),
            request.targetLevel(),
            request.targetUsername(),
            request.reasonCode(),
            request.remarks()));
  }

  /**
   * Records one promise to pay on several invoices (BRCLXN.051/055).
   *
   * @param request invoices, dates, amount and remarks
   * @return outcome per invoice
   */
  @PostMapping("/promises")
  @PreAuthorize("hasAuthority('CLX_BULK_UPDATE') and hasAuthority('CLX_WORK')")
  public List<ItemResult> promises(@Valid @RequestBody PromisesRequest request) {
    return actions.promise(
        request.companyId(),
        request.invoiceNos(),
        new PromiseInput(
            request.promisedOn(),
            request.promisedDate(),
            request.amount(),
            null,
            request.remarks()));
  }
}
