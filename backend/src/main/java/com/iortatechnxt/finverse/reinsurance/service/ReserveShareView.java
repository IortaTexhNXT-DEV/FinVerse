package com.iortatechnxt.finverse.reinsurance.service;

import com.iortatechnxt.finverse.insurance.ClaimReinsuranceView;
import com.iortatechnxt.finverse.reinsurance.domain.ClaimAmount;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimShareRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the kernel port {@link ClaimReinsuranceView}: the reinsurers' share of each claim's
 * outstanding reserve at a date, the sum of the shares of the reserve changes dated on or before it
 * (base currency). Claims without a share are omitted.
 */
@Service
@Transactional(readOnly = true)
public class ReserveShareView implements ClaimReinsuranceView {

  private final RiClaimShareRepository shares;

  /**
   * Creates the view.
   *
   * @param shares claim shares
   */
  public ReserveShareView(RiClaimShareRepository shares) {
    this.shares = shares;
  }

  @Override
  public Map<Long, BigDecimal> reinsuranceShareOfOutstanding(Long companyId, LocalDate asOf) {
    Map<Long, BigDecimal> out = new LinkedHashMap<>();
    for (ClaimAmount a : shares.reserveShares(companyId, asOf)) {
      if (a.amount().signum() != 0) {
        out.put(a.claimId(), a.amount());
      }
    }
    return out;
  }
}
