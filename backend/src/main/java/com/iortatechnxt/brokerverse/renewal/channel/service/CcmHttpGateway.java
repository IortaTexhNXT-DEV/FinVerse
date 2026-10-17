package com.iortatechnxt.brokerverse.renewal.channel.service;

import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessage;
import com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelStatus;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * The live CCM interface (FRRN.017.02, FRRN.022.02): the handoff payload of a communication (the
 * recipients, subject, text and the protected file with its password) posted to CCM, which answers
 * with its transaction reference; the delivery status is read back by that reference. The endpoint,
 * client identifier and key are settings; the connection details come from the IT team of the bank.
 */
@Component
public class CcmHttpGateway implements ChannelGateway {

  private static final String KEY_HEADER = "X-Api-Key";
  private static final Map<String, ChannelStatus> STATUSES =
      Map.of(
          "SENT", ChannelStatus.SENT,
          "DELIVERED", ChannelStatus.DELIVERED,
          "FAILED", ChannelStatus.FAILED,
          "BOUNCED", ChannelStatus.FAILED,
          "CANCELLED", ChannelStatus.CANCELLED);

  private final HttpConnection connection;
  private final SystemParameterService parameters;

  /**
   * Creates the gateway.
   *
   * @param parameters settings of the connection
   * @param environment access key
   */
  public CcmHttpGateway(SystemParameterService parameters, Environment environment) {
    this.parameters = parameters;
    this.connection =
        new HttpConnection(parameters, environment, "RNW_CCM_ENDPOINT", "RNW_CCM_KEY_SETTING");
  }

  @Override
  public String channel() {
    return ChannelGateways.CCM;
  }

  @Override
  public boolean live() {
    return true;
  }

  @Override
  public Reply transmit(Transmission t) {
    if (connection.endpoint().isEmpty()) {
      return Reply.refused(UNAVAILABLE, "The CCM endpoint is not set");
    }
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("clientId", parameters.text("RNW_CCM_CLIENT_ID", ""));
    payload.put("messageNo", t.messageNo());
    payload.put("to", t.to());
    payload.put("cc", t.cc());
    payload.put("subject", t.subject());
    payload.put("body", t.body());
    if (t.content() != null) {
      Map<String, Object> file = new LinkedHashMap<>();
      file.put("fileName", t.fileName());
      file.put("content", Base64.getEncoder().encodeToString(t.content()));
      file.put("password", t.password());
      payload.put("attachment", file);
    }
    try {
      Map<?, ?> answer =
          connection
              .client()
              .post()
              .uri(connection.endpoint() + "/communications")
              .header(KEY_HEADER, connection.key())
              .contentType(MediaType.APPLICATION_JSON)
              .body(payload)
              .retrieve()
              .body(Map.class);
      Object ref = answer == null ? null : answer.get("reference");
      return ref == null
          ? Reply.refused(REJECTED, "CCM gave no transaction reference")
          : Reply.accepted(ref.toString());
    } catch (HttpClientErrorException e) {
      return e.getResponseBodyAsString().contains(INVALID_RECIPIENT)
          ? Reply.refused(INVALID_RECIPIENT, "One or more recipient email addresses are invalid")
          : Reply.refused(REJECTED, "CCM refused the communication: " + e.getStatusCode());
    } catch (RestClientException e) {
      return Reply.refused(UNAVAILABLE, "CCM service is currently unavailable");
    }
  }

  @Override
  public Optional<Report> status(ChannelMessage m) {
    if (m.getExternalRef() == null || connection.endpoint().isEmpty()) {
      return Optional.empty();
    }
    try {
      Map<?, ?> answer =
          connection
              .client()
              .get()
              .uri(connection.endpoint() + "/communications/{ref}", m.getExternalRef())
              .header(KEY_HEADER, connection.key())
              .retrieve()
              .body(Map.class);
      return report(answer, m.getStatus());
    } catch (RestClientException e) {
      return Optional.empty();
    }
  }

  private static Optional<Report> report(Map<?, ?> answer, ChannelStatus current) {
    if (answer == null || answer.get("status") == null) {
      return Optional.empty();
    }
    ChannelStatus s = STATUSES.get(answer.get("status").toString());
    Object detail = answer.get("detail");
    return s == null || s == current
        ? Optional.empty()
        : Optional.of(new Report(s, detail == null ? null : detail.toString()));
  }

  @Override
  public Reply check() {
    if (connection.endpoint().isEmpty()) {
      return Reply.refused(UNAVAILABLE, "The CCM endpoint is not set");
    }
    try {
      connection
          .client()
          .get()
          .uri(connection.endpoint() + "/health")
          .header(KEY_HEADER, connection.key())
          .retrieve()
          .toBodilessEntity();
      return Reply.accepted("CCM reachable");
    } catch (RestClientException e) {
      return Reply.refused(UNAVAILABLE, "CCM service is currently unavailable");
    }
  }
}
