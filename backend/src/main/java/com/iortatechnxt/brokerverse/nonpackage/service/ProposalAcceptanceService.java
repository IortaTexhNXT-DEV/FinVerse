package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.Account.Origin;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponseRepository;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Acceptance of a proposal and creation of its accounts (BRNB.045/102, BRD 2.2): acceptance needs
 * the client's acceptance e-mail (document type CLIENT_ACCEPTANCE); each accepted risk group
 * becomes a draft account through {@code AccountService.createDraft} with the PRF's ARN (suffix
 * -01, -02... when there are several), the chosen insurer and its quoted rate, for a confirmed
 * client (BRNB.029).
 */
@Service
@Transactional
public class ProposalAcceptanceService {

  private static final String ACCEPTANCE_DOCUMENT = "CLIENT_ACCEPTANCE";

  private final ProposalService proposals;
  private final RiskDetailsCodec codec;
  private final InsurerResponseRepository responses;
  private final ClientService clients;
  private final AccountService accounts;
  private final DocumentService documents;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param proposals PRF reads
   * @param codec risk details JSON
   * @param responses insurer responses
   * @param clients clients
   * @param accounts account creation
   * @param documents documents of the PRF
   * @param workflow workflow engine
   * @param audit audit trail
   * @param clock clock
   */
  public ProposalAcceptanceService(
      ProposalService proposals,
      RiskDetailsCodec codec,
      InsurerResponseRepository responses,
      ClientService clients,
      AccountService accounts,
      DocumentService documents,
      WorkflowService workflow,
      AuditTrailService audit,
      Clock clock) {
    this.proposals = proposals;
    this.codec = codec;
    this.responses = responses;
    this.clients = clients;
    this.accounts = accounts;
    this.documents = documents;
    this.workflow = workflow;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records the client's acceptance of the proposal.
   *
   * @param id PRF
   * @param groups accepted risk groups; empty for all
   * @param comment comment
   * @return the PRF, now ACCEPTED
   */
  public ProposalRequest accept(Long id, List<Integer> groups, String comment) {
    ProposalRequest p = proposals.get(id);
    boolean evidence =
        documents
            .documentTypesOf(new AttachmentTarget(ProposalService.ENTITY, String.valueOf(id)))
            .contains(ACCEPTANCE_DOCUMENT);
    if (!evidence) {
      throw new BusinessRuleException(
          "ACCEPTANCE_EMAIL_REQUIRED", "Attach the client's acceptance e-mail first");
    }
    List<Integer> offered = codec.of(p).groups();
    List<Integer> chosen =
        groups == null || groups.isEmpty() ? offered : groups.stream().distinct().sorted().toList();
    if (!offered.containsAll(chosen)) {
      throw new BusinessRuleException(
          "ACCEPTANCE_GROUP_UNKNOWN", "The proposal covers risk groups " + offered + " only");
    }
    workflow.transition(
        ProposalService.ENTITY, String.valueOf(id), "accept", TransitionNote.comment(comment));
    p.markAccepted(chosen, clock.instant());
    audit.record(
        ProposalService.ENTITY,
        p.getPrfNo(),
        AuditAction.UPDATE,
        "Proposal accepted by the client, risk group(s) " + chosen);
    return p;
  }

  /**
   * Creates the accounts of the accepted risk groups with the chosen insurer.
   *
   * @param id PRF
   * @param comment comment
   * @return the PRF, now CONVERTED, with the account ARNs
   */
  public ProposalRequest createAccounts(Long id, String comment) {
    ProposalRequest p = proposals.get(id);
    clients.requireConfirmed(p.getClientId());
    List<Integer> groups = p.getAcceptedGroupList();
    if (groups.isEmpty() || p.getChosenInsurer() == null) {
      throw new BusinessRuleException(
          "PROPOSAL_NOT_ACCEPTED", "Record the client's acceptance of the proposal first");
    }
    InsurerResponse terms =
        responses
            .findByProposalIdAndInsurerCode(id, p.getChosenInsurer())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "PROPOSAL_TERMS_MISSING",
                        "No terms of " + p.getChosenInsurer() + " are recorded"));
    RiskDetails details = codec.of(p);
    List<String> arns = new ArrayList<>();
    int sequence = 0;
    for (int group : groups) {
      sequence++;
      String arn =
          groups.size() == 1
              ? p.getArn()
              : p.getArn() + String.format(Locale.ROOT, "-%02d", sequence);
      Account account =
          accounts.createDraft(
              new NewAccount(
                  p.getCompanyId(),
                  arn,
                  new Origin(null, p.getPrfNo()),
                  accountOf(p, details.itemsOf(group), terms.getRate()),
                  null,
                  p.getCreatedBy()));
      arns.add(account.getArn());
    }
    p.linkAccounts(arns);
    workflow.transition(
        ProposalService.ENTITY,
        String.valueOf(id),
        "create_accounts",
        TransitionNote.comment(
            "Accounts " + String.join(", ", arns) + (comment == null ? "" : " - " + comment)));
    audit.record(
        ProposalService.ENTITY,
        p.getPrfNo(),
        AuditAction.UPDATE,
        "Accounts created with " + p.getChosenInsurer() + ": " + String.join(", ", arns));
    return p;
  }

  private static AccountDraft accountOf(
      ProposalRequest p, List<RiskItemData> items, BigDecimal insurerRate) {
    List<RiskItemData> rated =
        items.stream()
            .map(
                d ->
                    new RiskItemData(
                        d.description(),
                        d.sumInsured(),
                        insurerRate != null ? insurerRate : d.rate(),
                        d.biLimit(),
                        d.pdLimit(),
                        d.vehicle(),
                        d.location(),
                        d.person()))
            .toList();
    return new AccountDraft(
        p.getClientId(),
        p.getProductCode(),
        p.getMarketSegment(),
        p.getSourceChannel(),
        p.getChosenInsurer(),
        null,
        p.getPeriodFrom(),
        p.getPeriodTo(),
        false,
        1,
        p.getCurrency(),
        PaymentArrangement.VIA_BDOI,
        Mortgage.NONE,
        null,
        rated,
        null,
        null,
        null);
  }
}
