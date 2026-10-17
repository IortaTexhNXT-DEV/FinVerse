package com.iortatechnxt.brokerverse.issuance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import com.iortatechnxt.brokerverse.issuance.domain.EpolicyStatus;
import com.iortatechnxt.brokerverse.issuance.domain.MatchMethod;
import com.iortatechnxt.brokerverse.issuance.domain.UploadBatch;
import com.iortatechnxt.brokerverse.issuance.domain.UploadItem;
import com.iortatechnxt.brokerverse.issuance.domain.UploadStatus;
import com.iortatechnxt.brokerverse.issuance.service.DocumentTriggerService;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService.ReceivedFile;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyUploadConfirmation;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyUploadService;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyUploadService.IncomingFile;
import com.iortatechnxt.brokerverse.issuance.service.InsuranceAdviceService;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceQueryService;
import com.iortatechnxt.brokerverse.issuance.service.IssuanceTab;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PlacementTestData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/** E-policy receipt, extraction review and policy number update (BRNB.073/074/104/105). */
@IntegrationTest
class EpolicyIT {

  @Autowired private EpolicyService epolicies;
  @Autowired private EpolicyUploadService uploads;
  @Autowired private EpolicyUploadConfirmation uploadConfirmation;
  @Autowired private DocumentTriggerService triggers;
  @Autowired private InsuranceAdviceService advices;
  @Autowired private IssuanceQueryService queries;
  @Autowired private AccountQueryService accounts;
  @Autowired private PlacementTestData fx;
  @Autowired private AsUser as;

  private Epolicy receive(String fileName, byte[] pdf, String arn, String policyNo) {
    return as.run(
        "proc",
        () -> epolicies.receive(new ReceivedFile(fx.company(), fileName, pdf, arn, policyNo)));
  }

  @Test
  void aReceivedEpolicyIsMatchedExtractedReviewedAndUpdatesThePolicy() {
    String arn = fx.placed(fx.fire());
    String number = "MGIC-FI-" + PlacementTestData.token();
    Epolicy received = receive("policy.pdf", EpolicyPdf.policy(arn, number), null, null);
    assertThat(received.getMatchMethod()).isEqualTo(MatchMethod.CONTENT);
    assertThat(received.getStatus()).isEqualTo(EpolicyStatus.REVIEW);
    assertThat(received.getExtractedPolicyNumberList()).containsExactly(number);
    assertThat(received.getExtractedPeriodFrom()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(received.getExtractedPremium()).isEqualByComparingTo("12345.67");
    assertThat(received.getFileName()).startsWith(arn + "_EPOLICY_");
    assertThat(triggers.rules()).isNotEmpty();
    assertThat(queries.workbench(fx.company(), IssuanceTab.REVIEW, arn, PageRequest.of(0, 5)))
        .hasSize(1);

    EpolicyService.Review review = epolicies.review(received.getId());
    assertThat(review.account().getArn()).isEqualTo(arn);
    Epolicy again = as.run("proc", () -> epolicies.reextract(received.getId()));
    assertThat(again.getStatus()).isEqualTo(EpolicyStatus.REVIEW);
    assertThatThrownBy(
            () ->
                as.run("proc", () -> epolicies.confirm(received.getId(), List.of("A", "B"), null)))
        .extracting("code")
        .isEqualTo("POLICY_NUMBERS_MISMATCH");
    Epolicy confirmed =
        as.run(
            "proc",
            () -> epolicies.confirm(received.getId(), List.of(number), LocalDate.of(2026, 9, 30)));
    assertThat(confirmed.getStatus()).isEqualTo(EpolicyStatus.CONFIRMED);
    Account issued = accounts.requireByArn(arn);
    assertThat(issued.getStatus()).isEqualTo(AccountStatus.POLICY_ISSUED);
    assertThat(issued.getPolicyNumbers()).containsExactly(number);
    assertThat(advices.ofAccount(arn))
        .singleElement()
        .satisfies(ia -> assertThat(ia.getPolicyNumbers()).isEqualTo(number));
    assertThat(queries.policyFor(arn).epolicies()).hasSize(1);
    assertThatThrownBy(
            () -> as.run("proc", () -> epolicies.confirm(received.getId(), List.of(number), null)))
        .extracting("code")
        .isEqualTo("EPOLICY_REVIEWED");
    assertThat(
            queries.workbench(
                fx.company(), IssuanceTab.READY_TO_DISPATCH, arn, PageRequest.of(0, 5)))
        .hasSize(1);

    Epolicy byPolicy = receive("endorsement.pdf", EpolicyPdf.of("Copy"), null, number);
    assertThat(byPolicy.getMatchMethod()).isEqualTo(MatchMethod.POLICY_NUMBER);
    assertThat(byPolicy.getArn()).isEqualTo(arn);
  }

  @Test
  void filesAreMatchedByArnPolicyNumberOrFileNameAndMayBeRejected() {
    String arn = fx.placed(fx.liability());
    assertThatThrownBy(() -> receive("unknown.pdf", EpolicyPdf.of("no reference"), null, null))
        .extracting("code")
        .isEqualTo("EPOLICY_ACCOUNT_NOT_FOUND");
    String awaiting = fx.awaitingPayment(fx.liability()).getArn();
    assertThatThrownBy(() -> receive("x.pdf", EpolicyPdf.of("x"), awaiting, null))
        .extracting("code")
        .isEqualTo("EPOLICY_ACCOUNT_STATUS");
    Epolicy byName = receive(arn + "_policy.pdf", EpolicyPdf.of("Scanned copy"), null, null);
    assertThat(byName.getMatchMethod()).isEqualTo(MatchMethod.FILE_NAME);
    assertThat(byName.getExtractionNote()).contains("policy number not found");
    Epolicy chosen = receive("copy.pdf", EpolicyPdf.of("copy"), arn, null);
    assertThat(chosen.getMatchMethod()).isEqualTo(MatchMethod.MANUAL);
    assertThatThrownBy(() -> as.run("proc", () -> epolicies.reject(byName.getId(), "NOPE", null)))
        .isInstanceOf(RuntimeException.class);
    Epolicy rejected =
        as.run("proc", () -> epolicies.reject(byName.getId(), "WRONG_DOCUMENT", "Draft copy"));
    assertThat(rejected.getStatus()).isEqualTo(EpolicyStatus.REJECTED);
    assertThat(rejected.getRejectReason()).isEqualTo("WRONG_DOCUMENT");
    assertThatThrownBy(() -> receive("p.pdf", EpolicyPdf.of("p"), null, "NOT-A-POLICY"))
        .extracting("code")
        .isEqualTo("EPOLICY_ACCOUNT_NOT_FOUND");
  }

  @Test
  void manyEpoliciesAreUploadedReviewedAndConfirmed() {
    String first = fx.placed(fx.motor());
    String second = fx.placed(fx.motor());
    UploadBatch batch =
        as.run(
            "proc",
            () ->
                uploads.upload(
                    fx.company(),
                    List.of(
                        new IncomingFile(first + ".pdf", EpolicyPdf.of("Policy No: B1-" + first)),
                        new IncomingFile("scan.pdf", EpolicyPdf.of("unreadable scan")))));
    assertThat(batch.getStatus()).isEqualTo(UploadStatus.REVIEW);
    UploadItem matched = batch.getItems().get(0);
    UploadItem unmatched = batch.getItems().get(1);
    assertThat(matched.getArn()).isEqualTo(first);
    assertThat(matched.isIncluded()).isTrue();
    assertThat(unmatched.getArn()).isNull();
    assertThat(unmatched.isIncluded()).isFalse();
    as.run("proc", () -> uploads.choose(batch.getId(), unmatched.getId(), second, true));
    UploadBatch done = as.run("proc", () -> uploadConfirmation.confirm(batch.getId()));
    assertThat(done.getStatus()).isEqualTo(UploadStatus.CONFIRMED);
    assertThat(done.getItems()).allMatch(i -> i.getEpolicyId() != null);
    assertThat(queries.policyFor(second).epolicies()).hasSize(1);
    assertThatThrownBy(() -> as.run("proc", () -> uploads.discard(batch.getId())))
        .extracting("code")
        .isEqualTo("EPOLICY_UPLOAD_CLOSED");
    UploadBatch other =
        as.run(
            "proc",
            () ->
                uploads.upload(
                    fx.company(), List.of(new IncomingFile("a.pdf", EpolicyPdf.of("a")))));
    assertThat(as.run("proc", () -> uploads.discard(other.getId())).getStatus())
        .isEqualTo(UploadStatus.DISCARDED);
    assertThatThrownBy(() -> as.run("proc", () -> uploads.upload(fx.company(), List.of())))
        .extracting("code")
        .isEqualTo("EPOLICY_FILE_COUNT");
  }
}
