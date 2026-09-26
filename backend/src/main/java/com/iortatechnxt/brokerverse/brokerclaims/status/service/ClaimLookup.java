package com.iortatechnxt.brokerverse.brokerclaims.status.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds the claim of a request within its company (FR-CL-002 R3: a claim of another company is
 * "not found") and guards the closed claims (design 5.1: a CLOSED claim accepts only diary entries,
 * insurer updates and reopen).
 */
@Service
@Transactional(readOnly = true)
public class ClaimLookup {

  private final BrokerClaimRepository claims;

  /**
   * Creates the lookup.
   *
   * @param claims claims
   */
  public ClaimLookup(BrokerClaimRepository claims) {
    this.claims = claims;
  }

  /**
   * A claim of a company.
   *
   * @param companyId company
   * @param claimId claim
   * @return the claim
   */
  public Claim require(Long companyId, Long claimId) {
    return claims
        .findByIdAndCompanyId(claimId, companyId)
        .orElseThrow(() -> new ResourceNotFoundException(ClaimCodes.ENTITY_TYPE, claimId));
  }

  /**
   * Refuses a change on a permanently closed claim.
   *
   * @param claim claim
   * @param what what is changed, e.g. "the status" (message: "Reopen it before changing ...")
   */
  public static void requireOpen(Claim claim, String what) {
    if (claim.isClosed()) {
      throw new BusinessRuleException(
          "BCL_CLAIM_CLOSED",
          "Claim " + claim.getClaimNo() + " is closed. Reopen it before changing " + what);
    }
  }
}
