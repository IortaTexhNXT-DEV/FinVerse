package com.iortatechnxt.brokerverse.security.sso;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An in-process OpenID Connect provider for the tests (JDK HTTP server): discovery, the token
 * endpoint (checks the client secret and the PKCE verifier of the code) and the key set; ID tokens
 * signed RS256.
 */
public final class OidcTestProvider implements AutoCloseable {

  /** Client id registered at the provider. */
  public static final String CLIENT_ID = "bibs-test-client";

  /** Client secret registered at the provider. */
  public static final String CLIENT_SECRET = "test-client-credential-0123456789";

  private final HttpServer server;
  private final RSAKey key;
  private final Map<String, Grant> codes = new ConcurrentHashMap<>();

  /** What a code stands for. */
  public record Grant(String username, String nonce, String challenge, String audience) {}

  /**
   * Starts the provider on a free local port.
   *
   * @throws Exception on error
   */
  public OidcTestProvider() throws Exception {
    key = new RSAKeyGenerator(2048).keyID("test-key").generate();
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/.well-known/openid-configuration", this::discovery);
    server.createContext("/token", this::token);
    server.createContext("/jwks", this::jwks);
    server.start();
  }

  /**
   * Issuer of the provider.
   *
   * @return issuer
   */
  public String issuer() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  /**
   * Registers a code the browser would bring back.
   *
   * @param code code
   * @param grant what it stands for
   */
  public void register(String code, Grant grant) {
    codes.put(code, grant);
  }

  @Override
  public void close() {
    server.stop(0);
  }

  private void discovery(HttpExchange exchange) throws IOException {
    String body =
        "{\"issuer\":\""
            + issuer()
            + "\",\"authorization_endpoint\":\""
            + issuer()
            + "/authorize\",\"token_endpoint\":\""
            + issuer()
            + "/token\",\"jwks_uri\":\""
            + issuer()
            + "/jwks\"}";
    send(exchange, 200, body);
  }

  private void jwks(HttpExchange exchange) throws IOException {
    send(exchange, 200, new JWKSet(key.toPublicJWK()).toString());
  }

  private void token(HttpExchange exchange) throws IOException {
    String expected =
        "Basic "
            + Base64.getEncoder()
                .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
    if (!expected.equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
      send(exchange, 401, "{\"error\":\"invalid_client\"}");
      return;
    }
    Map<String, String> form =
        form(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    Grant grant = codes.remove(form.getOrDefault("code", ""));
    if (grant == null || !grant.challenge().equals(s256(form.getOrDefault("code_verifier", "")))) {
      send(exchange, 400, "{\"error\":\"invalid_grant\"}");
      return;
    }
    try {
      Instant now = Instant.now();
      SignedJWT idToken =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256)
                  .keyID(key.getKeyID())
                  .type(JOSEObjectType.JWT)
                  .build(),
              new JWTClaimsSet.Builder()
                  .issuer(issuer())
                  .subject("idp-" + grant.username())
                  .audience(grant.audience())
                  .claim("preferred_username", grant.username())
                  .claim("nonce", grant.nonce())
                  .claim("groups", List.of("bibs-users"))
                  .issueTime(Date.from(now))
                  .expirationTime(Date.from(now.plusSeconds(300)))
                  .build());
      idToken.sign(new RSASSASigner(key));
      send(
          exchange,
          200,
          "{\"access_token\":\"x\",\"token_type\":\"Bearer\",\"id_token\":\""
              + idToken.serialize()
              + "\"}");
    } catch (Exception ex) {
      send(exchange, 500, "{\"error\":\"server_error\"}");
    }
  }

  /**
   * The S256 challenge of a verifier.
   *
   * @param verifier verifier
   * @return challenge
   */
  public static String s256(String verifier) {
    try {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(
              MessageDigest.getInstance("SHA-256")
                  .digest(verifier.getBytes(StandardCharsets.US_ASCII)));
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * The parameters of a query or form.
   *
   * @param text query or form text
   * @return parameters
   */
  public static Map<String, String> form(String text) {
    Map<String, String> values = new HashMap<>();
    for (String pair : text.split("&")) {
      int at = pair.indexOf('=');
      if (at > 0) {
        values.put(
            URLDecoder.decode(pair.substring(0, at), StandardCharsets.UTF_8),
            URLDecoder.decode(pair.substring(at + 1), StandardCharsets.UTF_8));
      }
    }
    return values;
  }

  private static void send(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(bytes);
    }
  }
}
