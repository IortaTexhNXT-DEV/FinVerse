package com.iortatechnxt.brokerverse.collections.legacy.api;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.iortatechnxt.brokerverse.collections.legacy.domain.LegacyState;
import com.iortatechnxt.brokerverse.collections.legacy.service.LegacyItemStateService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The follow-up carried from legacy for a legacy invoice (Data Migration object F03), shown on the
 * collection account.
 */
@RestController
@RequestMapping("/api/v1/collections")
public class LegacyFollowUpController {

  private final LegacyItemStateService states;

  /**
   * Creates the controller.
   *
   * @param states legacy follow-up
   */
  public LegacyFollowUpController(LegacyItemStateService states) {
    this.states = states;
  }

  /**
   * The legacy follow-up of an account.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @return rows in load order
   */
  @GetMapping("/items/{invoiceNo}/legacy-follow-up")
  @PreAuthorize("hasAuthority('CLX_VIEW')")
  public List<FollowUpResponse> followUp(
      @RequestParam Long companyId, @PathVariable String invoiceNo) {
    return states.of(companyId, invoiceNo).stream().map(FollowUpResponse::from).toList();
  }

  /**
   * A follow-up row.
   *
   * @param legacyInvoiceNo legacy invoice
   * @param sourceSystem source system
   * @param migrationBatch loading batch
   * @param row the follow-up (unwrapped)
   */
  public record FollowUpResponse(
      String legacyInvoiceNo,
      String sourceSystem,
      String migrationBatch,
      @JsonUnwrapped LegacyState.Row row) {

    static FollowUpResponse from(LegacyState s) {
      return new FollowUpResponse(
          s.getLegacyInvoiceNo(), s.getSourceSystem(), s.getMigrationBatch(), s.row());
    }
  }
}
