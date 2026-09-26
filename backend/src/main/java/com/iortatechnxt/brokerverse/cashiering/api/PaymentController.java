package com.iortatechnxt.brokerverse.cashiering.api;

import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.IntakeResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.PaymentResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.PrebookedResponse;
import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.PreviewRequest;
import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.ReasonBody;
import com.iortatechnxt.brokerverse.cashiering.api.dto.PaymentDtos.ReceivePaymentRequest;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.MatchCategory;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Prebooked;
import com.iortatechnxt.brokerverse.cashiering.service.AutomatchService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeTarget;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentPreviewService;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentPreviewService.Preview;
import com.iortatechnxt.brokerverse.cashiering.service.PrebookedService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment intake (CSHID.008/020): the over-the-counter screen with its live application preview,
 * the payments received with their matching result, the pre-booked queue and the automatch run.
 */
@RestController
@RequestMapping("/api/v1/cashiering")
public class PaymentController {

  private final PaymentIntakeService intake;
  private final PaymentPreviewService preview;
  private final PaymentRepository payments;
  private final PrebookedService prebooked;
  private final AutomatchService automatch;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param intake payment intake
   * @param preview preview
   * @param payments payments
   * @param prebooked pre-booked queue
   * @param automatch automatch
   * @param clock clock
   */
  public PaymentController(
      PaymentIntakeService intake,
      PaymentPreviewService preview,
      PaymentRepository payments,
      PrebookedService prebooked,
      AutomatchService automatch,
      Clock clock) {
    this.intake = intake;
    this.preview = preview;
    this.payments = payments;
    this.prebooked = prebooked;
    this.automatch = automatch;
    this.clock = clock;
  }

  /**
   * Previews the matching and the application by component of a payment.
   *
   * @param request references and amount
   * @return preview
   */
  @PostMapping("/payments/preview")
  @PreAuthorize(CashAccess.RECEIPT)
  public Preview preview(@Valid @RequestBody PreviewRequest request) {
    return preview.preview(
        request.companyId(),
        request.references(),
        request.amount(),
        request.currency(),
        request.date() == null ? LocalDate.now(clock) : request.date());
  }

  /**
   * Receives an over-the-counter payment: AR, matching and application.
   *
   * @param request payment
   * @return outcome
   */
  @PostMapping("/payments")
  @PreAuthorize(CashAccess.RECEIPT)
  public IntakeResponse receive(@Valid @RequestBody ReceivePaymentRequest request) {
    return IntakeResponse.from(
        intake.receive(
            new IntakeTarget(
                request.companyId(),
                request.branchId(),
                request.arClass() == null ? "OTC" : request.arClass(),
                ReceiptSource.OTC),
            new PaymentIntake(
                PaymentChannel.OTC,
                null,
                "OTC:" + UUID.randomUUID(),
                null,
                null,
                request.references() == null ? List.of() : request.references(),
                new PaymentIntake.Payor(request.payorCode(), request.payorName()),
                request.assuredName(),
                new PaymentIntake.Money(
                    request.amount(), request.currency(), request.paymentDate()),
                new PaymentIntake.Tender(
                    request.mode(), request.checkNo(), request.checkBank(), null, false))));
  }

  /**
   * Payments received, filtered.
   *
   * @param companyId company
   * @param channel channel
   * @param category matching result
   * @param batchRef upload job
   * @param page page
   * @param size size
   * @return payments, newest first
   */
  @GetMapping("/payments")
  @PreAuthorize(CashAccess.VIEW)
  public PageResponse<PaymentResponse> payments(
      @RequestParam Long companyId,
      @RequestParam(required = false) PaymentChannel channel,
      @RequestParam(required = false) MatchCategory category,
      @RequestParam(required = false) String batchRef,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        payments.search(companyId, channel, category, batchRef, CashAccess.page(page, size)),
        PaymentResponse::from);
  }

  /**
   * The pre-booked queue.
   *
   * @param companyId company
   * @param status OPEN, APPLIED or RELEASED
   * @param page page
   * @param size size
   * @return items, oldest first
   */
  @GetMapping("/prebooked")
  @PreAuthorize(CashAccess.VIEW)
  public PageResponse<PrebookedResponse> prebooked(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = Prebooked.OPEN) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    LocalDate today = LocalDate.now(clock);
    return PageResponse.of(
        prebooked.list(companyId, status, CashAccess.page(page, size)),
        p -> PrebookedResponse.from(p, today));
  }

  /**
   * Re-matches one pre-booked payment now.
   *
   * @param id item
   * @return the item
   */
  @PostMapping("/prebooked/{id}/rematch")
  @PreAuthorize(CashAccess.APPLY)
  public PrebookedResponse rematch(@PathVariable Long id) {
    return PrebookedResponse.from(prebooked.rematchNow(id), LocalDate.now(clock));
  }

  /**
   * Releases a pre-booked payment to the unapplied workbench.
   *
   * @param id item
   * @param body reason
   * @return the item
   */
  @PostMapping("/prebooked/{id}/release")
  @PreAuthorize(CashAccess.APPLY)
  public PrebookedResponse release(@PathVariable Long id, @Valid @RequestBody ReasonBody body) {
    return PrebookedResponse.from(prebooked.release(id, body.reason()), LocalDate.now(clock));
  }

  /**
   * Re-runs the pre-booked matching and the automatch now (CSHID.020 "re-run manual").
   *
   * @return items applied per run
   */
  @PostMapping("/matching/run")
  @PreAuthorize(CashAccess.APPLY)
  public Map<String, Integer> runMatching() {
    LocalDate today = LocalDate.now(clock);
    return Map.of("prebooked", prebooked.rematchAll(today), "automatch", automatch.run(today));
  }
}
