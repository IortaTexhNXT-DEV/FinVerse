package com.iortatechnxt.brokerverse.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The checks of what UIDM-ISC sends before anything is recorded: only the attributes of the agreed
 * SCIM User schema and its extensions, each within the size of the field that keeps it and with the
 * characters it may hold. A refusal names the attribute, never its value.
 */
public final class ScimInput {

  /** Most group profiles one account may carry in an event. */
  public static final int MAX_GROUPS = 50;

  /** Most operations of one PATCH of a group profile. */
  public static final int MAX_OPERATIONS = 100;

  private static final String INVALID = "INVALID_SCIM";
  private static final int CODE = 40;
  private static final int NAME = 60;
  private static final int TEXT = 120;

  private static final Pattern USER_ID = Pattern.compile("[A-Za-z0-9._-]{1,50}");
  private static final Pattern WINDOWS_ID = Pattern.compile("[A-Za-z0-9._\\\\@-]{1,50}");
  private static final Pattern REQUEST_NO = Pattern.compile("[A-Za-z0-9._/-]{1,40}");
  private static final Pattern GROUP_CODE = Pattern.compile("[A-Za-z0-9_.-]{1,40}");
  private static final Pattern EMAIL = Pattern.compile("[^@\\s]{1,64}@[^@\\s]{1,120}");
  private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");

  /** Attributes of a SCIM User UIDM-ISC may send (core, enterprise and the bank's extension). */
  private static final Set<String> USER_ATTRIBUTES =
      Set.of(
          "schemas",
          "id",
          "externalId",
          "userName",
          "name",
          "displayName",
          "emails",
          "active",
          "groups",
          "meta",
          ScimUsers.ENTERPRISE,
          ScimUsers.BIBS);

  private ScimInput() {}

  /**
   * Refuses a User resource with an attribute outside the agreed schema.
   *
   * @param user the resource
   * @throws IdentityRefused INVALID_SCIM naming the attribute
   */
  public static void checkAttributes(JsonNode user) {
    for (Iterator<String> names = user.fieldNames(); names.hasNext(); ) {
      String name = names.next();
      if (!USER_ATTRIBUTES.contains(name)) {
        throw new IdentityRefused(INVALID, "Attribute " + shown(name) + " is not supported");
      }
    }
  }

  /**
   * Refuses an account whose values do not fit.
   *
   * @param account the account read from the resource
   * @throws IdentityRefused INVALID_SCIM naming the attribute
   */
  public static void check(DirectoryAccount account) {
    matches("userName", account.userId(), USER_ID);
    matches("windowsId", account.windowsId(), WINDOWS_ID);
    matches("uidmRequestNo", account.uidmRequestNo(), REQUEST_NO);
    matches("emails", account.email(), EMAIL);
    fits("name.givenName", account.firstName(), NAME);
    fits("name.familyName", account.lastName(), NAME);
    fits("displayName", account.displayName(), TEXT);
    fits("adGroup", account.adGroup(), TEXT);
    fits("teamLeaderName", account.hierarchy().teamLeader(), TEXT);
    fits("teamHeadName", account.hierarchy().teamHead(), TEXT);
    fits("sectionHeadName", account.hierarchy().sectionHead(), TEXT);
    fits("unitHeadName", account.hierarchy().unitHead(), TEXT);
    fits("unitSegment", account.organisation().unitSegment(), TEXT);
    fits("department", account.organisation().department(), TEXT);
    fits("location", account.organisation().location(), TEXT);
    groups(account.groupProfiles());
  }

  /**
   * Refuses a resource id (user ID or Windows ID) of a path that cannot be one.
   *
   * @param id the id of the path
   * @throws IdentityRefused INVALID_SCIM
   */
  public static void checkId(String id) {
    matches("id", id, WINDOWS_ID);
  }

  /**
   * Refuses a UIDM request number that cannot be one.
   *
   * @param requestNo request number, may be null
   * @throws IdentityRefused INVALID_SCIM
   */
  public static void checkRequestNo(String requestNo) {
    matches("uidmRequestNo", requestNo, REQUEST_NO);
  }

  /**
   * Refuses a PatchOp with too many operations.
   *
   * @param body the PatchOp
   * @throws IdentityRefused INVALID_SCIM
   */
  public static void checkOperations(JsonNode body) {
    JsonNode operations = body.path("Operations");
    if (!operations.isArray() || operations.isEmpty()) {
      throw new IdentityRefused(INVALID, "Operations is not a list of operations");
    }
    if (operations.size() > MAX_OPERATIONS) {
      throw new IdentityRefused(
          INVALID, "Operations holds more than " + MAX_OPERATIONS + " operations");
    }
  }

  /**
   * Refuses a list of group profile codes that is too long or holds an invalid code.
   *
   * @param codes group profile codes
   * @throws IdentityRefused INVALID_SCIM
   */
  public static void groups(List<String> codes) {
    if (codes.size() > MAX_GROUPS) {
      throw new IdentityRefused(
          INVALID, "groups holds more than " + MAX_GROUPS + " group profiles");
    }
    codes.forEach(code -> matches("groups.value", code, GROUP_CODE));
  }

  private static void matches(String attribute, String value, Pattern pattern) {
    if (value != null && !pattern.matcher(value).matches()) {
      throw new IdentityRefused(
          INVALID, attribute + " is too long or holds characters that are not allowed");
    }
  }

  private static void fits(String attribute, String value, int max) {
    if (value == null) {
      return;
    }
    if (value.length() > max) {
      throw new IdentityRefused(INVALID, attribute + " is longer than " + max + " characters");
    }
    if (CONTROL.matcher(value).find()) {
      throw new IdentityRefused(INVALID, attribute + " holds control characters");
    }
  }

  private static String shown(String name) {
    String cleaned = CONTROL.matcher(name).replaceAll("");
    return cleaned.length() > CODE ? cleaned.substring(0, CODE) : cleaned;
  }
}
