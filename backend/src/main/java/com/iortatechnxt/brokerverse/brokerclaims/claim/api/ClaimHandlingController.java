package com.iortatechnxt.brokerverse.brokerclaims.claim.api;

import com.iortatechnxt.brokerverse.brokerclaims.claim.api.dto.ClaimResponse;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimExtrasService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimViewService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.status.domain.FollowUp;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.ClaimLookup;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.DocumentChecklist;
import com.iortatechnxt.brokerverse.brokerclaims.status.service.FollowUpRecorder;
import com.iortatechnxt.brokerverse.common.security.OwnedRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Claim handling of the FRS BRD-07 Claims v1.0 in the BRD scope: follow-ups with the fields of the
 * BRD (FRCLM.009.04), the document checklist of the claim (FRCLM.006.02), the plate number and
 * preferred repair shop (FRCLM.008.02, 007.02) and the return of the claim of an unpaid policy to
 * the Account Officer (FRCLM.008.04).
 */
@OwnedRecord(Claim.class)
@RestController
@RequestMapping("/api/v1/broker-claims/{id}")
public class ClaimHandlingController {

  private final FollowUpRecorder followUps;
  private final DocumentChecklist checklist;
  private final ClaimExtrasService extras;
  private final ClaimViewService views;
  private final ClaimLookup lookup;

  /**
   * Creates the controller.
   *
   * @param followUps follow-ups
   * @param checklist document checklist
   * @param extras motor details and return to the Account Officer
   * @param views claim record
   * @param lookup claims of the company
   */
  public ClaimHandlingController(
      FollowUpRecorder followUps,
      DocumentChecklist checklist,
      ClaimExtrasService extras,
      ClaimViewService views,
      ClaimLookup lookup) {
    this.followUps = followUps;
    this.checklist = checklist;
    this.extras = extras;
    this.views = views;
    this.lookup = lookup;
  }

  /**
   * Follow-ups of the claim, newest first.
   *
   * @param id claim
   * @param companyId company
   * @return follow-ups
   */
  @GetMapping("/follow-ups")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public List<FollowUpResponse> followUps(@PathVariable Long id, @RequestParam Long companyId) {
    return followUps.of(companyId, id).stream().map(FollowUpResponse::from).toList();
  }

  /**
   * Records a follow-up.
   *
   * @param id claim
   * @param companyId company
   * @param request follow-up
   * @return the follow-up
   */
  @PostMapping("/follow-ups")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('BCL_STATUS_UPDATE')")
  public FollowUpResponse recordFollowUp(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @Valid @RequestBody FollowUpEntryRequest request) {
    return FollowUpResponse.from(followUps.record(companyId, id, request.toRequest()));
  }

  /**
   * The document checklist of the claim with what is uploaded.
   *
   * @param id claim
   * @param companyId company
   * @return checklist items
   */
  @GetMapping("/document-checklist")
  @PreAuthorize("hasAuthority('BCL_VIEW')")
  public List<DocumentChecklist.Item> documentChecklist(
      @PathVariable Long id, @RequestParam Long companyId) {
    return checklist.of(lookup.require(companyId, id));
  }

  /**
   * Sets the plate number and the preferred repair shop.
   *
   * @param id claim
   * @param companyId company
   * @param request plate and shop
   * @return the claim
   */
  @PutMapping("/motor")
  @PreAuthorize("hasAuthority('BCL_RECORD')")
  public ClaimResponse motor(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody MotorRequest request) {
    extras.motor(companyId, id, request.plateNo(), request.preferredShopCode());
    return ClaimResponse.from(views.view(companyId, id));
  }

  /**
   * Returns the claim of a partly paid or unpaid policy to the Account Officer.
   *
   * @param id claim
   * @param companyId company
   * @param request note
   * @return the claim
   */
  @PostMapping("/return-to-ao")
  @PreAuthorize("hasAnyAuthority('BCL_RECORD', 'BCL_STATUS_UPDATE')")
  public ClaimResponse returnToAccountOfficer(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody ReturnRequest request) {
    extras.returnToAccountOfficer(companyId, id, request.note());
    return ClaimResponse.from(views.view(companyId, id));
  }

  /**
   * A follow-up to record.
   *
   * @param type follow-up type ({@code BCL_FOLLOW_UP_TYPE})
   * @param actionTaken action taken today
   * @param nextPlanSummary next action plan summary
   * @param nextPlanDetail next action plan detail
   * @param statusCode status as of today, the current one when blank
   * @param settlementAmount settlement amount
   * @param adjusterEvaluation adjuster's evaluation
   * @param insurerDeductible insurer's deductible
   */
  public record FollowUpEntryRequest(
      @NotBlank String type,
      @NotBlank @Size(max = 2000) String actionTaken,
      @Size(max = 500) String nextPlanSummary,
      @Size(max = 2000) String nextPlanDetail,
      String statusCode,
      BigDecimal settlementAmount,
      BigDecimal adjusterEvaluation,
      BigDecimal insurerDeductible) {

    FollowUpRecorder.Request toRequest() {
      return new FollowUpRecorder.Request(
          new FollowUp.Entry(type, actionTaken, nextPlanSummary, nextPlanDetail),
          statusCode,
          new FollowUp.Amounts(settlementAmount, adjusterEvaluation, insurerDeductible));
    }
  }

  /**
   * A follow-up.
   *
   * @param id id
   * @param type follow-up type
   * @param actionTaken action taken
   * @param nextPlanSummary next action plan summary
   * @param nextPlanDetail next action plan detail
   * @param statusCode status as of the date
   * @param settlementAmount settlement amount
   * @param adjusterEvaluation adjuster's evaluation
   * @param insurerDeductible insurer's deductible
   * @param followedUpOn business date
   * @param createdBy handler
   * @param createdAt time
   */
  public record FollowUpResponse(
      Long id,
      String type,
      String actionTaken,
      String nextPlanSummary,
      String nextPlanDetail,
      String statusCode,
      BigDecimal settlementAmount,
      BigDecimal adjusterEvaluation,
      BigDecimal insurerDeductible,
      LocalDate followedUpOn,
      String createdBy,
      Instant createdAt) {

    static FollowUpResponse from(FollowUp f) {
      return new FollowUpResponse(
          f.getId(),
          f.getFollowUpType(),
          f.getActionTaken(),
          f.getNextPlanSummary(),
          f.getNextPlanDetail(),
          f.getStatusCode(),
          f.getSettlementAmount(),
          f.getAdjusterEvaluation(),
          f.getInsurerDeductible(),
          f.getFollowedUpOn(),
          f.getCreatedBy(),
          f.getCreatedAt());
    }
  }

  /**
   * Plate number and preferred repair shop.
   *
   * @param plateNo plate number, blank clears it
   * @param preferredShopCode repair shop party code, blank clears it
   */
  public record MotorRequest(@Size(max = 20) String plateNo, String preferredShopCode) {}

  /**
   * Note of a return to the Account Officer.
   *
   * @param note note, may be blank
   */
  public record ReturnRequest(@Size(max = 500) String note) {}
}
