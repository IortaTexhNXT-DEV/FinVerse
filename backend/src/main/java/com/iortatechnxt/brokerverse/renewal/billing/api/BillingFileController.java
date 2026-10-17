package com.iortatechnxt.brokerverse.renewal.billing.api;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.billing.service.BillingFiles;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessageRepository;
import com.iortatechnxt.brokerverse.renewal.domain.BillingFile;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CLPC billing files of the renewal accounts (FRRN.027): the list and the manual generation. */
@RestController
@RequestMapping("/api/v1/renewal/billing-files")
public class BillingFileController {

  private final BillingFiles files;
  private final ChannelMessageRepository messages;
  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param files billing files
   * @param messages delivery status
   * @param clock clock
   */
  public BillingFileController(BillingFiles files, ChannelMessageRepository messages, Clock clock) {
    this.files = files;
    this.messages = messages;
    this.clock = clock;
  }

  /**
   * The billing files, newest first.
   *
   * @param companyId company
   * @return files
   */
  @GetMapping
  @PreAuthorize("hasAuthority('RNW_PROCESS')")
  public List<View> list(@RequestParam Long companyId) {
    return files.list(companyId).stream().map(f -> view(companyId, f)).toList();
  }

  /**
   * Generates the billing files of a period.
   *
   * @param companyId company
   * @param body period
   * @return files
   */
  @PostMapping
  @PreAuthorize("hasAuthority('RNW_PROCESS')")
  public List<View> generate(@RequestParam Long companyId, @RequestBody Period body) {
    return files.manual(companyId, body.from(), body.to(), BusinessClock.today(clock)).stream()
        .map(f -> view(companyId, f))
        .toList();
  }

  private View view(Long companyId, BillingFile f) {
    String delivery =
        f.getMessageNo() == null
            ? f.getDeliveryStatus()
            : messages
                .findByCompanyIdAndMessageNo(companyId, f.getMessageNo())
                .map(ChannelMessage::getStatus)
                .map(Enum::name)
                .orElse(f.getDeliveryStatus());
    return new View(
        f.getId(),
        f.getFileName(),
        f.getLineCode(),
        f.isBuiltIn(),
        f.getRunKind(),
        f.getAccounts(),
        f.getAttachmentId(),
        delivery,
        f.getDeliveryError(),
        f.getLmsStatus(),
        f.getCreatedBy(),
        f.getCreatedAt());
  }

  /**
   * A period of at most 31 days.
   *
   * @param from first day
   * @param to last day
   */
  public record Period(LocalDate from, LocalDate to) {}

  /**
   * A billing file.
   *
   * @param id id
   * @param fileName file name
   * @param lineCode product line
   * @param builtIn amortized accounts
   * @param runKind CBG_HOME, FFY or MANUAL
   * @param accounts number of accounts
   * @param attachmentId stored file
   * @param deliveryStatus delivery status
   * @param deliveryError error
   * @param lmsStatus LMS upload
   * @param generatedBy user
   * @param generatedAt time
   */
  public record View(
      Long id,
      String fileName,
      String lineCode,
      boolean builtIn,
      String runKind,
      int accounts,
      Long attachmentId,
      String deliveryStatus,
      String deliveryError,
      String lmsStatus,
      String generatedBy,
      Instant generatedAt) {}
}
