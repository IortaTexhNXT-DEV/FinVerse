package com.iortatechnxt.brokerverse.security.service.sso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * SAML 2.0 service provider: the AuthnRequest of the HTTP-Redirect binding, the validation of the
 * provider's Response ({@link SamlResponseValidator}) posted to the assertion consumer service, and
 * the metadata to register BrokerVerse at the provider.
 */
@Component
public class SamlServiceProvider {

  /** Path of the assertion consumer service under the base address. */
  public static final String ACS_PATH = "/api/v1/auth/sso/saml/acs";

  private static final String POST_BINDING = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";
  private static final String UNSPECIFIED = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
  private static final String PEM_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
  private static final String FILE_PREFIX = "file:";

  private final SsoProperties properties;
  private final Clock clock;
  private final AtomicReference<SamlResponseValidator> validator = new AtomicReference<>();

  /**
   * Creates the service provider.
   *
   * @param properties single sign-on settings
   * @param clock clock
   */
  public SamlServiceProvider(SsoProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Whether a provider is configured.
   *
   * @return true when the provider and the base address are set
   */
  public boolean configured() {
    return properties.saml().configured() && !properties.base().isEmpty();
  }

  /**
   * The address that sends the browser to the provider with an AuthnRequest.
   *
   * @param requestId ID of the request (kept to check InResponseTo)
   * @param relayState state of the request
   * @return address
   */
  public String redirectUrl(String requestId, String relayState) {
    SsoProperties.Saml saml = properties.saml();
    String format =
        saml.nameIdFormat() == null || saml.nameIdFormat().isBlank()
            ? UNSPECIFIED
            : saml.nameIdFormat().trim();
    String request =
        "<samlp:AuthnRequest xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\""
            + " xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\""
            + requestId
            + "\" Version=\"2.0\" IssueInstant=\""
            + clock.instant().truncatedTo(ChronoUnit.SECONDS)
            + "\" Destination=\""
            + xml(saml.idpSsoUrl())
            + "\" AssertionConsumerServiceURL=\""
            + xml(acsUrl())
            + "\" ProtocolBinding=\""
            + POST_BINDING
            + "\"><saml:Issuer>"
            + xml(spEntityId())
            + "</saml:Issuer><samlp:NameIDPolicy Format=\""
            + xml(format)
            + "\" AllowCreate=\"false\"/></samlp:AuthnRequest>";
    String base = saml.idpSsoUrl().trim();
    return base
        + (base.contains("?") ? "&" : "?")
        + "SAMLRequest="
        + URLEncoder.encode(deflate(request), StandardCharsets.UTF_8)
        + "&RelayState="
        + URLEncoder.encode(relayState, StandardCharsets.UTF_8);
  }

  /**
   * Validates the Response posted by the provider.
   *
   * @param samlResponse the Base64 SAMLResponse form field
   * @param knownRequest tells (and uses) whether an InResponseTo is a pending request
   * @return the identity
   */
  public SsoIdentity validate(String samlResponse, Predicate<String> knownRequest) {
    byte[] xml;
    try {
      xml = Base64.getMimeDecoder().decode(samlResponse);
    } catch (IllegalArgumentException ex) {
      throw new SsoException(SsoException.INVALID, "SAMLResponse is not Base64", ex);
    }
    return validator()
        .validate(xml, knownRequest, properties.usernameClaim(), properties.groupsClaim());
  }

  /**
   * The metadata of BrokerVerse as a service provider.
   *
   * @return metadata XML
   */
  public String metadata() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\""
        + xml(spEntityId())
        + "\"><md:SPSSODescriptor AuthnRequestsSigned=\"false\" WantAssertionsSigned=\"true\""
        + " protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
        + "<md:AssertionConsumerService Binding=\""
        + POST_BINDING
        + "\" Location=\""
        + xml(acsUrl())
        + "\" index=\"0\" isDefault=\"true\"/></md:SPSSODescriptor></md:EntityDescriptor>";
  }

  /**
   * Our entity id: the configured one, else the base address.
   *
   * @return entity id
   */
  public String spEntityId() {
    String configured = properties.saml().spEntityId();
    return configured == null || configured.isBlank() ? properties.base() : configured.trim();
  }

  /**
   * The assertion consumer service.
   *
   * @return address
   */
  public String acsUrl() {
    return properties.base() + ACS_PATH;
  }

  private SamlResponseValidator validator() {
    SamlResponseValidator current = validator.get();
    if (current == null) {
      current =
          new SamlResponseValidator(
              publicKey(properties.saml().idpCertificate()),
              properties.saml().idpEntityId().trim(),
              spEntityId(),
              acsUrl(),
              properties.clockSkew(),
              clock);
      validator.compareAndSet(null, current);
    }
    return current;
  }

  /**
   * The public key of a PEM certificate or public key (or a {@code file:} path to one).
   *
   * @param configured PEM text or {@code file:} path
   * @return public key
   */
  static PublicKey publicKey(String configured) {
    String pem = configured.trim();
    try {
      if (pem.startsWith(FILE_PREFIX)) {
        pem =
            Files.readString(Path.of(pem.substring(FILE_PREFIX.length())), StandardCharsets.UTF_8);
      }
      if (pem.contains(PEM_CERTIFICATE)) {
        return CertificateFactory.getInstance("X.509")
            .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)))
            .getPublicKey();
      }
      byte[] der = Base64.getMimeDecoder().decode(pem.replaceAll("-----[A-Z ]+-----", ""));
      GeneralSecurityException last = null;
      for (String algorithm : List.of("RSA", "EC")) {
        try {
          return KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException ex) {
          last = ex;
        }
      }
      throw new IllegalStateException("The SAML provider key is not an RSA or EC key", last);
    } catch (IOException | GeneralSecurityException | IllegalArgumentException ex) {
      throw new IllegalStateException("The SAML provider certificate cannot be read", ex);
    }
  }

  private static String deflate(String xml) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    Deflater deflater = new Deflater(Deflater.DEFLATED, true);
    try (DeflaterOutputStream out = new DeflaterOutputStream(bytes, deflater)) {
      out.write(xml.getBytes(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    } finally {
      deflater.end();
    }
    return Base64.getEncoder().encodeToString(bytes.toByteArray());
  }

  private static String xml(String value) {
    return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
  }
}
