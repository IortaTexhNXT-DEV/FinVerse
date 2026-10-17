package com.iortatechnxt.brokerverse.renewal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotClient;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot.SnapshotMortgage;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.letter.service.LetterContent;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.submitted.SubmittedHandOffRecord;
import com.iortatechnxt.brokerverse.renewal.submitted.SubmittedHandOffRecordRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Templates of the renewal letters of submitted policies (wave R3): FFY RA and SFU follow-up. */
class SubmittedLetterTemplateTest {

  private final SubmittedHandOffRecordRepository handOffs =
      mock(SubmittedHandOffRecordRepository.class);
  private final LetterContent content =
      new LetterContent(
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          handOffs,
          new com.iortatechnxt.brokerverse.renewal.letter.service.AnnexTemplates(
              mock(com.iortatechnxt.brokerverse.account.domain.AccountRepository.class)));

  private RenewalCandidate candidate(CandidateSource source, boolean mortgaged) {
    RenewalCandidate c =
        new RenewalCandidate(
            1L,
            "RNW-1",
            new RenewalCandidate.Origin(source, "SBM-1", null, null, null),
            new CandidateSnapshot(
                "POL-1",
                null,
                null,
                "PN-1",
                new SnapshotClient(null, null, "Client", "Client", null),
                null,
                null,
                "INS-1",
                new SnapshotMortgage(mortgaged, mortgaged ? "BDO Unibank" : null),
                false,
                LocalDate.of(2025, 11, 1),
                LocalDate.of(2026, 11, 1),
                null,
                null,
                null),
            null);
    ReflectionTestUtils.setField(c, "id", 9L);
    return c;
  }

  @Test
  void aFreeFirstYearSubmittedPolicyGetsTheFfyAdvice() {
    when(handOffs.findByCandidateId(9L))
        .thenReturn(
            Optional.of(
                new SubmittedHandOffRecord(
                    1L,
                    9L,
                    "SBM-1",
                    new SubmittedHandOffRecord.Terms("FFY", null, null, null, null, false))));
    RenewalCandidate c = candidate(CandidateSource.SUBMITTED_POLICY, false);
    assertThat(content.templateOf(c, LetterType.RA, RaNotice.FIRST))
        .isEqualTo(RenewalCodes.TEMPLATE_RA_FFY);
    assertThat(content.templateOf(c, LetterType.RA, RaNotice.SECOND))
        .isEqualTo(RenewalCodes.TEMPLATE_RA_SECOND);
  }

  @Test
  void aMortgagedSubmittedPolicyGetsTheFollowUpInsteadOfTheReminder() {
    when(handOffs.findByCandidateId(9L)).thenReturn(Optional.empty());
    assertThat(
            content.templateOf(
                candidate(CandidateSource.SUBMITTED_POLICY, true), LetterType.NRNS_REMINDER, null))
        .isEqualTo(RenewalCodes.TEMPLATE_SFU);
    assertThat(
            content.templateOf(
                candidate(CandidateSource.BIBS_INVOICE, true), LetterType.NRNS_REMINDER, null))
        .isEqualTo(RenewalCodes.TEMPLATE_NRNS);
    assertThat(
            content.templateOf(
                candidate(CandidateSource.SUBMITTED_POLICY, false), LetterType.NRNS_REMINDER, null))
        .isEqualTo(RenewalCodes.TEMPLATE_NRNS);
  }
}
