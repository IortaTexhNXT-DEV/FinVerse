package com.iortatechnxt.brokerverse.renewal.channel.service;

import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelStatus;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The MFT simulator of SIT and UAT: it accepts a file put in the outbound folder of an insurer and
 * reports it delivered (picked up by the insurer) at the next status check; {@value
 * ChannelGateways#OUTAGE} listing MFT simulates an outage.
 */
@Component
public class MftSimulator implements ChannelGateway {

  private final SystemParameterService parameters;

  /**
   * Creates the simulator.
   *
   * @param parameters outage setting
   */
  public MftSimulator(SystemParameterService parameters) {
    this.parameters = parameters;
  }

  @Override
  public String channel() {
    return ChannelGateways.MFT;
  }

  @Override
  public boolean live() {
    return false;
  }

  @Override
  public Reply transmit(Transmission t) {
    if (down()) {
      return Reply.refused(UNAVAILABLE, "MFT service is currently unavailable");
    }
    if (t.content() == null || t.content().length == 0 || t.fileName() == null) {
      return Reply.refused(REJECTED, "The transfer has no file");
    }
    return Reply.accepted("MFTSIM-" + t.messageNo());
  }

  @Override
  public Optional<Report> status(ChannelMessage m) {
    return m.getStatus() == ChannelStatus.SUBMITTED || m.getStatus() == ChannelStatus.SENT
        ? Optional.of(new Report(ChannelStatus.DELIVERED, "Picked up by the insurer"))
        : Optional.empty();
  }

  @Override
  public Reply check() {
    return down()
        ? Reply.refused(UNAVAILABLE, "MFT service is currently unavailable")
        : Reply.accepted("MFT simulator");
  }

  private boolean down() {
    return parameters.items(ChannelGateways.OUTAGE).stream()
        .anyMatch(c -> ChannelGateways.MFT.equals(c.strip()));
  }
}
