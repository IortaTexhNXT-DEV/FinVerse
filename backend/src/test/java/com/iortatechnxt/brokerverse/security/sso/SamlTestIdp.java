package com.iortatechnxt.brokerverse.security.sso;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * An in-process SAML identity provider for the tests: an RSA key pair and signed Responses with one
 * Assertion (enveloped RSA-SHA256 signature on the Assertion or on the Response).
 */
public final class SamlTestIdp {

  /** Entity id of the test provider. */
  public static final String ENTITY_ID = "https://idp.test/saml";

  private final KeyPair keys;

  /** Creates a provider with a new key pair. */
  public SamlTestIdp() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      this.keys = generator.generateKeyPair();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * The public key in PEM (the configured provider certificate of the tests).
   *
   * @return PEM
   */
  public String publicKeyPem() {
    return "-----BEGIN PUBLIC KEY-----\n"
        + Base64.getMimeEncoder().encodeToString(keys.getPublic().getEncoded())
        + "\n-----END PUBLIC KEY-----\n";
  }

  /** What the Response says. */
  public record Claims(
      String inResponseTo,
      String username,
      String audience,
      String acs,
      Instant now,
      List<String> groups) {}

  /**
   * The unsigned XML of a Response.
   *
   * @param c content
   * @return XML
   */
  public static String xml(Claims c) {
    String later = c.now().plusSeconds(300).toString();
    StringBuilder groups = new StringBuilder();
    c.groups()
        .forEach(
            g -> groups.append("<saml:AttributeValue>").append(g).append("</saml:AttributeValue>"));
    return "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\""
        + " xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" ID=\"_resp1\" Version=\"2.0\""
        + " IssueInstant=\""
        + c.now()
        + "\" Destination=\""
        + c.acs()
        + "\""
        + " InResponseTo=\""
        + c.inResponseTo()
        + "\">"
        + "<saml:Issuer>"
        + ENTITY_ID
        + "</saml:Issuer>"
        + "<samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/></samlp:Status>"
        + "<saml:Assertion ID=\"_assert1\" Version=\"2.0\" IssueInstant=\""
        + c.now()
        + "\">"
        + "<saml:Issuer>"
        + ENTITY_ID
        + "</saml:Issuer>"
        + "<saml:Subject><saml:NameID>"
        + c.username()
        + "</saml:NameID>"
        + "<saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">"
        + "<saml:SubjectConfirmationData NotOnOrAfter=\""
        + later
        + "\" Recipient=\""
        + c.acs()
        + "\" InResponseTo=\""
        + c.inResponseTo()
        + "\"/></saml:SubjectConfirmation></saml:Subject>"
        + "<saml:Conditions NotBefore=\""
        + c.now().minusSeconds(60)
        + "\" NotOnOrAfter=\""
        + later
        + "\">"
        + "<saml:AudienceRestriction><saml:Audience>"
        + c.audience()
        + "</saml:Audience></saml:AudienceRestriction>"
        + "</saml:Conditions>"
        + "<saml:AttributeStatement><saml:Attribute Name=\"groups\">"
        + groups
        + "</saml:Attribute></saml:AttributeStatement>"
        + "</saml:Assertion></samlp:Response>";
  }

  /**
   * A Response whose Assertion is signed.
   *
   * @param c content
   * @return the Base64 SAMLResponse
   */
  public String signedResponse(Claims c) {
    return Base64.getEncoder()
        .encodeToString(signAssertion(xml(c)).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Signs the Assertion of an XML Response (enveloped signature after its Issuer).
   *
   * @param xml Response XML
   * @return signed XML
   */
  public String signAssertion(String xml) {
    try {
      Document doc = parse(xml);
      Element assertion =
          (Element)
              doc.getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Assertion")
                  .item(0);
      assertion.setIdAttribute("ID", true);
      Element issuer =
          (Element)
              assertion
                  .getElementsByTagNameNS("urn:oasis:names:tc:SAML:2.0:assertion", "Issuer")
                  .item(0);
      sign(assertion, issuer.getNextSibling());
      return serialise(doc);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Parses XML (namespace aware).
   *
   * @param xml XML
   * @return document
   * @throws Exception on error
   */
  public static Document parse(String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory
        .newDocumentBuilder()
        .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Serialises a document.
   *
   * @param doc document
   * @return XML
   * @throws Exception on error
   */
  public static String serialise(Document doc) throws Exception {
    StringWriter out = new StringWriter();
    TransformerFactory.newInstance()
        .newTransformer()
        .transform(new DOMSource(doc), new StreamResult(out));
    return out.toString();
  }

  private void sign(Element element, org.w3c.dom.Node before) throws Exception {
    XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
    Reference reference =
        factory.newReference(
            "#" + element.getAttribute("ID"),
            factory.newDigestMethod(DigestMethod.SHA256, null),
            List.of(
                factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null),
                factory.newTransform(
                    CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null)),
            null,
            null);
    SignedInfo signedInfo =
        factory.newSignedInfo(
            factory.newCanonicalizationMethod(
                CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
            factory.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null),
            List.of(reference));
    DOMSignContext context = new DOMSignContext(keys.getPrivate(), element, before);
    factory.newXMLSignature(signedInfo, null).sign(context);
  }
}
