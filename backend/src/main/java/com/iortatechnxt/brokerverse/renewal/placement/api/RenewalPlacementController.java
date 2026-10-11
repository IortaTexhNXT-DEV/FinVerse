package com.iortatechnxt.brokerverse.renewal.placement.api;

import com.iortatechnxt.brokerverse.renewal.domain.AdviceVersion;
import com.iortatechnxt.brokerverse.renewal.placement.service.InsuranceAdvices;
import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementGeneration;
import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementResponses;
import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementSending;
import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementTracking;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Placement of the renewal accounts submitted for placement (FRRN.029 to FRRN.032): generation of
 * the placement documents, their sending, the placements of an account with the turnaround time and
 * the With Issue tag, the insurer's response, the cancellation, the return to Marketing and the
 * Insurance Advice.
 */
@RestController
@RequestMapping("/api/v1/renewal")
public class RenewalPlacementController {

  private static final String PROCESS = "hasAuthority('RNW_PROCESS')";
  private static final String VIEW = "hasAuthority('RNW_VIEW')";

  private final PlacementGeneration generation;
  private final PlacementSending sending;
  private final PlacementTracking tracking;
  private final PlacementResponses responses;
  private final InsuranceAdvices advices;
  private final RenewalRecords records;

  /**
   * Creates the controller.
   *
   * @param generation placement documents
   * @param sending sending
   * @param tracking placements of an account
   * @param responses insurer responses
   * @param advices Insurance Advice
   * @param records renewals
   */
  public RenewalPlacementController(
      PlacementGeneration generation,
      PlacementSending sending,
      PlacementTracking tracking,
      PlacementResponses responses,
      InsuranceAdvices advices,
      RenewalRecords records) {
    this.generation = generation;
    this.sending = sending;
    this.tracking = tracking;
    this.responses = responses;
    this.advices = advices;
    this.records = records;
  }

  /**
   * Generates the placement documents of renewal accounts.
   *
   * @param companyId company
   * @param body renewals
   * @return generated and refused
   */
  @PostMapping("/placements/generate")
  @PreAuthorize(PROCESS)
  public BatchOutcome generate(@RequestParam Long companyId, @RequestBody Refs body) {
    return generation.generate(companyId, body.refs());
  }

  /**
   * The insurers of the generated placements with their default recipients.
   *
   * @param companyId company
   * @param refs renewals
   * @return recipients per insurer
   */
  @GetMapping("/placements/recipients")
  @PreAuthorize(PROCESS)
  public List<PlacementSending.Recipients> recipients(
      @RequestParam Long companyId, @RequestParam List<String> refs) {
    return sending.recipients(companyId, refs);
  }

  /**
   * Sends the generated placements.
   *
   * @param companyId company
   * @param body renewals, recipients per insurer, copy recipients
   * @return summary
   */
  @PostMapping("/placements/send")
  @PreAuthorize(PROCESS)
  public PlacementSending.Summary send(
      @RequestParam Long companyId, @RequestBody PlacementSending.Request body) {
    return sending.send(companyId, body);
  }

  /**
   * Returns accounts to Marketing.
   *
   * @param companyId company
   * @param body renewals, reason and remarks
   * @return returned and refused
   */
  @PostMapping("/placements/return")
  @PreAuthorize(PROCESS)
  public BatchOutcome returnToMarketing(@RequestParam Long companyId, @RequestBody Return body) {
    return tracking.returnToMarketing(companyId, body.refs(), body.reasonCode(), body.remarks());
  }

  /**
   * The placements of an account.
   *
   * @param companyId company
   * @param ref renewal
   * @return placements
   */
  @GetMapping("/candidates/{ref}/placements")
  @PreAuthorize(VIEW)
  public List<PlacementTracking.View> placements(
      @RequestParam Long companyId, @PathVariable String ref) {
    return tracking.of(companyId, ref);
  }

  /**
   * Tags or clears the With Issue tag of a placement.
   *
   * @param companyId company
   * @param ref renewal
   * @param id placement
   * @param body tag and resolution date
   * @return placement
   */
  @PutMapping("/candidates/{ref}/placements/{id}/issue")
  @PreAuthorize(PROCESS)
  public PlacementTracking.View issue(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @PathVariable Long id,
      @RequestBody Issue body) {
    return tracking.issue(companyId, ref, id, body.withIssue(), body.resolutionDate());
  }

  /**
   * Records the insurer's response.
   *
   * @param companyId company
   * @param ref renewal
   * @param body response
   * @return placement status of the account
   */
  @PostMapping("/candidates/{ref}/placements/response")
  @PreAuthorize(PROCESS)
  public Map<String, String> respond(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody Answer body) {
    String status =
        responses.apply(
            companyId,
            new PlacementResponses.Response(
                ref,
                body.insurerCode(),
                body.approved(),
                body.date(),
                body.reason(),
                body.remarks()));
    return Map.of("placementStatus", status);
  }

  /**
   * Cancels a rejected placement.
   *
   * @param companyId company
   * @param ref renewal
   * @param body remarks
   */
  @PostMapping("/candidates/{ref}/placements/cancel")
  @PreAuthorize(PROCESS)
  public void cancel(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody Remarks body) {
    tracking.cancel(companyId, ref, body.remarks());
  }

  /**
   * The Insurance Advice versions of an account.
   *
   * @param companyId company
   * @param ref renewal
   * @return versions, newest first
   */
  @GetMapping("/candidates/{ref}/insurance-advices")
  @PreAuthorize(VIEW)
  public List<Advice> advices(@RequestParam Long companyId, @PathVariable String ref) {
    return advices.of(companyId, ref).stream().map(Advice::of).toList();
  }

  /**
   * Generates the Insurance Advice of an account (a new version when the placement changed).
   *
   * @param companyId company
   * @param ref renewal
   * @return the new version, or the latest when nothing changed
   */
  @PostMapping("/candidates/{ref}/insurance-advices")
  @PreAuthorize(PROCESS)
  public Advice generateAdvice(@RequestParam Long companyId, @PathVariable String ref) {
    AdviceVersion v = advices.afterPlacement(records.get(companyId, ref), "PLACEMENT_CHANGED");
    if (v != null) {
      return Advice.of(v);
    }
    List<AdviceVersion> all = advices.of(companyId, ref);
    return all.isEmpty() ? null : Advice.of(all.get(0));
  }

  /**
   * Sends the latest Insurance Advice of accounts through CCM.
   *
   * @param companyId company
   * @param body accounts and recipients
   * @return CCM message numbers
   */
  @PostMapping("/insurance-advices/send")
  @PreAuthorize(PROCESS)
  public List<String> sendAdvices(@RequestParam Long companyId, @RequestBody AdviceSend body) {
    return advices.send(companyId, body.refs(), body.to(), body.cc()).stream()
        .map(m -> m.getMessageNo())
        .toList();
  }

  /**
   * Renewals.
   *
   * @param refs references
   */
  public record Refs(List<String> refs) {}

  /**
   * A return to Marketing.
   *
   * @param refs renewals
   * @param reasonCode reason
   * @param remarks remarks
   */
  public record Return(List<String> refs, String reasonCode, String remarks) {}

  /**
   * The With Issue tag.
   *
   * @param withIssue tag
   * @param resolutionDate resolution date
   */
  public record Issue(boolean withIssue, LocalDate resolutionDate) {}

  /**
   * An insurer's response recorded by the user.
   *
   * @param insurerCode insurer, null for all
   * @param approved approved or rejected
   * @param date response date
   * @param reason rejection reason code
   * @param remarks remarks
   */
  public record Answer(
      String insurerCode, boolean approved, LocalDate date, String reason, String remarks) {}

  /**
   * Remarks.
   *
   * @param remarks text
   */
  public record Remarks(String remarks) {}

  /**
   * Sending of Insurance Advices.
   *
   * @param refs accounts
   * @param to nominated recipients
   * @param cc copy recipients
   */
  public record AdviceSend(List<String> refs, List<String> to, List<String> cc) {}

  /**
   * An Insurance Advice version.
   *
   * @param versionNo version
   * @param fileName file name
   * @param attachmentId stored file
   * @param trigger what generated it
   * @param messageNo CCM message of the sending, may be null
   * @param generatedBy user
   * @param generatedAt time
   */
  public record Advice(
      int versionNo,
      String fileName,
      Long attachmentId,
      String trigger,
      String messageNo,
      String generatedBy,
      Instant generatedAt) {

    static Advice of(AdviceVersion v) {
      return new Advice(
          v.getVersionNo(),
          v.getFileName(),
          v.getAttachmentId(),
          v.getTriggerEvent(),
          v.getMessageNo(),
          v.getCreatedBy(),
          v.getCreatedAt());
    }
  }
}
