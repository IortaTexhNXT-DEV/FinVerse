package com.iortatechnxt.brokerverse.events.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Parses and checks the envelope of a consumed record; consumers call it first so a malformed
 * record goes straight to the dead-letter topic.
 */
@Component
public class EventEnvelopeReader {

  private final ObjectMapper mapper;

  /**
   * Creates the reader.
   *
   * @param mapper JSON mapper
   */
  public EventEnvelopeReader(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  /**
   * Reads an envelope.
   *
   * @param json record value
   * @return envelope
   * @throws JsonProcessingException when the value is not JSON of the envelope shape
   * @throws InvalidEventException when a mandatory field is missing
   */
  public EventEnvelope read(String json) throws JsonProcessingException {
    if (json == null || json.isBlank()) {
      throw new InvalidEventException("Empty event");
    }
    EventEnvelope envelope = mapper.readValue(json, EventEnvelope.class);
    if (envelope.eventId() == null
        || envelope.type() == null
        || envelope.occurredAt() == null
        || envelope.payload() == null) {
      throw new InvalidEventException("Envelope without eventId, type, occurredAt or payload");
    }
    return envelope;
  }

  /**
   * Reads a payload field as a number.
   *
   * @param envelope envelope
   * @param field payload field
   * @return value
   * @throws InvalidEventException when the field is missing or not a number
   */
  public static long longField(EventEnvelope envelope, String field) {
    if (!envelope.payload().path(field).canConvertToLong()) {
      throw new InvalidEventException("Payload field " + field + " missing in " + envelope.type());
    }
    return envelope.payload().path(field).asLong();
  }
}
