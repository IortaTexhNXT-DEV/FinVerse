package com.iortatechnxt.brokerverse.renewal.channel.api;

import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessageRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The deliveries of a renewal account (FRRN.022.01, FRRN.023.02): each document handed to CCM or
 * MFT with its file name, CCM transaction reference and delivery status.
 */
@RestController
@RequestMapping("/api/v1/renewal/candidates/{ref}/deliveries")
@PreAuthorize("hasAuthority('RNW_VIEW')")
public class RenewalDeliveriesController {

  private final RenewalRecords records;
  private final ChannelMessageRepository messages;

  /**
   * Creates the controller.
   *
   * @param records renewals (data scope)
   * @param messages channel messages
   */
  public RenewalDeliveriesController(RenewalRecords records, ChannelMessageRepository messages) {
    this.records = records;
    this.messages = messages;
  }

  /**
   * The deliveries of a renewal account, newest first.
   *
   * @param companyId company
   * @param ref renewal reference
   * @return deliveries
   */
  @GetMapping
  @Transactional(readOnly = true)
  public List<Delivery> list(@RequestParam Long companyId, @PathVariable String ref) {
    return messages.findByCandidateIdOrderByIdDesc(records.get(companyId, ref).getId()).stream()
        .map(Delivery::of)
        .toList();
  }

  /**
   * A delivery.
   *
   * @param messageNo message
   * @param channel CCM or MFT
   * @param docKind document
   * @param docRef document reference
   * @param fileName file name
   * @param recipients recipients
   * @param status status code
   * @param statusLabel status
   * @param externalRef CCM transaction reference
   * @param error last error
   * @param queuedAt queued at
   * @param sentAt sent at
   * @param deliveredAt delivered at
   * @param queuedBy queued by
   */
  public record Delivery(
      String messageNo,
      String channel,
      String docKind,
      String docRef,
      String fileName,
      String recipients,
      String status,
      String statusLabel,
      String externalRef,
      String error,
      Instant queuedAt,
      Instant sentAt,
      Instant deliveredAt,
      String queuedBy) {

    static Delivery of(ChannelMessage m) {
      return new Delivery(
          m.getMessageNo(),
          m.getChannel(),
          m.getDocKind(),
          m.getDocRef(),
          m.getFileName(),
          m.getRecipientsTo(),
          m.getStatus().name(),
          m.getStatus().label(),
          m.getExternalRef(),
          m.getLastError(),
          m.getCreatedAt(),
          m.getSentAt(),
          m.getDeliveredAt(),
          m.getCreatedBy());
    }
  }
}
