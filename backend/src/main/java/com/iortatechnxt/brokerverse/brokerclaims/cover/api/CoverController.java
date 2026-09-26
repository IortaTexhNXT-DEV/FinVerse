package com.iortatechnxt.brokerverse.brokerclaims.cover.api;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.BrokerClaimQueryService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.CoverClaimDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.CoverHit;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.EndorsementDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.InvoiceDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.ItemDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.PolicyYearDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.PremiumDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.ShareDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverViews.ClaimDraft;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverViews.CoverDetail;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.CoverService.SearchBy;
import com.iortatechnxt.brokerverse.brokerclaims.domain.CoverSnapshot;
import com.iortatechnxt.brokerverse.brokerclaims.location.api.dto.LocationDtos.LocationRefResponse;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.LocationRefService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cover Lookup and the cover card of Record Claim (BRCLM.001/002/003/016/039/042; FR-CL-010/011):
 * read-only views of any cover of the company. Opening a cover is logged as a view in the audit
 * trail of the account.
 */
@RestController
@RequestMapping("/api/v1/broker-claims/covers")
@Transactional(readOnly = true)
public class CoverController {

  private static final String LOOKUP = "hasAnyAuthority('BCL_COVER_VIEW', 'BCL_RECORD')";

  private final CoverService covers;
  private final BrokerClaimQueryService claims;
  private final LocationRefService refs;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;

  /**
   * Creates the controller.
   *
   * @param covers covers
   * @param claims claims of a cover
   * @param refs insurer location references
   * @param audit view log
   * @param currentUser current user
   */
  public CoverController(
      CoverService covers,
      BrokerClaimQueryService claims,
      LocationRefService refs,
      AuditTrailService audit,
      CurrentUser currentUser) {
    this.covers = covers;
    this.claims = claims;
    this.refs = refs;
    this.audit = audit;
    this.currentUser = currentUser;
  }

  /**
   * Covers by ARN, policy number or assured.
   *
   * @param companyId company
   * @param by ARN, POLICY_NO or ASSURED
   * @param q text, at least 3 characters
   * @return covers
   */
  @GetMapping
  @PreAuthorize(LOOKUP)
  public List<CoverHit> search(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "ARN") SearchBy by,
      @RequestParam String q) {
    return covers.search(companyId, by, q).stream().map(CoverHit::from).toList();
  }

  /**
   * One cover, read-only.
   *
   * @param arn cover
   * @param companyId company
   * @return cover with its years, items, endorsements, invoices, claims and references
   */
  @GetMapping("/{arn}")
  @PreAuthorize(LOOKUP)
  public CoverDetail cover(@PathVariable String arn, @RequestParam Long companyId) {
    Account a = covers.account(companyId, arn);
    audit.recordIndependently(
        currentUser.username(),
        "Account",
        a.getArn(),
        AuditAction.OPEN,
        "Cover viewed in Claims Handling");
    return new CoverDetail(
        CoverHit.from(a),
        Math.max(1, a.getTermYears()),
        a.getTotalSumInsured(),
        a.getSales() == null ? null : a.getSales().team(),
        a.getSales() == null ? null : a.getSales().accountOfficer(),
        a.getPaymentArrangement() == null ? null : a.getPaymentArrangement().name(),
        CoverService.policyYears(a).stream().map(PolicyYearDto::from).toList(),
        a.getItems().stream().map(ItemDto::from).toList(),
        covers.endorsements(a.getArn()).stream().map(EndorsementDto::from).toList(),
        covers.invoices(a.getArn()).stream().map(InvoiceDto::from).toList(),
        claims.ofCover(companyId, a.getArn()).stream().map(CoverClaimDto::from).toList(),
        refs.ofCover(companyId, a.getArn()).stream().map(LocationRefResponse::from).toList());
  }

  /**
   * The cover card of Record Claim: snapshot facts, version at the loss date, premium check,
   * locations and proposed insurers.
   *
   * @param arn cover
   * @param companyId company
   * @param policyYear policy year
   * @param lossDate loss date, today when empty
   * @return card
   */
  @GetMapping("/{arn}/claim-draft")
  @PreAuthorize("hasAuthority('BCL_RECORD')")
  public ClaimDraft draft(
      @PathVariable String arn,
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "1") int policyYear,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate lossDate) {
    Account a = covers.account(companyId, arn);
    CoverSnapshot snapshot = covers.snapshot(a, policyYear, lossDate);
    return new ClaimDraft(
        CoverHit.from(a),
        CoverService.policyYears(a).stream().map(PolicyYearDto::from).toList(),
        policyYear,
        snapshot.getPolicyNo(),
        snapshot.getCoverVersionNo(),
        snapshot.versionLabel(),
        snapshot.getPeriodFrom(),
        snapshot.getPeriodTo(),
        snapshot.getSumInsured(),
        snapshot.getCurrency(),
        snapshot.getSalesTeam(),
        snapshot.getAccountOfficer(),
        snapshot.getInvoicingBranchId(),
        PremiumDto.from(covers.premium(a.getArn(), policyYear)),
        CoverService.locations(a).stream().map(ItemDto::from).toList(),
        covers.shares(a, policyYear).stream().map(ShareDto::from).toList(),
        lossDate == null || snapshot.covers(lossDate));
  }
}
