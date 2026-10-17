package com.iortatechnxt.brokerverse.security.sso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.security.service.sso.SamlResponseValidator;
import com.iortatechnxt.brokerverse.security.service.sso.SsoException;
import com.iortatechnxt.brokerverse.security.service.sso.SsoIdentity;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The SAML Response checks: signature with the configured key only, signature wrapping, issuer,
 * audience, recipient, validity, request correlation and hostile XML.
 */
class SamlResponseValidatorTest {

  private static final String AUDIENCE = "https://bibs.test";
  private static final String ACS = "https://bibs.test/api/v1/auth/sso/saml/acs";
  private static final String NS = "urn:oasis:names:tc:SAML:2.0:assertion";
  private static final Instant NOW = Instant.parse("2026-09-28T03:00:00Z");

  private final SamlTestIdp idp = new SamlTestIdp();

  private SamlResponseValidator validator(SamlTestIdp keyOwner) throws Exception {
    String pem = keyOwner.publicKeyPem().replaceAll("-----[A-Z ]+-----", "");
    PublicKey key =
        KeyFactory.getInstance("RSA")
            .generatePublic(new X509EncodedKeySpec(Base64.getMimeDecoder().decode(pem)));
    return new SamlResponseValidator(
        key,
        SamlTestIdp.ENTITY_ID,
        AUDIENCE,
        ACS,
        Duration.ofMinutes(2),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static SamlTestIdp.Claims claims(String username) {
    return new SamlTestIdp.Claims("_req1", username, AUDIENCE, ACS, NOW, List.of("finance"));
  }

  private SsoIdentity validate(SamlTestIdp keyOwner, String xml) throws Exception {
    return validator(keyOwner)
        .validate(xml.getBytes(StandardCharsets.UTF_8), "_req1"::equals, "", "groups");
  }

  @Test
  void aSignedAssertionGivesTheIdentity() throws Exception {
    SsoIdentity identity = validate(idp, idp.signAssertion(SamlTestIdp.xml(claims("jdoe"))));
    assertThat(identity.username()).isEqualTo("jdoe");
    assertThat(identity.groups()).containsExactly("finance");
  }

  @Test
  void anUnsignedOrForeignSignedResponseIsRefused() {
    assertThatThrownBy(() -> validate(idp, SamlTestIdp.xml(claims("jdoe"))))
        .isInstanceOf(SsoException.class)
        .hasMessageContaining("signed");
    SamlTestIdp other = new SamlTestIdp();
    assertThatThrownBy(() -> validate(idp, other.signAssertion(SamlTestIdp.xml(claims("jdoe")))))
        .isInstanceOf(SsoException.class)
        .hasMessageContaining("signature");
  }

  @Test
  void aChangedAssertionIsRefused() {
    String signed = idp.signAssertion(SamlTestIdp.xml(claims("jdoe")));
    assertThatThrownBy(
            () -> validate(idp, signed.replace(">jdoe</saml:NameID>", ">admin</saml:NameID>")))
        .isInstanceOf(SsoException.class)
        .hasMessageContaining("signature");
  }

  @Test
  void signatureWrappingIsRefused() throws Exception {
    String signed = idp.signAssertion(SamlTestIdp.xml(claims("jdoe")));
    // A second, unsigned assertion for another user next to the signed one.
    Document twoAssertions = SamlTestIdp.parse(signed);
    Element original = (Element) twoAssertions.getElementsByTagNameNS(NS, "Assertion").item(0);
    Element forged = (Element) original.cloneNode(true);
    forged.setAttribute("ID", "_forged");
    forged.removeChild(
        forged.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature").item(0));
    forged.getElementsByTagNameNS(NS, "NameID").item(0).setTextContent("admin");
    original.getParentNode().insertBefore(forged, original);
    assertThatThrownBy(() -> validate(idp, SamlTestIdp.serialise(twoAssertions)))
        .isInstanceOf(SsoException.class)
        .hasMessageContaining("exactly one Assertion");

    // The signed assertion hidden inside another element, a forged one in its place.
    Document moved = SamlTestIdp.parse(signed);
    Element signedAssertion = (Element) moved.getElementsByTagNameNS(NS, "Assertion").item(0);
    Node response = signedAssertion.getParentNode();
    Element wrapper =
        moved.createElementNS("urn:oasis:names:tc:SAML:2.0:protocol", "samlp:Extensions");
    response.replaceChild(wrapper, signedAssertion);
    wrapper.appendChild(signedAssertion);
    assertThatThrownBy(() -> validate(idp, SamlTestIdp.serialise(moved)))
        .isInstanceOf(SsoException.class)
        .hasMessageContaining("not a child of the Response");
  }

  @Test
  void theAudienceRecipientRequestAndValidityAreChecked() {
    SamlTestIdp.Claims other =
        new SamlTestIdp.Claims("_req1", "jdoe", "https://other.test", ACS, NOW, List.of());
    assertThatThrownBy(() -> validate(idp, idp.signAssertion(SamlTestIdp.xml(other))))
        .hasMessageContaining("audience");
    SamlTestIdp.Claims recipient =
        new SamlTestIdp.Claims("_req1", "jdoe", AUDIENCE, "https://evil.test/acs", NOW, List.of());
    assertThatThrownBy(() -> validate(idp, idp.signAssertion(SamlTestIdp.xml(recipient))))
        .isInstanceOf(SsoException.class);
    SamlTestIdp.Claims unknownRequest =
        new SamlTestIdp.Claims("_other", "jdoe", AUDIENCE, ACS, NOW, List.of());
    assertThatThrownBy(() -> validate(idp, idp.signAssertion(SamlTestIdp.xml(unknownRequest))))
        .hasMessageContaining("InResponseTo");
    SamlTestIdp.Claims old =
        new SamlTestIdp.Claims("_req1", "jdoe", AUDIENCE, ACS, NOW.minusSeconds(3600), List.of());
    assertThatThrownBy(() -> validate(idp, idp.signAssertion(SamlTestIdp.xml(old))))
        .hasMessageContaining("expired");
  }

  @Test
  void aDoctypeIsRefused() {
    String hostile =
        "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
            + "<r>&x;</r>";
    assertThatThrownBy(() -> validate(idp, hostile))
        .isInstanceOf(SsoException.class)
        .hasMessageContaining("not readable");
  }
}
