package com.iortatechnxt.brokerverse.brokerclaims.insurer.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaimRepository;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds the claim of a bulk upload row (BRCLM.041/043; design 9.5): by BDOI claim number, or by
 * insurer and insurer claim number when the insurer's file carries only its own reference.
 */
@Component
@Transactional(readOnly = true)
public class BulkClaimResolver {

  /** Column of the BDOI claim number. */
  public static final String CLAIM_NO = "Claim No.";

  /** Column of the insurer. */
  public static final String INSURER = "Insurer";

  /** Column of the insurer claim number. */
  public static final String INSURER_CLAIM_NO = "Insurer Claim No.";

  private static final String NOT_FOUND = "BCL_BULK_CLAIM";

  private final BrokerClaimRepository claims;
  private final InsurerClaimRepository lines;

  /**
   * Creates the resolver.
   *
   * @param claims claims
   * @param lines insurer lines
   */
  public BulkClaimResolver(BrokerClaimRepository claims, InsurerClaimRepository lines) {
    this.claims = claims;
    this.lines = lines;
  }

  /**
   * The claim of a row by claim number only.
   *
   * @param companyId company
   * @param row row
   * @return the claim
   */
  public Claim byClaimNo(Long companyId, BulkRow row) {
    String claimNo = row.text(CLAIM_NO);
    if (claimNo == null) {
      throw new BusinessRuleException(NOT_FOUND, "Enter the claim number");
    }
    return claims
        .findByCompanyIdAndClaimNo(companyId, claimNo)
        .orElseThrow(() -> new BusinessRuleException(NOT_FOUND, "No claim found for " + claimNo));
  }

  /**
   * The claim and insurer line of a row: by claim number, else by insurer and insurer claim number.
   *
   * @param companyId company
   * @param row row
   * @return the claim and the matching line (null when found by claim number)
   */
  public Match match(Long companyId, BulkRow row) {
    if (row.text(CLAIM_NO) != null) {
      return new Match(byClaimNo(companyId, row), null);
    }
    String insurer = row.text(INSURER);
    String number = row.text(INSURER_CLAIM_NO);
    if (insurer == null || number == null) {
      throw new BusinessRuleException(
          NOT_FOUND, "Enter the claim number, or the insurer and the insurer claim number");
    }
    List<InsurerClaim> found = lines.findNumbered(companyId, insurer, number);
    List<Long> claimIds = found.stream().map(InsurerClaim::getClaimId).distinct().toList();
    if (claimIds.size() != 1) {
      throw new BusinessRuleException(
          NOT_FOUND,
          claimIds.isEmpty()
              ? "No claim found for " + insurer + " " + number
              : "Several claims carry " + insurer + " " + number + "; enter the claim number");
    }
    Claim claim = claims.findById(claimIds.get(0)).orElseThrow();
    return new Match(claim, found.get(0));
  }

  /**
   * A claim found for a row.
   *
   * @param claim claim
   * @param line insurer line matched by number, null when found by claim number
   */
  public record Match(Claim claim, InsurerClaim line) {}
}
