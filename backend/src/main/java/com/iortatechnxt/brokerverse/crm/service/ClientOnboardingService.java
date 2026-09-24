package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client onboarding (BRNB.090/101): the Account Officer submits the KYC documents, a checker (not
 * the maker) verifies them and the client is confirmed, which issues the client code and opens the
 * client's sub-ledger party in the same transaction. Every step moves the {@value
 * ClientWorkflow#WORKFLOW} case and is audited.
 */
@Service
@Transactional
public class ClientOnboardingService {

  private static final String CLIENT_WORD = "Client ";

  private final ClientService clients;
  private final KycDocumentService kyc;
  private final ClientCompleteness completeness;
  private final DuplicateCheckService duplicates;
  private final ClientWorkflow workflow;
  private final ClientPartyLink parties;
  private final KycReviewPolicy reviewPolicy;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param clients clients
   * @param kyc KYC documents
   * @param completeness completeness rules
   * @param duplicates duplicate detection
   * @param workflow onboarding workflow
   * @param parties sub-ledger party link
   * @param reviewPolicy KYC review cycle
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public ClientOnboardingService(
      ClientService clients,
      KycDocumentService kyc,
      ClientCompleteness completeness,
      DuplicateCheckService duplicates,
      ClientWorkflow workflow,
      ClientPartyLink parties,
      KycReviewPolicy reviewPolicy,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.clients = clients;
    this.kyc = kyc;
    this.completeness = completeness;
    this.duplicates = duplicates;
    this.workflow = workflow;
    this.parties = parties;
    this.reviewPolicy = reviewPolicy;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Submits the KYC documents of a prospect for verification (maker step).
   *
   * @param id client
   * @param comment optional comment
   * @return the client
   */
  public Client submitKyc(Long id, String comment) {
    Client client = clients.requireUsable(id);
    requireCompleteInformation(client, "submit the KYC");
    kyc.requireComplete(client, "submit the KYC");
    client.submitKyc(currentUser.username(), clock.instant());
    workflow.act(client, "submit_kyc", TransitionNote.comment(comment));
    record(client, AuditAction.SUBMIT, "KYC submitted for verification");
    return client;
  }

  /**
   * Verifies the KYC (checker step, four eyes): of a prospect after submission, or the periodic
   * review of a confirmed client (BRNB.110). Sets the next review date from the risk rating.
   *
   * @param id client
   * @param comment optional comment
   * @return the client
   */
  public Client verifyKyc(Long id, String comment) {
    Client client = clients.requireUsable(id);
    String checker = currentUser.username();
    if (CurrentUser.sameUser(checker, client.getCreatedBy())
        || CurrentUser.sameUser(checker, client.getKycSubmittedBy())) {
      throw new BusinessRuleException(
          "KYC_FOUR_EYES", "The KYC must be verified by a user other than its maker");
    }
    kyc.requireComplete(client, "verify the KYC");
    LocalDate due = reviewPolicy.nextReview(client.getRiskRating(), LocalDate.now(clock));
    String action = client.getStatus() == ClientStatus.CONFIRMED ? "review_kyc" : "verify_kyc";
    workflow.act(client, action, TransitionNote.comment(comment));
    client.verifyKyc(checker, clock.instant(), due);
    record(client, AuditAction.AUTHORIZE, "KYC verified; next review due " + due);
    return client;
  }

  /**
   * Confirms a KYC-verified prospect (BRNB.101): issues the client code (the prospect code is kept)
   * and opens and authorizes its sub-ledger party so the client can carry receivables.
   *
   * @param id client
   * @param comment optional comment
   * @return the confirmed client
   */
  public Client confirm(Long id, String comment) {
    Client client = clients.requireUsable(id);
    if (client.getKycStatus() != KycStatus.VERIFIED) {
      throw new BusinessRuleException(
          "KYC_NOT_VERIFIED", CLIENT_WORD + client.getCode() + " has no verified KYC");
    }
    requireCompleteInformation(client, "confirm the client");
    kyc.requireComplete(client, "confirm the client");
    duplicates.requireNoHardMatch(
        client.getCompanyId(),
        DuplicateProbe.of(client),
        client.getId(),
        "confirmation of " + client.getCode());
    workflow.act(client, "confirm", TransitionNote.comment(comment));
    String code = numbers.next("CL-" + LocalDate.now(clock).getYear());
    String partyCode = parties.openParty(client, code);
    client.confirm(code, partyCode, currentUser.username(), clock.instant());
    workflow.describe(client);
    record(
        client,
        AuditAction.AUTHORIZE,
        "Confirmed as "
            + code
            + " (prospect "
            + client.getProspectCode()
            + ", party "
            + partyCode
            + ")");
    return client;
  }

  /**
   * Deactivates a client with a reason; the client can no longer be used for new business.
   *
   * @param id client
   * @param reason reason code (list of values CLIENT_DEACTIVATION_REASON)
   * @param comment optional comment
   * @return the client
   */
  public Client deactivate(Long id, String reason, String comment) {
    Client client = clients.requireUsable(id);
    workflow.act(client, "deactivate", new TransitionNote(reason, comment));
    client.deactivate(reason, comment, currentUser.username(), clock.instant());
    record(
        client,
        AuditAction.DEACTIVATE,
        "Deactivated (" + reason + ")" + (comment == null ? "" : ": " + comment));
    return client;
  }

  private void requireCompleteInformation(Client client, String step) {
    List<String> missing = completeness.missing(client);
    if (!missing.isEmpty()) {
      throw new BusinessRuleException(
          "CLIENT_INFO_INCOMPLETE",
          "Complete the client information before you " + step + ": " + String.join(", ", missing));
    }
  }

  private void record(Client client, AuditAction action, String summary) {
    audit.record(ClientService.ENTITY, client.getProspectCode(), action, summary);
  }
}
