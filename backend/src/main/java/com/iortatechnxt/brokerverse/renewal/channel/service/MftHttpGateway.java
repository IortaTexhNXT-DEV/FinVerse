package com.iortatechnxt.brokerverse.renewal.channel.service;

import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelStatus;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * The live MFT interface: a file put in the outbound folder of the insurer on the MFT server (HTTPS
 * file interface of the server); a file put is delivered. The endpoint, folders and key are
 * settings; the connection details come from the IT team of the bank.
 */
@Component
public class MftHttpGateway implements ChannelGateway {

  private static final String KEY_HEADER = "X-Api-Key";

  private final HttpConnection connection;
  private final SystemParameterService parameters;

  /**
   * Creates the gateway.
   *
   * @param parameters settings of the connection
   * @param environment access key
   */
  public MftHttpGateway(SystemParameterService parameters, Environment environment) {
    this.parameters = parameters;
    this.connection =
        new HttpConnection(parameters, environment, "RNW_MFT_ENDPOINT", "RNW_MFT_KEY_SETTING");
  }

  @Override
  public String channel() {
    return ChannelGateways.MFT;
  }

  @Override
  public boolean live() {
    return true;
  }

  @Override
  public Reply transmit(Transmission t) {
    if (connection.endpoint().isEmpty()) {
      return Reply.refused(UNAVAILABLE, "The MFT endpoint is not set");
    }
    String folder = parameters.text("RNW_MFT_OUTBOUND_FOLDER", "/outbound").strip();
    String insurer = t.to().isEmpty() ? "" : "/" + t.to().get(0);
    try {
      connection
          .client()
          .put()
          .uri(connection.endpoint() + folder + insurer + "/{file}", t.fileName())
          .header(KEY_HEADER, connection.key())
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .body(t.content())
          .retrieve()
          .toBodilessEntity();
      return Reply.accepted(folder + insurer + "/" + t.fileName());
    } catch (RestClientException e) {
      return Reply.refused(UNAVAILABLE, "MFT service is currently unavailable");
    }
  }

  @Override
  public Optional<Report> status(ChannelMessage m) {
    return m.getStatus() == ChannelStatus.SUBMITTED
        ? Optional.of(new Report(ChannelStatus.DELIVERED, "Put in the insurer's folder"))
        : Optional.empty();
  }

  @Override
  public Reply check() {
    if (connection.endpoint().isEmpty()) {
      return Reply.refused(UNAVAILABLE, "The MFT endpoint is not set");
    }
    try {
      connection
          .client()
          .get()
          .uri(connection.endpoint() + "/health")
          .header(KEY_HEADER, connection.key())
          .retrieve()
          .toBodilessEntity();
      return Reply.accepted("MFT reachable");
    } catch (RestClientException e) {
      return Reply.refused(UNAVAILABLE, "MFT service is currently unavailable");
    }
  }
}
