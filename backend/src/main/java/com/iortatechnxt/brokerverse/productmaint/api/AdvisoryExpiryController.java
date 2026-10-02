package com.iortatechnxt.brokerverse.productmaint.api;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.AdvisoryBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.AdvisoryView;
import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.ExpiryRow;
import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.RenewalBody;
import com.iortatechnxt.brokerverse.productmaint.api.dto.OutputDtos.RenewalView;
import com.iortatechnxt.brokerverse.productmaint.domain.Advisory;
import com.iortatechnxt.brokerverse.productmaint.service.AdvisoryService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageExpiryService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Package advisories and the package expiry list (BRPM.016/017): the advisories of a request and
 * the drafts pending for a company, editing and sending (blocked without the supporting documents);
 * the packages by end date with their renewal status and the bulk "Generate Renewal Request".
 */
@RestController
@RequestMapping("/api/v1/product-maintenance")
public class AdvisoryExpiryController {

  private static final String ADVISORY = "hasAuthority('PKG_ADVISORY')";
  private static final String EXPIRY_VIEW =
      "hasAnyAuthority('PKG_NEGOTIATE', 'PRODUCT_MAINTAIN', 'PKG_REPORT_VIEW')";
  private static final int DEFAULT_WITHIN = 90;
  private static final int MAX_WITHIN = 730;

  private final AdvisoryService advisories;
  private final PackageExpiryService expiry;

  /**
   * Creates the controller.
   *
   * @param advisories advisories
   * @param expiry expiry list and renewals
   */
  public AdvisoryExpiryController(AdvisoryService advisories, PackageExpiryService expiry) {
    this.advisories = advisories;
    this.expiry = expiry;
  }

  /**
   * Advisories of a request, newest first.
   *
   * @param id request
   * @return advisories
   */
  @GetMapping("/requests/{id}/advisories")
  @PreAuthorize(PackageRequestController.VIEW)
  public List<AdvisoryView> ofRequest(@PathVariable Long id) {
    return advisories.ofRequest(id).stream().map(this::view).toList();
  }

  /**
   * Draft advisories of a company.
   *
   * @param companyId company
   * @return drafts
   */
  @GetMapping("/advisories")
  @PreAuthorize(PackageRequestController.VIEW)
  public List<AdvisoryView> pending(@RequestParam Long companyId) {
    return advisories.pending(companyId).stream().map(this::view).toList();
  }

  /**
   * Changes a draft advisory.
   *
   * @param advisoryId advisory
   * @param body groups, addresses, subject and text
   * @return the advisory
   */
  @PutMapping("/advisories/{advisoryId}")
  @PreAuthorize(ADVISORY)
  public AdvisoryView update(@PathVariable Long advisoryId, @Valid @RequestBody AdvisoryBody body) {
    return view(
        advisories.update(
            advisoryId,
            new Advisory.Content(
                body.groups(), body.emailTo(), body.subject(), body.body(), null)));
  }

  /**
   * Sends an advisory (blocked without the supporting documents).
   *
   * @param advisoryId advisory
   * @return the advisory
   */
  @PostMapping("/advisories/{advisoryId}/send")
  @PreAuthorize(ADVISORY)
  public AdvisoryView send(@PathVariable Long advisoryId) {
    return view(advisories.send(advisoryId));
  }

  /**
   * Released packages ending within n days, with their renewal status.
   *
   * @param companyId company
   * @param within look-ahead in days (default 90)
   * @return packages
   */
  @GetMapping("/expiry")
  @PreAuthorize(EXPIRY_VIEW)
  public List<ExpiryRow> expiring(
      @RequestParam Long companyId, @RequestParam(required = false) Integer within) {
    int days = within == null ? DEFAULT_WITHIN : Math.min(Math.max(within, 0), MAX_WITHIN);
    return expiry.expiring(companyId, days).stream().map(ExpiryRow::from).toList();
  }

  /**
   * Generates renewal requests for packages (bulk action).
   *
   * @param body company and products
   * @return one result per package
   */
  @PostMapping("/expiry/renewal-requests")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('PKG_NEGOTIATE')")
  public List<RenewalView> renew(@RequestBody RenewalBody body) {
    if (body.companyId() == null) {
      throw new BusinessRuleException("COMPANY_REQUIRED", "Select the company");
    }
    return expiry.generateRenewals(body.companyId(), body.productCodes()).stream()
        .map(RenewalView::from)
        .toList();
  }

  private AdvisoryView view(Advisory a) {
    return AdvisoryView.from(a, advisories.checklist(a));
  }
}
