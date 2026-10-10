package com.iortatechnxt.brokerverse.productmaint.api;

import com.iortatechnxt.brokerverse.productmaint.api.dto.RequestDtos.CommentBody;
import com.iortatechnxt.brokerverse.productmaint.domain.ManComApproval;
import com.iortatechnxt.brokerverse.productmaint.domain.MarketingApproval;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestRouting;
import com.iortatechnxt.brokerverse.productmaint.service.DeploymentRequests;
import com.iortatechnxt.brokerverse.productmaint.service.ManComRouting;
import com.iortatechnxt.brokerverse.productmaint.service.MarketingChain;
import com.iortatechnxt.brokerverse.productmaint.service.PackageRequests;
import com.iortatechnxt.brokerverse.productmaint.service.RequestDetails;
import com.iortatechnxt.brokerverse.productmaint.service.RequestDetailsService;
import com.iortatechnxt.brokerverse.productmaint.service.RequirementsService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The routing of a package request in BDOI's FRS: the Source and Annex E details with the Marketing
 * approval history (FRPM.011.02), the ManCom approver selection, decisions and progress
 * (FRPM.014.01) and the Request for Deployment (FRPM.015.01).
 */
@RestController
@RequestMapping("/api/v1/product-maintenance/requests/{id}")
public class PackageRoutingController {

  private final PackageRequests requests;
  private final RequestDetailsService details;
  private final MarketingChain marketing;
  private final ManComRouting mancom;
  private final DeploymentRequests deployments;
  private final SystemParameterService parameters;

  /**
   * Creates the controller.
   *
   * @param requests package requests
   * @param details source and Annex E details
   * @param marketing Marketing approvals
   * @param mancom ManCom routing
   * @param deployments deployment requests
   * @param parameters business parameters (deployment setting)
   */
  public PackageRoutingController(
      PackageRequests requests,
      RequestDetailsService details,
      MarketingChain marketing,
      ManComRouting mancom,
      DeploymentRequests deployments,
      SystemParameterService parameters) {
    this.requests = requests;
    this.details = details;
    this.marketing = marketing;
    this.mancom = mancom;
    this.deployments = deployments;
    this.parameters = parameters;
  }

  /**
   * The routing facts of a request.
   *
   * @param id request
   * @return source, details, approvals, ManCom progress and deployment request
   */
  @GetMapping("/routing")
  @PreAuthorize(PackageRequestController.VIEW)
  public RoutingView routing(@PathVariable Long id) {
    PackageRequest p = requests.get(id);
    RequestRouting r = p.getRouting();
    return new RoutingView(
        r.getSource(),
        details.details(p),
        new MarketingView(
            marketing.chained(),
            r.getMarketingLevel(),
            marketing.history(id).stream().map(MarketingStep::from).toList()),
        new ManComView(
            mancom.selectedRouting(),
            mancom.president(),
            mancom.members(),
            mancom.progress(id).stream().map(ManComStep::from).toList()),
        new DeploymentView(
            parameters.text(RequirementsService.DEPLOYMENT_SETTING, "AUTO").strip(),
            r.getDeploymentNo(),
            r.getDeploymentRequestedAt(),
            r.getDeploymentRequestedBy()));
  }

  /**
   * Selects the ManCom approvers.
   *
   * @param id request
   * @param body approvers
   * @return the routing facts
   */
  @PostMapping("/mancom-approvers")
  @PreAuthorize("hasAnyAuthority('PKG_NEGOTIATE', 'PKG_TSU_APPROVE')")
  public RoutingView selectApprovers(
      @PathVariable Long id, @Valid @RequestBody ApproversBody body) {
    mancom.select(id, body.approvers());
    return routing(id);
  }

  /**
   * The decision of a selected ManCom approver.
   *
   * @param id request
   * @param body decision and remarks
   * @return the routing facts
   */
  @PostMapping("/mancom-decision")
  @PreAuthorize("hasAuthority('PKG_MANCOM_SIGNOFF')")
  public RoutingView decide(@PathVariable Long id, @Valid @RequestBody DecisionBody body) {
    mancom.decide(id, body.decision(), body.remarks());
    return routing(id);
  }

  /**
   * Request for Deployment.
   *
   * @param id request
   * @param body comment
   * @return the routing facts
   */
  @PostMapping("/request-deployment")
  @PreAuthorize("hasAnyAuthority('PKG_NEGOTIATE', 'PKG_REQUEST')")
  public RoutingView requestDeployment(
      @PathVariable Long id, @Valid @RequestBody CommentBody body) {
    deployments.request(id, body.text());
    return routing(id);
  }

  /**
   * Selected approvers.
   *
   * @param approvers usernames
   */
  public record ApproversBody(@NotEmpty List<@NotBlank String> approvers) {}

  /**
   * A decision.
   *
   * @param decision APPROVE, RETURN or REJECT
   * @param remarks remarks
   */
  public record DecisionBody(@NotBlank String decision, @Size(max = 1000) String remarks) {}

  /**
   * The routing facts of a request.
   *
   * @param source Marketing, TSU or Insurer
   * @param details Annex E details
   * @param marketing Marketing approvals
   * @param mancom ManCom routing
   * @param deployment deployment request
   */
  public record RoutingView(
      String source,
      RequestDetails details,
      MarketingView marketing,
      ManComView mancom,
      DeploymentView deployment) {}

  /**
   * Marketing approvals.
   *
   * @param chained whether the Team Leader, Team Head and Unit Head approve in turn
   * @param levelsGiven approvals given
   * @param history approvals
   */
  public record MarketingView(boolean chained, int levelsGiven, List<MarketingStep> history) {}

  /**
   * One Marketing approval.
   *
   * @param level 1 Team Leader, 2 Team Head, 3 Unit Head
   * @param approver approver
   * @param decision decision
   * @param remarks remarks
   * @param decidedAt when
   */
  public record MarketingStep(
      int level, String approver, String decision, String remarks, Instant decidedAt) {

    static MarketingStep from(MarketingApproval a) {
      return new MarketingStep(
          a.getLevelNo(), a.getApprover(), a.getDecision(), a.getRemarks(), a.getDecidedAt());
    }
  }

  /**
   * ManCom routing.
   *
   * @param selectedRouting whether the selected approvers approve in parallel, then the President
   * @param president the President
   * @param members the ManCom approver list
   * @param tasks the approval tasks, newest round first
   */
  public record ManComView(
      boolean selectedRouting, String president, List<String> members, List<ManComStep> tasks) {}

  /**
   * One ManCom approval task.
   *
   * @param round approval round
   * @param approver approver
   * @param president whether the approver is the President
   * @param status WAITING, PENDING, APPROVED, RETURNED, REJECTED or CLOSED
   * @param remarks remarks
   * @param decidedAt when
   */
  public record ManComStep(
      int round,
      String approver,
      boolean president,
      String status,
      String remarks,
      Instant decidedAt) {

    static ManComStep from(ManComApproval t) {
      return new ManComStep(
          t.getRoundNo(),
          t.getApprover(),
          t.isPresident(),
          t.getStatus(),
          t.getRemarks(),
          t.getDecidedAt());
    }
  }

  /**
   * The deployment request.
   *
   * @param mode AUTO or MANUAL
   * @param number deployment request number
   * @param requestedAt when
   * @param requestedBy who
   */
  public record DeploymentView(
      String mode, String number, Instant requestedAt, String requestedBy) {}
}
