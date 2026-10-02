package com.iortatechnxt.brokerverse.messaging.service;

/**
 * Result of queuing an e-mail.
 *
 * @param messageId message with the body and attachments
 * @param passwordMessageId separate password message, null when none
 */
public record QueuedEmail(Long messageId, Long passwordMessageId) {}
