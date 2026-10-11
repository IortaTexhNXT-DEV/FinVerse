package com.iortatechnxt.brokerverse.events.api.dto;

import com.iortatechnxt.brokerverse.events.service.DeadLetterStore.DeadLetter;
import java.time.Instant;
import java.util.UUID;

/**
 * One dead letter for the support screen.
 *
 * @param id id
 * @param eventId event id, null when the record was not an envelope
 * @param originalTopic topic it failed on
 * @param deadLetterTopic dead-letter topic
 * @param key record key
 * @param consumerGroup consumer group that failed
 * @param errorMessage last exception
 * @param status NEW, RETRIED or DISCARDED
 * @param receivedAt time recorded
 * @param resolvedAt time retried or discarded
 * @param resolvedBy support user
 * @param payload record value
 */
public record DeadLetterResponse(
    long id,
    UUID eventId,
    String originalTopic,
    String deadLetterTopic,
    String key,
    String consumerGroup,
    String errorMessage,
    String status,
    Instant receivedAt,
    Instant resolvedAt,
    String resolvedBy,
    String payload) {

  /**
   * Maps a dead letter.
   *
   * @param d dead letter
   * @return response
   */
  public static DeadLetterResponse from(DeadLetter d) {
    return new DeadLetterResponse(
        d.id(),
        d.eventId(),
        d.originalTopic(),
        d.deadLetterTopic(),
        d.key(),
        d.consumerGroup(),
        d.errorMessage(),
        d.status(),
        d.receivedAt(),
        d.resolvedAt(),
        d.resolvedBy(),
        d.payload());
  }
}
