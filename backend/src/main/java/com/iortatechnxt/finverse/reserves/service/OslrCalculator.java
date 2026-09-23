package com.iortatechnxt.finverse.reserves.service;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.insurance.OutstandingClaim;
import com.iortatechnxt.finverse.reserves.domain.ReserveKey;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PolicySnapshot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outstanding loss reserve (PGIBR080): Σ outstanding (approved estimate − paid) of open claims at
 * the valuation date, base currency, grouped by branch, line of business, product and channel of
 * the policy; the reinsurers' share comes from {@code ClaimReinsuranceView}. Both come from the
 * insurance shared kernel and are zero when the claims / reinsurance modules are not deployed.
 */
@Component
@Transactional(readOnly = true)
public class OslrCalculator {

  private static final String UNKNOWN = "UNKNOWN";

  private final ReservePorts ports;
  private final PolicyQueryService policies;

  /**
   * Creates the calculator.
   *
   * @param ports claims and reinsurance views
   * @param policies policy headers (product, channel)
   */
  public OslrCalculator(ReservePorts ports, PolicyQueryService policies) {
    this.ports = ports;
    this.policies = policies;
  }

  /**
   * OSLR by reporting unit.
   *
   * @param companyId company
   * @param asOf valuation date
   * @return gross and reinsurers' share by key
   */
  public Map<ReserveKey, GrossRi> calculate(Long companyId, LocalDate asOf) {
    List<OutstandingClaim> open = ports.outstanding(companyId, asOf);
    Map<Long, BigDecimal> ceded =
        open.isEmpty() ? Map.of() : ports.reinsuranceShare(companyId, asOf);
    Map<Long, PolicySnapshot> headers = new HashMap<>();
    Map<ReserveKey, GrossRi> out = new HashMap<>();
    for (OutstandingClaim c : open) {
      PolicySnapshot p =
          c.policyId() == null ? null : headers.computeIfAbsent(c.policyId(), policies::get);
      ReserveKey key =
          new ReserveKey(
              c.branchId(),
              c.lineOfBusiness(),
              p == null ? UNKNOWN : p.productCode(),
              p == null ? UNKNOWN : p.sourceType().name());
      GrossRi amount =
          new GrossRi(
              Money.round(c.baseOutstanding()),
              Money.round(ceded.getOrDefault(c.claimId(), BigDecimal.ZERO)));
      out.merge(key, amount, GrossRi::plus);
    }
    return out;
  }
}
