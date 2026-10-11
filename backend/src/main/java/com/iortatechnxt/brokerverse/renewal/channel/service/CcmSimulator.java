package com.iortatechnxt.brokerverse.renewal.channel.service;

import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelStatus;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * The CCM simulator of SIT and UAT: it accepts a communication with valid recipients and returns a
 * transaction reference; the next status check reports it Sent, the one after Delivered; an address
 * of the domain bounce.invalid is reported Failed; {@value ChannelGateways#OUTAGE} listing CCM
 * simulates an outage. It keeps no copy of the documents.
 */
@Component
public class CcmSimulator implements ChannelGateway {

  /** The CCM transaction reference: TRN- and the message number, as a business reference. */
  static final String REFERENCE_PREFIX = "TRN-";

  static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final SystemParameterService parameters;

  /**
   * Creates the simulator.
   *
   * @param parameters outage setting
   */
  public CcmSimulator(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  @Override
  public String channel() {
    return ChannelGateways.CCM;
  }

  @Override
  public boolean live() {
    return false;
  }

  @Override
  public Reply transmit(Transmission t) {
    if (down()) {
      return Reply.refused(UNAVAILABLE, "CCM service is currently unavailable");
    }
    boolean invalid =
        t.to().isEmpty()
            || t.to().stream().anyMatch(a -> !EMAIL.matcher(a).matches())
            || t.cc().stream().anyMatch(a -> !EMAIL.matcher(a).matches());
    if (invalid) {
      return Reply.refused(INVALID_RECIPIENT, "One or more recipient email addresses are invalid");
    }
    if (t.content() == null || t.content().length == 0) {
      return Reply.refused(REJECTED, "The communication has no file");
    }
    return Reply.accepted(REFERENCE_PREFIX + t.messageNo());
  }

  @Override
  public Optional<Report> status(ChannelMessage m) {
    if (m.getExternalRef() == null) {
      return Optional.empty();
    }
    if (m.getRecipientsTo() != null && m.getRecipientsTo().contains("@bounce.invalid")) {
      return Optional.of(new Report(ChannelStatus.FAILED, "Mailbox unavailable"));
    }
    return switch (m.getStatus()) {
      case SUBMITTED -> Optional.of(new Report(ChannelStatus.SENT, "Sent by CCM"));
      case SENT -> Optional.of(new Report(ChannelStatus.DELIVERED, "Delivered to the mailbox"));
      default -> Optional.empty();
    };
  }

  @Override
  public Reply check() {
    return down()
        ? Reply.refused(UNAVAILABLE, "CCM service is currently unavailable")
        : Reply.accepted("CCM simulator");
  }

  private boolean down() {
    return parameters.items(ChannelGateways.OUTAGE).stream()
        .anyMatch(c -> ChannelGateways.CCM.equals(c.strip()));
  }
}
