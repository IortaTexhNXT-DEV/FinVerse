package com.iortatechnxt.brokerverse.crm.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.crm.api.dto.Client360Response;
import com.iortatechnxt.brokerverse.crm.api.dto.Client360Response.HistoryEntry;
import com.iortatechnxt.brokerverse.crm.api.dto.ClientListItem;
import com.iortatechnxt.brokerverse.crm.service.Client360Service;
import com.iortatechnxt.brokerverse.crm.service.KycDueQuery;
import com.iortatechnxt.brokerverse.crm.service.KycReviewService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Client 360 (BRNB.099: linked records, missing linkages, history) and the KYC reviews due list
 * (BRNB.110).
 */
@RestController
@RequestMapping("/api/v1/crm")
@PreAuthorize("hasAuthority('CLIENT_VIEW')")
public class Client360Controller {

  private static final int PAGE_SIZE = 25;
  private static final String ALL = "ALL";

  private final Client360Service view;
  private final KycReviewService reviews;

  /**
   * Creates the controller.
   *
   * @param view client 360 service
   * @param reviews KYC reviews
   */
  public Client360Controller(Client360Service view, KycReviewService reviews) {
    this.view = view;
    this.reviews = reviews;
  }

  /**
   * Records linked to the client across modules, and flagged gaps.
   *
   * @param id client
   * @return 360 view
   */
  @GetMapping("/clients/{id}/records")
  public Client360Response records(@PathVariable Long id) {
    return Client360Response.from(view.view(id));
  }

  /**
   * Audit history of the client.
   *
   * @param id client
   * @return entries, newest first
   */
  @GetMapping("/clients/{id}/history")
  public List<HistoryEntry> history(@PathVariable Long id) {
    return view.history(id).stream().map(HistoryEntry::from).toList();
  }

  /**
   * Clients whose KYC review is overdue or due (BRNB.110).
   *
   * @param companyId company
   * @param dueBy last review date included (default: the configured window)
   * @param bank NON_BANK (default), BANK or ALL
   * @param riskRating risk rating
   * @param marketSegment segment
   * @param page page
   * @return clients, earliest due first
   */
  @GetMapping("/kyc-reviews")
  public PageResponse<ClientListItem> kycReviews(
      @RequestParam Long companyId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dueBy,
      @RequestParam(defaultValue = "NON_BANK") String bank,
      @RequestParam(required = false) String riskRating,
      @RequestParam(required = false) String marketSegment,
      @RequestParam(defaultValue = "0") int page) {
    Boolean bankClient = ALL.equals(bank) ? null : "BANK".equals(bank);
    return PageResponse.of(
        reviews.due(
            new KycDueQuery(companyId, dueBy, bankClient, riskRating, marketSegment),
            PageRequest.of(page, PAGE_SIZE, Sort.by("kycReviewDue", "displayName"))),
        ClientListItem::from);
  }
}
