package com.iortatechnxt.brokerverse.screening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.screening.cases.service.CommitteeRule;
import com.iortatechnxt.brokerverse.screening.config.domain.StrFormat;
import com.iortatechnxt.brokerverse.screening.config.service.StrLayout;
import com.iortatechnxt.brokerverse.screening.str.service.StrFileWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pure rules of the case wave: the AML Committee decision rule (SNSRP-704, FR-SS-064 R2) and the
 * STR extraction file of the STR layout (SNSRP-706, FR-SS-017 / 071).
 */
class ScreeningCaseLogicTest {

  private static final String APPROVE = "APPROVE_STR";
  private static final String NO_STR = "NO_STR";
  private static final String RETURN = "COMMITTEE_RETURN";

  @Test
  void majorityOfFiveNeedsThreeSameVotes() {
    assertThat(CommitteeRule.decide("MAJORITY", 5, List.of())).isEmpty();
    assertThat(CommitteeRule.decide("MAJORITY", 5, List.of(APPROVE, APPROVE))).isEmpty();
    assertThat(CommitteeRule.decide("MAJORITY", 5, List.of(APPROVE, NO_STR, APPROVE, APPROVE)))
        .contains(APPROVE);
    assertThat(
            CommitteeRule.decide("majority", 5, List.of(APPROVE, APPROVE, NO_STR, NO_STR, RETURN)))
        .contains(CommitteeRule.NO_MAJORITY);
    assertThat(CommitteeRule.decide(null, 3, List.of(NO_STR, NO_STR))).contains(NO_STR);
  }

  @Test
  void anyDecidesOnTheFirstVoteAndAllNeedsUnanimity() {
    assertThat(CommitteeRule.decide("ANY", 5, List.of(RETURN))).contains(RETURN);
    assertThat(CommitteeRule.decide("ALL", 3, List.of(APPROVE, APPROVE))).isEmpty();
    assertThat(CommitteeRule.decide("ALL", 3, List.of(APPROVE, APPROVE, APPROVE)))
        .contains(APPROVE);
    assertThat(CommitteeRule.decide("ALL", 3, List.of(APPROVE, NO_STR)))
        .contains(CommitteeRule.NO_MAJORITY);
    assertThat(CommitteeRule.decide("ALL", 0, List.of(NO_STR))).contains(NO_STR);
  }

  private static StrLayout layout(StrFormat format) {
    return new StrLayout(
        null,
        format,
        ";",
        "UTF-8",
        List.of(
            new StrLayout.Column(2, "SUBJECT_NAME", null, "Subject", 10, "RIGHT", null),
            new StrLayout.Column(1, "STR_NO", null, "STR No.", 6, "LEFT", null),
            new StrLayout.Column(3, "REASON_CODE", null, "Reason", 4, null, Map.of("RSN01", "R01")),
            new StrLayout.Column(4, null, "BDOI", "Entity", 4, null, null)));
  }

  @Test
  void csvFilesFollowTheLayoutWithHeadersQuotingAndCodeMaps() {
    byte[] file =
        StrFileWriter.write(
            layout(StrFormat.CSV),
            List.of(
                Map.of(
                    "STR_NO", "S1", "SUBJECT_NAME", "Cruz; Juan \"JD\"", "REASON_CODE", "RSN01")));
    assertThat(new String(file, StandardCharsets.UTF_8))
        .isEqualTo("STR No.;Subject;Reason;Entity\r\nS1;\"Cruz; Juan \"\"JD\"\"\";R01;BDOI\r\n");
    assertThat(StrFileWriter.extension(layout(StrFormat.CSV))).isEqualTo("csv");
  }

  @Test
  void fixedWidthFilesPadAndCutEachColumn() {
    byte[] file =
        StrFileWriter.write(
            layout(StrFormat.FIXED),
            List.of(
                Map.of(
                    "STR_NO",
                    "S1",
                    "SUBJECT_NAME",
                    "A very long subject name",
                    "REASON_CODE",
                    "X")));
    assertThat(new String(file, StandardCharsets.UTF_8)).isEqualTo("    S1A very lonX   BDOI\r\n");
    assertThat(StrFileWriter.extension(layout(StrFormat.FIXED))).isEqualTo("txt");
    assertThatThrownBy(() -> StrFileWriter.write(layout(StrFormat.XML), List.of()))
        .hasMessageContaining("SQ09");
  }
}
