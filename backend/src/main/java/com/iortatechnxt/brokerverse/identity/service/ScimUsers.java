package com.iortatechnxt.brokerverse.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The SCIM 2.0 User resource of the provisioning interface (the standard user-provisioning
 * interface of UIDM-ISC): the core schema, the enterprise extension (department, manager) and the
 * extension of the system for the Windows ID, the AD group and status, the hierarchy names, the
 * unit / segment, the location and the UIDM request number.
 */
public final class ScimUsers {

  /** Core User schema. */
  public static final String CORE = "urn:ietf:params:scim:schemas:core:2.0:User";

  /** Enterprise User extension. */
  public static final String ENTERPRISE =
      "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User";

  /** Extension of the system. */
  public static final String BIBS = "urn:bibs:params:scim:schemas:extension:bdo:2.0:User";

  /** List response. */
  public static final String LIST = "urn:ietf:params:scim:api:messages:2.0:ListResponse";

  /** Error response. */
  public static final String ERROR = "urn:ietf:params:scim:api:messages:2.0:Error";

  private static final String VALUE = "value";
  private static final String WINDOWS_ID = "windowsId";
  private static final String DISPLAY_NAME = "displayName";

  private ScimUsers() {}

  /**
   * Reads an account from a SCIM User resource.
   *
   * @param user the resource
   * @return the account
   */
  public static DirectoryAccount read(JsonNode user) {
    JsonNode name = user.path("name");
    JsonNode enterprise = user.path(ENTERPRISE);
    JsonNode ext = user.path(BIBS);
    String windowsId = firstText(ext.path(WINDOWS_ID), user.path("externalId"));
    DirectoryStatus status = status(ext.path("adStatus"), user.path("active"));
    List<String> groups = new ArrayList<>();
    ext.path("groupProfiles").forEach(g -> groups.add(g.asText()));
    return new DirectoryAccount(
        windowsId,
        text(user.path("userName")),
        email(user.path("emails")),
        text(name.path("givenName")),
        text(name.path("familyName")),
        text(user.path(DISPLAY_NAME)),
        text(ext.path("adGroup")),
        status,
        new DirectoryAccount.Hierarchy(
            text(ext.path("teamLeaderName")),
            text(ext.path("teamHeadName")),
            text(ext.path("sectionHeadName")),
            firstText(ext.path("unitHeadName"), enterprise.path("manager").path(DISPLAY_NAME))),
        new DirectoryAccount.Organisation(
            firstText(ext.path("unitSegment"), enterprise.path("division")),
            text(enterprise.path("department")),
            text(ext.path("location"))),
        text(ext.path("uidmRequestNo")),
        groups);
  }

  /**
   * Writes an account as a SCIM User resource.
   *
   * @param json object mapper
   * @param account the account
   * @param groupProfiles the group profiles held in the system (certification extract)
   * @return the resource
   */
  public static ObjectNode write(
      ObjectMapper json, DirectoryAccount account, List<String> groupProfiles) {
    ObjectNode user = json.createObjectNode();
    user.putArray("schemas").add(CORE).add(ENTERPRISE).add(BIBS);
    user.put("id", account.userId());
    user.put("externalId", account.windowsId());
    user.put("userName", account.userId());
    user.put(DISPLAY_NAME, account.fullName());
    user.put("active", account.status().grantsAccess());
    ObjectNode name = user.putObject("name");
    name.put("givenName", account.firstName());
    name.put("familyName", account.lastName());
    if (account.email() != null) {
      user.putArray("emails").addObject().put(VALUE, account.email()).put("primary", true);
    }
    ObjectNode enterprise = user.putObject(ENTERPRISE);
    enterprise.put("department", account.organisation().department());
    enterprise.put("division", account.organisation().unitSegment());
    ObjectNode ext = user.putObject(BIBS);
    ext.put(WINDOWS_ID, account.windowsId());
    ext.put("adGroup", account.adGroup());
    ext.put("adStatus", account.status().name());
    ext.put("teamLeaderName", account.hierarchy().teamLeader());
    ext.put("teamHeadName", account.hierarchy().teamHead());
    ext.put("sectionHeadName", account.hierarchy().sectionHead());
    ext.put("unitHeadName", account.hierarchy().unitHead());
    ext.put("unitSegment", account.organisation().unitSegment());
    ext.put("location", account.organisation().location());
    ext.put("uidmRequestNo", account.uidmRequestNo());
    ArrayNode groups = ext.putArray("groupProfiles");
    groupProfiles.forEach(groups::add);
    return user;
  }

  /**
   * A SCIM error answer.
   *
   * @param json object mapper
   * @param status HTTP status
   * @param scimType SCIM error type (for example uniqueness), may be null
   * @param detail detail
   * @return the error
   */
  public static ObjectNode error(ObjectMapper json, int status, String scimType, String detail) {
    ObjectNode error = json.createObjectNode();
    error.putArray("schemas").add(ERROR);
    error.put("status", String.valueOf(status));
    if (scimType != null) {
      error.put("scimType", scimType);
    }
    error.put("detail", detail);
    return error;
  }

  private static DirectoryStatus status(JsonNode adStatus, JsonNode active) {
    String text = text(adStatus);
    if (text != null) {
      try {
        return DirectoryStatus.valueOf(text.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        throw new IdentityRefused("UNKNOWN_STATUS", "Unknown AD status " + text, ex);
      }
    }
    return active.isBoolean() && !active.asBoolean()
        ? DirectoryStatus.INACTIVE
        : DirectoryStatus.ACTIVE;
  }

  private static String email(JsonNode emails) {
    String first = null;
    for (JsonNode e : emails) {
      if (e.path("primary").asBoolean(false)) {
        return text(e.path(VALUE));
      }
      if (first == null) {
        first = text(e.path(VALUE));
      }
    }
    return first;
  }

  private static String firstText(JsonNode a, JsonNode b) {
    String value = text(a);
    return value != null ? value : text(b);
  }

  private static String text(JsonNode node) {
    if (node == null || node.isMissingNode() || node.isNull()) {
      return null;
    }
    String value = node.asText().trim();
    return value.isEmpty() ? null : value;
  }
}
