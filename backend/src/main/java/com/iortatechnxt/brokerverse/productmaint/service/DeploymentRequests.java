package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequestRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Request Package Deployment (BDOI FRS FRPM.015.01): once every required approval is complete the
 * requestor is told; with PM_DEPLOYMENT_REQUEST MANUAL the Request for Deployment button forwards
 * the package to MBS, with AUTO the ManCom approval does. Either way a Package Deployment Request
 * is created with its number, date and time and requestor, the request shows For Deployment and the
 * designated MBS users are told.
 */
@Service
@Transactional
public class DeploymentRequests {

  /** Notification event of a deployment request. */
  public static final String REQUESTED_EVENT = "PM_DEPLOYMENT_REQUESTED";

  /** Notification event of the approvals complete. */
  public static final String APPROVED_EVENT = "PM_APPROVALS_COMPLETE";

  private static final String MBS = "PRODUCT_MAINTAIN";

  private final PackageRequestRepository requests;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final NoticeDelivery delivery;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param requests package requests
   * @param workflow workflow
   * @param numbers number series
   * @param delivery notices
   * @param audit audit trail
   * @param currentUser current user
   * @param parameters business parameters (prefix)
   * @param clock clock
   */
  public DeploymentRequests(
      PackageRequestRepository requests,
      WorkflowService workflow,
      DocumentNumberService numbers,
      NoticeDelivery delivery,
      AuditTrailService audit,
      CurrentUser currentUser,
      SystemParameterService parameters,
      Clock clock) {
    this.requests = requests;
    this.workflow = workflow;
    this.numbers = numbers;
    this.delivery = delivery;
    this.audit = audit;
    this.currentUser = currentUser;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * The Request for Deployment button.
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public PackageRequest request(Long id, String comment) {
    PackageRequest p =
        requests
            .findById(id)
            .orElseThrow(() -> new BusinessRuleException("PKG_NOT_FOUND", "Request not found"));
    if (p.getStatus() != RequestStage.MANCOM_APPROVED) {
      throw new BusinessRuleException(
          "PKG_DEPLOYMENT_NOT_READY",
          "The deployment is requested once every required approval is complete");
    }
    workflow.transition(
        PackageRequests.ENTITY,
        String.valueOf(id),
        "request_deployment",
        TransitionNote.comment(comment));
    return p;
  }

  /**
   * Follows the stage changes: the approvals complete, and the request entering With MBS.
   *
   * @param event transition
   */
  @EventListener
  public void onTransition(WorkCaseTransitioned event) {
    boolean ours = PackageRequests.ENTITY.equals(event.entityType());
    boolean approved =
        ours
            && RequestStage.FOR_MANCOM.name().equals(event.fromStage())
            && (RequestStage.MANCOM_APPROVED.name().equals(event.toStage())
                || RequestStage.WITH_MBS.name().equals(event.toStage()));
    boolean deployment = ours && RequestStage.WITH_MBS.name().equals(event.toStage());
    if (approved || deployment) {
      follow(event, approved, deployment);
    }
  }

  private void follow(WorkCaseTransitioned event, boolean approved, boolean deployment) {
    requests
        .findById(Long.valueOf(event.entityId()))
        .ifPresent(
            p -> {
              if (approved) {
                tellRequestor(p, RequestStage.MANCOM_APPROVED.name().equals(event.toStage()));
              }
              if (deployment && p.getRouting().getDeploymentNo() == null) {
                createDeploymentRequest(p);
              }
            });
  }

  private void tellRequestor(PackageRequest p, boolean toRequest) {
    delivery.toUser(
        p.getCreatedBy(),
        notice(
            p,
            p.getRequestNo() + ": all approvals complete",
            toRequest
                ? "Request the deployment of the package."
                : "The package was sent to MBS for deployment."),
        APPROVED_EVENT,
        true);
  }

  private void createDeploymentRequest(PackageRequest p) {
    String number =
        numbers.next(
            parameters.text("PKG_DEPLOYMENT_PREFIX", "PDR-")
                + BusinessClock.today(clock).getYear());
    p.getRouting().requestDeployment(number, currentUser.username(), clock.instant());
    audit.record(
        PackageRequests.ENTITY,
        p.getRequestNo(),
        AuditAction.SUBMIT,
        "Package deployment request " + number);
    delivery.toPermission(
        MBS,
        notice(p, "Package deployment request " + number, p.getTitle()),
        REQUESTED_EVENT,
        true);
  }

  private static Notice notice(PackageRequest p, String title, String body) {
    return new Notice(
        title, body, PackageRequests.link(p), PackageRequests.ENTITY, String.valueOf(p.getId()));
  }
}
