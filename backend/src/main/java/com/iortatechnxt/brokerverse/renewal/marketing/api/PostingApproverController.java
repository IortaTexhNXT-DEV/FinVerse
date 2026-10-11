package com.iortatechnxt.brokerverse.renewal.marketing.api;

import com.iortatechnxt.brokerverse.renewal.marketing.service.PostingApprovers;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The approvers offered when an account is submitted for posting (FRRN.016.01). */
@RestController
@RequestMapping("/api/v1/renewal/posting-approvers")
@PreAuthorize("hasAnyAuthority('RNW_DISPOSE','RNW_REVIEW')")
public class PostingApproverController {

  private final PostingApprovers approvers;
  private final RenewalRecords records;

  /**
   * Creates the controller.
   *
   * @param approvers approvers
   * @param records renewals
   */
  public PostingApproverController(PostingApprovers approvers, RenewalRecords records) {
    this.approvers = approvers;
    this.records = records;
  }

  /**
   * The Team Leaders who may approve the posting of a renewal, and whether one must be chosen.
   *
   * @param companyId company
   * @param renewalRef renewal (its unit), may be null for every Team Leader
   * @return setting and approvers
   */
  @GetMapping
  @Transactional(readOnly = true)
  public Approvers list(
      @RequestParam Long companyId, @RequestParam(required = false) String renewalRef) {
    String unit =
        renewalRef == null || renewalRef.isBlank()
            ? null
            : records.get(companyId, renewalRef).getOwnerUnit();
    return new Approvers(approvers.mode(), approvers.of(companyId, unit));
  }

  /**
   * The setting and the approvers.
   *
   * @param mode REQUIRED, OPTIONAL or NONE
   * @param approvers Team Leaders
   */
  public record Approvers(String mode, List<PostingApprovers.Approver> approvers) {}
}
