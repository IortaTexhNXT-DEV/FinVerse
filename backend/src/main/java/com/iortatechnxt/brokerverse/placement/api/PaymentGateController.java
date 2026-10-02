package com.iortatechnxt.brokerverse.placement.api;

import com.iortatechnxt.brokerverse.placement.api.dto.ClientConfirmationRequest;
import com.iortatechnxt.brokerverse.placement.api.dto.GateResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.GateRuleResponse;
import com.iortatechnxt.brokerverse.placement.api.dto.NoteRequest;
import com.iortatechnxt.brokerverse.placement.service.PaymentConfirmationSweep;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService.ClientConfirmation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The payment gate (BRD 2.3.1, BRNB.068/114): rules, the gate of an account with its evidence,
 * client confirmation, release of direct-payment accounts and an on-demand sweep of the payment
 * confirmation sources.
 */
@RestController
@RequestMapping("/api/v1/placement/gate")
public class PaymentGateController {

  private static final String BILLING = "hasAuthority('BILLING_MANAGE')";

  private final PaymentGateService gate;
  private final PaymentConfirmationSweep sweep;

  /**
   * Creates the controller.
   *
   * @param gate payment gate
   * @param sweep payment confirmation sweep
   */
  public PaymentGateController(PaymentGateService gate, PaymentConfirmationSweep sweep) {
    this.gate = gate;
    this.sweep = sweep;
  }

  /**
   * The payment gate rules.
   *
   * @return rules, highest priority first
   */
  @GetMapping("/rules")
  @PreAuthorize(PlacementController.VIEW)
  public List<GateRuleResponse> rules() {
    return gate.rules().stream().map(GateRuleResponse::from).toList();
  }

  /**
   * The gate of an account.
   *
   * @param arn Account Reference Number
   * @return rule, position and evidence
   */
  @GetMapping("/{arn}")
  @PreAuthorize(PlacementController.VIEW)
  public GateResponse view(@PathVariable String arn) {
    return GateResponse.from(gate.view(arn));
  }

  /**
   * Records the client's confirmation and opens the gate (Other Lines).
   *
   * @param arn account awaiting confirmation
   * @param request channel, remarks and document
   * @return the gate
   */
  @PostMapping("/{arn}/client-confirmation")
  @PreAuthorize(BILLING)
  public GateResponse confirmClient(
      @PathVariable String arn, @Valid @RequestBody ClientConfirmationRequest request) {
    gate.confirmClient(
        arn, new ClientConfirmation(request.channel(), request.remarks(), request.attachmentId()));
    return GateResponse.from(gate.view(arn));
  }

  /**
   * Releases a direct-payment account still awaiting payment (BRNB.114).
   *
   * @param arn account
   * @param request remarks
   * @return the gate
   */
  @PostMapping("/{arn}/direct-payment")
  @PreAuthorize(BILLING)
  public GateResponse confirmDirect(
      @PathVariable String arn, @Valid @RequestBody NoteRequest request) {
    gate.confirmDirect(arn, request.comment());
    return GateResponse.from(gate.view(arn));
  }

  /**
   * Applies the payments confirmed by every source to the company's accounts awaiting payment.
   *
   * @param companyId company
   * @return number of gates opened
   */
  @PostMapping("/sweep")
  @PreAuthorize(BILLING)
  public Map<String, Integer> sweep(@RequestParam Long companyId) {
    return Map.of("opened", sweep.sweep(companyId));
  }
}
