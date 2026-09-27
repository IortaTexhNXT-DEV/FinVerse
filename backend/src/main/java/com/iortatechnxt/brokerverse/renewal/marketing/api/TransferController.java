package com.iortatechnxt.brokerverse.renewal.marketing.api;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalTransfer;
import com.iortatechnxt.brokerverse.renewal.marketing.service.TransferService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Transfers of renewals between Marketing units (FR-RN-031, 032). */
@RestController
@RequestMapping("/api/v1/renewal/transfers")
@PreAuthorize("hasAuthority('RNW_ASSIGN')")
public class TransferController {

  private final TransferService transfers;
  private final RenewalCandidateRepository candidates;

  /**
   * Creates the controller.
   *
   * @param transfers transfers
   * @param candidates renewals (list columns)
   */
  public TransferController(TransferService transfers, RenewalCandidateRepository candidates) {
    this.transfers = transfers;
    this.candidates = candidates;
  }

  /**
   * Requests a transfer.
   *
   * @param companyId company
   * @param request renewal, receiving unit, reason and remarks
   * @return the request
   */
  @PostMapping
  public TransferView request(@RequestParam Long companyId, @RequestBody Request request) {
    RenewalTransfer t =
        transfers.request(
            companyId,
            request.renewalRef(),
            new TransferService.Request(request.toUnit(), request.reasonCode(), request.remarks()));
    return view(t, candidates.findById(t.getCandidateId()).orElseThrow());
  }

  /**
   * Requests received by the user's units.
   *
   * @param companyId company
   * @return requests
   */
  @GetMapping("/incoming")
  @Transactional(readOnly = true)
  public List<TransferView> incoming(@RequestParam Long companyId) {
    return views(transfers.incoming(companyId));
  }

  /**
   * Requests sent by the user's units.
   *
   * @param companyId company
   * @return requests
   */
  @GetMapping("/outgoing")
  @Transactional(readOnly = true)
  public List<TransferView> outgoing(@RequestParam Long companyId) {
    return views(transfers.outgoing(companyId));
  }

  /**
   * Accepts a request.
   *
   * @param id request
   * @param request remarks
   * @return the request
   */
  @PostMapping("/{id}/accept")
  public TransferView accept(
      @PathVariable Long id, @RequestBody(required = false) Remarks request) {
    RenewalTransfer t = transfers.decide(id, true, request == null ? null : request.remarks());
    return view(t, candidates.findById(t.getCandidateId()).orElseThrow());
  }

  /**
   * Declines a request.
   *
   * @param id request
   * @param request remarks
   * @return the request
   */
  @PostMapping("/{id}/decline")
  public TransferView decline(@PathVariable Long id, @RequestBody Remarks request) {
    RenewalTransfer t = transfers.decide(id, false, request.remarks());
    return view(t, candidates.findById(t.getCandidateId()).orElseThrow());
  }

  /**
   * Cancels a request (its requester).
   *
   * @param id request
   * @return the request
   */
  @PostMapping("/{id}/cancel")
  public TransferView cancel(@PathVariable Long id) {
    RenewalTransfer t = transfers.cancel(id);
    return view(t, candidates.findById(t.getCandidateId()).orElseThrow());
  }

  private List<TransferView> views(List<RenewalTransfer> list) {
    Map<Long, RenewalCandidate> byId =
        candidates.findAllById(list.stream().map(RenewalTransfer::getCandidateId).toList()).stream()
            .collect(Collectors.toMap(RenewalCandidate::getId, Function.identity()));
    return list.stream()
        .filter(t -> byId.containsKey(t.getCandidateId()))
        .map(t -> view(t, byId.get(t.getCandidateId())))
        .toList();
  }

  private static TransferView view(RenewalTransfer t, RenewalCandidate c) {
    return new TransferView(
        t.getId(),
        c.getRenewalRef(),
        c.getSnapshot().clientName(),
        c.getExpiryDate(),
        t.getFromUnit(),
        t.getToUnit(),
        t.getReasonCode(),
        t.getRemarks(),
        t.getStatus().name(),
        t.getCreatedBy(),
        t.getCreatedAt(),
        t.getDecidedBy(),
        t.getDecidedAt(),
        t.getDecisionRemarks());
  }

  /**
   * A transfer request.
   *
   * @param renewalRef renewal
   * @param toUnit receiving unit
   * @param reasonCode reason
   * @param remarks remarks
   */
  public record Request(String renewalRef, String toUnit, String reasonCode, String remarks) {}

  /**
   * Remarks of a decision.
   *
   * @param remarks remarks
   */
  public record Remarks(String remarks) {}

  /**
   * A transfer.
   *
   * @param id id
   * @param renewalRef renewal
   * @param clientName client
   * @param expiry expiry date
   * @param fromUnit sending unit
   * @param toUnit receiving unit
   * @param reasonCode reason
   * @param remarks remarks
   * @param status status
   * @param requestedBy requester
   * @param requestedAt request time
   * @param decidedBy decided by
   * @param decidedAt decision time
   * @param decisionRemarks decision remarks
   */
  public record TransferView(
      Long id,
      String renewalRef,
      String clientName,
      LocalDate expiry,
      String fromUnit,
      String toUnit,
      String reasonCode,
      String remarks,
      String status,
      String requestedBy,
      Instant requestedAt,
      String decidedBy,
      Instant decidedAt,
      String decisionRemarks) {}
}
