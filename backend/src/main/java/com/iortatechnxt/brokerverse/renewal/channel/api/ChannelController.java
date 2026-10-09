package com.iortatechnxt.brokerverse.renewal.channel.api;

import com.iortatechnxt.brokerverse.common.api.ContentDispositions;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelMonitor;
import com.iortatechnxt.brokerverse.renewal.channel.service.ChannelService;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Channel Monitor: the CCM and MFT messages, their delivery history, resends and connections. */
@RestController
@RequestMapping("/api/v1/renewal/channels")
@PreAuthorize("hasAuthority('RNW_CHANNEL_MONITOR')")
public class ChannelController {

  private final ChannelService channels;
  private final ChannelMonitor monitor;

  /**
   * Creates the controller.
   *
   * @param channels messages
   * @param monitor listing, error report and settings
   */
  public ChannelController(ChannelService channels, ChannelMonitor monitor) {
    this.channels = channels;
    this.monitor = monitor;
  }

  /**
   * The messages, latest first.
   *
   * @param companyId company
   * @param channel channel
   * @param status status
   * @param kind kind of document
   * @param search reference
   * @return messages
   */
  @GetMapping
  public List<ChannelMonitor.Row> list(
      @RequestParam Long companyId,
      @RequestParam(required = false) String channel,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String kind,
      @RequestParam(required = false) String search) {
    return monitor.list(companyId, new ChannelMonitor.Filter(channel, status, kind, search));
  }

  /**
   * The delivery history of a message.
   *
   * @param companyId company
   * @param messageNo message
   * @return events
   */
  @GetMapping("/{messageNo}/history")
  public List<Event> history(@RequestParam Long companyId, @PathVariable String messageNo) {
    return channels.history(companyId, messageNo).stream()
        .map(
            e ->
                new Event(
                    e.getStatus().name(),
                    e.getStatus().label(),
                    e.getDetail(),
                    e.getCreatedAt(),
                    e.getCreatedBy()))
        .toList();
  }

  /**
   * Resends a message (a new transmission of the same document).
   *
   * @param companyId company
   * @param messageNo message
   * @return the new message number and the outcome
   */
  @PostMapping("/{messageNo}/resend")
  public Outcome resend(@RequestParam Long companyId, @PathVariable String messageNo) {
    ChannelService.Sent sent = channels.resend(companyId, messageNo);
    return new Outcome(
        sent.message().getMessageNo(), sent.message().getStatus().name(), sent.error());
  }

  /**
   * Cancels a message not transmitted.
   *
   * @param companyId company
   * @param messageNo message
   * @return the outcome
   */
  @PostMapping("/{messageNo}/cancel")
  public Outcome cancel(@RequestParam Long companyId, @PathVariable String messageNo) {
    var m = channels.cancel(companyId, messageNo);
    return new Outcome(m.getMessageNo(), m.getStatus().name(), null);
  }

  /**
   * The error report of the failed messages.
   *
   * @param companyId company
   * @param channel channel
   * @return workbook
   */
  @GetMapping("/error-report")
  public ResponseEntity<byte[]> errorReport(
      @RequestParam Long companyId, @RequestParam(required = false) String channel) {
    MessageFile f = monitor.errorReport(companyId, channel);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(f.mimeType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.attachment(f.fileName()))
        .body(f.content());
  }

  /**
   * The connection settings and the check of a channel.
   *
   * @param companyId company
   * @param channel CCM or MFT
   * @return settings and the answer of the channel
   */
  @GetMapping("/connection")
  public Connection connection(@RequestParam Long companyId, @RequestParam String channel) {
    return new Connection(monitor.settings(channel), channels.check(channel));
  }

  /**
   * The outcome of an action.
   *
   * @param messageNo message
   * @param status status
   * @param error BDOI's error message, null when submitted
   */
  public record Outcome(String messageNo, String status, String error) {}

  /**
   * A status of the delivery history.
   *
   * @param status status code
   * @param label status
   * @param detail detail
   * @param at time
   * @param by user or SYSTEM
   */
  public record Event(String status, String label, String detail, Instant at, String by) {}

  /**
   * A connection.
   *
   * @param settings settings
   * @param check answer of the channel
   */
  public record Connection(ChannelMonitor.Settings settings, ChannelService.Check check) {}
}
