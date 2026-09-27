package com.iortatechnxt.brokerverse.renewal.check.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.service.KycReviewPolicy;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * {@code KYC_DUE} (BRRN.028; FR-RN-026): the client's KYC review falls within the due window
 * ({@code KYC_DUE_WINDOW_DAYS}) in the segments of {@code RNW_KYC_SEGMENTS}. Information only: it
 * sets the KYC due chip with the time first identified and never blocks, stops or reroutes the
 * renewal.
 */
@Component
public class KycDueCheck implements RenewalCheck {

  /** Check code. */
  public static final String CODE = "KYC_DUE";

  private final ClientRepository clients;
  private final KycReviewPolicy kyc;
  private final RenewalParameters parameters;

  /**
   * Creates the check.
   *
   * @param clients clients
   * @param kyc KYC review window
   * @param parameters renewal parameters
   */
  public KycDueCheck(ClientRepository clients, KycReviewPolicy kyc, RenewalParameters parameters) {
    this.clients = clients;
    this.kyc = kyc;
    this.parameters = parameters;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public Verdict evaluate(CheckContext context) {
    CandidateSnapshot s = context.candidate().getSnapshot();
    Long clientId = s.client() == null ? null : s.client().clientId();
    String segment = s.product() == null ? null : s.product().segment();
    if (clientId == null || !parameters.kycApplies(segment)) {
      return Verdict.notApplicable("KYC due flag not applicable");
    }
    LocalDate due = clients.findById(clientId).map(Client::getKycReviewDue).orElse(null);
    if (due != null && !due.isAfter(kyc.dueHorizon(context.today()))) {
      return Verdict.info("KYC review due on " + due);
    }
    return Verdict.pass(due == null ? "No KYC review date" : "KYC review due on " + due);
  }
}
