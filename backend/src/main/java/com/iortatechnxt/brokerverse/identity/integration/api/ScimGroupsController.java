package com.iortatechnxt.brokerverse.identity.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventSource;
import com.iortatechnxt.brokerverse.identity.service.GroupProfileChecks;
import com.iortatechnxt.brokerverse.identity.service.IdentityExtract;
import com.iortatechnxt.brokerverse.identity.service.IdentityMemberships;
import com.iortatechnxt.brokerverse.identity.service.ScimInput;
import com.iortatechnxt.brokerverse.identity.service.ScimUsers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The group profiles of the provisioning interface (SCIM 2.0 Groups). UIDM-ISC orchestrates their
 * membership: a {@code PATCH /Groups/{id}} adds, removes or replaces the members (the value of a
 * member is the user id of {@code GET /Users}), applied at once without an approval in the system;
 * every user whose group profiles change gets a membership event, all are checked before any is
 * applied, so a refusal changes nobody. The group profiles themselves (name, permissions) are
 * maintained in the system through group-profile requests and read through {@code GET /Groups},
 * also the extract for access certification.
 */
@RestController
@RequestMapping("/integration/v1/scim/v2/Groups")
public class ScimGroupsController {

  /** Header of the UIDM request number of a change of the members of a group profile. */
  public static final String REQUEST_NO = "X-UIDM-Request-No";

  private static final String RESOURCES = "Resources";
  private static final String MEMBERS = "members";
  private static final Pattern MEMBER_FILTER =
      Pattern.compile("members\\[\\s*value\\s+eq\\s+\"([^\"]+)\"\\s*]", Pattern.CASE_INSENSITIVE);
  private static final Pattern MEMBERS_PATH = Pattern.compile(MEMBERS, Pattern.CASE_INSENSITIVE);

  /** The PATCH operations, matched without regard to case as SCIM allows. */
  private static final List<String> VERBS = List.of("add", "remove", "replace");

  private final IdentityMemberships memberships;
  private final IdentityExtract extract;
  private final ScimAnswers answers;
  private final ObjectMapper json;

  /**
   * Creates the controller.
   *
   * @param memberships membership events
   * @param extract the certification extract
   * @param answers SCIM answers
   * @param json object mapper
   */
  public ScimGroupsController(
      IdentityMemberships memberships,
      IdentityExtract extract,
      ScimAnswers answers,
      ObjectMapper json) {
    this.memberships = memberships;
    this.extract = extract;
    this.answers = answers;
    this.json = json;
  }

  /**
   * The group profiles with their permissions and members (certification extract), a page at a
   * time.
   *
   * @param startIndex first group profile, from 1
   * @param count group profiles of the page, at most 200
   * @return SCIM ListResponse of Groups
   */
  @GetMapping
  public ResponseEntity<JsonNode> groups(
      @RequestParam(defaultValue = "1") int startIndex,
      @RequestParam(defaultValue = "" + ScimAnswers.MAX_COUNT) int count) {
    List<IdentityExtract.Group> groups = extract.groups();
    ScimAnswers.Page page = new ScimAnswers.Page(startIndex, count);
    List<IdentityExtract.Group> shown = page.of(groups);
    ObjectNode list = answers.listResponse(groups.size(), page, shown.size());
    ArrayNode resources = list.putArray(RESOURCES);
    shown.forEach(g -> resources.add(resource(g)));
    return ResponseEntity.ok().contentType(ScimAnswers.SCIM).body(list);
  }

  /**
   * One group profile.
   *
   * @param id code of the group profile
   * @return SCIM Group, or 404
   */
  @GetMapping("/{id}")
  public ResponseEntity<JsonNode> group(@PathVariable String id) {
    return extract
        .group(id)
        .map(g -> ResponseEntity.ok().contentType(ScimAnswers.SCIM).body((JsonNode) resource(g)))
        .orElseGet(
            () -> answers.error(HttpStatus.NOT_FOUND, null, GroupProfileChecks.NO_GROUP + id));
  }

  /**
   * Adds, removes or replaces the members of a group profile (SCIM PatchOp of {@code members}, the
   * value of a member being the user id of {@code GET /Users}). Every user whose group profiles
   * change gets a membership event; all are checked before any is applied, so a refusal changes
   * nobody.
   *
   * @param id code of the group profile
   * @param body SCIM PatchOp
   * @param requestNo UIDM request number, may be absent
   * @return the group profile, or the SCIM error
   */
  @PatchMapping("/{id}")
  public ResponseEntity<JsonNode> patchGroup(
      @PathVariable String id,
      @RequestBody JsonNode body,
      @RequestHeader(value = REQUEST_NO, required = false) String requestNo) {
    ScimInput.checkId(id);
    ScimInput.checkRequestNo(requestNo);
    ScimInput.checkOperations(body);
    Optional<IdentityExtract.Group> found = extract.group(id);
    if (found.isEmpty()) {
      return answers.error(HttpStatus.NOT_FOUND, null, GroupProfileChecks.NO_GROUP + id);
    }
    String code = found.get().code();
    Membership membership = new Membership(code, requestNo);
    ResponseEntity<JsonNode> refused = membership.read(body);
    if (refused == null) {
      refused = membership.apply();
    }
    return refused != null
        ? refused
        : ResponseEntity.ok()
            .contentType(ScimAnswers.SCIM)
            .body(resource(extract.group(code).orElseThrow()));
  }

  private ObjectNode resource(IdentityExtract.Group g) {
    ObjectNode group = json.createObjectNode();
    group.putArray("schemas").add("urn:ietf:params:scim:schemas:core:2.0:Group");
    group.put("id", g.code());
    group.put("displayName", g.name());
    group.put("active", g.active());
    ArrayNode members = group.putArray(MEMBERS);
    g.members().forEach(m -> members.addObject().put("value", m));
    ArrayNode permissions = group.putArray("permissions");
    g.permissions().forEach(permissions::add);
    return group;
  }

  /**
   * A change of the members of a group profile: the group profiles every user it touches would
   * hold, user by user; a member without an account here (no Windows ID) is left as is.
   */
  private final class Membership {

    private final String code;
    private final String requestNo;
    private final Map<String, DirectoryAccount> accounts = new LinkedHashMap<>();
    private final Map<String, Set<String>> held = new LinkedHashMap<>();

    Membership(String code, String requestNo) {
      this.code = code;
      this.requestNo = requestNo;
    }

    /** Reads the operations; the SCIM error of the first invalid one, null when all are valid. */
    ResponseEntity<JsonNode> read(JsonNode body) {
      for (JsonNode op : body.path("Operations")) {
        ResponseEntity<JsonNode> refused = operation(op);
        if (refused != null) {
          return refused;
        }
      }
      return null;
    }

    private ResponseEntity<JsonNode> operation(JsonNode op) {
      String path = op.path("path").asText("").trim();
      JsonNode value = op.path("value");
      if (path.isEmpty() && value.has(MEMBERS)) {
        path = MEMBERS;
        value = value.path(MEMBERS);
      }
      Matcher filter = MEMBER_FILTER.matcher(path);
      boolean filtered = filter.matches();
      if (!filtered && !MEMBERS_PATH.matcher(path).matches()) {
        return answers.error(
            HttpStatus.BAD_REQUEST, "invalidPath", "Only the members of a group profile change");
      }
      List<String> members = filtered ? List.of(filter.group(1)) : ScimUsers.values(value);
      ResponseEntity<JsonNode> unknown = unknownMember(members);
      if (unknown != null) {
        return unknown;
      }
      String sent = op.path("op").asText("").trim();
      String verb =
          VERBS.stream()
              .filter(v -> Pattern.compile(v, Pattern.CASE_INSENSITIVE).matcher(sent).matches())
              .findFirst()
              .orElse(sent);
      return change(verb, members, filtered)
          ? null
          : answers.error(HttpStatus.BAD_REQUEST, "invalidSyntax", "Unknown operation");
    }

    /** A member that is no user of {@code GET /Users}: a refused membership event. */
    private ResponseEntity<JsonNode> unknownMember(List<String> members) {
      return members.stream()
          .filter(m -> extract.account(m).isEmpty())
          .findFirst()
          .map(
              m ->
                  answers.refusal(
                      memberships.send(
                          ScimAnswers.bare(m).withMembership(List.of(code), requestNo),
                          IdentityEventSource.UIDM_ISC)))
          .orElse(null);
    }

    private boolean change(String verb, List<String> members, boolean filtered) {
      switch (verb) {
        case "add" -> members.forEach(m -> set(m, true));
        case "remove" ->
            (members.isEmpty() && !filtered ? extract.holders(code) : members)
                .forEach(m -> set(m, false));
        case "replace" -> {
          extract.holders(code).forEach(m -> set(m, false));
          members.forEach(m -> set(m, true));
        }
        default -> {
          return false;
        }
      }
      return true;
    }

    private void set(String member, boolean holds) {
      extract
          .account(member)
          .ifPresent(
              a -> {
                String user = a.account().userId();
                accounts.putIfAbsent(user, a.account());
                Set<String> groups =
                    held.computeIfAbsent(user, u -> new TreeSet<>(a.groupProfiles()));
                if (holds) {
                  groups.add(code);
                } else {
                  groups.remove(code);
                }
              });
    }

    /**
     * Checks every user whose group profiles change, then applies them; nothing is applied when one
     * is refused.
     *
     * @return the SCIM error, null when every change applied
     */
    ResponseEntity<JsonNode> apply() {
      List<DirectoryAccount> changed = new ArrayList<>();
      held.forEach(
          (user, groups) -> {
            if (!groups.equals(new TreeSet<>(extract.groupsOf(user)))) {
              changed.add(accounts.get(user).withMembership(List.copyOf(groups), requestNo));
            }
          });
      Optional<IdentityEvent> refused =
          changed.stream()
              .map(a -> memberships.refused(a, IdentityEventSource.UIDM_ISC))
              .flatMap(Optional::stream)
              .findFirst();
      if (refused.isPresent()) {
        return answers.refusal(refused.get());
      }
      return changed.stream()
          .map(a -> answers.refusal(memberships.send(a, IdentityEventSource.UIDM_ISC)))
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
    }
  }
}
