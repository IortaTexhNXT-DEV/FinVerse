package com.iortatechnxt.brokerverse.renewal.check.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.service.KycReviewPolicy;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotClient;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotMortgage;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The messages of the renewal checks as the Checks tab shows them: dates and amounts formatted. */
class CheckMessageWordsTest {

  private static final LocalDate TODAY = LocalDate.of(2027, 6, 1);

  private final RenewalParameters parameters = mock(RenewalParameters.class);

  private static RenewalCandidate candidate() {
    return new RenewalCandidate(
        1L,
        "RNW-2027-000001",
        new RenewalCandidate.Origin(CandidateSource.BIBS_INVOICE, "BI-1", null, null, null),
        new CandidateSnapshot(
            "POL-1",
            null,
            null,
            null,
            new SnapshotClient(5L, "CL-1", "Pacific Harbor Logistics Inc.", null, null),
            null,
            null,
            "INS-MGIC",
            new SnapshotMortgage(false, null),
            false,
            TODAY.minusYears(1),
            TODAY.plusMonths(3),
            null,
            null,
            null),
        null);
  }

  @Test
  void theOutstandingPremiumReadsAsAnAmount() {
    when(parameters.outstandingThreshold()).thenReturn(BigDecimal.ZERO);
    OpsInvoice invoice = mock(OpsInvoice.class);
    when(invoice.premiumBalance()).thenReturn(new BigDecimal("28281.25"));
    CheckContext context = mock(CheckContext.class);
    when(context.bibs()).thenReturn(true);
    when(context.family()).thenReturn(List.of(invoice));

    RenewalCheck.Verdict verdict = new OutstandingPremiumCheck(parameters).evaluate(context);

    assertThat(verdict.message()).isEqualTo("Outstanding premium 28,281.25");
    assertThat(verdict.detail()).isEqualTo("28281.25");
  }

  @Test
  void theKycReviewDateReadsAsTheScreensShowDates() {
    ClientRepository clients = mock(ClientRepository.class);
    KycReviewPolicy kyc = mock(KycReviewPolicy.class);
    Client client = mock(Client.class);
    when(client.getKycReviewDue()).thenReturn(LocalDate.of(2029, 6, 28));
    when(clients.findById(5L)).thenReturn(Optional.of(client));
    when(parameters.kycApplies(any())).thenReturn(true);
    when(kyc.dueHorizon(TODAY)).thenReturn(TODAY.plusMonths(6));
    CheckContext context = mock(CheckContext.class);
    when(context.candidate()).thenReturn(candidate());
    when(context.today()).thenReturn(TODAY);

    RenewalCheck.Verdict verdict = new KycDueCheck(clients, kyc, parameters).evaluate(context);

    assertThat(verdict.message()).isEqualTo("KYC review due on 28-Jun-2029");
  }
}
