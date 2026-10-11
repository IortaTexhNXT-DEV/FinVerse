package com.iortatechnxt.brokerverse.integration.api;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Connectivity check of the system-to-system path (API gateway to {@code bibs-integration}):
 * answers with the calling client and the scopes of its token, so the gateway team can verify a
 * proxy, its token and the private route end to end. Guarded by the integration security chain
 * (rule {@code brokerverse.integration.security.apis.ping}).
 */
@RestController
@RequestMapping("/integration/v1")
public class IntegrationPingController {

  private static final String SCOPE_PREFIX = "SCOPE_";

  private final Clock clock;

  /**
   * Creates the controller.
   *
   * @param clock clock
   */
  public IntegrationPingController(Clock clock) {
    this.clock = clock;
  }

  /**
   * Answers the check.
   *
   * @param authentication validated gateway token and its scopes
   * @return client, scopes and server time
   */
  @GetMapping("/ping")
  public PingResponse ping(JwtAuthenticationToken authentication) {
    String client =
        Stream.of("client_id", "azp", "sub")
            .map(authentication.getToken()::getClaimAsString)
            .filter(value -> value != null && !value.isBlank())
            .findFirst()
            .orElse("unknown");
    List<String> scopes =
        authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith(SCOPE_PREFIX))
            .map(authority -> authority.substring(SCOPE_PREFIX.length()))
            .sorted()
            .toList();
    return new PingResponse("UP", client, scopes, clock.instant());
  }

  /**
   * Answer of the connectivity check.
   *
   * @param status always {@code UP}
   * @param client calling client (client id of the token)
   * @param scopes scopes of the token
   * @param serverTime time of the answer
   */
  public record PingResponse(
      String status, String client, List<String> scopes, Instant serverTime) {}
}
