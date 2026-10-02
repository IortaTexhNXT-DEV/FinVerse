package com.iortatechnxt.brokerverse.opsledger.service.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** JSON payload of a hand-off: the port request as the default adapter received it. */
final class HandoffPayloads {

  private HandoffPayloads() {}

  /**
   * The request as JSON, or its text when it cannot be serialised.
   *
   * @param json JSON mapper
   * @param request port request
   * @return payload
   */
  static String of(ObjectMapper json, Object request) {
    try {
      return json.writeValueAsString(request);
    } catch (JsonProcessingException e) {
      return String.valueOf(request);
    }
  }

  /**
   * Reference of a hand-off returned to the caller in place of the real document reference.
   *
   * @param id hand-off id
   * @return {@code HANDOFF-<id>}
   */
  static String reference(Long id) {
    return "HANDOFF-" + id;
  }
}
