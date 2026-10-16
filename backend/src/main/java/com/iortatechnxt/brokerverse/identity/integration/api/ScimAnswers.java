package com.iortatechnxt.brokerverse.identity.integration.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iortatechnxt.brokerverse.identity.domain.DirectoryAccount;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEvent;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventStatus;
import com.iortatechnxt.brokerverse.identity.domain.IdentityEventType;
import com.iortatechnxt.brokerverse.identity.service.GroupProfileChecks;
import com.iortatechnxt.brokerverse.identity.service.IdentityExtract;
import com.iortatechnxt.brokerverse.identity.service.ScimUsers;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * The answers of the provisioning interface (SCIM 2.0): the User of an applied event, the SCIM
 * error of a refused or failed one (409 uniqueness for a Windows ID or user taken, 404 for a group
 * profile that does not exist, 400 invalidValue for a refused membership, 500 for a failure), and
 * the list responses.
 */
@Component
class ScimAnswers {

  /** Largest page of a list, and the page when none is asked. */
  static final int MAX_COUNT = 200;

  /** Media type of the interface. */
  static final MediaType SCIM = MediaType.valueOf("application/scim+json");

  private static final String INVALID_VALUE = "invalidValue";

  private final IdentityExtract extract;
  private final ObjectMapper json;

  ScimAnswers(IdentityExtract extract, ObjectMapper json) {
    this.extract = extract;
    this.json = json;
  }

  /** An account known only by its id (user ID or Windows ID). */
  static DirectoryAccount bare(String id) {
    return new DirectoryAccount(id, id, null, null, null, null, null, null, null, null, null, null);
  }

  /** A SCIM error. */
  ResponseEntity<JsonNode> error(HttpStatus status, String scimType, String detail) {
    return ResponseEntity.status(status)
        .contentType(SCIM)
        .body(ScimUsers.error(json, status.value(), scimType, detail));
  }

  /**
   * The start of a SCIM ListResponse.
   *
   * @param total items of the whole list
   * @param page the page asked
   * @param items items in this answer
   * @return the response without its resources
   */
  ObjectNode listResponse(int total, Page page, int items) {
    ObjectNode list = json.createObjectNode();
    list.putArray("schemas").add(ScimUsers.LIST);
    list.put("totalResults", total);
    list.put("itemsPerPage", items);
    list.put("startIndex", page.startIndex());
    return list;
  }

  /**
   * A page of a list (RFC 7644 section 3.4.2.4): {@code startIndex} from 1, {@code count} at most
   * {@value #MAX_COUNT} (also the default), so a list never answers in one unbounded response.
   *
   * @param startIndex first item, from 1 (below 1 counts as 1)
   * @param count items asked (negative counts as 0, above the maximum as the maximum)
   */
  record Page(int startIndex, int count) {

    Page {
      startIndex = Math.max(1, startIndex);
      count = Math.clamp(count, 0, MAX_COUNT);
    }

    <T> List<T> of(List<T> all) {
      int from = Math.min(all.size(), startIndex - 1);
      return all.subList(from, Math.min(all.size(), from + count));
    }
  }

  /** The User of an applied event, or the SCIM error of a refused or failed one. */
  ResponseEntity<JsonNode> answer(IdentityEvent event, HttpStatus ok) {
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

  /** The SCIM error of a refused or failed event, null when it applied. */
  ResponseEntity<JsonNode> refusal(IdentityEvent event) {
    if (event.getStatus() == IdentityEventStatus.APPLIED
        || event.getStatus() == IdentityEventStatus.NO_CHANGE) {
      return null;
    }
    boolean conflict =
        event.getMessage() != null
            && (event.getMessage().contains("belongs to another user")
                || event.getMessage().contains("already exists"));
    boolean noGroup =
        event.getMessage() != null && event.getMessage().startsWith(GroupProfileChecks.NO_GROUP);
    boolean membership = event.getEventType() == IdentityEventType.MEMBERSHIP;
    HttpStatus status;
    String scimType = null;
    if (event.getStatus() == IdentityEventStatus.FAILED) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    } else if (conflict) {
      status = HttpStatus.CONFLICT;
      scimType = "uniqueness";
    } else if (noGroup) {
      status = HttpStatus.NOT_FOUND;
    } else {
      status = HttpStatus.BAD_REQUEST;
      scimType = membership ? INVALID_VALUE : null;
    }
    return error(status, scimType, "Event " + event.getId() + ": " + event.getMessage());
  }
}
