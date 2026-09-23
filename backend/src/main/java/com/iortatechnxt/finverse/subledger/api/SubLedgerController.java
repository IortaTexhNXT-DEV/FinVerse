package com.iortatechnxt.finverse.subledger.api;

import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.subledger.api.dto.MatchRequest;
import com.iortatechnxt.finverse.subledger.api.dto.OpenItemResponse;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Party sub-ledger inquiry and matching (knock-off). */
@RestController
@RequestMapping("/api/v1/subledger")
public class SubLedgerController {

  private final OpenItemService openItems;
  private final PartyService parties;

  /**
   * Creates the controller.
   *
   * @param openItems open item service
   * @param parties party service
   */
  public SubLedgerController(OpenItemService openItems, PartyService parties) {
    this.openItems = openItems;
    this.parties = parties;
  }

  /**
   * Lists a party's open items (statement of account).
   *
   * @param companyId company
   * @param partyCode party code
   * @return items
   */
  @GetMapping("/items")
  @PreAuthorize("hasAuthority('JOURNAL_VIEW')")
  public List<OpenItemResponse> partyItems(
      @RequestParam Long companyId, @RequestParam String partyCode) {
    Long partyId = parties.getByCode(companyId, partyCode).getId();
    return openItems.partyItems(companyId, partyId).stream().map(OpenItemResponse::from).toList();
  }

  /**
   * Lists all outstanding items as of a date.
   *
   * @param companyId company
   * @param asOf date
   * @return items
   */
  @GetMapping("/outstanding")
  @PreAuthorize("hasAuthority('JOURNAL_VIEW')")
  public List<OpenItemResponse> outstanding(
      @RequestParam Long companyId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
    return openItems.outstanding(companyId, asOf).stream().map(OpenItemResponse::from).toList();
  }

  /**
   * Matches a debit against a credit item.
   *
   * @param request match request
   * @return updated debit item
   */
  @PostMapping("/match")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public OpenItemResponse match(@Valid @RequestBody MatchRequest request) {
    openItems.match(
        request.debitItemId(), request.creditItemId(), request.amount(), request.matchDate());
    return OpenItemResponse.from(openItems.get(request.debitItemId()));
  }

  /**
   * Allocates an item against the party's oldest opposite items.
   *
   * @param id item
   * @param date matching date
   * @return item after allocation
   */
  @PostMapping("/items/{id}/allocate")
  @PreAuthorize("hasAuthority('RECEIPT_PAYMENT_MAINTAIN')")
  public OpenItemResponse allocate(
      @PathVariable Long id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    openItems.allocateFifo(id, date);
    return OpenItemResponse.from(openItems.get(id));
  }
}
