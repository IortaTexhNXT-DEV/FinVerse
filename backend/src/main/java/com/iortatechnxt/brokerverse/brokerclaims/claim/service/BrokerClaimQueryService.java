package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Claim reads shared by the Claims services (BRCLM.041/043; FR-CM-002 R3): a claim is found only
 * within its company, so a reference of another company answers "not found"; a permanently closed
 * claim refuses changes other than insurer updates, diary entries and reopen (design 5.1).
 */
@Service
@Transactional(readOnly = true)
public class BrokerClaimQueryService {

  private static final int MAX_HITS = 50;

  private final BrokerClaimRepository claims;

  /**
   * Creates the service.
   *
   * @param claims claims
   */
  public BrokerClaimQueryService(BrokerClaimRepository claims) {
    this.claims = claims;
  }

  /**
   * A claim of a company.
   *
   * @param companyId company
   * @param id claim id
   * @return the claim
   */
  public Claim require(Long companyId, Long id) {
    return claims
        .findByIdAndCompanyId(id, companyId)
        .orElseThrow(() -> new ResourceNotFoundException(ClaimCodes.ENTITY_TYPE, id));
  }

  /**
   * A claim of a company that is not permanently closed.
   *
   * @param companyId company
   * @param id claim id
   * @return the claim
   */
  public Claim requireOpen(Long companyId, Long id) {
    Claim claim = require(companyId, id);
    if (claim.isClosed()) {
      throw new BusinessRuleException(
          "BCL_CLAIM_CLOSED", "Claim " + claim.getClaimNo() + " is closed and cannot be changed");
    }
    return claim;
  }

  /**
   * A claim by its number.
   *
   * @param companyId company
   * @param claimNo claim number
   * @return the claim
   */
  public Claim requireByNo(Long companyId, String claimNo) {
    String number = claimNo == null ? "" : claimNo.strip();
    return claims
        .findByCompanyIdAndClaimNo(companyId, number)
        .orElseThrow(() -> new ResourceNotFoundException(ClaimCodes.ENTITY_TYPE, number));
  }

  /**
   * Claims of a cover, all policy years, newest first.
   *
   * @param companyId company
   * @param arn account reference number
   * @return claims
   */
  public List<Claim> ofCover(Long companyId, String arn) {
    return claims.findByCompanyIdAndCoverArnOrderByIdDesc(companyId, arn);
  }

  /**
   * Claims whose number, ARN, policy number or assured contains a text, newest first.
   *
   * @param companyId company
   * @param text text
   * @return at most 50 claims
   */
  public List<Claim> search(Long companyId, String text) {
    String term = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
    return claims.search(companyId, "%" + term + "%", PageRequest.of(0, MAX_HITS));
  }
}
