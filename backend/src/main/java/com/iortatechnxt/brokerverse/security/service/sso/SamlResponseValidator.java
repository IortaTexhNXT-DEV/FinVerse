package com.iortatechnxt.brokerverse.security.service.sso;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.xml.XMLConstants;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Validates a SAML 2.0 Response of the HTTP-POST binding (a plain class, unit tested on its own):
 *
 * <ol>
 *   <li>Parsed without DTDs, external entities or XInclude.
 *   <li>The root is a {@code samlp:Response} with status Success, its Destination (when present) is
 *       the assertion consumer service, and it answers a request we sent ({@code InResponseTo}).
 *   <li>It holds exactly one {@code saml:Assertion}, a direct child of the Response (an encrypted
 *       assertion is not supported). The Assertion, or the Response, carries an enveloped XML
 *       signature made with the provider's configured key (the KeyInfo of the message is never
 *       trusted), with exactly one reference that points at that element's ID, an ID that occurs
 *       once in the document (signature wrapping is refused). Validation runs with the JDK's secure
 *       validation (no weak algorithms, limited transforms).
 *   <li>The issuer is the configured provider; the Conditions hold the time now (with the clock
 *       skew) and an AudienceRestriction naming our entity id; a bearer SubjectConfirmation names
 *       the assertion consumer service, is still valid and answers the same request.
 *   <li>The user name is the configured attribute, else the NameID; groups from the configured
 *       attribute.
 * </ol>
 */
public class SamlResponseValidator {

  /** SAML 2.0 protocol namespace. */
  public static final String PROTOCOL = "urn:oasis:names:tc:SAML:2.0:protocol";

  /** SAML 2.0 assertion namespace. */
  public static final String ASSERTION = "urn:oasis:names:tc:SAML:2.0:assertion";

  private static final String DSIG = XMLSignature.XMLNS;
  private static final String SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";
  private static final String BEARER = "urn:oasis:names:tc:SAML:2.0:cm:bearer";
  private static final String ID = "ID";
  private static final String MAX_DEPTH_PROPERTY =
      "http://www.oracle.com/xml/jaxp/properties/maxElementDepth";
  private static final int MAX_DEPTH = 64;

  private final PublicKey idpKey;
  private final String idpEntityId;
  private final String spEntityId;
  private final String acsUrl;
  private final Duration skew;
  private final Clock clock;

  /**
   * Creates the validator.
   *
   * @param idpKey the provider's signing key
   * @param idpEntityId the provider's entity id
   * @param spEntityId our entity id (the audience)
   * @param acsUrl our assertion consumer service
   * @param skew clock skew
   * @param clock clock
   */
  public SamlResponseValidator(
      PublicKey idpKey,
      String idpEntityId,
      String spEntityId,
      String acsUrl,
      Duration skew,
      Clock clock) {
    this.idpKey = idpKey;
    this.idpEntityId = idpEntityId;
    this.spEntityId = spEntityId;
    this.acsUrl = acsUrl;
    this.skew = skew;
    this.clock = clock;
  }

  /**
   * Validates a response.
   *
   * @param xml the decoded SAMLResponse
   * @param knownRequest tells whether an {@code InResponseTo} is a request we sent and have not
   *     used yet (and uses it)
   * @param usernameAttribute attribute holding the user name, blank for the NameID
   * @param groupsAttribute attribute holding the groups, blank for none
   * @return the identity
   * @throws SsoException INVALID with the reason
   */
  public SsoIdentity validate(
      byte[] xml,
      Predicate<String> knownRequest,
      String usernameAttribute,
      String groupsAttribute) {
    Document document = parse(xml);
    Element response = document.getDocumentElement();
    require(
        PROTOCOL.equals(response.getNamespaceURI()) && "Response".equals(response.getLocalName()),
        "not a SAML Response");
    requireSuccess(response);
    String destination = response.getAttribute("Destination");
    require(destination.isEmpty() || destination.equals(acsUrl), "Destination is not ours");
    String inResponseTo = response.getAttribute("InResponseTo");
    require(!inResponseTo.isEmpty(), "unsolicited response (no InResponseTo)");
    Element responseIssuer = child(response, ASSERTION, "Issuer");
    require(
        responseIssuer == null || idpEntityId.equals(responseIssuer.getTextContent().trim()),
        "Response issuer is not the provider");
    require(
        document.getElementsByTagNameNS(ASSERTION, "EncryptedAssertion").getLength() == 0,
        "encrypted assertions are not supported");
    NodeList assertions = document.getElementsByTagNameNS(ASSERTION, "Assertion");
    require(assertions.getLength() == 1, "exactly one Assertion is expected");
    Element assertion = (Element) assertions.item(0);
    require(assertion.getParentNode() == response, "the Assertion is not a child of the Response");
    require(response.hasAttribute(ID) && assertion.hasAttribute(ID), "an ID is missing");
    response.setIdAttribute(ID, true);
    assertion.setIdAttribute(ID, true);
    verifySignature(document, response, assertion);
    Element issuer = child(assertion, ASSERTION, "Issuer");
    require(
        issuer != null && idpEntityId.equals(issuer.getTextContent().trim()),
        "Assertion issuer is not the provider");
    Instant now = clock.instant();
    checkConditions(assertion, now);
    checkSubjectConfirmation(assertion, inResponseTo, now);
    require(knownRequest.test(inResponseTo), "InResponseTo is not a pending request");
    String username = username(assertion, usernameAttribute);
    return new SsoIdentity(username, attributeValues(assertion, groupsAttribute));
  }

  private static Document parse(byte[] xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setAttribute(MAX_DEPTH_PROPERTY, String.valueOf(MAX_DEPTH));
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setErrorHandler(null);
      return builder.parse(new ByteArrayInputStream(xml));
    } catch (ParserConfigurationException | SAXException | IOException ex) {
      throw new SsoException(
          SsoException.INVALID, "SAML Response not readable: " + ex.getMessage(), ex);
    }
  }

  private static void requireSuccess(Element response) {
    Element status = child(response, PROTOCOL, "Status");
    Element code = status == null ? null : child(status, PROTOCOL, "StatusCode");
    require(code != null && SUCCESS.equals(code.getAttribute("Value")), "status is not Success");
  }

  /** Validates the signature of the Assertion, else of the Response; one of them must be signed. */
  private void verifySignature(Document document, Element response, Element assertion) {
    Element signed = assertion;
    Element signature = child(assertion, DSIG, "Signature");
    if (signature == null) {
      signed = response;
      signature = child(response, DSIG, "Signature");
    }
    require(signature != null, "neither the Assertion nor the Response is signed");
    String id = signed.getAttribute(ID);
    require(!id.isEmpty(), "the signed element has no ID");
    require(countIds(document, id) == 1, "the signed ID is not unique");
    try {
      DOMValidateContext context = new DOMValidateContext(idpKey, signature);
      context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
      context.setIdAttributeNS(signed, null, ID);
      XMLSignature xmlSignature =
          XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context);
      List<?> references = xmlSignature.getSignedInfo().getReferences();
      require(references.size() == 1, "the signature must have exactly one reference");
      Reference reference = (Reference) references.get(0);
      require(("#" + id).equals(reference.getURI()), "the signature does not cover the element");
      require(xmlSignature.validate(context), "the signature is not valid");
    } catch (MarshalException | XMLSignatureException ex) {
      throw new SsoException(SsoException.INVALID, "signature not valid: " + ex.getMessage(), ex);
    }
  }

  private static int countIds(Document document, String id) {
    NodeList all = document.getElementsByTagNameNS("*", "*");
    int count = 0;
    for (int i = 0; i < all.getLength(); i++) {
      if (id.equals(((Element) all.item(i)).getAttribute(ID))) {
        count++;
      }
    }
    return count;
  }

  private void checkConditions(Element assertion, Instant now) {
    Element conditions = child(assertion, ASSERTION, "Conditions");
    require(conditions != null, "the Assertion has no Conditions");
    Instant notBefore = time(conditions.getAttribute("NotBefore"));
    Instant notOnOrAfter = time(conditions.getAttribute("NotOnOrAfter"));
    require(
        notBefore == null || !now.plus(skew).isBefore(notBefore), "the Assertion is not valid yet");
    require(
        notOnOrAfter == null || now.minus(skew).isBefore(notOnOrAfter),
        "the Assertion has expired");
    boolean audience = false;
    for (Element restriction : children(conditions, ASSERTION, "AudienceRestriction")) {
      for (Element value : children(restriction, ASSERTION, "Audience")) {
        audience |= spEntityId.equals(value.getTextContent().trim());
      }
    }
    require(audience, "the Assertion is not for this service provider (audience)");
  }

  private void checkSubjectConfirmation(Element assertion, String inResponseTo, Instant now) {
    Element subject = child(assertion, ASSERTION, "Subject");
    require(subject != null, "the Assertion has no Subject");
    boolean confirmed = false;
    for (Element confirmation : children(subject, ASSERTION, "SubjectConfirmation")) {
      Element data = child(confirmation, ASSERTION, "SubjectConfirmationData");
      if (BEARER.equals(confirmation.getAttribute("Method")) && data != null) {
        Instant notOnOrAfter = time(data.getAttribute("NotOnOrAfter"));
        confirmed |=
            acsUrl.equals(data.getAttribute("Recipient"))
                && notOnOrAfter != null
                && now.minus(skew).isBefore(notOnOrAfter)
                && inResponseTo.equals(data.getAttribute("InResponseTo"));
      }
    }
    require(confirmed, "no valid bearer SubjectConfirmation for this service");
  }

  private static String username(Element assertion, String usernameAttribute) {
    if (usernameAttribute != null && !usernameAttribute.isBlank()) {
      List<String> values = attributeValues(assertion, usernameAttribute);
      require(!values.isEmpty(), "the Assertion has no attribute " + usernameAttribute);
      return values.get(0).trim();
    }
    Element subject = child(assertion, ASSERTION, "Subject");
    Element nameId = subject == null ? null : child(subject, ASSERTION, "NameID");
    require(nameId != null && !nameId.getTextContent().isBlank(), "the Assertion has no NameID");
    return nameId.getTextContent().trim();
  }

  private static List<String> attributeValues(Element assertion, String name) {
    List<String> values = new ArrayList<>();
    if (name == null || name.isBlank()) {
      return values;
    }
    for (Element statement : children(assertion, ASSERTION, "AttributeStatement")) {
      for (Element attribute : children(statement, ASSERTION, "Attribute")) {
        if (name.equals(attribute.getAttribute("Name"))
            || name.equals(attribute.getAttribute("FriendlyName"))) {
          children(attribute, ASSERTION, "AttributeValue")
              .forEach(v -> values.add(v.getTextContent().trim()));
        }
      }
    }
    return values;
  }

  private static Instant time(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value.trim());
    } catch (DateTimeParseException ex) {
      throw new SsoException(SsoException.INVALID, "invalid time " + value, ex);
    }
  }

  private static Element child(Element parent, String namespace, String name) {
    List<Element> found = children(parent, namespace, name);
    return found.isEmpty() ? null : found.get(0);
  }

  private static List<Element> children(Element parent, String namespace, String name) {
    List<Element> found = new ArrayList<>();
    for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
      if (node instanceof Element element
          && namespace.equals(element.getNamespaceURI())
          && name.equals(element.getLocalName())) {
        found.add(element);
      }
    }
    return found;
  }

  private static void require(boolean condition, String reason) {
    if (!condition) {
      throw new SsoException(SsoException.INVALID, "SAML Response refused: " + reason);
    }
  }
}
