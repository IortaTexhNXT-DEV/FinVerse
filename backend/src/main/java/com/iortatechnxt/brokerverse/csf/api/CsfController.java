package com.iortatechnxt.brokerverse.csf.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.csf.domain.ChangeStatus;
import com.iortatechnxt.brokerverse.csf.service.ChangeQueryService;
import com.iortatechnxt.brokerverse.csf.service.ChangeQueryService.ChangeFilter;
import com.iortatechnxt.brokerverse.csf.service.ContactChangeService;
import com.iortatechnxt.brokerverse.csf.service.CsfCodes;
import com.iortatechnxt.brokerverse.csf.service.CsfDocumentService;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.AccountLine;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ChangeView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ClientSummary;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.DocumentView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.EpolicyView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.PaymentHistory;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.SearchView;
import com.iortatechnxt.brokerverse.csf.service.CustomerSearchService;
import com.iortatechnxt.brokerverse.csf.service.SearchKey;
import com.iortatechnxt.brokerverse.csf.service.ServicingViewService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer Search and Servicing View of the Customer Servicing Facility (FR-CSF-010 to 013, 021,
 * 030 to 033; CUSTOMER_SERVICING_DESIGN section 10): the search by key type and the tabs of a
 * client, each loaded on its own. Every read is under {@code CSF_VIEW}.
 */
@RestController
@RequestMapping("/api/v1/csf")
@PreAuthorize("hasAuthority('CSF_VIEW')")
public class CsfController {

  private final CustomerSearchService search;
  private final ServicingViewService views;
  private final CsfDocumentService documents;
  private final ContactChangeService changes;
  private final ChangeQueryService changeList;

  /**
   * Creates the controller.
   *
   * @param search customer search
   * @param views servicing view
   * @param documents documents
   * @param changes contact changes of a client
   * @param changeList contact changes of the company
   */
  public CsfController(
      CustomerSearchService search,
      ServicingViewService views,
      CsfDocumentService documents,
      ContactChangeService changes,
      ChangeQueryService changeList) {
    this.search = search;
    this.views = views;
    this.documents = documents;
    this.changes = changes;
    this.changeList = changeList;
  }

  /**
   * Searches clients by one key; results grouped by client with the matching accounts.
   *
   * @param companyId company
   * @param keyType NAME, CLIENT_ID, ACCOUNT_NO, PN_NO or APPLICATION_NO
   * @param q value
   * @return clients found
   */
  @GetMapping("/search")
  public SearchView search(
      @RequestParam Long companyId, @RequestParam SearchKey keyType, @RequestParam String q) {
    return search.search(companyId, keyType, q);
  }

  /**
   * The summary card of a client (logs the opening of the Servicing View).
   *
   * @param companyId company
   * @param clientId client
   * @return summary
   */
  @GetMapping("/clients/{clientId}")
  public ClientSummary summary(@RequestParam Long companyId, @PathVariable Long clientId) {
    return views.summary(companyId, clientId);
  }

  /**
   * The Accounts tab.
   *
   * @param companyId company
   * @param clientId client
   * @return accounts
   */
  @GetMapping("/clients/{clientId}/accounts")
  public List<AccountLine> accounts(@RequestParam Long companyId, @PathVariable Long clientId) {
    return views.accounts(companyId, clientId);
  }

  /**
   * The Payments tab.
   *
   * @param companyId company
   * @param clientId client
   * @param months months back, default the parameter CSF_PAYMENT_HISTORY_MONTHS
   * @param arn one account only
   * @return payment history
   */
  @GetMapping("/clients/{clientId}/payments")
  public PaymentHistory payments(
      @RequestParam Long companyId,
      @PathVariable Long clientId,
      @RequestParam(required = false) Integer months,
      @RequestParam(required = false) String arn) {
    return views.payments(companyId, clientId, months, arn);
  }

  /**
   * The Renewal Advice tab: documents of type RENEWAL_ADVICE of the client and its accounts.
   *
   * @param companyId company
   * @param clientId client
   * @return renewal advices
   */
  @GetMapping("/clients/{clientId}/renewal-advices")
  public List<DocumentView> renewalAdvices(
      @RequestParam Long companyId, @PathVariable Long clientId) {
    return documents.ofType(companyId, clientId, CsfCodes.DOC_RENEWAL_ADVICE);
  }

  /**
   * The E-policies tab.
   *
   * @param companyId company
   * @param clientId client
   * @return e-policies
   */
  @GetMapping("/clients/{clientId}/epolicies")
  public List<EpolicyView> epolicies(@RequestParam Long companyId, @PathVariable Long clientId) {
    return views.epolicies(companyId, clientId);
  }

  /**
   * The Documents tab.
   *
   * @param companyId company
   * @param clientId client
   * @return documents
   */
  @GetMapping("/clients/{clientId}/documents")
  public List<DocumentView> documents(@RequestParam Long companyId, @PathVariable Long clientId) {
    return documents.list(companyId, clientId);
  }

  /**
   * The Contact History tab.
   *
   * @param companyId company
   * @param clientId client
   * @return contact changes, refusals and referrals
   */
  @GetMapping("/clients/{clientId}/contact-changes")
  public List<ChangeView> contactHistory(
      @RequestParam Long companyId, @PathVariable Long clientId) {
    return changes.history(companyId, clientId);
  }

  /**
   * The Contact Changes list of the company.
   *
   * @param companyId company
   * @param status APPLIED, REFUSED or REFERRED
   * @param agent agent user ID
   * @param from first day
   * @param to last day
   * @param q change number, client code or name
   * @param pageable page
   * @return changes
   */
  @GetMapping("/contact-changes")
  @PreAuthorize("hasAnyAuthority('CSF_REPORT_VIEW', 'CSF_CONTACT_UPDATE')")
  public PageResponse<ChangeView> changes(
      @RequestParam Long companyId,
      @RequestParam(required = false) ChangeStatus status,
      @RequestParam(required = false) String agent,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String q,
      Pageable pageable) {
    return PageResponse.of(
        changeList.list(new ChangeFilter(companyId, status, agent, from, to, q), pageable), v -> v);
  }
}
