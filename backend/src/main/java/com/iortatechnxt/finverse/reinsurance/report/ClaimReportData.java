package com.iortatechnxt.finverse.reinsurance.report;

import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.reinsurance.domain.Cession;
import com.iortatechnxt.finverse.reinsurance.domain.CessionLine;
import com.iortatechnxt.finverse.reinsurance.domain.CessionRepository;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimMovement;
import com.iortatechnxt.finverse.reinsurance.domain.RiClaimShare;
import com.iortatechnxt.finverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Data helpers of the reinsurance claims reports: the cession behind each claim movement, policy
 * numbers and the split of the reinsurers' shares by layer.
 */
@Component
public class ClaimReportData {

  private final CessionRepository cessions;
  private final PolicyQueryService policies;

  /**
   * Creates the helper.
   *
   * @param cessions cessions
   * @param policies policy numbers of policies without cession
   */
  public ClaimReportData(CessionRepository cessions, PolicyQueryService policies) {
    this.cessions = cessions;
    this.policies = policies;
  }

  /**
   * Cessions whose percentages apply to the movements.
   *
   * @param movements movements
   * @return cessions by id
   */
  public Map<Long, Cession> cessionsOf(Collection<RiClaimMovement> movements) {
    List<Long> ids =
        movements.stream()
            .map(RiClaimMovement::getCessionId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    // A mutable map: movements without a cession look up a null id, which Map.of() rejects.
    Map<Long, Cession> out = new HashMap<>();
    if (!ids.isEmpty()) {
      cessions.findAllById(ids).forEach(c -> out.put(c.getId(), c));
    }
    return out;
  }

  /**
   * Policy numbers of the movements (from the cession, else from underwriting).
   *
   * @param movements movements
   * @param byId cessions by id
   * @return policy number by policy id
   */
  public Map<Long, String> policyNumbers(
      Collection<RiClaimMovement> movements, Map<Long, Cession> byId) {
    Map<Long, String> out = new HashMap<>();
    for (RiClaimMovement m : movements) {
      Cession c = byId.get(m.getCessionId());
      if (c != null) {
        out.putIfAbsent(m.getPolicyId(), c.getPolicyNo());
      }
    }
    movements.stream()
        .map(RiClaimMovement::getPolicyId)
        .distinct()
        .filter(id -> !out.containsKey(id))
        .forEach(id -> out.put(id, policies.get(id).policyNo()));
    return out;
  }

  /**
   * Reinsurers' shares by layer.
   *
   * @param shares shares
   * @return base amount by layer (every layer present)
   */
  public static Map<RiLayer, BigDecimal> byLayer(Collection<RiClaimShare> shares) {
    Map<RiLayer, BigDecimal> out = new EnumMap<>(RiLayer.class);
    for (RiLayer l : RiLayer.values()) {
      out.put(l, Money.zero());
    }
    shares.forEach(s -> out.merge(s.getLayer(), s.getBaseAmount(), BigDecimal::add));
    return out;
  }

  /**
   * Description of the first risk of a cession.
   *
   * @param c cession, may be null
   * @return risk description, empty when unknown
   */
  public static String firstRisk(Cession c) {
    if (c == null || c.getLines().isEmpty()) {
      return "";
    }
    CessionLine l = c.getLines().get(0);
    return l.getRiskLineNo() + " " + l.getRiskDescription();
  }

  /**
   * Sum insured at 100 % of a cession (base currency).
   *
   * @param c cession, may be null
   * @return sum insured
   */
  public static BigDecimal fullSi(Cession c) {
    return c == null ? Money.zero() : c.toBase(RiReportSupport.full(c.getOurSi(), c.getSharePct()));
  }

  /**
   * Company share % of a cession.
   *
   * @param c cession, may be null
   * @return share %, 100 when unknown
   */
  public static BigDecimal sharePct(Cession c) {
    return c == null ? BigDecimal.valueOf(100) : c.getSharePct();
  }
}
