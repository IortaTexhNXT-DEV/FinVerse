package com.iortatechnxt.brokerverse.renewal.channel.service;

import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * The connection of a live channel interface, from its settings: the endpoint, the name of the
 * environment setting holding the access key and the time limit. The key is read from the
 * environment of the server, never from the database.
 */
final class HttpConnection {

  private static final int DEFAULT_TIMEOUT = 30;

  private final SystemParameterService parameters;
  private final Environment environment;
  private final String endpointKey;
  private final String secretKey;

  HttpConnection(
      SystemParameterService parameters,
      Environment environment,
      String endpointKey,
      String secretKey) {
    this.parameters = parameters;
    this.environment = environment;
    this.endpointKey = endpointKey;
    this.secretKey = secretKey;
  }

  /** The endpoint, without a trailing slash; empty when not set. */
  String endpoint() {
    String e = parameters.text(endpointKey, "").strip();
    return e.endsWith("/") ? e.substring(0, e.length() - 1) : e;
  }

  /** The access key from the environment, empty when not set. */
  String key() {
    String name = parameters.text(secretKey, "").strip();
    String value = name.isEmpty() ? null : environment.getProperty(name);
    return value == null ? "" : value;
  }

  /** A client with the time limit of the settings. */
  RestClient client() {
    Duration timeout =
        Duration.ofSeconds(parameters.intValue("RNW_CCM_TIMEOUT_SECONDS", DEFAULT_TIMEOUT));
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    factory.setReadTimeout(timeout);
    return RestClient.builder().requestFactory(factory).build();
  }
}
