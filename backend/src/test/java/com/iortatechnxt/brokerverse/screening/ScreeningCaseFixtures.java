package com.iortatechnxt.brokerverse.screening;

import static com.iortatechnxt.brokerverse.screening.ScreeningMatchingFixtures.person;
import static com.iortatechnxt.brokerverse.screening.ScreeningMatchingFixtures.word;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseDocument.DocumentMeta;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseStatus;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseTypes;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseAssignmentService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseDocumentService;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseReviewService;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.time.LocalDate;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** Screening cases for the case tests: a listed client's NAME_MATCH case and its investigation. */
@Component
public class ScreeningCaseFixtures {

  /** The investigator the tests work as. */
  public static final String INVESTIGATOR = "investigator";

  private final ScreeningMatchingFixtures matching;
  private final ScreeningCaseRepository cases;
  private final CaseAssignmentService assignments;
  private final CaseReviewService reviews;
  private final CaseDocumentService documents;
  private final AsUser as;

  ScreeningCaseFixtures(
      ScreeningMatchingFixtures matching,
      ScreeningCaseRepository cases,
      CaseAssignmentService assignments,
      CaseReviewService reviews,
      CaseDocumentService documents,
      AsUser as) {
    this.matching = matching;
    this.cases = cases;
    this.assignments = assignments;
    this.reviews = reviews;
    this.documents = documents;
    this.as = as;
  }

  <T> T as(String user, Supplier<T> action) {
    return as.run(user, action);
  }

  /**
   * A new prospect whose name is on the internal list: registration screens it and opens its
   * NAME_MATCH case in INVESTIGATION.
   *
   * @return the client
   */
  Client listedClient() {
    String first = word();
    String last = word();
    matching.listed(first + " " + last, null);
    return matching.prospect(matching.demoCompany(), person(first, last, null));
  }

  /**
   * The open NAME_MATCH case of a client.
   *
   * @param client the client
   * @return the case
   */
  ScreeningCase caseOf(Client client) {
    return cases
        .findByClientIdAndCaseTypeAndStatus(client.getId(), CaseTypes.NAME_MATCH, CaseStatus.OPEN)
        .orElseThrow(() -> new AssertionError("no open case of " + client.getCode()));
  }

  /**
   * A new NAME_MATCH case assigned to {@value #INVESTIGATOR}.
   *
   * @return the case
   */
  ScreeningCase investigatedCase() {
    return toInvestigator(caseOf(listedClient()));
  }

  /**
   * A case assigned to {@value #INVESTIGATOR} (re-assigned by the UCC when needed).
   *
   * @param c the case
   * @return the case
   */
  ScreeningCase toInvestigator(ScreeningCase c) {
    if (!CurrentUser.sameUser(c.getAssignee(), INVESTIGATOR)) {
      as("ucc", () -> assignments.reassign(c.getId(), INVESTIGATOR, "WORKLOAD", "Test assignment"));
    }
    return reload(c);
  }

  /**
   * A case reloaded.
   *
   * @param c the case
   * @return the case
   */
  ScreeningCase reload(ScreeningCase c) {
    return cases.findById(c.getId()).orElseThrow();
  }

  /**
   * Completes the review of a case as the investigator.
   *
   * @param c the case
   */
  void completeReview(ScreeningCase c) {
    completeReviewAs(c, INVESTIGATOR);
  }

  /**
   * Completes the review of a case as a user.
   *
   * @param c the case
   * @param user the user
   */
  void completeReviewAs(ScreeningCase c, String user) {
    as(
        user,
        () ->
            reviews.save(
                reload(c),
                Map.of(
                    "IDENTITY_VERIFIED", "true",
                    "SOURCE_OF_FUNDS", "Salary from employment",
                    "MATCH_ASSESSMENT",
                        "Same name as the internal entry; no birth date on the list",
                    "PROPOSED_RATING", "HIGH",
                    "RECOMMENDATION", "Escalate for enhanced review")));
  }

  /**
   * Uploads the KYC form of a case as the investigator.
   *
   * @param c the case
   */
  void uploadKycForm(ScreeningCase c) {
    uploadKycFormAs(c, INVESTIGATOR);
  }

  /**
   * Uploads the KYC form of a case as a user.
   *
   * @param c the case
   * @param user the user
   */
  void uploadKycFormAs(ScreeningCase c, String user) {
    as(
        user,
        () ->
            documents.upload(
                reload(c),
                new DocumentMeta("KYC_REVIEW", "KYC_FORM", LocalDate.now().minusDays(1), "Branch"),
                "kyc form.pdf",
                ScreeningMatchingFixtures.PDF));
  }
}
