package com.iortatechnxt.brokerverse.identity.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryStatus;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventSource;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventStatus;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventType;
import com.iortatechnxt.brokerverse.identity.service.IdentityExtract;
import com.iortatechnxt.brokerverse.identity.service.IdentityRefused;
import com.iortatechnxt.brokerverse.identity.service.IdentitySyncService;
import com.iortatechnxt.brokerverse.identity.service.ScimUsers;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
 * of the AD status, a leaver a {@code DELETE}; {@code GET /Users} and {@code GET /Groups} are the
 * extract for access certification. Every event is recorded with its outcome; a refused event
 * answers with the SCIM error and is alerted to the System Administrators.
 */
@RestController
@RequestMapping("/integration/v1/scim/v2")
public class ScimProvisioningController {

  private static final MediaType SCIM = MediaType.valueOf("application/scim+json");
  private static final String RESOURCES = "Resources";
  private static final String ACTIVE = "active";

  private final IdentitySyncService sync;
  private final IdentityExtract extract;
  private final ObjectMapper json;

  /**
   * Creates the controller.
   *
   * @param sync the synchronisation
   * @param extract the certification extract
   * @param json object mapper
   */
  public ScimProvisioningController(
      IdentitySyncService sync, IdentityExtract extract, ObjectMapper json) {
    this.sync = sync;
    this.extract = extract;
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
    return answer(
        sync.receive(read(body), IdentityEventType.JOINER, IdentityEventSource.UIDM_ISC),
        HttpStatus.CREATED);
  }

  /**
   * A mover (and the status it carries): updates the user; the group profiles stay.
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
    return answer(
        sync.receive(account, IdentityEventType.MOVER, IdentityEventSource.UIDM_ISC),
        HttpStatus.OK);
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
        extract.account(id).map(IdentityExtract.Account::account).orElseGet(() -> bare(id));
    IdentityEventType type =
        status.grantsAccess() ? IdentityEventType.REHIRE : IdentityEventType.STATUS;
    return answer(
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
        extract.account(id).map(IdentityExtract.Account::account).orElseGet(() -> bare(id));
    IdentityEvent event =
        sync.receive(
            current.withStatus(DirectoryStatus.DEACTIVATED),
            IdentityEventType.LEAVER,
            IdentityEventSource.UIDM_ISC);
    ResponseEntity<JsonNode> refused = refusal(event);
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
    ObjectNode list = listResponse(accounts.size());
    ArrayNode resources = list.putArray(RESOURCES);
    accounts.forEach(a -> resources.add(ScimUsers.write(json, a.account(), a.groupProfiles())));
    return ResponseEntity.ok().contentType(SCIM).body(list);
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
                    .contentType(SCIM)
                    .body(ScimUsers.write(json, a.account(), a.groupProfiles())))
        .orElseGet(
            () ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(SCIM)
                    .body(
                        ScimUsers.error(
                            json, HttpStatus.NOT_FOUND.value(), null, "No user " + id)));
  }

  /**
   * The group profiles with their permissions and members (certification extract).
   *
   * @return SCIM ListResponse of Groups
   */
  @GetMapping("/Groups")
  public ResponseEntity<JsonNode> groups() {
    List<IdentityExtract.Group> groups = extract.groups();
    ObjectNode list = listResponse(groups.size());
    ArrayNode resources = list.putArray(RESOURCES);
    for (IdentityExtract.Group g : groups) {
      ObjectNode group = resources.addObject();
      group.putArray("schemas").add("urn:ietf:params:scim:schemas:core:2.0:Group");
      group.put("id", g.code());
      group.put("displayName", g.name());
      group.put(ACTIVE, g.active());
      ArrayNode members = group.putArray("members");
      g.members().forEach(m -> members.addObject().put("value", m));
      ArrayNode permissions = group.putArray("permissions");
      g.permissions().forEach(permissions::add);
    }
    return ResponseEntity.ok().contentType(SCIM).body(list);
  }

  private ObjectNode listResponse(int total) {
    ObjectNode list = json.createObjectNode();
    list.putArray("schemas").add(ScimUsers.LIST);
    list.put("totalResults", total);
    list.put("itemsPerPage", total);
    list.put("startIndex", 1);
    return list;
  }

  private ResponseEntity<JsonNode> answer(IdentityEvent event, HttpStatus ok) {
    ResponseEntity<JsonNode> refused = refusal(event);
    if (refused != null) {
      return refused;
    }
    JsonNode user =
        extract
            .account(event.getWindowsId())
            .<JsonNode>map(a -> ScimUsers.write(json, a.account(), a.groupProfiles()))
            .orElseGet(json::createObjectNode);
    return ResponseEntity.status(ok).contentType(SCIM).body(user);
  }

  private ResponseEntity<JsonNode> refusal(IdentityEvent event) {
    if (event.getStatus() == IdentityEventStatus.APPLIED
        || event.getStatus() == IdentityEventStatus.NO_CHANGE) {
      return null;
    }
    boolean conflict =
        event.getMessage() != null
            && (event.getMessage().contains("belongs to another user")
                || event.getMessage().contains("already exists"));
    HttpStatus status =
        event.getStatus() == IdentityEventStatus.FAILED
            ? HttpStatus.INTERNAL_SERVER_ERROR
            : conflict ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
    return ResponseEntity.status(status)
        .contentType(SCIM)
        .body(
            ScimUsers.error(
                json,
                status.value(),
                conflict ? "uniqueness" : null,
                "Event " + event.getId() + ": " + event.getMessage()));
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

  private static DirectoryAccount bare(String id) {
    return new DirectoryAccount(id, id, null, null, null, null, null, null, null, null, null, null);
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
