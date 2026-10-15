package com.iortatechnxt.brokerverse.identity.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryStatus;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventSource;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventType;
import com.iortatechnxt.brokerverse.identity.service.IdentityExtract;
import com.iortatechnxt.brokerverse.identity.service.IdentityMemberships;
import com.iortatechnxt.brokerverse.identity.service.IdentityRefused;
import com.iortatechnxt.brokerverse.identity.service.IdentitySyncService;
import com.iortatechnxt.brokerverse.identity.service.ScimUsers;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The standard user-provisioning interface (SCIM 2.0) that UIDM-ISC calls through the API gateway
 * (BDOI FRS FRUM.002.02, FRUM.003.01 to FRUM.003.03; Annex N option a): a joiner is a {@code POST
 * /Users}, a mover a {@code PUT /Users/{id}}, a status change a {@code PATCH} of {@code active} or
 * of the AD status, a leaver a {@code DELETE}; {@code GET /Users} and {@code GET /Groups} ({@link
 * ScimGroupsController}) are the extract for access certification. UIDM-ISC orchestrates the
 * membership of the group profiles: the {@code groups} of a User sent on a create or a replace
 * become the user's group profiles at once, without an approval in the system (absent, they stay).
 * Every event is recorded with its outcome; a refused event answers with the SCIM error and is
 * alerted to the System Administrators.
 */
@RestController
@RequestMapping("/integration/v1/scim/v2")
public class ScimProvisioningController {

  private static final String ACTIVE = "active";

  private static final String RESOURCES = "Resources";

  private final IdentitySyncService sync;
  private final IdentityMemberships memberships;
  private final IdentityExtract extract;
  private final ScimAnswers answers;
  private final ObjectMapper json;

  /**
   * Creates the controller.
   *
   * @param sync the synchronisation
   * @param memberships membership events (the groups of a User)
   * @param extract the certification extract
   * @param answers SCIM answers
   * @param json object mapper
   */
  public ScimProvisioningController(
      IdentitySyncService sync,
      IdentityMemberships memberships,
      IdentityExtract extract,
      ScimAnswers answers,
      ObjectMapper json) {
    this.sync = sync;
    this.memberships = memberships;
    this.extract = extract;
    this.answers = answers;
    this.json = json;
  }

  /**
   * A joiner: creates the user.
   *
   * @param body SCIM User
   * @return the user created, or the SCIM error
   */
  @PostMapping("/Users")
  public ResponseEntity<JsonNode> create(@RequestBody JsonNode body) {
    DirectoryAccount account = read(body);
    return provision(account, ScimUsers.groups(body), IdentityEventType.JOINER, HttpStatus.CREATED);
  }

  /**
   * A mover (and the status it carries): updates the user; the group profiles stay unless the User
   * carries {@code groups}, which replace them.
   *
   * @param id user ID (the resource id) or Windows ID
   * @param body SCIM User
   * @return the user, or the SCIM error
   */
  @PutMapping("/Users/{id}")
  public ResponseEntity<JsonNode> replace(@PathVariable String id, @RequestBody JsonNode body) {
    DirectoryAccount account = read(body);
    if (account.windowsId() == null) {
      account =
          withWindowsId(account, extract.account(id).map(a -> a.account().windowsId()).orElse(id));
    }
    return provision(account, ScimUsers.groups(body), IdentityEventType.MOVER, HttpStatus.OK);
  }

  /**
   * A status change: {@code active} false (leaver, or an inactive, disabled or locked account) or
   * true (rehire), or the AD status of the extension.
   *
   * @param id user ID (the resource id) or Windows ID
   * @param body SCIM PatchOp
   * @return the user, or the SCIM error
   */
  @PatchMapping("/Users/{id}")
  public ResponseEntity<JsonNode> patch(@PathVariable String id, @RequestBody JsonNode body) {
    DirectoryStatus status = patchedStatus(body);
    DirectoryAccount current =
        extract
            .account(id)
            .map(IdentityExtract.Account::account)
            .orElseGet(() -> ScimAnswers.bare(id));
    IdentityEventType type =
        status.grantsAccess() ? IdentityEventType.REHIRE : IdentityEventType.STATUS;
    return answers.answer(
        sync.receive(current.withStatus(status), type, IdentityEventSource.UIDM_ISC),
        HttpStatus.OK);
  }

  /**
   * A leaver: deactivates the user and ends the open sessions (the user and its history stay).
   *
   * @param id user ID (the resource id) or Windows ID
   * @return no content, or the SCIM error
   */
  @DeleteMapping("/Users/{id}")
  public ResponseEntity<JsonNode> leaver(@PathVariable String id) {
    DirectoryAccount current =
        extract
            .account(id)
            .map(IdentityExtract.Account::account)
            .orElseGet(() -> ScimAnswers.bare(id));
    IdentityEvent event =
        sync.receive(
            current.withStatus(DirectoryStatus.DEACTIVATED),
            IdentityEventType.LEAVER,
            IdentityEventSource.UIDM_ISC);
    ResponseEntity<JsonNode> refused = answers.refusal(event);
    return refused != null ? refused : ResponseEntity.noContent().build();
  }

  /**
   * The accounts with their group profiles (certification extract).
   *
   * @return SCIM ListResponse
   */
  @GetMapping("/Users")
  public ResponseEntity<JsonNode> users() {
    List<IdentityExtract.Account> accounts = extract.accounts();
    ObjectNode list = answers.listResponse(accounts.size());
    ArrayNode resources = list.putArray(RESOURCES);
    accounts.forEach(a -> resources.add(ScimUsers.write(json, a.account(), a.groupProfiles())));
    return ResponseEntity.ok().contentType(ScimAnswers.SCIM).body(list);
  }

  /**
   * One account.
   *
   * @param id user ID (the resource id) or Windows ID
   * @return SCIM User, or 404
   */
  @GetMapping("/Users/{id}")
  public ResponseEntity<JsonNode> user(@PathVariable String id) {
    return extract
        .account(id)
        .<ResponseEntity<JsonNode>>map(
            a ->
                ResponseEntity.ok()
                    .contentType(ScimAnswers.SCIM)
                    .body(ScimUsers.write(json, a.account(), a.groupProfiles())))
        .orElseGet(
            () ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(ScimAnswers.SCIM)
                    .body(
                        ScimUsers.error(
                            json, HttpStatus.NOT_FOUND.value(), null, "No user " + id)));
  }

  /**
   * Applies a joiner or a mover and, when the User carries {@code groups}, the membership event
   * that gives the user those group profiles; the group profiles are checked first, so a refused
   * membership changes nothing.
   */
  private ResponseEntity<JsonNode> provision(
      DirectoryAccount account,
      Optional<List<String>> groups,
      IdentityEventType type,
      HttpStatus ok) {
    if (groups.isPresent()) {
      Optional<IdentityEvent> refused =
          memberships.refused(
              account.withMembership(groups.get(), account.uidmRequestNo()),
              IdentityEventSource.UIDM_ISC);
      if (refused.isPresent()) {
        return answers.refusal(refused.get());
      }
    }
    IdentityEvent event = sync.receive(account, type, IdentityEventSource.UIDM_ISC);
    if (groups.isPresent() && answers.refusal(event) == null) {
      event =
          memberships.send(
              account.withMembership(groups.get(), account.uidmRequestNo()),
              IdentityEventSource.UIDM_ISC);
    }
    return answers.answer(event, ok);
  }

  private static DirectoryAccount read(JsonNode body) {
    if (body == null || !body.isObject()) {
      throw new IdentityRefused("INVALID_SCIM", "The body is not a SCIM User");
    }
    return ScimUsers.read(body);
  }

  private static DirectoryStatus patchedStatus(JsonNode body) {
    for (JsonNode op : body.path("Operations")) {
      DirectoryStatus status = statusOf(op.path("path").asText(""), op.path("value"));
      if (status != null) {
        return status;
      }
    }
    throw new IdentityRefused(
        "INVALID_PATCH", "The patch changes neither active nor the AD status");
  }

  private static DirectoryStatus statusOf(String path, JsonNode value) {
    if (ACTIVE.equals(path) && value.isBoolean()) {
      return value.asBoolean() ? DirectoryStatus.ACTIVE : DirectoryStatus.INACTIVE;
    }
    if (path.endsWith("adStatus") && value.isTextual()) {
      return DirectoryStatus.valueOf(value.asText().trim());
    }
    if (value.has(ACTIVE)) {
      return value.path(ACTIVE).asBoolean() ? DirectoryStatus.ACTIVE : DirectoryStatus.INACTIVE;
    }
    return null;
  }

  private static DirectoryAccount withWindowsId(DirectoryAccount a, String windowsId) {
    return new DirectoryAccount(
        windowsId,
        a.userId(),
        a.email(),
        a.firstName(),
        a.lastName(),
        a.displayName(),
        a.adGroup(),
        a.status(),
        a.hierarchy(),
        a.organisation(),
        a.uidmRequestNo(),
        a.groupProfiles());
  }
}
