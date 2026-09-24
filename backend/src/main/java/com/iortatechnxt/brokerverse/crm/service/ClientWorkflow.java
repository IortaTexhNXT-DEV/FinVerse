package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.crm.domain.ClientStatus;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Onboarding workflow {@value #WORKFLOW} of a client (BRNB.090/101): prospect, KYC for
 * verification, KYC verified, confirmed, inactive. Opens the case of a new client, runs the
 * business actions of the client screens and mirrors the stage on the client.
 */
@Component
public class ClientWorkflow {

  /** Workflow code. */
  public static final String WORKFLOW = "NB_CLIENT";

  /** Stage: prospect, KYC documents to complete. */
  public static final String PROSPECT = "PROSPECT";

  /** Stage: KYC waiting for the checker. */
  public static final String KYC_REVIEW = "KYC_REVIEW";

  /** Stage: KYC verified, client to confirm. */
  public static final String KYC_VERIFIED = "KYC_VERIFIED";

  /** Stage: confirmed client. */
  public static final String CONFIRMED = "CONFIRMED";

  /** Stage: inactive client. */
  public static final String INACTIVE = "INACTIVE";

  private final WorkflowService workflow;
  private final WorkflowViewService views;
  private final ClientRepository clients;

  /**
   * Creates the component.
   *
   * @param workflow workflow engine
   * @param views workflow reads
   * @param clients clients
   */
  public ClientWorkflow(
      WorkflowService workflow, WorkflowViewService views, ClientRepository clients) {
    this.workflow = workflow;
    this.views = views;
    this.clients = clients;
  }

  /**
   * Opens the onboarding case of a new prospect.
   *
   * @param client saved prospect
   */
  public void start(Client client) {
    workflow.start(new StartCase(client.getCompanyId(), WORKFLOW, record(client), null));
    client.mirrorStage(PROSPECT);
  }

  /**
   * Runs a business action of the client screens; opens the case first for clients created before
   * the onboarding workflow existed.
   *
   * @param client client
   * @param action action code
   * @param note reason and comment
   */
  public void act(Client client, String action, TransitionNote note) {
    ensureCase(client);
    workflow.transition(ClientService.ENTITY, key(client), action, note);
  }

  /**
   * Refreshes the reference and title shown in the work queues.
   *
   * @param client client
   */
  public void describe(Client client) {
    ensureCase(client);
    workflow.describe(ClientService.ENTITY, key(client), client.getCode(), client.getDisplayName());
  }

  private void ensureCase(Client client) {
    if (views.view(ClientService.ENTITY, key(client)).isEmpty()) {
      workflow.start(
          new StartCase(client.getCompanyId(), WORKFLOW, record(client), stageFor(client)));
      client.mirrorStage(stageFor(client));
    }
  }

  private static String stageFor(Client client) {
    if (client.getStatus() == ClientStatus.CONFIRMED) {
      return CONFIRMED;
    }
    if (client.getStatus() == ClientStatus.INACTIVE) {
      return INACTIVE;
    }
    return client.getKycStatus() == KycStatus.VERIFIED ? KYC_VERIFIED : PROSPECT;
  }

  /**
   * Mirrors every stage change of a client case (user, system and generic actions such as the
   * checker returning the KYC to the Account Officer).
   *
   * @param event transition
   */
  @EventListener
  public void onTransition(WorkCaseTransitioned event) {
    if (!ClientService.ENTITY.equals(event.entityType())) {
      return;
    }
    clients.findById(Long.valueOf(event.entityId())).ifPresent(c -> c.mirrorStage(event.toStage()));
  }

  private static CaseRecord record(Client c) {
    return new CaseRecord(
        ClientService.ENTITY,
        key(c),
        c.getCode(),
        c.getDisplayName(),
        "/crm/clients/" + c.getId(),
        c.getMarketSegment());
  }

  private static String key(Client c) {
    return String.valueOf(c.getId());
  }
}
