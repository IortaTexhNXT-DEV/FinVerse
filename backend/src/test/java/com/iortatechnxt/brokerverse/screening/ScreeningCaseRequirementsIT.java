package com.iortatechnxt.brokerverse.screening;

import static com.iortatechnxt.brokerverse.screening.ScreeningCaseFixtures.INVESTIGATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocument;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocument.DocumentMeta;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStage;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseDocumentService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseReviewService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseSubmissionService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseTimeline;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseValidator;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseValidator.Decision;
import com.iortatechnxt.brokerverse.screening.config.service.ReviewTemplate.Field;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The acceptance criteria of the investigation step of a screening case that the end-to-end case
 * (ScreeningCasesIT) does not show: FR-SS-050 (mandatory fields of the guided review template are
 * marked and a blank one stops the submission), FR-SS-051 (only INVESTIGATION dispositions are
 * accepted) and FR-SS-052 (the document name follows the naming pattern; a document without date
 * received is refused; the upload is on the timeline).
 */
@IntegrationTest
class ScreeningCaseRequirementsIT {

  @Autowired private ScreeningCaseFixtures fx;
  @Autowired private CaseReviewService reviews;
  @Autowired private CaseDocumentService documents;
  @Autowired private CaseSubmissionService submissions;
  @Autowired private CaseValidator validator;
  @Autowired private CaseTimeline timeline;
  @Autowired private JdbcTemplate jdbc;

  private List<CaseEventType> events(ScreeningCase c) {
    return timeline.of(c.getId()).stream().map(CaseEvent::getEvent).toList();
  }

  @Test
  void theMandatoryFieldsOfTheReviewAreMarkedAndABlankOneStopsTheSubmission() {
    // FR-SS-050
    ScreeningCase c = fx.investigatedCase();
    fx.as(INVESTIGATOR, () -> reviews.save(fx.reload(c), Map.of()));
    List<Field> fields = reviews.form(fx.reload(c)).orElseThrow().template().fields();
    assertThat(fields).anyMatch(Field::mandatory);
    String mandatory = fields.stream().filter(Field::mandatory).findFirst().orElseThrow().label();
    assertThat(reviews.missing(fx.reload(c))).containsValue(mandatory + " is required");
    fx.uploadKycForm(c);
    Decision decision = new Decision("TRUE_MATCH_REVIEW", "Escalate", true);
    assertThatThrownBy(() -> fx.as(INVESTIGATOR, () -> submissions.submit(c.getId(), decision)))
        .isInstanceOf(RuntimeException.class);
    assertThat(events(c)).contains(CaseEventType.VALIDATION_FAILED);
    assertThat(fx.reload(c).getStage()).isEqualTo(CaseStage.INVESTIGATION);
  }

  @Test
  void onlyTheDispositionsOfTheInvestigationStageAreAccepted() {
    // FR-SS-051
    String approverDisposition =
        jdbc.queryForObject(
            "select code from lov_value where type_code = 'SCR_DISPOSITION'"
                + " and parent_code <> 'INVESTIGATION' order by code limit 1",
            String.class);
    assertThat(validator.allowed(CaseStage.INVESTIGATION, "TRUE_MATCH_REVIEW")).isTrue();
    assertThat(validator.allowed(CaseStage.INVESTIGATION, approverDisposition)).isFalse();
    ScreeningCase c = fx.investigatedCase();
    fx.completeReview(c);
    fx.uploadKycForm(c);
    assertThatThrownBy(
            () ->
                fx.as(
                    INVESTIGATOR,
                    () ->
                        submissions.submit(
                            c.getId(), new Decision(approverDisposition, "Wrong stage", false))))
        .hasMessageContaining("is not a disposition of stage INVESTIGATION");
    assertThat(fx.reload(c).getStage()).isEqualTo(CaseStage.INVESTIGATION);
  }

  @Test
  void aDocumentIsNamedByThePatternAndNeedsItsDateReceived() {
    // FR-SS-052
    ScreeningCase c = fx.investigatedCase();
    LocalDate received = BusinessClock.today(Clock.systemUTC()).minusDays(1);
    assertThatThrownBy(
            () ->
                fx.as(
                    INVESTIGATOR,
                    () ->
                        documents.upload(
                            fx.reload(c),
                            new DocumentMeta("KYC_REVIEW", "KYC_FORM", null, "Branch"),
                            "kyc form.pdf",
                            ScreeningMatchingFixtures.PDF)))
        .hasMessage("Enter the date received of the document");
    CaseDocument stored =
        fx.as(
            INVESTIGATOR,
            () ->
                documents.upload(
                    fx.reload(c),
                    new DocumentMeta("KYC_REVIEW", "KYC_FORM", received, "Branch"),
                    "kyc form.pdf",
                    ScreeningMatchingFixtures.PDF));
    assertThat(stored.getNominatedName())
        .startsWith("KYC-REVIEW_")
        .contains("_" + received.format(DateTimeFormatter.BASIC_ISO_DATE) + "_")
        .contains("_KYC-FORM_1");
    assertThat(events(c)).contains(CaseEventType.DOCUMENT_ADDED);
  }
}
