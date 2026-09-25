package com.iortatechnxt.brokerverse.events.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.events.api.dto.ArchivedEventResponse;
import com.iortatechnxt.brokerverse.events.api.dto.DeadLetterResponse;
import com.iortatechnxt.brokerverse.events.api.dto.OutboxEventResponse;
import com.iortatechnxt.brokerverse.events.api.dto.TopicResponse;
import com.iortatechnxt.brokerverse.events.service.DeadLetterStore;
import com.iortatechnxt.brokerverse.events.service.EventArchiveStore;
import com.iortatechnxt.brokerverse.events.service.EventArchiveStore.ArchiveFilter;
import com.iortatechnxt.brokerverse.events.service.EventSupportService;
import com.iortatechnxt.brokerverse.events.service.EventTopicCatalogue;
import com.iortatechnxt.brokerverse.events.service.EventsProperties;
import com.iortatechnxt.brokerverse.events.service.OutboxStore;
import com.iortatechnxt.brokerverse.events.service.OutboxStore.OutboxFilter;
import java.util.List;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Support API of the integration events (Setup &amp; Administration, System Administrator only):
 * the topic catalogue, the outbox (inspect, requeue a FAILED event), the event archive
 * (traceability by key or correlation id) and the dead letters (list, retry, discard).
 */
@RestController
@RequestMapping("/api/v1/admin/events")
@PreAuthorize("hasAuthority('SYSTEM_PARAMETER_MANAGE')")
public class EventAdminController {

  private static final int MAX_PAGE_SIZE = 200;

  private final EventTopicCatalogue topics;
  private final OutboxStore outbox;
  private final EventArchiveStore archive;
  private final DeadLetterStore deadLetters;
  private final EventSupportService support;
  private final EventsProperties properties;

  /**
   * Creates the controller.
   *
   * @param topics topic catalogue
   * @param outbox outbox store
   * @param archive event archive
   * @param deadLetters dead letters
   * @param support support actions
   * @param properties Kafka settings
   */
  public EventAdminController(
      EventTopicCatalogue topics,
      OutboxStore outbox,
      EventArchiveStore archive,
      DeadLetterStore deadLetters,
      EventSupportService support,
      EventsProperties properties) {
    this.topics = topics;
    this.outbox = outbox;
    this.archive = archive;
    this.deadLetters = deadLetters;
    this.support = support;
    this.properties = properties;
  }

  /**
   * The topic catalogue.
   *
   * @return declared topics with their dead-letter topics
   */
  @GetMapping("/topics")
  public List<TopicResponse> topics() {
    return topics.topics().stream().map(t -> TopicResponse.from(t, properties.enabled())).toList();
  }

  /**
   * Outbox rows, newest first.
   *
   * @param status PENDING, SENT, LOCAL or FAILED
   * @param topic topic
   * @param key event key
   * @param page page
   * @param size page size
   * @return rows
   */
  @GetMapping("/outbox")
  public PageResponse<OutboxEventResponse> outbox(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String topic,
      @RequestParam(required = false) String key,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    OutboxFilter filter =
        new OutboxFilter(blankToNull(status), blankToNull(topic), blankToNull(key));
    int limit = limit(size);
    return page(
        outbox.search(filter, limit, offset(page, limit)),
        outbox.count(filter),
        page,
        limit,
        OutboxEventResponse::from);
  }

  /**
   * Sends a FAILED outbox event again.
   *
   * @param id outbox row id
   */
  @PostMapping("/outbox/{id}/retry")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void retryOutbox(@PathVariable long id) {
    support.requeueOutbox(id);
  }

  /**
   * Archived (consumed) events, newest first.
   *
   * @param topic topic
   * @param key event key
   * @param correlationId correlation id
   * @param page page
   * @param size page size
   * @return events
   */
  @GetMapping("/archive")
  public PageResponse<ArchivedEventResponse> archive(
      @RequestParam(required = false) String topic,
      @RequestParam(required = false) String key,
      @RequestParam(required = false) String correlationId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    ArchiveFilter filter =
        new ArchiveFilter(blankToNull(topic), blankToNull(key), blankToNull(correlationId));
    int limit = limit(size);
    return page(
        archive.search(filter, limit, offset(page, limit)),
        archive.count(filter),
        page,
        limit,
        ArchivedEventResponse::from);
  }

  /**
   * Dead letters, newest first.
   *
   * @param status NEW, RETRIED or DISCARDED (default NEW)
   * @param page page
   * @param size page size
   * @return dead letters
   */
  @GetMapping("/dead-letters")
  public PageResponse<DeadLetterResponse> deadLetters(
      @RequestParam(defaultValue = "NEW") String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    String filter = "ALL".equals(status) ? null : status;
    int limit = limit(size);
    return page(
        deadLetters.search(filter, limit, offset(page, limit)),
        deadLetters.count(filter),
        page,
        limit,
        DeadLetterResponse::from);
  }

  /**
   * Publishes a dead letter again to its original topic.
   *
   * @param id dead-letter id
   */
  @PostMapping("/dead-letters/{id}/retry")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void retryDeadLetter(@PathVariable long id) {
    support.retryDeadLetter(id);
  }

  /**
   * Discards a dead letter.
   *
   * @param id dead-letter id
   */
  @PostMapping("/dead-letters/{id}/discard")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void discardDeadLetter(@PathVariable long id) {
    support.discardDeadLetter(id);
  }

  private static <E, T> PageResponse<T> page(
      List<E> rows, long total, int page, int size, Function<E, T> mapper) {
    int pages = (int) ((total + size - 1) / size);
    return new PageResponse<>(rows.stream().map(mapper).toList(), page, size, total, pages);
  }

  private static int limit(int size) {
    return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
  }

  private static int offset(int page, int limit) {
    return Math.max(0, page) * limit;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
