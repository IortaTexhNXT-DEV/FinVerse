package com.iortatechnxt.brokerverse.messaging.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.messaging.api.dto.MessageResponse;
import com.iortatechnxt.brokerverse.messaging.domain.MessageStatus;
import com.iortatechnxt.brokerverse.messaging.service.MessageSearch;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Outbound message log (Administration) and the e-mails sent for one record (record pages).
 * Attachments are not downloadable here: the business record keeps the original document.
 */
@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

  private static final int MAX_PAGE_SIZE = 200;
  private static final String VIEW = "hasAuthority('MESSAGE_VIEW')";

  private final MessageService messages;

  /**
   * Creates the controller.
   *
   * @param messages message service
   */
  public MessageController(MessageService messages) {
    this.messages = messages;
  }

  /**
   * Searches the outbox.
   *
   * @param status status filter
   * @param purpose purpose filter
   * @param text recipient / subject / reference contains
   * @param page page
   * @param size size
   * @return messages
   */
  @GetMapping
  @PreAuthorize(VIEW)
  public PageResponse<MessageResponse> search(
      @RequestParam(required = false) MessageStatus status,
      @RequestParam(required = false) String purpose,
      @RequestParam(required = false) String text,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return PageResponse.of(
        messages.search(
            new MessageSearch(status, purpose, text),
            PageRequest.of(page, Math.min(size, MAX_PAGE_SIZE))),
        m -> MessageResponse.from(m, messages.attachments(m.getId())));
  }

  /**
   * E-mails sent for a record.
   *
   * @param entityType record type
   * @param entityId record id
   * @return messages, newest first
   */
  @GetMapping("/by-record")
  @PreAuthorize(
      "hasAnyAuthority('MESSAGE_VIEW', 'CLIENT_VIEW', 'QUOTE_VIEW', 'ACCOUNT_VIEW', 'TSU_PROCESS')")
  public List<MessageResponse> forRecord(
      @RequestParam String entityType, @RequestParam String entityId) {
    return messages.forRecord(entityType, entityId).stream()
        .map(m -> MessageResponse.from(m, messages.attachments(m.getId())))
        .toList();
  }

  /**
   * Queues a failed message again.
   *
   * @param id message
   * @return message
   */
  @PostMapping("/{id}/retry")
  @PreAuthorize(VIEW)
  public MessageResponse retry(@PathVariable Long id) {
    return MessageResponse.from(messages.retry(id), messages.attachments(id));
  }
}
