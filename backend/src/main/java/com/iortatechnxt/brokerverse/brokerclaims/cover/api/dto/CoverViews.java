package com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto;

import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.CoverClaimDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.CoverHit;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.EndorsementDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.InvoiceDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.ItemDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.PolicyYearDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.PremiumDto;
import com.iortatechnxt.brokerverse.brokerclaims.cover.api.dto.CoverDtos.ShareDto;
import com.iortatechnxt.brokerverse.brokerclaims.location.api.dto.LocationDtos.LocationRefResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The composed views of a cover: the Cover Lookup page and the cover card of Record Claim. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class CoverViews {

  private CoverViews() {}

  /**
   * A cover read-only (BRCLM.002/003/042; FR-CL-010).
   *
   * @param header account facts
   * @param termYears policy years of the term
   * @param sumInsured total sum insured
   * @param salesTeam Marketing team
   * @param accountOfficer account officer
   * @param paymentArrangement via BDOI or direct to the insurer
   * @param years policy years with their policy numbers
   * @param items risk items and locations
   * @param endorsements endorsements (cover versions)
   * @param invoices ledger invoices with their payment and remittance status
   * @param claims claims of the cover
   * @param locationRefs insurer location references, current and past
   */
  public record CoverDetail(
      CoverHit header,
      int termYears,
      BigDecimal sumInsured,
      String salesTeam,
      String accountOfficer,
      String paymentArrangement,
      List<PolicyYearDto> years,
      List<ItemDto> items,
      List<EndorsementDto> endorsements,
      List<InvoiceDto> invoices,
      List<CoverClaimDto> claims,
      List<LocationRefResponse> locationRefs) {}

  /**
   * The cover card of Record Claim for a policy year and loss date (BRCLM.001/003/016/037/039/043).
   *
   * @param header account facts
   * @param years policy years of the term
   * @param policyYear chosen policy year
   * @param policyNo policy number of the year, null while not issued
   * @param versionNo cover version at the loss date
   * @param versionLabel "Cover v&lt;n&gt; (&lt;endorsement no.&gt;)"
   * @param periodFrom start of the policy year
   * @param periodTo end of the policy year
   * @param sumInsured sum insured
   * @param currency claim currency
   * @param salesTeam Marketing team
   * @param accountOfficer account officer
   * @param invoicingBranchId invoicing branch
   * @param premium premium check with the invoices not fully paid
   * @param locations insured locations of the cover
   * @param insurers insurers and shares proposed
   * @param lossInsidePeriod the loss date is inside the policy year (false asks for confirmation)
   */
  public record ClaimDraft(
      CoverHit header,
      List<PolicyYearDto> years,
      int policyYear,
      String policyNo,
      int versionNo,
      String versionLabel,
      LocalDate periodFrom,
      LocalDate periodTo,
      BigDecimal sumInsured,
      String currency,
      String salesTeam,
      String accountOfficer,
      Long invoicingBranchId,
      PremiumDto premium,
      List<ItemDto> locations,
      List<ShareDto> insurers,
      boolean lossInsidePeriod) {}
}
