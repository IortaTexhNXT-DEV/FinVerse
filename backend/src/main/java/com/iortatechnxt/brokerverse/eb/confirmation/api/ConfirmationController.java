package com.iortatechnxt.brokerverse.eb.confirmation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.eb.confirmation.api.dto.ConfirmationDtos.ConfirmationRequest;
import com.iortatechnxt.brokerverse.eb.confirmation.api.dto.ConfirmationDtos.ConfirmationResponse;
import com.iortatechnxt.brokerverse.eb.confirmation.api.dto.ConfirmationDtos.VoidRequest;
import com.iortatechnxt.brokerverse.eb.confirmation.service.ConfirmationInput;
import com.iortatechnxt.brokerverse.eb.confirmation.service.ConfirmationService;
import com.iortatechnxt.brokerverse.eb.confirmation.service.PlacementTrigger;
import com.iortatechnxt.brokerverse.eb.cycle.api.dto.CycleDtos.AccountResponse;
import com.iortatechnxt.brokerverse.eb.document.api.EbUploads;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The client's confirmation of a cycle and Trigger Placement (FR-EB-046). */
@RestController
@RequestMapping("/api/v1/eb")
@Transactional
public class ConfirmationController {

  private static final String VIEW = "hasAuthority('EB_VIEW')";
  private static final String MARKET = "hasAuthority('EB_MARKET')";

  private final ConfirmationService confirmations;
  private final PlacementTrigger trigger;
  private final EbRecords records;
  private final ObjectMapper json;

  /**
   * Creates the controller.
   *
   * @param confirmations confirmations
   * @param trigger placement trigger
   * @param records cycle look-up
   * @param json JSON field of the confirmation form
   */
  public ConfirmationController(
      ConfirmationService confirmations,
      PlacementTrigger trigger,
      EbRecords records,
      ObjectMapper json) {
    this.confirmations = confirmations;
    this.trigger = trigger;
    this.records = records;
    this.json = json;
  }

  /**
   * The confirmations of a cycle.
   *
   * @param cycleId cycle
   * @param companyId company
   * @return confirmations, latest first
   */
  @GetMapping("/cycles/{cycleId}/confirmations")
  @PreAuthorize(VIEW)
  public List<ConfirmationResponse> list(@PathVariable Long cycleId, @RequestParam Long companyId) {
    return confirmations.ofCycle(companyId, cycleId).stream()
        .map(ConfirmationResponse::from)
        .toList();
  }

  /**
   * Records the client's confirmation with its evidence.
   *
   * @param cycleId cycle
   * @param companyId company
   * @param confirmation the confirmation as JSON
   * @param file the evidence
   * @return the confirmation
   */
  @PostMapping(
      value = "/cycles/{cycleId}/confirmation",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MARKET)
  public ConfirmationResponse confirm(
      @PathVariable Long cycleId,
      @RequestParam Long companyId,
      @RequestParam String confirmation,
      @RequestParam(required = false) MultipartFile file) {
    ConfirmationRequest body = EbUploads.read(json, confirmation, ConfirmationRequest.class);
    return ConfirmationResponse.from(
        confirmations.confirm(
            companyId,
            cycleId,
            new ConfirmationInput(
                body.channel(),
                body.confirmedOn(),
                body.remarks(),
                body.choices(),
                EbUploads.file(file))));
  }

  /**
   * Voids the active confirmation.
   *
   * @param cycleId cycle
   * @param companyId company
   * @param request reason
   * @return the voided confirmation
   */
  @PostMapping("/cycles/{cycleId}/confirmation/void")
  @PreAuthorize(MARKET)
  public ConfirmationResponse voidConfirmation(
      @PathVariable Long cycleId, @RequestParam Long companyId, @RequestBody VoidRequest request) {
    return ConfirmationResponse.from(
        confirmations.voidConfirmation(companyId, cycleId, request.reason()));
  }

  /**
   * Triggers the placement: the accounts of the confirmed lines are created and submitted.
   *
   * @param cycleId cycle
   * @param companyId company
   * @return the accounts
   */
  @PostMapping("/cycles/{cycleId}/trigger-placement")
  @PreAuthorize(MARKET)
  public List<AccountResponse> triggerPlacement(
      @PathVariable Long cycleId, @RequestParam Long companyId) {
    EbCycle cycle = records.cycle(companyId, cycleId);
    return trigger.trigger(companyId, cycleId).stream()
        .map(a -> AccountResponse.from(a, cycle.getId(), cycle.getCycleNo()))
        .toList();
  }
}
