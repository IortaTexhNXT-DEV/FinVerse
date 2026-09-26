package com.iortatechnxt.brokerverse.crm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Contact;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.Identity;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import com.iortatechnxt.brokerverse.crm.service.Client360Service;
import com.iortatechnxt.brokerverse.crm.service.ClientOnboardingService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.crm.service.ClientWorkflow;
import com.iortatechnxt.brokerverse.crm.service.DuplicateCheckService;
import com.iortatechnxt.brokerverse.crm.service.DuplicateMatch;
import com.iortatechnxt.brokerverse.crm.service.DuplicateProbe;
import com.iortatechnxt.brokerverse.crm.service.KycDocumentService;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ClientOnboardingIT {

  @Autowired private CrmFixtures fx;
  @Autowired private ClientService clients;
  @Autowired private ClientOnboardingService onboarding;
  @Autowired private KycDocumentService kyc;
  @Autowired private DuplicateCheckService duplicates;
  @Autowired private Client360Service view;
  @Autowired private PartyService parties;
  @Autowired private WorkflowViewService workflow;
  @Autowired private AuditTrailService audit;
  @Autowired private AsUser as;

  @Test
  void prospectIsOnboardedThroughKycVerificationAndConfirmation() {
    Client p = fx.prospect(CrmFixtures.person());
    assertThat(p.getProspectCode()).startsWith("PR-");
    assertThat(p.getOnboardingStage()).isEqualTo(ClientWorkflow.PROSPECT);
    assertThat(stage(p)).isEqualTo(ClientWorkflow.PROSPECT);

    assertThatThrownBy(() -> as.run("ao", () -> onboarding.submitKyc(p.getId(), null)))
        .extracting("code")
        .isEqualTo("KYC_DOCUMENTS_MISSING");
    fx.uploadChecklist(p);
    assertThat(kyc.checklist(p.getId()).complete()).isTrue();
    assertThat(clients.get(p.getId()).getKycStatus()).isEqualTo(KycStatus.PENDING);

    as.run("ao", () -> onboarding.submitKyc(p.getId(), "ready"));
    assertThat(stage(p)).isEqualTo(ClientWorkflow.KYC_REVIEW);
    assertThat(clients.get(p.getId()).getOnboardingStage()).isEqualTo(ClientWorkflow.KYC_REVIEW);
    assertThatThrownBy(() -> as.run("ao", () -> onboarding.confirm(p.getId(), null)))
        .extracting("code")
        .isEqualTo("KYC_NOT_VERIFIED");
    assertThatThrownBy(() -> as.run("mkttl", () -> onboarding.submitKyc(p.getId(), null)))
        .extracting("code")
        .isEqualTo("WORKFLOW_TRANSITION_NOT_ALLOWED");

    Client verified = as.run("mkttl", () -> onboarding.verifyKyc(p.getId(), "ok"));
    assertThat(verified.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
    assertThat(verified.getKycVerifiedBy()).isEqualTo("mkttl");
    assertThat(verified.getKycReviewDue()).isEqualTo(LocalDate.now(ZoneOffset.UTC).plusMonths(36));

    Client confirmed = as.run("ao", () -> onboarding.confirm(p.getId(), null));
    assertThat(confirmed.getStatus()).isEqualTo(ClientStatus.CONFIRMED);
    assertThat(confirmed.getClientCode()).startsWith("CL-");
    assertThat(confirmed.getProspectCode()).isEqualTo(p.getProspectCode());
    assertThat(confirmed.getConfirmedBy()).isEqualTo("ao");
    assertThat(confirmed.getPartyCode()).isEqualTo(confirmed.getClientCode());
    var party =
        parties.requireActive(
            fx.company(), confirmed.getClientCode(), List.of(PartyType.INDIVIDUAL_CLIENT));
    assertThat(party.getAuthorizedBy()).isEqualTo("SYSTEM");
    assertThat(stage(p)).isEqualTo(ClientWorkflow.CONFIRMED);
    assertThat(clients.requireConfirmed(p.getId()).getId()).isEqualTo(p.getId());
    assertThat(view.warnings(clients.get(p.getId()))).isEmpty();
    assertThat(view.history(p.getId()))
        .extracting(a -> a.getAction().name())
        .contains("CREATE", "SUBMIT", "AUTHORIZE");

    Client inactive =
        as.run("mkttl", () -> onboarding.deactivate(p.getId(), "CLIENT_REQUEST", "closed"));
    assertThat(inactive.getStatus()).isEqualTo(ClientStatus.INACTIVE);
    assertThat(inactive.getDeactivatedBy()).isEqualTo("mkttl");
    assertThat(stage(p)).isEqualTo(ClientWorkflow.INACTIVE);
    assertThatThrownBy(() -> clients.requireUsable(p.getId()))
        .extracting("code")
        .isEqualTo("CLIENT_INACTIVE");
  }

  @Test
  void kycIsVerifiedByAnotherUserThanItsMaker() {
    Client p = fx.prospect(CrmFixtures.corporate());
    fx.uploadChecklist(p);
    as.run("mkttl", () -> onboarding.submitKyc(p.getId(), null));
    assertThatThrownBy(() -> as.run("mkttl", () -> onboarding.verifyKyc(p.getId(), null)))
        .extracting("code")
        .isEqualTo("KYC_FOUR_EYES");
    assertThatThrownBy(() -> as.run("ao", () -> onboarding.verifyKyc(p.getId(), null)))
        .extracting("code")
        .isEqualTo("KYC_FOUR_EYES");
  }

  @Test
  void deactivationNeedsAReason() {
    Client p = fx.prospect(CrmFixtures.person());
    assertThatThrownBy(() -> as.run("ao", () -> onboarding.deactivate(p.getId(), null, "x")))
        .extracting("code")
        .isEqualTo("WORKFLOW_REASON_REQUIRED");
  }

  @Test
  void incompleteInformationBlocksSubmission() {
    Client p =
        as.run(
            "ao",
            () ->
                clients.createProspect(
                    fx.company(),
                    new ClientDetails(
                        ClientType.INDIVIDUAL,
                        new PersonName("Minimal" + CrmFixtures.word(5), "Only", null, null, null),
                        null,
                        null,
                        null,
                        null,
                        false,
                        null)));
    fx.uploadChecklist(p);
    assertThatThrownBy(() -> as.run("ao", () -> onboarding.submitKyc(p.getId(), null)))
        .extracting("code")
        .isEqualTo("CLIENT_INFO_INCOMPLETE");
    assertThat(view.warnings(clients.get(p.getId())))
        .extracting(w -> w.code())
        .contains("INFO_INCOMPLETE");
  }

  @Test
  void hardDuplicatesAreBlockedAndLoggedSoftOnesOnlyWarn() {
    ClientDetails original = CrmFixtures.person();
    Client first = fx.prospect(original);
    ClientDetails sameTin =
        new ClientDetails(
            ClientType.INDIVIDUAL,
            new PersonName("Other" + CrmFixtures.word(5), "Person", null, null, null),
            LocalDate.of(1980, 2, 2),
            new Identity(original.identity().tin(), null, null),
            null,
            null,
            false,
            null);
    assertThatThrownBy(() -> fx.prospect(sameTin))
        .extracting("code", "message")
        .containsExactly(
            "CLIENT_DUPLICATE",
            "This client already exists: "
                + first.getCode()
                + " "
                + first.getDisplayName()
                + " (TIN)");
    assertThat(audit.history("Client", List.of(first.getCode())))
        .anyMatch(a -> a.getSummary().startsWith("Duplicate blocked"));

    ClientDetails sameMobile =
        new ClientDetails(
            ClientType.INDIVIDUAL,
            new PersonName(
                original.name().lastName().toUpperCase(),
                " " + original.name().firstName(),
                null,
                null,
                null),
            original.birthDate().plusDays(1),
            null,
            new Contact(
                null,
                "+63" + original.contact().mobile().substring(1),
                null,
                null,
                null,
                null,
                null),
            null,
            false,
            null);
    List<DuplicateMatch> soft =
        duplicates.candidates(fx.company(), DuplicateProbe.of(sameMobile), null);
    assertThat(soft)
        .singleElement()
        .satisfies(
            m -> {
              assertThat(m.hard()).isFalse();
              assertThat(m.keys()).containsExactly("MOBILE");
            });
    Client second = fx.prospect(sameMobile);
    assertThat(second.getId()).isNotEqualTo(first.getId());

    ClientDetails sameNameAndBirth =
        new ClientDetails(
            ClientType.INDIVIDUAL,
            new PersonName(
                original.name().lastName().toLowerCase(),
                original.name().firstName(),
                null,
                null,
                null),
            original.birthDate(),
            null,
            null,
            null,
            false,
            null);
    assertThat(duplicates.candidates(fx.company(), DuplicateProbe.of(sameNameAndBirth), null))
        .anySatisfy(m -> assertThat(m.keys()).contains("NAME_BIRTH_DATE"));
    assertThat(duplicates.candidates(fx.company(), DuplicateProbe.of(original), first.getId()))
        .noneMatch(m -> m.clientId().equals(first.getId()));
  }

  @Test
  void validatesFormatsListsAndAge() {
    ClientDetails good = CrmFixtures.person();
    ClientDetails young =
        new ClientDetails(
            ClientType.INDIVIDUAL,
            good.name(),
            LocalDate.now(ZoneOffset.UTC).minusYears(10),
            good.identity(),
            good.contact(),
            good.marketSegment(),
            false,
            null);
    assertThatThrownBy(() -> fx.prospect(young)).extracting("code").isEqualTo("CLIENT_UNDER_AGE");
    ClientDetails badMobile =
        new ClientDetails(
            ClientType.INDIVIDUAL,
            good.name(),
            good.birthDate(),
            good.identity(),
            new Contact(null, "12345", null, null, null, null, null),
            "NOT_A_SEGMENT",
            false,
            null);
    assertThatThrownBy(() -> fx.prospect(badMobile))
        .extracting("code")
        .isEqualTo("LOV_VALUE_INVALID");
  }

  private String stage(Client c) {
    return workflow.stageOf(ClientService.ENTITY, String.valueOf(c.getId()));
  }
}
