package com.iortatechnxt.brokerverse.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * The user directory of the Enterprise SSO platform through its SCIM 2.0 interface ({@code GET
 * /Users?filter=externalId eq "<Windows ID>"}), at the address and with the token of {@link
 * IdentityProperties}. Until BDOI IT gives the connection, every request answers that the platform
 * is not connected.
 */
@Service
@ConditionalOnProperty(
    name = "brokerverse.identity.simulator",
    havingValue = "false",
    matchIfMissing = true)
public class ScimEnterpriseDirectory implements EnterpriseDirectory {

  private static final Logger LOG = LoggerFactory.getLogger(ScimEnterpriseDirectory.class);
  private static final String UNAVAILABLE = "DIRECTORY_UNAVAILABLE";

  private final IdentityProperties properties;
  private final RestClient http;

  /**
   * Creates the client.
   *
   * @param properties connection
   */
  public ScimEnterpriseDirectory(IdentityProperties properties) {
    this.properties = properties;
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(properties.timeout());
    factory.setReadTimeout(properties.timeout());
    this.http = RestClient.builder().requestFactory(factory).build();
  }

  @Override
  public Optional<DirectoryAccount> find(String windowsId) {
    if (!properties.directoryConfigured()) {
      throw new IdentityRefused(UNAVAILABLE, "The Enterprise SSO platform is not connected yet");
    }
    String base = properties.directoryUrl().trim().replaceAll("/+$", "");
    String filter = "externalId eq \"" + windowsId.replace("\"", "") + "\"";
    JsonNode answer;
    try {
      answer =
          http.get()
              .uri(
                  URI.create(
                      base + "/Users?filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8)))
              .accept(MediaType.valueOf("application/scim+json"), MediaType.APPLICATION_JSON)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.directoryToken())
              .retrieve()
              .body(JsonNode.class);
    } catch (RestClientException ex) {
      LOG.warn("Enterprise SSO directory not reached: {}", ex.getMessage());
      throw new IdentityRefused(
          UNAVAILABLE, "The Enterprise SSO platform could not be reached", ex);
    }
    JsonNode first = answer == null ? null : answer.path("Resources").path(0);
    return first == null || first.isMissingNode()
        ? Optional.empty()
        : Optional.of(ScimUsers.read(first));
  }

  @Override
  public String name() {
    return "Enterprise SSO platform";
  }
}
