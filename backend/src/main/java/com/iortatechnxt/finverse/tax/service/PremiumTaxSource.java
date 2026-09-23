package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.tax.domain.TaxPeriod;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PremiumTransaction;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Premium transactions of a period as tax documents, read through the public read API of
 * underwriting ({@link PolicyQueryService#approvedTransactions}).
 *
 * <p>Basis (assumption): premium taxes are reported in the period of the approval (accounting) date
 * of the policy or endorsement — the date the premium and its taxes were booked — which is also
 * when the debit note is issued. Endorsements carry their own (possibly negative) taxes;
 * cancellations and return premiums therefore reduce the period in which they are approved.
 */
@Component
public class PremiumTaxSource {

  private static final String POLICY = "POLICY";
  private static final String ENDORSEMENT = "ENDORSEMENT";

  private final PolicyQueryService policies;

  /**
   * Creates the source.
   *
   * @param policies underwriting read API
   */
  public PremiumTaxSource(PolicyQueryService policies) {
    this.policies = policies;
  }

  /**
   * Premium documents approved in a period.
   *
   * @param companyId company
   * @param period period
   * @return documents ordered by policy and endorsement
   */
  public List<PremiumDocument> documents(Long companyId, TaxPeriod period) {
    return policies.approvedTransactions(companyId, period.from(), period.to()).stream()
        .filter(t -> t.premium().isFinancial())
        .map(PremiumTaxSource::toDocument)
        .toList();
  }

  private static PremiumDocument toDocument(PremiumTransaction t) {
    PremiumBreakdown p = t.premium();
    return new PremiumDocument(
        t.ref().isOriginal() ? POLICY : ENDORSEMENT,
        t.policy().id(),
        t.documentNo(),
        t.approvalDate(),
        t.policy().customerCode(),
        t.policy().customerName(),
        t.policy().intermediaryCode(),
        t.policy().intermediaryName(),
        t.policy().businessLine(),
        t.toBase(p.getOurNetPremium()),
        t.toBase(p.getVat()),
        t.toBase(p.getDst()),
        t.toBase(p.getLgt()),
        t.toBase(p.getFst()),
        t.toBase(p.getPremiumTax()),
        t.toBase(p.getCommission()),
        t.toBase(p.getWithholdingTax()));
  }
}
