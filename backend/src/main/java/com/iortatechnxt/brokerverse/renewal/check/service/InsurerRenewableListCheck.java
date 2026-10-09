package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerRenewableRisk;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerRenewableRiskRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@code INSURER_RENEWABLE_LIST} (Annex BRRN.020 SC-10; FR-RN-020; CLR-RN-38): a risk that is not
 * on the renewable list of its insurer stops the continuation for review. An insurer that has not
 * given a list is not checked.
 */
@Component
public class InsurerRenewableListCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "INSURER_RENEWABLE_LIST";

  private final InsurerRenewableRiskRepository lists;

  /**
   * Creates the check.
   *
   * @param lists insurer renewable lists
   */
  public InsurerRenewableListCheck(InsurerRenewableRiskRepository lists) {
    this.lists = lists;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    CandidateSnapshot s = context.candidate().getSnapshot();
    String insurer = s.insurerCode();
    String risk = s.product() == null ? null : s.product().productCode();
    if (insurer == null || risk == null) {
      return Verdict.notApplicable("No insurer or risk code");
    }
    List<InsurerRenewableRisk> list =
        lists.findByCompanyIdAndInsurerCode(context.candidate().getCompanyId(), insurer).stream()
            .filter(r -> r.inForce(context.today()))
            .toList();
    if (list.isEmpty()) {
      return Verdict.notApplicable("The insurer has given no renewable list");
    }
    return list.stream().anyMatch(r -> r.getRiskCode().equals(risk))
        ? Verdict.pass("Risk code " + risk + " is on the insurer's renewable list")
        : Verdict.fail(
            "Risk code " + risk + " is not on the renewable list of the insurer",
            insurer + " " + risk);
  }
}
