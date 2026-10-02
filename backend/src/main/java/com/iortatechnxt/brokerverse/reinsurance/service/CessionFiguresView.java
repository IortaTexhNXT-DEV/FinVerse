package com.iortatechnxt.brokerverse.reinsurance.service;

import com.iortatechnxt.brokerverse.reinsurance.domain.Cession;
import com.iortatechnxt.brokerverse.reinsurance.domain.CessionRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyReinsuranceView;
import com.iortatechnxt.brokerverse.underwriting.service.ReinsuranceFigures;
import com.iortatechnxt.brokerverse.underwriting.service.TransactionRef;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements the underwriting port {@link PolicyReinsuranceView}: for each ceded transaction, the
 * treaty premium (quota share + surplus), the facultative premium (the facultative layer, placed or
 * provisional) and the net retention (company net premium − treaty − FAC), in the policy currency.
 * Transactions not ceded are omitted (underwriting then reports everything as retained).
 */
@Service
@Transactional(readOnly = true)
public class CessionFiguresView implements PolicyReinsuranceView {

  private final CessionRepository cessions;

  /**
   * Creates the view.
   *
   * @param cessions cessions
   */
  public CessionFiguresView(CessionRepository cessions) {
    this.cessions = cessions;
  }

  @Override
  public Map<TransactionRef, ReinsuranceFigures> figures(Collection<TransactionRef> transactions) {
    Set<TransactionRef> wanted = new HashSet<>(transactions);
    List<Long> policyIds = wanted.stream().map(TransactionRef::policyId).distinct().toList();
    Map<TransactionRef, ReinsuranceFigures> out = new HashMap<>();
    if (policyIds.isEmpty()) {
      return out;
    }
    for (Cession c : cessions.findByPolicyIdIn(policyIds)) {
      TransactionRef ref = new TransactionRef(c.getPolicyId(), c.getEndorsementNo());
      if (wanted.contains(ref)) {
        out.put(ref, figures(c));
      }
    }
    return out;
  }

  /**
   * Figures of one cession.
   *
   * @param c cession
   * @return figures in the policy currency
   */
  static ReinsuranceFigures figures(Cession c) {
    BigDecimal treaty = c.premiumOf(RiLayer.QUOTA_SHARE).add(c.premiumOf(RiLayer.SURPLUS));
    BigDecimal fac = c.premiumOf(RiLayer.FAC);
    return new ReinsuranceFigures(treaty, fac, c.getOurPremium().subtract(treaty).subtract(fac));
  }
}
