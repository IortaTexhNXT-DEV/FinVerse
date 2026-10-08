package com.iortatechnxt.brokerverse.renewal.candidate.api.dto;

import com.iortatechnxt.brokerverse.renewal.domain.CandidateFlags;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotClient;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotProduct;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotSales;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Responses of the renewal lists and record page. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // namespace of records
public final class CandidateDtos {

  private static final SnapshotClient NO_CLIENT = new SnapshotClient(null, null, null, null, null);
  private static final SnapshotProduct NO_PRODUCT =
      new SnapshotProduct(null, null, null, null, null, null);
  private static final SnapshotSales NO_SALES =
      new SnapshotSales(null, null, null, null, null, null);

  private CandidateDtos() {}

  /**
   * A row of a renewal list (FR-RN-013, section 6.2 columns): premium columns are empty for the
   * read-only projection of LAMD and Contact Center (RQ21).
   *
   * @param renewalRef renewal reference
   * @param stage stage code
   * @param stageLabel stage name
   * @param bucket bucket
   * @param disposition disposition
   * @param reason reason for Not for Renewal
   * @param remarks disposition remarks
   * @param policy policy columns
   * @param parties client, sales and officer columns
   * @param money premium columns (empty when hidden)
   * @param flags flag chips
   * @param expiry expiry date
   * @param daysToExpiry days to expiry
   */
  public record CandidateRow(
      String renewalRef,
      String stage,
      String stageLabel,
      String bucket,
      String disposition,
      String reason,
      String remarks,
      PolicyColumns policy,
      PartyColumns parties,
      MoneyColumns money,
      FlagChips flags,
      LocalDate expiry,
      long daysToExpiry,
      Names names) {

    /**
     * Maps a candidate.
     *
     * @param c candidate
     * @param money premium, outstanding and claims columns
     * @param daysToExpiry days to expiry
     * @param names names of the insurer and the owner unit
     * @return row
     */
    public static CandidateRow of(
        RenewalCandidate c, MoneyColumns money, long daysToExpiry, Names names) {
      return new CandidateRow(
          c.getRenewalRef(),
          c.getStage().name(),
          c.getStage().label(),
          c.getBucket() == null ? null : c.getBucket().name(),
          c.getDisposition().code() == null ? null : c.getDisposition().code().name(),
          c.getDisposition().reasonCode(),
          c.getDisposition().remarks(),
          PolicyColumns.of(c),
          PartyColumns.of(c),
          money,
          FlagChips.of(c),
          c.getExpiryDate(),
          daysToExpiry,
          names);
    }
  }

  /**
   * The names the lists show for the codes of a renewal, read by the server so that every user of
   * Renewal (Contact Center and LAMD included) sees them, whatever master data the user may read.
   *
   * @param insurer insurer name, the code when the insurer is not known
   * @param ownerUnit name of the owner unit, the code when the unit is not known
   * @param product name of the product (risk), the code when the product is not known
   */
  public record Names(String insurer, String ownerUnit, String product) {}

  /**
   * Policy columns.
   *
   * @param source source
   * @param sourceRef legacy reference or SBM number
   * @param expiringInvoiceNo expiring invoice
   * @param expiringArn expiring ARN
   * @param policyNo expiring policy number
   * @param coverNo cover number
   * @param versionNo package version
   * @param productCode risk code
   * @param productName risk name
   * @param lineCode product line
   * @param insurerCode insurer
   * @param inception inception date
   * @param pnNos PN numbers
   * @param mortgaged mortgaged
   * @param mortgageeBank mortgagee bank
   */
  public record PolicyColumns(
      String source,
      String sourceRef,
      String expiringInvoiceNo,
      String expiringArn,
      String policyNo,
      String coverNo,
      Integer versionNo,
      String productCode,
      String productName,
      String lineCode,
      String insurerCode,
      LocalDate inception,
      String pnNos,
      boolean mortgaged,
      String mortgageeBank) {

    static PolicyColumns of(RenewalCandidate c) {
      CandidateSnapshot s = c.getSnapshot();
      var product = s.product();
      return new PolicyColumns(
          c.getSource().name(),
          c.getSourceRef(),
          c.getExpiringInvoiceNo(),
          c.getExpiringArn(),
          s.policyNo(),
          s.coverNo(),
          s.versionNo(),
          product == null ? null : product.productCode(),
          product == null ? null : product.productName(),
          product == null ? null : product.lineCode(),
          s.insurerCode(),
          s.inceptionDate(),
          s.pnNos(),
          s.mortgage() != null && s.mortgage().mortgaged(),
          s.mortgage() == null ? null : s.mortgage().bank());
    }
  }

  /**
   * Client, classification and sales columns.
   *
   * @param clientId crm client
   * @param clientCode client code
   * @param clientName client name
   * @param assuredName assured
   * @param segment market segment
   * @param businessOrigin business origin
   * @param accountType account type
   * @param branchCode invoicing branch
   * @param regionCode region
   * @param departmentCode department
   * @param salesUnit sales unit of the account
   * @param ownerUnit Marketing unit that owns the renewal
   * @param unitHead unit head
   * @param accountOfficer account officer of the account
   * @param assignedAo assigned Marketing AO
   * @param assignedPo assigned Processing Officer
   */
  public record PartyColumns(
      Long clientId,
      String clientCode,
      String clientName,
      String assuredName,
      String segment,
      String businessOrigin,
      String accountType,
      String branchCode,
      String regionCode,
      String departmentCode,
      String salesUnit,
      String ownerUnit,
      String unitHead,
      String accountOfficer,
      String assignedAo,
      String assignedPo) {

    static PartyColumns of(RenewalCandidate c) {
      CandidateSnapshot s = c.getSnapshot();
      SnapshotClient client = s.client() == null ? NO_CLIENT : s.client();
      SnapshotProduct product = s.product() == null ? NO_PRODUCT : s.product();
      SnapshotSales sales = s.sales() == null ? NO_SALES : s.sales();
      return new PartyColumns(
          client.clientId(),
          client.clientCode(),
          s.clientName(),
          client.assuredName(),
          product.segment(),
          product.businessOrigin(),
          product.accountType(),
          sales.branchCode(),
          sales.regionCode(),
          sales.departmentCode(),
          sales.salesUnit(),
          c.getOwnerUnit(),
          sales.unitHead(),
          sales.accountOfficer(),
          c.getAssignedAo(),
          c.getAssignedPo());
    }
  }

  /**
   * Premium, outstanding premium and claims columns.
   *
   * @param currency currency
   * @param basicPremium basic premium
   * @param grossPremium gross premium
   * @param sumInsured total sum insured
   * @param premiumRate premium rate
   * @param commissionRate commission rate
   * @param outstanding outstanding premium of the family (latest check)
   * @param claimCount number of claims (Claims module), null when not connected
   * @param claimStatus claim statuses as read by the check
   */
  public record MoneyColumns(
      String currency,
      BigDecimal basicPremium,
      BigDecimal grossPremium,
      BigDecimal sumInsured,
      BigDecimal premiumRate,
      BigDecimal commissionRate,
      BigDecimal outstanding,
      Integer claimCount,
      String claimStatus) {

    /** Hidden premium columns (LAMD and Contact Center). */
    public static final MoneyColumns HIDDEN =
        new MoneyColumns(null, null, null, null, null, null, null, null, null);
  }

  /**
   * Flag chips (Workshop addendum): shown next to the reference, never inside the pill.
   *
   * @param urgent urgent (go-live window)
   * @param returned returned to the AO
   * @param transferred transferred from another unit
   * @param endorsed endorsement in progress
   * @param claims claims on the expiring term
   * @param outstanding outstanding premium
   * @param kycDue KYC review due
   * @param kycFlaggedAt when the KYC flag was first set
   * @param nrns not yet renewed or submitted
   * @param stp straight-through
   * @param locked locked by the Renewal Advice
   * @param nfrSent NFR sent
   * @param holdCoverUntil end of the confirmed hold cover (chip HC confirmed), null when none
   * @param closingRoute closing letter due at the effective expiry (NAL or NRL), null when none
   * @param closingLetter closing letter sent (NAL or NFR), null when none
   * @param attention attention flag of the listing (Ageing, Overdue, High risk), null when none
   * @param attentionRule rule that set the attention flag, null when none
   */
  public record FlagChips(
      boolean urgent,
      boolean returned,
      boolean transferred,
      boolean endorsed,
      boolean claims,
      boolean outstanding,
      boolean kycDue,
      Instant kycFlaggedAt,
      boolean nrns,
      boolean stp,
      boolean locked,
      boolean nfrSent,
      LocalDate holdCoverUntil,
      String closingRoute,
      String closingLetter,
      String attention,
      String attentionRule) {

    static FlagChips of(RenewalCandidate c) {
      CandidateFlags f = c.getFlags();
      return new FlagChips(
          f.isUrgent(),
          f.isReturned(),
          f.isTransferred(),
          f.isEndorsementPending(),
          f.isClaims(),
          f.isOutstanding(),
          f.isKycDue(),
          f.getKycFlaggedAt(),
          f.isNrns(),
          f.isStp(),
          c.getMarketingLockedAt() != null || c.getStage().isMarketingLocked(),
          f.isNfrSent(),
          c.getExpiry().getHoldCoverUntil(),
          c.getExpiry().getClosingRoute(),
          c.getExpiry().getClosingLetter(),
          c.getAttention().getFlag() == null ? null : c.getAttention().getFlag().label(),
          c.getAttention().getRule());
    }
  }
}
